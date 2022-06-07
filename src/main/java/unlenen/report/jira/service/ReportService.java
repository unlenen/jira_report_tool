package unlenen.report.jira.service;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.api.domain.Worklog;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTimeFieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unlenen.report.jira.config.JiraConfig;
import unlenen.report.jira.entity.ReportEntity;

/**
 *
 * @author Nebi
 */
@Service
public class ReportService {

    @Autowired
    JiraConfig artConfig;

    JiraRestClient restClient;

    public List<ReportEntity> getWeekReport(String user, String password, String requestedUser, int requestedWeek) throws URISyntaxException {
        List<ReportEntity> result = new ArrayList();
        login(user, password);
        int weekDiff = getCurrentWeekNumber() - requestedWeek;

        HashSet<String> fields = Sets.newHashSet("*all");
        String filter = artConfig.getIssueFilter().replaceAll("\\$WEEKB", weekDiff + "").replaceAll("\\$WEEKE", (weekDiff - 1) + "").replaceAll("\\$USER", requestedUser + "");
        System.out.println(filter);
        SearchResult searchResult = (SearchResult) (restClient.getSearchClient().searchJql(filter, artConfig.getIssueFilterMax(), 0, fields).claim());

        asStream(searchResult.getIssues().iterator(), false).forEach(issue -> {
            String userDisplayName = "";

            List<Worklog> worklogs = new ArrayList();
            asStream(issue.getWorklogs().iterator(), false)
                    .filter(worklog -> worklog.getAuthor().getName().equals(requestedUser) && worklog.getStartDate().get(DateTimeFieldType.weekOfWeekyear()) == (getCurrentWeekNumber() - weekDiff))
                    .forEach(worklog -> {
                        worklogs.add(worklog);
                        System.out.println(issue.getKey() + " ---> time:  " + worklog.getStartDate() + " spend : " + (worklog.getMinutesSpent() / 60) + " week : " + worklog.getStartDate().get(DateTimeFieldType.weekOfWeekyear()));
                    });

            float totalMin = worklogs.stream()
                    .reduce(0f, (partialResult, t) -> partialResult + (float) t.getMinutesSpent(), Float::sum);

            Worklog firstWorklog = !worklogs.isEmpty() ? worklogs.get(0) : null;

            if (firstWorklog != null) {
                userDisplayName = firstWorklog.getAuthor().getDisplayName();
            }
            if (totalMin != 0) {
                result.add(toReportEntity(requestedUser, userDisplayName, issue, totalMin, requestedWeek));
            }
        });
        return result;
    }

    public String writeToExcel(List<ReportEntity> reportEntities) throws Exception {
        File tmpFile = File.createTempFile("reportTool", ".xlsx");
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Rapor");
        int rowId = 0;
        String displayName = "";
        for (ReportEntity reportEntity : reportEntities) {
            if ("".equals(displayName) && !"".equals(reportEntity.getUserDisplayname())) {
                displayName = reportEntity.getUserDisplayname();
            }
        }
        Integer headerCellCount = 0;
        Row header = sheet.createRow(rowId++);
        createExcelCell(header, headerCellCount, "Hafta");
        headerCellCount++;
        createExcelCell(header, headerCellCount, "Çalışan");
        headerCellCount++;
        createExcelCell(header, headerCellCount, "Yapılacak İş");
        headerCellCount++;
        createExcelCell(header, headerCellCount, "ART");
        headerCellCount++;
        createExcelCell(header, headerCellCount, "WREQ");
        headerCellCount++;
        createExcelCell(header, headerCellCount, "İşin Kategorisi");
        headerCellCount++;
        createExcelCell(header, headerCellCount, "Efor");
        headerCellCount++;
        for (ReportEntity reportEntity : reportEntities) {
            Row row = sheet.createRow(rowId++);
            Integer cellCount = 0;
            createExcelCell(row, cellCount, "Hafta " + reportEntity.getWeek());
            cellCount++;
            createExcelCell(row, cellCount, displayName);
            cellCount++;
            createExcelCell(row, cellCount, reportEntity.getIssueName());
            cellCount++;
            String issueKey = reportEntity.getIssueKey();
            if (reportEntity.isWreqType()) {
                issueKey = "";
            }
            createExcelCell(row, cellCount, issueKey);
            cellCount++;
            createExcelCell(row, cellCount, reportEntity.getWreq());
            cellCount++;
            createExcelCell(row, cellCount, "");
            cellCount++;
            createExcelCell(row, cellCount, (reportEntity.getSpentTime() / 60));
            cellCount++;

        }
        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }
        workbook.write(new FileOutputStream(tmpFile));
        workbook.close();
        return tmpFile.getPath();
    }

    private void createExcelCell(Row row, int cellCount, String text) {
        Cell week = row.createCell(cellCount);
        week.setCellValue(text);
    }

    private void createExcelCell(Row row, int cellCount, Float value) {
        Cell week = row.createCell(cellCount);
        week.setCellValue(value);
    }

    private void login(String user, String password) throws URISyntaxException {
        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        this.restClient = factory.createWithBasicHttpAuthentication(new URI(artConfig.getUrl()), user, password);
    }

    private int getCurrentWeekNumber() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.WEEK_OF_YEAR) - 1;
    }

    static <T> Stream<T> asStream(Iterator<T> sourceIterator, boolean parallel) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), parallel);
    }

    private ReportEntity toReportEntity(String username, String userDisplayName, Issue issue, float totalMin, int requestedWeek) {
        boolean isWreq = issue.getKey().startsWith("WREQ-") || issue.getKey().startsWith("INIT-");
        String issueKey = isWreq ? "-" : issue.getKey();
        String issueWreq = isWreq ? issue.getKey() : issue.getFieldByName("WREQ INIT REF").getValue() + "";
        ReportEntity reportEntity = new ReportEntity();
        reportEntity.setWeek(requestedWeek);
        reportEntity.setIssueName(issue.getSummary());
        reportEntity.setIssueKey(issueKey);
        reportEntity.setSpentTime(totalMin);
        reportEntity.setUserName(username);
        reportEntity.setUserDisplayname(userDisplayName);
        reportEntity.setWreq(issueWreq);
        reportEntity.setWreqType(isWreq);
        return reportEntity;
    }
}
