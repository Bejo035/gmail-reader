import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class EmailReader3 {
    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);

    public static void main(String[] args) {
        try {
            // Build and authorize the Gmail service
            Gmail service = buildGmailService();

            // Retrieve the list of messages in the inbox
            ListMessagesResponse response = service.users().messages().list("me").execute();
            for (Message message : response.getMessages()) {
                // Get the full message details
                Message fullMessage = service.users().messages().get("me", message.getId()).execute();

                // Print message details
                System.out.println("Subject: " + fullMessage.getPayload().getHeaders().stream()
                        .filter(header -> header.getName().equalsIgnoreCase("Subject"))
                        .findFirst()
                        .orElse(null)
                        .getValue());

                System.out.println("From: " + fullMessage.getPayload().getHeaders().stream()
                        .filter(header -> header.getName().equalsIgnoreCase("From"))
                        .findFirst()
                        .orElse(null)
                        .getValue());

                System.out.println("Snippet: " + fullMessage.getSnippet());
                System.out.println("-----------------------------");
            }
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private static Gmail buildGmailService() throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(httpTransport);
        return new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static Credential authorize(NetHttpTransport httpTransport) throws IOException, GeneralSecurityException {
        File credentialsFile = new File("src/main/resources/credentials.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(new FileInputStream(credentialsFile)));
        System.out.println("clientSecrets = " + clientSecrets);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .build();


        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }
}
