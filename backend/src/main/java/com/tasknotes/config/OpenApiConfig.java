package com.tasknotes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskNotesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaskNotes API")
                        .description("API REST para gerenciamento de categorias, tarefas e anotações")
                        .version("v1.0.0"));
    }
}
