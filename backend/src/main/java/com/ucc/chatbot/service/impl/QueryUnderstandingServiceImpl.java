package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.dto.QueryUnderstandingResult;
import com.ucc.chatbot.model.Conversation;
import com.ucc.chatbot.repository.ConversationRepository;
import com.ucc.chatbot.service.ConversationService;
import com.ucc.chatbot.service.QueryUnderstandingService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QueryUnderstandingServiceImpl implements QueryUnderstandingService {

    private final ConversationRepository conversationRepository;

    private static final Set<String> PROGRAMME_CODES = Set.of("DBIT", "DCIT", "CCIT", "CBIT");
    private static final Map<String, String> PROGRAMME_FULL_NAMES;
    static {
        Map<String, String> m = new java.util.HashMap<>();
        m.put("DBIT", "Diploma in Business Information Technology");
        m.put("DCIT", "Diploma in Computing and Information Technology");
        m.put("CCIT", "Certificate in Computing and Information Technology");
        m.put("CBIT", "Certificate in Business Information Technology");
        PROGRAMME_FULL_NAMES = java.util.Collections.unmodifiableMap(m);
    }

    private static final Map<String, String> CONCEPT_SYNONYMS_EN;
    static {
        Map<String, String> m = new java.util.HashMap<>();
        m.put("fee", "FEES"); m.put("fees", "FEES"); m.put("cost", "FEES"); m.put("price", "FEES");
        m.put("payment", "FEES"); m.put("tuition", "FEES"); m.put("amount to pay", "FEES");
        m.put("admission", "ADMISSION"); m.put("apply", "ADMISSION"); m.put("application", "ADMISSION");
        m.put("apply for course", "ADMISSION");
        m.put("requirement", "ENTRY_REQUIREMENTS"); m.put("requirements", "ENTRY_REQUIREMENTS");
        m.put("qualification", "ENTRY_REQUIREMENTS"); m.put("qualifications", "ENTRY_REQUIREMENTS");
        m.put("qualifications needed", "ENTRY_REQUIREMENTS"); m.put("entry criteria", "ENTRY_REQUIREMENTS");
        m.put("duration", "PROGRAMME_DURATION"); m.put("how long", "PROGRAMME_DURATION");
        m.put("years", "PROGRAMME_DURATION"); m.put("months", "PROGRAMME_DURATION");
        m.put("takes how long", "PROGRAMME_DURATION");
        m.put("contact", "CONTACT_INFORMATION"); m.put("phone number", "CONTACT_INFORMATION");
        m.put("telephone", "CONTACT_INFORMATION"); m.put("email", "CONTACT_INFORMATION");
        m.put("reach you", "CONTACT_INFORMATION");
        m.put("location", "LOCATION"); m.put("address", "LOCATION"); m.put("where", "LOCATION");
        m.put("where are you", "LOCATION"); m.put("direction", "LOCATION");
        CONCEPT_SYNONYMS_EN = java.util.Collections.unmodifiableMap(m);
    }

    private static final Map<String, String> CONCEPT_SYNONYMS_SW;
    static {
        Map<String, String> m = new java.util.HashMap<>();
        m.put("ada", "FEES"); m.put("gharama", "FEES"); m.put("bei", "FEES"); m.put("malipo", "FEES");
        m.put("kiasi cha kulipa", "FEES");
        m.put("kuomba", "ADMISSION"); m.put("maombi", "ADMISSION"); m.put("udahili", "ADMISSION");
        m.put("kujiunga", "ADMISSION"); m.put("kutuma maombi", "ADMISSION");
        m.put("vigezo", "ENTRY_REQUIREMENTS"); m.put("sifa", "ENTRY_REQUIREMENTS");
        m.put("masharti", "ENTRY_REQUIREMENTS"); m.put("qualifications za kujiunga", "ENTRY_REQUIREMENTS");
        m.put("muda", "PROGRAMME_DURATION"); m.put("inachukua muda gani", "PROGRAMME_DURATION");
        m.put("miaka mingapi", "PROGRAMME_DURATION"); m.put("muda wa kozi", "PROGRAMME_DURATION");
        m.put("mawasiliano", "CONTACT_INFORMATION"); m.put("namba", "CONTACT_INFORMATION");
        m.put("simu", "CONTACT_INFORMATION"); m.put("email ya ucc", "CONTACT_INFORMATION");
        m.put("nitawasiliana vipi", "CONTACT_INFORMATION");
        m.put("mahali", "LOCATION"); m.put("anwani", "LOCATION"); m.put("mko wapi", "LOCATION");
        m.put("ofisi zipo wapi", "LOCATION");
        CONCEPT_SYNONYMS_SW = java.util.Collections.unmodifiableMap(m);
    }

    private static final Set<String> GREETING_EN = Set.of(
            "hi", "hello", "hey", "good morning", "good afternoon", "good evening",
            "how are you", "howdy", "morning", "afternoon", "evening"
    );

    private static final Set<String> GREETING_SW = Set.of(
            "habari", "mambo", "vipi", "niaje", "hujambo", "shikamoo", "salama",
            "za kwako", "mambo vipi", "mambo bro", "mambo boss", "habari yako",
            "habari za asubuhi", "habari za mchana", "habari za jioni"
    );

    private static final Set<String> FAREWELL_EN = Set.of(
            "bye", "goodbye", "see you", "see ya", "later", "take care"
    );

    private static final Set<String> FAREWELL_SW = Set.of(
            "kwaheri", "kwa heri", "tutaonana", "baadaye", "tutaonana baadaye",
            "nisaidie kwaheri"
    );

    private static final Set<String> THANK_YOU_EN = Set.of(
            "thanks", "thank you", "thx", "thanks a lot", "thank you very much"
    );

    private static final Set<String> THANK_YOU_SW = Set.of(
            "asante", "asante sana", "shukrani", "nashukuru", "asante kwa msaada",
            "shukrani kwa msaada"
    );

    private static final Set<String> HELP_EN = Set.of(
            "help", "can you help me", "i need help", "assist me", "i need assistance"
    );

    private static final Set<String> HELP_SW = Set.of(
            "msaada", "nahitaji msaada", "naomba msaada", "nawezaje kupata msaada",
            "ninaomba msaada", "ninahitaji msaada"
    );

    private static final Map<String, String> INFORMAL_SW_NORMALIZATIONS;
    static {
        Map<String, String> m = new java.util.HashMap<>();
        m.put("ada ngap", "ada ngapi");
        m.put("kozi ipo", "kozi zipo");
        m.put("nataka kujoin", "nataka kujiunga");
        m.put("vigezo vya kujoin", "vigezo vya kujiunga");
        m.put("ina take miaka mingapi", "inachukua miaka mingapi");
        m.put("nipo interested na", "nina maswali kuhusu");
        m.put("db it bei gan", "db it ada ngapi");
        m.put("naapply aje", "naomba kuapply vipi");
        m.put("nina apply", "ninaomba");
        m.put("join", "kujiunga");
        m.put("apply", "kuomba");
        INFORMAL_SW_NORMALIZATIONS = java.util.Collections.unmodifiableMap(m);
    }

    private static final Map<String, String> TYPO_CORRECTIONS_EN;
    static {
        Map<String, String> m = new java.util.HashMap<>();
        m.put("admision", "admission");
        m.put("reqirement", "requirement");
        m.put("reqirements", "requirements");
        m.put("progam", "programme");
        m.put("prog", "programme");
        m.put("info", "information");
        m.put("info rm", "information");
        TYPO_CORRECTIONS_EN = java.util.Collections.unmodifiableMap(m);
    }

    private static final Map<String, String> TYPO_CORRECTIONS_SW;
    static {
        Map<String, String> m = new java.util.HashMap<>();
        m.put("vigezoo", "vigezo");
        m.put("maombii", "maombi");
        m.put("adha", "ada");
        m.put("adhaa", "ada");
        m.put("msaad", "msaada");
        m.put("wasilina", "wasiliana");
        TYPO_CORRECTIONS_SW = java.util.Collections.unmodifiableMap(m);
    }

    public QueryUnderstandingServiceImpl(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Override
    public QueryUnderstandingResult understand(String userMessage, String conversationContext) {
        QueryUnderstandingResult result = new QueryUnderstandingResult();
        result.setOriginalMessage(userMessage);

        String normalized = normalizeText(userMessage);
        String detectedLang = detectLanguage(normalized);
        String normalizedLang = normalizeLanguage(detectedLang, normalized);

        result.setDetectedLanguage(detectedLang);
        result.setNormalizedLanguage(normalizedLang);
        result.setResponseLanguage(detectedLang);

        if (isGreeting(normalized, detectedLang)) {
            result.setIntent("GREETING");
            result.setCanonicalQuery(null);
            result.setRetrievalQueries(List.of());
            result.setRequiresRetrieval(false);
            result.setRequiresClarification(false);
            result.setVerifiedInformationFound(true);
            return result;
        }

        if (isFarewell(normalized, detectedLang)) {
            result.setIntent("FAREWELL");
            result.setCanonicalQuery(null);
            result.setRetrievalQueries(List.of());
            result.setRequiresRetrieval(false);
            result.setRequiresClarification(false);
            result.setVerifiedInformationFound(true);
            return result;
        }

        if (isThankYou(normalized, detectedLang)) {
            result.setIntent("THANK_YOU");
            result.setCanonicalQuery(null);
            result.setRetrievalQueries(List.of());
            result.setRequiresRetrieval(false);
            result.setRequiresClarification(false);
            result.setVerifiedInformationFound(true);
            return result;
        }

        if (isHelpRequest(normalized, detectedLang)) {
            result.setIntent("HELP_REQUEST");
            result.setCanonicalQuery(null);
            result.setRetrievalQueries(List.of());
            result.setRequiresRetrieval(false);
            result.setRequiresClarification(false);
            result.setVerifiedInformationFound(true);
            return result;
        }

        QueryUnderstandingResult.Entities entities = extractEntities(normalized, conversationContext);
        List<String> concepts = extractConcepts(normalized, detectedLang);
        String intent = classifyIntent(entities, concepts, normalizedLang);

        if (concepts.isEmpty() && entities.getProgramme() != null) {
            concepts = List.of("PROGRAMME_INFO");
            intent = "PROGRAMME_INFO_QUERY";
        }

        if (intent == null || intent.equals("UNKNOWN")) {
            if (entities.getProgramme() == null && (concepts.isEmpty() || concepts.contains("PROGRAMME_INFO"))) {
                result.setRequiresClarification(true);
                result.setIntent("PROGRAMME_INFO_QUERY");
                result.setEntities(entities);
                result.setConcepts(concepts);
                result.setCanonicalQuery(null);
                result.setRetrievalQueries(List.of());
                result.setRequiresRetrieval(false);
                result.setVerifiedInformationFound(true);
                return result;
            }
            intent = "GENERAL_QUERY";
        }

        String canonicalQuery = generateCanonicalQuery(intent, entities, concepts, normalizedLang);
        List<String> retrievalQueries = expandQueries(intent, entities, concepts, normalizedLang);

        boolean requiresRetrieval = !List.of("GREETING", "FAREWELL", "THANK_YOU", "HELP_REQUEST").contains(intent);

        result.setIntent(intent);
        result.setEntities(entities);
        result.setConcepts(concepts);
        result.setCanonicalQuery(canonicalQuery);
        result.setRetrievalQueries(retrievalQueries);
        result.setRequiresRetrieval(requiresRetrieval);
        result.setRequiresClarification(false);
        result.setVerifiedInformationFound(true);

        return result;
    }

    private String normalizeText(String text) {
        String normalized = text.toLowerCase().trim();

        for (Map.Entry<String, String> entry : INFORMAL_SW_NORMALIZATIONS.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, String> entry : TYPO_CORRECTIONS_EN.entrySet()) {
            normalized = normalized.replaceAll("\\b" + java.util.regex.Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
        }
        for (Map.Entry<String, String> entry : TYPO_CORRECTIONS_SW.entrySet()) {
            normalized = normalized.replaceAll("\\b" + java.util.regex.Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
        }

        return normalized;
    }

    private String detectLanguage(String text) {
        String lower = text.toLowerCase();

        if (lower.contains("habari") || lower.contains("hujambo") || lower.contains("salamu") ||
            lower.contains("mambo") || lower.contains("niaje") || lower.contains("shikamoo") ||
            lower.contains("vipi") || lower.contains("za kwako") || lower.contains("asante") ||
            lower.contains("shukrani") || lower.contains("kwaheri") || lower.contains("msaada") ||
            lower.contains("ada") || lower.contains("kozi") || lower.contains("vigezo") ||
            lower.contains("maombi") || lower.contains("kujiunga") || lower.contains("miaka") ||
            lower.contains("muda") || lower.contains("anwani") || lower.contains("mahali")) {
            return "sw";
        }

        if (lower.contains("hello") || lower.startsWith("hi ") || lower.equals("hi") ||
            lower.contains("good morning") || lower.contains("good afternoon") ||
            lower.contains("good evening") || lower.contains("programme") ||
            lower.contains("admission") || lower.contains("application") ||
            lower.contains("requirement") || lower.contains("duration") ||
            lower.contains("contact") || lower.contains("location") ||
            lower.contains("address") || lower.contains("fee") || lower.contains("fees")) {
            return "en";
        }

        Set<String> swahiliIndicators = Set.of(
                "nina", "ni", "na", "wa", "kwa", "ya", "za", "vya", "kozi", "omba", "sasa",
                "hii", "hilo", "hizi", "hayo", "kweli", "labda", "kama", "au", "kabla", "baada",
                "mimi", "wewe", "sisi", "ninyi", "huyu", "huyo", "hawa", "ndani", "nje", "karibu",
                "habari", "hapo", "huku", "kule", "chini", "juu", "mbele", "nyuma", "mbali", "moja",
                "mbili", "nini", "kazi", "ali", "vyo", "si", "hadi", "kati", "pia"
        );

        long swahiliCount = swahiliIndicators.stream()
                .filter(lower::contains)
                .count();

        return swahiliCount >= 1 ? "sw" : "en";
    }

    private String normalizeLanguage(String detectedLang, String text) {
        if (detectedLang.equals("mixed")) {
            return "en";
        }
        return detectedLang;
    }

    private boolean isGreeting(String text, String lang) {
        if ("sw".equals(lang)) {
            for (String g : GREETING_SW) {
                if (text.contains(g)) return true;
            }
        }
        for (String g : GREETING_EN) {
            if (text.contains(g)) return true;
        }
        return false;
    }

    private boolean isFarewell(String text, String lang) {
        if ("sw".equals(lang)) {
            for (String f : FAREWELL_SW) {
                if (text.contains(f)) return true;
            }
        }
        for (String f : FAREWELL_EN) {
            if (text.contains(f)) return true;
        }
        return false;
    }

    private boolean isThankYou(String text, String lang) {
        if ("sw".equals(lang)) {
            for (String t : THANK_YOU_SW) {
                if (text.contains(t)) return true;
            }
        }
        for (String t : THANK_YOU_EN) {
            if (text.contains(t)) return true;
        }
        return false;
    }

    private boolean isHelpRequest(String text, String lang) {
        if ("sw".equals(lang)) {
            for (String h : HELP_SW) {
                if (text.contains(h)) return true;
            }
        }
        for (String h : HELP_EN) {
            if (text.contains(h)) return true;
        }
        return false;
    }

    private QueryUnderstandingResult.Entities extractEntities(String text, String conversationContext) {
        QueryUnderstandingResult.Entities entities = new QueryUnderstandingResult.Entities();

        for (String code : PROGRAMME_CODES) {
            if (text.contains(code.toLowerCase())) {
                entities.setProgramme(code);
                return entities;
            }
        }

        if (text.contains("dbit")) entities.setProgramme("DBIT");
        else if (text.contains("dcit")) entities.setProgramme("DCIT");
        else if (text.contains("ccit")) entities.setProgramme("CCIT");
        else if (text.contains("cbit")) entities.setProgramme("CBIT");

        if (entities.getProgramme() == null && conversationContext != null) {
            for (String code : PROGRAMME_CODES) {
                if (conversationContext.contains(code)) {
                    entities.setProgramme(code);
                    break;
                }
            }
        }

        return entities;
    }

    private List<String> extractConcepts(String text, String lang) {
        List<String> concepts = new ArrayList<>();
        Map<String, String> synonymMap = "sw".equals(lang) ? CONCEPT_SYNONYMS_SW : CONCEPT_SYNONYMS_EN;
        synonymMap.forEach((key, concept) -> {
            if (text.contains(key) && !concepts.contains(concept)) {
                concepts.add(concept);
            }
        });
        return concepts;
    }

    private String classifyIntent(QueryUnderstandingResult.Entities entities, List<String> concepts, String normalizedLang) {
        if (entities.getProgramme() != null) {
            if (concepts.contains("FEES")) return "PROGRAMME_FEE_QUERY";
            if (concepts.contains("ADMISSION")) return "PROGRAMME_ADMISSION_QUERY";
            if (concepts.contains("ENTRY_REQUIREMENTS")) return "ENTRY_REQUIREMENTS_QUERY";
            if (concepts.contains("PROGRAMME_DURATION")) return "PROGRAMME_DURATION_QUERY";
            if (concepts.contains("CONTACT_INFORMATION")) return "CONTACT_QUERY";
            if (concepts.contains("LOCATION")) return "LOCATION_QUERY";
            if (concepts.contains("PROGRAMME_INFO") || concepts.isEmpty()) return "PROGRAMME_INFO_QUERY";
        }

        if (!concepts.isEmpty()) {
            if (concepts.contains("FEES")) return "PROGRAMME_FEE_QUERY";
            if (concepts.contains("ADMISSION")) return "PROGRAMME_ADMISSION_QUERY";
            if (concepts.contains("ENTRY_REQUIREMENTS")) return "ENTRY_REQUIREMENTS_QUERY";
            if (concepts.contains("PROGRAMME_DURATION")) return "PROGRAMME_DURATION_QUERY";
            if (concepts.contains("CONTACT_INFORMATION")) return "CONTACT_QUERY";
            if (concepts.contains("LOCATION")) return "LOCATION_QUERY";
            if (concepts.contains("PROGRAMME_INFO")) return "PROGRAMME_INFO_QUERY";
        }

        return "GENERAL_QUERY";
    }

    private String generateCanonicalQuery(String intent, QueryUnderstandingResult.Entities entities, List<String> concepts, String normalizedLang) {
        String programme = entities.getProgramme();
        String programmeFullName = programme != null ? PROGRAMME_FULL_NAMES.get(programme) : "";

        return switch (intent) {
            case "GREETING", "FAREWELL", "THANK_YOU", "HELP_REQUEST" -> null;
            case "PROGRAMME_FEE_QUERY" -> programme != null
                    ? "What are the official fees for the " + programmeFullName + " (" + programme + ") programme?"
                    : "What are the official fees for UCC programmes?";
            case "PROGRAMME_ADMISSION_QUERY" -> programme != null
                    ? "What are the admission requirements and application procedure for the " + programmeFullName + " (" + programme + ") programme?"
                    : "What are the admission requirements and application procedure for UCC programmes?";
            case "ENTRY_REQUIREMENTS_QUERY" -> programme != null
                    ? "What are the official entry requirements for the " + programmeFullName + " (" + programme + ") programme?"
                    : "What are the official entry requirements for UCC programmes?";
            case "PROGRAMME_DURATION_QUERY" -> programme != null
                    ? "What is the official duration of the " + programmeFullName + " (" + programme + ") programme?"
                    : "What is the official duration of UCC programmes?";
            case "CONTACT_QUERY" -> "What are the official contact details for UCC?";
            case "LOCATION_QUERY" -> "What are the official locations and addresses for UCC?";
            case "PROGRAMME_INFO_QUERY" -> programme != null
                    ? "What is the official information about the " + programmeFullName + " (" + programme + ") programme?"
                    : "What academic programmes does UCC offer?";
            default -> "What information can you provide about UCC programmes and services?";
        };
    }

    private List<String> expandQueries(String intent, QueryUnderstandingResult.Entities entities, List<String> concepts, String normalizedLang) {
        List<String> queries = new ArrayList<>();
        String programme = entities.getProgramme();
        String programmeFullName = programme != null ? PROGRAMME_FULL_NAMES.get(programme) : "UCC";

        if (programme != null) {
            queries.add(programme + " fees");
            queries.add(programmeFullName + " fees");
            queries.add(programme + " tuition fee");
            queries.add(programme + " total fee");
            queries.add(programme + " payment");
            queries.add(programme + " entry requirements");
            queries.add(programmeFullName + " entry requirements");
            queries.add(programme + " admission requirements");
            queries.add(programme + " qualifications");
            queries.add(programme + " duration");
            queries.add(programmeFullName + " duration");
            queries.add(programme + " how long");
            queries.add(programme + " years");
            queries.add(programme + " location");
            queries.add(programmeFullName + " location");
            queries.add(programme + " address");
            queries.add(programme + " contact");
            queries.add(programmeFullName + " contact");
            queries.add(programme + " information");
            queries.add(programmeFullName + " information");
            queries.add("UCC " + programme);
        } else {
            if (concepts.contains("FEES")) {
                queries.addAll(List.of("UCC fees", "UCC tuition fees", "UCC programme fees", "UCC fee structure"));
            }
            if (concepts.contains("ADMISSION")) {
                queries.addAll(List.of("UCC admission", "UCC application procedure", "UCC how to apply", "admission.ucc.co.tz"));
            }
            if (concepts.contains("ENTRY_REQUIREMENTS")) {
                queries.addAll(List.of("UCC entry requirements", "UCC admission requirements", "UCC qualifications needed"));
            }
            if (concepts.contains("PROGRAMME_DURATION")) {
                queries.addAll(List.of("UCC programme duration", "UCC how long", "UCC programme years"));
            }
            if (concepts.contains("CONTACT_INFORMATION")) {
                queries.addAll(List.of("UCC contact", "UCC phone number", "UCC email", "UCC telephone"));
            }
            if (concepts.contains("LOCATION")) {
                queries.addAll(List.of("UCC location", "UCC address", "UCC where is UCC", "UCC directions"));
            }
            if (concepts.contains("PROGRAMME_INFO") || concepts.isEmpty()) {
                queries.addAll(List.of("UCC programmes", "UCC academic programmes", "UCC courses", "UCC available programmes"));
            }
        }

        return queries.stream().distinct().toList();
    }
}
