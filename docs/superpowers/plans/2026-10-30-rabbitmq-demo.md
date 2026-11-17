# RabbitMQ Demo Implementation Plan

> **For agentic workers:** Implement the tasks in order and verify against the local RabbitMQ instance.

**Goal:** Add a runnable Spring Boot module demonstrating the seven RabbitMQ official tutorial patterns through HTTP endpoints.

**Architecture:** One application contains isolated packages and broker resources for each pattern. It uses Spring AMQP directly, declares resources through `Declarables`, and logs consumer behavior for observation.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring AMQP, Maven, RabbitMQ 3.13

---

### Task 1: Module foundation

- [ ] Add the module to the root reactor and BOM.
- [ ] Create the application POM, bootstrap class, RabbitMQ connection properties, JSON converter and publisher callback configuration.

### Task 2: Queue and exchange demos

- [ ] Implement simple queue and competing work consumers.
- [ ] Implement Fanout, Direct and Topic declarations, producers, consumers and HTTP endpoints.

### Task 3: RPC and publisher confirms

- [ ] Implement request/reply RPC with a bounded reply timeout.
- [ ] Implement single, batch and asynchronous correlated publisher confirms plus returned-message logging.

### Task 4: Documentation and live verification

- [ ] Document startup, endpoints, routing behavior and management-console resources.
- [ ] Compile the module, start it against localhost:5672, invoke representative HTTP requests and inspect logs.
