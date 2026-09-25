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
- Não adicione Web, Security, JWT, JPA, PostgreSQL, Redis, Kafka ou WebFlux sem requisito da fase correspondente.

Topologia de entrada:

- exchange direct durável: `risco.exchange`
- fila durável: `risco.resultados`
- routing key: `risco.resultado`

Centralize os nomes RabbitMQ em `RabbitMQConfig`. O conversor JSON deve permitir contratos locais e não depender da classe Java do produtor. Não invente exchanges, filas ou routing keys.

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
- Integrações futuras devem usar MongoDB e RabbitMQ reais via Testcontainers e validar contratos e persistência, não apenas chamadas internas.
- Não crie `contextLoads` que dependa de MongoDB ou RabbitMQ locais.
- O comando completo é `mvn verify`; unidades isoladas usam `mvn test`. Informe qualquer validação não executada.
- Não remova testes para fazer o build passar.

## Próximas fases

### `feature/registro-auditoria`

- DTO local do evento;
- Document;
- Repository;
- Mapper;
- Consumer;
- Service;
- persistência;
- testes unitários e de integração.

### `feature/api-consulta`

- endpoints de consulta;
- paginação;
- Swagger/OpenAPI.

### `feature/containerizacao`

- Dockerfile;
- `.dockerignore`;
- adequação final do README.

Não antecipe esses itens sem solicitação. Não execute commits, crie ou remova branches, faça merge, rebase, push ou qualquer outra operação que altere o histórico Git.
