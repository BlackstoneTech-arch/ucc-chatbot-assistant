package com.ucc.chatbot.service.impl;

import com.ucc.chatbot.dto.ChatRequest;
import com.ucc.chatbot.dto.ChatResponse;
import com.ucc.chatbot.model.KnowledgeDocument;
import com.ucc.chatbot.repository.KnowledgeDocumentRepository;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIServiceImpl implements com.ucc.chatbot.service.AIService {

    private final KnowledgeDocumentRepository knowledgeRepository;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.api.url:https://api.openai.com/v1}")
    private String aiApiUrl;

    @Value("${ai.model:gpt-4o-mini}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            You are the UCC Chatbot Assistant for the University of Dar es Salaam Computing Centre (UCC).
            
            IDENTITY:
            - Name: UCC Chatbot Assistant
            - Organization: University of Dar es Salaam Computing Centre (UCC)
            - Website: https://www.ucc.co.tz/
            
            ROLE:
            You are a professional university customer-care assistant. Provide accurate, helpful information about UCC.
            
            CRITICAL RULES:
            1. ONLY answer using approved UCC information provided in the context or static knowledge base.
            2. Do NOT claim that UCC offers BSc Computer Science, BSc Information Technology, PGDIT, or MSc IT unless explicitly verified in an approved current UCC source.
            3. If information is not in the provided context, say: "I couldn't find verified information about that. Please contact the relevant UCC office or visit https://www.ucc.co.tz/."
            4. ALWAYS cite sources when providing UCC-specific information.
            5. Answer clearly, professionally and concisely.
            6. Never claim access to private student records.
            """;

    private static final Map<String, List<String>> STATIC_KB_EN = new HashMap<>();
    private static final Map<String, List<String>> STATIC_KB_SW = new HashMap<>();

    static {
        STATIC_KB_EN.put("programme", Arrays.asList(
                "UCC offers the following verified academic programmes for 2026/2027: Diploma in Computing and Information Technology (DCIT), Diploma in Business Information Technology (DBIT), Certificate in Computing and Information Technology (CCIT), and Certificate in Business Information Technology (CBIT). UCC also lists professional and short courses such as CCNA, CCNP, PMP, CISA, CISM, ITIL, COBIT, Java Certified Programmer, and Microsoft Certified Solutions Developer. For the complete approved list, visit https://www.ucc.co.tz/ or contact admissions@ucc.co.tz."
        ));
        STATIC_KB_EN.put("dcit", Arrays.asList(
                "Diploma in Computing and Information Technology (DCIT): Duration 2 years (4 semesters). Entry requirements: (1) ACSEE with at least 1 principal pass and 1 subsidiary pass; OR (2) Basic Technician Certificate (NTA Level 4) in Computer Science, Information Technology, Business Information Technology, Computer Engineering, or Electronic Engineering. Fees for 2026/2027: Tuition TZS 2,800,000, Examination TZS 60,000, ID Card TZS 20,000, ICT Services TZS 100,000, NACTE Quality Assurance TZS 40,000, Total TZS 3,020,000. Apply at https://admission.ucc.co.tz/. Locations: UCC Headquarters at UDSM Mlimani Campus opposite NBC Bank, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji. Source: https://www.ucc.co.tz/course/diploma-in-computing-and-information-technology-dcit-81"
        ));
        STATIC_KB_EN.put("dbit", Arrays.asList(
                "Diploma in Business Information Technology (DBIT): Academic Year 2026/2027. Entry requirements: (1) ACSEE with at least 1 principal pass and 1 subsidiary pass; OR (2) Basic Technician Certificate (NTA Level 4) in Business Administration, Accountancy, Computer Science, Information Technology, Business Information Technology, or Computer Engineering. Fees for 2026/2027: Tuition TZS 2,800,000, Examination TZS 60,000, ID Card TZS 20,000, ICT Services TZS 100,000, NACTE Quality Assurance TZS 40,000, Total TZS 3,020,000. Apply at https://admission.ucc.co.tz/. Locations: UCC Headquarters at UDSM Mlimani Campus opposite NBC Bank, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji. Source: https://www.ucc.co.tz/course/diploma-in-business-information-technology-dbit-82"
        ));
        STATIC_KB_EN.put("ccit", Arrays.asList(
                "Certificate in Computing and Information Technology (CCIT): Academic Year 2026/2027. Entry requirements: (1) Certificate of Secondary Education with at least 4 passes in non-religious subjects; OR (2) National Vocational Training Award Level III (Trade Test Grade I) from a recognized institution. Fees for 2026/2027: Tuition TZS 1,200,000, Examination TZS 30,000, ID Card TZS 20,000, ICT Services TZS 100,000, NACTE Quality Assurance TZS 20,000, Total TZS 1,370,000. Apply at https://admission.ucc.co.tz/. Locations: UCC Headquarters at UDSM Mlimani Campus opposite NBC Bank, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji. Source: https://www.ucc.co.tz/course/certificate-in-computing-and-information-technology-ccit-172"
        ));
        STATIC_KB_EN.put("cbit", Arrays.asList(
                "Certificate in Business Information Technology (CBIT): UCC currently lists CBIT among its academic programmes for 2026/2027. Detailed entry requirements, fees, duration, and other information must be taken from the latest approved UCC CBIT brochure or official course page. Apply at https://admission.ucc.co.tz/. Source: https://www.ucc.co.tz/course/academic"
        ));
        STATIC_KB_EN.put("apply", Arrays.asList(
                "To apply to UCC: 1. Visit https://admission.ucc.co.tz/ 2. Create an account 3. Select your preferred programme 4. Complete the application form 5. Upload required documents 6. Pay the application fee 7. Submit. Contact admissions@ucc.co.tz / +255 22 2410 002."
        ));
        STATIC_KB_EN.put("fee", Arrays.asList(
                "Tuition fees vary by programme, academic year, and intake. For current fee structure, contact the Finance office at finance@ucc.co.tz or +255 22 2410 004, or visit https://www.ucc.co.tz/."
        ));
        STATIC_KB_EN.put("contact", Arrays.asList(
                "UCC contacts: Main Office: info@ucc.co.tz / +255 22 2410 000. Admissions: admissions@ucc.co.tz / +255 22 2410 002. ICT Support: ict@ucc.co.tz / +255 22 2410 003. Finance: finance@ucc.co.tz / +255 22 2410 004. Student Services: students@ucc.co.tz / +255 22 2410 005. Physical address: P.O. Box 35091, Dar es Salaam, Tanzania."
        ));
        STATIC_KB_EN.put("ict", Arrays.asList(
                "For ICT support: email ict@ucc.co.tz or call +255 22 2410 003. UCC provides computer lab access, internet, email accounts, LMS support, and software installation assistance."
        ));
        STATIC_KB_EN.put("registration", Arrays.asList(
                "To register for courses: 1. Log in to the UCC student portal 2. Navigate to the registration section 3. Select courses 4. Review selection 5. Confirm registration."
        ));
        STATIC_KB_EN.put("hello", Arrays.asList(
                "Hello! I'm UCC Chatbot Assistant. I can help you find information about programmes, admissions, fees, registration, student services, ICT services, and other UCC services. How can I help you today?"
        ));
        STATIC_KB_EN.put("ccna", Arrays.asList(
                "UCC lists professional and short courses including Cisco Certified Network Associate (CCNA). For current schedules, fees, and intake dates, please contact UCC directly or visit https://www.ucc.co.tz/."
        ));
        STATIC_KB_EN.put("professional", Arrays.asList(
                "UCC lists professional and short courses including: Project Management Professional (PMP), Certified Information Systems Auditor (CISA), Certified Information Security Manager (CISM), ITIL Foundation, ITIL Practitioner, COBIT Foundation, CCNA, CCNP, CCIP, Java Certified Programmer (JCP), Microsoft Certified Solutions Developer (MCSD), Business Processes Management, Enterprise Architecture for Managers, IT Governance, IT Service Management, Information Security and Risk Management, Ethical Hacking, and Mobile Application Development. Availability, schedules, fees, and intake dates may change. For the latest information, contact UCC or visit https://www.ucc.co.tz/."
        ));

        STATIC_KB_SW.put("programme", Arrays.asList(
                "UCC inatoa programu zifuatazo zilizothibitishwa kwa mwaka wa masomo 2026/2027: Diploma in Computing and Information Technology (DCIT), Diploma in Business Information Technology (DBIT), Certificate in Computing and Information Technology (CCIT), na Certificate in Business Information Technology (CBIT). UCC pia inaorodhesha kozi za kitaalamu na masomo mafupi kama CCNA, CCNP, PMP, CISA, CISM, ITIL, COBIT, Java Certified Programmer, na Microsoft Certified Solutions Developer. Kwa orodha kamili iliyoidhinishwa, tembelea https://www.ucc.co.tz/ au wasiliana na admissions@ucc.co.tz."
        ));
        STATIC_KB_SW.put("programu", Arrays.asList(
                "UCC inatoa programu zifuatazo zilizothibitishwa kwa mwaka wa masomo 2026/2027: Diploma in Computing and Information Technology (DCIT), Diploma in Business Information Technology (DBIT), Certificate in Computing and Information Technology (CCIT), na Certificate in Business Information Technology (CBIT). UCC pia inaorodhesha kozi za kitaalamu na masomo mafupi kama CCNA, CCNP, PMP, CISA, CISM, ITIL, COBIT, Java Certified Programmer, na Microsoft Certified Solutions Developer. Kwa orodha kamili iliyoidhinishwa, tembelea https://www.ucc.co.tz/ au wasiliana na admissions@ucc.co.tz."
        ));
        STATIC_KB_SW.put("kozi", Arrays.asList(
                "UCC inatoa programu zifuatazo zilizothibitishwa kwa mwaka wa masomo 2026/2027: Diploma in Computing and Information Technology (DCIT), Diploma in Business Information Technology (DBIT), Certificate in Computing and Information Technology (CCIT), na Certificate in Business Information Technology (CBIT). UCC pia inaorodhesha kozi za kitaalamu na masomo mafupi kama CCNA, CCNP, PMP, CISA, CISM, ITIL, COBIT, Java Certified Programmer, na Microsoft Certified Solutions Developer. Kwa orodha kamili iliyoidhinishwa, tembelea https://www.ucc.co.tz/ au wasiliana na admissions@ucc.co.tz."
        ));
        STATIC_KB_SW.put("dcit", Arrays.asList(
                "Diploma in Computing and Information Technology (DCIT): Muda wa masomo miaka 2 (semester 4). Vigezo vya kujiunga: (1) ACSEE na angalau pass 1 ya kiini na 1 ya subsidiary; AU (2) Basic Technician Certificate (NTA Level 4) katika Computer Science, Information Technology, Business Information Technology, Computer Engineering, au Electronic Engineering. Ada ya 2026/2027: Ada ya masomo TZS 2,800,000, Ichapishaji TZS 60,000, Kadi ya Utambulisho TZS 20,000, Huduma za ICT TZS 100,000, NACTE Quality Assurance TZS 40,000, Jumla TZS 3,020,000. Jiandikishe kwa https://admission.ucc.co.tz/. Maeneo: UCC Headquarters kwenye UDSM Mlimani Campus kando ya NBC Bank, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji. Chanzo: https://www.ucc.co.tz/course/diploma-in-computing-and-information-technology-dcit-81"
        ));
        STATIC_KB_SW.put("dbit", Arrays.asList(
                "Diploma in Business Information Technology (DBIT): Mwaka wa masomo 2026/2027. Vigezo vya kujiunga: (1) ACSEE na angalau pass 1 ya kiini na 1 ya subsidiary; AU (2) Basic Technician Certificate (NTA Level 4) katika Business Administration, Accountancy, Computer Science, Information Technology, Business Information Technology, au Computer Engineering. Ada ya 2026/2027: Ada ya masomo TZS 2,800,000, Ichapishaji TZS 60,000, Kadi ya Utambulisho TZS 20,000, Huduma za ICT TZS 100,000, NACTE Quality Assurance TZS 40,000, Jumla TZS 3,020,000. Jiandikishe kwa https://admission.ucc.co.tz/. Maeneo: UCC Headquarters kwenye UDSM Mlimani Campus kando ya NBC Bank, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji. Chanzo: https://www.ucc.co.tz/course/diploma-in-business-information-technology-dbit-82"
        ));
        STATIC_KB_SW.put("ccit", Arrays.asList(
                "Certificate in Computing and Information Technology (CCIT): Mwaka wa masomo 2026/2027. Vigezo vya kujiunga: (1) Certificate of Secondary Education na angalau passes 4 katika masomo yasiyo ya dini; AU (2) National Vocational Training Award Level III (Trade Test Grade I) kutoka kwenye taasisi iliyoidhinishwa. Ada ya 2026/2027: Ada ya masomo TZS 1,200,000, Ichapishaji TZS 30,000, Kadi ya Utambulisho TZS 20,000, Huduma za ICT TZS 100,000, NACTE Quality Assurance TZS 20,000, Jumla TZS 1,370,000. Jiandikishe kwa https://admission.ucc.co.tz/. Maeneo: UCC Headquarters kwenye UDSM Mlimani Campus kando ya NBC Bank, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji. Chanzo: https://www.ucc.co.tz/course/certificate-in-computing-and-information-technology-ccit-172"
        ));
        STATIC_KB_SW.put("cbit", Arrays.asList(
                "Certificate in Business Information Technology (CBIT): UCC inaorodhesha CBIT kati ya programu zake za masomo kwa mwaka 2026/2027. Maelezo ya kina ya vigezo vya kujiunga, ada, muda wa masomo, na taarifa nyingine lazima yawe kwenye broshuri iliyoidhinishwa au ukurasa rasmi wa kozi. Jiandikishe kwa https://admission.ucc.co.tz/. Chanzo: https://www.ucc.co.tz/course/academic"
        ));
        STATIC_KB_SW.put("omba", Arrays.asList(
                "Kujiunga UCC: 1. Tembelea https://admission.ucc.co.tz/ 2. Fungua akaunti 3. Chagua programu unayotaka 4. Jaza fomu ya maombi 5. Weka nyaraka zinazohitajika 6. Lipa ada ya maombi 7. Wasilisha. Wasiliana na admissions@ucc.co.tz / +255 22 2410 002."
        ));
        STATIC_KB_SW.put("ada", Arrays.asList(
                "Ada ya masomo inatofautiana kwa programu, mwaka wa masomo, na intake. Kwa taarifa za sasa, wasiliana na Ofisi ya Fedha kwa finance@ucc.co.tz au +255 22 2410 004, au tembelea https://www.ucc.co.tz/."
        ));
        STATIC_KB_SW.put("wasiliana", Arrays.asList(
                "Wasiliana na UCC: Ofisi Kuu: info@ucc.co.tz / +255 22 2410 000. Maombi: admissions@ucc.co.tz / +255 22 2410 002. Msaada wa ICT: ict@ucc.co.tz / +255 22 2410 003. Fedha: finance@ucc.co.tz / +255 22 2410 004. Huduma za Wanafunzi: students@ucc.co.tz / +255 22 2410 005. Anwani: S.L.P. 35091, Dar es Salaam, Tanzania."
        ));
        STATIC_KB_SW.put("msaada", Arrays.asList(
                "Kwa msaada wa ICT: tuma email kwa ict@ucc.co.tz au piga +255 22 2410 003. UCC inatoa ufikiaji wa maabara ya kompyuta, intaneti, akaunti za email, msaada wa LMS, na usaidizi wa usakinishaji wa programu."
        ));
        STATIC_KB_SW.put("usajili", Arrays.asList(
                "Kujiandikisha kwa masomo: 1. Ingia kwenye portal ya wanafunzi wa UCC 2. Nenda kwenye sehemu ya usajili 3. Chagua masomo 4. Kagua uteuzi wako 5. Thibitisha usajili."
        ));
        STATIC_KB_SW.put("habari", Arrays.asList(
                "Habari! Mimi ni UCC Chatbot Assistant. Naweza kukusaidia kupata taarifa kuhusu programu zilizothibitishwa (DCIT, DBIT, CCIT, CBIT), maombi, ada, usajili, huduma za wanafunzi, huduma za ICT, na huduma nyingine za UCC. Niko hapa kukusaidia leo?"
        ));
        STATIC_KB_SW.put("ccna", Arrays.asList(
                "UCC inaorodhesha kozi za kitaalamu na mafunzo mafupi kama Cisco Certified Network Associate (CCNA). Kwa ratiba za sasa, ada, na tarehe za kujiunga, wasiliana na UCC moja kwa moja au tembelea https://www.ucc.co.tz/."
        ));
        STATIC_KB_SW.put("professional", Arrays.asList(
                "UCC inaorodhesha kozi za kitaalamu na mafunzo mafupi kama: Project Management Professional (PMP), Certified Information Systems Auditor (CISA), Certified Information Security Manager (CISM), ITIL Foundation, ITIL Practitioner, COBIT Foundation, CCNA, CCNP, CCIP, Java Certified Programmer (JCP), Microsoft Certified Solutions Developer (MCSD), Business Processes Management, Enterprise Architecture for Managers, IT Governance, IT Service Management, Information Security and Risk Management, Ethical Hacking, na Mobile Application Development. Upatikanio, ratiba, ada, na tarehe za kujiunga kinaweza kubadilika. Kwa taarifa za hivi punde, wasiliana na UCC au tembelea https://www.ucc.co.tz/."
        ));
    }

    public AIServiceImpl(KnowledgeDocumentRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @Override
    public ChatResponse generateResponse(ChatRequest request, String context) {
        String lowerMessage = request.getMessage().toLowerCase();
        String language = detectLanguage(request.getMessage(), request.getLanguage());

        Map<String, List<String>> staticKB = "sw".equals(language) ? STATIC_KB_SW : STATIC_KB_EN;

        for (Map.Entry<String, List<String>> entry : staticKB.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                return ChatResponse.builder()
                        .answer(entry.getValue().get(0))
                        .language(language)
                        .conversationId(request.getConversationId())
                        .sources(List.of(Map.of("title", "UCC Static Knowledge Base", "url", "https://www.ucc.co.tz/")))
                        .confidence(0.7)
                        .escalationRequired(false)
                        .build();
            }
        }

        if (aiApiKey == null || aiApiKey.isBlank()) {
            String noInfoMsg = "sw".equals(language)
                    ? "Sikuweza kupata taarifa zilizothibitishwa kuhusu hili. Tafadhali wasiliana na ofisi inayofaa ya UCC au tembelea https://www.ucc.co.tz/."
                    : "I couldn't find verified information about that. Please contact the relevant UCC office or visit https://www.ucc.co.tz/.";
            return ChatResponse.builder()
                    .answer(noInfoMsg)
                    .language(language)
                    .conversationId(request.getConversationId())
                    .sources(List.of(Map.of("title", "UCC Knowledge Base", "url", "https://www.ucc.co.tz/")))
                    .confidence(0.0)
                    .escalationRequired(true)
                    .build();
        }

        try {
            String fullPrompt = SYSTEM_PROMPT + "\n\nCONTEXT:\n" + (context != null && !context.isBlank() ? context : "No specific context available.");
            String languageInstruction = "sw".equals(language)
                    ? "\n\nIMPORTANT: Respond in Kiswahili."
                    : "\n\nIMPORTANT: Respond in English.";

            String response = callOpenAI(fullPrompt + languageInstruction, request.getMessage());

            return ChatResponse.builder()
                    .answer(response)
                    .language(language)
                    .conversationId(request.getConversationId())
                    .sources(List.of(Map.of("title", "UCC Knowledge Base", "url", "https://www.ucc.co.tz/")))
                    .confidence(0.8)
                    .escalationRequired(false)
                    .build();
        } catch (Exception e) {
            String errorMsg = "sw".equals(language)
                    ? "Nina shida za kiufundi. Tafadhali jaribu tena baadaye au wasiliana na UCC moja kwa moja kwa https://www.ucc.co.tz/."
                    : "I'm experiencing technical difficulties. Please try again later or contact UCC directly at https://www.ucc.co.tz/.";
            return ChatResponse.builder()
                    .answer(errorMsg)
                    .language(language)
                    .conversationId(request.getConversationId())
                    .sources(List.of())
                    .confidence(0.0)
                    .escalationRequired(true)
                    .build();
        }
    }

    private String callOpenAI(String systemPrompt, String userMessage) throws Exception {
        String requestBody = String.format(
                "{\"model\":\"%s\",\"messages\":[{\"role\":\"system\",\"content\":\"%s\"},{\"role\":\"user\",\"content\":\"%s\"}],\"max_tokens\":1024,\"temperature\":0.7}",
                model,
                escapeJson(systemPrompt),
                escapeJson(userMessage)
        );

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(aiApiUrl + "/chat/completions");
            request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
            request.setHeader("Authorization", "Bearer " + aiApiKey);

            try (ClassicHttpResponse response = httpClient.executeOpen(null, request, null)) {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    throw new RuntimeException("OpenAI API returned status: " + statusCode);
                }

                String responseBody = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
                        .lines().collect(Collectors.joining("\n"));

                return extractContentFromResponse(responseBody);
            }
        }
    }

    private String extractContentFromResponse(String jsonResponse) {
        int contentIndex = jsonResponse.indexOf("\"content\":\"");
        if (contentIndex == -1) return jsonResponse;

        int start = contentIndex + 11;
        int end = jsonResponse.indexOf("\"", start);
        if (end == -1) end = jsonResponse.length();

        String content = jsonResponse.substring(start, end);
        return content.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String detectLanguage(String message, String explicitLanguage) {
        if (explicitLanguage != null && !explicitLanguage.isBlank()) {
            return explicitLanguage;
        }

        String lower = message.toLowerCase();

        if (lower.contains("habari") || lower.contains("hujambo") || lower.contains("salamu")) {
            return "sw";
        }

        if (lower.contains("hello") || lower.startsWith("hi ") || lower.equals("hi") || lower.contains("good morning") || lower.contains("good afternoon")) {
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
}
