package org.example.springairobot.Config;

import org.example.springairobot.Advisor.ConversationLoggingAdvisor;
import org.example.springairobot.service.ConversationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    @Qualifier("chatClient")
    public ChatClient chatClient(
            ChatClient.Builder builder,
            ConversationLoggingAdvisor loggingAdvisor,
            SimpleLoggerAdvisor loggerAdvisor
    ) {
        return builder
                .defaultAdvisors(loggingAdvisor, loggerAdvisor)
                .build();
    }

    @Bean
    @Qualifier("ragChatClient")
    public ChatClient ragChatClient(
            ChatClient.Builder builder,
            ConversationLoggingAdvisor loggingAdvisor,
            QuestionAnswerAdvisor ragAdvisor,
            SimpleLoggerAdvisor loggerAdvisor
    ) {
        return builder
                .defaultAdvisors(loggingAdvisor, ragAdvisor, loggerAdvisor)
                .build();
    }

    @Bean
    public ConversationLoggingAdvisor conversationLoggingAdvisor(ConversationService conversationService) {
        return new ConversationLoggingAdvisor(conversationService);
    }

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor(VectorStore vectorStore) {
        return new QuestionAnswerAdvisor(vectorStore);
    }

    @Bean
    public SimpleLoggerAdvisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }
}