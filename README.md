# Notificação Clínica

Microserviço consumidor de eventos de uma API de clínica odontológica. Escuta os eventos de agendamento publicados no RabbitMQ e dispara as notificações correspondentes ao paciente.

Faz parte de um sistema distribuído poliglota: a API de negócio é .NET 10, este worker é Java 21 com Spring Boot 4. Os dois se comunicam exclusivamente por mensageria — **este serviço não tem acesso ao banco de dados da clínica**.

## Stack

| Componente | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring AMQP | 4.1.1 |
| Jackson | 3 (`tools.jackson`) |
| RabbitMQ | 3.x |

## Arquitetura

![Topologia RabbitMQ](docs/topologia-rabbitmq.svg)

### Convenção de nomes

| Elemento | Formato | Exemplo |
|---|---|---|
| Exchange | `sistema.eventos` | `clinica.eventos` |
| Routing key | `entidade.evento` | `consulta.agendada` |
| Fila | `sistema.consumidor.entidade.evento` | `clinica.notificacao.consulta.agendada` |

A fila carrega um nível a mais porque **fila pertence ao consumidor**, não ao produtor. Um segundo consumidor dos mesmos eventos (métricas, auditoria) declara `clinica.metricas.consulta.agendada` sem colidir.

### Uma fila por tipo de evento

Cada fila tem binding com routing key **exata**, sem curinga. A alternativa — fila única com `consulta.*` — entrega formatos diferentes ao mesmo método, e o Jackson preenche o que casa e deixa o resto nulo, sem lançar exceção. Falha silenciosa.

A garantia de tipo vem da topologia, não do corpo da mensagem: o payload é JSON e não carrega informação de tipo.

### Topic com chave exata

O comportamento hoje é idêntico ao de uma `direct exchange`. A escolha por `topic` é sobre o futuro: exchange é imutável, e migrar exige apagar e recriar, perdendo mensagens no intervalo. Com `topic`, um consumidor futuro pode declarar `consulta.#` sem tocar em nada do que existe.

### Tratamento de falha

Falha no listener dispara 3 tentativas com 2s de intervalo, executadas em memória pelo Spring. Esgotadas as tentativas, a mensagem vai para a `clinica.eventos.dlx` e repousa na `clinica.notificacao.dlq` para inspeção manual.

A propriedade `default-requeue-rejected=false` é o que impede o loop infinito de redelivery — sem ela, a configuração de retry não tem efeito prático.

A DLQ não possui dead letter exchange própria, para não reintroduzir o loop um nível acima.

## Eventos consumidos

Contratos publicados pela API .NET (projeto `Clinica.Contratos`), serializados em camelCase.

| Routing key | Record | Disparado por |
|---|---|---|
| `consulta.agendada` | `ConsultaAgendada` | Criação de consulta |
| `consulta.cancelada` | `ConsultaCancelada` | Cancelamento |
| `consulta.reagendada` | `ConsultaReagendada` | Alteração de horário |
| `lembrete.gerado` | `LembreteDeConsulta` | Scheduler (pendente) |

Todos os eventos carregam nome e telefone do paciente. É duplicação deliberada de dado: permite que o worker opere sem consultar o banco da clínica.

## Configuração

`src/main/resources/application.properties`:

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=admin
spring.rabbitmq.password=admin

spring.rabbitmq.listener.simple.retry.enabled=true
spring.rabbitmq.listener.simple.retry.max-attempts=3
spring.rabbitmq.listener.simple.retry.initial-interval=2000
spring.rabbitmq.listener.simple.default-requeue-rejected=false
```

## Execução

Broker:

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin \
  rabbitmq:3-management
```

Aplicação:

```bash
./mvnw spring-boot:run
```

Exchanges, filas e bindings são declarados automaticamente na subida. Painel de administração em `http://localhost:15672`.

## Estrutura

```
src/main/java/com/clinica/notificacao/
├── config/
│   └── RabbitMqConfig.java      # exchanges, filas, bindings, converter
├── evento/
│   ├── ConsultaAgendada.java
│   ├── ConsultaCancelada.java
│   ├── ConsultaReagendada.java
│   └── LembreteDeConsulta.java
└── listener/
    └── ConsultaListener.java    # um método tipado por fila
```

## Notas de implementação

**Jackson 3.** O Spring Boot 4 migrou para o Jackson 3, cujo pacote raiz é `tools.jackson` (o 2.x era `com.fasterxml.jackson`). O starter do AMQP não traz a dependência; ela é declarada explicitamente, sem versão, delegando ao BOM.

**Nomes de fila como constantes.** `@RabbitListener(queues = RabbitMqConfig.FILA_AGENDADA)` em vez de string literal. O nome existe em um lugar só, e uma renomeação quebra na compilação em vez de criar silenciosamente uma fila vazia no broker.

**Bindings injetados por nome.** Os parâmetros dos métodos `@Bean` de binding casam com os nomes dos métodos de fila. Uma renomeação inconsistente falha na subida da aplicação, não em runtime.

## Pendências

