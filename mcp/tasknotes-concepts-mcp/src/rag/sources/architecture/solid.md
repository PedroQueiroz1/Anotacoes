---
title: SOLID
source_type: curated
source_url: https://en.wikipedia.org/wiki/SOLID
trust_level: high
tags: [solid, srp, ocp, lsp, isp, dip, clean code, oop, princípios, design]
---

## SOLID — Cinco princípios de design OOP

SOLID é um acrônimo para cinco princípios de design orientado a objetos que tornam o código mais manutenível, extensível e testável.

### S — Single Responsibility Principle (SRP)

Uma classe deve ter apenas **uma razão para mudar**, ou seja, uma única responsabilidade.

```java
// Ruim: classe faz persistência E envio de e-mail
class Usuario { void salvar() {...} void enviarBoasVindas() {...} }

// Bom: responsabilidades separadas
class UsuarioRepository { void salvar(Usuario u) {...} }
class EmailService { void enviarBoasVindas(Usuario u) {...} }
```

### O — Open/Closed Principle (OCP)

Classes devem ser **abertas para extensão** e **fechadas para modificação**.

Adicionar novas funcionalidades sem alterar código existente, usando abstração e herança/interfaces.

### L — Liskov Substitution Principle (LSP)

Subtipos devem ser substituíveis por seus tipos base sem alterar o comportamento do programa.

Se `B` extende `A`, então `A` pode ser substituído por `B` em qualquer lugar sem quebrar a aplicação.

### I — Interface Segregation Principle (ISP)

Interfaces grandes devem ser divididas em interfaces menores e específicas.

Clientes não devem ser forçados a depender de métodos que não usam.

### D — Dependency Inversion Principle (DIP)

Módulos de alto nível não devem depender de módulos de baixo nível. Ambos devem depender de **abstrações** (interfaces).

É a base da **Injeção de Dependência** usada em frameworks como Spring.

### Resumo rápido

| Princípio | Frase-chave |
|-----------|-------------|
| SRP | Uma classe, uma responsabilidade |
| OCP | Estender sem modificar |
| LSP | Subtipos substituíveis |
| ISP | Interfaces pequenas e específicas |
| DIP | Depender de abstrações, não concretos |
