package com.chatbot.service;

import com.chatbot.dto.DataUploadResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class UploadedCollegeDataService {
    private final ObjectMapper objectMapper;
    private final List<UploadedCollegeRecord> records = new CopyOnWriteArrayList<>();

    @Value("${data.upload.file:uploaded-data/colleges.json}")
    private String uploadFile;

    public UploadedCollegeDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadExistingData() {
        try {
            Path file = resolveUploadFile();
            if (Files.exists(file)) {
                List<UploadedCollegeRecord> loaded = objectMapper.readValue(file.toFile(), new TypeReference<>() {
                });
                records.clear();
                records.addAll(loaded);
            }
        } catch (Exception ignored) {
        }
    }

    public DataUploadResponse upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return new DataUploadResponse(false, 0, "Please choose a CSV or JSON file.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        List<UploadedCollegeRecord> parsed;
        if (filename.endsWith(".json")) {
            parsed = parseJson(file);
        } else {
            parsed = parseCsv(file);
        }

        records.clear();
        records.addAll(parsed);
        persist();
        return new DataUploadResponse(true, parsed.size(), "Uploaded college data loaded successfully.");
    }

    public UploadedCollegeRecord findBestMatch(String collegeName, String courseName) {
        String normalizedCollege = normalize(collegeName);
        String normalizedCourse = normalize(courseName);
        for (UploadedCollegeRecord record : records) {
            boolean collegeMatches = normalizedCollege.isBlank()
                    || normalize(record.getCollege()).contains(normalizedCollege)
                    || normalizedCollege.contains(normalize(record.getCollege()));
            boolean courseMatches = normalizedCourse.isBlank()
                    || normalize(record.getCourse()).equals(normalizedCourse)
                    || normalize(record.getCourse()).contains(normalizedCourse);
            if (collegeMatches && courseMatches) {
                return record;
            }
        }
        return null;
    }

    public int getRecordCount() {
        return records.size();
    }

    private List<UploadedCollegeRecord> parseJson(MultipartFile file) throws IOException {
        List<Map<String, Object>> rows = objectMapper.readValue(file.getInputStream(), new TypeReference<>() {
        });
        List<UploadedCollegeRecord> parsed = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            UploadedCollegeRecord record = new UploadedCollegeRecord();
            record.setCollege(asString(row.get("college")));
            record.setCourse(asString(row.get("course")));
            record.setFees(asString(row.get("fees")));
            record.setEligibility(asString(row.get("eligibility")));
            record.setDocuments(asString(row.get("documents")));
            record.setLastDate(asString(row.get("lastDate")));
            record.setScholarship(asString(row.get("scholarship")));
            record.setAdmissionProcess(asString(row.get("admissionProcess")));
            record.setWebsite(asString(row.get("website")));
            record.setCampus(asString(row.get("campus")));
            if (isUseful(record)) {
                parsed.add(record);
            }
        }
        return parsed;
    }

    private List<UploadedCollegeRecord> parseCsv(MultipartFile file) throws IOException {
        String content = new String(file.getBytes());
        String[] lines = content.split("\\r?\\n");
        if (lines.length == 0) {
            return List.of();
        }
        String[] headers = splitCsv(lines[0]);
        List<UploadedCollegeRecord> parsed = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isBlank()) {
                continue;
            }
            String[] values = splitCsv(line);
            UploadedCollegeRecord record = new UploadedCollegeRecord();
            for (int j = 0; j < headers.length && j < values.length; j++) {
                String header = headers[j].trim().toLowerCase(Locale.ROOT);
                String value = unquote(values[j].trim());
                switch (header) {
                    case "college" -> record.setCollege(value);
                    case "course" -> record.setCourse(value);
                    case "fees", "fee" -> record.setFees(value);
                    case "eligibility" -> record.setEligibility(value);
                    case "documents", "document" -> record.setDocuments(value);
                    case "lastdate", "last_date", "deadline" -> record.setLastDate(value);
                    case "scholarship", "scholarships" -> record.setScholarship(value);
                    case "admissionprocess", "admission_process", "process" -> record.setAdmissionProcess(value);
                    case "website" -> record.setWebsite(value);
                    case "campus" -> record.setCampus(value);
                    default -> {
                    }
                }
            }
            if (isUseful(record)) {
                parsed.add(record);
            }
        }
        return parsed;
    }

    private void persist() throws IOException {
        Path file = resolveUploadFile();
        Files.createDirectories(file.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), records);
    }

    private Path resolveUploadFile() {
        return Paths.get(uploadFile);
    }

    private boolean isUseful(UploadedCollegeRecord record) {
        return !normalize(record.getCollege()).isBlank() && !normalize(record.getCourse()).isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(".", "").replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String[] splitCsv(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char ch : line.toCharArray()) {
            if (ch == '"') {
                inQuotes = !inQuotes;
            } else if (ch == ',' && !inQuotes) {
                tokens.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        tokens.add(current.toString());
        return tokens.toArray(String[]::new);
    }

    private String unquote(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public static class UploadedCollegeRecord {
        private String college;
        private String course;
        private String fees;
        private String eligibility;
        private String documents;
        private String lastDate;
        private String scholarship;
        private String admissionProcess;
        private String website;
        private String campus;

        public String getCollege() {
            return college;
        }

        public void setCollege(String college) {
            this.college = college;
        }

        public String getCourse() {
            return course;
        }

        public void setCourse(String course) {
            this.course = course;
        }

        public String getFees() {
            return fees;
        }

        public void setFees(String fees) {
            this.fees = fees;
        }

        public String getEligibility() {
            return eligibility;
        }

        public void setEligibility(String eligibility) {
            this.eligibility = eligibility;
        }

        public String getDocuments() {
            return documents;
        }

        public void setDocuments(String documents) {
            this.documents = documents;
        }

        public String getLastDate() {
            return lastDate;
        }

        public void setLastDate(String lastDate) {
            this.lastDate = lastDate;
        }

        public String getScholarship() {
            return scholarship;
        }

        public void setScholarship(String scholarship) {
            this.scholarship = scholarship;
        }

        public String getAdmissionProcess() {
            return admissionProcess;
        }

        public void setAdmissionProcess(String admissionProcess) {
            this.admissionProcess = admissionProcess;
        }

        public String getWebsite() {
            return website;
        }

        public void setWebsite(String website) {
            this.website = website;
        }

        public String getCampus() {
            return campus;
        }

        public void setCampus(String campus) {
            this.campus = campus;
        }
    }
}
