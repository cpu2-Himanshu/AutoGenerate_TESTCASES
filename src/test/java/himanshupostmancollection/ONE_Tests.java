package himanshupostmancollection;

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
public class ONE_Tests extends StatusCodeClass  {

    private Response response;
    private ExtentTest test;

    @Test(priority=1)
    public void LIST() {
        test = ExtentLiistener.getTest();
        RestAssured.baseURI = BASE_URI;

        String Payload = "{\"sortBy\":\"buyingDate\",\"pageNumber\":0,\"pageSize\":10,\"sortDirection\":\"desc\",\"filters\":[{\"key\":\"buyingDate\",\"value\":\"buyingDate\",\"from\":\"2025-09-22T23:59:00.000Z\",\"to\":\"2025-12-22T00:00:00.000Z\"}],\"searchText\":\"\"}";
        StatusCodeClass.logPayload(test, Payload);

        try {
            response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", TokenManager.getToken())
                    .header("apikey", "4grtm51a8e35gfvl7icab76hdr")
                    .body(Payload)
                    .when().post("https://1qhy8b6jni.execute-api.ap-south-1.amazonaws.com/uat/api/buying/getBuyingList")
                    .then().extract().response();

            StatusCodeClass.validateStatusCode(response, test);
            StatusCodeClass.validateAuth(response, test);
            StatusCodeClass.validateResponseTime(response, test);
        } catch (Exception e) {
            Assert.fail("API Failed: LIST");
        }
    }

}