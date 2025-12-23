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
public class getfilter_Tests extends StatusCodeClass  {

    private Response response;
    private ExtentTest test;

    @Test(priority=1)
    public void getFilters_role_buying() {
        test = ExtentLiistener.getTest();
        RestAssured.baseURI = BASE_URI;

        String Payload = "";
        StatusCodeClass.logPayload(test, Payload);

        try {
            response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", TokenManager.getToken())
                    .header("apikey", "mra7menthnqeo950cs6gsh4hl")
                    .when().get("https://s1sgfdgek6.execute-api.ap-south-1.amazonaws.com/prod/api/buying/getFilters?role=buying")
                    .then().extract().response();

            StatusCodeClass.validateStatusCode(response, test);
            StatusCodeClass.validateAuth(response, test);
            StatusCodeClass.validateResponseTime(response, test);
        } catch (Exception e) {
            Assert.fail("API Failed: getFilters_role_buying");
        }
    }

    @Test(priority=2)
    public void getFilterValues() {
        test = ExtentLiistener.getTest();
        RestAssured.baseURI = BASE_URI;

        String Payload = "{\"columnName\":\"zone\",\"parentColumnName\":null}";
        StatusCodeClass.logPayload(test, Payload);

        try {
            response = given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", TokenManager.getToken())
                    .header("apikey", "mra7menthnqeo950cs6gsh4hl")
                    .body(Payload)
                    .when().post("https://s1sgfdgek6.execute-api.ap-south-1.amazonaws.com/prod/api/buying/getFilterValues")
                    .then().extract().response();

            StatusCodeClass.validateStatusCode(response, test);
            StatusCodeClass.validateAuth(response, test);
            StatusCodeClass.validateResponseTime(response, test);
        } catch (Exception e) {
            Assert.fail("API Failed: getFilterValues");
        }
    }

}