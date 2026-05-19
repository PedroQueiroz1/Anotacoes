## PRÓXIMAS ATUALIZAÇÕES

1. Melhorias de segurança, autenticação e autorização.
5. Melhorias no aplicativo Android.
6. Ampliação dos testes automatizados.
7. Melhorias de observabilidade, logs e tratamento de erros.
8. Ajustes na funcionalidade de autocomplete
9. Correções de Bugs
10. Monolito -> Monolito modular

---

# TaskNotes

**TaskNotes** é uma aplicação de produtividade pessoal para organizar tarefas, checklists e anotações por categorias temáticas.

O projeto nasceu de uma necessidade pessoal: concentrar anotações de estudo, programação, economia, desenvolvimento de software e outros temas em um sistema simples, rápido e acessível. A proposta é permitir que o usuário organize conteúdos por categoria, acompanhe tarefas, registre anotações importantes e consulte informações técnicas de forma prática no dia a dia.

> Status atual: **v2.0 — aplicação web e backend publicados, com novas funcionalidades em evolução e melhorias de segurança planejadas**

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

A aplicação foi planejada para funcionar em ambiente web e mobile, consumindo uma mesma API REST centralizada.

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
- Suporte a URLs amigáveis por categoria, quando configurado por slug.

### Tarefas

- Criar tarefas dentro de uma categoria.
- Editar título e descrição.
- Editar status da tarefa.
- Editar prioridade da tarefa.
- Excluir tarefas.
- Alterar status entre:
  - A Fazer
  - Em andamento
  - Concluída
- Filtrar tarefas por status.
- Reorganizar tarefas por drag and drop.
- Exibir prioridade visualmente por nível:
  - Baixa
  - Média
  - Alta

### Checklists / Subtarefas

- Adicionar itens de checklist em uma tarefa.
- Marcar e desmarcar itens como concluídos.
- Exibir itens concluídos com marcação visual.
- Exibir barra de progresso do checklist.
- Calcular progresso com base nas subtarefas concluídas.
- Editar itens do checklist.
- Excluir itens do checklist.
- Suporte visual para links do YouTube dentro de itens do checklist.

### Links do YouTube em Checklists

- Detecção de links do YouTube em itens de checklist.
- Exibição do título do vídeo quando o item contém um link reconhecido.
- Link clicável para abrir o vídeo original.
- Separação da área clicável do link em relação aos botões de editar e excluir.
- Fallback para exibição do link quando o título não puder ser obtido.

### Anotações

- Criar anotações dentro de uma categoria.
- Editar título e conteúdo.
- Excluir anotações.
- Listar anotações por categoria.
- Preservar quebras de linha no conteúdo.
- Exibir conteúdo longo de forma resumida.
- Expandir conteúdo com **Ver mais**.
- Recolher conteúdo com **Ver menos**.
- Manter o conteúdo completo disponível para edição e busca.

### Autocomplete Técnico em Anotações

- Sugestão de conceitos de programação no campo de conteúdo das anotações.
- Foco em termos técnicos como:
  - JSON
  - UUID
  - POJO
  - DTO
  - REST
  - CORS
  - TypeScript
  - Array
  - Bubble Sort
  - N8N
  - Docker
  - Kubernetes
- Base local de conceitos técnicos.
- Salvamento de conceitos aceitos na base local.
- Estrutura preparada para fallback externo e IA opcional por variável de ambiente.
- Comportamento de teclado:
  - `Enter` aceita sugestão quando o autocomplete está aberto.
  - `Enter` cria nova linha quando não há sugestão ativa.
  - `Shift + Enter` sempre cria nova linha.
  - `Esc` fecha a sugestão.

### Interface Web

- Layout com sidebar lateral.
- Cards de estatísticas por categoria.
- Busca global.
- Filtros de tarefas.
- Modo claro e modo escuro.
- Design responsivo para desktop.
- Ações por hover em categorias, tarefas e anotações.
- Feedback visual para estados de carregamento e erro.
- Deploy do frontend na Vercel.

### Backend

- API REST com Spring Boot.
- Persistência em MySQL.
- Integração com frontend Angular.
- Deploy do backend no Railway.
- Banco MySQL hospedado no Railway.
- Configuração de CORS para ambientes locais e produção.
- Estrutura de services, controllers, repositories e DTOs.
- Logs planejados para fluxos críticos sem expor conteúdo sensível.

---

## Ponto importante do projeto

### Sincronização e consistência dos dados

O projeto foi estruturado para centralizar os dados no backend, permitindo que o frontend web e o aplicativo Android consumam a mesma API REST.

A API é responsável por:

- Persistir categorias, tarefas, subtarefas, anotações e conceitos técnicos.
- Aplicar regras de negócio.
- Validar dados enviados pelo frontend.
- Garantir integridade entre categorias e seus conteúdos vinculados.
- Manter o banco MySQL como fonte principal de dados.
- Preparar a aplicação para futuras melhorias de segurança e autenticação.

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
- Modelo relacional com entidades para:
  - categorias;
  - tarefas;
  - subtarefas;
  - anotações;
  - conceitos técnicos de programação.

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

Tanto a aplicação web quanto o aplicativo Android foram pensados para consumir os mesmos endpoints.

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
- O nome da categoria é obrigatório.
- O título da tarefa é obrigatório.
- Toda tarefa deve possuir um status válido.
- Toda tarefa deve possuir uma prioridade válida.
- A prioridade padrão de uma tarefa é **Baixa**, quando nenhuma prioridade é informada.
- Uma tarefa pode possuir subtarefas/checklist.
- Itens de checklist podem ser marcados como concluídos ou pendentes.
- A barra de progresso do checklist deve considerar os itens concluídos.
- O título da anotação é obrigatório.
- O conteúdo da anotação deve preservar quebras de linha.
- O conteúdo longo de uma anotação pode ser exibido de forma resumida.
- Ao excluir uma categoria, seus dados vinculados devem ser tratados de forma consistente.
- Ao excluir uma tarefa, suas subtarefas também devem ser removidas.
- O sistema não deve mover tarefa para a área de anotações nem anotação para a área de tarefas.
- Sugestões de autocomplete devem ser limitadas a conceitos de programação/tecnologia.
- Conteúdo completo de anotações não deve ser enviado para serviços externos de sugestão.

---

## Endpoints Principais

> Os caminhos podem variar conforme a evolução do projeto.

### Categorias

```http
GET    /api/categories
POST   /api/categories
PUT    /api/categories/{id}
DELETE /api/categories/{id}
GET    /api/categories/slug/{slug}
```

### Tarefas

```http
GET    /api/categories/{categoryId}/tasks
POST   /api/categories/{categoryId}/tasks
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
PATCH  /api/tasks/{id}/status
PATCH  /api/tasks/{id}/priority
```

### Subtarefas

```http
GET    /api/tasks/{taskId}/subtasks
POST   /api/tasks/{taskId}/subtasks
PUT    /api/subtasks/{id}
PATCH  /api/subtasks/{id}/toggle
DELETE /api/subtasks/{id}
```

### Anotações

```http
GET    /api/categories/{categoryId}/notes
POST   /api/categories/{categoryId}/notes
PUT    /api/notes/{id}
DELETE /api/notes/{id}
```

### Conceitos Técnicos

```http
GET    /api/concepts/suggest?term={term}
POST   /api/concepts/accept
```

---

## Testes Planejados

### Backend

- Testes unitários com JUnit 5 e Mockito.
- Testes de integração com MockMvc.
- Validação de regras de negócio em services.
- Validação de endpoints REST.
- Testes para categorias, tarefas, subtarefas e anotações.
- Testes para autocomplete de conceitos técnicos.
- Testes para regras de prioridade, status e progresso de checklist.

### Frontend Angular

- Testes unitários de services.
- Testes de componentes.
- Validação de estados da interface.
- Testes de fluxo para criação, edição e exclusão de categorias.
- Testes de filtros, busca global e alternância de tema.
- Testes de autocomplete no textarea de anotações.
- Testes de expansão/recolhimento de conteúdo de anotações.

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

Configure as variáveis de ambiente necessárias para conexão com o banco.

Exemplo:

```properties
MYSQLHOST=localhost
MYSQLPORT=3306
MYSQLDATABASE=tasknotes
MYSQLUSER=root
MYSQLPASSWORD=senha
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

## Variáveis de Ambiente

### Backend

Exemplos de variáveis usadas ou previstas:

```text
MYSQLHOST
MYSQLPORT
MYSQLDATABASE
MYSQLUSER
MYSQLPASSWORD
APP_CORS_ALLOWED_ORIGIN_PATTERNS
CONCEPT_AI_ENABLED
CONCEPT_AI_PROVIDER
CONCEPT_AI_API_KEY
CONCEPT_EXTERNAL_SEARCH_ENABLED
CONCEPT_EXTERNAL_SEARCH_API_KEY
```

### CORS

A aplicação pode usar uma variável para controlar origens permitidas:

```text
APP_CORS_ALLOWED_ORIGIN_PATTERNS
```

Exemplo:

```text
http://localhost:4200,https://*.vercel.app,https://*.pedroqueiroz.app
```

---

## Segurança

Este projeto ainda possui melhorias de segurança planejadas.

Pontos de atenção:

- A aplicação ainda é voltada para uso pessoal/single-user.
- Não deve ser usada para dados sensíveis em produção sem autenticação e autorização.
- Melhorias futuras devem incluir autenticação, controle de acesso, proteção de endpoints e revisão de CORS.
- O banco de dados deve possuir backup.
- Variáveis sensíveis devem permanecer fora do repositório.
- Chaves de serviços externos não devem ser expostas no frontend.
- Logs não devem registrar conteúdo completo de anotações, descrições ou dados sensíveis.

