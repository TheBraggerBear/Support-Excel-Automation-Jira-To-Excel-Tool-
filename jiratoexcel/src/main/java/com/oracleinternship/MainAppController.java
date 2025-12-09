package com.oracleinternship;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

public class MainAppController {

    @FXML
    private TextField jiraUrlField;

    @FXML
    private Label projectLabel;

    @FXML
    private Label dateRangeLabel;

    @FXML
    private Label toLabel;

    @FXML
    private Label fromLabel;

    @FXML
    private Label assigneeLabel;

    @FXML
    private Label issueKeyLabel;

    @FXML
    private CheckBox specificTicketCheck;

    @FXML
    private PasswordField patField;

    @FXML
    private TextField issueKeyField;

    @FXML
    private TextField assigneeField;

    @FXML
    private Button excelButton;

    @FXML
    private Button clearButton;

    @FXML
    private ComboBox<String> comboBox;

    @FXML
    private ComboBox<String> projectComboBox;

    @FXML
    private TextField newProjectField;

    @FXML
    private Button addProjectButton;

    @FXML
    private StackPane statusContainer;

    @FXML
    private TextArea statusArea;

    @FXML
    private DatePicker datePicker1, datePicker2;

    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    private void initialize() {
        // Initialize method called after FXML loading
        statusArea.appendText("Application started successfully!\n");
        statusArea.appendText("Ready to process Jira issues.\n");
        statusArea.appendText("⚠️ For Large Datasets, processing may take longer.\n");
        statusArea.appendText("⚠️ The application may appear unresponsive during this time. \n");
        issueKeyField.setDisable(true); // Disable issue key field by default
        issueKeyLabel.setDisable(true); // Disable issue key label by default
        
        // Initialize Project ComboBox with default projects
        if (projectComboBox != null) {
            projectComboBox.setItems(FXCollections.observableArrayList(
                "DTVIEWER",  // Default project
                "All Projects"
            ));
            projectComboBox.setValue("DTVIEWER"); // Set default selection
        }
    }

    @FXML
    private void addProject() {
        String newProject = newProjectField.getText().trim();
        if (newProject != null && !newProject.isEmpty()) {
            // Check if project already exists
            if (!projectComboBox.getItems().contains(newProject)) {
                projectComboBox.getItems().add(newProject);
                statusArea.appendText("Added project: " + newProject + "\n");
                newProjectField.clear();
            } else {
                statusArea.appendText("Project '" + newProject + "' already exists.\n");
            }
        } else {
            statusArea.appendText("Please enter a project key.\n");
        }
    }

    @FXML
    private void exportToExcel() {
        try {
            // Check if single ticket mode is selected
            if (specificTicketCheck.isSelected()) {
                String ticketKey = getIssueKey();
                if (ticketKey != null && !ticketKey.trim().isEmpty()) {
                    exportSingleTicket("Excel");
                } else {
                    statusArea.appendText("Please enter a ticket key when using single ticket mode.\n");
                }
            } else {
                // Use date range mode
                if (startDate != null && endDate != null) {
                    exportTicketsByDateRange("Excel");
                } else {
                    statusArea.appendText("Please select a date range first or check 'Grab Specific Ticket?' for single ticket mode.\n");
                }
            }
        } catch (Exception e) {
            statusArea.appendText("Error exporting to Excel: " + e.getMessage() + "\n");
        }
    }



    private void exportSingleTicket(String format) throws IOException, InterruptedException {
        String ticketKey = getIssueKey();
        statusArea.appendText("Fetching ticket: " + ticketKey + "\n");

        JiraApiClient jiraClient = new JiraApiClient(getJiraUrl(), getPersonalAccessToken(), false);
        String ticketResponse = jiraClient.getIssue(ticketKey);

        JsonParser parser = new JsonParser(ticketResponse);
        Ticket ticket = parser.parseSingleIssue();

        if (ticket == null) {
            statusArea.appendText("Failed to retrieve ticket: " + ticketKey + "\n");
            return;
        }

        statusArea.appendText("Successfully retrieved ticket: " + ticketKey + "\n");

        // Create a list with single ticket for ExcelWriter compatibility
        List<Ticket> tickets = List.of(ticket);

        // Ensure test directory exists
        Path testDir = Paths.get("test");
        try {
            Files.createDirectories(testDir);
        } catch (IOException e) {
            statusArea.appendText("Error creating test directory: " + e.getMessage() + "\n");
        }

        String fileName = "ticket_" + ticketKey + (format.equals("CSV") ? ".csv" : ".xlsx");
        String filePath = "test/" + fileName;

        if (format.equals("Excel")) {
            ExcelWriter.writeTickets(tickets, filePath, getJiraUrl());
            statusArea.appendText("Exported ticket to: " + filePath + "\n");
        } else {
            // TODO: Implement CSV export for single ticket
            statusArea.appendText("CSV export not yet implemented.\n");
        }
    }

    private void exportTicketsByDateRange(String format) throws IOException, InterruptedException {
        String assignee = getAssignee();
        String project = getSelectedProject();

        StringBuilder searchMessage = new StringBuilder();
        searchMessage.append("Searching for tickets from ").append(startDate).append(" to ").append(endDate);

        if (project != null && !project.trim().isEmpty() && !"All Projects".equals(project)) {
            searchMessage.append(" in project '").append(project).append("'");
        }

        if (assignee != null && !assignee.trim().isEmpty()) {
            searchMessage.append(" assigned to '").append(assignee).append("'");
        }

        searchMessage.append("...\n");
        statusArea.appendText(searchMessage.toString());


        JiraApiClient jiraClient = new JiraApiClient(getJiraUrl(), getPersonalAccessToken(), false);
        String searchResponse = jiraClient.searchIssues(startDate, endDate, assignee, project);

        JsonParser parser = new JsonParser(searchResponse);
        List<Ticket> tickets = parser.parseSearchResults();

        statusArea.appendText("Found " + tickets.size() + " tickets in the date range.\n");

        if (tickets.isEmpty()) {
            String noTicketsMessage = assignee != null && !assignee.trim().isEmpty()
                ? "No tickets found for the selected date range and assignee.\n"
                : "No tickets found for the selected date range.\n";
            statusArea.appendText(noTicketsMessage);
            return;
        }

        // Ensure test directory exists
        Path testDir = Paths.get("test");
        try {
            Files.createDirectories(testDir);
        } catch (IOException e) {
            statusArea.appendText("Error creating test directory: " + e.getMessage() + "\n");
        }

        String assigneeSuffix = assignee != null && !assignee.trim().isEmpty() ? "_assigned_to_" + assignee.replace(" ", "_") : "";
        String projectSuffix = project != null && !project.trim().isEmpty() && !"All Projects".equals(project) ? "_project_" + project : "";
        String fileName = "tickets_" + startDate + "_to_" + endDate + projectSuffix + assigneeSuffix + (format.equals("CSV") ? ".csv" : ".xlsx");
        String filePath = "test/" + fileName;

        if (format.equals("Excel")) {
            ExcelWriter.writeTickets(tickets, filePath, getJiraUrl());
            statusArea.appendText("Exported " + tickets.size() + " tickets to: " + filePath + "\n");
        } else {
            // TODO: Implement CSV export for multiple tickets
            statusArea.appendText("CSV export for multiple tickets not yet implemented.\n");
        }
    }

    @FXML
    private void clearStatus() {
        statusArea.clear();

        // Create a temporary label for the fading message
        Label fadeLabel = new Label("Status cleared");
        fadeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2e7d32; -fx-background-color: rgba(255, 255, 255, 0.9); -fx-padding: 10px; -fx-background-radius: 5px;");
        fadeLabel.setMaxWidth(Double.MAX_VALUE);
        fadeLabel.setAlignment(Pos.CENTER);

        // Add the label to the StackPane
        statusContainer.getChildren().add(fadeLabel);

        // Create fade transition
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(2), fadeLabel);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(0.5)); // Wait 0.5 seconds before starting fade

        // Remove the label when animation completes
        fadeOut.setOnFinished(event -> {
            statusContainer.getChildren().remove(fadeLabel);
        });

        // Start the animation
        fadeOut.play();
    }

    @FXML
    private void toggleFullscreen() {
        if (primaryStage.isFullScreen()) {
            primaryStage.setFullScreen(false);
            statusArea.appendText("Exited fullscreen mode.\n");
        } else {
            primaryStage.setFullScreen(true);
            statusArea.appendText("Entered fullscreen mode.\n");
        }
    }

    // Getter methods for accessing field values (for future functionality)
    public String getJiraUrl() {
        return jiraUrlField.getText();
    }

    public String getPersonalAccessToken() {
        return patField.getText();
    }

    public String getIssueKey() {
        return issueKeyField.getText();
    }

    public String getAssignee() {
        return assigneeField.getText();
    }

    @FXML
    public void specificBoxToggled() {
        boolean isSpecific = specificTicketCheck.isSelected();
        if (isSpecific) {
            statusArea.appendText("Specific Ticket mode enabled.\n");
        } else {
            statusArea.appendText("Date Range mode enabled.\n");
        }
        issueKeyField.setDisable(!isSpecific);
        datePicker1.setDisable(isSpecific);
        datePicker2.setDisable(isSpecific);
        newProjectField.setDisable(isSpecific);
        addProjectButton.setDisable(isSpecific);
        projectComboBox.setDisable(isSpecific);
        toLabel.setDisable(isSpecific);
        fromLabel.setDisable(isSpecific);
        dateRangeLabel.setDisable(isSpecific);
        projectLabel.setDisable(isSpecific);
        assigneeLabel.setDisable(isSpecific);
        issueKeyLabel.setDisable(!isSpecific);
    }

    public String getSelectedProject() {
        return projectComboBox != null ? projectComboBox.getValue() : null;
    }

    // Date range management
    private LocalDate startDate;
    private LocalDate endDate;

    @FXML
    public void setDateRange() {
        LocalDate start = datePicker1.getValue();
        LocalDate end = datePicker2.getValue();
        if (start != null && end != null) {
            this.startDate = start;
            this.endDate = end;
            statusArea.appendText("Date range set: " + start + " to " + end + "\n");
        }
    }

    // public void setDateRange(LocalDate startDate, LocalDate endDate) {
    //     this.startDate = startDate;
    //     this.endDate = endDate;
    //     statusArea.appendText("Date range set: " + startDate + " to " + endDate + "\n");
    // }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
