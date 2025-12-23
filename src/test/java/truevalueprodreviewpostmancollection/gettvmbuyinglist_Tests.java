package truevalueprodreviewpostmancollection;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.Listeners;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.http.ContentType;
import static io.restassured.RestAssured.given;
import com.aventstack.extentreports.ExtentTest;
import AutoGenerateTestCasesBy_Json_File_PostmanCollection.*;

@Listeners(ExtentLiistener.class)
public class gettvmbuyinglist_Tests extends StatusCodeClass  {

    private Response response;
    private ExtentTest test;

    @Test(priority=1)
    public void getTvmBuyingList_tvmCustomerDStatus_approve_byPass_true() {
        test = ExtentLiistener.getTest();
        RestAssured.baseURI = BASE_URI;

        String Payload = "{\"sortBy\":\"buyingDate\",\"pageNumber\":0,\"pageSize\":10,\"sortDirection\":\"desc\",\"tvmCustomerDStatus\":\"approve\",\"byPass\":true,\"filters\":[{\"key\":\"buyingDate\",\"value\":\"buyingDate\",\"from\":\"2025-09-11T23:59:00.000Z\",\"to\":\"2025-12-11T23:59:00.000Z\"},{\"key\":\"zone\",\"value\":\"CENTRAL\",\"from\":\"\",\"to\":\"\"}],\"searchText\":\"\"}";
        StatusCodeClass.logPayload(test, Payload);

        try {
            response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", TokenManager.getToken())
                    .header("apikey", "mra7menthnqeo950cs6gsh4hl")
                    .body(Payload)
                    .when().post("https://s1sgfdgek6.execute-api.ap-south-1.amazonaws.com/prod/api/buying/getTvmBuyingList?tvmCustomerDStatus=approve&byPass=true")
                    .then().extract().response();

            StatusCodeClass.validateStatusCode(response, test);
            StatusCodeClass.validateAuth(response, test);
            StatusCodeClass.validateResponseTime(response, test);
        } catch (Exception e) {
            Assert.fail("API Failed: getTvmBuyingList_tvmCustomerDStatus_approve_byPass_true");
        }
    }

    @Test(priority=2)
    public void getTvmBuyingList_tvmCustomerDStatus_reject_byPass_true() {
        test = ExtentLiistener.getTest();
        RestAssured.baseURI = BASE_URI;

        String Payload = "{\"sortBy\":\"buyingDate\",\"pageNumber\":0,\"pageSize\":10,\"sortDirection\":\"desc\",\"tvmCustomerDStatus\":\"reject\",\"byPass\":true,\"filters\":[{\"key\":\"buyingDate\",\"value\":\"buyingDate\",\"from\":\"2025-09-11T23:59:00.000Z\",\"to\":\"2025-12-11T23:59:00.000Z\"},{\"key\":\"zone\",\"value\":\"CENTRAL\",\"from\":\"\",\"to\":\"\"}],\"searchText\":\"\"}";
        StatusCodeClass.logPayload(test, Payload);

        try {
            response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", TokenManager.getToken())
                    .header("apikey", "mra7menthnqeo950cs6gsh4hl")
                    .body(Payload)
                    .when().post("https://s1sgfdgek6.execute-api.ap-south-1.amazonaws.com/prod/api/buying/getTvmBuyingList?tvmCustomerDStatus=reject&byPass=true")
                    .then().extract().response();

            StatusCodeClass.validateStatusCode(response, test);
            StatusCodeClass.validateAuth(response, test);
            StatusCodeClass.validateResponseTime(response, test);
        } catch (Exception e) {
            Assert.fail("API Failed: getTvmBuyingList_tvmCustomerDStatus_reject_byPass_true");
        }
    }

    @Test(priority=3)
    public void getTvmBuyingList_tvmCustomerDStatus_pending_byPass_true() {
        test = ExtentLiistener.getTest();
        RestAssured.baseURI = BASE_URI;

        String Payload = "{\"sortBy\":\"buyingDate\",\"pageNumber\":0,\"pageSize\":10,\"sortDirection\":\"desc\",\"tvmCustomerDStatus\":\"pending\",\"byPass\":true,\"filters\":[{\"key\":\"buyingDate\",\"value\":\"buyingDate\",\"from\":\"2025-09-11T23:59:00.000Z\",\"to\":\"2025-12-11T23:59:00.000Z\"},{\"key\":\"zone\",\"value\":\"CENTRAL\",\"from\":\"\",\"to\":\"\"}],\"searchText\":\"\"}";
        StatusCodeClass.logPayload(test, Payload);

        try {
            response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", TokenManager.getToken())
                    .header("apikey", "mra7menthnqeo950cs6gsh4hl")
                    .body(Payload)
                    .when().post("https://s1sgfdgek6.execute-api.ap-south-1.amazonaws.com/prod/api/buying/getTvmBuyingList?tvmCustomerDStatus=pending&byPass=true")
                    .then().extract().response();

            StatusCodeClass.validateStatusCode(response, test);
            StatusCodeClass.validateAuth(response, test);
            StatusCodeClass.validateResponseTime(response, test);
        } catch (Exception e) {
            Assert.fail("API Failed: getTvmBuyingList_tvmCustomerDStatus_pending_byPass_true");
        }
    }

}