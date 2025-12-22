package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@SpringBootApplication(
    scanBasePackages = "AutoGenerateTestCasesBy_Json_File_PostmanCollection"
)
public class PostmanWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostmanWebApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        try {
            String url = "http://localhost:8085/postman-generator-ui.html";
            new ProcessBuilder("cmd", "/c", "start", url).start();
        } catch (Exception e) {
            System.out.println("Open manually: http://localhost:8085/postman-generator-ui.html");
        }
    }
}
