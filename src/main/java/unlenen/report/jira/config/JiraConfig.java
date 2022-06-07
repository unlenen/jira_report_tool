package unlenen.report.jira.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Nebi
 */
@Configuration
@Getter
public class JiraConfig {

    @Value("${jira.url}")
    String url;

    @Value("${jira.filter.jql}")
    String issueFilter;

    @Value("${jira.filter.fields}")
    String[] issueFields;

    @Value("${jira.filter.max}")
    int issueFilterMax;

}
