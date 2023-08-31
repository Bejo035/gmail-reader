import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.mashape.unirest.http.Unirest;
import org.json.JSONObject;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class racxa {
    private static final String APPLICATION_NAME = "Gmail API Java Quickstart";
    private static final String REFRESH_TOKEN = "1//09r0-YKCjMuzyCgYIARAAGAkSNwF-L9IrSVjHp1IQkYf6LAbAmNLOpBMFPcvxUV6ySsfFQlkXNaAKwOmUp-JpKWOvLh56zJE5WLU";
    private static final String GRANT_TYPE = "refresh_token";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String USER = "me";
    private static final File filePath = new File("src/main/resources/credentials.json");
    static Gmail gmailService = null;

    public static void main(String[] args) throws IOException, GeneralSecurityException {
        getGmailService();

        Message message = getMailBody("from: " + "trend");

        JSONObject jsonObject = new JSONObject(message);
        jsonObject = jsonObject.getJSONObject("payload");
        jsonObject = jsonObject.getJSONArray("parts").getJSONObject(1);
        jsonObject = jsonObject.getJSONObject("body");

        System.out.println("jsonObject = " + jsonObject.getString("attachmentId"));

    }

    private static void getGmailService() throws IOException, GeneralSecurityException {
        InputStream in = new FileInputStream(filePath);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        Credential authorize = new GoogleCredential.Builder().setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientSecrets.getDetails().getClientId(), clientSecrets.getDetails().getClientSecret())
                .build()
                .setAccessToken(getAccessToken(clientSecrets.getDetails().getClientId(), clientSecrets.getDetails().getClientSecret()))
                .setRefreshToken(REFRESH_TOKEN);


        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        gmailService = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, authorize)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static List<Message> getListSearchedMessages() throws IOException {
        List<Message> messageStubs = gmailService.users().messages().list(USER).setQ("from: " + "trend").execute().getMessages();
        return messageStubs == null ? new ArrayList<>() : messageStubs;
    }

    private static byte[] getAttachmentBytes(Gmail gmail, String messageId, String attachmentId)
            throws IOException {
        MessagePartBody attachmentPart = gmail.users().messages().attachments()
                .get(USER, messageId, attachmentId).execute();
        return com.google.api.client.util.Base64.decodeBase64(attachmentPart.getData());
    }

    private static void saveAttachment(byte[] attachmentBytes, String savePath) throws IOException {
        java.nio.file.Files.write(java.nio.file.Paths.get(savePath), attachmentBytes);
    }

    private static Message getMailBody(String searchString) throws IOException {
        Gmail.Users.Messages.List request = gmailService.users().messages().list(USER);
        List<Message> messageList = getListSearchedMessages();

        String attachmentSavePath = "src/main/resources/attachments/";

        for (Message msg : messageList) {
            String messageId = msg.getId();
            System.out.println("messageId = " + messageId);
            Message fullMessage = gmailService.users().messages().get(USER, messageId).execute();

            List<MessagePart> parts = fullMessage.getPayload().getParts();
            for (int i = 0; i < parts.size(); i++){
                System.out.printf("i(%d) : %s\n",i,parts.get(i).getFilename());
            }
            if (parts != null) {
                for (MessagePart part : parts) {
                    String filename = part.getFilename();
                    String attachmentId = part.getBody().getAttachmentId();
                    if (filename != null && attachmentId != null) {
                        byte[] attachmentBytes = getAttachmentBytes(gmailService, messageId, attachmentId);
                        processCSVFile(attachmentBytes);
                        saveAttachment(attachmentBytes, attachmentSavePath + filename);
                        System.out.println("Attachment saved: " + filename);
                    }
                }
            }
        }
        request = request.setQ(searchString);

        ListMessagesResponse messagesResponse = request.execute();
        request.setPageToken(messagesResponse.getNextPageToken());

        String messageId = messagesResponse.getMessages().get(0).getId();

        return gmailService.users().messages().get(USER, messageId).execute();
    }

    private static String getAccessToken(String clientId, String clientSecret) {
        String tokenUrl = "https://accounts.google.com/o/oauth2/token";

        // Construct the POST request to exchange the refresh token for access token
        String requestBody = createRequestBody(clientId, clientSecret);

        // Send the POST request and parse the response to extract the access token
        String response = null;
        try {
            response = sendPostRequest(tokenUrl, requestBody);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return extractAccessToken(response);
    }

    private static String createRequestBody(String clientId, String clientSecret) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("client_id", clientId);
        jsonObject.put("client_secret", clientSecret);
        jsonObject.put("refresh_token", REFRESH_TOKEN);
        jsonObject.put("grant_type", GRANT_TYPE);
        return jsonObject.toString();
    }

    private static String sendPostRequest(String url, String requestBody) throws Exception {
        return Unirest.post(url)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .asString().getBody(); // Return the response body as a string
    }

    private static String extractAccessToken(String response) {
        int startOfAccess_token = response.lastIndexOf("\"access_token\"") + 17;
        String subString = response.substring(startOfAccess_token);
        int endOfAccess_token = subString.indexOf("\"");
        return subString.substring(0, endOfAccess_token); // Return the extracted access token
    }

    private static void processCSVFile(byte[] attachmentBytes) throws IOException {
        // Read CSV data from byte array
        InputStreamReader inputStreamReader = new InputStreamReader(new ByteArrayInputStream(attachmentBytes));
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String line;
        int i = 0;
        while ((line = bufferedReader.readLine()) != null) {
//            System.out.printf("i = %s\n",line);
            // Process each line of the CSV file
            String[] data = line.split("\",\"");

//            for (int j = 0; j < data.length; j++) {
//                System.out.printf("j(%d): %-50s",j,data[j]);
//            }
//            System.out.println();
//            System.out.printf("i(%d) = %s\n",i,line);
            i++;
            // Do something with the data
//            System.out.println(data);
        }

        bufferedReader.close();
        inputStreamReader.close();
    }
}
