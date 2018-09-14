package com.opfocus.passwordmgr;

import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.Error;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.soap.enterprise.sobject.Password__c;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import java.util.ArrayList;
import java.util.Scanner;

public class PasswordChangerMain {
    static final String DEFAULT_USERNAME = "bfinn@opfocus.com.june2018sb";
    static final String DEFAULT_PASSWORD = "0pF0cus2018!MCvBGE2FxCJuzB5eYwszO6WH";
    static final String SF_SB_ENDPOINT = "https://test.salesforce.com/services/Soap/c/43.0";
    static final String SF_PROD_ENDPOINT = "https://login.salesforce.com/services/Soap/c/43.0";

    static EnterpriseConnection connection;

    public static void main(String[] args) {
        String username = DEFAULT_USERNAME;
        String password = DEFAULT_PASSWORD;
        String endpoint = SF_SB_ENDPOINT;

        if (args.length > 0) {
            Boolean blnIsSandbox = false;
            username = args[0];
            if (args.length == 2) {
                password = args[1];
            } else if (args.length == 3) {
                password = args[1];
                blnIsSandbox = args[2].equalsIgnoreCase("sandbox");
                endpoint = blnIsSandbox ? SF_SB_ENDPOINT : SF_PROD_ENDPOINT;
            }
            System.out.println("====> Logging in with user parameters : " + username + " for " +
                    (blnIsSandbox ? " sandbox " : " production ") + " org.");
        } else {
            System.out.println("====> Logging in with default configuration : " + DEFAULT_USERNAME + " @ OpFocus June2018sb");
        }
        // Prompt the user to specify a prefix. Should be at least 5 characters, mix of upper and lower case.
        Scanner scanner = new Scanner(System.in);
        String prompt = "Please provide password prefix. It must be at least 5 characters with a mix of upper and lower case: ";
        System.out.println(prompt);
        prefix = scanner.next();

        boolean hasUppercase = !prefix.equals(prefix.toLowerCase());
        boolean hasLowercase = !prefix.equals(prefix.toUpperCase());

        while (prefix.length() < 5 || !hasUppercase || !hasLowercase) {
            String errorMsg = "Prefix does not meet requirements.  ";
            errorMsg += prompt;
            System.out.println(errorMsg);
            prefix = scanner.next();
            hasUppercase = !prefix.equals(prefix.toLowerCase());
            hasLowercase = !prefix.equals(prefix.toUpperCase());
        }
        System.out.println("====> prefix = " + prefix);

        // get additional query criteria
        System.out.println("Please provide additional criteria for querying Password records: (Specify \'none \' to accept default : \'" +
                DEFAULT_WHERE_CLAUSE + "\') ");
        String whereClause = scanner.next();
        if (whereClause.equalsIgnoreCase(NONE)) {
            whereClause = "";
        }

        ConnectorConfig config = new ConnectorConfig();
        config.setUsername(username);
        config.setPassword(password);
        config.setAuthEndpoint(endpoint);
        config.setServiceEndpoint(endpoint);
        //config.setTraceMessage(true);

        try {
            connection = Connector.newConnection(config);
            // display some current settings
            System.out.println("Auth EndPoint: " + config.getAuthEndpoint());
            System.out.println("Service EndPoint: " + config.getServiceEndpoint());
            System.out.println("Username: " + config.getUsername());
            System.out.println("SessionId: " + config.getSessionId());

            getPasswords(whereClause);
        } catch (ConnectionException e1) {
            e1.printStackTrace();
        }
    }

    private static void getPasswords(String whereClause) {
        System.out.println("Querying our Password Records...");

        try {
            // query for our Password Records
            /**
             *  select Id, Active__c, Custom_Domain_URL__c, Name, Sandbox__c, User_Name__c, Password__c,
             *  Security_Token__c,Password_Changed_Date__c from Password__c
             *  where Active__c=true and Do_Not_Change__c=false and Non_SFDC_URL__c=null and
             *  Partner_Portal__c=false and Self_Service_Portal__c=false and Customer_Portal__c=false
             */
            QueryResult queryResults = connection.query("select Id, Account_Name__c, Active__c, Custom_Domain_URL__c, Name, Sandbox__c, User_Name__c, Password__c,  " +
                    "Security_Token__c,Password_Changed_Date__c from Password__c " +
                    "where " + DEFAULT_WHERE_CLAUSE + whereClause);
            ArrayList<Password__c> passwordRecords = null;
            if (queryResults.getSize() > 0) {
                passwordRecords = new ArrayList<Password__c>();
                for (SObject record : queryResults.getRecords()) {
                    // cast the SObject to a strongly-typed Password

                    Password__c password = (Password__c)record;
                    passwordRecords.add(password);
                }
                PasswordChangerEngine engine = new PasswordChangerEngine(connection, passwordRecords, prefix);
                engine.updatePasswords();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String prefix;
    private static final String DEFAULT_WHERE_CLAUSE = "Active__c=true and Do_Not_Change__c=false and Non_SFDC_URL__c=null and " +
        "Partner_Portal__c=false and Self_Service_Portal__c=false and Customer_Portal__c=false";
    private static final String NONE = "NONE";
}
