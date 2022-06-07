package unlenen.report.jira.controller;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unlenen.report.jira.entity.ReportEntity;
import unlenen.report.jira.service.ReportService;

/**
 *
 * @author Nebi
 */
@RestController
@RequestMapping("/jira/v1")
public class ReportController {

    @Autowired
    ReportService reportService;

    @GetMapping("/report/json/{user}/{password}/{requestedUser}/{week}")
    public ResponseEntity<List<ReportEntity>> getReportJSON(@PathVariable String user, @PathVariable String password, @PathVariable String requestedUser, @PathVariable int week) {
        try {
            return new ResponseEntity<>(reportService.getWeekReport(user, password, requestedUser, week), HttpStatus.OK);
        } catch (URISyntaxException ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/report/excel/{user}/{password}/{requestedUser}/{week}")
    public ResponseEntity<Resource> getReportExcel(@PathVariable String user, @PathVariable String password, @PathVariable String requestedUser, @PathVariable int week) {
        try {

            List<ReportEntity> reportEntities = reportService.getWeekReport(user, password, requestedUser, week);
            String fileExcelPath = reportService.writeToExcel(reportEntities);
            File fileExcel = new File(fileExcelPath);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            headers.add("Content-Disposition", "attachment; filename=Report_" + requestedUser + "_week_" + week + ".xlsx");

            InputStreamResource resource = new InputStreamResource(new FileInputStream(fileExcel));
            return ResponseEntity.ok().headers(headers).contentLength(fileExcel.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }
    }
}
