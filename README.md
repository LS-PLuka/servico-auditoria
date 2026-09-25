# servico-auditoria

[![CI](https://github.com/LS-PLuka/servico-auditoria/actions/workflows/ci.yml/badge.svg)](https://github.com/LS-PLuka/servico-auditoria/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?style=flat-square)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat-square)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=flat-square)

Microsserviço de auditoria do **antifraud-system**. Consome os resultados produzidos pelo `motor-risco` e persiste o histórico das análises no MongoDB.

> Status: fluxo principal de registro de auditoria implementado. A API de consulta e a containerização pertencem às próximas fases.

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

O serviço não calcula risco, executa regras antifraude, altera score, acessa PostgreSQL ou responde ao `motor-risco`. A integração interna é assíncrona e os microsserviços mantêm contratos Java independentes.

## Stack inicial

| Tecnologia | Versão/papel |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.14 |
| Spring AMQP | Integração com RabbitMQ |
| Spring Data MongoDB | Persistência do histórico |
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

## Testes e CI

Os testes unitários do mapper, service e consumer são executados pelo Maven Surefire. O teste `RegistroAuditoriaIT`, executado pelo Failsafe, sobe RabbitMQ 3.13 e MongoDB 7 com Testcontainers, publica o evento na exchange real e valida consumo, desserialização, persistência e idempotência.

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
│   └── RelogioConfig.java
├── consumer/
│   └── ResultadoAnaliseConsumer.java
├── document/
│   └── Auditoria.java
├── dto/
│   └── ResultadoAnaliseEventoDTO.java
├── enums/
│   └── NivelRisco.java
├── repository/
│   └── AuditoriaRepository.java
├── service/
│   └── AuditoriaService.java
├── util/
│   └── AuditoriaMapper.java
└── ServicoAuditoriaApplication.java
```

Não existem nesta fase API REST, Swagger ou containerização.

## Parte de um sistema maior

| Repositório | Papel |
|---|---|
| [antifraud-system](https://github.com/LS-PLuka/antifraud-system) | Orquestração e documentação geral |
| [servico-transacao](https://github.com/LS-PLuka/servico-transacao) | Entrada e persistência de transações |
| [motor-risco](https://github.com/LS-PLuka/motor-risco) | Análise e publicação do resultado de risco |
| **servico-auditoria** | **Este repositório** — histórico das análises |

---

Desenvolvido por [Pedro Luka](https://github.com/LS-PLuka).
