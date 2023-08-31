import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

public class GmailCSVReader {

    private static final String APPLICATION_NAME = "Gmail CSV Reader";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_READONLY);

    public static void main(String[] args) {
        String credentialsPath = "path/to/credentials.json";
        String tokenPath = "path/to/token.json";

        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getCredentials(httpTransport, credentialsPath, tokenPath);
            Gmail gmail = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            String user = "me";
            List<Message> messages = listMessagesWithAttachments(gmail, user);
            System.out.println("Total Messages: " + messages.size());

            for (Message message : messages) {
                String messageId = message.getId();
                Message fullMessage = gmail.users().messages().get(user, messageId).execute();
                String subject = getHeader(fullMessage, "Subject");

                List<MessagePart> parts = fullMessage.getPayload().getParts();
                if (parts != null) {
                    for (MessagePart part : parts) {
                        String filename = part.getFilename();
                        String attachmentId = part.getBody().getAttachmentId();
                        if (filename != null && attachmentId != null) {
                            byte[] attachmentBytes = getAttachmentBytes(gmail, user, messageId, attachmentId);
                            processCSVFile(attachmentBytes);
                            System.out.println("CSV data processed for attachment: " + filename);
                        }
                    }
                }
            }
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    private static Credential getCredentials(HttpTransport httpTransport, String credentialsPath, String tokenPath)
            throws IOException {
        GoogleCredential credential = GoogleCredential.fromStream(new java.io.FileInputStream(credentialsPath))
                .createScoped(SCOPES);
        credential.setAccessToken(tokenPath);
        return credential;
    }

    private static List<Message> listMessagesWithAttachments(Gmail gmail, String user) throws IOException {
        String query = "has:attachment";
        return gmail.users().messages().list(user).setQ(query).execute().getMessages();
    }

    private static String getHeader(Message message, String headerName) {
        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        if (headers != null) {
            for (MessagePartHeader header : headers) {
                if (header.getName().equals(headerName)) {
                    return header.getValue();
                }
            }
        }
        return "";
    }

    private static byte[] getAttachmentBytes(Gmail gmail, String user, String messageId, String attachmentId)
            throws IOException {
        MessagePartBody attachmentPart = gmail.users().messages().attachments()
                .get(user, messageId, attachmentId).execute();
        return com.google.api.client.util.Base64.decodeBase64(attachmentPart.getData());
    }

    private static void processCSVFile(byte[] attachmentBytes) throws IOException {
        // Read CSV data from byte array
        InputStreamReader inputStreamReader = new InputStreamReader(new ByteArrayInputStream(attachmentBytes));
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
            // Process each line of the CSV file
            String[] data = line.split(",");
            // Do something with the data
            System.out.println(data);
        }

        bufferedReader.close();
        inputStreamReader.close();
    }
}
