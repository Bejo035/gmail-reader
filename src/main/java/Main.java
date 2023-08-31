import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args){

        GmailApi gmailApi = new GmailApi("898543226384-5qtd3kngjii2706l2mq2a846i00kk53c.apps.googleusercontent.com",
                "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",
                "1//09r0-YKCjMuzyCgYIARAAGAkSNwF-L9IrSVjHp1IQkYf6LAbAmNLOpBMFPcvxUV6ySsfFQlkXNaAKwOmUp-JpKWOvLh56zJE5WLU");
        try {
            List<String> urls = gmailApi.getMailBody("188d9147be8a0252");
            urls.forEach(System.out::println);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
