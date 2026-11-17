package com.example.zolieapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer";

    private static final String DESCRIPTION = """
            Social feed, Reels, 18+ dating and messaging in one account.

            **How to test here**

            1. Click **Authorize** and enter a token. In development use `dev:<name>`, for example `dev:dara`. \
            Add `:admin` (`dev:dara:admin`) for the admin endpoints. In production send a Firebase ID token.
            2. Call `POST /me/sync` once to create the account for that token.
            3. Use a second browser or a second token (`dev:sophea`) to test follows, chats and dating between two people.

            Lists use cursor paging: pass the `nextCursor` of one page as `cursor` to get the next page.
            """;

    @Bean
    public OpenAPI zolieOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Zolie API").version("v1").description(DESCRIPTION))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("Firebase ID token")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
