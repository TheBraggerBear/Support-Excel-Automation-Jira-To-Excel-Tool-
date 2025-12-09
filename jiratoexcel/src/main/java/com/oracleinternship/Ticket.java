package com.oracleinternship;

// POJO class for Ticket representation
public class Ticket {
    private String id, assignee, status, summary, customfield_27101, issuetype, defectJira, resolutiondate, customfield_10704, priority, created, linkedIssues;
    private String[] fields = {"id", "summary", "viewer_version", "waiting_time", "assignee", "current_status", "issue_type", "defect_jira","ticket_priority", "date_created", "date_resolved"};

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getCustomfield_27101() {
        return customfield_27101;
    }

    public void setCustomfield_27101(String customfield_27101) {
        this.customfield_27101 = customfield_27101;
    }

    public String getIssuetype() {
        return issuetype;
    }

    public void setIssuetype(String issuetype) {
        this.issuetype = issuetype;
    }

    public String getResolutiondate() {
        return resolutiondate;
    }

    public void setResolutiondate(String resolutiondate) {
        this.resolutiondate = resolutiondate;
    }

    public String getCustomfield_10704() {
        return customfield_10704;
    }

    public void setCustomfield_10704(String customfield_10704) {
        this.customfield_10704 = customfield_10704;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getDefectJira() {
        return defectJira;
    }

    public void setDefectJira(String defectJira) {
        this.defectJira = defectJira;
    }
    
    public String getLinkedIssues() {
        return linkedIssues;
    }

    public void setLinkedIssues(String linkedIssues) {
        this.linkedIssues = linkedIssues;
    }

    public String[] getFields() {
        return fields;
    }

    public String[] getFieldValues() {
        return new String[] {
            id, summary, customfield_10704, customfield_27101, assignee, status, issuetype, priority, created, resolutiondate
        };
    }

    public void printFields() {
        for (String string : fields) {
            System.out.println(string);
        }
    }
}
