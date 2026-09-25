# servico-auditoria

[![CI](https://github.com/LS-PLuka/servico-auditoria/actions/workflows/ci.yml/badge.svg)](https://github.com/LS-PLuka/servico-auditoria/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?style=flat-square)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248?style=flat-square)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-FF6600?style=flat-square)

Serviço de auditoria do **antifraud-system**. Consome de forma assíncrona os resultados produzidos pelo `motor-risco`, persiste o histórico das análises no MongoDB e disponibiliza uma API REST somente leitura para consulta das decisões.

Este serviço **não calcula risco, não executa regras antifraude e não altera a decisão recebida**. Sua responsabilidade é registrar o resultado final produzido pelo motor e disponibilizar esse histórico para consulta.

<p align="center">
  <img src="docs/banner.jpg" alt="Serviço de Auditoria" width="100%">
</p>

---

## Índice

- [Arquitetura](#arquitetura)
- [Evento consumido](#evento-consumido)
- [Persistência e idempotência](#persistência-e-idempotência)
- [API](#api)
- [Contrato de erro](#contrato-de-erro)
- [Decisões de arquitetura e trade-offs](#decisões-de-arquitetura-e-trade-offs)
- [Testes](#testes)
- [Limitações conhecidas](#limitações-conhecidas)
- [Stack](#stack)
- [Configuração](#configuração)
- [Containerização](#containerização)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Parte de um sistema maior](#parte-de-um-sistema-maior)

---

## Arquitetura

```text
motor-risco
     │
     ▼
risco.exchange
routing key: risco.resultado
     │
     ▼
risco.resultados
     │
     ▼
┌──────────────────────────────────────────────┐
│             servico-auditoria                │
│                                              │
│        ResultadoAnaliseConsumer              │
│                    │                         │
│                    ▼                         │
│             AuditoriaService                 │
│                    │                         │
│                    ▼                         │
│             AuditoriaMapper                  │
│                    │                         │
│                    ▼                         │
│            AuditoriaRepository               │
│                    │                         │
└────────────────────┼─────────────────────────┘
                     │
                     ▼
                  MongoDB
```

A escrita acontece exclusivamente pelo fluxo assíncrono.

A consulta segue um caminho independente:

```text
Cliente HTTP
     │
     ▼
AuditoriaController
     │
     ▼
AuditoriaService
     │
     ▼
AuditoriaRepository
     │
     ▼
MongoDB
```

O `ResultadoAnaliseConsumer` é deliberadamente fino: recebe o evento e delega seu registro ao service.

O `AuditoriaService` é responsável pela idempotência e pela persistência. O `AuditoriaMapper` transforma o contrato recebido no documento armazenado, enquanto o repository permanece restrito ao acesso ao MongoDB.

A API não cria, altera ou remove auditorias.

---

## Evento consumido

A topologia RabbitMQ é declarada no `RabbitMQConfig`:

| Componente | Nome | Tipo |
|---|---|---|
| Exchange | `risco.exchange` | `direct`, durável |
| Routing key | `risco.resultado` | — |
| Fila | `risco.resultados` | `durable` |

Payload recebido (`ResultadoAnaliseEventoDTO`):

```json
{
  "transacaoId": "7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c",
  "pontuacao": 155,
  "nivel": "BLOQUEADA",
  "regrasDisparadas": [
    "VALOR_ALTO",
    "CONTA_NOVA",
    "HORARIO_SUSPEITO",
    "VALOR_MUITO_ALTO_CONTA_NOVA",
    "PAIS_ESTRANGEIRO"
  ],
  "analisadoEm": "2026-09-24T10:30:00"
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `transacaoId` | `UUID` | transação à qual a decisão pertence |
| `pontuacao` | `int` | score calculado pelo motor de risco |
| `nivel` | `NivelRisco` | `APROVADA`, `SINALIZADA` ou `BLOQUEADA` |
| `regrasDisparadas` | `List<String>` | regras que contribuíram para o score |
| `analisadoEm` | `LocalDateTime` | momento em que o motor concluiu a análise |

O serviço possui seu próprio `ResultadoAnaliseEventoDTO`.

A desserialização não depende da classe Java utilizada pelo `motor-risco`, evitando compartilhamento de código entre os microsserviços.

O `servico-auditoria` registra a decisão **exatamente como foi recebida**. Ele não recalcula score, não altera o nível e não interpreta novamente as regras disparadas.

---

## Persistência e idempotência

As decisões são persistidas na coleção:

```text
auditorias
```

Cada documento contém:

```text
id
transacaoId
pontuacao
nivel
regrasDisparadas
analisadoEm
registradoEm
```

`analisadoEm` representa o momento em que o `motor-risco` concluiu a análise.

`registradoEm` representa o momento em que o `servico-auditoria` armazenou o resultado.

### Idempotência

`transacaoId` possui índice único no MongoDB.

Antes de persistir um resultado, o service verifica se já existe uma auditoria correspondente à transação.

O comportamento é:

```text
primeira entrega
      ↓
auditoria persistida


mesmo evento entregue novamente
      ↓
auditoria já existe
      ↓
nova gravação ignorada
```

O índice único continua sendo a última garantia contra duplicidades em situações concorrentes.

Isso é importante porque sistemas de mensageria devem assumir que uma mensagem pode ser entregue mais de uma vez.

---

## API

A API é exclusivamente de leitura.

### Auditorias — `/auditorias`

| Método | Rota | Sucesso | Erros |
|---|---|---|---|
| GET | `/auditorias/transacao/{transacaoId}` | `200` | `404` |
| GET | `/auditorias?pagina=0` | `200` | `400` |

### Buscar por transação

```http
GET /auditorias/transacao/7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c
```

Resposta:

```json
{
  "id": "68d52f78d2929559786d5af1",
  "transacaoId": "7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c",
  "pontuacao": 155,
  "nivel": "BLOQUEADA",
  "regrasDisparadas": [
    "VALOR_ALTO",
    "CONTA_NOVA",
    "HORARIO_SUSPEITO",
    "VALOR_MUITO_ALTO_CONTA_NOVA",
    "PAIS_ESTRANGEIRO"
  ],
  "analisadoEm": "2026-09-24T10:30:00",
  "registradoEm": "2026-09-24T10:30:01"
}
```

Quando não existe histórico para a transação:

```text
404 Not Found
```

### Paginação

A listagem utiliza:

```http
GET /auditorias?pagina=0
```

As páginas são base zero, possuem tamanho fixo de **10 registros** e são ordenadas por:

```text
registradoEm DESC
```

Assim, as auditorias mais recentes aparecem primeiro.

Resposta:

```json
{
  "conteudo": [
    {
      "id": "68d52f78d2929559786d5af1",
      "transacaoId": "7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c",
      "pontuacao": 155,
      "nivel": "BLOQUEADA",
      "regrasDisparadas": [
        "VALOR_ALTO",
        "CONTA_NOVA"
      ],
      "analisadoEm": "2026-09-24T10:30:00",
      "registradoEm": "2026-09-24T10:30:01"
    }
  ],
  "paginaAtual": 0,
  "totalPaginas": 3,
  "totalItens": 27,
  "tamanhoPagina": 10
}
```

Uma página negativa retorna `400`.

O documento MongoDB não é exposto diretamente pela API. As respostas utilizam `AuditoriaResponseDTO`.

### Swagger

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8082/swagger-ui.html |
| OpenAPI JSON | http://localhost:8082/v3/api-docs |

A API não possui autenticação nesta versão.

---

## Contrato de erro

As exceções REST passam por um `@RestControllerAdvice` centralizado.

Formato:

```json
{
  "status": 404,
  "erro": "Não encontrado",
  "mensagem": "Auditoria não encontrada para a transação informada"
}
```

| Situação | Status |
|---|---|
| Auditoria inexistente | `404` |
| Página negativa | `400` |
| Parâmetro malformado | `400` |

Stack traces e detalhes internos da aplicação não são expostos ao cliente.

---

## Decisões de arquitetura e trade-offs

### 1. Escrita exclusivamente assíncrona

Nenhum endpoint REST cria auditorias.

Novos registros chegam exclusivamente pela fila `risco.resultados`.

**Por quê:** o histórico deve representar exatamente as decisões produzidas pelo `motor-risco`. Permitir criação manual pela API abriria um segundo caminho de escrita e quebraria essa garantia.

**Custo aceito:** não existe forma de criar manualmente um registro pela API para fins operacionais.

---

### 2. MongoDB para histórico de decisões

O serviço utiliza MongoDB em vez de um banco relacional.

**Por quê:** a auditoria é naturalmente representada como um documento autocontido contendo decisão, score, regras disparadas e timestamps.

O serviço não precisa realizar joins ou manter relacionamentos relacionais com as entidades dos demais microsserviços.

**Custo aceito:** consistência relacional com PostgreSQL não existe por design. O vínculo entre os serviços acontece através de `transacaoId`.

---

### 3. Idempotência por `transacaoId`

Uma transação possui apenas um histórico de decisão nesta versão.

**Por quê:** mensagens podem ser entregues novamente pelo broker. Persistir cegamente cada entrega criaria duplicidades que não representam novas análises reais.

O service verifica a existência do registro e o MongoDB possui índice único como garantia adicional.

**Custo aceito:** o modelo atual não mantém múltiplas versões de análise para a mesma transação.

---

### 4. DTO local para o evento

O serviço não importa classes do `motor-risco`.

**Por quê:** cada microsserviço é uma aplicação independente. Compartilhar DTOs Java criaria acoplamento de implementação entre os repositórios.

O contrato de integração é o JSON publicado no RabbitMQ.

---

### 5. API somente leitura

A API expõe apenas consultas.

**Por quê:** escrita pertence ao fluxo assíncrono; atualização ou exclusão de auditorias comprometeria a ideia de histórico imutável.

**Custo aceito:** correções de dados exigem intervenção operacional fora da API pública atual.

---

## Testes

O comando principal de validação é:

```bash
./mvnw verify
```

No Windows:

```powershell
.\mvnw.cmd verify
```

Para executar somente os testes unitários e HTTP:

```bash
./mvnw test
```

Os testes de integração seguem o sufixo `*IT` e são executados pelo Maven Failsafe durante `verify`.

### Testes unitários

Os testes cobrem:

- mapeamento do evento para `Auditoria`;
- mapeamento para `AuditoriaResponseDTO`;
- geração de `registradoEm`;
- persistência de um evento novo;
- idempotência de evento duplicado;
- consulta por `transacaoId`;
- auditoria inexistente;
- paginação;
- ordenação;
- Consumer delegando para o service;
- comportamento do Controller;
- respostas `200`, `400` e `404`.

Os testes da camada HTTP utilizam MockMvc e não dependem de MongoDB ou RabbitMQ reais.

### Teste de integração

`RegistroAuditoriaIT` utiliza:

```text
RabbitMQ 3.13
MongoDB 7
```

através de Testcontainers.

O teste executa o fluxo real:

```text
risco.exchange
      ↓
risco.resultados
      ↓
ResultadoAnaliseConsumer
      ↓
AuditoriaService
      ↓
AuditoriaRepository
      ↓
MongoDB
```

A mensagem é publicada no RabbitMQ real e o teste aguarda o processamento assíncrono para validar no MongoDB:

- `transacaoId`;
- `pontuacao`;
- `nivel`;
- `regrasDisparadas`;
- `analisadoEm`;
- `registradoEm`.

O cenário de redelivery também é exercitado para confirmar que uma segunda mensagem da mesma transação não produz um segundo documento.

### CI

O GitHub Actions executa em pushes e pull requests direcionados a `develop` e `main`.

A etapa principal é:

```bash
mvn -B verify
```

Assim, a CI executa os testes unitários, testes HTTP e a integração RabbitMQ + MongoDB com Testcontainers.

---

## Stack

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 21 | LTS |
| Spring Boot | 3.5.14 | Framework base |
| Spring Web | — | API REST de consulta |
| Spring Data MongoDB | 3.5.x | Persistência de documentos |
| Spring AMQP | — | Consumo RabbitMQ |
| MongoDB | 7 | Banco de auditoria |
| RabbitMQ | 3.13 | Broker de mensagens |
| SpringDoc OpenAPI | 2.8.16 | Swagger UI |
| Lombok | 1.18.46 | Redução de boilerplate |
| JUnit 5 + Mockito | — | Testes unitários |
| MockMvc | — | Testes da camada HTTP |
| Testcontainers | 1.20.4 | Testes de integração |
| Awaitility | — | Asserções sobre fluxo assíncrono |
| Maven Failsafe | — | Separação unit / integração |
| Docker | — | Containerização |
| GitHub Actions | — | Integração contínua |

`UUID` é utilizado como identificador da transação e `record` para os contratos de transferência quando adequado.

O serviço não utiliza:

- Spring Security;
- JPA;
- PostgreSQL;
- comunicação HTTP com outros microsserviços.

---

## Configuração

| Variável | Default | Descrição |
|---|---|---|
| `SERVER_PORT` | `8082` | Porta HTTP da aplicação |
| `MONGODB_URI` | `mongodb://localhost:27017/antifraude` | URI de conexão com MongoDB |
| `RABBITMQ_HOST` | `localhost` | Host do broker |
| `RABBITMQ_PORT` | `5672` | Porta AMQP |
| `RABBITMQ_USERNAME` | `guest` | Usuário do broker |
| `RABBITMQ_PASSWORD` | `guest` | Senha do broker |

Para executar fora de container, MongoDB e RabbitMQ precisam estar disponíveis.

```bash
./mvnw spring-boot:run
```

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

---

## Containerização

O serviço possui `Dockerfile` próprio com build multi-stage.

O estágio de build utiliza Maven 3.9 + JDK 21. A imagem final contém somente JRE 21 e o JAR da aplicação.

Para construir:

```bash
docker build -t servico-auditoria .
```

O container expõe:

```text
8082
```

A execução exige MongoDB e RabbitMQ acessíveis pela rede:

```bash
docker run --rm \
  -p 8082:8082 \
  -e MONGODB_URI=mongodb://<host-do-mongodb>:27017/antifraude \
  -e RABBITMQ_HOST=<host-do-rabbitmq> \
  -e RABBITMQ_PORT=5672 \
  -e RABBITMQ_USERNAME=guest \
  -e RABBITMQ_PASSWORD=guest \
  servico-auditoria
```

Os hosts precisam ser resolvíveis de dentro do container.

O `servico-auditoria` não possui Docker Compose próprio. A orquestração completa pertence ao repositório central `antifraud-system`, que fornece MongoDB, RabbitMQ e os demais microsserviços.

---

## Estrutura do projeto

```text
src/main/java/antifraud/servicoauditoria/
├── config/           # RabbitMQ, relógio e OpenAPI
├── consumer/         # consumo dos resultados do motor-risco
├── controller/       # endpoints REST somente leitura
├── document/         # documentos MongoDB
├── dto/              # contratos RabbitMQ, API e erros
├── enums/            # NivelRisco
├── exception/        # exceções de domínio + GlobalExceptionHandler
├── repository/       # Spring Data MongoDB
├── service/          # registro, idempotência e consultas
├── util/             # mapeamentos
└── ServicoAuditoriaApplication.java

src/test/java/antifraud/servicoauditoria/
├── consumer/         # delegação do Consumer
├── controller/       # testes HTTP com MockMvc
├── integration/      # RabbitMQ + MongoDB com Testcontainers
├── service/          # registro, idempotência e consultas
└── util/             # mapeamentos
```

A direção principal das dependências é:

```text
RabbitMQ
   ↓
consumer
   ↓
service
   ├── mapper
   └── repository
         ↓
       MongoDB
```

Para consultas:

```text
controller
    ↓
service
    ↓
repository
    ↓
MongoDB
```

O Consumer não persiste diretamente.

O Controller não acessa o repository.

O Mapper não conhece RabbitMQ.

---

## Parte de um sistema maior

| Repositório | Papel |
|---|---|
| [antifraud-system](https://github.com/LS-PLuka/antifraud-system) | Orquestração e documentação geral |
| [servico-transacao](https://github.com/LS-PLuka/servico-transacao) | Entrada, validação, persistência e publicação das transações |
| [motor-risco](https://github.com/LS-PLuka/motor-risco) | Análise, classificação e publicação do risco |
| **servico-auditoria** | **Este repositório** — persistência e consulta do histórico das decisões |

Fluxo completo:

```text
Cliente
   ↓ REST
servico-transacao
   ↓
transacoes.analise
   ↓
motor-risco
   ↓
risco.resultados
   ↓
servico-auditoria
   ↓
MongoDB
```

A consulta do histórico acontece diretamente na API do `servico-auditoria`:

```text
Cliente
   ↓ REST
servico-auditoria
   ↓
MongoDB
```

Nenhum microsserviço acessa diretamente o banco de outro. A comunicação interna entre `servico-transacao`, `motor-risco` e `servico-auditoria` acontece de forma assíncrona pelo RabbitMQ.

---

Desenvolvido por [Pedro Luka](https://github.com/LS-PLuka).