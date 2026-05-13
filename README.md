# TaskNotes

**TaskNotes** é uma aplicação de produtividade pessoal para organizar tarefas e anotações por categorias temáticas.  
O objetivo desse projeto é somente pra eu conseguir fazer anotações dos meus estudos em casa e poder acessar elas facilmente pelo apk enquanto eu estiver na academia. </br>
Sabe aqueles aprox.30s a 2min de descanso entre uma série e outra na academia? Então, eu costumo ficar lendo notícias de economia e estudando um pouco de geopolítica. </br>
Tem vezes que dá vontade de pesquisar algo de programação ali na hora mas fico com preguiça. </br>
Criei esse projeto para eu poder concentrar todas minhas anotações nele e acessar diretamente pelo celular enquanto eu estiver na academia. </br>

> Status atual: **FINALIZADO v1.0 - Ainda é necessário ajustes de segurança no código**

---

## Visão Geral

O sistema permite criar categorias como **Estudos**, **Saúde**, **Economia**, **Trabalho** ou qualquer outro tema pessoal.  
Dentro de cada categoria, o usuário poderá gerenciar:

- **Tarefas**, com título, descrição, prazo, prioridade, status e subtarefas.
- **Anotações**, com título e conteúdo em texto livre.
- **Categorias**, com limite máximo de 5 categorias simultâneas.

A aplicação foi planejada para funcionar em ambiente web e mobile, consumindo uma mesma API REST.

---

## Ponto importante do projeto e também o mais complexo...

### Sincronização Manual

O projeto também prevê um fluxo de sincronização manual com os botões:

- **Salvar**: envia alterações locais para o servidor.
- **Atualizar**: busca a versão mais recente do servidor.

Esse fluxo utilizará o campo `updated_at` para detectar possíveis conflitos entre alterações locais e dados já salvos no servidor.

Também existe o tratamento para evitar conflitos de "merge" entre as versões.

---

## Stack Tecnológica

### Backend

- Java 17+
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Hibernate
- Jakarta Validation
- MySQL 8.x / MariaDB 10.x
- Maven
- Swagger / OpenAPI

### Frontend Web

- Angular 17+
- TypeScript
- Angular Router
- HttpClient
- CSS / SCSS

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
- Modelo relacional com entidades para categorias, tarefas, subtarefas e anotações.

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
```

A API REST será responsável por centralizar as regras de negócio e persistência dos dados.  
Tanto a aplicação web quanto o aplicativo Android consumirão os mesmos endpoints.

---

## Modelo de Dados Planejado

### Category

Representa uma categoria temática.

Campos principais:

- `id`
- `name`
- `created_at`
- `updated_at`

### Task

Representa uma tarefa dentro de uma categoria.

Campos principais:

- `id`
- `category_id`
- `title`
- `description`
- `due_date`
- `priority`
- `status`
- `created_at`
- `updated_at`

### Subtask

Representa um item de checklist vinculado a uma tarefa.

Campos principais:

- `id`
- `task_id`
- `text`
- `done`
- `created_at`

### Note

Representa uma anotação dentro de uma categoria.

Campos principais:

- `id`
- `category_id`
- `title`
- `content`
- `created_at`
- `updated_at`

---

## Principais Regras de Negócio

- O sistema permite no máximo **5 categorias**.
- O nome da categoria é obrigatório e possui limite de 50 caracteres.
- O título da tarefa é obrigatório e possui limite de 100 caracteres.
- A descrição da tarefa possui limite de 500 caracteres.
- Toda tarefa criada inicia com status **A Fazer**.
- A prioridade padrão de uma tarefa é **Média**.
- Uma tarefa pode ter no máximo **20 subtarefas**.
- O título da anotação é obrigatório e possui limite de 100 caracteres.
- O conteúdo da anotação possui limite de 2000 caracteres.
- Ao excluir uma categoria, suas tarefas e anotações são removidas em cascata.
- Ao excluir uma tarefa, suas subtarefas também são removidas.

---

## Testes Planejados

### Backend

- Testes unitários com JUnit 5 e Mockito.
- Testes de integração com MockMvc.
- Validação de regras de negócio em services.
- Validação de endpoints REST.

### Frontend Angular

- Testes unitários de services.
- Testes de componentes.
- Validação de estados da interface.
- Testes de fluxo para criação, edição e exclusão de categorias.

### Android

- Testes unitários de ViewModels.
- Testes de UI com Compose.
- Validação de estados de sucesso, carregamento e erro.

---

## Como Executar o Projeto

> Instruções em construção. Esta seção será atualizada conforme as etapas forem implementadas.

### Backend

```bash
cd backend
mvn spring-boot:run
```

API local:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

### Frontend

```bash
cd frontend
npm install
npm start
```

Aplicação web:

```text
http://localhost:4200
```

### Android

O app Android será executado pelo Android Studio ou emulador configurado com acesso à API local.

Base URL prevista para o emulador:

```text
http://10.0.2.2:8080
```

---

## EXTRA

Este projeto foi criado como parte do meu portfólio full-stack, com foco em demonstrar:

- Desenvolvimento de API REST com Java e Spring Boot.
- Integração com banco de dados relacional MySQL.
- Construção de interface web com Angular.
- Planejamento de arquitetura separando backend, frontend e mobile.
- Organização de regras de negócio, validações e critérios de aceite.
- Evolução incremental do projeto por etapas.
