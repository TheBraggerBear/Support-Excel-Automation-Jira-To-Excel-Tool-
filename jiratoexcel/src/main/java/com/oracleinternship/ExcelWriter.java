package com.oracleinternship;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelWriter {
    public static void writeIssues(String[] issues, String filePath) throws IOException {
        // Delete existing file if it exists to ensure overwrite
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        try (XSSFWorkbook wb = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(filePath)) {
            XSSFSheet sheet = wb.createSheet("Issues");
            int row = 0;
            XSSFRow header = sheet.createRow(row++);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Summary");
            header.createCell(2).setCellValue("Status");

            for (String issue : issues) {
                XSSFRow r = sheet.createRow(row++);
                r.createCell(0).setCellValue(issue);
            }

            for (int i = 0; i < 3; i++) sheet.autoSizeColumn(i);
            wb.write(out);
        }
    }

    public static void writeTickets(List<Ticket> tickets, String filePath, String baseUrl) throws IOException {
        // Delete existing file if it exists to ensure overwrite
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        try (XSSFWorkbook wb = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(filePath)) {
            XSSFSheet sheet = wb.createSheet("Tickets");

            // Create header style (green accent 6, 50% darker - Excel theme color)
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Font headerFont = wb.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Add borders to header style
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Create data style (with borders and text wrapping)
            CellStyle dataStyle = wb.createCellStyle();
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setWrapText(true); // Enable text wrapping

            // Create header row
            int row = 0;
            XSSFRow header = sheet.createRow(row++);
            String[] headers = {"ID", "Summary", "Root Cause", "Toolkit Version", "Viewer Version", "Issue Type", "Defect Jira", "Ticket Priority", "Waiting Time", "Investigation Effort", "Assignee", "Current Status",
                               "Ageing", "Next Action Item", "Comments", "Date Created", "Date Resolved", "Linked Issues"};

            for (int i = 0; i < headers.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            for (Ticket ticket : tickets) {
                XSSFRow r = sheet.createRow(row++);
                // Create hyperlink for ticket ID
                var idCell = r.createCell(0);
                idCell.setCellValue(ticket.getId());
                idCell.setCellStyle(dataStyle); // Apply border style
                if (baseUrl != null && !baseUrl.isEmpty()) {
                    XSSFHyperlink link = wb.getCreationHelper().createHyperlink(HyperlinkType.URL);
                    link.setAddress(baseUrl + "/browse/" + ticket.getId());
                    idCell.setHyperlink(link);
                    // Let Excel handle the hyperlink styling automatically
                }

                var cell1 = r.createCell(1); cell1.setCellValue(ticket.getSummary()); cell1.setCellStyle(dataStyle);
                var cell2 = r.createCell(2); cell2.setCellValue("null"); cell2.setCellStyle(dataStyle); // Root Cause
                if (ticket.getCustomfield_10704() == null || ticket.getCustomfield_10704().isEmpty()) {
                    var cell3 = r.createCell(3); cell3.setCellValue("N/A"); cell3.setCellStyle(dataStyle); // Toolkit Version
                    var cell4 = r.createCell(4); cell4.setCellValue("N/A"); cell4.setCellStyle(dataStyle); // Viewer Version
                } else if (ticket.getCustomfield_10704().contains("Management")) {
                    var cell3 = r.createCell(3); cell3.setCellValue(ticket.getCustomfield_10704()); cell3.setCellStyle(dataStyle); // Toolkit Version
                    var cell4 = r.createCell(4); cell4.setCellValue("N/A"); cell4.setCellStyle(dataStyle); // Viewer Version
                } else {
                    var cell4 = r.createCell(4); cell4.setCellValue(ticket.getCustomfield_10704()); cell4.setCellStyle(dataStyle); // Viewer Version
                    var cell3 = r.createCell(3); cell3.setCellValue("N/A"); cell3.setCellStyle(dataStyle); // Toolkit Version
                }
                var cell5 = r.createCell(5); cell5.setCellValue(ticket.getIssuetype()); cell5.setCellStyle(dataStyle); // Issue Type
                // Defect Jira - with hyperlink
                var cell6 = r.createCell(6);
                String defectKey = ticket.getDefectJira();
                if (defectKey != null && !defectKey.trim().isEmpty()) {
                    cell6.setCellValue(defectKey);
                    if (baseUrl != null && !baseUrl.isEmpty()) {
                        XSSFHyperlink link = wb.getCreationHelper().createHyperlink(HyperlinkType.URL);
                        link.setAddress(baseUrl + "/browse/" + defectKey);
                        cell6.setHyperlink(link);
                    }
                } else {
                    cell6.setCellValue("No Defect Jira");
                }
                cell6.setCellStyle(dataStyle);
                var cell7 = r.createCell(7); cell7.setCellValue(ticket.getPriority()); cell7.setCellStyle(dataStyle); // Ticket Priority
                var cell8 = r.createCell(8); cell8.setCellValue(parseWaitingTime(ticket.getCustomfield_27101())); cell8.setCellStyle(dataStyle); // Waiting Time
                var cell9 = r.createCell(9); cell9.setCellValue("null"); cell9.setCellStyle(dataStyle); // Investigation Effort
                var cell10 = r.createCell(10); cell10.setCellValue(ticket.getAssignee()); cell10.setCellStyle(dataStyle); // Assignee
                var cell11 = r.createCell(11); cell11.setCellValue(ticket.getStatus()); cell11.setCellStyle(dataStyle); // Current Status
                var cell12 = r.createCell(12); cell12.setCellValue("null"); cell12.setCellStyle(dataStyle); // Ageing
                var cell13 = r.createCell(13); cell13.setCellValue("null"); cell13.setCellStyle(dataStyle); // Next Action Item
                var cell14 = r.createCell(14); cell14.setCellValue("null"); cell14.setCellStyle(dataStyle); // Comments
                var cell15 = r.createCell(15); cell15.setCellValue(formatDate(ticket.getCreated())); cell15.setCellStyle(dataStyle); // Date Created
                var cell16 = r.createCell(16); cell16.setCellValue(formatDate(ticket.getResolutiondate())); cell16.setCellStyle(dataStyle); // Date Resolved
                // Linked Issues - show other linked issues without hyperlinks
                var cell17 = r.createCell(17);
                String linkedIssuesText = ticket.getLinkedIssues();
                cell17.setCellValue(linkedIssuesText != null ? linkedIssuesText : "");
                cell17.setCellStyle(dataStyle);
                
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
        }
    }

    private static String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }
        try {
            // Assuming ISO format like "2023-10-01T12:00:00.000Z"
            LocalDateTime dateTime = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");
            return dateTime.format(formatter);
        } catch (Exception e) {
            // If parsing fails, return original string
            return dateString;
        }
    }

    private static String parseWaitingTime(String htmlString) {
        if (htmlString == null || htmlString.isEmpty()) {
            return "";
        }
        // Regex to extract something like "8.0 days" from HTML
        Pattern pattern = Pattern.compile("<[^>]*>([^<]+)<[^>]*>");
        Matcher matcher = pattern.matcher(htmlString);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return htmlString; // Return original if no match
    }
}
