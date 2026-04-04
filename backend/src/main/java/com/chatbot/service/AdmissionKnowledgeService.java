package com.chatbot.service;

import com.chatbot.config.CollegeInfoProperties;
import com.chatbot.dto.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AdmissionKnowledgeService {
    private final CollegeInfoService collegeInfoService;
    private final ConversationContextService conversationContextService;
    private final UploadedCollegeDataService uploadedCollegeDataService;

    public AdmissionKnowledgeService(
            CollegeInfoService collegeInfoService,
            ConversationContextService conversationContextService,
            UploadedCollegeDataService uploadedCollegeDataService
    ) {
        this.collegeInfoService = collegeInfoService;
        this.conversationContextService = conversationContextService;
        this.uploadedCollegeDataService = uploadedCollegeDataService;
    }

    public String buildSystemPrompt(List<ChatMessage> history, String latestMessage) {
        String conversationSummary = conversationContextService.buildConversationSummary(history, latestMessage);
        return """
                You are an Admission Help Desk assistant.

                Rules:
                1. Answer admission help desk questions clearly and practically.
                2. Admission help desk scope includes colleges, universities, courses, eligibility, documents, scholarships, deadlines, counselling, hostel, placements, entrance exams, application steps, and other admission-related guidance.
                3. If the user asks anything clearly outside admissions, reply:
                   "I can help with admission help desk questions like eligibility, documents, deadlines, scholarships, and the admission process."
                4. Reply in clear, simple English.
                5. Be concise and practical.
                6. If a fact is uncertain, say so briefly instead of guessing.
                7. If the user asks for fees, use the provided context or approximate it if exact fees are unknown.

                Demo college data available in this app:
                - College Name: %s
                - Courses: %s
                - Application Window: %s
                - Counselling: %s
                - Scholarships: %s

                Current conversation context:
                %s
                """.formatted(
                collegeInfoService.getCollegeInfo().getName(),
                collegeInfoService.getCollegeInfo().getCourses().stream().map(course -> course.getName()).toList(),
                collegeInfoService.getCollegeInfo().getApplicationWindow(),
                collegeInfoService.getCollegeInfo().getCounsellingMonth(),
                collegeInfoService.getCollegeInfo().getScholarships(),
                conversationSummary.isBlank() ? "No saved context yet." : conversationSummary
        );
    }

    public String getRuleBasedResponse(String userMessage, List<ChatMessage> history) {
        String normalized = userMessage == null ? "" : userMessage.toLowerCase(Locale.ROOT).trim();
        CollegeInfoProperties collegeInfo = collegeInfoService.getCollegeInfo();
        String collegeName = conversationContextService.getCollegeName(history, userMessage);
        String courseName = conversationContextService.detectCourseName(userMessage);
        UploadedCollegeDataService.UploadedCollegeRecord uploadedRecord = uploadedCollegeDataService.findBestMatch(collegeName, courseName);

        if (normalized.isEmpty()) {
            return "Please type your admission help desk question.";
        }

        if (isOutsideAdmissionScope(normalized)) {
            return "I can help with admission help desk questions like eligibility, documents, deadlines, scholarships, and the admission process.";
        }

        if (containsAny(normalized, "hello", "hi", "hey", "good morning", "good evening")) {
            return "Hello! You can ask about eligibility, documents, scholarships, deadlines, counselling, hostel, entrance exams, courses, and the admission process.";
        }

        if (containsAny(normalized, "fees", "fee", "cost")) {
            if (courseName != null) {
                if (uploadedRecord != null && uploadedRecord.getFees() != null && !uploadedRecord.getFees().isBlank()) {
                    return "The approximate fee for " + uploadedRecord.getCourse() + " at " + uploadedRecord.getCollege() + " is " + uploadedRecord.getFees() + ".";
                }
                CollegeInfoProperties.CourseInfo configuredCourse = findConfiguredCourse(courseName, collegeInfo);
                if (configuredCourse != null && configuredCourse.getFees() != null && !configuredCourse.getFees().isBlank()) {
                    return "The fee for " + configuredCourse.getName() + " is " + configuredCourse.getFees() + ".";
                }
                if ("MCA".equalsIgnoreCase(courseName)) {
                    return "The approximate fee for MCA is around INR 1 lakh to 3 lakh per year, depending on the college.";
                }
                if ("MBA".equalsIgnoreCase(courseName)) {
                    return "The approximate fee for MBA is around INR 1.5 lakh to 4 lakh per year, depending on the college.";
                }
                if ("BA".equalsIgnoreCase(courseName) || "B.Sc".equalsIgnoreCase(courseName) || "B.Com".equalsIgnoreCase(courseName) || "BCA".equalsIgnoreCase(courseName)) {
                    return "The approximate fee for " + courseName + " is around INR 50,000 to 2 lakh per year, depending on the college.";
                }
                return "The approximate fee for " + courseName + " usually ranges from INR 50,000 to 3.5 lakh per year, depending on the college and facilities.";
            }
            return "Please mention the specific course (like MBA, BTech, BA) or college to get approximate fee details.";
        }

        if (containsAny(normalized, "eligibility", "eligible") && courseName != null) {
            if (uploadedRecord != null && uploadedRecord.getEligibility() != null && !uploadedRecord.getEligibility().isBlank()) {
                return uploadedRecord.getCollege() + " " + uploadedRecord.getCourse() + " eligibility is " + uploadedRecord.getEligibility() + ".";
            }
            CollegeInfoProperties.CourseInfo configuredCourse = findConfiguredCourse(courseName, collegeInfo);
            if (configuredCourse != null && configuredCourse.getEligibility() != null && !configuredCourse.getEligibility().isBlank()) {
                return configuredCourse.getName() + " eligibility is " + configuredCourse.getEligibility() + ".";
            }
            if ("MCA".equalsIgnoreCase(courseName)) {
                return "MCA eligibility usually requires graduation with Maths at 10+2 or graduation level, minimum qualifying percentage as per the college, and entrance test or interview if applicable.";
            }
            if ("MBA".equalsIgnoreCase(courseName)) {
                return "MBA eligibility usually requires graduation with the minimum percentage asked by the college, and CAT, MAT, CMAT, or the college entrance test if applicable.";
            }
            if ("B.Sc".equalsIgnoreCase(courseName) || "BA".equalsIgnoreCase(courseName) || "B.Com".equalsIgnoreCase(courseName)) {
                return "Eligibility for " + courseName + " usually requires 10+2 from a recognized board with the minimum percentage asked by the college.";
            }
            return "Eligibility for " + courseName + " usually depends on the college. In most cases, you need the qualifying previous degree or class marksheet, minimum required percentage, and entrance exam or interview if applicable.";
        }

        if (courseName != null && containsAny(normalized, "document", "documents", "required papers", "certificates")) {
            if (uploadedRecord != null && uploadedRecord.getDocuments() != null && !uploadedRecord.getDocuments().isBlank()) {
                return "Required documents for " + uploadedRecord.getCourse() + " at " + uploadedRecord.getCollege() + " are " + uploadedRecord.getDocuments() + ".";
            }
            return "Common documents for " + courseName + " admission usually include marksheets, ID proof, passport-size photos, transfer or migration certificate, category certificate if applicable, and entrance exam scorecard if required.";
        }

        if (courseName != null && containsAny(normalized, "admission process", "how to apply", "apply process", "application process", "admission")) {
            if (uploadedRecord != null && uploadedRecord.getAdmissionProcess() != null && !uploadedRecord.getAdmissionProcess().isBlank()) {
                return uploadedRecord.getCourse() + " admission process at " + uploadedRecord.getCollege() + " is " + uploadedRecord.getAdmissionProcess() + ".";
            }
            return "For " + courseName + " admission, usually you need to check eligibility, fill the application form, upload documents, and appear for entrance test or interview if the college requires it.";
        }

        if (courseName != null && containsAny(normalized, "last date", "deadline", "apply date", "application date")) {
            if (uploadedRecord != null && uploadedRecord.getLastDate() != null && !uploadedRecord.getLastDate().isBlank()) {
                return "The last date for " + uploadedRecord.getCourse() + " at " + uploadedRecord.getCollege() + " is " + uploadedRecord.getLastDate() + ".";
            }
            if (collegeName != null && !normalizeCourse(collegeName).contains(normalizeCourse(collegeInfo.getName()))) {
                return "The exact application deadline for " + courseName + " at " + collegeName + " is typically announced on their official portal online. Please check their site.";
            }
            if (collegeName != null) {
                return "The application window for " + collegeName + " is " + collegeInfo.getApplicationWindow() + ".";
            }
        }

        if (courseName != null && containsAny(normalized, "scholarship", "scholarships")) {
            if (uploadedRecord != null && uploadedRecord.getScholarship() != null && !uploadedRecord.getScholarship().isBlank()) {
                return "Scholarship details for " + uploadedRecord.getCourse() + " at " + uploadedRecord.getCollege() + ": " + uploadedRecord.getScholarship() + ".";
            }
        }

        if (containsAny(normalized, "document", "documents", "required papers", "certificates")) {
            return "Common documents required for admission are: "
                    + String.join(", ", collegeInfo.getCommonDocuments()) + ".";
        }

        if (containsAny(normalized, "last date", "deadline", "apply date", "application date", "application window")) {
            if (collegeName != null && !normalizeCourse(collegeName).contains(normalizeCourse(collegeInfo.getName()))) {
                return "The exact application deadline for " + collegeName + " is typically announced on their official portal online. Please check their site.";
            }
            if (collegeName != null) {
                return "The application window for " + collegeName + " is " + collegeInfo.getApplicationWindow() + ".";
            }
            return "The application window for " + collegeInfo.getName() + " is " + collegeInfo.getApplicationWindow() + ".";
        }

        if (containsAny(normalized, "counselling", "counseling")) {
            if (collegeName != null && !normalizeCourse(collegeName).contains(normalizeCourse(collegeInfo.getName()))) {
                return "Counselling procedures for " + collegeName + " generally rely on course cutoff scores or merit. Exact details can be verified online.";
            }
            return "Counselling usually happens in " + collegeInfo.getCounsellingMonth() + ".";
        }

        if (containsAny(normalized, "scholarship", "scholarships")) {
            if (collegeName != null && !normalizeCourse(collegeName).contains(normalizeCourse(collegeInfo.getName()))) {
                return "Scholarship offerings at " + collegeName + " are typically based on merit exams or financial criteria. Please consult their website.";
            }
            return collegeInfo.getScholarships() + ".";
        }

        if (containsAny(normalized, "admission mode", "selection process", "admission process", "how to apply", "apply process", "application process")) {
            if (collegeName != null && !normalizeCourse(collegeName).contains(normalizeCourse(collegeInfo.getName()))) {
                return "The admission process for " + collegeName + " normally requires fulfilling eligibility criteria, submitting an application, and attending a potential screening/entrance exam.";
            }
            return collegeInfo.getName() + " follows " + collegeInfo.getAdmissionMode() + ".";
        }

        if (containsAny(normalized, "contact", "phone", "email", "mail", "website")) {
            if (collegeName != null && !normalizeCourse(collegeName).contains(normalizeCourse(collegeInfo.getName()))) {
                return "Please search the web to find the official contact details for " + collegeName + ".";
            }
            return "You can contact " + collegeInfo.getName() + " at " + collegeInfo.getContactEmail()
                    + " or " + collegeInfo.getContactPhone() + ". Website: " + collegeInfo.getWebsite() + ".";
        }

        if (containsAny(normalized, "course", "courses", "program", "programs")) {
            String courseList = collegeInfo.getCourses().stream()
                    .map(CollegeInfoProperties.CourseInfo::getName)
                    .collect(Collectors.joining(", "));
            return "Available courses include " + courseList + ".";
        }

        if (containsAny(normalized, "hostel", "placement", "entrance exam", "entrance", "seat")) {
            String subject = collegeName != null ? collegeName : courseNameOrGeneral(courseName);
            String topic = extractTopic(normalized);
            if (collegeName == null || normalizeCourse(collegeName).contains(normalizeCourse(collegeInfo.getName()))) {
                return "For detailed " + topic + " availability at " + collegeInfo.getName() + ", please contact our admissions office directly.";
            }
            return "I don't have the exact " + topic + " figures for " + subject + " right now. Generally, " + topic + " criteria vary dramatically by campus. Check the official webpage for exact stats.";
        }

        if (collegeName != null && courseName == null) {
            return "You asked about " + collegeName + ". Please ask what you want to know, such as eligibility, documents, last date, scholarship, or admission process.";
        }

        if (courseName != null) {
            return "You asked about " + courseName + ". You can ask for eligibility, documents, last date, scholarship, or admission process for this course.";
        }

        if (collegeName != null) {
            return "You asked about " + collegeName + ". Please ask what you want to know, such as eligibility, documents, last date, scholarship, or admission process.";
        }

        return "Please ask a specific admission question such as eligibility, required documents, scholarship details, application dates, counselling, or how to apply.";
    }

    public String extractCourseName(String userMessage) {
        return conversationContextService.detectCourseName(userMessage);
    }

    public String extractCollegeName(List<ChatMessage> history, String userMessage) {
        return conversationContextService.getCollegeName(history, userMessage);
    }

    private boolean isOutsideAdmissionScope(String message) {
        return containsAny(message, "weather", "movie", "cricket", "joke", "recipe", "song", "news", "capital of", "photosynthesis")
                && !containsAny(
                message,
                "admission",
                "admissions",
                "college",
                "university",
                "course",
                "fees",
                "fee",
                "eligibility",
                "document",
                "documents",
                "scholarship",
                "scholarships",
                "counselling",
                "counseling",
                "deadline",
                "last date",
                "application",
                "apply",
                "entrance exam",
                "exam",
                "hostel",
                "placement",
                "seat",
                "admission process"
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private CollegeInfoProperties.CourseInfo findConfiguredCourse(String courseName, CollegeInfoProperties collegeInfo) {
        for (CollegeInfoProperties.CourseInfo course : collegeInfo.getCourses()) {
            if (normalizeCourse(course.getName()).equals(normalizeCourse(courseName))) {
                return course;
            }
        }
        return null;
    }

    private String normalizeCourse(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(".", "").replace(" ", "");
    }

    private String courseNameOrGeneral(String courseName) {
        return courseName == null ? "general" : courseName;
    }

    private String extractTopic(String normalized) {
        if (normalized.contains("hostel")) {
            return "hostel";
        }
        if (normalized.contains("placement")) {
            return "placement";
        }
        if (normalized.contains("entrance")) {
            return "entrance exam";
        }
        if (normalized.contains("seat")) {
            return "seat availability";
        }
        return "admission";
    }
}
