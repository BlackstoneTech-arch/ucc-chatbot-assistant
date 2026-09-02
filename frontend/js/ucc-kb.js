// ============================================
// UCC AI ASSISTANT — Client-side Static Knowledge Base
// Used as a fallback when the backend API is unreachable.
// All data verified against https://ucc.co.tz/
// ============================================

const UCC_KB_EN = {
  hello: "Hello! Welcome to the University of Dar es Salaam Computing Centre. I'm the UCC AI Assistant. How can I help you today?",
  hi: "Hello! I'm the UCC AI Assistant. How can I help you today?",
  thank: "You're welcome. If you have any other questions about UCC programmes, admissions, or services, feel free to ask.",
  bye: "Goodbye. For further assistance, please contact UCC at info@ucc.co.tz or +255 22 2410641/5.",
  help: "I can help you with information about:\n• Academic programmes (DCIT, DBIT, CCIT, CBIT)\n• Professional courses (PMP, CISA, CISM, ITIL, COBIT and more)\n• Admissions and applications\n• Tuition fees and payment\n• IT services and software products\n• Campus locations and contacts\n\nWhat would you like to know?",

  programme: "UCC offers the following verified academic programmes for 2026/2027:\n\n• Diploma in Computing and Information Technology (DCIT) — 2 years, 4 semesters\n• Diploma in Business Information Technology (DBIT)\n• Certificate in Computing and Information Technology (CCIT)\n• Certificate in Business Information Technology (CBIT)\n\nUCC also offers 24+ professional courses including PMP, CISA, CISM, ITIL, COBIT, CGEIT, Microsoft SQL Server DBA, and many more.\n\nFor the complete approved list, visit https://ucc.co.tz/course or contact us at info@ucc.co.tz.\n\nSource: https://ucc.co.tz/",

  dcit: "Diploma in Computing and Information Technology (DCIT):\n\n• Duration: 2 years (4 semesters)\n• Description: Follows both NACTE and CCNA based curricula. Practical-oriented, competence-based programme.\n\nEntry Requirements (any one):\n• ACSEE with at least 1 principal pass and 1 subsidiary pass, OR\n• Basic Technician Certificate (NTA Level 4) in Computer Science, Information Technology, Business Information Technology, Computer Engineering, or Electronic Engineering\n\nFees (Total TZS 3,020,000):\n• Tuition: TZS 2,800,000\n• Examination: TZS 60,000\n• ID Card: TZS 20,000\n• ICT Services: TZS 100,000\n• NACTE Quality Assurance: TZS 40,000\n\nApply at https://admission.ucc.co.tz/\nLocations: UCC HQ at UDSM Mlimani Campus, and Dodoma Branch.\n\nSource: https://ucc.co.tz/course/diploma-in-computing-and-information-technology-dcit-81",

  dbit: "Diploma in Business Information Technology (DBIT):\n\nCombines business management with information technology skills.\n\nEntry Requirements (any one):\n• ACSEE with at least 1 principal pass and 1 subsidiary pass, OR\n• Basic Technician Certificate (NTA Level 4) in relevant fields\n\nApply at https://admission.ucc.co.tz/\n\nSource: https://ucc.co.tz/course/diploma-in-business-information-technology-dbit-82",

  ccit: "Certificate in Computing and Information Technology (CCIT):\n\nA foundational programme for entry into the ICT field.\n\nEntry Requirements:\n• Certificate of Secondary Education Examination (CSEE) with passes in relevant subjects\n\nApply at https://admission.ucc.co.tz/\n\nSource: https://ucc.co.tz/course/certificate-in-computing-and-information-technology-ccit-172",

  cbit: "Certificate in Business Information Technology (CBIT):\n\nFoundation programme combining basic business concepts with information technology skills.\n\nEntry Requirements:\n• Certificate of Secondary Education Examination (CSEE) with passes in relevant subjects\n\nApply at https://admission.ucc.co.tz/\n\nSource: https://ucc.co.tz/course/academic",

  apply: "Applying to UCC is simple and entirely online:\n\n1. Visit https://admission.ucc.co.tz/\n2. Create your account\n3. Select your preferred programme\n4. Complete the application form\n5. Upload the required documents\n6. Pay the application fee\n7. Submit your application\n\nContact: info@ucc.co.tz or +255 22 2410641/5. The current intake is October 2026/2027 for Certificate and Diploma programmes.\n\nSource: https://ucc.co.tz/",

  admission: "Admissions to UCC for the October 2026/2027 intake are now open for Certificate and Diploma programmes. All applications are submitted online through https://admission.ucc.co.tz/\n\nAvailable programmes:\n• DCIT — Diploma in Computing and Information Technology (2 years)\n• DBIT — Diploma in Business Information Technology\n• CCIT — Certificate in Computing and Information Technology\n• CBIT — Certificate in Business Information Technology\n\nSource: https://ucc.co.tz/news",

  fee: "For the Diploma in Computing and Information Technology (DCIT), the total fee is TZS 3,020,000:\n• Tuition: TZS 2,800,000\n• Examination: TZS 60,000\n• Identity Card (one-time): TZS 20,000\n• ICT Services: TZS 100,000\n• NACTE Quality Assurance: TZS 40,000\n\nFor other programmes (DBIT, CCIT, CBIT) and professional courses, contact info@ucc.co.tz or +255 22 2410641/5.\n\nSource: https://ucc.co.tz/course/diploma-in-computing-and-information-technology-dcit-81",

  contact: "UCC contact information:\n\n• General: info@ucc.co.tz | +255 22 2410641/5 | +255 754782120\n• Main Office (UDSM Mlimani): ucc@udsm.ac.tz | +255 754782120\n• Dodoma Branch: dodoma@udsm.ac.tz | +255 0747 626 619\n• Admission Portal: https://admission.ucc.co.tz/\n• Website: https://ucc.co.tz/\n\nOffice hours: Mon-Fri 8:00-17:00, Sat 8:00-13:00, Sun closed.\n\nSource: https://ucc.co.tz/contact-us",

  registration: "Course registration at UCC:\n\n1. Visit the UCC admission portal at https://admission.ucc.co.tz/\n2. Log in with your account credentials\n3. Navigate to the course registration section\n4. Select your preferred courses from the available list\n5. Review your course selection\n6. Confirm and submit your registration\n\nIf you need help:\n• Email: info@ucc.co.tz\n• Phone: +255 22 2410641/5\n• Mobile: +255 754782120\n\nIn person:\n• Main Office: UDSM Mlimani Campus, Opp. NBC Bank, Dar es Salaam\n• Dodoma Branch: Plot No. 113, Mathias Street, Miyuji\n\nSource: https://ucc.co.tz/",

  register: "Course registration at UCC:\n\n1. Visit the UCC admission portal at https://admission.ucc.co.tz/\n2. Log in with your account credentials\n3. Navigate to the course registration section\n4. Select your preferred courses from the available list\n5. Review your course selection\n6. Confirm and submit your registration\n\nContact: info@ucc.co.tz or +255 22 2410641/5.\n\nSource: https://ucc.co.tz/",

  ict: "For ICT support: email info@ucc.co.tz or call +255 22 2410641/5. UCC provides computer lab access, internet, email accounts, LMS support, and software installation assistance.\n\nSource: https://ucc.co.tz/",

  professional: "UCC offers 24+ professional courses including:\n\nManagerial: PMP, CISA, CISM, ITIL Foundation, ITIL Practitioner, COBIT Foundation, CGEIT, Business Processes Management, Enterprise Architecture for Managers\n\nTechnical: Microsoft SQL Server DBA, Network and Systems Administration, Microsoft Certifications (MCSA, MCSE, MCDA), Cisco Certifications (CCNA, CCNP, CCIP)\n\nUCC is an Authorised Pearson VUE Testing Centre.\n\nSource: https://ucc.co.tz/course/professional",

  ccna: "Cisco Certified Network Associate (CCNA) is one of our professional courses. UCC is an Authorised Pearson VUE Testing Centre.\n\nContact: info@ucc.co.tz or +255 22 2410641/5.\n\nSource: https://ucc.co.tz/course/professional",

  pmp: "Project Management Professional (PMP) is one of our professional certification courses.\n\nContact: info@ucc.co.tz or +255 22 2410641/5.\n\nSource: https://ucc.co.tz/course/project-management-professional-pmp",

  cisa: "Certified Information Systems Auditor (CISA) is one of our professional certification courses.\n\nContact: info@ucc.co.tz or +255 22 2410641/5.\n\nSource: https://ucc.co.tz/course/certified-information-systems-auditor-cisa",

  cism: "Certified Information Security Manager (CISM) is one of our professional certification courses.\n\nContact: info@ucc.co.tz or +255 22 2410641/5.\n\nSource: https://ucc.co.tz/course/certified-information-security-manager-cism",

  itil: "ITIL Foundation and ITIL Practitioner are professional certification courses at UCC.\n\nContact: info@ucc.co.tz or +255 22 2410641/5.\n\nSource: https://ucc.co.tz/course/itil-foundation",

  cobit: "COBIT Foundation is a professional certification course at UCC.\n\nContact: info@ucc.co.tz or +255 22 2410641/5.\n\nSource: https://ucc.co.tz/course/cobit-foundation-96",

  software: "UCC develops and provides 6 major software products:\n\n• ARIS — Academic Registration Information System\n• OLASS — Online Application and Selection System\n• IFMIS — Integrated Financial Management Information System\n• eTac — e-Ticketing and Access Control System\n• HMS — Hospital Management System\n• MES — Monitoring and Evaluation System\n\nSource: https://ucc.co.tz/software-product-&-services",

  infrastructure: "UCC provides comprehensive IT Infrastructure services:\n• IT Infrastructure design, installation, and management\n• Data Hosting, Co-location and Cloud Services\n• IT Security solutions\n• IT Managed Services\n• Domain Registration and Web Hosting\n\nIT Consulting: Business Process Improvement, Enterprise Architecture, IT Strategic Planning, Disaster Recovery, Tender Evaluations, and more.\n\nSource: https://ucc.co.tz/it-infrastructure",

  consulting: "UCC IT consulting services:\n• Business Process Improvement\n• Enterprise Architecture Development\n• IT Strategic Planning\n• Disaster Recovery Planning\n• Tender Evaluations\n• IT Project Management\n• Staff Recruitment\n\nSource: https://ucc.co.tz/it-infrastructure",

  testing: "UCC is an Authorised Pearson VUE Testing Centre. Examinations include:\n• Microsoft Certification: MCSA, MCSE, MCDA\n• Cisco Certification: CCNA, CCNP, CCIP\n\nSource: https://ucc.co.tz/it-infrastructure",

  location: "UCC has two branches:\n\n1. Main Office (Headquarters):\n• University of Dar es Salaam, Mlimani Road\n• P.O. Box 35062, Dar es Salaam\n• Located at UDSM Mlimani Campus, Opp. NBC Bank\n• Phone: +255 22 2410641/5 | Mobile: +255 754782120\n• Email: ucc@udsm.ac.tz\n\n2. Dodoma Branch:\n• Plot No. 113, Mathias Street, Miyuji\n• P.O. Box 2501, Dodoma\n• Phone: +255 22 2410641/5 | Mobile: +255 0747 626 619\n• Email: dodoma@udsm.ac.tz\n\nOffice hours: Mon-Fri 8:00-17:00, Sat 8:00-13:00, Sun closed.\n\nSource: https://ucc.co.tz/contact-us",

  vision: "UCC's Vision: To become a regionally recognized ICT center of excellence.\n\nUCC's Mission: To lead in the innovation and development of the most advanced ICT products and services that contribute to the social-economic development in the region.\n\nMotto: Excellence, Innovation and Technological Foresight.\n\nCore values: Professionalism, Integrity, Accountability, Customer Focus.\n\nSource: https://ucc.co.tz/about-us",

  about: "The University of Dar es Salaam Computing Centre (UCC) is an Information and Communication Technology (ICT) company owned by the University of Dar es Salaam (UDSM), established in 1999. UCC's headquarters is at UDSM's Mlimani Campus, with a branch in Dodoma.\n\nUCC provides: IT training, IT infrastructure services, software products, IT consulting, and operates as a Pearson VUE testing centre.\n\nSource: https://ucc.co.tz/about-us"
};

const UCC_KB_SW = {
  habari: "Habari! Karibu katika Kituo cha Kompyuta cha Chuo Kikuu cha Dar es Salaam (UCC). Mimi ni UCC AI Assistant. Naweza kukusaidia nini leo?",
  hujambo: "Hujambo! Mimi ni UCC AI Assistant. Naweza kukusaidia nini leo?",
  asante: "Karibu. Ukiwa na maswali mengine kuhusu programu, udahili, au huduma za UCC, usisite kuuliza.",
  kwaheri: "Kwaheri. Kwa msaada zaidi, wasiliana na UCC kwa info@ucc.co.tz au +255 22 2410641/5.",
  msaada: "Naweza kukusaidia na taarifa kuhusu:\n• Programu za masomo (DCIT, DBIT, CCIT, CBIT)\n• Kozi za kitaalamu (PMP, CISA, CISM, ITIL, COBIT na nyinginezo)\n• Udaahili na maombi\n• Ada na malipo\n• Huduma za IT na programu za kompyuta\n• Maeneo ya kampasi na mawasiliano\n\nUnataka kujua nini?",

  programme: "UCC inatoa programu zifuatazo kwa mwaka wa masomo 2026/2027:\n\n• Diploma in Computing and Information Technology (DCIT) — miaka 2, semester 4\n• Diploma in Business Information Technology (DBIT)\n• Certificate in Computing and Information Technology (CCIT)\n• Certificate in Business Information Technology (CBIT)\n\nPia kozi 24+ za kitaalamu.\n\nTembelea https://ucc.co.tz/course au wasiliana nasi kwa info@ucc.co.tz.\n\nChanzo: https://ucc.co.tz/",

  programu: "UCC inatoa programu zifuatazo:\n• DCIT (Diploma ya miaka 2)\n• DBIT (Diploma)\n• CCIT (Cheti)\n• CBIT (Cheti)\n\nTembelea https://ucc.co.tz/course.\n\nChanzo: https://ucc.co.tz/",

  kozi: "UCC inatoa programu mbalimbali: DCIT, DBIT, CCIT, CBIT, na kozi 24+ za kitaalamu. Tembelea https://ucc.co.tz/course.\n\nChanzo: https://ucc.co.tz/",

  dcit: "Diploma in Computing and Information Technology (DCIT):\n\n• Muda: Miaka 2 (semester 4)\n• Maelezo: Inafuata mtaala wa NACTE na CCNA.\n\nVigezo vya kujiunga:\n• ACSEE na angalau pass 1 ya kiini na 1 ya subsidiary, AU\n• Basic Technician Certificate (NTA Level 4)\n\nAda (Jumla TZS 3,020,000):\n• Ada ya masomo: TZS 2,800,000\n• Mitihani: TZS 60,000\n• Kadi ya Utambulisho: TZS 20,000\n• Huduma za ICT: TZS 100,000\n• NACTE Quality Assurance: TZS 40,000\n\nJiandikishe kwa https://admission.ucc.co.tz/\n\nChanzo: https://ucc.co.tz/course/diploma-in-computing-and-information-technology-dcit-81",

  omba: "Kujiunga na UCC:\n1. Tembelea https://admission.ucc.co.tz/\n2. Fungua akaunti\n3. Chagua programu\n4. Kamilisha fomu\n5. Weka nyaraka\n6. Lipa ada\n7. Wasilisha\n\nIntake ya sasa: Oktoba 2026/2027.\n\nChanzo: https://ucc.co.tz/",

  ada: "DCIT jumla ya ada ni TZS 3,020,000:\n• Ada ya masomo: TZS 2,800,000\n• Mitihani: TZS 60,000\n• Kadi ya Utambulisho: TZS 20,000\n• Huduma za ICT: TZS 100,000\n• NACTE Quality Assurance: TZS 40,000\n\nKwa programu nyingine, wasiliana nasi.\n\nChanzo: https://ucc.co.tz/",

  wasiliana: "Mawasiliano ya UCC:\n\n• Jumla: info@ucc.co.tz | +255 22 2410641/5\n• Ofisi Kuu: ucc@udsm.ac.tz | +255 754782120\n• Tawi la Dodoma: dodoma@udsm.ac.tz | +255 0747 626 619\n• Portal ya Udaahili: https://admission.ucc.co.tz/\n• Tovuti: https://ucc.co.tz/\n\nMasaa: Mon-Fri 8:00-17:00, Sat 8:00-13:00.\n\nChanzo: https://ucc.co.tz/contact-us",

  usajili: "Usajili wa kozi za UCC:\n\n1. Tembelea portal ya udahili https://admission.ucc.co.tz/\n2. Ingia kwa akaunti yako\n3. Nenda kwenye sehemu ya usajili wa kozi\n4. Chagua kozi\n5. Kagua uteuzi\n6. Thibitisha usajili\n\nMsaada: info@ucc.co.tz | +255 22 2410641/5\n\nChanzo: https://ucc.co.tz/"
};

function uccFallbackAnswer(message, lang) {
  const lower = message.toLowerCase().trim();
  const detectedLang = (typeof lang === 'string') ? lang : ((typeof detectLanguage === 'function') ? detectLanguage(message) : 'en');

  const kbs = [
    { kb: UCC_KB_EN, lang: 'en' },
    { kb: UCC_KB_SW, lang: 'sw' }
  ];

  for (const { kb, lang: kbLang } of kbs) {
    for (const key of Object.keys(kb)) {
      if (lower.includes(key)) {
        return {
          answer: kb[key],
          sources: [{ title: "UCC Knowledge Base", url: "https://ucc.co.tz/" }],
          confidence: kbLang === detectedLang ? 0.9 : 0.8,
          escalationRequired: false,
          language: detectedLang
        };
      }
    }
  }

  const fallback = detectedLang === 'sw'
    ? "Samahani, sina taarifa maalum kuhusu hilo kwa sasa. Tafadhali wasiliana na UCC kwa info@ucc.co.tz au +255 22 2410641/5, au tembelea https://ucc.co.tz/."
    : "I couldn't find verified information about that in the UCC knowledge base. I don't want to give you incorrect information. Please contact UCC at info@ucc.co.tz or +255 22 2410641/5, or visit https://ucc.co.tz/.";

  return {
    answer: fallback,
    sources: [{ title: "UCC Knowledge Base", url: "https://ucc.co.tz/" }],
    confidence: 0.0,
    escalationRequired: true,
    language: detectedLang
  };
}
