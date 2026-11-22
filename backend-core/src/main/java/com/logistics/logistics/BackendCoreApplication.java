package com.logistics.logistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendCoreApplication.class, args);
	}

}
