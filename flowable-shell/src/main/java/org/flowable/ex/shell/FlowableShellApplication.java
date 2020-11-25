package org.flowable.ex.shell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FlowableShellApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlowableShellApplication.class, args);
	}

	@Bean
	Configuration configuration() {
		Configuration configuration = new Configuration();
		configuration.setLogin("admin");
		configuration.setPassword("test");
		configuration.setRestURL("http://localhost:8080/flowable-ui/app-api/");
		return configuration;
	}

}
