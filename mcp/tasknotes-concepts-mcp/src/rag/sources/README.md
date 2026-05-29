# Fontes confiáveis RAG — TaskNotes Concepts MCP

Este diretório contém documentos curados usados pelo RAG (Retrieval-Augmented Generation) para responder buscas de termos técnicos.

## Como adicionar um documento

1. Crie um arquivo `.md` na subpasta temática (`architecture/`, `java/`, `spring/`, `web/`, etc.)
2. Adicione o frontmatter obrigatório no topo do arquivo:

```markdown
---
title: Nome do Conceito
source_type: curated
source_url: https://referencia-oficial.com (opcional)
trust_level: high
tags: [tag1, tag2, tag3]
---

Conteúdo em português...
```

## Campos do frontmatter

| Campo | Obrigatório | Valores |
|-------|-------------|---------|
| title | sim | Nome do conceito |
| source_type | sim | `curated`, `official`, `glossary` |
| source_url | não | URL da referência oficial |
| trust_level | sim | `high`, `medium`, `low` |
| tags | sim | Lista de termos relacionados |

## Regras de qualidade

- Escreva em português claro e objetivo
- Seja conciso: prefira parágrafos curtos
- Indique a fonte quando possível
- Não copie capítulos inteiros de livros
- Não inclua conteúdo gerado por IA não revisado
- Trust level `high` só para documentação oficial ou material próprio revisado

## Estrutura de pastas

```
sources/
  architecture/   — padrões e princípios de arquitetura
  java/           — conceitos Java e JVM
  spring/         — Spring Framework e Spring Boot
  web/            — HTTP, CORS, REST, WebSockets
  database/       — SQL, NoSQL, JPA, ORM
  devops/         — Docker, CI/CD, Git
  glossary/       — glossário geral curado
```
