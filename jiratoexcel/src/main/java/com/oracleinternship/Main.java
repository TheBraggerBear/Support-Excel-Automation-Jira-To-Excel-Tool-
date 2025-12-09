package com.oracleinternship;

import java.io.IOException;

import javafx.application.Application;

public class Main {
        public static void main(String[] args) {
        String jiraBaseUrl = "https://jira2.cerner.com/";
        boolean debugMode = false; // true or false to enable/disable debug logs

        // JiraApiClient jira = new JiraApiClient(jiraBaseUrl, personalAccessToken, debugMode);

        // this was for testing the JiraApiClient and JsonParser classes
        // try {
        //     String issueKey = "DTVIEWER-19479";
        //     String response = jira.getIssue(issueKey);
        //     System.out.println("\nIssue Data:\n" + response);
        //     JsonParser parser = new JsonParser(response);
        //     Ticket ticket = new Ticket();
        //     System.out.println();
        //     parser.cleanJson("id", "cleaned_issue.json", ticket);
        //     // System.out.println("Cleaned JSON saved to cleaned_issue.json");
        //     ExcelWriter.writeIssues(ticket.getFieldValues(), "jiratoexcel/test/ApacheCOITest.xlsx"); // Test Apache POI
        // } catch (IOException | InterruptedException e) {
        //     System.err.println("Error fetching issue: " + e.getMessage());
        //     if (debugMode) e.printStackTrace();
        // }

        MainApp.launch(MainApp.class, args);


    }
}
