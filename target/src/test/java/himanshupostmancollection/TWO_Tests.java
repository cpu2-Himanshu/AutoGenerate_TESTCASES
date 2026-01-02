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
public class TWO_Tests extends StatusCodeClass {

    protected Response response;
    protected ExtentTest test;

    @Test(priority=1)
    public void FILTER() {
        test = ExtentLiistener.getTest();
        RestAssured.baseURI = "https://1qhy8b6jni.execute-api.ap-south-1.amazonaws.com/uat/";

        try {
            response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", TokenManager.getToken())
                .header("apikey", "4grtm51a8e35gfvl7icab76hdr")
                .when().get("api/buying/getFilters?role=buying")
                .then().extract().response();

            StatusCodeClass.validateStatusCode(response, test);
            StatusCodeClass.validateAuth(response, test);
            StatusCodeClass.validateResponseTime(response, test);
        } catch (Exception e) {
            Assert.fail("API Failed: FILTER");
        }
    }

}