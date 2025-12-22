package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import static io.restassured.RestAssured.given;

import org.testng.Assert;
import org.testng.annotations.Listeners;

import com.aventstack.extentreports.ExtentTest;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

@Listeners(ExtentLiistener.class)

public class StatusCodeClass implements TokenNs_vERIFICATIONN {

	public Response response;
	public ExtentTest test;

	// ==============================================================================================
	// 1️ Collapsible Payload Logger
	public static void logPayload(ExtentTest test, String payload) {
		String safePayload = (payload == null || payload.trim().isEmpty()) 
				? "No Payload (Empty Request Body)" : payload;

		String collapsiblePayload = "<details><summary><span style='color:#2E86C1;'>👉 Click to See Payload</span></summary>"
				+ "<pre>" + safePayload + "</pre></details>";

		ReportloggerforColorr.logInfo(test, collapsiblePayload);
	}

	// ==============================================================================================
	// 2️ Status Code Validation
	public static void validateStatusCode(Response response, ExtentTest test) {
		int statusCode = response.getStatusCode();

		if (statusCode >= 200 && statusCode < 300) {
			ReportloggerforColorr.logPass(test, "✅ Status Code: " + statusCode + " (Success)");
		} else {
			ReportloggerforColorr.logFail(test, "❌ Unexpected Status Code: " + statusCode);
			Assert.fail("Expected 200 but got: " + statusCode);
		}

		// Log full response
		String responseBody = response.getBody().asString();
		String collapsibleResponse = "<details><summary><span style='color:#C71585;'>👉 Click to See Full Response Body</span></summary>"
				+ "<pre>" + responseBody + "</pre></details>";

		ReportloggerforColorr.logInfo(test, collapsibleResponse);
	}

	// ==============================================================================================
	//  Token & API key Authentication Validation
	public static void validateAuth(Response response, ExtentTest test) {

		// Validate Token present
		if (TOKEN_Generated == null || TOKEN_Generated.trim().isEmpty()) {
			ReportloggerforColorr.logFail(test, "❌ Authorization Token is Missing");
			Assert.fail("Token missing.");
		} else {
			ReportloggerforColorr.logPass(test, "🔐 Token Present (Authorization Verified)");
		}

		// Validate API Key present
		if (apiKey == null || apiKey.trim().isEmpty()) {
			ReportloggerforColorr.logFail(test, "❌ API Key is Missing");
			Assert.fail("API Key missing.");
		} else {
			ReportloggerforColorr.logPass(test, "🔑 API Key Present (Authentication Verified)");
		}
	}

	// ==============================================================================================
	//  Response Time Validation
	public static void validateResponseTime(Response response, ExtentTest test) {
		long time = response.time();
		long maxAllowed = 3000; // 3 seconds

		if (time <= maxAllowed) {
			ReportloggerforColorr.logPass(test, "⏱ Response Time: " + time + " ms (Within Limit)");
		} else {
			ReportloggerforColorr.logInfo(test,
					"❌ Slow API! Took: " + time + " ms | Allowed: " + maxAllowed + " ms");
		}
	}

	// ==============================================================================================
	//  Content-Type Validation
	public void validateContentType(Response response) {
		String contentType = response.getContentType();

		if (contentType != null && contentType.contains("application/json")) {
			ReportloggerforColorr.logPass(test, "📄 Content-Type Valid: " + contentType);
		} else {
			ReportloggerforColorr.logInfo(test, "❌ Invalid Content-Type: " + contentType);
		}
	}

	// ==============================================================================================
	// 6️ POST OR GET API CALL (Enhanced)
	public void performApiTestforPOST(String endpoint, String payload, boolean isPost) {

		test = ExtentLiistener.getTest();
		RestAssured.baseURI = BASE_URI;

		ReportloggerforColorr.logInfo(test, "🔄 Initiating " + endpoint + " API request...");

		// Log payload
		StatusCodeClass.logPayload(test, payload);

		//  Validate Authentication & Authorization
		validateAuth( response, test);

		try {
			if (isPost) {
				response = given().relaxedHTTPSValidation().contentType(ContentType.JSON)
						.header("authorization", TOKEN_Generated).header("apikey", apiKey).body(payload).when()
						.post(endpoint).then().extract().response();
			} else {
				response = given().relaxedHTTPSValidation().contentType(ContentType.JSON)
						.header("authorization", TOKEN_Generated).header("apikey", apiKey).when().get(endpoint)
						.then().extract().response();
			}

			ReportloggerforColorr.logInfo(test, "✔ API call completed. Validating...");

			// Validate core items
			StatusCodeClass.validateStatusCode(response, test);
			validateResponseTime( response, test);
			validateContentType(response);

		} catch (Exception e) {
			ReportloggerforColorr.logFail(test, "❌ Exception occurred during " + endpoint + " API.", e);
			Assert.fail(endpoint + " API failed due to exception.");
		}
	}

	// ==============================================================================================
	// 7️ GET API CALL (Enhanced)
	public void performApiTestforGET(String endpoint) {

		test = ExtentLiistener.getTest();
		RestAssured.baseURI = BASE_URI;

		ReportloggerforColorr.logInfo(test, "🔄 Initiating " + endpoint + " API request...");

		// 🔐 Validate Authentication & Authorization
		validateAuth( response,  test);

		try {
			response = given().relaxedHTTPSValidation().contentType(ContentType.JSON)
					.header("authorization", TOKEN_Generated).header("apikey", apiKey).when().get(endpoint).then()
					.extract().response();

			ReportloggerforColorr.logInfo(test, "✔ API call completed. Validating...");

			// Validate core items
			StatusCodeClass.validateStatusCode(response, test);
			validateResponseTime( response,  test);
			validateContentType(response);

		} catch (Exception e) {
			ReportloggerforColorr.logFail(test, "❌ Exception occurred during GET API: " + endpoint, e);
			Assert.fail(endpoint + " GET API failed due to exception.");
		}
	}

}
