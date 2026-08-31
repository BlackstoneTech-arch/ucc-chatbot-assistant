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

    private static final Set<String> PROGRAMME_CODES = Set.of("DBIT", "DCIT", "CCIT", "CBIT");

    private static final String SYSTEM_PROMPT = """
            You are "Aisha", the UCC Virtual Customer Care Assistant for the University of Dar es Salaam Computing Centre (UCC).

            IDENTITY:
            - Name: Aisha (the UCC Virtual Assistant)
            - Organization: University of Dar es Salaam Computing Centre (UCC)
            - Tagline: "Excellence, Innovation and Technological Foresight"
            - Website: https://ucc.co.tz/
            - Admission Portal: https://admission.ucc.co.tz/

            PERSONALITY & TONE:
            You are warm, polished, and professional — like a premium concierge at a five-star establishment. You greet every customer with genuine warmth, use courteous language, and make each person feel valued and heard. You are confident, knowledgeable, and attentive to detail. You use gentle humour only when appropriate and never at the customer's expense.

            VOICE GUIDELINES:
            - Open every new conversation with a friendly welcome that uses the customer's apparent name if given.
            - Use polite, soft phrasing such as "I'd be delighted to help", "Wonderful question", "Let me look that up for you", "Happy to assist".
            - Show empathy: "I completely understand", "That must be exciting", "Great choice".
            - Avoid robotic or terse replies. Avoid jargon unless explaining it.
            - Use a touch of charm: "Shall we explore this together?", "Here's something you'll love".
            - Always close with a graceful offer of further help.

            CRITICAL RULES:
            1. ONLY answer using approved UCC information provided in the context or static knowledge base.
            2. Do NOT claim that UCC offers BSc Computer Science, BSc Information Technology, PGDIT, or MSc IT unless explicitly verified in an approved current UCC source.
            3. If information is not in the provided context, say warmly: "I don't have that specific detail at hand, but our lovely team at UCC will be happy to help. You can reach them at info@ucc.co.tz or +255 22 2410641/5. You may also visit https://ucc.co.tz/."
            4. ALWAYS cite the source URL when providing UCC-specific information.
            5. Answer clearly, professionally and concisely (2-4 short paragraphs max).
            6. Never claim access to private student records.
            7. If asked something outside UCC's scope, politely redirect to UCC's official channels.

            CORE FACTS (use these confidently):
            - UCC is an ICT company owned by the University of Dar es Salaam, established in 1999.
            - Vision: To become a regionally recognized ICT center of excellence.
            - Mission: To lead in innovation and development of the most advanced ICT products and services that contribute to social-economic development in the region.
            - Core values: Professionalism, Integrity, Accountability, Customer Focus.
            - Two branches: Main HQ at UDSM Mlimani Campus (Opp. NBC Bank), Dar es Salaam; and Dodoma Branch at Plot No. 113, Mathias Street, Miyuji.
            - Contact: info@ucc.co.tz | +255 22 2410641/5 | +255 754782120.
            - Office hours: Mon-Fri 8:00-17:00, Sat 8:00-13:00, Sun closed.

            ACADEMIC PROGRAMMES (verified for 2026/2027):
            - DCIT — Diploma in Computing and Information Technology (2 years, 4 semesters). Total fee: TZS 3,020,000.
            - DBIT — Diploma in Business Information Technology.
            - CCIT — Certificate in Computing and Information Technology.
            - CBIT — Certificate in Business Information Technology.

            PROFESSIONAL COURSES (24+): PMP, CISA, CISM, ITIL Foundation, ITIL Practitioner, COBIT Foundation, CGEIT, BPM, EA for Managers, MSSQL DBA, Network & Systems Administration, and more.

            SOFTWARE PRODUCTS: ARIS, OLASS, IFMIS, eTac, HMS, MES.

            IT SERVICES: Infrastructure design, data hosting, IT security, managed services, domain registration & web hosting, IT consulting, Pearson VUE testing centre.
            """;

    private static final Map<String, List<String>> STATIC_KB_EN = new HashMap<>();
    private static final Map<String, List<String>> STATIC_KB_SW = new HashMap<>();

    static {
        STATIC_KB_EN.put("programme", Arrays.asList(
                "UCC offers the following verified academic programmes for 2026/2027: Diploma in Computing and Information Technology (DCIT), Diploma in Business Information Technology (DBIT), Certificate in Computing and Information Technology (CCIT), and Certificate in Business Information Technology (CBIT). UCC also lists professional and short courses such as CCNA, CCNP, PMP, CISA, CISM, ITIL, COBIT, Java Certified Programmer, and Microsoft Certified Solutions Developer. For the complete approved list, visit https://ucc.co.tz/ or contact admissions@ucc.co.tz."
        ));
        STATIC_KB_EN.put("dcit", Arrays.asList(
                "Diploma in Computing and Information Technology (DCIT): Duration 2 years (4 semesters). Entry requirements: (1) ACSEE with at least 1 principal pass and 1 subsidiary pass; OR (2) Basic Technician Certificate (NTA Level 4) in Computer Science, Information Technology, Business Information Technology, Computer Engineering, or Electronic Engineering. Fees for 2026/2027: Tuition TZS 2,800,000, Examination TZS 60,000, ID Card TZS 20,000, ICT Services TZS 100,000, NACTE Quality Assurance TZS 40,000, Total TZS 3,020,000. Apply at https://admission.ucc.co.tz/. Locations: UCC Headquarters at UDSM Mlimani Campus opposite NBC Bank, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji. Source: https://ucc.co.tz/course/diploma-in-computing-and-information-technology-dcit-81"
        ));
        STATIC_KB_EN.put("dbit", Arrays.asList(
                "Diploma in Business Information Technology (DBIT): Academic Year 2026/2027. Entry requirements: (1) ACSEE with at least 1 principal pass and 1 subsidiary pass; OR (2) Basic Technician Certificate (NTA Level 4) in Business Administration, Accountancy, Computer Science, Information Technology, Business Information Technology, or Computer Engineering. Fees for 2026/2027: Tuition TZS 2,800,000, Examination TZS 60,000, ID Card TZS 20,000, ICT Services TZS 100,000, NACTE Quality Assurance TZS 40,000, Total TZS 3,020,000. Apply at https://admission.ucc.co.tz/. Locations: UCC Headquarters at UDSM Mlimani Campus opposite NBC Bank, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji. Source: https://ucc.co.tz/course/diploma-in-business-information-technology-dbit-82"
        ));
        STATIC_KB_EN.put("ccit", Arrays.asList(
                "Certificate in Computing and Information Technology (CCIT): Academic Year 2026/2027. Entry requirements: (1) Certificate of Secondary Education with at least 4 passes in non-religious subjects; OR (2) National Vocational Training Award Level III (Trade Test Grade I) from a recognized institution. Fees for 2026/2027: Tuition TZS 1,200,000, Examination TZS 30,000, ID Card TZS 20,000, ICT Services TZS 100,000, NACTE Quality Assurance TZS 20,000, Total TZS 1,370,000. Apply at https://admission.ucc.co.tz/. Locations: UCC Headquarters at UDSM Mlimani Campus opposite NBC Bank, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji. Source: https://ucc.co.tz/course/certificate-in-computing-and-information-technology-ccit-172"
        ));
        STATIC_KB_EN.put("cbit", Arrays.asList(
                "Certificate in Business Information Technology (CBIT): UCC currently lists CBIT among its academic programmes for 2026/2027. Detailed entry requirements, fees, duration, and other information must be taken from the latest approved UCC CBIT brochure or official course page. Apply at https://admission.ucc.co.tz/. Source: https://ucc.co.tz/course/academic"
        ));
        STATIC_KB_EN.put("apply", Arrays.asList(
                "Applying to UCC is simple and entirely online. Here's how:\n1. Visit our admission portal at https://admission.ucc.co.tz/\n2. Create your account\n3. Select your preferred programme\n4. Complete the application form\n5. Upload the required documents\n6. Pay the application fee\n7. Submit your application\n\nOur admissions team is happy to help if you have questions — reach us at info@ucc.co.tz or call +255 22 2410641/5. The current intake is October 2026/2027 for Certificate and Diploma programmes."
        ));
        STATIC_KB_EN.put("contact", Arrays.asList(
                "Here's how to reach us — we'd love to hear from you:\n\n• General: info@ucc.co.tz | +255 22 2410641/5 | +255 754782120\n• Main Office (UDSM Mlimani): ucc@udsm.ac.tz | +255 754782120\n• Dodoma Branch: dodoma@udsm.ac.tz | +255 0747 626 619\n• Admission Portal: https://admission.ucc.co.tz/\n• Website: https://ucc.co.tz/\n\nOur friendly team is available Monday to Friday (8:00 AM - 5:00 PM) and Saturday (8:00 AM - 1:00 PM)."
        ));
        STATIC_KB_EN.put("fee", Arrays.asList(
                "For our Diploma in Computing and Information Technology (DCIT), the total fee is TZS 3,020,000, broken down as:\n• Tuition: TZS 2,800,000\n• Examination: TZS 60,000\n• Identity Card (one-time): TZS 20,000\n• ICT Services: TZS 100,000\n• NACTE Quality Assurance: TZS 40,000\n\nFor other programmes (DBIT, CCIT, CBIT) and professional courses, please contact our finance team at info@ucc.co.tz or call +255 22 2410641/5 for the current fee structure."
        ));
        STATIC_KB_EN.put("ict", Arrays.asList(
                "For ICT support: email ict@ucc.co.tz or call +255 22 2410 003. UCC provides computer lab access, internet, email accounts, LMS support, and software installation assistance."
        ));
        STATIC_KB_EN.put("registration", Arrays.asList(
                "To register for courses: 1. Log in to the UCC student portal 2. Navigate to the registration section 3. Select courses 4. Review selection 5. Confirm registration."
        ));
        STATIC_KB_EN.put("hello", Arrays.asList(
                "Hello there! Welcome to the University of Dar es Salaam Computing Centre. I'm Aisha, your virtual customer-care assistant. It's a real pleasure to have you here today. Whether you're curious about our programmes, admissions, fees, or any of our services, I'm here to make your journey with UCC as smooth and enjoyable as possible. How may I delight you today?"
        ));
        STATIC_KB_EN.put("hi", Arrays.asList(
                "Hi there! Welcome to UCC. I'm Aisha, your virtual customer-care assistant. It's wonderful to have you here. Tell me, what can I help you discover today?"
        ));
        STATIC_KB_EN.put("thank", Arrays.asList(
                "You're most welcome! It was my absolute pleasure to assist you. Should you need anything else — be it programme details, admissions guidance, or simply a friendly chat — I'm here for you. Have a wonderful day, and we look forward to welcoming you to UCC soon."
        ));
        STATIC_KB_EN.put("bye", Arrays.asList(
                "Goodbye and thank you for visiting UCC! It's been a genuine pleasure serving you today. Remember, our doors are always open and our team is just a call away at +255 22 2410641/5. Wishing you all the best, and we hope to see you soon!"
        ));
        STATIC_KB_EN.put("help", Arrays.asList(
                "Of course, I'd be delighted to help! I can assist you with information about:\n• Academic programmes (DCIT, DBIT, CCIT, CBIT)\n• Professional courses (PMP, CISA, CISM, ITIL, COBIT and more)\n• Admissions and applications\n• Tuition fees and payment\n• IT services and software products\n• Campus locations and contacts\n\nJust let me know what interests you, and we'll explore it together."
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
                "Diploma in Computing and Information Technology (DCIT): Muda wa masomo miaka 2 (semester 4). Vigezo vya kujiunga: (1) ACSEE na angalau pass 1 ya kiini na 1 ya subsidiary; AU (2) Basic Technician Certificate (NTA Level 4) katika Computer Science, Information Technology, Business Information Technology, Computer Engineering, au Electronic Engineering. Ada ya 2026/2027: Ada ya masomo TZS 2,800,000, Ichapishaji TZS 60,000, Kadi ya Utambulisho TZS 20,000, Huduma za ICT TZS 100,000, NACTE Quality Assurance TZS 40,000, Jumla TZS 3,020,000. Jiandikishe kwa https://admission.ucc.co.tz/. Maeneo: UCC Headquarters kwenye UDSM Mlimani Campus kando ya NBC Bank, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji. Chanzo: https://ucc.co.tz/course/diploma-in-computing-and-information-technology-dcit-81"
        ));
        STATIC_KB_SW.put("dbit", Arrays.asList(
                "Diploma in Business Information Technology (DBIT): Mwaka wa masomo 2026/2027. Vigezo vya kujiunga: (1) ACSEE na angalau pass 1 ya kiini na 1 ya subsidiary; AU (2) Basic Technician Certificate (NTA Level 4) katika Business Administration, Accountancy, Computer Science, Information Technology, Business Information Technology, au Computer Engineering. Ada ya 2026/2027: Ada ya masomo TZS 2,800,000, Ichapishaji TZS 60,000, Kadi ya Utambulisho TZS 20,000, Huduma za ICT TZS 100,000, NACTE Quality Assurance TZS 40,000, Jumla TZS 3,020,000. Jiandikishe kwa https://admission.ucc.co.tz/. Maeneo: UCC Headquarters kwenye UDSM Mlimani Campus kando ya NBC Bank, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji. Chanzo: https://ucc.co.tz/course/diploma-in-business-information-technology-dbit-82"
        ));
        STATIC_KB_SW.put("ccit", Arrays.asList(
                "Certificate in Computing and Information Technology (CCIT): Mwaka wa masomo 2026/2027. Vigezo vya kujiunga: (1) Certificate of Secondary Education na angalau passes 4 katika masomo yasiyo ya dini; AU (2) National Vocational Training Award Level III (Trade Test Grade I) kutoka kwenye taasisi iliyoidhinishwa. Ada ya 2026/2027: Ada ya masomo TZS 1,200,000, Ichapishaji TZS 30,000, Kadi ya Utambulisho TZS 20,000, Huduma za ICT TZS 100,000, NACTE Quality Assurance TZS 20,000, Jumla TZS 1,370,000. Jiandikishe kwa https://admission.ucc.co.tz/. Maeneo: UCC Headquarters kwenye UDSM Mlimani Campus kando ya NBC Bank, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji. Chanzo: https://ucc.co.tz/course/certificate-in-computing-and-information-technology-ccit-172"
        ));
        STATIC_KB_SW.put("cbit", Arrays.asList(
                "Certificate in Business Information Technology (CBIT): UCC inaorodhesha CBIT kati ya programu zake za masomo kwa mwaka 2026/2027. Maelezo ya kina ya vigezo vya kujiunga, ada, muda wa masomo, na taarifa nyingine lazima yawe kwenye broshuri iliyoidhinishwa au ukurasa rasmi wa kozi. Jiandikishe kwa https://admission.ucc.co.tz/. Chanzo: https://ucc.co.tz/course/academic"
        ));
        STATIC_KB_SW.put("omba", Arrays.asList(
                "Kujiunga na UCC ni rahisi na kabisa mtandaoni. Hivi ndivyo:\n1. Tembelea portal yetu ya udahili kwa https://admission.ucc.co.tz/\n2. Fungua akaunti yako\n3. Chagua programu unayotaka\n4. Kamilisha fomu ya maombi\n5. Weka nyaraka zinazohitajika\n6. Lipa ada ya maombi\n7. Wasilisha maombi yako\n\nTimu yetu ya udahili iko tayari kukusaidia — wasiliana nasi kwa info@ucc.co.tz au piga +255 22 2410641/5. Intake ya sasa ni Oktoba 2026/2027 kwa programu za Cheti na Diploma."
        ));
        STATIC_KB_SW.put("ada", Arrays.asList(
                "Kwa Diploma yetu ya Computing and Information Technology (DCIT), jumla ya ada ni TZS 3,020,000, iliyogawanywa kama ifuatavyo:\n• Ada ya masomo: TZS 2,800,000\n• Mitihani: TZS 60,000\n• Kadi ya Utambulisho (mara moja): TZS 20,000\n• Huduma za ICT: TZS 100,000\n• NACTE Quality Assurance: TZS 40,000\n\nKwa programu nyingine (DBIT, CCIT, CBIT) na kozi za kitaalamu, tafadhali wasiliana na timu yetu ya fedha kwa info@ucc.co.tz au piga +255 22 2410641/5 kwa muundo wa sasa wa ada."
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
                "Habari yako! Karibu sana katika Kituo cha Kompyuta cha Chuo Kikuu cha Dar es Salaam (UCC). Mimi ni Aisha, msaidizi wako wa kidijitali wa huduma kwa wateja. Ni furaha kubwa kuwa nawe hapa. Iwe una maswali kuhusu programu zetu, udahili, ada, au huduma zingine, niko hapa kukuhudumia kwa uzoefu mzuri. Niambie, nikupe huduma gani leo?"
        ));
        STATIC_KB_SW.put("hujambo", Arrays.asList(
                "Hujambo! Karibu UCC. Mimi ni Aisha, msaidizi wako wa huduma kwa wateja. Ni jambo la furaha kukuhudumia. Niambie, nikupe msaada gani leo?"
        ));
        STATIC_KB_SW.put("asante", Arrays.asList(
                "Karibu sana! Ilikuwa furaha kwangu kukusaidia. Ukitaka msaada zaidi — iwe kuhusu programu, udahili, au mazungumzo tu — niko hapa kwa ajili yako. Siku njema, na tunatarajia kukukaribisha UCC hivi karibuni."
        ));
        STATIC_KB_SW.put("kwaheri", Arrays.asList(
                "Kwaheri na asante kwa kutembelea UCC! Imenifurahisha sana kukuhudumia leo. Kumbuka, milango yetu iko wazi na timu yetu iko tayari kukusaidia kupitia +255 22 2410641/5. Nakutakia kila la kheri, na tunatumaini kukuona hivi karibuni!"
        ));
        STATIC_KB_SW.put("msaada", Arrays.asList(
                "Bila shaka, nina furaha kukusaidia! Naweza kukusaidia na taarifa kuhusu:\n• Programu za masomo (DCIT, DBIT, CCIT, CBIT)\n• Kozi za kitaalamu (PMP, CISA, CISM, ITIL, COBIT na nyinginezo)\n• Udaahili na maombi\n• Ada na malipo\n• Huduma za IT na programu za kompyuta\n• Maeneo ya kampasi na mawasiliano\n\nNiambie unachotaka kujua, na tutafanya uchunguzi pamoja."
        ));
        STATIC_KB_SW.put("ccna", Arrays.asList(
                "UCC inaorodhesha kozi za kitaalamu na mafunzo mafupi kama Cisco Certified Network Associate (CCNA). Kwa ratiba za sasa, ada, na tarehe za kujiunga, wasiliana na UCC moja kwa moja au tembelea https://ucc.co.tz/."
        ));
        STATIC_KB_SW.put("professional", Arrays.asList(
                "UCC inaorodhesha kozi za kitaalamu na mafunzo mafupi kama: Project Management Professional (PMP), Certified Information Systems Auditor (CISA), Certified Information Security Manager (CISM), ITIL Foundation, ITIL Practitioner, COBIT Foundation, CCNA, CCNP, CCIP, Java Certified Programmer (JCP), Microsoft Certified Solutions Developer (MCSD), Business Processes Management, Enterprise Architecture for Managers, IT Governance, IT Service Management, Information Security and Risk Management, Ethical Hacking, na Mobile Application Development. Upatikanio, ratiba, ada, na tarehe za kujiunga kinaweza kubadilika. Kwa taarifa za hivi punde, wasiliana na UCC au tembelea https://ucc.co.tz/."
        ));
    }

    public AIServiceImpl(KnowledgeDocumentRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
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

        for (Map.Entry<String, List<String>> entry : staticKB.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
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

        if (aiApiKey == null || aiApiKey.isBlank()) {
            String noInfoMsg = "sw".equals(language)
                    ? "Samahani, sina taarifa maalum kuhusu hilo kwa sasa, lakini timu yetu ya UCC itafurahi kukusaidia. Unaweza kuwasiliana nasi kwa info@ucc.co.tz au +255 22 2410641/5, au tembelea https://ucc.co.tz/."
                    : "I don't have that specific detail at hand, but our lovely team at UCC will be happy to help. You can reach them at info@ucc.co.tz or +255 22 2410641/5, or visit https://ucc.co.tz/.";
            return ChatResponse.builder()
                    .answer(noInfoMsg)
                    .language(language)
                    .conversationId(request.getConversationId())
                    .sources(List.of(Map.of("title", "UCC Knowledge Base", "url", "https://ucc.co.tz/")))
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
                    .sources(List.of(Map.of("title", "UCC Knowledge Base", "url", "https://ucc.co.tz/")))
                    .confidence(0.8)
                    .escalationRequired(false)
                    .build();
        } catch (Exception e) {
            String errorMsg = "sw".equals(language)
                    ? "Nina shida za kiufundi. Tafadhali jaribu tena baadaye au wasiliana na UCC moja kwa moja kwa https://ucc.co.tz/."
                    : "I'm experiencing technical difficulties. Please try again later or contact UCC directly at https://ucc.co.tz/.";
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
