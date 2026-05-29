---
title: HTTP
source_type: official
source_url: https://developer.mozilla.org/en-US/docs/Web/HTTP
trust_level: high
tags: [http, https, protocolo, status code, verbos, rest, requisição, resposta, headers]
---

## HTTP — HyperText Transfer Protocol

HTTP é o protocolo de comunicação base da web, definindo como clientes e servidores trocam dados.

### Verbos HTTP

| Verbo | Uso semântico |
|-------|---------------|
| GET | Buscar recurso (sem efeitos colaterais) |
| POST | Criar novo recurso |
| PUT | Atualizar recurso completo |
| PATCH | Atualizar recurso parcialmente |
| DELETE | Remover recurso |
| OPTIONS | Verificar opções disponíveis (preflight CORS) |

### Códigos de status

#### 2xx — Sucesso
- `200 OK` — requisição bem-sucedida
- `201 Created` — recurso criado
- `204 No Content` — sucesso sem corpo de resposta

#### 3xx — Redirecionamento
- `301 Moved Permanently` — URL permanentemente movida
- `302 Found` — redirecionamento temporário

#### 4xx — Erro do cliente
- `400 Bad Request` — dados inválidos enviados
- `401 Unauthorized` — não autenticado
- `403 Forbidden` — autenticado mas sem permissão
- `404 Not Found` — recurso não encontrado
- `409 Conflict` — conflito (ex: duplicado)
- `422 Unprocessable Entity` — erro de validação

#### 5xx — Erro do servidor
- `500 Internal Server Error` — erro interno
- `502 Bad Gateway` — erro de proxy/gateway
- `503 Service Unavailable` — serviço indisponível

### Headers importantes

```http
Content-Type: application/json
Authorization: Bearer <jwt-token>
Accept: application/json
Cache-Control: no-cache
```

### HTTPS

HTTPS = HTTP + TLS. Todo o tráfego é criptografado. Em produção, HTTPS é obrigatório para qualquer dado sensível.

### HTTP/2 e HTTP/3

HTTP/2 introduz multiplexação (múltiplas requisições por conexão). HTTP/3 usa QUIC (UDP) para menor latência.
