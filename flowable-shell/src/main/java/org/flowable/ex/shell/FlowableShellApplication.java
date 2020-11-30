package org.flowable.ex.shell;

import org.flowable.ex.shell.utils.Configuration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FlowableShellApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlowableShellApplication.class, args);
	}

}
