package com.ucc.chatbot.service;

import com.ucc.chatbot.model.*;
import com.ucc.chatbot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WebsiteSyncService {

    private final WebsitePageRepository pageRepository;
    private final WebsiteSyncJobRepository jobRepository;
    private final KnowledgeService knowledgeService;
    private final KnowledgeDocumentRepository knowledgeRepository;

    private static final List<String> IMPORTANT_PATHS = Arrays.asList(
        "", "about-us", "course", "course/academic", "course/professional", "course/tailor-made",
        "it-infrastructure", "software-product-&-services", "news", "downloads", "contact-us"
    );

    @Autowired
    public WebsiteSyncService(WebsitePageRepository pageRepository,
                               WebsiteSyncJobRepository jobRepository,
                               KnowledgeService knowledgeService,
                               KnowledgeDocumentRepository knowledgeRepository) {
        this.pageRepository = pageRepository;
        this.jobRepository = jobRepository;
        this.knowledgeService = knowledgeService;
        this.knowledgeRepository = knowledgeRepository;
    }

    @Async
    @Transactional
    public WebsiteSyncJob startScan(String baseUrl, String startedBy) {
        WebsiteSyncJob job = new WebsiteSyncJob();
        job.setStatus("RUNNING");
        job.setStartedAt(LocalDateTime.now());
        job.setStartedBy(startedBy);
        jobRepository.save(job);
        try {
            int newCount = 0, updatedCount = 0, unchangedCount = 0, failedCount = 0;
            for (String path : IMPORTANT_PATHS) {
                String url = baseUrl + (path.isEmpty() ? "" : "/" + path);
                try {
                    ScanResult r = scanPage(url);
                    if (r == null) { failedCount++; continue; }
                    WebsitePage existing = pageRepository.findByUrl(url).orElse(null);
                    if (existing == null) {
                        WebsitePage p = new WebsitePage();
                        p.setUrl(url);
                        p.setTitle(r.title);
                        p.setCategory(inferCategory(path));
                        p.setLanguage(r.language);
                        p.setLastHash(r.hash);
                        p.setLastScannedAt(LocalDateTime.now());
                        p.setLastStatus("NEW");
                        pageRepository.save(p);
                        if (r.content != null && !r.content.isBlank()) {
                            knowledgeService.uploadAndProcess(r.title, r.content, inferCategory(path), "WEBSITE_SCAN");
                        }
                        newCount++;
                    } else if (!Objects.equals(existing.getLastHash(), r.hash)) {
                        existing.setTitle(r.title);
                        existing.setLastHash(r.hash);
                        existing.setLastScannedAt(LocalDateTime.now());
                        existing.setLastStatus("UPDATED");
                        pageRepository.save(existing);
                        if (r.content != null && !r.content.isBlank()) {
                            knowledgeService.uploadAndProcess(r.title, r.content, inferCategory(path), "WEBSITE_SCAN");
                        }
                        updatedCount++;
                    } else {
                        existing.setLastScannedAt(LocalDateTime.now());
                        existing.setLastStatus("UNCHANGED");
                        pageRepository.save(existing);
                        unchangedCount++;
                    }
                } catch (Exception e) {
                    failedCount++;
                }
                job.setPagesScanned(job.getPagesScanned() + 1);
            }
            job.setPagesNew(newCount);
            job.setPagesUpdated(updatedCount);
            job.setPagesUnchanged(unchangedCount);
            job.setPagesFailed(failedCount);
            job.setStatus("COMPLETED");
            job.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
        }
        return jobRepository.save(job);
    }

    private ScanResult scanPage(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "UCC-Chatbot-Bot/1.0");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            if (code != 200) return null;
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            br.close();
            String html = sb.toString();
            String title = extractTitle(html);
            String content = extractReadableText(html);
            String lang = detectLanguageFromText(content);
            String hash = sha256(title + "|" + content);
            return new ScanResult(title, content, lang, hash);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractTitle(String html) {
        Matcher m = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE).matcher(html);
        return m.find() ? m.group(1).trim() : "UCC Page";
    }

    private String extractReadableText(String html) {
        String text = html.replaceAll("<script[\\s\\S]*?</script>", " ");
        text = text.replaceAll("<style[\\s\\S]*?</style>", " ");
        text = text.replaceAll("<nav[\\s\\S]*?</nav>", " ");
        text = text.replaceAll("<header[\\s\\S]*?</header>", " ");
        text = text.replaceAll("<footer[\\s\\S]*?</footer>", " ");
        text = text.replaceAll("<[^>]+>", " ");
        text = text.replaceAll("&nbsp;", " ");
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > 8000 ? text.substring(0, 8000) : text;
    }

    private String detectLanguageFromText(String text) {
        if (text == null) return "en";
        String lower = text.toLowerCase();
        String[] sw = {"habari", "kozi", "ada", "programu", "maombi"};
        int count = 0;
        for (String m : sw) if (lower.contains(m)) count++;
        return count >= 2 ? "sw" : "en";
    }

    private String inferCategory(String path) {
        if (path.contains("about")) return "ABOUT_UCC";
        if (path.contains("course/academic")) return "PROGRAMMES";
        if (path.contains("course/professional")) return "PROFESSIONAL_TRAINING";
        if (path.contains("course")) return "PROGRAMMES";
        if (path.contains("it-infrastructure")) return "ICT_SUPPORT";
        if (path.contains("software")) return "SOFTWARE_SERVICES";
        if (path.contains("news")) return "NEWS";
        if (path.contains("contact")) return "CONTACTS";
        if (path.contains("downloads")) return "OTHER";
        return "OTHER";
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "0";
        }
    }

    private static class ScanResult {
        String title; String content; String language; String hash;
        ScanResult(String t, String c, String l, String h) { title=t; content=c; language=l; hash=h; }
    }
}
