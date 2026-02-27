package com.example.financial.dto;

public class AssistantRequest {
    private String query;
    private String context;

    public AssistantRequest() {
    }

    public AssistantRequest(String query, String context) {
        this.query = query;
        this.context = context;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
