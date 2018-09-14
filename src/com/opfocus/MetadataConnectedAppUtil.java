package com.opfocus;


import java.util.ArrayList;

import com.sforce.soap.metadata.ConnectedApp;
import com.sforce.soap.metadata.ConnectedAppOauthAccessScope;
import com.sforce.soap.metadata.ConnectedAppOauthConfig;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.SaveResult;
import com.sforce.ws.ConnectionException;

public class MetadataConnectedAppUtil {

	public static void create(MetadataConnection connection) throws ConnectionException {
		ConnectedApp connectedApp = new ConnectedApp();
		connectedApp.setFullName("OAuth Test App");
		connectedApp.setContactEmail("bsullivan@opfocus.com");
		connectedApp.setDescription("Connected App to Test OAuth");
		connectedApp.setLabel("OAuth Test App");
		
		String consumerKey = "";
		String consumerSecret = "";
		
		connectedApp.setOauthConfig(new ConnectedAppOauthConfig());
		connectedApp.getOauthConfig().setCallbackUrl(
				"https://98.249.49.220:8443/oAuthPoc");
		connectedApp.getOauthConfig().setScopes(new ConnectedAppOauthAccessScope[]{
			ConnectedAppOauthAccessScope.Api,
			ConnectedAppOauthAccessScope.RefreshToken,
			ConnectedAppOauthAccessScope.OfflineAccess});
		connectedApp.getOauthConfig().setConsumerKey(consumerKey);
		connectedApp.getOauthConfig().setConsumerSecret(consumerSecret);

        SaveResult[] results = connection.createMetadata(new Metadata[]{connectedApp});
	}
}
