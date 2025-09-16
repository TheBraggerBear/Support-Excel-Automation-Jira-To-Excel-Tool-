package com.oracleinternshiip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
        String key = "ATATT3xFfGF0ggGq3WIF086JdZp9yAksq5lzDK2j6UMg88Jyhi2FBzvUe7q4-obDnM0u5M-K_NV-S4VlZQ2n3LpuHh8M3NPVgws27H26Xb6qpdXXn2Sv-o46icAJFSgCTWYwheyYflQH_XVzNg9cK0VGizYvQT3bbyRUOT8T6-pv3GubFFgT0JI=8DA9FF53";

        JiraAPIClient client = new JiraAPIClient("https://jira2.cerner.com", "aiden.burgstahle@oracle.com", key);
        try {
            client.getIssue("DTVIEWER-18999");
            System.out.println("Success");
            
        } catch (Exception e) {
            e.printStackTrace();
    }
}
}