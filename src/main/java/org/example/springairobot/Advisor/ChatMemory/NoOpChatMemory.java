package org.example.springairobot.Advisor.ChatMemory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import java.util.Collections;
import java.util.List;

public class NoOpChatMemory implements ChatMemory {
    @Override
    public void add(String conversationId, Message message) {
        // 不执行任何操作
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        // 不执行任何操作
    }

    @Override
    public List<Message> get(String conversationId) {
        return Collections.emptyList();
    }

    @Override
    public void clear(String conversationId) {
        // 不执行任何操作
    }
}