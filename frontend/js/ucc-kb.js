// ============================================
// UCC AI ASSISTANT — Client-side Static Knowledge Base
// Used as a fallback when the backend API is unreachable.
// All data verified against https://ucc.co.tz/
// ============================================

const UCC_KB_EN = {
  hello: "Hello! 👋 Welcome to the University of Dar es Salaam Computing Centre (UCC). I'm your UCC Customer Care Assistant.\n\nHere's what I can help you with right now:\n• Programmes and fees (DCIT, DBIT, CCIT, CBIT, professional courses)\n• Admissions (open 1 June – 30 Sept 2026, intake September 2026)\n• How to apply, entry requirements, locations\n• Contacts and campus info\n\nJust type your question or pick one of the quick options below.",

  hi: "Hello! I'm your UCC Customer Care Assistant. How can I help you today?",

  thank: "You're very welcome. If you have any other questions about UCC programmes, admissions, or services, feel free to ask.",

  bye: "Goodbye! For further assistance, please contact UCC at info@ucc.co.tz or +255 22 2410641/5.",

  help: "I can help you with information about:\n• Academic programmes (DCIT, DBIT, CCIT, CBIT)\n• Professional courses (PMP, CISA, CISM, ITIL, COBIT and more)\n• Admissions and applications (open 1 June – 30 Sept 2026)\n• Tuition fees and payment\n• IT services and software products\n• Campus locations and contacts\n\nWhat would you like to know?",

  programme: "UCC offers the following verified academic programmes for 2026/2027:\n\n• Diploma in Computing and Information Technology (DCIT) — 2 years\n• Diploma in Business Information Technology (DBIT) — 2 years\n• Certificate in Computing and Information Technology (CCIT) — 1 year\n• Certificate in Business Information Technology (CBIT) — 1 year\n\nUCC also lists 20+ professional and short courses (PMP, CISA, CISM, ITIL, COBIT, CCNA, CCNP, Ethical Hacking, Mobile App Dev, etc.).\n\nFor the complete approved list, visit https://ucc.co.tz/ or contact admissions@ucc.co.tz.",

  dcit: "Diploma in Computing and Information Technology (DCIT):\n• Duration: 2 years (4 semesters)\n• Entry requirements: (1) ACSEE with at least 1 principal pass and 1 subsidiary pass; OR (2) Basic Technician Certificate (NTA Level 4) in Computer Science, Information Technology, Business Information Technology, Computer Engineering, or Electronic Engineering.\n• Fees for 2026/2027 (Total TZS 3,020,000):\n   - Tuition: TZS 2,800,000\n   - Examination: TZS 60,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n• Locations: UCC HQ at UDSM Mlimani Campus (opposite NBC Bank), Dar es Salaam, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji.\n• Apply online: https://admission.ucc.co.tz/\nSource: https://ucc.co.tz/course/diploma-in-computing-and-information-technology-dcit-81",

  dbit: "Diploma in Business Information Technology (DBIT):\n• Duration: 2 years (4 semesters) plus project work\n• Entry requirements: (1) ACSEE with at least 1 principal pass and 1 subsidiary pass; OR (2) Basic Technician Certificate (NTA Level 4) in Business Administration, Accountancy, Computer Science, Information Technology, Business Information Technology, or Computer Engineering.\n• Fees for 2026/2027 (Total TZS 3,020,000):\n   - Tuition: TZS 2,800,000\n   - Examination: TZS 60,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n• Locations: UCC HQ at UDSM Mlimani Campus (opposite NBC Bank), Dar es Salaam, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji.\n• Apply online: https://admission.ucc.co.tz/\nSource: https://ucc.co.tz/course/diploma-in-business-information-technology-dbit-82",

  ccit: "Certificate in Computing and Information Technology (CCIT):\n• Duration: 1 year (2 semesters) plus field work\n• Entry requirements: (1) Certificate of Secondary Education (CSEE) with at least 4 passes in non-religious subjects; OR (2) National Vocational Training Award Level III (Trade Test Grade I) from a recognized institution.\n• Fees for 2026/2027 (Total TZS 1,370,000):\n   - Tuition: TZS 1,200,000\n   - Examination: TZS 30,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n• Locations: UCC HQ at UDSM Mlimani Campus (opposite NBC Bank), Dar es Salaam, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji.\n• Apply online: https://admission.ucc.co.tz/\nSource: https://ucc.co.tz/course/certificate-in-computing-and-information-technology-ccit-172",

  cbit: "Certificate in Business Information Technology (CBIT):\n• Duration: 1 year (2 semesters) plus field work\n• Entry requirements: (1) Certificate of Secondary Education (CSEE) with at least 4 passes in non-religious subjects; OR (2) National Vocational Training Award Level III (Trade Test Grade I) from a recognized institution.\n• Fees for 2026/2027 (Total TZS 1,370,000):\n   - Tuition: TZS 1,200,000\n   - Examination: TZS 30,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n• Locations: UCC HQ at UDSM Mlimani Campus (opposite NBC Bank), Dar es Salaam, and UCC Dodoma Branch at Capital Compound, Mathias Street, Miyuji.\n• Apply online: https://admission.ucc.co.tz/\nSource: https://ucc.co.tz/course/certificate-in-business-information-technology-cbit-173",

  application: "UCC Admissions 2026/2027 — important dates:\n\n• Applications OPEN: 1st June 2026\n• Applications CLOSE: 30th September 2026\n• Intake / classes begin: September 2026\n• Application fee: TZS 10,000 (non-refundable, paid online via mobile money or bank card)\n• Apply at: https://admission.ucc.co.tz/\n\nHow to apply (4 steps):\n1. Visit https://admission.ucc.co.tz/ and create an account with your email and phone number.\n2. Select your preferred programme (DCIT, DBIT, CCIT or CBIT).\n3. Complete the application form and upload the required documents.\n4. Pay the application fee and submit. You will receive a confirmation SMS/email within 24 hours.\n\nRequired documents: CSEE/ACSEE certificates or equivalent, birth certificate, passport-size photo. For help, email admissions@ucc.co.tz or call +255 22 2410641/5 (Mon–Fri 8:00–17:00, Sat 8:00–13:00).",

  join: "Joining UCC is easy. Here's the path:\n\n• Pick your programme: DCIT, DBIT, CCIT or CBIT.\n• Apply online between 1st June 2026 and 30th September 2026 at https://admission.ucc.co.tz/.\n• Pay the TZS 10,000 application fee and upload your certificates.\n• Once selected, complete registration at the campus (UDSM Mlimani in Dar es Salaam, or UCC Dodoma Branch).\n\nRequired documents: CSEE/ACSEE certificates, birth certificate, passport photo. For help, email admissions@ucc.co.tz or call +255 22 2410641/5.",

  apply: "UCC Admissions — 2026/2027 Academic Year:\n\n• Application window: 1st June 2026 — 30th September 2026 (intake: September 2026)\n• Online application portal: https://admission.ucc.co.tz/\n• Application fee: TZS 10,000 (non-refundable, paid online via mobile money or bank card)\n• Required documents: CSEE/ACSEE certificates or equivalent, birth certificate, passport-size photo.\n\nHow to apply (4 steps):\n1. Visit https://admission.ucc.co.tz/ and create an account with your email and phone number.\n2. Select your preferred programme (DCIT, DBIT, CCIT or CBIT).\n3. Complete the application form and upload the required documents.\n4. Pay the application fee and submit. You will receive a confirmation SMS/email within 24 hours.\n\nFor help, email admissions@ucc.co.tz or call +255 22 2410641/5 (Mon–Fri 8:00–17:00, Sat 8:00–13:00).",

  admission: "UCC Admissions 2026/2027 — important dates:\n\n• Applications OPEN: 1st June 2026\n• Applications CLOSE: 30th September 2026\n• Intake / classes begin: September 2026\n• Application fee: TZS 10,000\n• Apply at: https://admission.ucc.co.tz/\n\nEligible programmes and basic requirements:\n• DCIT (Diploma, 2 years) — ACSEE with 1 principal + 1 subsidiary pass, OR NTA Level 4 in CS/IT/BIT/Computer Eng./Electronic Eng.\n• DBIT (Diploma, 2 years) — ACSEE with 1 principal + 1 subsidiary pass, OR NTA Level 4 in Business Admin/Accountancy/CS/IT/BIT/Computer Eng.\n• CCIT (Certificate, 1 year) — CSEE with 4 passes in non-religious subjects, OR NVTA Level III / Trade Test Grade I.\n• CBIT (Certificate, 1 year) — CSEE with 4 passes in non-religious subjects, OR NVTA Level III / Trade Test Grade I.\n\nFor late or special intakes, contact admissions@ucc.co.tz or +255 22 2410641/5. Late applications may be considered if seats are still available.",

  compare: "DCIT vs DBIT — quick comparison:\n\n• DCIT — Diploma in Computing and Information Technology (2 years, total TZS 3,020,000)\n   - Focus: hardware, networking, programming, web, databases, server & CCNA-aligned practicals.\n   - Best for: students who want a strong technical / software-development / network-engineering career path, or who plan to bridge to a CS/IT degree.\n\n• DBIT — Diploma in Business Information Technology (2 years, total TZS 3,020,000)\n   - Focus: business + IT (accounting packages, e-business, web services, business law, entrepreneurship, management).\n   - Best for: students who want to work at the intersection of business and IT — business analyst, IT sales, e-commerce, ERP/SAP, or run their own IT business.\n\nQuick decision guide:\n• Love coding & networks → DCIT\n• Love business + tech → DBIT\n• Not sure yet → DCIT keeps more doors open for degree bridging.",

  fee: "Official UCC fee structure for academic year 2026/2027:\n\n• Diploma in Computing and Information Technology (DCIT) — Total TZS 3,020,000:\n   - Tuition: TZS 2,800,000\n   - Examination: TZS 60,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n\n• Diploma in Business Information Technology (DBIT) — Total TZS 3,020,000:\n   - Tuition: TZS 2,800,000\n   - Examination: TZS 60,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n\n• Certificate in Computing and Information Technology (CCIT) — Total TZS 1,370,000:\n   - Tuition: TZS 1,200,000\n   - Examination: TZS 30,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n\n• Certificate in Business Information Technology (CBIT) — Total TZS 1,370,000:\n   - Tuition: TZS 1,200,000\n   - Examination: TZS 30,000\n   - Identity Card (one-time): TZS 20,000\n   - ICT Services: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n\nFor professional and short courses (PMP, CISA, CISM, ITIL, COBIT, CCNA, CCNP, etc.) please contact info@ucc.co.tz or +255 22 2410641/5 for current fees and intake dates.",

  contact: "Here's how to reach us — we'd love to hear from you:\n\n• General: info@ucc.co.tz | +255 22 2410641/5 | +255 754782120\n• Main Office (UDSM Mlimani): ucc@udsm.ac.tz | +255 754782120\n• Dodoma Branch: dodoma@udsm.ac.tz | +255 0747 626 619\n• Admission Portal: https://admission.ucc.co.tz/\n• Website: https://ucc.co.tz/\n\nOur friendly team is available Monday to Friday (8:00 AM - 5:00 PM) and Saturday (8:00 AM - 1:00 PM).",

  location: "UCC has two branches:\n\n1. Main Office (Headquarters):\n• University of Dar es Salaam, Mlimani Road\n• P.O. Box 35062, Dar es Salaam\n• Located at UDSM Mlimani Campus, Opp. NBC Bank\n• Phone: +255 22 2410641/5 | Mobile: +255 754782120\n• Email: ucc@udsm.ac.tz\n\n2. Dodoma Branch:\n• Plot No. 113, Mathias Street, Miyuji\n• P.O. Box 2501, Dodoma\n• Phone: +255 22 2410641/5 | Mobile: +255 0747 626 619\n• Email: dodoma@udsm.ac.tz\n\nOffice hours: Mon-Fri 8:00-17:00, Sat 8:00-13:00, Sun closed.",

  registration: "Course registration at UCC:\n\n1. Visit the UCC admission portal at https://admission.ucc.co.tz/\n2. Log in with your account credentials\n3. Navigate to the course registration section\n4. Select your preferred courses from the available list\n5. Review your course selection\n6. Confirm and submit your registration\n\nIf you need help:\n• Email: info@ucc.co.tz\n• Phone: +255 22 2410641/5\n• Mobile: +255 754782120\n\nIn person:\n• Main Office: UDSM Mlimani Campus, Opp. NBC Bank, Dar es Salaam\n• Dodoma Branch: Plot No. 113, Mathias Street, Miyuji",

  ict: "For ICT support: email ict@ucc.co.tz or call +255 22 2410 003. UCC provides computer lab access, internet, email accounts, LMS support, and software installation assistance.",

  professional: "UCC offers 20+ professional courses including:\n\nManagerial: PMP, CISA, CISM, ITIL Foundation, ITIL Practitioner, COBIT Foundation, CGEIT, Business Processes Management, Enterprise Architecture for Managers\n\nTechnical: Microsoft SQL Server DBA, Network and Systems Administration, Microsoft Certifications (MCSA, MCSE, MCDA), Cisco Certifications (CCNA, CCNP, CCIP), Ethical Hacking, Mobile Application Development\n\nUCC is an Authorised Pearson VUE Testing Centre.\n\nSource: https://ucc.co.tz/course/professional",

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

  vision: "UCC's Vision: To become a regionally recognized ICT center of excellence.\n\nUCC's Mission: To lead in the innovation and development of the most advanced ICT products and services that contribute to the social-economic development in the region.\n\nMotto: Excellence, Innovation and Technological Foresight.\n\nCore values: Professionalism, Integrity, Accountability, Customer Focus.\n\nSource: https://ucc.co.tz/about-us",

  about: "The University of Dar es Salaam Computing Centre (UCC) is an Information and Communication Technology (ICT) company owned by the University of Dar es Salaam (UDSM), established in 1999. UCC's headquarters is at UDSM's Mlimani Campus, with a branch in Dodoma.\n\nUCC provides: IT training, IT infrastructure services, software products, IT consulting, and operates as a Pearson VUE testing centre.\n\nSource: https://ucc.co.tz/about-us"
};

const UCC_KB_SW = {
  habari: "Habari! 👋 Karibu katika Kituo cha Kompyuta cha Chuo Kikuu cha Dar es Salaam (UCC). Mimi ni Msaidizi wako wa Huduma kwa Wateja wa UCC.\n\nHivi ndivyo ninavyoweza kukusaidia sasa hivi:\n• Programu na ada (DCIT, DBIT, CCIT, CBIT, kozi za kitaalamu)\n• Udaahili (dirisha wazi 1 Juni – 30 Septemba 2026, intake Septemba 2026)\n• Jinsi ya kuomba, vigezo vya kujiunga, maeneo\n• Mawasiliano na taarifa za kampasi\n\nAndika swali lako au chagua chaguo la haraka hapa chini.",

  hujambo: "Hujambo! 👋 Karibu katika UCC. Mimi ni Msaidizi wako wa Huduma kwa Wateja. Naweza kukusaidia na programu, udahili, ada, na huduma nyingine za UCC. Andika swali lako au chagua chaguo la haraka.",

  asante: "Karibu sana. Ukiwa na maswali mengine kuhusu programu, udahili, au huduma za UCC, usisite kuuliza.",

  kwaheri: "Kwaheri. Kwa msaada zaidi, wasiliana na UCC kwa info@ucc.co.tz au +255 22 2410641/5.",

  msaada: "Naweza kukusaidia na taarifa kuhusu:\n• Programu za masomo (DCIT, DBIT, CCIT, CBIT)\n• Kozi za kitaalamu (PMP, CISA, CISM, ITIL, COBIT na nyinginezo)\n• Udaahili na maombi (dirisha wazi 1 Juni – 30 Septemba 2026)\n• Ada na malipo\n• Huduma za IT na programu za kompyuta\n• Maeneo ya kampasi na mawasiliano\n\nUnataka kujua nini?",

  programme: "UCC inatoa programu zifuatazo kwa mwaka wa masomo 2026/2027:\n\n• Diploma in Computing and Information Technology (DCIT) — miaka 2\n• Diploma in Business Information Technology (DBIT) — miaka 2\n• Certificate in Computing and Information Technology (CCIT) — mwaka 1\n• Certificate in Business Information Technology (CBIT) — mwaka 1\n\nPia kozi 20+ za kitaalamu na mafunzo mafupi (PMP, CISA, CISM, ITIL, COBIT, CCNA, CCNP, Ethical Hacking, Mobile App Dev, n.k.).\n\nTembelea https://ucc.co.tz/ au wasiliana na admissions@ucc.co.tz.",

  programu: "Programu za UCC:\n• DCIT (Diploma, miaka 2)\n• DBIT (Diploma, miaka 2)\n• CCIT (Cheti, mwaka 1)\n• CBIT (Cheti, mwaka 1)\n\nTembelea https://ucc.co.tz/.",

  kozi: "UCC inatoa programu mbalimbali: DCIT, DBIT, CCIT, CBIT, na kozi 20+ za kitaalamu. Tembelea https://ucc.co.tz/.",

  dcit: "Diploma in Computing and Information Technology (DCIT):\n• Muda: miaka 2 (semesta 4)\n• Vigezo vya kujiunga: (1) ACSEE na angalau pass 1 ya kiini na 1 ya subsidiary; AU (2) Basic Technician Certificate (NTA Level 4) katika Computer Science, Information Technology, Business Information Technology, Computer Engineering, au Electronic Engineering.\n• Ada ya 2026/2027 (Jumla TZS 3,020,000):\n   - Ada ya masomo: TZS 2,800,000\n   - Mitihani: TZS 60,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n• Maeneo: UCC HQ kwenye UDSM Mlimani Campus (kando ya NBC Bank), Dar es Salaam, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji.\n• Jiandikishe mtandaoni: https://admission.ucc.co.tz/\nChanzo: https://ucc.co.tz/course/diploma-in-computing-and-information-technology-dcit-81",

  dbit: "Diploma in Business Information Technology (DBIT):\n• Muda: miaka 2 (semesta 4) pamoja na mradi\n• Vigezo vya kujiunga: (1) ACSEE na angalau pass 1 ya kiini na 1 ya subsidiary; AU (2) Basic Technician Certificate (NTA Level 4) katika Business Administration, Accountancy, Computer Science, Information Technology, Business Information Technology, au Computer Engineering.\n• Ada ya 2026/2027 (Jumla TZS 3,020,000):\n   - Ada ya masomo: TZS 2,800,000\n   - Mitihani: TZS 60,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n• Maeneo: UCC HQ kwenye UDSM Mlimani Campus (kando ya NBC Bank), Dar es Salaam, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji.\n• Jiandikishe mtandaoni: https://admission.ucc.co.tz/\nChanzo: https://ucc.co.tz/course/diploma-in-business-information-technology-dbit-82",

  ccit: "Certificate in Computing and Information Technology (CCIT):\n• Muda: mwaka 1 (semesta 2) pamoja na kazi ya uwandani\n• Vigezo vya kujiunga: (1) Certificate of Secondary Education (CSEE) na angalau passes 4 katika masomo yasiyo ya dini; AU (2) National Vocational Training Award Level III (Trade Test Grade I) kutoka taasisi iliyoidhinishwa.\n• Ada ya 2026/2027 (Jumla TZS 1,370,000):\n   - Ada ya masomo: TZS 1,200,000\n   - Mitihani: TZS 30,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n• Maeneo: UCC HQ kwenye UDSM Mlimani Campus (kando ya NBC Bank), Dar es Salaam, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji.\n• Jiandikishe mtandaoni: https://admission.ucc.co.tz/\nChanzo: https://ucc.co.tz/course/certificate-in-computing-and-information-technology-ccit-172",

  cbit: "Certificate in Business Information Technology (CBIT):\n• Muda: mwaka 1 (semesta 2) pamoja na kazi ya uwandani\n• Vigezo vya kujiunga: (1) Certificate of Secondary Education (CSEE) na angalau passes 4 katika masomo yasiyo ya dini; AU (2) National Vocational Training Award Level III (Trade Test Grade I) kutoka taasisi iliyoidhinishwa.\n• Ada ya 2026/2027 (Jumla TZS 1,370,000):\n   - Ada ya masomo: TZS 1,200,000\n   - Mitihani: TZS 30,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n• Maeneo: UCC HQ kwenye UDSM Mlimani Campus (kando ya NBC Bank), Dar es Salaam, na UCC Dodoma Branch kwenye Capital Compound, Mathias Street, Miyuji.\n• Jiandikishe mtandaoni: https://admission.ucc.co.tz/\nChanzo: https://ucc.co.tz/course/certificate-in-business-information-technology-cbit-173",

  omba: "Udaahili wa UCC — Mwaka wa Masomo 2026/2027:\n\n• Dirisha la maombi: 1 Juni 2026 — 30 Septemba 2026 (intake: Septemba 2026)\n• Portal ya maombi mtandaoni: https://admission.ucc.co.tz/\n• Ada ya maombi: TZS 10,000 (haitarejeshwa)\n• Nyaraka zinazohitajika: vyeti vya CSEE/ACSEE, cheti cha kuzaliwa, picha ya paspoti.\n\nJinsi ya kuomba (hatua 4):\n1. Tembelea https://admission.ucc.co.tz/ na ufungue akaunti.\n2. Chagua programu (DCIT, DBIT, CCIT au CBIT).\n3. Kamilisha fomu ya maombi na upakie nyaraka.\n4. Lipa ada ya maombi na uwasilishe. Utapokea ujumbe wa kuthibitisha ndani ya masaa 24.\n\nMsaada: admissions@ucc.co.tz au +255 22 2410641/5 (Jumatatu–Ijumaa 8:00–17:00, Jumamosi 8:00–13:00).",

  udahili: "Udaahili wa UCC 2026/2027 — tarehe muhimu:\n\n• Maombi YANAFUNGULIWA: 1 Juni 2026\n• Maombi YANAFUNGWA: 30 Septemba 2026\n• Intake / masomo yanayoanza: Septemba 2026\n• Ada ya maombi: TZS 10,000\n• Tuma maombi kwa: https://admission.ucc.co.tz/\n\nProgramu zinazokubaliwa na vigezo vya msingi:\n• DCIT (Diploma, miaka 2) — ACSEE na pass 1 ya kiini + 1 ya subsidiary, AU NTA Level 4 katika CS/IT/BIT/Computer Eng./Electronic Eng.\n• DBIT (Diploma, miaka 2) — ACSEE na pass 1 ya kiini + 1 ya subsidiary, AU NTA Level 4 katika Business Admin/Accountancy/CS/IT/BIT/Computer Eng.\n• CCIT (Cheti, mwaka 1) — CSEE na passes 4 katika masomo yasiyo ya dini, AU NVTA Level III / Trade Test Grade I.\n• CBIT (Cheti, mwaka 1) — CSEE na passes 4 katika masomo yasiyo ya dini, AU NVTA Level III / Trade Test Grade I.",

  linganisha: "DCIT vs DBIT — kulinganisha kwa ufupi:\n\n• DCIT — Diploma in Computing and Information Technology (miaka 2, jumla TZS 3,020,000)\n   - Lengo: hardware, mitandao, programming, web, databases, server na mazoezi ya CCNA.\n   - Kwa: wanafunzi wanaotaka njia ya kiufundi / software development / network engineering, au wanaopanga kuendelea na shahada ya CS/IT.\n\n• DBIT — Diploma in Business Information Technology (miaka 2, jumla TZS 3,020,000)\n   - Lengo: biashara + IT (accounting packages, e-business, web services, business law, entrepreneurship, management).\n   - Kwa: wanafunzi wanaotaka kufanya kazi katika mwingiliano wa biashara na IT — business analyst, mauzo ya IT, e-commerce, ERP/SAP, au kuendesha biashara yao wenyewe ya IT.\n\nMwongozo wa haraka wa uamuzi:\n• Unapenda coding & mitandao → DCIT\n• Unapenda biashara + teknolojia → DBIT\n• Bado huna uhakika → DCIT inakuweka njia nyingi wazi za kujenga shahada.",

  ada: "Muundo rasmi wa ada wa UCC kwa mwaka wa masomo 2026/2027:\n\n• Diploma in Computing and Information Technology (DCIT) — Jumla TZS 3,020,000:\n   - Ada ya masomo: TZS 2,800,000\n   - Mitihani: TZS 60,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n\n• Diploma in Business Information Technology (DBIT) — Jumla TZS 3,020,000:\n   - Ada ya masomo: TZS 2,800,000\n   - Mitihani: TZS 60,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 40,000\n\n• Certificate in Computing and Information Technology (CCIT) — Jumla TZS 1,370,000:\n   - Ada ya masomo: TZS 1,200,000\n   - Mitihani: TZS 30,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n\n• Certificate in Business Information Technology (CBIT) — Jumla TZS 1,370,000:\n   - Ada ya masomo: TZS 1,200,000\n   - Mitihani: TZS 30,000\n   - Kadi ya Utambulisho (mara moja): TZS 20,000\n   - Huduma za ICT: TZS 100,000\n   - NACTE Quality Assurance: TZS 20,000\n\nKwa kozi za kitaalamu na mafunzo mafupi wasiliana na info@ucc.co.tz au +255 22 2410641/5.",

  wasiliana: "Mawasiliano ya UCC:\n\n• Jumla: info@ucc.co.tz | +255 22 2410641/5 | +255 754782120\n• Ofisi Kuu (UDSM Mlimani): ucc@udsm.ac.tz | +255 754782120\n• Tawi la Dodoma: dodoma@udsm.ac.tz | +255 0747 626 619\n• Portal ya Udaahili: https://admission.ucc.co.tz/\n• Tovuti: https://ucc.co.tz/\n\nMasaa: Mon-Fri 8:00-17:00, Sat 8:00-13:00.",

  usajili: "Usajili wa kozi za UCC:\n\n1. Tembelea portal ya udahili https://admission.ucc.co.tz/\n2. Ingia kwa akaunti yako\n3. Nenda kwenye sehemu ya usajili wa kozi\n4. Chagua kozi\n5. Kagua uteuzi\n6. Thibitisha usajili\n\nMsaada: info@ucc.co.tz | +255 22 2410641/5",

  bora: "DCIT vs DBIT — kulinganisha kwa ufupi:\n\n• DCIT — Diploma in Computing and Information Technology (miaka 2, jumla TZS 3,020,000) — Lengo: hardware, mitandao, programming, web, databases, server na mazoezi ya CCNA.\n• DBIT — Diploma in Business Information Technology (miaka 2, jumla TZS 3,020,000) — Lengo: biashara + IT (accounting packages, e-business, web services, business law, entrepreneurship).\n\nUamuzi:\n• Unapenda coding & mitandao → DCIT\n• Unapenda biashara + teknolojia → DBIT\n• Bado huna uhakika → DCIT inakuweka njia nyingi wazi za kujenga shahada.",

  lini: "Udaahili wa UCC 2026/2027:\n\n• Maombi YANAFUNGULIWA: 1 Juni 2026\n• Maombi YANAFUNGWA: 30 Septemba 2026\n• Intake / masomo yanayoanza: Septemba 2026\n• Ada ya maombi: TZS 10,000\n• Portal: https://admission.ucc.co.tz/",

  "application fee": "Ada ya maombi ya udahili wa UCC kwa mwaka wa masomo 2026/2027:\n\n• Ada ya maombi: TZS 10,000 (haitarejeshwa)\n• Hulipwa mtandaoni kupitia portal ya maombi kwa njia ya mobile money au kadi ya benki\n• Dirisha la maombi: 1 Juni 2026 — 30 Septemba 2026\n• Portal: https://admission.ucc.co.tz/\n\nKwa msaada zaidi kuhusu ada ya maombi, wasiliana na admissions@ucc.co.tz au piga +255 22 2410641/5.",

  ada_ya_maombi: "Ada ya maombi ya udahili wa UCC kwa mwaka wa masomo 2026/2027:\n\n• Ada ya maombi: TZS 10,000 (haitarejeshwa)\n• Hulipwa mtandaoni kupitia portal ya maombi kwa njia ya mobile money au kadi ya benki\n• Dirisha la maombi: 1 Juni 2026 — 30 Septemba 2026\n• Portal: https://admission.ucc.co.tz/\n\nKwa msaada zaidi kuhusu ada ya maombi, wasiliana na admissions@ucc.co.tz au piga +255 22 2410641/5.",

  jiunga: "Kujiunga na UCC ni rahisi:\n\n• Chagua programu yako: DCIT, DBIT, CCIT au CBIT.\n• Omba mtandaoni kati ya 1 Juni 2026 na 30 Septemba 2026 kwenye https://admission.ucc.co.tz/.\n• Lipa ada ya maombi ya TZS 10,000 na upakie vyeti vyako.\n• Ukichaguliwa, kamilisha usajili katika kampasi (UDSM Mlimani Dar es Salaam, au Tawi la UCC Dodoma).\n\nNyaraka zinazohitajika: vyeti vya CSEE/ACSEE, cheti cha kuzaliwa, picha ya paspoti. Msaada: admissions@ucc.co.tz au +255 22 2410641/5."
};

function uccFallbackAnswer(message, lang) {
  const lower = (message || '').toLowerCase().trim();
  const detectedLang = (typeof lang === 'string') ? lang : ((typeof detectLanguage === 'function') ? detectLanguage(message) : 'en');

  // Comparison pre-check: if user mentions DCIT + DBIT + a comparison cue,
  // return the comparison answer instead of the single-programme detail.
  const hasDcit = lower.includes('dcit');
  const hasDbit = lower.includes('dbit');
  if (hasDcit && hasDbit) {
    const enCue = /\b(better|compare|comparison|difference|versus|which|choose|or)\b/.test(lower) || lower.includes(' vs ') || lower.includes(' or ');
    const swCue = /\b(bora|ipi|linganisha|tofauti|au)\b/.test(lower) || lower.includes(' vs ') || lower.includes(' au ');
    const cue = detectedLang === 'sw' ? swCue : enCue;
    if (cue) {
      const kbs = [
        { kb: UCC_KB_EN, lang: 'en' },
        { kb: UCC_KB_SW, lang: 'sw' }
      ];
      for (const { kb, kbLang } of kbs) {
        if (kbLang === detectedLang && kb.compare) {
          return {
            answer: kb.compare,
            sources: [{ title: "UCC Knowledge Base", url: "https://ucc.co.tz/" }],
            confidence: 0.9,
            escalationRequired: false,
            language: detectedLang
          };
        }
      }
      for (const { kb, kbLang } of kbs) {
        if (kbLang === detectedLang && kb.linganisha) {
          return {
            answer: kb.linganisha,
            sources: [{ title: "UCC Knowledge Base", url: "https://ucc.co.tz/" }],
            confidence: 0.85,
            escalationRequired: false,
            language: detectedLang
          };
        }
      }
    }
  }

  const kbs = [
    { kb: UCC_KB_EN, lang: 'en' },
    { kb: UCC_KB_SW, lang: 'sw' }
  ];

  const matches = (text, key) => {
    if (key.length < 4) {
      let idx = 0;
      while ((idx = text.indexOf(key, idx)) !== -1) {
        const startOk = (idx === 0) || !/[a-z0-9]/.test(text.charAt(idx - 1));
        const after = idx + key.length;
        const endOk = (after >= text.length) || !/[a-z0-9]/.test(text.charAt(after));
        if (startOk && endOk) return true;
        idx = after;
      }
      return false;
    }
    return text.includes(key);
  };

  for (const { kb, lang: kbLang } of kbs) {
    for (const key of Object.keys(kb)) {
      if (matches(lower, key)) {
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
