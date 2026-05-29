---
title: CORS
source_type: official
source_url: https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
trust_level: high
tags: [cors, cross-origin, same-origin, http, segurança, api, preflight, access-control]
---

## CORS — Cross-Origin Resource Sharing

CORS é um mecanismo HTTP que controla quais origens externas podem acessar recursos de um servidor.

### O que é uma "origem"

Uma origem é composta por: **protocolo + domínio + porta**.

```
http://localhost:4200  ← origem do Angular
http://localhost:8080  ← origem do Spring Boot (diferente!)
```

Duas URLs com origens diferentes formam um **cross-origin request**.

### Same-Origin Policy

Por padrão, navegadores bloqueiam requisições para origens diferentes (Same-Origin Policy). CORS é o mecanismo para relaxar essa política de forma controlada.

### Como funciona

O servidor responde com headers especiais:

```http
Access-Control-Allow-Origin: http://localhost:4200
Access-Control-Allow-Methods: GET, POST, PUT, DELETE
Access-Control-Allow-Headers: Content-Type, Authorization
```

### Preflight Request

Para métodos não simples (POST com JSON, PUT, DELETE), o navegador envia uma requisição **OPTIONS** antes — o preflight. O servidor deve responder com os headers CORS corretos.

### Configuração no Spring Boot

```java
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:4200")
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowCredentials(true);
            }
        };
    }
}
```

### Erros comuns

- `Access-Control-Allow-Origin: *` não funciona com `allowCredentials(true)`
- Esquecer de incluir cabeçalhos customizados em `allowedHeaders`
- Configurar CORS no Spring Security mas não no WebMvc (ou vice-versa)
