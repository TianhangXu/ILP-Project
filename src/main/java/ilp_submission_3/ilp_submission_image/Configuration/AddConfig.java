package ilp_submission_3.ilp_submission_image.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AddConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

