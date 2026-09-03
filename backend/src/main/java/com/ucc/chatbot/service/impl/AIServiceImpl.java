package com.ucc.chatbot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ucc.chatbot.dto.ChatRequest;
import com.ucc.chatbot.dto.ChatResponse;
import com.ucc.chatbot.model.KnowledgeDocument;
import com.ucc.chatbot.model.Feedback;
import com.ucc.chatbot.repository.KnowledgeDocumentRepository;
import com.ucc.chatbot.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIServiceImpl implements com.ucc.chatbot.service.AIService {

    private final KnowledgeDocumentRepository knowledgeRepository;
    private final FeedbackRepository feedbackRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.api.url:https://api.openai.com/v1}")
    private String aiApiUrl;

    @Value("${ai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.provider:openai}")
    private String provider;

    private static final Set<String> PROGRAMME_CODES = Set.of("DBIT", "DCIT", "CCIT", "CBIT");

    // UCC AI CUSTOMER CARE SYSTEM PROMPT
    // Full 23-section persona used by the LLM when an AI provider is configured.
    // Keeps the chatbot in customer-care mode: accurate, polite, intent-driven,
    // never hallucinates financial or contact information, always offers a
    // practical next step.
    private static final String SYSTEM_PROMPT = """
            # UCC AI CUSTOMER CARE SYSTEM PROMPT

            You are the official AI Customer Care Assistant for the University Computing Centre (UCC).

            Your primary purpose is to provide students, applicants, staff, and visitors with accurate, polite, helpful, clear, and professional information about UCC.

            You must behave like a highly trained UCC customer-care representative, not like a generic AI chatbot.

            ---

            ## 1. CORE OBJECTIVE

            Your most important priorities are:

            1. Accuracy
            2. Customer friendliness
            3. Understanding the user's real intention
            4. Using verified UCC knowledge
            5. Giving practical next steps
            6. Avoiding hallucinations or invented information
            7. Clearly communicating uncertainty when information cannot be verified

            Never sacrifice accuracy for the sake of producing an answer.

            ---

            # 2. CUSTOMER CARE PERSONALITY

            Always communicate in a:

            * Polite
            * Friendly
            * Respectful
            * Professional
            * Helpful
            * Patient
            * Natural
            * Human-like

            manner.

            Do not sound robotic.

            Do not repeatedly say:

            "I couldn't find verified information..."

            unless the requested information genuinely cannot be found.

            Instead, first try all relevant knowledge-base information and related keywords before deciding that information is unavailable.

            Example:

            BAD:

            > I couldn't find verified information about application fees.

            BETTER:

            > Certainly! I can help you with the UCC application process. The application fee is TZS 10,000 according to the current UCC admissions information. You can pay through the payment options provided in the application portal.

            If payment instructions are available in the knowledge base, provide them.

            ---

            # 3. UNDERSTAND USER INTENT, NOT JUST EXACT WORDS

            Never depend only on exact keyword matching.

            Understand synonyms, abbreviations, spelling mistakes, short questions, Swahili/English variations, and different ways users may express the same request.

            For example, all of the following may refer to the same topic:

            "How can I apply to UCC?"

            "How do I apply?"

            "How can I join UCC?"

            "I want admission"

            "UCC admission"

            "UCC application"

            "How to apply for UCC"

            "Where can I apply?"

            "Where is the application portal?"

            "Nataka kuomba UCC"

            "Nawezaje kuomba UCC?"

            "UCC application fee"

            "How much is application?"

            "Application costs how much?"

            "How do I pay application fee?"

            "Nawezaje kulipia application?"

            Treat these as related intents and retrieve information from the relevant UCC knowledge-base section.

            ---

            # 4. KEYWORD AND INTENT EXPANSION

            Before searching the knowledge base, internally identify the user's intent and generate related concepts.

            Example:

            User:

            "How can I apply to UCC?"

            Possible intent:

            ADMISSIONS / APPLICATION

            Related keywords:

            * admission
            * admissions
            * application
            * apply
            * applying
            * applicant
            * application portal
            * admission portal
            * online application
            * joining UCC
            * entry requirements
            * admission requirements
            * application fee
            * payment
            * application deadline
            * intake
            * programme
            * course
            * undergraduate
            * diploma
            * certificate
            * postgraduate

            Swahili equivalents:

            * udahili
            * kuomba
            * maombi
            * maombi ya chuo
            * kujiunga
            * fomu ya maombi
            * ada ya maombi
            * malipo
            * masharti ya kujiunga
            * sifa za kujiunga

            Use these concepts when retrieving relevant knowledge.

            ---

            # 5. DO NOT ANSWER ONLY FROM ONE SEARCH RESULT

            When answering an important question, consider all relevant knowledge-base information available.

            For example, for:

            "How do I apply?"

            Look for:

            * application procedure
            * application portal
            * application fee
            * payment method
            * admission requirements
            * application deadline
            * required documents
            * programmes
            * contact information

            Combine the relevant information into one useful answer.

            Do not unnecessarily expose internal retrieval results to the user.

            ---

            # 6. SOURCE PRIORITY

            When multiple sources contain information, prioritize them in this order:

            1. Current official UCC information
            2. Official UCC admissions information
            3. Official UCC website
            4. Official UCC documents
            5. Approved UCC knowledge-base content
            6. Older UCC information

            Never prioritize outdated information over newer verified official information.

            If two official sources conflict, prefer the newest valid source.

            If the date cannot be determined, do not silently choose one.

            Explain the conflict briefly and recommend verification with UCC.

            ---

            # 7. FEES AND PAYMENT ACCURACY

            Fees, application charges, tuition fees, accommodation fees, registration fees, examination fees, and other financial information are HIGH-ACCURACY information.

            Never invent or estimate a fee.

            If the knowledge base states:

            Application fee = TZS 10,000

            answer:

            "The current UCC information in my knowledge base indicates an application fee of TZS 10,000."

            If the knowledge base also contains payment instructions, provide them.

            For example:

            "To pay the application fee, use the payment option provided in the UCC application portal and follow the instructions displayed during the application process."

            If exact payment methods are verified in the knowledge base, state them.

            Do NOT invent:

            * Mobile money numbers
            * Bank account numbers
            * Control numbers
            * Paybill numbers
            * Merchant numbers
            * Account names
            * Payment procedures

            If these details are not verified, do not guess them.

            ---

            # 8. CONSISTENCY RULE

            If the user asks:

            "What is the application fee?"

            and the knowledge base says TZS 10,000,

            answer consistently with TZS 10,000.

            If the user asks:

            "How much should I pay?"

            answer using the same verified fee.

            If the user asks:

            "Is the application fee TZS 10,000?"

            confirm it only if the knowledge base supports it.

            If conflicting information exists, do not pretend they are the same.

            ---

            # 9. NEVER HALLUCINATE

            Never create information that does not exist in the verified UCC knowledge base.

            Do not invent:

            * Fees
            * Dates
            * Programmes
            * Admission requirements
            * Contact numbers
            * Email addresses
            * URLs
            * Payment numbers
            * Bank details
            * Staff names
            * Departments
            * Campus information
            * Opening hours
            * Policies
            * Application procedures

            If information is unavailable, say so politely.

            Example:

            "I'd be happy to help. I don't currently have verified UCC information for that specific question. Please contact UCC directly through the official contact details below so you can receive the correct information."

            ---

            # 10. ANSWER WITH PRACTICAL HELP

            Whenever possible, answer the user's question AND provide the next useful step.

            For example:

            User:

            "How do I apply?"

            Answer should preferably contain:

            * Where to apply
            * Basic process
            * Required documents
            * Application fee
            * Payment procedure, if verified
            * Deadline, if verified
            * Where to get help

            Do not overwhelm the user with unrelated information.

            ---

            # 11. FOLLOW-UP QUESTIONS

            If the user's question is too broad, ask a short clarification.

            Example:

            User:

            "Fees?"

            Possible response:

            "Certainly. Are you asking about the UCC application fee, tuition fees, accommodation fees, or another fee?"

            If the knowledge base clearly indicates what they mean from context, do not ask unnecessary questions.

            ---

            # 12. CONTEXT AWARENESS

            Remember the conversation context.

            Example:

            User:

            "How can I apply?"

            Assistant answers about application.

            User:

            "How much?"

            Understand that "how much?" most likely refers to the application fee.

            Do not respond:

            "How much what?"

            Use the previous conversation context.

            ---

            # 13. LANGUAGE

            Support both English and Swahili.

            If the user asks in Swahili, respond naturally in Swahili.

            If the user asks in English, respond in English.

            If the user mixes English and Swahili, respond naturally using the language style that best matches the user.

            Example:

            User:

            "Application fee ni kiasi gani?"

            Response:

            "Application fee ya UCC ni TZS 10,000, kulingana na taarifa iliyothibitishwa kwenye mfumo."

            Do not translate technical UCC names unnecessarily.

            ---

            # 14. LINKS

            When providing official UCC links, only use verified URLs stored in the knowledge base.

            Never generate or modify URLs.

            Display important links clearly.

            Example:

            Application Portal:
            https://admission.ucc.co.tz/

            Official UCC Website:
            https://ucc.co.tz/

            Only provide these if they are verified in the knowledge base.

            ---

            # 15. CONTACT INFORMATION

            Only provide UCC phone numbers, emails, addresses, or social-media accounts when they exist in the verified knowledge base.

            Never guess contact information.

            ---

            # 16. RESPONSE STRUCTURE

            For normal customer questions, use this structure when appropriate:

            1. Friendly acknowledgement
            2. Direct answer
            3. Useful details
            4. Practical next step
            5. Offer further assistance

            Example:

            "Certainly! I'd be happy to help you apply to UCC.

            For the 2026/2027 academic year, you can apply through the official UCC online application portal.

            Application portal:
            [verified portal]

            The application fee is TZS 10,000.

            To apply:

            1. Create an account.
            2. Select your programme.
            3. Complete the application form.
            4. Upload the required documents.
            5. Pay the application fee using the verified payment method provided by the portal.
            6. Submit your application.

            If you'd like, I can also explain the UCC admission requirements or guide you through the application process step by step."

            ---

            # 17. SEARCH FAILURE BEHAVIOR

            Do NOT immediately respond with:

            "I couldn't find verified information."

            Before doing so:

            1. Analyze the user's intent.
            2. Expand related keywords.
            3. Search synonyms.
            4. Search related UCC topics.
            5. Check relevant categories.
            6. Check current and older approved UCC information.
            7. Check whether the answer can be constructed from multiple verified pieces of information.

            Only after these steps should you report that information is unavailable.

            ---

            # 18. PARTIAL INFORMATION

            If some information is available and some is unavailable, provide the available verified information.

            Example:

            If the system knows:

            Application fee = TZS 10,000

            but does not know the exact mobile-money procedure:

            Say:

            "The application fee is TZS 10,000. For the exact mobile-money payment steps, please follow the payment instructions shown in the official application portal, as I don't want to provide an incorrect payment procedure."

            Do NOT reject the entire question.

            ---

            # 19. IMPORTANT INFORMATION SHOULD BE EXPLICIT

            When answering questions about:

            * Fees
            * Deadlines
            * Admission requirements
            * Payment
            * Application
            * Programmes
            * Contacts
            * Registration

            clearly state the information instead of hiding it inside a long paragraph.

            ---

            # 20. SOURCE TRANSPARENCY

            Do not expose internal technical retrieval information.

            Do not tell the customer:

            "Vector database returned..."

            "Embedding search found..."

            "RAG retrieved..."

            "Knowledge-base chunk..."

            Instead, say:

            "According to the available UCC information..."

            or

            "According to the current UCC admissions information..."

            Only show a source/reference section if the application interface is specifically designed to display sources.

            ---

            # 21. CUSTOMER CARE SAFETY

            If the user asks for information involving money or payment:

            Be especially careful.

            Never ask users to send:

            * Passwords
            * OTPs
            * Bank PINs
            * Mobile-money PINs
            * Credit/debit card numbers
            * Authentication codes

            Never request sensitive credentials.

            ---

            # 22. RESPONSE QUALITY CHECK

            Before sending every answer, internally check:

            - Did I understand the user's actual question?
            - Did I search related keywords and synonyms?
            - Is the information supported by UCC knowledge?
            - Is the information current?
            - Did I accidentally invent anything?
            - Are fees accurate?
            - Are payment instructions verified?
            - Are dates accurate?
            - Are links official and verified?
            - Did I provide a useful next step?
            - Is my tone polite and professional?
            - Can the customer understand the answer easily?

            If any critical information is uncertain, do not guess.

            ---

            # 23. GOLDEN RULE

            Your job is not simply to answer questions.

            Your job is to help the customer successfully obtain accurate UCC information.

            Always aim to make the customer say:

            "Yes, this answered my question."

            rather than:

            "I need to contact UCC because the chatbot couldn't find anything."

            However, accuracy is more important than pretending to know something.

            When information truly cannot be verified, politely direct the customer to the official UCC support channels.

            ---

            # FINAL BEHAVIOR

            Act as:

            UCC INFORMATION ASSISTANT
            +
            UCC CUSTOMER CARE REPRESENTATIVE
            +
            ACCURATE KNOWLEDGE-BASE SEARCH ASSISTANT

            Be friendly.

            Be intelligent.

            Understand intent.

            Search broadly.

            Use context.

            Be consistent.

            Never hallucinate.

            Never invent financial or official information.

            Give practical answers.

            Help the customer complete their task.
            """;

    private static final Map<String, List<String>> STATIC_KB_EN = new LinkedHashMap<>();
    private static final Map<String, List<String>> STATIC_KB_SW = new LinkedHashMap<>();

    static {
        STATIC_KB_EN.put("compare", Arrays.asList(
                "DCIT vs DBIT — quick comparison:\n\n• DCIT — Diploma in Computing and Information Technology (2 years, total TZS 3,020,000)\n   - Focus: hardware, networking, programming, web, databases, server & CCNA-aligned practicals.\n   - Best for: students who want a strong technical / software-development / network-engineering career path, or who plan to bridge to a CS/IT degree.\n\n• DBIT — Diploma in Business Information Technology (2 years, total TZS 3,020,000)\n   - Focus: business + IT (accounting packages, e-business, web services, business law, entrepreneurship, management).\n   - Best for: students who want to work at the intersection of business and IT — business analyst, IT sales, e-commerce, ERP/SAP, or run their own IT business.\n\nBoth lead to the same Diploma-level jobs but DBIT is the better choice if you enjoy business subjects, and DCIT is the better choice for pure software / hardware / networking careers.\n\nQuick decision guide:\n• Love coding & networks → DCIT\n• Love business + tech → DBIT\n• Not sure yet → DCIT keeps more doors open for degree bridging."
        ));
        STATIC_KB_EN.put("better", Arrays.asList(
                "DCIT vs DBIT — quick comparison:\n\n• DCIT — Diploma in Computing and Information Technology (2 years, total TZS 3,020,000)\n   - Focus: hardware, networking, programming, web, databases, server & CCNA-aligned practicals.\n   - Best for: students who want a strong technical / software-development / network-engineering career path, or who plan to bridge to a CS/IT degree.\n\n• DBIT — Diploma in Business Information Technology (2 years, total TZS 3,020,000)\n   - Focus: business + IT (accounting packages, e-business, web services, business law, entrepreneurship, management).\n   - Best for: students who want to work at the intersection of business and IT — business analyst, IT sales, e-commerce, ERP/SAP, or run their own IT business.\n\nBoth lead to the same Diploma-level jobs but DBIT is the better choice if you enjoy business subjects, and DCIT is the better choice for pure software / hardware / networking careers.\n\nQuick decision guide:\n• Love coding & networks → DCIT\n• Love business + tech → DBIT\n• Not sure yet → DCIT keeps more doors open for degree bridging."
        ));
        STATIC_KB_EN.put("programme", Arrays.asList(
                "UCC offers the following verified academic programmes for 2026/2027: Diploma in Computing and Information Technology (DCIT), Diploma in Business Information Technology (DBIT), Certificate in Computing and Information Technology (CCIT), and Certificate in Business Information Technology (CBIT). UCC also lists professional and short courses such as CCNA, CCNP, PMP, CISA, CISM, ITIL, COBIT, Java Certified Programmer, and Microsoft Certified Solutions Developer. For the complete approved list, visit https://ucc.co.tz/ or contact admissions@ucc.co.tz."
        ));
        STATIC_KB_EN.put("dcit", Arrays.asList(
                "Diploma in Computing and Information Technology (DCIT):\n• Duration: 2 years (4 semesters)\n• Entry requirements: (1) ACSEE with at least 1 principal pass and 1 subsidiary pass; OR (2) Basic Technician Certificate (NTA Level 4) in Computer Science, Information Technology, Business Information Technology, Computer Engineering, or Electronic Engineering.\n• Fees for 2026/2027 (Total TZS 3,020,000):\n   - Tuition: TZS 2,800,000\n   - Examination: TZS 60,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n• Locations: UCC HQ at UDSM Mlimani Campus (opposite NBC Bank), Dar es Salaam, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji.\n• Apply online: https://admission.ucc.co.tz/\nSource: https://ucc.co.tz/course/diploma-in-computing-and-information-technology-dcit-81"
        ));
        STATIC_KB_EN.put("dbit", Arrays.asList(
                "Diploma in Business Information Technology (DBIT):\n• Duration: 2 years (4 semesters) plus project work\n• Entry requirements: (1) ACSEE with at least 1 principal pass and 1 subsidiary pass; OR (2) Basic Technician Certificate (NTA Level 4) in Business Administration, Accountancy, Computer Science, Information Technology, Business Information Technology, or Computer Engineering.\n• Fees for 2026/2027 (Total TZS 3,020,000):\n   - Tuition: TZS 2,800,000\n   - Examination: TZS 60,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n• Locations: UCC HQ at UDSM Mlimani Campus (opposite NBC Bank), Dar es Salaam, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji.\n• Apply online: https://admission.ucc.co.tz/\nSource: https://ucc.co.tz/course/diploma-in-business-information-technology-dbit-82"
        ));
        STATIC_KB_EN.put("ccit", Arrays.asList(
                "Certificate in Computing and Information Technology (CCIT):\n• Duration: 1 year (2 semesters) plus field work\n• Entry requirements: (1) Certificate of Secondary Education (CSEE) with at least 4 passes in non-religious subjects; OR (2) National Vocational Training Award Level III (Trade Test Grade I) from a recognized institution.\n• Fees for 2026/2027 (Total TZS 1,370,000):\n   - Tuition: TZS 1,200,000\n   - Examination: TZS 30,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n• Locations: UCC HQ at UDSM Mlimani Campus (opposite NBC Bank), Dar es Salaam, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji.\n• Apply online: https://admission.ucc.co.tz/\nSource: https://ucc.co.tz/course/certificate-in-computing-and-information-technology-ccit-172"
        ));
        STATIC_KB_EN.put("cbit", Arrays.asList(
                "Certificate in Business Information Technology (CBIT):\n• Duration: 1 year (2 semesters) plus field work\n• Entry requirements: (1) Certificate of Secondary Education (CSEE) with at least 4 passes in non-religious subjects; OR (2) National Vocational Training Award Level III (Trade Test Grade I) from a recognized institution.\n• Fees for 2026/2027 (Total TZS 1,370,000):\n   - Tuition: TZS 1,200,000\n   - Examination: TZS 30,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n• Locations: UCC HQ at UDSM Mlimani Campus (opposite NBC Bank), Dar es Salaam, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji.\n• Apply online: https://admission.ucc.co.tz/\nSource: https://ucc.co.tz/course/certificate-in-business-information-technology-cbit-173"
        ));
        STATIC_KB_EN.put("join", Arrays.asList(
                "Joining UCC is easy. Here's the path:\n\n• Pick your programme: DCIT, DBIT, CCIT or CBIT.\n• Apply online between 1st June 2026 and 30th September 2026 at https://admission.ucc.co.tz/.\n• Pay the TZS 10,000 application fee and upload your certificates.\n• Once selected, complete registration at the campus (UDSM Mlimani in Dar es Salaam, or UCC Dodoma Branch).\n\nRequired documents: CSEE/ACSEE certificates, birth certificate, passport photo. For help, email admissions@ucc.co.tz or call +255 22 2410641/5."
        ));
        STATIC_KB_EN.put("application", Arrays.asList(
                "UCC Admissions 2026/2027 — important dates:\n\n• Applications OPEN: 1st June 2026\n• Applications CLOSE: 30th September 2026\n• Intake / classes begin: September 2026\n• Application fee: TZS 10,000 (non-refundable, paid online via mobile money or bank card)\n• Apply at: https://admission.ucc.co.tz/\n\nHow to apply (4 steps):\n1. Visit https://admission.ucc.co.tz/ and create an account with your email and phone number.\n2. Select your preferred programme (DCIT, DBIT, CCIT or CBIT).\n3. Complete the application form and upload the required documents.\n4. Pay the application fee and submit. You will receive a confirmation SMS/email within 24 hours.\n\nRequired documents: CSEE/ACSEE certificates or equivalent, birth certificate, passport-size photo. For help, email admissions@ucc.co.tz or call +255 22 2410641/5 (Mon–Fri 8:00–17:00, Sat 8:00–13:00)."
        ));
        STATIC_KB_EN.put("admission", Arrays.asList(
                "UCC Admissions 2026/2027 — important dates:\n\n• Applications OPEN: 1st June 2026\n• Applications CLOSE: 30th September 2026\n• Intake / classes begin: September 2026\n• Application fee: TZS 10,000\n• Apply at: https://admission.ucc.co.tz/\n\nEligible programmes and basic requirements:\n• DCIT (Diploma, 2 years) — ACSEE with 1 principal + 1 subsidiary pass, OR NTA Level 4 in CS/IT/BIT/Computer Eng./Electronic Eng.\n• DBIT (Diploma, 2 years) — ACSEE with 1 principal + 1 subsidiary pass, OR NTA Level 4 in Business Admin/Accountancy/CS/IT/BIT/Computer Eng.\n• CCIT (Certificate, 1 year) — CSEE with 4 passes in non-religious subjects, OR NVTA Level III / Trade Test Grade I.\n• CBIT (Certificate, 1 year) — CSEE with 4 passes in non-religious subjects, OR NVTA Level III / Trade Test Grade I.\n\nFor late or special intakes, contact admissions@ucc.co.tz or +255 22 2410641/5. Late applications may be considered if seats are still available."
        ));
        STATIC_KB_EN.put("apply", Arrays.asList(
                "UCC Admissions — 2026/2027 Academic Year:\n\n• Application window: 1st June 2026 — 30th September 2026 (intake: September 2026)\n• Online application portal: https://admission.ucc.co.tz/\n• Application fee: TZS 10,000 (non-refundable, paid online via mobile money or bank card)\n• Required documents: CSEE/ACSEE certificates or equivalent, birth certificate, passport-size photo.\n\nHow to apply (4 steps):\n1. Visit https://admission.ucc.co.tz/ and create an account with your email and phone number.\n2. Select your preferred programme (DCIT, DBIT, CCIT or CBIT).\n3. Complete the application form and upload the required documents.\n4. Pay the application fee and submit. You will receive a confirmation SMS/email within 24 hours.\n\nSelection and joining instructions are released on a rolling basis. For help, email admissions@ucc.co.tz or call +255 22 2410641/5 (Mon–Fri 8:00–17:00, Sat 8:00–13:00)."
        ));
        STATIC_KB_EN.put("contact", Arrays.asList(
                "Here's how to reach us — we'd love to hear from you:\n\n• General: info@ucc.co.tz | +255 22 2410641/5 | +255 754782120\n• Main Office (UDSM Mlimani): ucc@udsm.ac.tz | +255 754782120\n• Dodoma Branch: dodoma@udsm.ac.tz | +255 0747 626 619\n• Admission Portal: https://admission.ucc.co.tz/\n• Website: https://ucc.co.tz/\n\nOur friendly team is available Monday to Friday (8:00 AM - 5:00 PM) and Saturday (8:00 AM - 1:00 PM)."
        ));
        STATIC_KB_EN.put("fee", Arrays.asList(
                "Official UCC fee structure for academic year 2026/2027:\n\n• Diploma in Computing and Information Technology (DCIT) — Total TZS 3,020,000:\n   - Tuition: TZS 2,800,000\n   - Examination: TZS 60,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n\n• Diploma in Business Information Technology (DBIT) — Total TZS 3,020,000:\n   - Tuition: TZS 2,800,000\n   - Examination: TZS 60,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n\n• Certificate in Computing and Information Technology (CCIT) — Total TZS 1,370,000:\n   - Tuition: TZS 1,200,000\n   - Examination: TZS 30,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n\n• Certificate in Business Information Technology (CBIT) — Total TZS 1,370,000:\n   - Tuition: TZS 1,200,000\n   - Examination: TZS 30,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n\nFor professional and short courses (PMP, CISA, CISM, ITIL, COBIT, CCNA, CCNP, etc.) please contact info@ucc.co.tz or +255 22 2410641/5 for current fees and intake dates."
        ));
        STATIC_KB_EN.put("ict", Arrays.asList(
                "For ICT support: email ict@ucc.co.tz or call +255 22 2410 003. UCC provides computer lab access, internet, email accounts, LMS support, and software installation assistance."
        ));
        STATIC_KB_EN.put("registration", Arrays.asList(
                "To register for courses: 1. Log in to the UCC student portal 2. Navigate to the registration section 3. Select courses 4. Review selection 5. Confirm registration."
        ));
        STATIC_KB_EN.put("hello", Arrays.asList(
                "Hello! 👋 Welcome to the University of Dar es Salaam Computing Centre (UCC). I'm your UCC Customer Care Assistant.\n\nHere's what I can help you with right now:\n• Programmes and fees (DCIT, DBIT, CCIT, CBIT, professional courses)\n• Admissions (open 1 June – 30 Sept 2026, intake September 2026)\n• How to apply, entry requirements, locations\n• Contacts and campus info\n\nJust type your question or pick one of the quick options below."
        ));
        STATIC_KB_EN.put("hi", Arrays.asList(
                "Hello! I'm the UCC AI Assistant. How can I help you today?"
        ));
        STATIC_KB_EN.put("thank", Arrays.asList(
                "You're welcome. If you have any other questions about UCC programmes, admissions, or services, feel free to ask."
        ));
        STATIC_KB_EN.put("bye", Arrays.asList(
                "Goodbye. For further assistance, please contact UCC at info@ucc.co.tz or +255 22 2410641/5."
        ));
        STATIC_KB_EN.put("help", Arrays.asList(
                "I can help you with information about:\n• Academic programmes (DCIT, DBIT, CCIT, CBIT)\n• Professional courses (PMP, CISA, CISM, ITIL, COBIT and more)\n• Admissions and applications (open 1 June – 30 Sept 2026)\n• Tuition fees and payment\n• IT services and software products\n• Campus locations and contacts\n\nWhat would you like to know?"
        ));
        STATIC_KB_EN.put("when", Arrays.asList(
                "UCC Admissions 2026/2027 — important dates:\n\n• Applications OPEN: 1st June 2026\n• Applications CLOSE: 30th September 2026\n• Intake / classes begin: September 2026\n• Application fee: TZS 10,000\n• Apply at: https://admission.ucc.co.tz/\n\nEligible programmes and basic requirements:\n• DCIT (Diploma, 2 years) — ACSEE with 1 principal + 1 subsidiary pass, OR NTA Level 4 in CS/IT/BIT/Computer Eng./Electronic Eng.\n• DBIT (Diploma, 2 years) — ACSEE with 1 principal + 1 subsidiary pass, OR NTA Level 4 in Business Admin/Accountancy/CS/IT/BIT/Computer Eng.\n• CCIT (Certificate, 1 year) — CSEE with 4 passes in non-religious subjects, OR NVTA Level III / Trade Test Grade I.\n• CBIT (Certificate, 1 year) — CSEE with 4 passes in non-religious subjects, OR NVTA Level III / Trade Test Grade I.\n\nFor late or special intakes, contact admissions@ucc.co.tz or +255 22 2410641/5. Late applications may be considered if seats are still available."
        ));
        STATIC_KB_EN.put("intake", Arrays.asList(
                "UCC Admissions 2026/2027 — important dates:\n\n• Applications OPEN: 1st June 2026\n• Applications CLOSE: 30th September 2026\n• Intake / classes begin: September 2026\n• Application fee: TZS 10,000\n• Apply at: https://admission.ucc.co.tz/\n\nEligible programmes and basic requirements:\n• DCIT (Diploma, 2 years) — ACSEE with 1 principal + 1 subsidiary pass, OR NTA Level 4 in CS/IT/BIT/Computer Eng./Electronic Eng.\n• DBIT (Diploma, 2 years) — ACSEE with 1 principal + 1 subsidiary pass, OR NTA Level 4 in Business Admin/Accountancy/CS/IT/BIT/Computer Eng.\n• CCIT (Certificate, 1 year) — CSEE with 4 passes in non-religious subjects, OR NVTA Level III / Trade Test Grade I.\n• CBIT (Certificate, 1 year) — CSEE with 4 passes in non-religious subjects, OR NVTA Level III / Trade Test Grade I.\n\nFor late or special intakes, contact admissions@ucc.co.tz or +255 22 2410641/5. Late applications may be considered if seats are still available."
        ));
        STATIC_KB_EN.put("ccna", Arrays.asList(
                "UCC lists professional and short courses including Cisco Certified Network Associate (CCNA). For current schedules, fees, and intake dates, please contact UCC directly or visit https://ucc.co.tz/."
        ));
        STATIC_KB_EN.put("professional", Arrays.asList(
                "UCC lists professional and short courses including: Project Management Professional (PMP), Certified Information Systems Auditor (CISA), Certified Information Security Manager (CISM), ITIL Foundation, ITIL Practitioner, COBIT Foundation, CCNA, CCNP, CCIP, Java Certified Programmer (JCP), Microsoft Certified Solutions Developer (MCSD), Business Processes Management, Enterprise Architecture for Managers, IT Governance, IT Service Management, Information Security and Risk Management, Ethical Hacking, and Mobile Application Development. Availability, schedules, fees, and intake dates may change. For the latest information, contact UCC or visit https://ucc.co.tz/."
        ));

        STATIC_KB_SW.put("programme", Arrays.asList(
                "UCC inatoa programu zifuatazo zilizothibitishwa kwa mwaka wa masomo 2026/2027: Diploma in Computing and Information Technology (DCIT), Diploma in Business Information Technology (DBIT), Certificate in Computing and Information Technology (CCIT), na Certificate in Business Information Technology (CBIT). UCC pia inaorodhesha kozi za kitaalamu na masomo mafupi kama CCNA, CCNP, PMP, CISA, CISM, ITIL, COBIT, Java Certified Programmer, na Microsoft Certified Solutions Developer. Kwa orodha kamili iliyoidhinishwa, tembelea https://ucc.co.tz/ au wasiliana na admissions@ucc.co.tz."
        ));
        STATIC_KB_SW.put("programu", Arrays.asList(
                "UCC inatoa programu zifuatazo zilizothibitishwa kwa mwaka wa masomo 2026/2027: Diploma in Computing and Information Technology (DCIT), Diploma in Business Information Technology (DBIT), Certificate in Computing and Information Technology (CCIT), na Certificate in Business Information Technology (CBIT). UCC pia inaorodhesha kozi za kitaalamu na masomo mafupi kama CCNA, CCNP, PMP, CISA, CISM, ITIL, COBIT, Java Certified Programmer, na Microsoft Certified Solutions Developer. Kwa orodha kamili iliyoidhinishwa, tembelea https://ucc.co.tz/ au wasiliana na admissions@ucc.co.tz."
        ));
        STATIC_KB_SW.put("kozi", Arrays.asList(
                "UCC inatoa programu zifuatazo zilizothibitishwa kwa mwaka wa masomo 2026/2027: Diploma in Computing and Information Technology (DCIT), Diploma in Business Information Technology (DBIT), Certificate in Computing and Information Technology (CCIT), na Certificate in Business Information Technology (CBIT). UCC pia inaorodhesha kozi za kitaalamu na masomo mafupi kama CCNA, CCNP, PMP, CISA, CISM, ITIL, COBIT, Java Certified Programmer, na Microsoft Certified Solutions Developer. Kwa orodha kamili iliyoidhinishwa, tembelea https://ucc.co.tz/ au wasiliana na admissions@ucc.co.tz."
        ));
        STATIC_KB_SW.put("dcit", Arrays.asList(
                "Diploma in Computing and Information Technology (DCIT):\n• Muda: miaka 2 (semesta 4)\n• Vigezo vya kujiunga: (1) ACSEE na angalau pass 1 ya kiini na 1 ya subsidiary; AU (2) Basic Technician Certificate (NTA Level 4) katika Computer Science, Information Technology, Business Information Technology, Computer Engineering, au Electronic Engineering.\n• Ada ya 2026/2027 (Jumla TZS 3,020,000):\n   - Ada ya masomo: TZS 2,800,000\n   - Mitihani: TZS 60,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n• Maeneo: UCC HQ kwenye UDSM Mlimani Campus (kando ya NBC Bank), Dar es Salaam, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji.\n• Jiandikishe mtandaoni: https://admission.ucc.co.tz/\nChanzo: https://ucc.co.tz/course/diploma-in-computing-and-information-technology-dcit-81"
        ));
        STATIC_KB_SW.put("dbit", Arrays.asList(
                "Diploma in Business Information Technology (DBIT):\n• Muda: miaka 2 (semesta 4) pamoja na mradi\n• Vigezo vya kujiunga: (1) ACSEE na angalau pass 1 ya kiini na 1 ya subsidiary; AU (2) Basic Technician Certificate (NTA Level 4) katika Business Administration, Accountancy, Computer Science, Information Technology, Business Information Technology, au Computer Engineering.\n• Ada ya 2026/2027 (Jumla TZS 3,020,000):\n   - Ada ya masomo: TZS 2,800,000\n   - Mitihani: TZS 60,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n• Maeneo: UCC HQ kwenye UDSM Mlimani Campus (kando ya NBC Bank), Dar es Salaam, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji.\n• Jiandikishe mtandaoni: https://admission.ucc.co.tz/\nChanzo: https://ucc.co.tz/course/diploma-in-business-information-technology-dbit-82"
        ));
        STATIC_KB_SW.put("ccit", Arrays.asList(
                "Certificate in Computing and Information Technology (CCIT):\n• Muda: mwaka 1 (semesta 2) pamoja na kazi ya uwandani\n• Vigezo vya kujiunga: (1) Certificate of Secondary Education (CSEE) na angalau passes 4 katika masomo yasiyo ya dini; AU (2) National Vocational Training Award Level III (Trade Test Grade I) kutoka taasisi iliyoidhinishwa.\n• Ada ya 2026/2027 (Jumla TZS 1,370,000):\n   - Ada ya masomo: TZS 1,200,000\n   - Mitihani: TZS 30,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n• Maeneo: UCC HQ kwenye UDSM Mlimani Campus (kando ya NBC Bank), Dar es Salaam, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji.\n• Jiandikishe mtandaoni: https://admission.ucc.co.tz/\nChanzo: https://ucc.co.tz/course/certificate-in-computing-and-information-technology-ccit-172"
        ));
        STATIC_KB_SW.put("cbit", Arrays.asList(
                "Certificate in Business Information Technology (CBIT):\n• Muda: mwaka 1 (semesta 2) pamoja na kazi ya uwandani\n• Vigezo vya kujiunga: (1) Certificate of Secondary Education (CSEE) na angalau passes 4 katika masomo yasiyo ya dini; AU (2) National Vocational Training Award Level III (Trade Test Grade I) kutoka taasisi iliyoidhinishwa.\n• Ada ya 2026/2027 (Jumla TZS 1,370,000):\n   - Ada ya masomo: TZS 1,200,000\n   - Mitihani: TZS 30,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n• Maeneo: UCC HQ kwenye UDSM Mlimani Campus (kando ya NBC Bank), Dar es Salaam, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji.\n• Jiandikishe mtandaoni: https://admission.ucc.co.tz/\nChanzo: https://ucc.co.tz/course/certificate-in-business-information-technology-cbit-173"
        ));
        STATIC_KB_SW.put("omba", Arrays.asList(
                "Udaahili wa UCC — Mwaka wa Masomo 2026/2027:\n\n• Dirisha la maombi: 1 Juni 2026 — 30 Septemba 2026 (intake: Septemba 2026)\n• Portal ya maombi mtandaoni: https://admission.ucc.co.tz/\n• Ada ya maombi: TZS 10,000 (haitarejeshwa, hulipwa mtandaoni kwa njia ya mobile money au kadi ya benki)\n• Nyaraka zinazohitajika: vyeti vya CSEE/ACSEE au sawa na hivyo, cheti cha kuzaliwa, picha ya paspoti.\n\nJinsi ya kuomba (hatua 4):\n1. Tembelea https://admission.ucc.co.tz/ na ufungue akaunti kwa kutumia email na nambari yako ya simu.\n2. Chagua programu unayotaka (DCIT, DBIT, CCIT au CBIT).\n3. Kamilisha fomu ya maombi na upakie nyaraka zinazohitajika.\n4. Lipa ada ya maombi na uwasilishe. Utapokea ujumbe wa kuthibitisha ndani ya masaa 24.\n\nMaelekezo ya uchaguzi na kujiunga yanatolewa kwa awamu. Kwa msaada, tuma email kwa admissions@ucc.co.tz au piga +255 22 2410641/5 (Jumatatu–Ijumaa 8:00–17:00, Jumamosi 8:00–13:00)."
        ));
        STATIC_KB_SW.put("jiunga", Arrays.asList(
                "Kujiunga na UCC ni rahisi. Hapa kuna njia:\n\n• Chagua programu yako: DCIT, DBIT, CCIT au CBIT.\n• Omba mtandaoni kati ya 1 Juni 2026 na 30 Septemba 2026 kwenye https://admission.ucc.co.tz/.\n• Lipa ada ya maombi ya TZS 10,000 na upakie vyeti vyako.\n• Ukichaguliwa, kamilisha usajili katika kampasi (UDSM Mlimani Dar es Salaam, au Tawi la UCC Dodoma).\n\nNyaraka zinazohitajika: vyeti vya CSEE/ACSEE, cheti cha kuzaliwa, picha ya paspoti. Kwa msaada, tuma email kwa admissions@ucc.co.tz au piga +255 22 2410641/5."
        ));
        STATIC_KB_SW.put("application fee", Arrays.asList(
                "Ada ya maombi ya udahili wa UCC kwa mwaka wa masomo 2026/2027:\n\n• Ada ya maombi: TZS 10,000 (haitarejeshwa)\n• Hulipwa mtandaoni kupitia portal ya maombi kwa njia ya mobile money au kadi ya benki\n• Dirisha la maombi: 1 Juni 2026 — 30 Septemba 2026\n• Portal: https://admission.ucc.co.tz/\n\nKwa msaada zaidi kuhusu ada ya maombi, wasiliana na admissions@ucc.co.tz au piga +255 22 2410641/5."
        ));
        STATIC_KB_SW.put("ada_ya_maombi", Arrays.asList(
                "Ada ya maombi ya udahili wa UCC kwa mwaka wa masomo 2026/2027:\n\n• Ada ya maombi: TZS 10,000 (haitarejeshwa)\n• Hulipwa mtandaoni kupitia portal ya maombi kwa njia ya mobile money au kadi ya benki\n• Dirisha la maombi: 1 Juni 2026 — 30 Septemba 2026\n• Portal: https://admission.ucc.co.tz/\n\nKwa msaada zaidi kuhusu ada ya maombi, wasiliana na admissions@ucc.co.tz au piga +255 22 2410641/5."
        ));
        STATIC_KB_SW.put("ada", Arrays.asList(
                "Muundo rasmi wa ada wa UCC kwa mwaka wa masomo 2026/2027:\n\n• Diploma in Computing and Information Technology (DCIT) — Jumla TZS 3,020,000:\n   - Ada ya masomo: TZS 2,800,000\n   - Mitihani: TZS 60,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n\n• Diploma in Business Information Technology (DBIT) — Jumla TZS 3,020,000:\n   - Ada ya masomo: TZS 2,800,000\n   - Mitihani: TZS 60,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n\n• Certificate in Computing and Information Technology (CCIT) — Jumla TZS 1,370,000:\n   - Ada ya masomo: TZS 1,200,000\n   - Mitihani: TZS 30,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n\n• Certificate in Business Information Technology (CBIT) — Jumla TZS 1,370,000:\n   - Ada ya masomo: TZS 1,200,000\n   - Mitihani: TZS 30,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n\nKwa kozi za kitaalamu na mafunzo mafupi (PMP, CISA, CISM, ITIL, COBIT, CCNA, CCNP, n.k.) tafadhali wasiliana na info@ucc.co.tz au +255 22 2410641/5 kwa ada ya sasa na tarehe za kujiunga."
        ));
        STATIC_KB_SW.put("wasiliana", Arrays.asList(
                "Hivi ndivyo unavyoweza kutupata — tungependa kusikia kutoka kwako:\n\n• Jumla: info@ucc.co.tz | +255 22 2410641/5 | +255 754782120\n• Ofisi Kuu (UDSM Mlimani): ucc@udsm.ac.tz | +255 754782120\n• Tawi la Dodoma: dodoma@udsm.ac.tz | +255 0747 626 619\n• Portal ya Udaahili: https://admission.ucc.co.tz/\n• Tovuti: https://ucc.co.tz/\n\nTimu yetu ya urafiki inapatikana Jumatatu hadi Ijumaa (saa 3:00 asubuhi - saa 11:00 jioni) na Jumamosi (saa 3:00 asubuhi - saa 7:00 mchana)."
        ));
        STATIC_KB_SW.put("msaada", Arrays.asList(
                "Kwa msaada wa ICT: tuma email kwa ict@ucc.co.tz au piga +255 22 2410 003. UCC inatoa ufikiaji wa maabara ya kompyuta, intaneti, akaunti za email, msaada wa LMS, na usaidizi wa usakinishaji wa programu."
        ));
        STATIC_KB_SW.put("usajili", Arrays.asList(
                "Kujiandikisha kwa masomo: 1. Ingia kwenye portal ya wanafunzi wa UCC 2. Nenda kwenye sehemu ya usajili 3. Chagua masomo 4. Kagua uteuzi wako 5. Thibitisha usajili."
        ));
        STATIC_KB_SW.put("habari", Arrays.asList(
                "Habari! 👋 Karibu katika Kituo cha Kompyuta cha Chuo Kikuu cha Dar es Salaam (UCC). Mimi ni Msaidizi wako wa Huduma kwa Wateja wa UCC.\n\nHivi ndivyo ninavyoweza kukusaidia sasa hivi:\n• Programu na ada (DCIT, DBIT, CCIT, CBIT, kozi za kitaalamu)\n• Udaahili (dirisha wazi 1 Juni – 30 Septemba 2026, intake Septemba 2026)\n• Jinsi ya kuomba, vigezo vya kujiunga, maeneo\n• Mawasiliano na taarifa za kampasi\n\nAndika swali lako au chagua chaguo la haraka hapa chini."
        ));
        STATIC_KB_SW.put("hujambo", Arrays.asList(
                "Hujambo! 👋 Karibu katika UCC. Mimi ni Msaidizi wako wa Huduma kwa Wateja. Naweza kukusaidia na programu, udahili, ada, na huduma nyingine za UCC. Andika swali lako au chagua chaguo la haraka."
        ));
        STATIC_KB_SW.put("udahili", Arrays.asList(
                "Udaahili wa UCC 2026/2027 — tarehe muhimu:\n\n• Maombi YANAFUNGULIWA: 1 Juni 2026\n• Maombi YANAFUNGWA: 30 Septemba 2026\n• Intake / masomo yanayoanza: Septemba 2026\n• Ada ya maombi: TZS 10,000\n• Tuma maombi kwa: https://admission.ucc.co.tz/\n\nProgramu zinazokubaliwa na vigezo vya msingi:\n• DCIT (Diploma, miaka 2) — ACSEE na pass 1 ya kiini + 1 ya subsidiary, AU NTA Level 4 katika CS/IT/BIT/Computer Eng./Electronic Eng.\n• DBIT (Diploma, miaka 2) — ACSEE na pass 1 ya kiini + 1 ya subsidiary, AU NTA Level 4 katika Business Admin/Accountancy/CS/IT/BIT/Computer Eng.\n• CCIT (Cheti, mwaka 1) — CSEE na passes 4 katika masomo yasiyo ya dini, AU NVTA Level III / Trade Test Grade I.\n• CBIT (Cheti, mwaka 1) — CSEE na passes 4 katika masomo yasiyo ya dini, AU NVTA Level III / Trade Test Grade I.\n\nKwa maombi ya kuchelewa au intake maalum, wasiliana na admissions@ucc.co.tz au +255 22 2410641/5. Maombi ya kuchelewa yanaweza kuzingatiwa kama bado kuna nafasi."
        ));
        STATIC_KB_SW.put("linganisha", Arrays.asList(
                "DCIT vs DBIT — kulinganisha kwa ufupi:\n\n• DCIT — Diploma in Computing and Information Technology (miaka 2, jumla TZS 3,020,000)\n   - Lengo: hardware, mitandao, programming, web, databases, server na mazoezi ya CCNA.\n   - Kwa: wanafunzi wanaotaka njia ya kiufundi / software development / network engineering, au wanaopanga kuendelea na shahada ya CS/IT.\n\n• DBIT — Diploma in Business Information Technology (miaka 2, jumla TZS 3,020,000)\n   - Lengo: biashara + IT (accounting packages, e-business, web services, business law, entrepreneurship, management).\n   - Kwa: wanafunzi wanaotaka kufanya kazi katika mwingiliano wa biashara na IT — business analyst, mauzo ya IT, e-commerce, ERP/SAP, au kuendesha biashara yao wenyewe ya IT.\n\nZote zinafikia kazi za Diploma lakini DBIT ni chaguo bora ikiwa unapenda masomo ya biashara, na DCIT ni chaguo bora kwa kazi za software / hardware / mitandao.\n\nMwongozo wa haraka wa uamuzi:\n• Unapenda coding & mitandao → DCIT\n• Unapenda biashara + teknolojia → DBIT\n• Bado huna uhakika → DCIT inakuweka njia nyingi wazi za kujenga shahada."
        ));
        STATIC_KB_SW.put("bora", Arrays.asList(
                "DCIT vs DBIT — kulinganisha kwa ufupi:\n\n• DCIT — Diploma in Computing and Information Technology (miaka 2, jumla TZS 3,020,000)\n   - Lengo: hardware, mitandao, programming, web, databases, server na mazoezi ya CCNA.\n   - Kwa: wanafunzi wanaotaka njia ya kiufundi / software development / network engineering, au wanaopanga kuendelea na shahada ya CS/IT.\n\n• DBIT — Diploma in Business Information Technology (miaka 2, jumla TZS 3,020,000)\n   - Lengo: biashara + IT (accounting packages, e-business, web services, business law, entrepreneurship, management).\n   - Kwa: wanafunzi wanaotaka kufanya kazi katika mwingiliano wa biashara na IT — business analyst, mauzo ya IT, e-commerce, ERP/SAP, au kuendesha biashara yao wenyewe ya IT.\n\nZote zinafikia kazi za Diploma lakini DBIT ni chaguo bora ikiwa unapenda masomo ya biashara, na DCIT ni chaguo bora kwa kazi za software / hardware / mitandao.\n\nMwongozo wa haraka wa uamuzi:\n• Unapenda coding & mitandao → DCIT\n• Unapenda biashara + teknolojia → DBIT\n• Bado huna uhakika → DCIT inakuweka njia nyingi wazi za kujenga shahada."
        ));
        STATIC_KB_SW.put("lini", Arrays.asList(
                "Udaahili wa UCC 2026/2027 — tarehe muhimu:\n\n• Maombi YANAFUNGULIWA: 1 Juni 2026\n• Maombi YANAFUNGWA: 30 Septemba 2026\n• Intake / masomo yanayoanza: Septemba 2026\n• Ada ya maombi: TZS 10,000\n• Tuma maombi kwa: https://admission.ucc.co.tz/\n\nProgramu zinazokubaliwa na vigezo vya msingi:\n• DCIT (Diploma, miaka 2) — ACSEE na pass 1 ya kiini + 1 ya subsidiary, AU NTA Level 4 katika CS/IT/BIT/Computer Eng./Electronic Eng.\n• DBIT (Diploma, miaka 2) — ACSEE na pass 1 ya kiini + 1 ya subsidiary, AU NTA Level 4 katika Business Admin/Accountancy/CS/IT/BIT/Computer Eng.\n• CCIT (Cheti, mwaka 1) — CSEE na passes 4 katika masomo yasiyo ya dini, AU NVTA Level III / Trade Test Grade I.\n• CBIT (Cheti, mwaka 1) — CSEE na passes 4 katika masomo yasiyo ya dini, AU NVTA Level III / Trade Test Grade I."
        ));
        STATIC_KB_SW.put("asante", Arrays.asList(
                "Karibu. Ukiwa na maswali mengine kuhusu programu, udahili, au huduma za UCC, usisite kuuliza."
        ));
        STATIC_KB_SW.put("kwaheri", Arrays.asList(
                "Kwaheri. Kwa msaada zaidi, wasiliana na UCC kwa info@ucc.co.tz au +255 22 2410641/5."
        ));
        STATIC_KB_SW.put("msaada_help", Arrays.asList(
                "Naweza kukusaidia na taarifa kuhusu:\n• Programu za masomo (DCIT, DBIT, CCIT, CBIT)\n• Kozi za kitaalamu (PMP, CISA, CISM, ITIL, COBIT na nyinginezo)\n• Udaahili na maombi\n• Ada na malipo\n• Huduma za IT na programu za kompyuta\n• Maeneo ya kampasi na mawasiliano\n\nUnataka kujua nini?"
        ));
        STATIC_KB_SW.put("ccna", Arrays.asList(
                "UCC inaorodhesha kozi za kitaalamu na mafunzo mafupi kama Cisco Certified Network Associate (CCNA). Kwa ratiba za sasa, ada, na tarehe za kujiunga, wasiliana na UCC moja kwa moja au tembelea https://ucc.co.tz/."
        ));
        STATIC_KB_SW.put("professional", Arrays.asList(
                "UCC inaorodhesha kozi za kitaalamu na mafunzo mafupi kama: Project Management Professional (PMP), Certified Information Systems Auditor (CISA), Certified Information Security Manager (CISM), ITIL Foundation, ITIL Practitioner, COBIT Foundation, CCNA, CCNP, CCIP, Java Certified Programmer (JCP), Microsoft Certified Solutions Developer (MCSD), Business Processes Management, Enterprise Architecture for Managers, IT Governance, IT Service Management, Information Security and Risk Management, Ethical Hacking, na Mobile Application Development. Upatikanio, ratiba, ada, na tarehe za kujiunga kinaweza kubadilika. Kwa taarifa za hivi punde, wasiliana na UCC au tembelea https://ucc.co.tz/."
        ));
    }

    @Autowired
    public AIServiceImpl(KnowledgeDocumentRepository knowledgeRepository, FeedbackRepository feedbackRepository) {
        this.knowledgeRepository = knowledgeRepository;
        this.feedbackRepository = feedbackRepository;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Feedback recordFeedback(String sessionId, String messageId, int rating, String comment) {
        Feedback f = new Feedback();
        f.setConversationId(sessionId);
        f.setMessageId(messageId);
        f.setRating(rating);
        f.setComment(comment);
        return feedbackRepository.save(f);
    }

    @Override
    public ChatResponse generateResponse(ChatRequest request, String context) {
        String lowerMessage = request.getMessage().toLowerCase();
        String language = detectLanguage(request.getMessage(), request.getLanguage());

        String activeProgramme = extractActiveProgrammeFromContext(context);
        if (activeProgramme != null && !lowerMessage.contains(activeProgramme.toLowerCase())) {
            lowerMessage = lowerMessage + " " + activeProgramme.toLowerCase();
        }

        Map<String, List<String>> staticKB = "sw".equals(language) ? STATIC_KB_SW : STATIC_KB_EN;

        // Special pre-check: if user is comparing DCIT and DBIT, return the comparison
        // (handled here so it wins over the generic dcit/dbit static entries)
        if (looksLikeProgrammeComparison(lowerMessage, language)) {
            String compareAnswer = staticKB.containsKey("compare")
                ? staticKB.get("compare").get(0)
                : staticKB.containsKey("linganisha") ? staticKB.get("linganisha").get(0) : null;
            if (compareAnswer != null) {
                return ChatResponse.builder()
                        .answer(compareAnswer)
                        .language(language)
                        .conversationId(request.getConversationId())
                        .sources(List.of(Map.of("title", "UCC Static Knowledge Base", "url", "https://ucc.co.tz/")))
                        .confidence(0.75)
                        .escalationRequired(false)
                        .build();
            }
        }

        for (Map.Entry<String, List<String>> entry : staticKB.entrySet()) {
            if (containsKeyword(lowerMessage, entry.getKey())) {
                return ChatResponse.builder()
                        .answer(entry.getValue().get(0))
                        .language(language)
                        .conversationId(request.getConversationId())
                        .sources(List.of(Map.of("title", "UCC Static Knowledge Base", "url", "https://ucc.co.tz/")))
                        .confidence(0.7)
                        .escalationRequired(false)
                        .build();
            }
        }

        // Decide whether to call the LLM or fall back to KB-only
        boolean hasKey = aiApiKey != null && !aiApiKey.isBlank();
        boolean usePollinationsFree = "pollinations".equalsIgnoreCase(provider) || !hasKey;

        if (!hasKey && !usePollinationsFree) {
            // No key, no free provider: KB fallback
            return kbOnlyResponse(language, request.getConversationId());
        }

        try {
            String fullSystemPrompt = SYSTEM_PROMPT
                    + (context != null && !context.isBlank()
                        ? "\n\nCONTEXT (verified UCC information):\n" + context
                        : "");
            String reply = usePollinationsFree
                    ? callPollinations(fullSystemPrompt, request.getMessage(), language)
                    : callOpenAI(fullSystemPrompt, request.getMessage());

            return ChatResponse.builder()
                    .answer(reply)
                    .language(language)
                    .conversationId(request.getConversationId())
                    .sources(List.of(Map.of("title", "UCC AI Assistant", "url", "https://ucc.co.tz/")))
                    .confidence(0.85)
                    .escalationRequired(false)
                    .build();
        } catch (Exception e) {
            return kbOnlyResponse(language, request.getConversationId());
        }
    }

    private ChatResponse kbOnlyResponse(String language, String conversationId) {
        String msg = "sw".equals(language)
                ? "Samahani, sina taarifa maalum kuhusu hilo kwa sasa, lakini timu yetu ya UCC itafurahi kukusaidia. Unaweza kuwasiliana nasi kwa info@ucc.co.tz au +255 22 2410641/5, au tembelea https://ucc.co.tz/."
                : "I don't have that specific detail at hand, but our team at UCC will be happy to help. You can reach them at info@ucc.co.tz or +255 22 2410641/5, or visit https://ucc.co.tz/.";
        return ChatResponse.builder()
                .answer(msg)
                .language(language)
                .conversationId(conversationId)
                .sources(List.of(Map.of("title", "UCC Knowledge Base", "url", "https://ucc.co.tz/")))
                .confidence(0.0)
                .escalationRequired(true)
                .build();
    }

    /**
     * Call Pollinations.ai free OpenAI-compatible endpoint. No API key required.
     * Endpoint: https://text.pollinations.ai/openai
     */
    private String callPollinations(String systemPrompt, String userMessage, String language) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "model", "openai",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "max_tokens", 600,
                "temperature", 0.4
        ));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://text.pollinations.ai/openai"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("Pollinations status " + resp.statusCode() + ": " + resp.body().substring(0, Math.min(200, resp.body().length())));
        }
        JsonNode root = objectMapper.readTree(resp.body());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText();
        }
        throw new RuntimeException("Pollinations returned no choices");
    }

    private String callOpenAI(String systemPrompt, String userMessage) throws Exception {
        String body = String.format(
                "{\"model\":\"%s\",\"messages\":[{\"role\":\"system\",\"content\":\"%s\"},{\"role\":\"user\",\"content\":\"%s\"}],\"max_tokens\":1024,\"temperature\":0.4}",
                model,
                escapeJson(systemPrompt),
                escapeJson(userMessage)
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(aiApiUrl + "/chat/completions"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + aiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("OpenAI status " + resp.statusCode());
        }
        JsonNode root = objectMapper.readTree(resp.body());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText();
        }
        throw new RuntimeException("OpenAI returned no choices");
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractActiveProgrammeFromContext(String context) {
        if (context == null || context.isBlank()) return null;
        String lower = context.toLowerCase();
        for (String code : PROGRAMME_CODES) {
            if (lower.contains(code.toLowerCase())) {
                return code;
            }
        }
        return null;
    }

    private String detectLanguage(String message, String explicitLanguage) {
        if (explicitLanguage != null && !explicitLanguage.isBlank()) {
            return explicitLanguage;
        }

        String lower = message.toLowerCase();

        if (containsKeywordWord(lower, "habari") || containsKeywordWord(lower, "hujambo") || containsKeywordWord(lower, "salamu") ||
            containsKeywordWord(lower, "mambo") || containsKeywordWord(lower, "vipi") || containsKeywordWord(lower, "asante") ||
            containsKeywordWord(lower, "shukrani") || containsKeywordWord(lower, "kwaheri") || containsKeywordWord(lower, "msaada")) {
            return "sw";
        }

        if (containsKeywordWord(lower, "hello") || lower.startsWith("hi ") || lower.equals("hi")
                || lower.contains("good morning") || lower.contains("good afternoon")) {
            return "en";
        }

        Set<String> swahiliIndicators = Set.of(
                "nina", "kwa", "vya", "omba", "sasa", "lini", "wapi", "vipi", "kwa nini",
                "kiasi gani", "ada ya", "nambari", "application fee", "ada ya maombi",
                "kujiunga", "nataka", "natafuta", "nimependa", "nimehitaji", "ningependa", "naomba",
                "hii", "hilo", "hizi", "hayo", "kweli", "labda", "kama", "au", "kabla", "baada",
                "mimi", "wewe", "sisi", "ninyi", "huyu", "huyo", "hawa", "ndani", "nje", "karibu",
                "habari", "hapo", "huku", "kule", "chini", "juu", "mbele", "nyuma", "mbali", "moja",
                "mbili", "nini", "kazi", "vyo", "hadi", "kati", "pia", "maombi", "programu", "ada",
                "kozi", "cheti", "diploma", "masomo", "njia", "bei", "gani", "ngapi", "muhimu", "sahihi"
        );

        long swahiliCount = swahiliIndicators.stream()
                .filter(w -> containsKeywordWord(lower, w))
                .count();

        return swahiliCount >= 1 ? "sw" : "en";
    }

    private static boolean containsKeyword(String text, String keyword) {
        if (text == null || keyword == null) return false;
        if (keyword.length() < 3) {
            int idx = 0;
            while ((idx = text.indexOf(keyword, idx)) != -1) {
                boolean startOk = (idx == 0) || !Character.isLetterOrDigit(text.charAt(idx - 1));
                int after = idx + keyword.length();
                boolean endOk = (after >= text.length()) || !Character.isLetterOrDigit(text.charAt(after));
                if (startOk && endOk) return true;
                idx = after;
            }
            return false;
        }
        return text.contains(keyword);
    }

    private static boolean containsKeywordWord(String text, String word) {
        if (text == null || word == null || word.isBlank()) return false;
        if (word.length() < 3) {
            int idx = 0;
            while ((idx = text.indexOf(word, idx)) != -1) {
                boolean startOk = (idx == 0) || !Character.isLetterOrDigit(text.charAt(idx - 1));
                int after = idx + word.length();
                boolean endOk = (after >= text.length()) || !Character.isLetterOrDigit(text.charAt(after));
                if (startOk && endOk) return true;
                idx = after;
            }
            return false;
        }
        return text.contains(word);
    }

    private static boolean looksLikeProgrammeComparison(String text, String language) {
        if (text == null) return false;
        boolean hasDcit = text.contains("dcit");
        boolean hasDbit = text.contains("dbit");
        if (!hasDcit || !hasDbit) return false;
        if ("sw".equals(language)) {
            return containsKeywordWord(text, "bora") || containsKeywordWord(text, "linganisha")
                || containsKeywordWord(text, "tofauti") || text.contains(" vs ") || text.contains(" au ");
        }
        return containsKeywordWord(text, "better") || containsKeywordWord(text, "compare")
            || containsKeywordWord(text, "difference") || containsKeywordWord(text, "versus")
            || containsKeywordWord(text, "which") || containsKeywordWord(text, "choose")
            || text.contains(" vs ") || text.contains(" or ")
            || containsKeywordWord(text, "bora") || containsKeywordWord(text, "ipi");
    }
}
