package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import com.aventstack.extentreports.*;
import org.testng.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ExtentLiistener implements ITestListener {

    private static ExtentReports extent;
    private static Map<String, ExtentTest> classLevelTests = new HashMap<>();
    private static ThreadLocal<ExtentTest> methodLevelTest = new ThreadLocal<>();

    // ===============================
    // EXECUTION COUNTERS
    // ===============================
    public static AtomicInteger PASSED  = new AtomicInteger(0);
    public static AtomicInteger FAILED  = new AtomicInteger(0);
    public static AtomicInteger SKIPPED = new AtomicInteger(0);
    public static AtomicInteger TOTAL   = new AtomicInteger(0);

    public static void resetCounters() {
        PASSED.set(0);
        FAILED.set(0);
        SKIPPED.set(0);
        TOTAL.set(0);
    }

    // =====================================================
    // 🔥 SAFE ACCESS FOR StatusCodeClass
    // =====================================================
    public static ExtentTest getTest() {

        ExtentTest test = methodLevelTest.get();

        if (test == null) {
            // fallback node (pre-test / runtime safety)
            if (extent == null) {
                extent = ExtentmanagerSs.createNewInstance();
            }
            test = extent.createTest("Runtime API Execution");
            methodLevelTest.set(test);
        }
        return test;
    }

    // ===============================
    // SUITE START
    // ===============================
    @Override
    public void onStart(ITestContext context) {

        resetCounters();
        ExtentmanagerSs.flushAndReset();
        extent = ExtentmanagerSs.createNewInstance();

        classLevelTests.clear();
        methodLevelTest.remove();

        System.out.println("===== TEST EXECUTION STARTED =====");
    }

    // ===============================
    // TEST START
    // ===============================
    @Override
    public void onTestStart(ITestResult result) {

        TOTAL.incrementAndGet();

        String className =
                result.getTestClass().getRealClass().getSimpleName();
        String methodName =
                result.getMethod().getMethodName();

        ExtentTest classTest =
                classLevelTests.computeIfAbsent(
                        className,
                        k -> extent.createTest(
                                "<span style='color:#60a5fa'>" + k + "</span>")
                );

        ExtentTest methodTest = classTest.createNode(methodName);
        methodLevelTest.set(methodTest);
    }

    // ===============================
    // TEST PASSED
    // ===============================
    @Override
    public void onTestSuccess(ITestResult result) {
        PASSED.incrementAndGet();
        getTest().pass("✅ Test Passed");
    }

    // ===============================
    // TEST FAILED
    // ===============================
    @Override
    public void onTestFailure(ITestResult result) {
        FAILED.incrementAndGet();
        getTest().fail(result.getThrowable());
    }

    // ===============================
    // TEST SKIPPED
    // ===============================
    @Override
    public void onTestSkipped(ITestResult result) {
        SKIPPED.incrementAndGet();
        getTest().skip(result.getThrowable());
    }

    // ===============================
    // SUITE FINISH
    // ===============================
    @Override
    public void onFinish(ITestContext context) {

        System.out.println("===== TEST EXECUTION FINISHED =====");
        System.out.println("PASSED  : " + PASSED.get());
        System.out.println("FAILED  : " + FAILED.get());
        System.out.println("SKIPPED : " + SKIPPED.get());
        System.out.println("TOTAL   : " + TOTAL.get());

        methodLevelTest.remove();
    }

    // =====================================================
    // 🔥 SUMMARY FOR UI
    // =====================================================
    public static Map<String, Integer> getSummary() {
        return Map.of(
                "passed", PASSED.get(),
                "failed", FAILED.get(),
                "skipped", SKIPPED.get(),
                "total", TOTAL.get()
        );
    }
}
