package com.chatbot.dto;

import java.util.List;

public class CollegeInfoResponse {
    private String name;
    private String applicationWindow;
    private String counsellingMonth;
    private String admissionMode;
    private String scholarships;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private List<String> heroHighlights;
    private List<CourseSummary> courses;

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

    public List<CourseSummary> getCourses() {
        return courses;
    }

    public void setCourses(List<CourseSummary> courses) {
        this.courses = courses;
    }

    public static class CourseSummary {
        private String name;
        private String fees;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFees() {
            return fees;
        }

        public void setFees(String fees) {
            this.fees = fees;
        }
    }
}
