---
title: Lei de Demeter
source_type: curated
source_url: https://en.wikipedia.org/wiki/Law_of_Demeter
trust_level: high
tags: [lei de demeter, law of demeter, lod, clean code, oop, acoplamento, design]
---

## Lei de Demeter (LoD)

A Lei de Demeter, também chamada de **Princípio do Menor Conhecimento**, diz que um objeto só deve se comunicar com seus vizinhos imediatos.

### Regra resumida

Um método de um objeto deve chamar apenas métodos de:

1. O próprio objeto (`this`)
2. Objetos passados como parâmetro
3. Objetos criados dentro do próprio método
4. Campos diretos do objeto (atributos)

### O que evitar

Evitar "encadeamento de pontos" que navega por múltiplos objetos:

```java
// Ruim — viola a Lei de Demeter
pedido.getCliente().getEndereco().getCidade();

// Bom — delegar ao objeto responsável
pedido.getCidadeDoCliente();
```

### Por que importa

A violação da Lei de Demeter cria **acoplamento oculto**: quando a estrutura interna de um objeto muda, todos que acessam seus internos também precisam mudar.

### Benefícios

- Reduz acoplamento entre classes
- Facilita testes unitários
- Torna refatoração mais segura
- Melhora encapsulamento

### Contexto

O nome vem do Projeto Demeter (1987, Northeastern University). A lei é amplamente citada no livro *Clean Code* de Robert C. Martin e em materiais sobre design orientado a objetos.
