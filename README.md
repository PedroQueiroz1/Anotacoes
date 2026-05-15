## PRÓXIMAS ATUALIZAÇÕES

1. Correções de segurança.
2. Melhorias de usabilidade no frontend.
3. Ajustes e refinamentos no aplicativo Android.
4. Melhorias nos testes automatizados.
5. Evolução da documentação do projeto.

---

# TaskNotes

**TaskNotes** é uma aplicação de produtividade pessoal para organizar tarefas, checklists e anotações por categorias temáticas.

O projeto nasceu de uma necessidade pessoal: concentrar anotações de estudo, programação, economia e outros temas em um sistema simples, acessível pela web e também pelo celular. A ideia é permitir consultas rápidas em momentos do dia a dia, como intervalos entre tarefas, estudos ou até durante descansos na academia.

> Status atual: **FINALIZADO v1.0 — aplicação web e backend publicados, com melhorias de segurança ainda planejadas**

---

## Visão Geral

O sistema permite criar categorias como **Estudos**, **Economia**, **Programação**, **Saúde**, **Trabalho** ou qualquer outro tema pessoal.

Dentro de cada categoria, o usuário pode gerenciar:

- **Tarefas**, com título, descrição, prazo, prioridade, status e subtarefas.
- **Checklists**, vinculados às tarefas, com acompanhamento de progresso.
- **Anotações**, com título e conteúdo em texto livre.
- **Filtros por status**, para visualizar tarefas a fazer, em andamento, concluídas ou todas.
- **Busca global**, para localizar categorias, tarefas, descrições, anotações e itens de checklist.
- **Modo claro e modo escuro**, com alternância visual na interface.
- **Painel de estatísticas**, com resumo da categoria selecionada.

A aplicação foi planejada para funcionar em ambiente web e mobile, consumindo uma mesma API REST.

---

## Funcionalidades Implementadas

### Categorias

- Criar categoria.
- Listar categorias na sidebar.
- Editar categoria.
- Excluir categoria com confirmação.
- Limite máximo de 5 categorias.
- Reorganização por drag and drop.
- Navegação entre categorias pela sidebar.

### Tarefas

- Criar tarefas dentro de uma categoria.
- Editar título, descrição, prazo, prioridade e status.
- Excluir tarefas.
- Alterar status entre:
  - A Fazer
  - Em andamento
  - Concluída
- Filtrar tarefas por status.
- Reorganizar tarefas por drag and drop.

### Checklists / Subtarefas

- Adicionar itens de checklist em uma tarefa.
- Marcar e desmarcar itens como concluídos.
- Exibir barra de progresso do checklist.
- Calcular progresso geral da categoria com base nos checklists.
- Planejamento de edição inline dos itens do checklist.

### Anotações

- Criar anotações dentro de uma categoria.
- Editar título e conteúdo.
- Excluir anotações.
- Listar anotações por categoria.

### Interface Web

- Layout com sidebar lateral.
- Cards de estatísticas.
- Busca global.
- Filtros de tarefas.
- Modo claro e modo escuro.
- Interface responsiva para uso em desktop.
- Deploy do frontend na Vercel.

### Backend

- API REST com Spring Boot.
- Persistência em MySQL.
- Integração com frontend Angular.
- Deploy do backend no Railway.
- Banco MySQL hospedado no Railway.

---

## Ponto importante do projeto

### Sincronização e consistência dos dados

O projeto foi estruturado para centralizar os dados no backend, permitindo que o frontend web e o aplicativo Android consumam a mesma API REST.

A API é responsável por:

- Persistir categorias, tarefas, subtarefas e anotações.
- Aplicar regras de negócio.
- Validar dados enviados pelo frontend.
- Garantir integridade entre categorias e seus conteúdos vinculados.
- Manter o banco MySQL como fonte principal de dados.

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
- Banco hospedado no Railway
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

A API REST centraliza as regras de negócio e a persistência dos dados.

Tanto a aplicação web quanto o aplicativo Android consomem os mesmos endpoints.

---

## Deploy

### Frontend

O frontend web está preparado para deploy estático na **Vercel**.

Fluxo:

```text
Angular → Build estático → Vercel
```

### Backend

O backend está preparado para deploy no **Railway**.

Fluxo:

```text
Spring Boot API → Railway → MySQL Railway
```

### Banco de Dados

O banco MySQL está hospedado no Railway com volume persistente.

---

## Principais Regras de Negócio

- O sistema permite no máximo **5 categorias**.
- O nome da categoria é obrigatório e possui limite de 50 caracteres.
- O título da tarefa é obrigatório e possui limite de 100 caracteres.
- A descrição da tarefa possui limite de 500 caracteres.
- Toda tarefa criada inicia com status **A Fazer**.
- A prioridade padrão de uma tarefa é **Baixa**.
- Uma tarefa pode ter no máximo **20 subtarefas**.
- O título da anotação é obrigatório e possui limite de 100 caracteres.
- O conteúdo da anotação possui limite de 2000 caracteres.
- Ao excluir uma categoria, suas tarefas e anotações são removidas em cascata.
- Ao excluir uma tarefa, suas subtarefas também são removidas.
- Tarefas, categorias e anotações podem ser reorganizadas dentro de suas respectivas listas.
- O sistema não permite mover tarefa para a área de anotações nem anotação para a área de tarefas.

---

## Testes Planejados

### Backend

- Testes unitários com JUnit 5 e Mockito.
- Testes de integração com MockMvc.
- Validação de regras de negócio em services.
- Validação de endpoints REST.
- Testes para regras de categoria, tarefas, subtarefas e anotações.

### Frontend Angular

- Testes unitários de services.
- Testes de componentes.
- Validação de estados da interface.
- Testes de fluxo para criação, edição e exclusão de categorias.
- Testes de filtros, busca global e alternância de tema.

### Android

- Testes unitários de ViewModels.
- Testes de UI com Compose.
- Validação de estados de sucesso, carregamento e erro.

---

## Como Executar o Projeto Localmente

### Pré-requisitos

- Java 17+
- Maven
- Node.js
- Angular CLI
- MySQL 8.x ou MariaDB 10.x
- Android Studio, caso queira executar o app Android

---

### Backend

Acesse a pasta do backend:

```bash
cd backend
```

Execute a aplicação:

```bash
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

---

### Frontend

Acesse a pasta do frontend:

```bash
cd frontend
```

Instale as dependências:

```bash
npm install
```

Execute a aplicação:

```bash
npm start
```

Aplicação web local:

```text
http://localhost:4200
```

---

### Android

O app Android pode ser executado pelo Android Studio.

Para emulador Android, a base URL prevista para acessar o backend local é:

```text
http://10.0.2.2:8080
```

Para celular físico, é necessário apontar a base URL para o IP local do computador onde o backend está rodando.

---

## Segurança

Este projeto ainda possui melhorias de segurança planejadas.

Pontos de atenção:

- A aplicação é single-user e ainda não possui autenticação.
- Não deve ser usada para dados sensíveis em produção.
- Melhorias futuras devem incluir autenticação, controle de acesso, proteção de endpoints e revisão de CORS.
- O banco de dados deve possuir backup e variáveis sensíveis devem permanecer fora do repositório.

---
