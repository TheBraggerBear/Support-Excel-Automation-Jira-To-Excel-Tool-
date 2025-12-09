package com.oracleinternship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonParser {
    private String rawJson;

    public JsonParser(String rawJson) {
        this.rawJson = rawJson;
    }

    public void cleanJson(String fieldToRemove, String outputFilePath, Ticket ticket) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(rawJson);

        // Extract ticket details from the JSON structure
        String id = root.get("key").asText();
        JsonNode fields = root.get("fields");
        String summary = fields.get("summary").asText();
        String status = fields.get("status").get("name").asText();
        String assignee = fields.get("assignee").get("displayName").asText();
        String customfield_27101 = fields.get("customfield_27101").asText();
        String issuetype = fields.get("issuetype").get("name").asText();
        String resolutiondate = fields.get("resolutiondate").asText();
        String customfield_10704 = fields.get("customfield_10704").asText();

        ticket.setId(id);
        ticket.setSummary(summary);
        ticket.setStatus(status);
        ticket.setAssignee(assignee);
        ticket.setCustomfield_27101(customfield_27101);
        ticket.setIssuetype(issuetype);
        ticket.setResolutiondate(resolutiondate);
        ticket.setCustomfield_10704(customfield_10704);

        System.out.println("Ticket Details:");
        System.out.println("ID: " + ticket.getId());
        System.out.println("Summary: " + ticket.getSummary());
        System.out.println("Status: " + ticket.getStatus());
        System.out.println("Assignee: " + ticket.getAssignee());
        System.out.println("Custom Field 27101: " + ticket.getCustomfield_27101());
        System.out.println("Issue Type: " + ticket.getIssuetype());
        System.out.println("Resolution Date: " + ticket.getResolutiondate());
        System.out.println("Custom Field 10704: " + ticket.getCustomfield_10704());

        // // Save cleaned JSON to file
        // mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputFilePath), root);
    }

    public List<Ticket> parseSearchResults() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(rawJson);

        List<Ticket> tickets = new ArrayList<>();
        JsonNode issues = root.get("issues");

        if (issues != null && issues.isArray()) {
            for (JsonNode issue : issues) {
                Ticket ticket = new Ticket();

                // Extract ticket details from the JSON structure
                String id = issue.get("key").asText();
                JsonNode fields = issue.get("fields");

                String summary = getTextValue(fields, "summary");
                String status = getTextValue(fields.path("status"), "name");
                String assignee = getTextValue(fields.path("assignee"), "displayName");
                String customfield_27101 = getTextValue(fields, "customfield_27101");
                String issuetype = getTextValue(fields.path("issuetype"), "name");
                String resolutiondate = getTextValue(fields, "resolutiondate");
                String customfield_10704 = getTextValue(fields, "customfield_10704");
                String priority = getTextValue(fields.path("priority"), "name");
                String created = getTextValue(fields, "created");
                String[] linkResults = getSeparatedLinks(fields.get("issuelinks"));

                ticket.setId(id);
                ticket.setSummary(summary);
                ticket.setStatus(status);
                ticket.setAssignee(assignee);
                ticket.setCustomfield_27101(customfield_27101);
                ticket.setIssuetype(issuetype);
                ticket.setResolutiondate(resolutiondate);
                ticket.setCustomfield_10704(customfield_10704);
                ticket.setPriority(priority);
                ticket.setCreated(created);
                ticket.setDefectJira(linkResults[0]); // defect ticket
                ticket.setLinkedIssues(linkResults[1]); // other linked issues

                tickets.add(ticket);
            }
        }

        return tickets;
    }

    public Ticket parseSingleIssue() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(rawJson);

        Ticket ticket = new Ticket();

        // Extract ticket details from the JSON structure (same as cleanJson method)
        String id = root.get("key").asText();
        JsonNode fields = root.get("fields");

        String summary = getTextValue(fields, "summary");
        String status = getTextValue(fields.path("status"), "name");
        String assignee = getTextValue(fields.path("assignee"), "displayName");
        String customfield_27101 = getTextValue(fields, "customfield_27101");
        String issuetype = getTextValue(fields.path("issuetype"), "name");
        String resolutiondate = getTextValue(fields, "resolutiondate");
        String customfield_10704 = getTextValue(fields, "customfield_10704");
        String[] linkResults = getSeparatedLinks(fields.get("issuelinks"));

        ticket.setId(id);
        ticket.setSummary(summary);
        ticket.setStatus(status);
        ticket.setAssignee(assignee);
        ticket.setCustomfield_27101(customfield_27101);
        ticket.setIssuetype(issuetype);
        ticket.setResolutiondate(resolutiondate);
        ticket.setCustomfield_10704(customfield_10704);
        ticket.setDefectJira(linkResults[0]);
        ticket.setLinkedIssues(linkResults[1]);
        
        return ticket;
    }

    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) return "";

        if (fieldNode.isTextual()) {
            return fieldNode.asText();
        } else if (fieldNode.isObject()) {
            // For custom fields like versions or selects, check for "value" or "name"
            JsonNode valueNode = fieldNode.get("value");
            if (valueNode != null && valueNode.isTextual()) {
                return valueNode.asText();
            }
            JsonNode nameNode = fieldNode.get("name");
            if (nameNode != null && nameNode.isTextual()) {
                return nameNode.asText();
            }
            // If no value/name, try to convert object to string
            return fieldNode.toString();
        } else if (fieldNode.isArray()) {
            // For multi-select fields, join values or take first
            if (fieldNode.size() > 0) {
                JsonNode first = fieldNode.get(0);
                if (first.isTextual()) {
                    return first.asText();
                } else if (first.isObject()) {
                    JsonNode val = first.get("value");
                    if (val != null && val.isTextual()) return val.asText();
                    JsonNode nam = first.get("name");
                    if (nam != null && nam.isTextual()) return nam.asText();
                }
            }
            return fieldNode.toString();
        } else {
            // For other types (boolean, number), convert to string
            return fieldNode.asText();
        }
    }

    private String[] getSeparatedLinks(JsonNode issuelinksNode) {
        String[] result = {"", ""}; // [defectKey, otherLinks]

        if (issuelinksNode == null || !issuelinksNode.isArray()) {
            return result;
        }

        List<String> defectKeys = new ArrayList<>();
        List<String> otherKeys = new ArrayList<>();

        for (JsonNode link : issuelinksNode) {
            String linkType = getTextValue(link, "type");
            boolean isDefectLink = linkType != null &&
                                 (linkType.toLowerCase().contains("defect") ||
                                  linkType.toLowerCase().contains("bug") ||
                                  linkType.toLowerCase().contains("issue"));

            if (link.has("outwardIssue")) {
                JsonNode outward = link.get("outwardIssue");
                if (outward.has("key")) {
                    String key = outward.get("key").asText();
                    if (isDefectLink) {
                        defectKeys.add(key);
                    } else {
                        otherKeys.add(key);
                    }
                }
            }
            if (link.has("inwardIssue")) {
                JsonNode inward = link.get("inwardIssue");
                if (inward.has("key")) {
                    String key = inward.get("key").asText();
                    if (isDefectLink) {
                        defectKeys.add(key);
                    } else {
                        otherKeys.add(key);
                    }
                }
            }
        }

        // Take first defect key found (if any)
        if (!defectKeys.isEmpty()) {
            result[0] = defectKeys.get(0);
        }

        // Join other linked issues
        if (!otherKeys.isEmpty()) {
            result[1] = String.join(", ", otherKeys);
        }

        return result;
    }

    private String getLinkedIssuesText(JsonNode issuelinksNode) {
        if (issuelinksNode == null || !issuelinksNode.isArray()) {
            return "";
        }

        List<String> linkedIssueKeys = new ArrayList<>();
        for (JsonNode link : issuelinksNode) {
            if (link.has("outwardIssue")) {
                JsonNode outward = link.get("outwardIssue");
                if (outward.has("key")) {
                    linkedIssueKeys.add(outward.get("key").asText());
                }
            }
            if (link.has("inwardIssue")) {
                JsonNode inward = link.get("inwardIssue");
                if (inward.has("key")) {
                    linkedIssueKeys.add(inward.get("key").asText());
                }
            }
        }

        return String.join(", ", linkedIssueKeys);
    }
}
