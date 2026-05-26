## PRÓXIMAS ATUALIZAÇÕES

Foco atual: ADICIONAR MCP. Entender o que é RAG para ver se é possível adicionar nesse projeto!

Importante mas prioridade 0: Correções na versão android.

---

# TaskNotes

**TaskNotes** é uma aplicação de produtividade pessoal para organizar tarefas, checklists e anotações por categorias temáticas.

O projeto nasceu de uma necessidade pessoal: concentrar anotações de estudo, programação, economia, desenvolvimento de software e outros temas em um sistema simples, rápido e acessível. A proposta é permitir que o usuário organize conteúdos por categoria, acompanhe tarefas, registre anotações importantes e consulte informações técnicas de forma prática no dia a dia.

> Status atual: **v2.0 — aplicação web e backend publicados, com autenticação, isolamento de dados por usuário e novas funcionalidades em evolução**

---

## Visão Geral

O sistema permite criar categorias como **Estudos de Programação**, **Economia**, **Lista de Leitura**, **Projetos Pessoais** ou qualquer outro tema pessoal.

Dentro de cada categoria, o usuário pode gerenciar:

- **Tarefas**, com título, descrição, prioridade, status, prazo e subtarefas.
- **Checklists**, vinculados às tarefas, com controle de conclusão e barra de progresso.
- **Anotações**, com título e conteúdo em texto livre.
- **Categorias**, com limite máximo de 5 categorias simultâneas.
- **Filtros por status**, para visualizar tarefas a fazer, em andamento, concluídas ou todas.
- **Busca global**, para localizar categorias, tarefas, descrições, anotações e itens de checklist.
- **Modo claro e modo escuro**, com alternância visual na interface.
- **Painel de estatísticas**, com resumo da categoria selecionada.
- **Autocomplete técnico em anotações**, com sugestões de conceitos de programação e tecnologia.
- **Autenticação de usuários**, com login, sessão persistente e isolamento dos dados por conta.
- **Exportação de dados por categoria**, permitindo gerar um arquivo `.txt` com tarefas, subtarefas e anotações.

A aplicação foi planejada para funcionar em ambiente web e mobile, consumindo uma mesma API REST centralizada.

---

## Stack Tecnológica

### Backend

- Java 17+
- Spring Boot 3.x
- Spring Web
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- Jakarta Validation
- MySQL 8.x / MariaDB 10.x
- Maven
- Swagger / OpenAPI
- Nginx
- Deploy em VPS HostGator

### Frontend Web

- Angular 17+
- TypeScript
- Angular Router
- HttpClient
- CSS / SCSS
- Deploy na Vercel

### Mobile

- Kotlin
- Android nativo
- Jetpack Compose
- Retrofit
- OkHttp
- Navigation Compose
- MVVM com ViewModel e StateFlow

### Banco de Dados

- MySQL 8.x
- Banco hospedado em VPS HostGator
- Modelo relacional com entidades para:
  - usuários;
  - categorias;
  - tarefas;
  - subtarefas;
  - anotações;
  - conceitos técnicos de programação;
  - tokens de sessão/refresh.

---

## Arquitetura Geral

```text
Angular Web App        Android App
      |                    |
      | HTTP/REST          | HTTP/REST
      |                    |
      +------> Spring Boot API
                    |
                    | JPA / Hibernate
                    |
                  MySQL
