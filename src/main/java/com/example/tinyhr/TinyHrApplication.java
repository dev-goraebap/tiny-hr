package com.example.tinyhr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TinyHrApplication {

	public static void main(String[] args) {
		SpringApplication.run(TinyHrApplication.class, args);
	}

}
