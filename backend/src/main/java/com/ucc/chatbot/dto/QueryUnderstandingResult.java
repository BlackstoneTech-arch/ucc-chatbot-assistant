package com.ucc.chatbot.dto;

import java.util.List;

public class QueryUnderstandingResult {
    private String originalMessage;
    private String detectedLanguage;
    private String normalizedLanguage;
    private String intent;
    private Entities entities;
    private List<String> concepts;
    private String canonicalQuery;
    private List<String> retrievalQueries;
    private boolean requiresRetrieval;
    private boolean requiresClarification;
    private String responseLanguage;
    private boolean verifiedInformationFound;

    public QueryUnderstandingResult() {}

    public String getOriginalMessage() { return originalMessage; }
    public void setOriginalMessage(String originalMessage) { this.originalMessage = originalMessage; }
    public String getDetectedLanguage() { return detectedLanguage; }
    public void setDetectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; }
    public String getNormalizedLanguage() { return normalizedLanguage; }
    public void setNormalizedLanguage(String normalizedLanguage) { this.normalizedLanguage = normalizedLanguage; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public Entities getEntities() { return entities; }
    public void setEntities(Entities entities) { this.entities = entities; }
    public List<String> getConcepts() { return concepts; }
    public void setConcepts(List<String> concepts) { this.concepts = concepts; }
    public String getCanonicalQuery() { return canonicalQuery; }
    public void setCanonicalQuery(String canonicalQuery) { this.canonicalQuery = canonicalQuery; }
    public List<String> getRetrievalQueries() { return retrievalQueries; }
    public void setRetrievalQueries(List<String> retrievalQueries) { this.retrievalQueries = retrievalQueries; }
    public boolean isRequiresRetrieval() { return requiresRetrieval; }
    public void setRequiresRetrieval(boolean requiresRetrieval) { this.requiresRetrieval = requiresRetrieval; }
    public boolean isRequiresClarification() { return requiresClarification; }
    public void setRequiresClarification(boolean requiresClarification) { this.requiresClarification = requiresClarification; }
    public String getResponseLanguage() { return responseLanguage; }
    public void setResponseLanguage(String responseLanguage) { this.responseLanguage = responseLanguage; }
    public boolean isVerifiedInformationFound() { return verifiedInformationFound; }
    public void setVerifiedInformationFound(boolean verifiedInformationFound) { this.verifiedInformationFound = verifiedInformationFound; }

    public static class Entities {
        private String programme;
        private String concept;
        private String course;

        public String getProgramme() { return programme; }
        public void setProgramme(String programme) { this.programme = programme; }
        public String getConcept() { return concept; }
        public void setConcept(String concept) { this.concept = concept; }
        public String getCourse() { return course; }
        public void setCourse(String course) { this.course = course; }
    }
}
