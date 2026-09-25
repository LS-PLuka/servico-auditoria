# servico-auditoria

[![CI](https://github.com/LS-PLuka/servico-auditoria/actions/workflows/ci.yml/badge.svg)](https://github.com/LS-PLuka/servico-auditoria/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?style=flat-square)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat-square)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=flat-square)

Microsserviço de auditoria do **antifraud-system**. Consome os resultados produzidos pelo `motor-risco`, persiste o histórico das análises no MongoDB e disponibiliza uma API REST somente leitura.

> Status: fluxo de registro e API de consulta implementados. A containerização pertence à próxima fase.

## Fluxo

```text
motor-risco
    ↓
risco.exchange
routing key: risco.resultado
    ↓
risco.resultados
    ↓
ResultadoAnaliseConsumer
    ↓
AuditoriaService
    ↓
AuditoriaMapper
    ↓
AuditoriaRepository
    ↓
MongoDB
```

Consultas seguem um fluxo separado e somente de leitura:

```text
cliente HTTP -> AuditoriaController -> AuditoriaService -> AuditoriaRepository -> MongoDB
```

O serviço não calcula risco, executa regras antifraude, altera score, acessa PostgreSQL ou responde ao `motor-risco`. A API não cria, altera ou exclui auditorias; novos registros continuam sendo produzidos exclusivamente pelo fluxo assíncrono.

## Stack inicial

| Tecnologia | Versão/papel |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.14 |
| Spring AMQP | Integração com RabbitMQ |
| Spring Data MongoDB | Persistência do histórico |
| Spring Web | API REST de consulta |
| SpringDoc OpenAPI | 2.8.16 |
| JUnit 5 e Mockito | Testes unitários |
| Testcontainers | Integração real com MongoDB 7 e RabbitMQ 3.13 |
| Maven Surefire/Failsafe | Separação entre testes unitários e de integração |

## Configuração

| Variável | Default | Descrição |
|---|---|---|
| `SERVER_PORT` | `8082` | Porta da aplicação |
| `MONGODB_URI` | `mongodb://localhost:27017/antifraude` | URI de conexão com o MongoDB |
| `RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `RABBITMQ_PORT` | `5672` | Porta AMQP |
| `RABBITMQ_USERNAME` | `guest` | Usuário do RabbitMQ |
| `RABBITMQ_PASSWORD` | `guest` | Senha do RabbitMQ |

### Topologia RabbitMQ preparada

| Componente | Nome | Tipo |
|---|---|---|
| Exchange | `risco.exchange` | `direct`, durável |
| Routing key | `risco.resultado` | — |
| Fila | `risco.resultados` | durável |

A topologia e o conversor JSON usam o tipo inferido pelo método do listener, permitindo que o evento seja desserializado no contrato local sem dependência da classe Java do produtor.

## Contrato consumido

O `ResultadoAnaliseEventoDTO` é um `record` local com o seguinte formato:

```json
{
  "transacaoId": "7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c",
  "pontuacao": 155,
  "nivel": "BLOQUEADA",
  "regrasDisparadas": ["VALOR_ALTO", "CONTA_NOVA"],
  "analisadoEm": "2026-09-24T10:30:00"
}
```

Os níveis aceitos são `APROVADA`, `SINALIZADA` e `BLOQUEADA`. O serviço registra a decisão recebida sem recalcular score, reclassificar o nível ou interpretar regras.

## Persistência e idempotência

Cada resultado gera um documento na coleção `auditorias` com:

- `id`;
- `transacaoId`;
- `pontuacao`;
- `nivel`;
- `regrasDisparadas`;
- `analisadoEm`, recebido do `motor-risco`;
- `registradoEm`, definido no momento do registro.

Antes de persistir, o serviço consulta a existência de uma auditoria pela `transacaoId`. Repetições são ignoradas, e um índice único em `transacaoId` garante a unicidade também em entregas concorrentes.

## API REST de consulta

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/auditorias/transacao/{transacaoId}` | Busca a auditoria de uma transação; retorna `404` quando inexistente |
| `GET` | `/auditorias?pagina=0` | Lista o histórico paginado |

A listagem usa páginas base zero, tamanho fixo de 10 itens e ordenação por `registradoEm DESC`. Uma página negativa retorna `400`. O documento MongoDB não é exposto diretamente; a API retorna `AuditoriaResponseDTO`.

Exemplo de consulta individual:

```json
{
  "id": "68d52f78d2929559786d5af1",
  "transacaoId": "7f3e4c2a-1b5d-4e8f-9a2c-3d6b7e1f4a8c",
  "pontuacao": 155,
  "nivel": "BLOQUEADA",
  "regrasDisparadas": ["VALOR_ALTO", "CONTA_NOVA"],
  "analisadoEm": "2026-09-24T10:30:00",
  "registradoEm": "2026-09-24T10:30:01"
}
```

Exemplo de página:

```json
{
  "conteudo": [],
  "paginaAtual": 0,
  "totalPaginas": 3,
  "totalItens": 27,
  "tamanhoPagina": 10
}
```

Erros possuem `status`, `erro` e `mensagem`, sem exposição de stack trace.

## OpenAPI e Swagger

- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

A API não possui autenticação nesta fase.

## Testes e CI

Os testes unitários do mapper, service e consumer, além dos testes HTTP com MockMvc, são executados pelo Maven Surefire. Os testes de controller não dependem de MongoDB ou RabbitMQ. O teste `RegistroAuditoriaIT`, executado pelo Failsafe, sobe RabbitMQ 3.13 e MongoDB 7 com Testcontainers, publica o evento na exchange real e valida consumo, desserialização, persistência e idempotência.

```bash
./mvnw test
./mvnw verify
```

No Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

A CI executa `mvn -B verify` em pushes e pull requests para `develop` e `main`.

## Estrutura atual

```text
src/main/java/antifraud/servicoauditoria/
├── config/
│   ├── RabbitMQConfig.java
│   ├── RelogioConfig.java
│   └── OpenApiConfig.java
├── consumer/
│   └── ResultadoAnaliseConsumer.java
├── controller/
│   └── AuditoriaController.java
├── document/
│   └── Auditoria.java
├── dto/
│   ├── AuditoriaResponseDTO.java
│   ├── ErroResponseDTO.java
│   ├── PaginaResponseDTO.java
│   └── ResultadoAnaliseEventoDTO.java
├── enums/
│   └── NivelRisco.java
├── exception/
│   ├── AuditoriaNaoEncontradaException.java
│   ├── GlobalExceptionHandler.java
│   └── PaginaInvalidaException.java
├── repository/
│   └── AuditoriaRepository.java
├── service/
│   └── AuditoriaService.java
├── util/
│   └── AuditoriaMapper.java
└── ServicoAuditoriaApplication.java
```

## Limitações conhecidas

- não há autenticação ou autorização;
- o tamanho da página é fixo em 10;
- a API não possui filtros além de `transacaoId`;
- a containerização pertence à próxima fase.

## Parte de um sistema maior

| Repositório | Papel |
|---|---|
| [antifraud-system](https://github.com/LS-PLuka/antifraud-system) | Orquestração e documentação geral |
| [servico-transacao](https://github.com/LS-PLuka/servico-transacao) | Entrada e persistência de transações |
| [motor-risco](https://github.com/LS-PLuka/motor-risco) | Análise e publicação do resultado de risco |
| **servico-auditoria** | **Este repositório** — histórico das análises |

---

Desenvolvido por [Pedro Luka](https://github.com/LS-PLuka).
