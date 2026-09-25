# AGENTS.md

## Escopo

Estas instruções valem para todo o repositório `servico-auditoria`. Antes de alterar código, leia o estado real do projeto e preserve mudanças preexistentes. O código e os contratos presentes no repositório são a fonte da verdade. Não faça refatorações fora do escopo solicitado.

## Responsabilidade do serviço

O `servico-auditoria` é um microsserviço orientado a eventos. Sua responsabilidade é consumir resultados produzidos pelo `motor-risco` e persistir o histórico das análises no MongoDB.

Não calcule risco, execute regras antifraude, altere scores, acesse PostgreSQL ou o banco do `servico-transacao`, nem responda ao `motor-risco`. Não introduza comunicação HTTP entre os serviços internos.

Fluxo esperado:

`motor-risco -> risco.exchange -> risco.resultados -> servico-auditoria -> MongoDB`

## Arquitetura e contratos

- Java 21, Spring Boot 3.5.x, Maven, Spring AMQP e Spring Data MongoDB.
- A integração entre os microsserviços é assíncrona por RabbitMQ.
- Cada microsserviço mantém seus próprios DTOs. Não compartilhe classes Java entre repositórios.
- DTOs imutáveis devem ser `record` quando adequado.
- Modelos persistidos no MongoDB pertencem ao pacote `document`, nunca `entity`.
- Use a auto-configuração do Spring Boot para MongoDB enquanto não houver necessidade concreta de configuração customizada.
- A API REST é somente leitura; auditorias continuam sendo criadas exclusivamente pelo consumo de eventos.
- Não adicione Security, JWT, JPA, PostgreSQL, Redis, Kafka ou WebFlux sem requisito da fase correspondente.

Topologia de entrada:

- exchange direct durável: `risco.exchange`
- fila durável: `risco.resultados`
- routing key: `risco.resultado`

Centralize os nomes RabbitMQ em `RabbitMQConfig`. O conversor JSON deve permitir contratos locais e não depender da classe Java do produtor. Não invente exchanges, filas ou routing keys.

O contrato local de entrada é `ResultadoAnaliseEventoDTO`, com `transacaoId`, `pontuacao`, `nivel`, `regrasDisparadas` e `analisadoEm`. O documento `Auditoria` é persistido na coleção `auditorias`, acrescentando `registradoEm`.

A persistência é idempotente por `transacaoId`: o service detecta resultados já registrados e a coleção possui índice único nesse campo. O Consumer permanece fino e apenas delega ao `AuditoriaService`; o service aplica a idempotência e persiste, usando o `AuditoriaMapper` separado para converter o DTO local no documento.

Consultas REST disponíveis:

- `GET /auditorias/transacao/{transacaoId}`;
- `GET /auditorias?pagina=0`, com 10 itens por página e ordenação `registradoEm DESC`.

Controllers permanecem finos, documentos MongoDB não são expostos diretamente e respostas usam DTOs próprios. Erros HTTP são tratados centralmente. A API é documentada com OpenAPI/Swagger e não possui autenticação nesta fase. Não crie endpoints REST de escrita para auditorias.

## Padrão de código

- Prefira código simples, explícito e classes pequenas com responsabilidade única.
- Use nomes de classes, métodos, campos e testes em português e indentação de quatro espaços.
- Use injeção por construtor e dependências `private final`.
- Use Lombok somente quando eliminar boilerplate real; evite setters desnecessários.
- Não crie interfaces com uma única implementação sem benefício concreto, camadas genéricas ou arquitetura artificial.
- Não escreva JavaDocs gerados, comentários extensos ou comentários que apenas repitam o código.
- Consumers devem ser adaptadores finos; regras de orquestração pertencem ao service e persistência ao repository.
- Não crie classes ou pacotes vazios apenas para antecipar estrutura futura.

## Testes e build

- Use JUnit 5 e Mockito em testes unitários.
- Prefira `@ExtendWith(MockitoExtension.class)`, `@Mock` e `@InjectMocks` quando houver dependências.
- Nomeie testes como `metodo_cenario_resultado`, use `@DisplayName` em português e Arrange/Act/Assert quando ajudar a leitura.
- Testes unitários são executados pelo Surefire.
- Integrações usam Testcontainers, têm sufixo `*IT` e são executadas pelo Failsafe durante `verify`.
- Integrações usam MongoDB e RabbitMQ reais via Testcontainers e validam publicação, desserialização, consumo, persistência e idempotência, não apenas chamadas internas.
- Não crie `contextLoads` que dependa de MongoDB ou RabbitMQ locais.
- O comando completo é `mvn verify`; unidades isoladas usam `mvn test`. Informe qualquer validação não executada.
- Não remova testes para fazer o build passar.

## Containerização

- Use Dockerfile multi-stage com Maven 3.9 e JDK 21 no build e somente JRE 21 no runtime.
- A aplicação expõe a porta `8082`, configurável por `SERVER_PORT`.
- O runtime depende de MongoDB e RabbitMQ externos, configurados por `MONGODB_URI`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME` e `RABBITMQ_PASSWORD`.
- Não crie Docker Compose neste repositório. A orquestração dos bancos, broker e microsserviços pertence ao `antifraud-system`.
- O build da imagem não executa testes; a CI permanece responsável por executar `mvn -B verify`.

Não execute commits, crie ou remova branches, faça merge, rebase, push ou qualquer outra operação que altere o histórico Git.
