package com.ucc.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads the static UCC document catalog (front-end /data/documents.json)
 * so the backend can serve /api/chat/documents and (optionally) pre-empt
 * download-intent messages with download links.
 */
@Service
public class DocumentCatalogService {

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode root;

    @PostConstruct
    public void init() {
        try (InputStream in = new ClassPathResource("documents.json").getInputStream()) {
            this.root = mapper.readTree(in);
        } catch (Exception e) {
            this.root = mapper.createObjectNode();
        }
    }

    public JsonNode getAll() {
        return root != null ? root : mapper.createObjectNode();
    }

    public List<JsonNode> getDocumentsByCategory(String category) {
        if (root == null || !root.has("documents")) return Collections.emptyList();
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode d : root.get("documents")) {
            if (category == null || category.equalsIgnoreCase(d.path("category").asText())) {
                out.add(d);
            }
        }
        return out;
    }

    public JsonNode findById(String id) {
        if (root == null || !root.has("documents")) return null;
        for (JsonNode d : root.get("documents")) {
            if (id.equals(d.path("id").asText())) return d;
        }
        return null;
    }

    public List<JsonNode> getIntakePack() {
        if (root == null || !root.has("intakePack")) return Collections.emptyList();
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode idNode : root.get("intakePack").path("documentIds")) {
            JsonNode d = findById(idNode.asText());
            if (d != null) out.add(d);
        }
        return out;
    }

    public List<JsonNode> search(String query) {
        if (root == null || !root.has("documents")) return Collections.emptyList();
        String q = (query == null ? "" : query.toLowerCase()).trim();
        if (q.isEmpty()) return Collections.emptyList();
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode d : root.get("documents")) {
            StringBuilder hay = new StringBuilder();
            hay.append(d.path("title").asText("")).append(' ')
               .append(d.path("description").asText("")).append(' ')
               .append(d.path("category").asText("")).append(' ')
               .append(d.path("programme").asText(""));
            for (JsonNode t : d.path("tags")) hay.append(' ').append(t.asText());
            if (hay.toString().toLowerCase().contains(q)) out.add(d);
        }
        return out;
    }
}
