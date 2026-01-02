package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;


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
            String url = "http://localhost:8097/postman-generator-ui.html";
            new ProcessBuilder("cmd", "/c", "start", url).start();
        } catch (Exception e) {
            System.out.println("Open manually: http://localhost:8097/postman-generator-ui.html");
        }
    }
    
//	@Value("${server.port}")
//	private int serverPort;
//
//	@EventListener(ApplicationReadyEvent.class)
//	public void openBrowser() {
//		// If you always use public IP:
//		String host = "15.206.213.218"; // or move this to application.properties
//		System.out.println("Open manually: http://" + host + ":" + serverPort + "/postman-generator-ui.html");
//	}
}
