package com.ucc.chatbot;

import com.ucc.chatbot.config.SchemaReset;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UccChatbotApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(UccChatbotApplication.class);
        app.addListeners(new SchemaReset());
        app.run(args);
    }
}
