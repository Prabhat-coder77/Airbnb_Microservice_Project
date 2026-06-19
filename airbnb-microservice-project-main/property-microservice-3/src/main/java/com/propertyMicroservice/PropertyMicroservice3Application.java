package com.propertyMicroservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PropertyMicroservice3Application {

	public static void main(String[] args) {
		SpringApplication.run(PropertyMicroservice3Application.class, args);
	}

}
