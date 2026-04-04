<<<<<<< HEAD
# College Admission Chatbot

Simple college admission chatbot with:
- `Spring Boot` backend API
- `HTML/CSS/JavaScript` frontend chat UI
- `OpenAI` support with safe fallback to built-in admission answers

## Run Backend

1. Open terminal in [backend/pom.xml](/C:/Users/Tisha%20Goel/college-admission-chatbot/backend/pom.xml)
2. Run:

```powershell
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`.

## Open Frontend

Open [frontend/index.html](/C:/Users/Tisha%20Goel/college-admission-chatbot/frontend/index.html) in the browser.

## OpenAI Setup Optional

If you want live OpenAI answers:

1. Copy [application-local.properties.example](/C:/Users/Tisha%20Goel/college-admission-chatbot/backend/src/main/resources/application-local.properties.example)
   to `backend/src/main/resources/application-local.properties`
2. Add your key:

```properties
openai.api.key=sk-...
```

Without API key, chatbot still works using built-in admission FAQ logic.

## Customize College Data

Edit [AdmissionKnowledgeService.java](/C:/Users/Tisha%20Goel/college-admission-chatbot/backend/src/main/java/com/chatbot/service/AdmissionKnowledgeService.java)
to change:
- college name
- courses
- fees
- dates
- scholarship info
- contact details
=======
# college-admission-chatbot
>>>>>>> c38da87231c780439655015aa63252fecd9a703b
