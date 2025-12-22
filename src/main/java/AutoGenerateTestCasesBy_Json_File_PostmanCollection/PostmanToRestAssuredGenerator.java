package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PostmanToRestAssuredGenerator {

	private static final Set<String> apiSignatureSet = new HashSet<>();
	private static int generatedClassCount = 0;

	public static File latestGeneratedXML;

	// Sanitized JSON filename used as package name
	public static String packageName = "";

	// Final directory for generated files
	public static String FINAL_PACKAGE_PATH = "";

	// =====================================================================
	// PUBLIC METHOD CALLED FROM UI
	// =====================================================================
	public static String generateTestsAndReturnXML(File postmanFile, String token) throws Exception {

		// 1️⃣ Sanitize JSON file name to create valid package
		packageName = sanitizePackageName(postmanFile.getName().replace(".json", ""));
		FINAL_PACKAGE_PATH = "src/test/java/" + packageName + "/";

		// 2️⃣ Check duplicate (package folder or XML already exists)
		File expectedXml = new File(packageName + ".xml");
		File expectedPackage = new File(FINAL_PACKAGE_PATH);

		if (expectedXml.exists() || expectedPackage.exists()) {
			throw new Exception("❌ Cannot regenerate. Existing package or XML found:\n" + "Package exists: "
					+ expectedPackage.exists() + "\n" + "XML exists: " + expectedXml.exists());
		}

		// Reset counters
		apiSignatureSet.clear();
		generatedClassCount = 0;
		latestGeneratedXML = null;

		// 3️⃣ Generate Java test classes
		generateTests(postmanFile);

		// 4️⃣ Create XML
		String xml = generateTestNGxmlAndReturnContent();

		return xml;
	}

	// =====================================================================
	// GENERATE TEST CLASSES
	// =====================================================================
	public static void generateTests(File postmanFile) throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(postmanFile);
		JsonNode items = root.get("item");

		if (items == null || !items.isArray())
			return;

		List<JsonNode> folderItems = new ArrayList<>();
		List<JsonNode> singleItems = new ArrayList<>();

		for (JsonNode item : items) {
			if (item.has("item"))
				folderItems.add(item);
			else
				singleItems.add(item);
		}

		// Make directory for package
		File dir = new File(FINAL_PACKAGE_PATH);
		dir.mkdirs();

		// ---------------------------
		// Create test class per folder
		// ---------------------------
		for (JsonNode folderNode : folderItems) {

			String folderName = sanitizeClassName(folderNode.get("name").asText());
			String className = folderName + "_Tests";

			StringBuilder cls = new StringBuilder();
			cls.append(getClassHeader(className));

			int priority = 1;
			for (JsonNode api : folderNode.get("item")) {

				if (isInvalidApi(api))
					continue;
				if (isDuplicate(api))
					continue;

				cls.append(generateTestMethod(api, priority++));
			}

			cls.append("}");

			writeClassFile(className, cls.toString());
		}

		// ---------------------------
		// Additional root-level APIs
		// ---------------------------
		if (!singleItems.isEmpty()) {

			String className = "Additional_APIs";

			StringBuilder cls = new StringBuilder();
			cls.append(getClassHeader(className));

			int priority = 1;
			for (JsonNode api : singleItems) {
				if (isInvalidApi(api))
					continue;
				if (isDuplicate(api))
					continue;
				cls.append(generateTestMethod(api, priority++));
			}

			cls.append("}");
			writeClassFile(className, cls.toString());
		}

		System.out.println("🔥 Test classes generated inside: " + FINAL_PACKAGE_PATH);
	}

	// =====================================================================
	// XML GENERATOR (JSON name = XML name)
	// =====================================================================
	private static String generateTestNGxmlAndReturnContent() throws Exception {

		StringBuilder xml = new StringBuilder();

		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<suite name=\"" + packageName + " Suite\" parallel=\"classes\" thread-count=\"5\">\n");
		xml.append("  <test name=\"" + packageName + " Tests\">\n");
		xml.append("    <classes>\n");

		File folder = new File(FINAL_PACKAGE_PATH);
		File[] files = folder.listFiles((d, n) -> n.endsWith(".java"));

		if (files != null) {
			for (File f : files) {
				xml.append("      <class name=\"").append(packageName).append(".")
						.append(f.getName().replace(".java", "")).append("\" />\n");
			}
		}

		xml.append("    </classes>\n");
		xml.append("  </test>\n");
		xml.append("</suite>");

		File xmlOut = new File(packageName + ".xml");

		if (xmlOut.exists())
			throw new Exception("❌ XML already exists: " + xmlOut.getName());

		try (FileWriter fw = new FileWriter(xmlOut)) {
			fw.write(xml.toString());
		}

		latestGeneratedXML = xmlOut;
		return xml.toString();
	}

	// =====================================================================
	// SAFE API VALIDATION (NO EXCEPTIONS)
	// =====================================================================
	private static boolean isInvalidApi(JsonNode apiItem) {
		try {

			if (apiItem == null || apiItem.isNull())
				return true;

			JsonNode name = apiItem.get("name");
			if (name == null || name.asText().trim().isEmpty())
				return true;

			JsonNode req = apiItem.get("request");
			if (req == null)
				return true;

			JsonNode method = req.get("method");
			if (method == null || method.asText().trim().isEmpty())
				return true;

			JsonNode urlNode = req.get("url");
			if (urlNode == null)
				return true;

			String url = extractUrl(urlNode);
			return url == null || url.trim().isEmpty();

		} catch (Exception e) {
			return true;
		}
	}

	// =====================================================================
	// DUPLICATE API DETECTION
	// =====================================================================
	private static boolean isDuplicate(JsonNode apiItem) {

		JsonNode req = apiItem.get("request");
		if (req == null)
			return false;

		String method = req.has("method") ? req.get("method").asText() : "";
		String url = extractUrl(req.get("url"));

		String signature = method + "::" + url;

		if (apiSignatureSet.contains(signature))
			return true;

		apiSignatureSet.add(signature);
		return false;
	}

	// =====================================================================
	// CLASS HEADER (PACKAGE = JSON NAME)
	// =====================================================================
	private static String getClassHeader(String className) {

		return "package " + packageName + ";\n\n" + "import org.testng.Assert;\n"
				+ "import org.testng.annotations.Test;\n" + "import org.testng.annotations.Listeners;\n"
				+ "import io.restassured.RestAssured;\n" + "import io.restassured.response.Response;\n"
				+ "import io.restassured.http.ContentType;\n" + "import static io.restassured.RestAssured.given;\n"
				+ "import com.aventstack.extentreports.ExtentTest;\n"
				+ "import AutoGenerateTestCasesBy_Json_File_PostmanCollection.*;\n\n"
				+ "@Listeners(ExtentLiistener.class)\n" + "public class " + className
				+ " extends StatusCodeClass implements TokenNs_vERIFICATIONN {\n\n" + "    private Response response;\n"
				+ "    private ExtentTest test;\n\n";
	}

	// =====================================================================
	// CREATE TEST METHOD
	// =====================================================================
	private static String generateTestMethod(JsonNode item, int priority) {

		String apiName = sanitizeClassName(item.get("name").asText());
		JsonNode request = item.get("request");

		String method = request.get("method").asText();
		String url = extractUrl(request.get("url"));

		String body = "";
		if (request.has("body") && request.get("body").has("raw"))
			body = request.get("body").get("raw").asText().replace("\"", "\\\"");

		String apiKey = extractApiKey(request);

		boolean hasBody = !body.isEmpty();

		StringBuilder sb = new StringBuilder();

		sb.append("    @Test(priority=" + priority + ")\n");
		sb.append("    public void " + apiName + "() {\n");
		sb.append("        test = ExtentLiistener.getTest();\n");
		sb.append("        RestAssured.baseURI = BASE_URI;\n\n");

		sb.append("        try {\n");
	//===================================================================
		sb.append("            System.out.println(\"[DEBUG] Token length=\" + TokenManager.getToken().length());\n");
		sb.append("            response = given()\n");
		sb.append("                    .log().headers()\n"); // 🔥 ADD THIS LINE
		sb.append("                    .contentType(ContentType.JSON)\n");
		sb.append("                    .header(\"Authorization\", TokenManager.getToken())\n");
		//===================================================================

		
		if (apiKey != null)
			sb.append("                    .header(\"apikey\", \"" + apiKey + "\")\n");

		if (hasBody)
			sb.append("                    .body(\"" + body + "\")\n");

		sb.append("                    .when()." + method.toLowerCase() + "(\"" + url + "\")\n");
		sb.append("                    .then().extract().response();\n");

		sb.append("            StatusCodeClass.validateStatusCode(response, test);\n");
		sb.append("            StatusCodeClass.validateAuth(response, test);\n");
		sb.append("            StatusCodeClass.validateResponseTime(response, test);\n");

		sb.append("        } catch (Exception e) {\n");
		sb.append("            Assert.fail(\"API Failed: " + apiName + "\");\n");
		sb.append("        }\n");

		sb.append("    }\n\n");

		return sb.toString();
	}

	// =====================================================================
	// UTILITIES
	// =====================================================================
	private static String sanitizePackageName(String name) {
		return name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
	}

	private static String sanitizeClassName(String name) {
		name = name.replaceAll("[^a-zA-Z0-9]", "_");
		if (Character.isDigit(name.charAt(0)))
			name = "API_" + name;
		return name;
	}

	private static String extractUrl(JsonNode urlNode) {
		try {
			if (urlNode == null)
				return "";
			if (urlNode.has("raw"))
				return urlNode.get("raw").asText();
		} catch (Exception ignored) {
		}
		return "";
	}

	private static String extractApiKey(JsonNode req) {
		try {
			if (req.has("header")) {
				for (JsonNode h : req.get("header")) {
					if (h.get("key").asText().equalsIgnoreCase("apikey"))
						return h.get("value").asText();
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private static void writeClassFile(String className, String content) throws Exception {

		File out = new File(FINAL_PACKAGE_PATH + className + ".java");

		if (out.exists())
			throw new Exception("❌ CLASS ALREADY EXISTS: " + out.getPath());

		try (FileWriter fw = new FileWriter(out)) {
			fw.write(content);
		}

		generatedClassCount++;
	}
}
