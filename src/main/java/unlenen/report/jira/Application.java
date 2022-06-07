package unlenen.report.jira;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.event.EventListener;
import unlenen.report.jira.service.ReportService;

/**
 *
 * @author Nebi
 */
@SpringBootApplication
public class Application extends SpringBootServletInitializer {

    @Autowired
    ReportService reportService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onAppRun() {
        System.out.println("App is loaded");
    }

}
