package org.flowable.ex.shell.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flowable.ex.shell.utils.JsonNodeResultHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.ResultHandler;

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

    @Bean
    ResultHandler<JsonNode> jsonNodeResultHandler() {
        return new JsonNodeResultHandler();
    }
}
