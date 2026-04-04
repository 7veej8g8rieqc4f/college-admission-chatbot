package com.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "college")
public class CollegeInfoProperties {
    private String name;
    private String applicationWindow;
    private String counsellingMonth;
    private String admissionMode;
    private String scholarships;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private List<String> heroHighlights = new ArrayList<>();
    private List<CourseInfo> courses = new ArrayList<>();
    private List<String> commonDocuments = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApplicationWindow() {
        return applicationWindow;
    }

    public void setApplicationWindow(String applicationWindow) {
        this.applicationWindow = applicationWindow;
    }

    public String getCounsellingMonth() {
        return counsellingMonth;
    }

    public void setCounsellingMonth(String counsellingMonth) {
        this.counsellingMonth = counsellingMonth;
    }

    public String getAdmissionMode() {
        return admissionMode;
    }

    public void setAdmissionMode(String admissionMode) {
        this.admissionMode = admissionMode;
    }

    public String getScholarships() {
        return scholarships;
    }

    public void setScholarships(String scholarships) {
        this.scholarships = scholarships;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public List<String> getHeroHighlights() {
        return heroHighlights;
    }

    public void setHeroHighlights(List<String> heroHighlights) {
        this.heroHighlights = heroHighlights;
    }

    public List<CourseInfo> getCourses() {
        return courses;
    }

    public void setCourses(List<CourseInfo> courses) {
        this.courses = courses;
    }

    public List<String> getCommonDocuments() {
        return commonDocuments;
    }

    public void setCommonDocuments(List<String> commonDocuments) {
        this.commonDocuments = commonDocuments;
    }

    public static class CourseInfo {
        private String name;
        private String eligibility;
        private String fees;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEligibility() {
            return eligibility;
        }

        public void setEligibility(String eligibility) {
            this.eligibility = eligibility;
        }

        public String getFees() {
            return fees;
        }

        public void setFees(String fees) {
            this.fees = fees;
        }
    }
}
