package com.newssearch;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CnnArticlesSearchEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(CnnArticlesSearchEngineApplication.class, args);
		
	}

	@Bean
    public CommandLineRunner run(IndexInitializer indexInitializer) {
        return args -> {
            indexInitializer.init();  
        };
    }
    
}
