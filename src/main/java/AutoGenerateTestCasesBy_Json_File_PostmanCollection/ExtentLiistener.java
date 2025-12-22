package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;


import java.util.HashMap;
import java.util.Map;

public class ExtentLiistener implements ITestListener {

    private static ExtentReports extent;
    private static Map<String, ExtentTest> classLevelTests = new HashMap<>();
    private static ThreadLocal<ExtentTest> methodLevelTest = new ThreadLocal<>();

    public static ExtentTest getTest() {
        return methodLevelTest.get();
    }

    @Override
    public void onStart(ITestContext context) {
        // 🔥 reset everything per execution
        ExtentmanagerSs.flushAndReset();
        extent = ExtentmanagerSs.createNewInstance();
        classLevelTests.clear();
    }

    @Override
    public void onTestStart(ITestResult result) {

        String className =
                result.getTestClass().getRealClass().getSimpleName();
        String methodName =
                result.getMethod().getMethodName();

        ExtentTest classTest =
                classLevelTests.computeIfAbsent(
                        className,
                        k -> extent.createTest(
                                "<span style='color:blue'>" + k + "</span>")
                );

        ExtentTest methodTest = classTest.createNode(methodName);
        methodLevelTest.set(methodTest);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        getTest().pass("Test Passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        getTest().fail(result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        getTest().skip(result.getThrowable());
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentmanagerSs.flushAndReset();
        methodLevelTest.remove();
    }
}
