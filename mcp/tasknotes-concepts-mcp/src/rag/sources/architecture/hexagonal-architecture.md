---
title: Arquitetura Hexagonal
source_type: curated
source_url: https://alistair.cockburn.us/hexagonal-architecture/
trust_level: high
tags: [arquitetura hexagonal, hexagonal architecture, ports and adapters, clean architecture, ddd]
---

## Arquitetura Hexagonal (Ports and Adapters)

A Arquitetura Hexagonal, criada por Alistair Cockburn, organiza o software em camadas concêntricas onde o **domínio** fica isolado de tecnologias externas.

### Ideia central

O núcleo da aplicação (domínio e casos de uso) não deve depender de nenhuma tecnologia:
- Não depende de banco de dados específico
- Não depende de framework web
- Não depende de serviços externos

### Estrutura

```
[Interface Web / CLI / API]
         |
    [Adapters de entrada]
         |
    [Ports (interfaces)]
         |
   [Domínio / Use Cases]  ← núcleo isolado
         |
    [Ports (interfaces)]
         |
   [Adapters de saída]
         |
[Banco de dados / APIs externas]
```

### Ports

**Ports** são interfaces que definem como o domínio se comunica com o mundo externo:
- **Driving ports** (primários): interfaces que o domínio expõe para ser acionado
- **Driven ports** (secundários): interfaces que o domínio usa para acessar recursos externos

### Adapters

**Adapters** são implementações concretas dos ports:
- Adapter de entrada: Controller HTTP, listener de fila
- Adapter de saída: Repository JDBC, client HTTP externo

### Benefícios

- Domínio totalmente testável sem banco ou servidor
- Fácil troca de tecnologia (ex: mudar MySQL por PostgreSQL)
- Separação clara de responsabilidades
- Estrutura comum em projetos DDD e Clean Architecture

### No Spring Boot

Em Spring, as interfaces `Repository` do JPA são ports driven. Os `@RestController` são adapters de entrada.
