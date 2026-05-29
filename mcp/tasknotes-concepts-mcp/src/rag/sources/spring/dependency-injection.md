---
title: Injeção de Dependência
source_type: curated
source_url: https://docs.spring.io/spring-framework/docs/current/reference/html/core.html
trust_level: high
tags: [injeção de dependência, dependency injection, di, ioc, spring, spring boot, beans]
---

## Injeção de Dependência (Dependency Injection)

Injeção de Dependência (DI) é um padrão em que as dependências de um objeto são fornecidas externamente, em vez de o próprio objeto criá-las.

### Inversão de Controle (IoC)

DI é uma forma de implementar **Inversão de Controle**: o controle de criação de objetos é delegado a um container (como o Spring IoC Container).

### Tipos de injeção no Spring

#### 1. Injeção por construtor (recomendada)

```java
@Service
public class PedidoService {
    private final EstoqueRepository estoqueRepository;

    public PedidoService(EstoqueRepository estoqueRepository) {
        this.estoqueRepository = estoqueRepository;
    }
}
```

#### 2. Injeção por campo (`@Autowired`)

```java
@Service
public class PedidoService {
    @Autowired
    private EstoqueRepository estoqueRepository;
}
```

A injeção por construtor é preferida porque torna dependências explícitas e facilita testes.

#### 3. Injeção por setter

Menos comum; usada quando a dependência é opcional.

### Beans no Spring

Um **Bean** é qualquer objeto gerenciado pelo container Spring. Anotações que criam beans:

| Anotação | Uso |
|----------|-----|
| `@Component` | Bean genérico |
| `@Service` | Camada de serviço/negócio |
| `@Repository` | Camada de persistência |
| `@Controller` / `@RestController` | Camada web |
| `@Configuration` + `@Bean` | Configuração manual |

### Benefícios

- Código mais testável (dependências podem ser substituídas por mocks)
- Baixo acoplamento entre classes
- Facilita aplicação do princípio SOLID (DIP)
- Gestão automática de ciclo de vida de objetos
