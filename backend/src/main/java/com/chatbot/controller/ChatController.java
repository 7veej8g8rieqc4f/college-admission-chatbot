package com.chatbot.controller;

import com.chatbot.dto.ChatRequest;
import com.chatbot.dto.ChatResponse;
import com.chatbot.dto.ChatMessage;
import com.chatbot.dto.CollegeInfoResponse;
import com.chatbot.dto.DataUploadResponse;
import com.chatbot.service.CollegeInfoService;
import com.chatbot.service.OpenAIService;
import com.chatbot.service.UploadedCollegeDataService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    private final OpenAIService openAIService;
    private final CollegeInfoService collegeInfoService;
    private final UploadedCollegeDataService uploadedCollegeDataService;

    public ChatController(
            OpenAIService openAIService,
            CollegeInfoService collegeInfoService,
            UploadedCollegeDataService uploadedCollegeDataService
    ) {
        this.openAIService = openAIService;
        this.collegeInfoService = collegeInfoService;
        this.uploadedCollegeDataService = uploadedCollegeDataService;
    }

    @GetMapping("/college-info")
    public CollegeInfoResponse collegeInfo() {
        return collegeInfoService.buildResponse();
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String message = request == null ? null : request.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return new ChatResponse("Please type a valid message.");
        }

        List<ChatMessage> history = request.getHistory() == null ? List.of() : request.getHistory();
        String reply = openAIService.getChatResponse(message.trim(), history);
        return new ChatResponse(reply);
    }

    @GetMapping("/data/status")
    public DataUploadResponse dataStatus() {
        return new DataUploadResponse(true, uploadedCollegeDataService.getRecordCount(), "Uploaded records available.");
    }

    @PostMapping("/data/upload")
    public DataUploadResponse uploadData(@RequestParam("file") MultipartFile file) throws Exception {
        return uploadedCollegeDataService.upload(file);
    }
}
