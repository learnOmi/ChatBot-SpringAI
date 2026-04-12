package org.example.springairobot;

import org.example.springairobot.service.IngestionService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringAiRobotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiRobotApplication.class, args);
    }


    // ApplicationRunner 的作用：在 Spring 容器完全初始化后执行，此时事务管理器已就绪，@Transactional 注解生效
    @Bean
    public ApplicationRunner ingestionRunner(IngestionService ingestionService) {
        return args -> ingestionService.runIngestion();
    }

}
