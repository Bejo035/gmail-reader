import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class EmailReader2 {
    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public static void main(String[] args) {
        // Your API key
        String apiKey = "ya29.a0AWY7CknT2QdIU2rnkApx586Q10rVoG_-0BLi-8lY72aNE3CKd6EB24bmnDj58Z_4ZAUf0CE05yyoo8t8ZJzihmRGN1Tj_4VXa4ATs8Hj2E6CGmH4kTlJ9MeDQsH9jAfdWnWkvz1dJkN9CClDlYajwxcezzcgM2gjaCgYKASoSARESFQG1tDrpZf-UzpBk_VHc4eyGBaaFyg0167";

        try {
            // Build and authorize the Gmail service
            Gmail service = buildGmailService(apiKey);

            // Retrieve the list of messages in the inbox
            ListMessagesResponse response = service.users().messages().list("me").execute();
            System.out.println("response = " + response);
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

    private static Gmail buildGmailService(String apiKey) throws IOException, GeneralSecurityException {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        HttpRequestInitializer requestInitializer = request -> {
            request.getHeaders().set("X-Goog-Api-Key", apiKey);
            request.setThrowExceptionOnExecuteError(false);
        };
        return new Gmail.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
