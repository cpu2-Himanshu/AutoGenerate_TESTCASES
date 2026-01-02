package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostmanToRestAssuredGenerator {

    private static final Logger log = LoggerFactory.getLogger(PostmanToRestAssuredGenerator.class);

    private static final Set<String> apiSignatureSet = new HashSet<>();
    public static File latestGeneratedXML;

    public static String packageName = "";
    public static String FINAL_PACKAGE_PATH = "";
    public static String CLEAN_JSON_NAME = "";
    
    // Runtime test source directory (works both in dev and production)
    private static final String RUNTIME_TEST_SRC = "runtime-generated-tests";
    private static final String FALLBACK_TEST_SRC = "target/runtime-generated-tests";

    // =====================================================================
    // ENTRY POINT (CALLED FROM UI)
    // =====================================================================
    public static String generateTestsAndReturnXML(
            File postmanFile,
            String originalJsonName
    ) throws Exception {

    long start = System.currentTimeMillis();
    log("[GEN] start generate for %s (%d bytes)", originalJsonName, postmanFile.length());

    CLEAN_JSON_NAME = originalJsonName.replace(".json", "");
    packageName = sanitizePackageName(CLEAN_JSON_NAME);
    log("[GEN] package=%s", packageName);
        
        // Use runtime-generated-tests directory (creates if doesn't exist)
        // Falls back to src/test/java for development mode compatibility
        File testSrcRoot = resolveWritableTestRoot();
        FINAL_PACKAGE_PATH = testSrcRoot.getPath() + "/" + packageName + "/";
    log("[GEN] testSrcRoot=%s", testSrcRoot.getAbsolutePath());

        File xml = new File(packageName + ".xml");
        File pkg = new File(FINAL_PACKAGE_PATH);

        // If same JSON name was generated before, clean it up so a new upload can proceed
        if (xml.exists()) {
            log("[GEN] deleting old xml %s", xml.getAbsolutePath());
            xml.delete();
        }
        if (pkg.exists()) {
            log("[GEN] deleting old package dir %s", pkg.getAbsolutePath());
            deleteRecursively(pkg);
        }

    apiSignatureSet.clear();
    generateTests(postmanFile);

    String xmlText = generateTestNGxmlAndReturnContent(CLEAN_JSON_NAME);
    log("[GEN] done in %d ms", System.currentTimeMillis() - start);
    return xmlText;
    }

    // =====================================================================
    // GENERATE TEST CLASSES
    // =====================================================================
    private static void generateTests(File postmanFile) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        log("[GEN] parsing JSON at %s", postmanFile.getAbsolutePath());
        JsonNode root = mapper.readTree(postmanFile);
        JsonNode items = root.get("item");
        if (items == null || !items.isArray()) {
            log("[GEN] no 'item' array present; nothing to generate");
            return;
        }

        new File(FINAL_PACKAGE_PATH).mkdirs();

        List<JsonNode> folders = new ArrayList<>();
        List<JsonNode> rootApis = new ArrayList<>();

        for (JsonNode item : items) {
            if (item.has("item")) folders.add(item);
            else if (item.has("request")) rootApis.add(item);
        }

        // Folder APIs
        for (JsonNode folder : folders) {
            String className =
                sanitizeClassName(folder.get("name").asText()) + "_Tests";

            StringBuilder cls = new StringBuilder();
            cls.append(getClassHeader(className));

            int p = 1;
            for (JsonNode api : folder.get("item")) {
                if (isInvalidApi(api) || isDuplicate(api)) continue;
                cls.append(generateTestMethod(api, p++));
            }
            cls.append("}");
            writeClassFile(className, cls.toString());
        }

        // Root APIs → Additional_APIs
        if (!rootApis.isEmpty()) {
            String className = "Additional_APIs";
            StringBuilder cls = new StringBuilder();
            cls.append(getClassHeader(className));

            int p = 1;
            for (JsonNode api : rootApis) {
                if (isInvalidApi(api) || isDuplicate(api)) continue;
                cls.append(generateTestMethod(api, p++));
            }
            cls.append("}");
            writeClassFile(className, cls.toString());
        }
    }

    // =====================================================================
    // TEST METHOD (BASE URI + ENDPOINT SPLIT)
    // =====================================================================
    private static String generateTestMethod(JsonNode item, int priority) {

        String apiName = sanitizeClassName(item.get("name").asText());
        JsonNode req = item.get("request");

        String method = req.get("method").asText().toLowerCase();
        String fullUrl = extractUrl(req.get("url"));

        String[] parts = splitBaseAndEndpoint(fullUrl);
        String baseUrl = parts[0];
        String endpoint = parts[1];

        String payload = "";
        if (req.has("body") && req.get("body").has("raw")) {
            payload = req.get("body").get("raw").asText();
        }

        String apiKey = extractApiKey(req);
        StringBuilder sb = new StringBuilder();

        sb.append("    @Test(priority=").append(priority).append(")\n");
        sb.append("    public void ").append(apiName).append("() {\n");
        sb.append("        test = ExtentLiistener.getTest();\n");
        sb.append("        RestAssured.baseURI = \"")
          .append(baseUrl).append("\";\n\n");

        if (!payload.isEmpty()) {
            sb.append("        String Payload = \"\"\"\n")
              .append(payload)
              .append("\n\"\"\";\n");
            sb.append("        StatusCodeClass.logPayload(test, Payload);\n\n");
        }

        sb.append("        try {\n");
        sb.append("            response = given()\n");
        sb.append("                .contentType(ContentType.JSON)\n");
        sb.append("                .header(\"Authorization\", TokenManager.getToken())\n");

        if (apiKey != null) {
            sb.append("                .header(\"apikey\", \"")
              .append(apiKey).append("\")\n");
        }

        if (!payload.isEmpty()) {
            sb.append("                .body(Payload)\n");
        }

        sb.append("                .when().").append(method)
          .append("(\"").append(endpoint).append("\")\n");
        sb.append("                .then().extract().response();\n\n");

        sb.append("            StatusCodeClass.validateStatusCode(response, test);\n");
        sb.append("            StatusCodeClass.validateAuth(response, test);\n");
        sb.append("            StatusCodeClass.validateResponseTime(response, test);\n");
        sb.append("        } catch (Exception e) {\n");
        sb.append("            Assert.fail(\"API Failed: ")
          .append(apiName).append("\");\n");
        sb.append("        }\n");
        sb.append("    }\n\n");

        return sb.toString();
    }

    // =====================================================================
    // URL SPLITTER (CORE FIX)
    // =====================================================================
    private static String[] splitBaseAndEndpoint(String fullUrl) {

        fullUrl = fullUrl.replaceAll("\\R.*", "")
                         .replaceAll("--.*", "")
                         .trim();

        int idx = fullUrl.indexOf("/", 8);
        if (idx == -1) return new String[]{fullUrl + "/", ""};

        String rem = fullUrl.substring(idx + 1);
        int stageEnd = rem.indexOf("/");

        if (stageEnd == -1)
            return new String[]{fullUrl + "/", ""};

        String base =
            fullUrl.substring(0, idx + 1 + stageEnd + 1);
        String endpoint = rem.substring(stageEnd + 1);

        return new String[]{base, endpoint};
    }

    // =====================================================================
    // XML GENERATOR
    // =====================================================================
    private static String generateTestNGxmlAndReturnContent(String name)
            throws Exception {

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<suite name=\"").append(packageName).append(" Suite\">\n");
        xml.append("  <test name=\"").append(packageName).append(" Tests\">\n");
        xml.append("    <classes>\n");

        for (File f : Objects.requireNonNull(
                new File(FINAL_PACKAGE_PATH)
                .listFiles((d, n) -> n.endsWith(".java")))) {

            xml.append("      <class name=\"")
               .append(packageName).append(".")
               .append(f.getName().replace(".java", ""))
               .append("\" />\n");
        }

        xml.append("    </classes>\n  </test>\n</suite>");

        File out = new File(packageName + ".xml");
        try (FileWriter fw = new FileWriter(out)) {
            fw.write(xml.toString());
        }

        latestGeneratedXML = out;
        log("[GEN] xml written %s (%d bytes)", out.getAbsolutePath(), out.length());
        return xml.toString();
    }

    // =====================================================================
    // HELPERS
    // =====================================================================
    private static String extractUrl(JsonNode url) {
        return url != null && url.has("raw") ? url.get("raw").asText() : "";
    }

    private static boolean isInvalidApi(JsonNode api) {
        return api == null || api.get("request") == null;
    }

    private static boolean isDuplicate(JsonNode api) {
        JsonNode r = api.get("request");
        String sig =
            r.get("method").asText() + "::" + extractUrl(r.get("url"));
        return !apiSignatureSet.add(sig);
    }

    private static String extractApiKey(JsonNode req) {
        if (req.has("header")) {
            for (JsonNode h : req.get("header")) {
                if ("apikey".equalsIgnoreCase(h.get("key").asText()))
                    return h.get("value").asText();
            }
        }
        return null;
    }

    private static void writeClassFile(String cls, String content)
            throws Exception {
        try (FileWriter fw =
             new FileWriter(FINAL_PACKAGE_PATH + cls + ".java")) {
            fw.write(content);
            log("[GEN] wrote class %s/%s.java", FINAL_PACKAGE_PATH, cls);
        }
    }

    private static String sanitizePackageName(String s) {
        if (s == null) {
            return "generated";
        }

        String cleaned = s.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (cleaned.isEmpty()) {
            return "generated";
        }
        if (!Character.isJavaIdentifierStart(cleaned.charAt(0))) {
            cleaned = "g" + cleaned;
        }
        return cleaned;
    }

    private static String sanitizeClassName(String s) {
        if (s == null) {
            return "GeneratedApi";
        }

        s = s.replaceAll("[^a-zA-Z0-9 ]", " ").trim();
        if (s.isEmpty()) {
            return "GeneratedApi";
        }

        StringBuilder b = new StringBuilder();
        for (String p : s.split("\\s+")) {
            if (p.isEmpty()) {
                continue;
            }
            b.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                b.append(p.substring(1));
            }
        }

        if (b.length() == 0) {
            return "GeneratedApi";
        }

        if (!Character.isJavaIdentifierStart(b.charAt(0))) {
            b.insert(0, "Api");
        }

        for (int i = 0; i < b.length(); i++) {
            if (!Character.isJavaIdentifierPart(b.charAt(i))) {
                b.setCharAt(i, '_');
            }
        }

        return b.toString();
    }

    private static String getClassHeader(String cls) {
        return "package " + packageName + ";\n\n"
             + "import org.testng.*;\n"
             + "import org.testng.annotations.*;\n"
             + "import io.restassured.*;\n"
             + "import io.restassured.http.ContentType;\n"
             + "import io.restassured.response.Response;\n"
             + "import static io.restassured.RestAssured.given;\n"
             + "import com.aventstack.extentreports.ExtentTest;\n"
             + "import AutoGenerateTestCasesBy_Json_File_PostmanCollection.*;\n\n"
             + "@Listeners(ExtentLiistener.class)\n"
             + "public class " + cls
             + " extends StatusCodeClass {\n\n"
             + "    protected Response response;\n"
             + "    protected ExtentTest test;\n\n";
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        if (!f.delete()) {
            log("[GEN] failed to delete %s", f.getAbsolutePath());
        }
    }

    private static void log(String fmt, Object... args) {
        try {
            log.info(fmt, args);
        } catch (Exception ignored) {
            // swallow logging issues to avoid impacting generation
        }
    }

    private static File resolveWritableTestRoot() throws Exception {
        List<File> candidates = List.of(
                new File("src/test/java"),
                new File(RUNTIME_TEST_SRC),
                new File(FALLBACK_TEST_SRC),
                new File(System.getProperty("java.io.tmpdir"), "runtime-generated-tests")
        );

        for (File candidate : candidates) {
            try {
                if (!candidate.exists() && !candidate.mkdirs()) {
                    log("[GEN] unable to create %s", candidate.getAbsolutePath());
                    continue;
                }
                File probe = new File(candidate, ".write-test");
                if (probe.exists() && !probe.delete()) {
                    log("[GEN] unable to delete probe %s", probe.getAbsolutePath());
                }
                if (!probe.createNewFile()) {
                    log("[GEN] failed to create probe %s", probe.getAbsolutePath());
                    continue;
                }
                if (!probe.delete()) {
                    log("[GEN] failed to delete probe %s", probe.getAbsolutePath());
                }
                log("[GEN] using test root %s", candidate.getAbsolutePath());
                return candidate;
            } catch (Exception ex) {
                log("[GEN] candidate %s not usable: %s", candidate.getAbsolutePath(), ex.getMessage());
            }
        }

        throw new Exception("No writable runtime test directory found");
    }
}
