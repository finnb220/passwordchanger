package com.opfocus;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONException;

public class SalesforceConnectionManager {	    
    private String clientId;
    private String clientSecret;
    
    public void setClientId(String clientId) {
    	this.clientId = clientId;
    }
    
    public void setClientSecret(String clientSecret) {
    	this.clientSecret = clientSecret;
    }
    
    public SalesforceConnectionManager() {}
    
    public void login(String endpoint, String payload) throws Exception {	
    DefaultHttpClient httpclient = new DefaultHttpClient();

        // Assemble the login request URL
        String loginURL = endpoint;

        // Login requests must be POSTs
        System.out.println("=====> loginURL = " + loginURL);
        HttpPost httpPost = new HttpPost(loginURL);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        HttpEntity entity = new StringEntity(payload);
        httpPost.setEntity(entity);
        
        httpPost.setEntity(entity);
        HttpResponse response = null;

        try {
            // Execute the login POST request
            response = httpclient.execute(httpPost);
        } catch (ClientProtocolException cpException) {
            // Handle protocol exception
        } catch (IOException ioException) {
            // Handle system IO exception
        }

        // verify response is HTTP OK
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            System.out.println("Error authenticating to Force.com: "+ statusCode + 
            		", " + response.getStatusLine().getReasonPhrase() + ", response = " + 
            		EntityUtils.toString(response.getEntity()));
            // Error is in EntityUtils.toString(response.getEntity()) 
            return;
        }

        String getResult = null;
        try {
            getResult = EntityUtils.toString(response.getEntity());
        } catch (IOException ioException) {
            // Handle system IO exception
        }
        JSONObject jsonObject = null;
        String loginAccessToken = null;
        String loginInstanceUrl = null;
        try {
            jsonObject = (JSONObject) new JSONTokener(getResult).nextValue();
            loginAccessToken = jsonObject.getString("access_token");
            loginInstanceUrl = jsonObject.getString("instance_url");
        } catch (JSONException jsonException) {
            // Handle JSON exception
        	System.err.println("ERROR : " + jsonException.getMessage());
        }
        System.out.println(response.getStatusLine());
        System.out.println("Successful login");
        System.out.println("  instance URL: "+loginInstanceUrl);
        System.out.println("  access token/session ID: "+loginAccessToken);

        // release connection
        httpPost.releaseConnection();
    }
}