package unlenen.report.jira.entity;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Nebi
 */
@Getter
@Setter
public class ReportEntity {

    int week;
    String userName;
    String userDisplayname;
    String issueKey;
    String issueName;
    String wreq;
    float spentTime;
    boolean wreqType;

}
