# UCC Chatbot Assistant

AI-powered customer-care assistant for the University of Dar es Salaam Computing Centre (UCC).

**Official Website:** https://www.ucc.co.tz/  
**Admission Portal:** https://admission.ucc.co.tz/  
**Frontend:** https://agent-6a87a4d1bce5537b6d8d53a5--uccchatbot.netlify.app/

## Technology Stack

- **Frontend:** HTML5, CSS3, Vanilla JavaScript
- **Backend:** Java 17+, Spring Boot 3.x, Spring Security, Spring Data JPA
- **Database:** MySQL
- **Build Tool:** Maven

## Project Structure

```
ucc-chatbot-assistant/
├── frontend/
│   ├── index.html
│   ├── chat.html
│   ├── about.html
│   ├── courses.html
│   ├── services.html
│   ├── contact.html
│   ├── css/
│   │   └── style.css
│   └── js/
│       ├── config.js
│       ├── api.js
│       ├── chatbot.js
│       └── app.js
├── admin/
│   ├── index.html
│   ├── dashboard.html
│   ├── css/
│   │   └── admin.css
│   └── js/
│       ├── admin-auth.js
│       └── admin.js
└── backend/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/ucc/chatbot/
        │   │   ├── controller/
        │   │   ├── service/
        │   │   ├── model/
        │   │   ├── repository/
        │   │   ├── dto/
        │   │   ├── config/
        │   │   └── exception/
        │   └── resources/
        │       ├── application.properties
        │       └── db/schema.sql
        └── test/
```

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Node.js (for serving frontend)

### Backend Setup

```bash
cd backend

# Configure environment variables
cp .env.example .env
# Edit .env with your database and AI credentials

# Run the application
mvn spring-boot:run
```

### Frontend Setup

```bash
# Serve frontend locally
npx serve frontend -l 3000

# Or serve admin dashboard
npx serve admin -l 3001
```

## Environment Variables

See `backend/.env.example` for required variables.

Key variables:
- `DB_URL` - MySQL connection string
- `DB_USERNAME` - MySQL username
- `DB_PASSWORD` - MySQL password
- `AI_API_KEY` - OpenAI/compatible API key
- `JWT_SECRET` - JWT signing secret
- `PORT` - Server port (default: 8080)

## API Endpoints

### Public
- `POST /api/chat` - Send a chat message
- `GET /api/faqs` - List FAQs
- `GET /api/courses` - List courses
- `GET /api/services` - List services
- `GET /api/contacts` - List contacts

### Authentication
- `POST /api/auth/login` - User login

### Admin
- `GET /api/admin/dashboard` - Dashboard analytics
- `GET /api/admin/knowledge` - List knowledge documents
- `POST /api/admin/knowledge` - Create knowledge document
- `PUT /api/admin/knowledge/{id}` - Update knowledge document
- `DELETE /api/admin/knowledge/{id}` - Delete knowledge document
- `GET /api/admin/faqs` - List FAQs
- `POST /api/admin/faqs` - Create FAQ
- `PUT /api/admin/faqs/{id}` - Update FAQ
- `DELETE /api/admin/faqs/{id}` - Delete FAQ
- `GET /api/admin/conversations` - List conversations

## Database Schema

See `backend/src/main/resources/db/schema.sql` for the complete MySQL schema.

## License

Proprietary - University of Dar es Salaam Computing Centre
