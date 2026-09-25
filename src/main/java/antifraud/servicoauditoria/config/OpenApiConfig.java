package antifraud.servicoauditoria.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Serviço de Auditoria - Antifraude")
                        .description("API somente leitura para consulta do histórico de análises antifraude.")
                        .version("v1")
                        .contact(new Contact().name("Pedro Luka")));
    }
}
