package org.flowable.ex.shell.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowableShellConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    org.flowable.ex.shell.utils.Configuration configuration() {
        org.flowable.ex.shell.utils.Configuration configuration = new org.flowable.ex.shell.utils.Configuration();
        configuration.setLogin("admin");
        configuration.setPassword("test");
        configuration.setRestURL("http://localhost:8080/flowable-ui/");
        return configuration;
    }
}
