package com.oracleinternship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JiraApiClient {

    private final String baseUrl;
    private final String token;
    private final boolean debug;
    private final HttpClient client;

    public JiraApiClient(String baseUrl, String token, boolean debug) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.token = token;
        this.debug = debug;

        this.client = HttpClient.newBuilder()
                .followRedirects(debug ? HttpClient.Redirect.NEVER : HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String getIssue(String issueKey) throws IOException, InterruptedException {
        String url = baseUrl + "rest/api/2/issue/" + issueKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "JiraApiClient/1.0")
                .GET()
                .build();

        if (debug) {
            System.out.println("=== Jira API Request ===");
            System.out.println("URL: " + url);
            System.out.println("Headers: " + request.headers());
        }

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (debug) {
            System.out.println("=== Jira API Response ===");
            System.out.println("Status: " + response.statusCode());
            printHeaders(response.headers());
            if (response.statusCode() == 302) {
                System.out.println("⚠️ Redirect detected to: " + response.headers().firstValue("location").orElse("<none>"));
            }
        }

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch issue (" + response.statusCode() + "): " + response.body());
        }

        return response.body();
    }

    public String searchIssues(LocalDate startDate, LocalDate endDate) throws IOException, InterruptedException {
        return searchIssues(startDate, endDate, null);
    }

    public String searchIssues(LocalDate startDate, LocalDate endDate, String assignee) throws IOException, InterruptedException {
        return searchIssues(startDate, endDate, assignee, null);
    }

    public String searchIssues(LocalDate startDate, LocalDate endDate, String assignee, String project) throws IOException, InterruptedException {
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        if (days > 30) {
            // Split into chunks of 30 days to avoid Jira's result limits
            List<String> responses = new ArrayList<>();
            LocalDate currentStart = startDate;
            while (currentStart.isBefore(endDate) || currentStart.equals(endDate)) {
                LocalDate currentEnd = currentStart.plusDays(30).isBefore(endDate) ? currentStart.plusDays(30) : endDate;
                String subResponse = searchIssuesInternal(currentStart, currentEnd, assignee, project);
                responses.add(subResponse);
                currentStart = currentEnd.plusDays(1);
                if (currentStart.isAfter(endDate)) break;
            }
            return combineMultipleResponses(responses);
        } else {
            return searchIssuesInternal(startDate, endDate, assignee, project);
        }
    }

    private String searchIssuesInternal(LocalDate startDate, LocalDate endDate, String assignee, String project) throws IOException, InterruptedException {
        // Build JQL query for tickets created OR resolved within the date range, optional assignee, and optional project
        StringBuilder jqlBuilder = new StringBuilder();
        jqlBuilder.append("((created >= ").append(startDate.toString());
        jqlBuilder.append(" AND created <= ").append(endDate.toString());
        jqlBuilder.append(") OR (resolutiondate >= ").append(startDate.toString());
        jqlBuilder.append(" AND resolutiondate <= ").append(endDate.toString()).append("))");

        if (project != null && !project.trim().isEmpty() && !"All Projects".equals(project)) {
            // Escape single quotes in project name and wrap in quotes
            String escapedProject = project.replace("'", "\\'");
            jqlBuilder.append(" AND project = '").append(escapedProject).append("'");
        }

        if (assignee != null && !assignee.trim().isEmpty()) {
            // Escape single quotes in assignee name and wrap in quotes
            String escapedAssignee = assignee.replace("'", "\\'");
            jqlBuilder.append(" AND assignee = '").append(escapedAssignee).append("'");
        }

        jqlBuilder.append(" AND issuetype = 'Issue Investigation'");

        String jql = jqlBuilder.toString();
        String encodedJql = URLEncoder.encode(jql, StandardCharsets.UTF_8);

        int maxResults = 1000; // Request up to 1000 results per chunk
        String fields = "summary,status,assignee,issuetype,resolutiondate,created,priority,customfield_27101,customfield_10704,issuelinks";
        String url = baseUrl + "rest/api/2/search?jql=" + encodedJql + "&startAt=0&maxResults=" + maxResults + "&fields=" + URLEncoder.encode(fields, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "JiraApiClient/1.0")
                .GET()
                .build();

        if (debug) {
            System.out.println("=== Jira Search API Request ===");
            System.out.println("URL: " + url);
            System.out.println("JQL: " + jql);
            System.out.println("Headers: " + request.headers());
        }

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (debug) {
            System.out.println("=== Jira Search API Response ===");
            System.out.println("Status: " + response.statusCode());
            printHeaders(response.headers());
            if (response.statusCode() == 302) {
                System.out.println("⚠️ Redirect detected to: " + response.headers().firstValue("location").orElse("<none>"));
            }
        }

        if (response.statusCode() != 200) {
            throw new IOException("Failed to search issues (" + response.statusCode() + "): " + response.body());
        }

        // Check if response is actually JSON
        String contentType = response.headers().firstValue("content-type").orElse("");
        if (!contentType.contains("application/json")) {
            throw new IOException("Expected JSON response but got: " + contentType + ". Response body: " + response.body().substring(0, Math.min(500, response.body().length())));
        }

        // Basic check if response starts with JSON
        String body = response.body().trim();
        if (!body.startsWith("{") && !body.startsWith("[")) {
            throw new IOException("Response does not appear to be valid JSON. Response body starts with: " + body.substring(0, Math.min(100, body.length())));
        }

        return body;
    }

    private String combineMultipleResponses(List<String> responses) throws IOException {
        if (responses.isEmpty()) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            root.put("total", 0);
            root.putArray("issues");
            root.put("maxResults", 0);
            root.put("startAt", 0);
            return mapper.writeValueAsString(root);
        }

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode allIssues = mapper.createArrayNode();
        for (String response : responses) {
            JsonNode root = mapper.readTree(response);
            JsonNode issues = root.get("issues");
            if (issues != null && issues.isArray()) {
                allIssues.addAll((ArrayNode) issues);
            }
        }

        // Use the first response as template
        JsonNode firstRoot = mapper.readTree(responses.get(0));
        ((ObjectNode) firstRoot).set("issues", allIssues);
        ((ObjectNode) firstRoot).put("total", allIssues.size());
        ((ObjectNode) firstRoot).put("maxResults", allIssues.size());
        ((ObjectNode) firstRoot).put("startAt", 0);

        return mapper.writeValueAsString(firstRoot);
    }

    private void printHeaders(HttpHeaders headers) {
        headers.map().forEach((k, v) -> System.out.println(k + ": " + v));
    }


}
