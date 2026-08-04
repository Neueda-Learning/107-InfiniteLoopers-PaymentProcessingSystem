package com.payment.payment_processing_system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration for the Payment Processing System.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentProcessingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Processing System API")
                        .version("1.0")
                        .description("This API manages customers, payments, transactions, and support operations "
                                + "for the Payment Processing System.")
                        .contact(new Contact()
                                .name("Payment Processing Support Team")
                                .email("support@paymentprocessing.com")
                                .url("https://paymentprocessing.com/support"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}

