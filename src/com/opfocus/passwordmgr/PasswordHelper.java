package com.opfocus.passwordmgr;

import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.enterprise.sobject.Password__c;
import com.sforce.soap.enterprise.sobject.User;

public class PasswordHelper {
    private EnterpriseConnection connection;
    private Password__c password;
    private Boolean blnHasValidCredentials;
    private String notes;
    private User user;

    public PasswordHelper(Password__c password) {
        this.password = password;
        this.blnHasValidCredentials = true;
        this.notes = "";
    }

    public Password__c getPassword() {
        return password;
    }

    public EnterpriseConnection getConnection() {
        return connection;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setValidCredentials(Boolean blnHasValidCredentials) {
        this.blnHasValidCredentials = blnHasValidCredentials;
    }

    public void setValidCredentials(EnterpriseConnection connection, Boolean blnHasValidCredentials) {
        this.connection = connection;
        this.blnHasValidCredentials = blnHasValidCredentials;
    }

    public Boolean areCredentialsValid() {
        return this.blnHasValidCredentials;
    }

    public void addNotes(String newNotes) {
        this.notes += newNotes;
    }

    public String getNotes() {
        return this.notes;
    }

    public void updatePassword(String newPasswordValue) {
        password.setPassword__c(newPasswordValue);
    }
}
