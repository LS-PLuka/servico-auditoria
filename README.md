# servico-auditoria

[![CI](https://github.com/LS-PLuka/servico-auditoria/actions/workflows/ci.yml/badge.svg)](https://github.com/LS-PLuka/servico-auditoria/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?style=flat-square)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat-square)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=flat-square)

Microsserviço de auditoria do **antifraud-system**. Será responsável por consumir os resultados produzidos pelo `motor-risco` e persistir o histórico das análises no MongoDB.

> Status: em desenvolvimento. Nesta fase, somente a estrutura inicial, as conexões externalizáveis e a topologia RabbitMQ estão preparadas.

## Fluxo

```text
motor-risco
    ↓
risco.exchange
routing key: risco.resultado
    ↓
risco.resultados
    ↓
servico-auditoria
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
| Spring Data MongoDB | Persistência futura do histórico |
| JUnit 5 e Mockito | Testes unitários |
| Testcontainers | Integrações futuras com MongoDB e RabbitMQ |
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

A topologia e o conversor JSON já estão declarados. O Consumer ainda não faz parte desta fase.

## Testes e CI

Testes unitários são executados pelo Maven Surefire. Testes de integração futuros devem usar o sufixo `*IT` e serão executados pelo Maven Failsafe durante `verify`.

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
│   └── RabbitMQConfig.java
└── ServicoAuditoriaApplication.java
```

Os pacotes `consumer`, `dto`, `document`, `repository` e `service` serão criados conforme suas implementações forem adicionadas. Não existem nesta fase Consumer, persistência, API de consulta ou containerização.

## Parte de um sistema maior

| Repositório | Papel |
|---|---|
| [antifraud-system](https://github.com/LS-PLuka/antifraud-system) | Orquestração e documentação geral |
| [servico-transacao](https://github.com/LS-PLuka/servico-transacao) | Entrada e persistência de transações |
| [motor-risco](https://github.com/LS-PLuka/motor-risco) | Análise e publicação do resultado de risco |
| **servico-auditoria** | **Este repositório** — histórico das análises |

---

Desenvolvido por [Pedro Luka](https://github.com/LS-PLuka).
