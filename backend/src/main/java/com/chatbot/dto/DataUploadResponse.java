package com.chatbot.dto;

public class DataUploadResponse {
    private boolean success;
    private int recordsLoaded;
    private String message;

    public DataUploadResponse() {
    }

    public DataUploadResponse(boolean success, int recordsLoaded, String message) {
        this.success = success;
        this.recordsLoaded = recordsLoaded;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getRecordsLoaded() {
        return recordsLoaded;
    }

    public void setRecordsLoaded(int recordsLoaded) {
        this.recordsLoaded = recordsLoaded;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
