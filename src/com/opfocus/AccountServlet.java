package com.opfocus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@WebServlet(urlPatterns = { "/AccountHandler" })
public class AccountServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	private static final String INSTANCE_URL = "INSTANCE_URL";

   	private void showAccounts(String contextPath, String instanceUrl, String accessToken,
		PrintWriter writer) throws ServletException, IOException {

		HttpClient httpClient =  HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(instanceUrl + "/services/data/v20.0/query" + 
				"?q=" + 
				URLEncoder.encode("SELECT Name, Id from Account LIMIT 100", "UTF-8"));
		// set the token in the header
		get.setHeader("Authorization", "OAuth " + accessToken);
		try {
			System.out.println("Executing get method.");
			HttpResponse postResponse = httpClient.execute(get);
			System.out.println("==========> Response Code : " 
		            + postResponse.getStatusLine().getStatusCode());

			if (postResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				System.out.println("response was a success.");
				// Now lets use the standard java json classes to work with the
				// results
				try {
					BufferedReader rd = new BufferedReader(
					        new InputStreamReader(postResponse.getEntity().getContent()));
					StringBuilder result = new StringBuilder();
					for (String line = null; (line=rd.readLine()) != null;) {
						result.append(line).append("\n");
					}

					System.out.println("==========> Parsed result =  "
							+ result.toString());
					JSONObject response = new JSONObject(result.toString());

					System.out.println("Query response: "
							+ response.toString(2));

					System.out.println("TotalSize = " + 
							response.getLong("totalSize"));
					
					writer.write("<div>There are " + response.getLong("totalSize")
							+ " record(s) returned</div><p/>");

					JSONArray results = response.getJSONArray("records");
					writer.write("<table>");
					for (int i = 0; i < results.length(); i++) {
						writer.write("<tr><td><a href=\"" + contextPath + 
								"/AccountHandler?accountId=" 
								+ results.getJSONObject(i).getString("Id")
								+ "\">" +  results.getJSONObject(i).getString("Name") +
								"</a></td></tr>");
					}
					writer.write("</table>");
				} catch (JSONException e) {
					e.printStackTrace();
					throw new ServletException(e);
				}
			} else {
				System.out.println("Response was NOT 200.");
			}
		} finally {
			get.releaseConnection();
		}
	}

	private void showAccount(String context, String accountId, String instanceUrl,
			String accessToken, PrintWriter writer) throws ServletException,
			IOException {
		HttpClient httpClient =  HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(instanceUrl + "/services/data/v20.0/sobjects/Account/" + 
				accountId);
		// set the token in the header
		System.out.println("get endpoint = " + instanceUrl + "/services/data/v20.0/sobjects/Account/" + 
				accountId);
		get.setHeader("Authorization", "OAuth " + accessToken);

		try {
			HttpResponse postResponse = httpClient.execute(get);
			System.out.println("==========> Response Code : " 
		            + postResponse.getStatusLine().getStatusCode());

			if (postResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				try {
					// Now lets use the standard java json classes to work with the
					// results
					BufferedReader rd = new BufferedReader(
					        new InputStreamReader(postResponse.getEntity().getContent()));
					StringBuilder result = new StringBuilder();
					for (String line = null; (line=rd.readLine()) != null;) {
						result.append(line).append("\n");
					}

					System.out.println("==========> Parsed result =  "
							+ result.toString());
					JSONObject response = new JSONObject(result.toString());

					writer.write("<div>Account content</div><p/>");

					Iterator<String> iterator = response.keys();
					writer.write("<table>");
					while (iterator.hasNext()) {
						String key = (String) iterator.next();
						if (key.equals("Site")) {
							continue;
						}
					
						String value = response.get(key).toString();	
						writer.write("<tr><td>" + key + "</td><td>" + 
								(value != null ? value : "")
								+ "</td></tr>");
					}
					writer.write("</table>");
					writer.write("<p/><div><a href=\"" + context + "/AccountHandler\">" + 
							"Return to Account List</a></div>");
				} catch (JSONException e) {
					e.printStackTrace();
					throw new ServletException(e);
				}
			}
		} finally {
			get.releaseConnection();
		}
	}
	
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");
		PrintWriter writer = response.getWriter();

		String accessToken = (String) request.getSession().getAttribute(
				ACCESS_TOKEN);

		String instanceUrl = (String) request.getSession().getAttribute(
				INSTANCE_URL);

		if (accessToken == null) {
			writer.write("Error - no access token");
			return;
		}

		System.out.println("accountId = " + request.getParameter("accountId"));
		if (request.getParameter("accountId") != null) {
			showAccount(request.getContextPath(),
					request.getParameter("accountId"), instanceUrl, accessToken, writer);
		} else {
	
			System.out.println("==========> We have an access token: " + accessToken + "\n"
					+ "Using instance " + instanceUrl + "\n\n");
			writer.write("<div>We have an access token: " + accessToken + "</div>" +
				 "<div>Using instance " + instanceUrl + "</div><p/>");
	
			showAccounts(request.getContextPath(), instanceUrl, accessToken, writer);
		}
	}
}
