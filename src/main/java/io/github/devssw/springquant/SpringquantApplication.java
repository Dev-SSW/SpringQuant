package io.github.devssw.springquant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringquantApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringquantApplication.class, args);
	}
}
