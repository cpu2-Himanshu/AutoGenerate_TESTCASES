package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.io.File;

public class ExtentmanagerSs {

    private static ExtentReports extent;

    public static final String REPORT_PATH =
            "target/test-output/AutoGenerateTestCasesBy-Json-File-API-Report.html";

    // 🔥 ALWAYS create new instance
    public static synchronized ExtentReports createNewInstance() {

        File reportFile = new File(REPORT_PATH);
        reportFile.getParentFile().mkdirs();

        ExtentHtmlReporter htmlReporter =
                new ExtentHtmlReporter(reportFile);

        htmlReporter.config().setDocumentTitle("API Automation Report");
        htmlReporter.config().setReportName("Postman → TestNG Report");
        htmlReporter.config().setTheme(Theme.DARK);

        extent = new ExtentReports();
        extent.attachReporter(htmlReporter);

        return extent;
    }

    public static synchronized ExtentReports getExtentReports() {
        if (extent == null) {
            createNewInstance();
        }
        return extent;
    }

    // 🔥 THIS IS THE KEY FIX
    public static synchronized void flushAndReset() {
        if (extent != null) {
            extent.flush();
            extent = null;   // 🔥 destroy memory
        }
    }
}
