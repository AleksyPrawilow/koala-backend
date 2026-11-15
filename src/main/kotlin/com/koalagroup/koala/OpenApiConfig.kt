package com.koalagroup.koala

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun koalaOpenApi(): OpenAPI {
        return OpenAPI().info(
            Info()
                .title("KOALA API")
                .version("0.0.1")
                .description(
                    "Backend of KOALA, a web application for organising the KOALA team contest: " +
                        "accounts and roles, administration of users and the audit log."
                )
        )
    }
}
