package com.opfocus.passwordmgr;

import com.sforce.soap.enterprise.*;
import com.sforce.soap.enterprise.sobject.Account;
import com.sforce.soap.enterprise.sobject.SObject;
import com.sforce.soap.enterprise.sobject.User;
import com.sforce.soap.enterprise.sobject.Password__c;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Scanner;
import java.util.Random;


public class PasswordChangerEngine {
    private ArrayList<PasswordHelper> helpers;
    private String prefix;
    private EnterpriseConnection opfocusConnection;
    private String today;

    public PasswordChangerEngine(EnterpriseConnection opfocusConnection, ArrayList<Password__c> passwords, String prefix) {
        helpers = new  ArrayList<PasswordHelper>();
        for (Password__c password : passwords) {
            helpers.add(new PasswordHelper(password));
        }
        this.opfocusConnection = opfocusConnection;
        this.prefix = prefix;
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        this.today =  dateFormat.format(new Date());
    }


    public void updatePasswords() {
        Scanner scanner = new Scanner(System.in);

        System.out.print(helpers.size() + " password records will be changed. Do you want to continue? (Y/N) ");
        String answer = scanner.next();
        if (answer.equalsIgnoreCase("N")) {
            // not updating passwords- return now
            System.out.println("======> Not Updating Passwords. Exiting now.");
            return;
        }
        Random generator = new Random();
        for (PasswordHelper helper : helpers) {
            if (validateCredentials(helper)) {
                // update password record
                String randomValue = String.format("%04d", generator.nextInt(10000));
                String newPassword = prefix + HYPHEN + randomValue;
                System.out.println("=====> randomValue = " + randomValue + ", password = " + newPassword);
                helper.updatePassword(newPassword);
                updatePassword(helper);
            }
        }
        updatePasswordRecords();
    }

//    public void outputInvalidPasswordResults() {
//        for (PasswordHelper helper : helpers) {
//            Password__c password = helper.getPassword();
//            System.out.println("=====> Password Record " + password.getName() + " (Id = " + password.getId() + ") update failed : "  +
//                    " Password Record is invalid in OpFocus Org : unable to login to update.");
//        }
//    }

    /**
     * Update the Password in the client org for the Password record associated with helper
     *
     * @param helper containing details of password record including updated password and status
     * @return true if we are able to successfully update the password in the client org, false otherwise
     */
    private Boolean updatePassword(PasswordHelper helper) {
        System.out.println("======> Updating Password for " + helper.getPassword());
        try {
            SetPasswordResult result = helper.getConnection().setPassword(helper.getUser().getId(), helper.getPassword().getPassword__c());
            System.out.println("The password for user ID " + helper.getUser().getId() + " changed to "
                    + helper.getPassword().getPassword__c());

            helper.addNotes(SUCCESS_MESSAGE + today);
            return true;
        } catch (ConnectionException ce) {
            helper.addNotes("Error changing password " +  today + " : " + ce.getMessage());
            System.err.println("Unable to change password for user ID " + helper.getUser().getId() + " : exception = " + ce);
            ce.printStackTrace();
            return false;
        }
    }

    private boolean validateCredentials(PasswordHelper helper) {
        ConnectorConfig config = new ConnectorConfig();
        Password__c password = helper.getPassword();
        config.setUsername(password.getUser_Name__c());
        config.setPassword(password.getPassword__c() + password.getSecurity_Token__c());
        if (password.getSandbox__c()) {
            config.setAuthEndpoint(PasswordChangerMain.SF_SB_ENDPOINT);
        } else {
            config.setAuthEndpoint(PasswordChangerMain.SF_PROD_ENDPOINT);
        }

        EnterpriseConnection connection;
        try {
            connection = Connector.newConnection(config);
            helper.setValidCredentials(connection, true);
            QueryResult results = connection.query("select Id, Name from User where UserName=\'" +
                    password.getUser_Name__c() + "\' and IsActive=true");
            if (results.getSize() > 0) {
                for (SObject record : results.getRecords()) {
                    User user = (User)record;
                    helper.setUser(user);
                }
            }
        } catch (ConnectionException exc) {
            helper.setValidCredentials(false);
            String message = "";
            if (exc.toString().contains("INVALID_LOGIN")) {
                message = "Error changing password " +  today + " : Invalid Login " + exc;
            } else {
                message = "Error changing password " + today + "  Exception thrown trying to login : details = " +
                        exc;
            }
            System.err.println("=====> " + message);
            helper.addNotes(message);
            return false;
        }
        return true;
    }


    private void updatePasswordRecords() {
        System.out.println("Updating Password Records...");
        Password__c records[] = new Password__c[helpers.size()];
        String fieldsToNull[] = new String[]{"Security_Token__c", "SToken_Date__c"};

        Integer idx = 0;
        for (PasswordHelper helper : helpers) {
            helper.getPassword().setAccount_Name__c(null); // read-only field so empty it out for update
            Password__c password = helper.getPassword();
            if (helper.areCredentialsValid()) {
                password.setFieldsToNull(fieldsToNull);
            }
            password.setAutomated_Password_Change_Notes__c(helper.getNotes());
            records[idx++] = helper.getPassword();
            System.out.println("=======> Updating password record " + helper.getPassword().toString());
        }

        try {
            // update the records in Salesforce.com
            SaveResult[] saveResults = opfocusConnection.update(records);

            // check the returned results for any errors
            for (int i=0; i< saveResults.length; i++) {
                if (saveResults[i].isSuccess()) {
                    System.out.println("====> Password Record " +  records[i].getName() + " (Id= " + saveResults[i].getId() + ") successfully updated.");
                } else {
                    com.sforce.soap.enterprise.Error[] errors = saveResults[i].getErrors();
                    for (int j=0; j< errors.length; j++) {
                        System.out.println("=====> Unable to update password record in OpFocus Org:  " + records[i].getName() + " (Id = " + saveResults[i].getId() + ") update failed : "  +
                                errors[j].getMessage());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String HYPHEN= "-";
    private static String SUCCESS_MESSAGE = "Password changed on ";

}
