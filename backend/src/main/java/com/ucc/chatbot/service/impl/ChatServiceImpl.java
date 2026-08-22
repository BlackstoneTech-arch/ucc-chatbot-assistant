package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.dto.ChatRequest;
import com.ucc.chatbot.dto.ChatResponse;
import com.ucc.chatbot.dto.QueryUnderstandingResult;
import com.ucc.chatbot.model.Conversation;
import com.ucc.chatbot.model.Message;
import com.ucc.chatbot.model.KnowledgeDocument;
import com.ucc.chatbot.repository.ConversationRepository;
import com.ucc.chatbot.repository.KnowledgeDocumentRepository;
import com.ucc.chatbot.service.AIService;
import com.ucc.chatbot.service.ConversationService;
import com.ucc.chatbot.service.QueryUnderstandingService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements com.ucc.chatbot.service.ChatService {

    private final AIService aiService;
    private final ConversationService conversationService;
    private final KnowledgeDocumentRepository knowledgeRepository;
    private final QueryUnderstandingService queryUnderstandingService;

    public ChatServiceImpl(AIService aiService, ConversationService conversationService, KnowledgeDocumentRepository knowledgeRepository, QueryUnderstandingService queryUnderstandingService) {
        this.aiService = aiService;
        this.conversationService = conversationService;
        this.knowledgeRepository = knowledgeRepository;
        this.queryUnderstandingService = queryUnderstandingService;
    }

    @Override
    public ChatResponse processMessage(ChatRequest request) {
        String message = request.getMessage();
        if (message == null || message.isBlank()) {
            return ChatResponse.builder()
                    .answer("Please enter a message.")
                    .language(request.getLanguage() != null ? request.getLanguage() : "en")
                    .conversationId(request.getConversationId())
                    .confidence(0.0)
                    .escalationRequired(false)
                    .build();
        }

        Conversation conversation = conversationService.getOrCreateConversation(
                request.getConversationId(),
                null
        ).orElse(null);

        if (conversation != null) {
            conversationService.saveMessage(conversation, "user", message);
        }

        String conversationContext = buildConversationContext(conversation);
        QueryUnderstandingResult understanding = queryUnderstandingService.understand(message, conversationContext);

        if (conversation != null) {
            conversationService.updateContext(
                    conversation.getId(),
                    understanding.getEntities() != null ? understanding.getEntities().getProgramme() : null,
                    understanding.getEntities() != null ? understanding.getEntities().getConcept() : null,
                    understanding.getIntent()
            );
        }

        String context = buildRetrievalContext(understanding, conversation);

        ChatResponse response;
        if (!understanding.isRequiresRetrieval()) {
            response = buildDirectResponse(understanding, request.getConversationId());
        } else {
            response = aiService.generateResponse(request, context);
            response.setLanguage(understanding.getResponseLanguage());
            response.setConversationId(request.getConversationId());
        }

        if (conversation != null && response.getAnswer() != null) {
            conversationService.saveMessage(conversation, "assistant", response.getAnswer());
        }

        return response;
    }

    private ChatResponse buildDirectResponse(QueryUnderstandingResult understanding, String conversationId) {
        String lang = understanding.getResponseLanguage();
        String intent = understanding.getIntent();

        String answer = switch (intent) {
            case "GREETING" -> "sw".equals(lang)
                    ? "Habari! Mimi ni UCC Chatbot Assistant. Naweza kukusaidia kupata taarifa kuhusu programu, maombi, ada, usajili, huduma za wanafunzi, huduma za ICT, na huduma nyingine za UCC. Niko hapa kukusaidia leo?"
                    : "Hello! I'm UCC Chatbot Assistant. I can help you find information about programmes, admissions, fees, registration, student services, ICT services, and other UCC services. How can I help you today?";
            case "FAREWELL" -> "sw".equals(lang)
                    ? "Kwaheri! Nakutakia kila la heri. Usisite kuzungumza nasi tena ukipo na maswali kuhusu UCC."
                    : "Goodbye! Wishing you all the best. Feel free to chat with us again if you have any questions about UCC.";
            case "THANK_YOU" -> "sw".equals(lang)
                    ? "Karibu sana! Niko hapa kukusaidia kila wakati. Kama una maswali mengine, usisite kuniuliza."
                    : "You're welcome! I'm here to help anytime. If you have more questions, feel free to ask.";
            case "HELP_REQUEST" -> "sw".equals(lang)
                    ? "Niko hapa kukusaidia! Unaweza kuniuliza kuhusu programu za UCC, maombi, ada, vigezo vya kujiunga, usajili, mawasiliano, na huduma nyingine za UCC. Jaribu kuniuliza chochote kuhusu UCC."
                    : "I'm here to help! You can ask me about UCC programmes, admissions, fees, entry requirements, registration, contacts, and other UCC services. Feel free to ask anything about UCC.";
            default -> "sw".equals(lang)
                    ? "Samahani, sikuweza kuthibitisha taarifa hii kutoka kwenye taarifa rasmi zilizopo kwenye mfumo wa UCC."
                    : "I could not verify this information from the currently approved UCC knowledge base.";
        };

        return ChatResponse.builder()
                .answer(answer)
                .language(lang)
                .conversationId(conversationId)
                .sources(List.of())
                .confidence(1.0)
                .escalationRequired(false)
                .build();
    }

    private String buildConversationContext(Conversation conversation) {
        if (conversation == null) return null;
        StringBuilder ctx = new StringBuilder();
        if (conversation.getLastProgramme() != null) {
            ctx.append("Active programme: ").append(conversation.getLastProgramme()).append(". ");
        }
        if (conversation.getLastConcept() != null) {
            ctx.append("Last concept: ").append(conversation.getLastConcept()).append(". ");
        }
        if (conversation.getLastIntent() != null) {
            ctx.append("Last intent: ").append(conversation.getLastIntent()).append(". ");
        }
        return ctx.toString();
    }

    private String buildRetrievalContext(QueryUnderstandingResult understanding, Conversation conversation) {
        StringBuilder context = new StringBuilder();

        if (understanding.getCanonicalQuery() != null) {
            context.append("Canonical Query: ").append(understanding.getCanonicalQuery()).append("\n\n");
        }

        if (understanding.getRetrievalQueries() != null && !understanding.getRetrievalQueries().isEmpty()) {
            context.append("Retrieval Queries:\n");
            for (String query : understanding.getRetrievalQueries()) {
                context.append("- ").append(query).append("\n");
            }
            context.append("\n");
        }

        if (understanding.getEntities() != null && understanding.getEntities().getProgramme() != null) {
            context.append("Active Programme: ").append(understanding.getEntities().getProgramme()).append("\n\n");
        }

        List<KnowledgeDocument> documents = knowledgeRepository.findByIsActiveTrue();
        String queryText = understanding.getCanonicalQuery() != null ? understanding.getCanonicalQuery().toLowerCase() : "";
        for (KnowledgeDocument doc : documents) {
            if (doc.getContent() != null && !doc.getContent().isBlank()) {
                String title = doc.getTitle() != null ? doc.getTitle().toLowerCase() : "";
                String category = doc.getCategory() != null ? doc.getCategory().toLowerCase() : "";
                if (queryText.contains(title) || queryText.contains(category) ||
                    (understanding.getEntities() != null && understanding.getEntities().getProgramme() != null &&
                     title.contains(understanding.getEntities().getProgramme().toLowerCase()))) {
                    context.append(doc.getTitle()).append(": ").append(doc.getContent()).append("\n\n");
                }
            }
        }

        return context.toString();
    }
}
