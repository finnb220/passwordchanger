package com.opfocus;

import org.apache.commons.codec.binary.Base64;

import java.io.*; 
import java.net.URLEncoder;
import java.security.*; 
import java.text.MessageFormat;  

public class JWTBearerFlowDriver {
  private StringBuffer token;
  private static final String header = "{\"alg\":\"RS256\"}";
  /* 
   * iss: Issuer - should be Consumer Key of SF Connected App
   * sub: Subject - should be Salesforce username
   * aud: Audience - should be the Salesforce login endpoint
   * exp: Expiration Time - should be within 5 minutes; format as # of seconds
   * 		from 1970-01-01T0:0:0Z 
   */
  private static final String claimTemplate = "'{'\"iss\": \"{0}\", \"sub\": \"{1}\", \"aud\": \"{2}\", \"exp\": \"{3}\"'}'";
  private static final String OAUTH_ACCESS = "https://login.salesforce.com/services/oauth2/token";
  public static void main(String[] args) {	  	
    try {
	  	JWTBearerFlowDriver driver = new JWTBearerFlowDriver();
	  	driver.generateJWTBearerToken();
    } catch (Exception e) {
        e.printStackTrace();
    }
  }
  
  @SuppressWarnings("deprecation")
private void generateJWTBearerToken() throws Exception {
	  token = new StringBuffer();
	  
      //Encode the JWT Header and add it to our string to sign
      token.append(Base64.encodeBase64URLSafeString(header.getBytes("UTF-8")));

      //Separate with a period
      token.append(".");

      //Create the JWT Claims Object
      String[] claimArray = new String[4];
      // Connected App's (Name=JWT OAuth POC) Consumer Key
      claimArray[0] = "3MVG9xOCXq4ID1uG7VarrZmsBKAXjq1dD8sgx5eF6ljrYi5RZXKy.ZAUS9MvN6i02itzlWBSDls6EIXtUtfAI";
      claimArray[1] = "finnb220@gmail.com";
      claimArray[2] = "https://login.salesforce.com";
      claimArray[3] = Long.toString( ( System.currentTimeMillis()/1000 ) + 300);
      MessageFormat claims = new MessageFormat(claimTemplate);
      String payload = claims.format(claimArray);

      //Add the encoded claims object
      token.append(Base64.encodeBase64URLSafeString(payload.getBytes("UTF-8")));

      String alias = "jwtkey";
      String password = "password";
      //Load the private key from a keystore
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(new FileInputStream("/applications/apache-tomcat-7.0.61/security/jwt11817.jks"), password.toCharArray());
      PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, password.toCharArray());

      //Sign the JWT Header + "." + JWT Claims Object
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initSign(privateKey);
      signature.update(token.toString().getBytes("UTF-8"));
      String signedPayload = Base64.encodeBase64URLSafeString(signature.sign());

      //Separate with a period
      token.append(".");

      //Add the encoded signature
      token.append(signedPayload);

      System.out.println(token.toString());	 
      
      String loginUrl = OAUTH_ACCESS;
      loginUrl += "?assertion=" + token;
      loginUrl += "&grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8");


      System.out.println("====> Login URL = " + loginUrl);

      SalesforceConnectionManager sfConnectMgr = new SalesforceConnectionManager();
      sfConnectMgr.login(loginUrl, token.toString());
  }
}