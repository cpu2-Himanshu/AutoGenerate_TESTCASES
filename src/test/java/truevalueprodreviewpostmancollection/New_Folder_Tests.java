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
public class New_Folder_Tests extends StatusCodeClass  {

    private Response response;
    private ExtentTest test;

}