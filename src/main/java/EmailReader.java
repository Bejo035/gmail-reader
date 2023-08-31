import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import java.util.Properties;

public class EmailReader {
    public static void main(String[] args) {
        // Gmail account credentials
        String username = "mchedlishvilisandro02@gmail.com";

        // OAuth2 credentials
        String clientId = "898543226384-5qtd3kngjii2706l2mq2a846i00kk53c.apps.googleusercontent.com";
        String clientSecret = "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";
        String refreshToken = "1//09r0-YKCjMuzyCgYIARAAGAkSNwF-L9IrSVjHp1IQkYf6LAbAmNLOpBMFPcvxUV6ySsfFQlkXNaAKwOmUp-JpKWOvLh56zJE5WLU";

        // IMAP server settings
        String host = "imap.gmail.com";
        int port = 993;

        try {
            // Set the properties for the mail session
            Properties props = new Properties();
            props.put("mail.imap.host", host);
            props.put("mail.imap.port", port);
            props.put("mail.imap.ssl.enable", "true");
            // Create the mail session with OAuth2 authentication
            Session session = Session.getInstance(props);
            Store store = session.getStore("imap");

            // Enable OAuth2 for the session
            String accessToken = getAccessToken(clientId, clientSecret, refreshToken);
//            System.out.println("accessToken = " + accessToken);
            store.connect(host, port, username, accessToken);
            // Open the INBOX folder
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Retrieve the messages
            Message[] messages = inbox.getMessages();

            // Print information about each message
            for (Message message : messages) {
                System.out.println("Subject: " + message.getSubject());
                System.out.println("From: " + message.getFrom()[0]);
                System.out.println("Text: " + message.getContent().toString());
                System.out.println("-----------------------------");
            }

            // Close the connection
            inbox.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getAccessToken(String clientId, String clientSecret, String refreshToken) throws Exception {
        String tokenUrl = "https://accounts.google.com/o/oauth2/token";
        String grantType = "refresh_token";

        // Construct the POST request to exchange the refresh token for an access token
        String requestBody = createRequestBody(clientId, clientSecret, refreshToken, grantType);
        System.out.println("requestBody = " + requestBody);
        // Send the POST request and parse the response to extract the access token
        String response = sendPostRequest(tokenUrl, requestBody);
        String accessToken = extractAccessToken(response);
        System.out.println("accessToken = " + accessToken);
        return accessToken;
    }

    private static String createRequestBody(String clientId, String clientSecret, String refreshToken, String grantType) {
        String requestBody = "{\"client_id\":\"" + clientId + "\",\"client_secret\":\"" + clientSecret +
                "\",\"refresh_token\":\"" + refreshToken + "\",\"grant_type\":\"" + grantType +"\"}";
        return requestBody;
    }

    private static String sendPostRequest(String url, String requestBody) throws Exception {
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post(url)
                .header("Content-Type", "application/json")
                .body(requestBody)
                .asString();
        return response.getBody(); // Return the response body as a string
    }

    private static String extractAccessToken(String response) {
        int startOfAccess_token = response.lastIndexOf("\"access_token\"") + 17;
        String subString = response.substring(startOfAccess_token);
        int endOfAccess_token = subString.indexOf("\"");
        return subString.substring(0,endOfAccess_token); // Return the extracted access token
    }
}
