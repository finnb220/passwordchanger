package com.opfocus;

import java.io.IOException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.ClientProtocolException;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONException;

public class OAuth2WebFlowDriver {

		
    static final String USERNAME     = "finnb220@gmail.com";
    static final String PASSWORD     = "D0m1n1c3002q2NwDApYIGj2TwLgjv5DuUZX";
    static final String LOGINURL     = "https://login.salesforce.com";
    // TODO:  replace grant_type=password with grant_type=authorization_code
//    static final String GRANTSERVICE = "/services/oauth2/token?grant_type=password";
    static final String GRANTSERVICE = "/services/oauth2/token?grant_type=authorization_code";
    static final String CLIENTID     = "3MVG9xOCXq4ID1uG7VarrZmsBKCrvJhrrYzHscwYUx5p0MtWqNTLtSROy1xCXpdEntqvbVGXnOBKr9rVJumok";
    static final String CLIENTSECRET = "4489179033385544145";
    static final String REDIRECT_URI  = "https://98.249.49.220:8443";
    
    public static void main(String[] args) {

        DefaultHttpClient httpclient = new DefaultHttpClient();

        // Assemble the login request URL
        String loginURL = LOGINURL + 
                          GRANTSERVICE + 
                          "&client_id=" + CLIENTID + 
                          "&client_secret=" + CLIENTSECRET +
                          "&redirect_uri="  + REDIRECT_URI +
                          "&response_type=code";
//                          "&username=" + USERNAME +
 //                        "&password=" + PASSWORD;

        // Login requests must be POSTs
        HttpPost httpPost = new HttpPost(loginURL);
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
            System.out.println("Error authenticating to Force.com: "+statusCode);
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
        }
        System.out.println(response.getStatusLine());
        System.out.println("Successful login");
        System.out.println("  instance URL: "+loginInstanceUrl);
        System.out.println("  access token/session ID: "+loginAccessToken);

        // release connection
        httpPost.releaseConnection();
    }
}