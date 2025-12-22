package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import com.aventstack.extentreports.ExtentTest;


public class ReportloggerforColorr {
	
	public static void logPass(ExtentTest test, String message) {
        test.pass("<span style='color:green;font-weight:bold;'>" + message + "</span>");
    }

    public static void logFail(ExtentTest test, String message, Exception e) {
        test.fail("<span style='color:red;font-weight:bold;'>" + message + "</span> " + e.getMessage());
    }

    public static void logFail(ExtentTest test, String message) {
        test.fail("<span style='color:red;font-weight:bold;'>" + message + "</span>");
    }

    public static void logInfo(ExtentTest test, String message) {
        test.info("<span style='color:yellow;font-weight:bold;'>" + message + "</span>");
    }

    public static void logWarning(ExtentTest test, String message) {
        test.warning("<span style='color:orange;font-weight:bold;'>" + message + "</span>");
    }
    
    
    public static void logFail2(ExtentTest test, String message, AssertionError ae) {
        test.fail("<span style='color:red;font-weight:bold;'>" + message + "</span> " + ae.getMessage());
    }

}


