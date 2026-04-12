package org.example.springairobot;

import org.example.springairobot.service.IngestionService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringAiRobotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiRobotApplication.class, args);
    }

    @Bean
    public ApplicationRunner ingestionRunner(IngestionService ingestionService) {
        return args -> ingestionService.runIngestion();
    }
}
