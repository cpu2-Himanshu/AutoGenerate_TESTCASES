package himanshupostmancollection;

import org.testng.*;
import org.testng.annotations.*;
import io.restassured.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import static io.restassured.RestAssured.given;
import com.aventstack.extentreports.ExtentTest;
import AutoGenerateTestCasesBy_Json_File_PostmanCollection.*;

@Listeners(ExtentLiistener.class)
public class ONE_Tests extends StatusCodeClass {

    protected Response response;
    protected ExtentTest test;

    @Test(priority=1)
    public void LIST() {
        test = ExtentLiistener.getTest();
        RestAssured.baseURI = "https://1qhy8b6jni.execute-api.ap-south-1.amazonaws.com/uat/";

        String Payload = """
{"sortBy":"buyingDate","pageNumber":0,"pageSize":10,"sortDirection":"desc","filters":[{"key":"buyingDate","value":"buyingDate","from":"2025-09-22T23:59:00.000Z","to":"2025-12-22T00:00:00.000Z"}],"searchText":""}
""";
        StatusCodeClass.logPayload(test, Payload);

        try {
            response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", TokenManager.getToken())
                .header("apikey", "4grtm51a8e35gfvl7icab76hdr")
                .body(Payload)
                .when().post("api/buying/getBuyingList")
                .then().extract().response();

            StatusCodeClass.validateStatusCode(response, test);
            StatusCodeClass.validateAuth(response, test);
            StatusCodeClass.validateResponseTime(response, test);
        } catch (Exception e) {
            Assert.fail("API Failed: LIST");
        }
    }

}