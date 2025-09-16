package com.oracleinternshiip;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;

import java.io.IOException;

public class JiraAPIClient {

    private final String baseUrl;
    private final String username;
    private final String apiToken;
    private final OkHttpClient client;

    public JiraAPIClient(String baseUrl, String username, String apiToken) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.apiToken = apiToken;
        this.client = new OkHttpClient();
    }

    public JSONObject getIssue(String issueId) throws IOException {
        String url = baseUrl + "/rest/api/2/issue/" + issueId;
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", Credentials.basic(username, apiToken))
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                System.out.println(json);
                // Parse JSON and return as JSONObject
                return new JSONObject(json);
            } else {
                throw new IOException("Failed to fetch issue: " + response.code() + " " + response.message());
            }
        }
    }

    // Example: extract summary and description from issue JSON
    public void printIssueDetails(String issueId) throws IOException {
        JSONObject issue = getIssue(issueId);
        JSONObject fields = issue.getJSONObject("fields");
        String summary = fields.optString("summary");
        String description = fields.optString("description");
        System.out.println("Summary: " + summary);
        System.out.println("Description: " + description);
    }
}
