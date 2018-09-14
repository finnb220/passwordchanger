package com.opfocus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

@WebServlet(urlPatterns="/",
	loadOnStartup = 1,
	initParams = { 
	    @WebInitParam(name = "clientId", value = 
	            "3MVG9xOCXq4ID1uG7VarrZmsBKOIR1F8MI5hP9jTrjQ6tLTB9b39jm_CEo0VDqElzsyBtOMhyEz2bpTazGcKT"),
	    @WebInitParam(name = "clientSecret", value = "3957012050563520654"),
	    @WebInitParam(name = "redirectUri", 
	    		value = "https://192.168.0.131:8443/oauth_test"),
	    @WebInitParam(name = "environment", 
	    	value = "https://login.salesforce.com/")
	 })
public class OAuthServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	private static final String INSTANCE_URL = "INSTANCE_URL";
	
	private String clientId = null;
	private String clientSecret = null;
	private String redirectUri = null;
	private String environment = null;
	private String tokenUrl = null;
	private String oauthUrl = null;
	
	public void init() throws ServletException {
		clientId = this.getInitParameter("clientId");
		clientSecret = this.getInitParameter("clientSecret");
		redirectUri = this.getInitParameter("redirectUri");
		environment = this.getInitParameter("environment");

		System.out.println("==========> clientId = " + clientId);
		System.out.println("==========> clientSecret = " + clientSecret);
		System.out.println("==========> redirectUri = " + redirectUri);
		tokenUrl = environment + "/services/oauth2/token";
		oauthUrl = environment + "/services/oauth2/authorize?";
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException{

		String accessToken = (String) request.getSession().getAttribute(
				ACCESS_TOKEN);
		// if we do not have an access token - then we need to authorize ourselves 
		// with SF for the first time in a session
		if (accessToken == null) {
			String instanceUrl = null;
			if (request.getRequestURI().endsWith("oauth")) {
				// we need to send the user to authorize
				// STEP 1: initiate oauth dance.
				// We authenticate with SF authorization server
				initiateOAuthDance(response);
				return;
			} else {
				// We do not have an access token in session and request
				// was NOT for oAuth path. Lets see if we have a code
				String code = request.getParameter("code");
				// check to see if we need a new code.
				if (code != null) {
					// STEP 3: We got a code. Now we need to get the access token. 
					System.out.println("==========> Auth successful - got callback");
					System.out.println("==========> code = " + code);
					HttpClient httpClient =  HttpClientBuilder.create().build();
					HttpPost post = new HttpPost(tokenUrl);
					ArrayList<BasicNameValuePair> urlParameters = new ArrayList<BasicNameValuePair>();
					urlParameters.add(new BasicNameValuePair("code", code));
					urlParameters.add(new BasicNameValuePair("grant_type", "authorization_code"));
//					urlParameters.add(new BasicNameValuePair("grant_type", "password"));
					urlParameters.add(new BasicNameValuePair("client_id", clientId));
					urlParameters.add(new BasicNameValuePair("client_secret", clientSecret));
					urlParameters.add(new BasicNameValuePair("redirect_uri", redirectUri));
//					urlParameters.add(new BasicNameValuePair("username", "finnb220@gmail.com"));
//					urlParameters.add(new BasicNameValuePair("password", "D0m1n1c3002!ucaGsYatHIaT24M9dNjuryhVx"));
					post.setEntity(new UrlEncodedFormEntity(urlParameters));

					try {
						HttpResponse postResponse = httpClient.execute(post);
						try {
							// extract code out of response here for use in doPost later.
							BufferedReader rd = new BufferedReader(
							        new InputStreamReader(postResponse.getEntity().getContent()));
						 
							StringBuilder result = new StringBuilder();
							for (String line = null; (line=rd.readLine()) != null;) {
								result.append(line).append("\n");
							}

							System.out.println("==========> Parsed result =  "
									+ result.toString());
							JSONObject authResponse = new JSONObject(result.toString());
							System.out.println("finalResult = " + authResponse);
							accessToken = authResponse.getString("access_token");
							instanceUrl = authResponse.getString("instance_url");
							String issuedAt = authResponse.getString("issued_at");
							
					    	/** Use this to validate session 
					    	  * instead of expiring on browser close.
					    	  */
							System.out.println("==========> Got access token: " + accessToken);
							System.out.println("==========> Got instance Url: " + instanceUrl);
							System.out.println("==========> Got issued At: " + issuedAt);					                               
						} catch (JSONException e) {
							e.printStackTrace();
							throw new ServletException(e);
						}
					} finally {
						post.releaseConnection();
					}
				} else {
					// our previous code must have expired - getting a new code
					System.out.println("==========> getting new code.");
					instanceUrl = null;
					// we need to send the user to authorize
					// Step 1: initiate oauth dance.
					// We authenticate with SF authorization server
					initiateOAuthDance(response);
					return;
				}
			}
			// Set a session attribute so that other servlets can get the access
			// token
			System.out.println("==========> Putting ACCESS_TOKEN = " + accessToken + " in session.");
			request.getSession().setAttribute(ACCESS_TOKEN, accessToken);
			// create a cookie for our access_token
			Cookie session = new Cookie("access_token" , accessToken);
			session.setMaxAge(-1); //cookie not persistent, destroyed on browser exit
			response.addCookie(session);

			// We also get the instance URL from the OAuth response, so set it
			// in the session too
			request.getSession().setAttribute(INSTANCE_URL, instanceUrl);
		}
		response.sendRedirect(request.getContextPath() + "/AccountHandler");
	}
	
	private void initiateOAuthDance(HttpServletResponse response) throws IOException, ServletException {
		// We authenticate with SF authorization server
		// providing these details from connected app
		// 1. clientId (previously called consumer id in OAuth 1.0)
		// 2. clientSecret (previously called consumer secret in OAuth 1.0)
		// 3. redirectUri (must match verbatim redirectUri in connected app)
	
		HttpClient httpClient =  HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(oauthUrl);

		ArrayList<BasicNameValuePair> urlParameters = new ArrayList<BasicNameValuePair>();
		urlParameters.add(new BasicNameValuePair("response_type", "code"));
		urlParameters.add(new BasicNameValuePair("client_id", clientId));
		urlParameters.add(new BasicNameValuePair("client_secret", clientSecret));
		urlParameters.add(new BasicNameValuePair("redirect_uri", redirectUri));
		
		
		post.setEntity(new UrlEncodedFormEntity(urlParameters));
		// authenticate with SF which will result in a subsequent 
		// call to us that is handled by below (Step 3).
		HttpResponse postResponse = httpClient.execute(post);
		System.out.println("==========> Response Code : " 
	            + postResponse.getStatusLine().getStatusCode());
		// extract code out of response here for use in doPost later.
		BufferedReader rd = new BufferedReader(
		        new InputStreamReader(postResponse.getEntity().getContent()));
	 
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}

		System.out.println("==========> Redirecting user to  = " + 
				postResponse.getFirstHeader("Location"));
		
		// Step 2: now we need to redirect to location (SF authorize) in response.
		response.sendRedirect(postResponse.getFirstHeader("Location").getValue());
	}
	
	/**
				// Step 1: initiate oauth dance.
				// We authenticate with SF authorization server
				// providing these details from connected app
				// 1. clientId (previously called consumer id in OAuth 1.0)
				// 2. clientSecret (previously called consumer secret in OAuth 1.0)
				// 3. redirectUri (must match verbatim redirectUri in connected app)
				 
				HttpClient httpClient =  HttpClientBuilder.create().build();
				// send request to SF authorization endpoint
				HttpPost post = new HttpPost(oauthUrl);

				ArrayList<BasicNameValuePair> urlParameters = new ArrayList<BasicNameValuePair>();
				// request a code
				urlParameters.add(new BasicNameValuePair("response_type", "code"));
				urlParameters.add(new BasicNameValuePair("client_id", clientId));
				urlParameters.add(new BasicNameValuePair("client_secret", clientSecret));
				urlParameters.add(new BasicNameValuePair("redirect_uri", redirectUri));
			 
				post.setEntity(new UrlEncodedFormEntity(urlParameters));
				// authenticate with SF which will result in a subsequent 
				// call to us that is handled by doPost method below.
				HttpResponse postResponse = httpClient.execute(post);
				System.out.println("==========> Response Code : " 
			            + postResponse.getStatusLine().getStatusCode());
				// extract code out of response here for use in doPost later.
				BufferedReader rd = new BufferedReader(
				        new InputStreamReader(postResponse.getEntity().getContent()));
			 
				StringBuilder result = new StringBuilder();
				for (String line = null; (line=rd.readLine()) != null;) {
					result.append(line).append("\n");
				}

				System.out.println("==========> Redirecting user to  = " + 
						postResponse.getFirstHeader("Location"));
				
				// Step 2: now we need to redirect to location returned by SF
				// authorize endpoint in HTTP response.
				response.sendRedirect(postResponse.getFirstHeader("Location").getValue());
	 */
}
