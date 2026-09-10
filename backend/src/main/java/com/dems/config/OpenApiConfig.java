package com.dems.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("DEMS - Digital Evidence Management System API")
                        .version("1.0.0")
                        .description("""
                                Secure Digital Document Management for Legal & Investigation.
                                
                                Provides enterprise-grade APIs for:
                                - Authentication & User Management (RBAC)
                                - Case Management
                                - Secure Document Management with integrity verification
                                - Evidence Registry & Chain of Custody
                                - Tamper-evident Audit Logs
                                - AI-powered OCR, Summarization, Semantic Search & Q&A
                                - Secure Document Sharing
                                - Investigation Timeline & Analytics
                                
                                Security: JWT Authentication with roles: ADMIN, INVESTIGATOR, LEGAL_OFFICER, VIEWER
                                """)
                        .contact(new Contact()
                                .name("DEMS Team")
                                .email("support@dems.local"))
                        .license(new License()
                                .name("Proprietary and Confidential")
                                .url("https://dems.local/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.dems.local")
                                .description("Production Server")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authorization header using the Bearer scheme. " +
                                                "Enter 'Bearer' [space] and then your token.")));
    }
}
