package dreamdays.Helf.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI helfOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Helf API")
                        .description("hellofriend_BE API 문서")
                        .version("v0.0.1"));
    }
}
