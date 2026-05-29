---
title: Clean Code
source_type: curated
source_url: https://www.oreilly.com/library/view/clean-code-a/9780136083238/
trust_level: high
tags: [clean code, código limpo, boas práticas, nomes, funções, refatoração, robert martin]
---

## Clean Code — Princípios de código limpo

Clean Code é uma filosofia de escrita de código legível, manutenível e expressivo, popularizada por Robert C. Martin (Uncle Bob).

### Nomes significativos

Use nomes que revelem intenção:

```java
// Ruim
int d; // elapsed time in days

// Bom
int elapsedTimeInDays;
```

Evite abreviações desnecessárias, termos genéricos (`data`, `info`, `temp`) e desinformação.

### Funções pequenas

- Funções devem fazer **uma única coisa**
- Máximo de 20 linhas (idealmente menos)
- Níveis de abstração consistentes
- Nomes descritivos de verbos

### Evitar comentários desnecessários

Código limpo se explica pelos próprios nomes. Comentários devem justificar o **porquê**, não descrever o **o quê**:

```java
// Ruim: óbvio demais
i++; // incrementa i

// Bom: explica decisão não óbvia
// RFC 2822 exige que datas sem fuso sejam tratadas como UTC
```

### Tratamento de erros

- Use exceções ao invés de códigos de erro
- Não retorne `null` desnecessariamente
- Não ignore exceções silenciosamente

### DRY — Don't Repeat Yourself

Duplicação é raiz de muitos problemas. Extraia lógica repetida em funções/métodos reutilizáveis.

### Boy Scout Rule

"Deixe o código mais limpo do que encontrou." Pequenas melhorias incrementais são o caminho para um código saudável ao longo do tempo.
