package com.vinay.gradingsystem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gradingSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Online Assignment Submission and Grading API")
                        .description("APIs for assignments, submissions, grading, analytics, and admin management.")
                        .version("v1.0")
                        .contact(new Contact().name("Grading System")));
    }
}
