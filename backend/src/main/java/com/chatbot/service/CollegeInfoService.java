package com.chatbot.service;

import com.chatbot.config.CollegeInfoProperties;
import com.chatbot.dto.CollegeInfoResponse;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CollegeInfoService {
    private final CollegeInfoProperties collegeInfoProperties;

    public CollegeInfoService(CollegeInfoProperties collegeInfoProperties) {
        this.collegeInfoProperties = collegeInfoProperties;
    }

    public CollegeInfoProperties getCollegeInfo() {
        return collegeInfoProperties;
    }

    public CollegeInfoResponse buildResponse() {
        CollegeInfoResponse response = new CollegeInfoResponse();
        response.setName(collegeInfoProperties.getName());
        response.setApplicationWindow(collegeInfoProperties.getApplicationWindow());
        response.setCounsellingMonth(collegeInfoProperties.getCounsellingMonth());
        response.setAdmissionMode(collegeInfoProperties.getAdmissionMode());
        response.setScholarships(collegeInfoProperties.getScholarships());
        response.setContactEmail(collegeInfoProperties.getContactEmail());
        response.setContactPhone(collegeInfoProperties.getContactPhone());
        response.setWebsite(collegeInfoProperties.getWebsite());
        response.setHeroHighlights(collegeInfoProperties.getHeroHighlights());
        response.setCourses(collegeInfoProperties.getCourses().stream().map(course -> {
            CollegeInfoResponse.CourseSummary summary = new CollegeInfoResponse.CourseSummary();
            summary.setName(course.getName());
            summary.setFees(course.getFees());
            return summary;
        }).collect(Collectors.toList()));
        return response;
    }
}
