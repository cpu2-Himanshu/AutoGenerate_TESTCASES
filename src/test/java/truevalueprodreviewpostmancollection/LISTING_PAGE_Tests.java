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
public class LISTING_PAGE_Tests extends StatusCodeClass  {

    private Response response;
    private ExtentTest test;

    @Test(priority=1)
    public void getBuyingList() {
        test = ExtentLiistener.getTest();
        RestAssured.baseURI = BASE_URI;

        String Payload = "{\"sortBy\":\"buyingDate\",\"pageNumber\":0,\"pageSize\":10,\"sortDirection\":\"desc\",\"filters\":[{\"key\":\"buyingDate\",\"value\":\"buyingDate\",\"from\":\"2025-09-11T23:59:00.000Z\",\"to\":\"2025-12-11T23:59:00.000Z\"}],\"searchText\":\"\"}";
        StatusCodeClass.logPayload(test, Payload);

        try {
            response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", TokenManager.getToken())
                    .header("apikey", "mra7menthnqeo950cs6gsh4hl")
                    .body(Payload)
                    .when().post("https://s1sgfdgek6.execute-api.ap-south-1.amazonaws.com/prod/api/buying/getBuyingList")
                    .then().extract().response();

            StatusCodeClass.validateStatusCode(response, test);
            StatusCodeClass.validateAuth(response, test);
            StatusCodeClass.validateResponseTime(response, test);
        } catch (Exception e) {
            Assert.fail("API Failed: getBuyingList");
        }
    }

}