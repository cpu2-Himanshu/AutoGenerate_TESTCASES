package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class GeneratorController {

    private static final Logger log =
            LoggerFactory.getLogger(GeneratorController.class);

    // =====================================================
    // 1) OPEN EXTENT REPORT
    // =====================================================
    @GetMapping(value = "/report", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> openReport() throws Exception {

        File reportFile = new File(
                "target/test-output/AutoGenerateTestCasesBy-Json-File-API-Report.html"
        );

        if (!reportFile.exists()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("Report not generated yet".getBytes());
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentType(MediaType.TEXT_HTML)
                .body(Files.readAllBytes(reportFile.toPath()));
    }

    // =====================================================
    // 2) HEALTH CHECK
    // =====================================================
    @GetMapping("/ping")
    public String ping() {
        return "API WORKING";
    }

    // =====================================================
    // 3) GENERATE TESTS FROM POSTMAN JSON
    // =====================================================
    @PostMapping(value = "/generate-from-postman",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> generateFromPostman(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "token", required = false) String token) {

        Map<String, Object> payload = new LinkedHashMap<>();

        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No file uploaded"));
            }

            // ---------------- TOKEN HANDLING ----------------
            String authToken = token == null ? "" : token.trim();
            if (authToken.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Authorization token required"));
            }

            // 🔥 STORE TOKEN GLOBALLY (FOR ALL TESTS)
            TokenManager.setToken(authToken);
            log.info("Token stored during XML generation | length={}",
                    authToken.length());

            // ---------------- FILE HANDLING ----------------
            File tempJson = File.createTempFile("postman-", ".json");
            file.transferTo(tempJson);

            // ---------------- GENERATE TESTS ----------------
            String xmlText =
                    PostmanToRestAssuredGenerator
                            .generateTestsAndReturnXML(tempJson, authToken);

            File latest =
                    PostmanToRestAssuredGenerator.latestGeneratedXML;

            payload.put("xml", xmlText);
            payload.put("name",
                    latest != null ? latest.getName() : "suite.xml");
            payload.put("path",
                    latest != null ? latest.getAbsolutePath() : "N/A");

            return ResponseEntity.ok(payload);

        } catch (Exception e) {
            log.error("Error generating tests", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // 4) LIST XML SUITES
    // =====================================================
    @GetMapping(value = "/xml-suites",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> listXmlSuites() {

        List<String> names = getXmlFiles()
                .stream()
                .map(File::getName)
                .toList();

        return ResponseEntity.ok(names);
    }

    // =====================================================
    // 5) OPEN XML SUITE
    // =====================================================
    @GetMapping(value = "/xml-suites/{name}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> openXmlSuite(
            @PathVariable String name) {

        File selected = getXmlFiles().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElse(null);

        if (selected == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "XML not found"));
        }

        try {
            PostmanToRestAssuredGenerator.latestGeneratedXML = selected;

            String xml = Files.readString(selected.toPath());

            List<String> classes = new ArrayList<>();
            Matcher m =
                    Pattern.compile("<class\\s+name=\"(.*?)\"")
                            .matcher(xml);

            while (m.find()) classes.add(m.group(1));

            return ResponseEntity.ok(Map.of(
                    "name", selected.getName(),
                    "path", selected.getAbsolutePath(),
                    "totalClasses", classes.size(),
                    "classes", classes,
                    "xml", xml
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // =====================================================
    // 6) RUN TESTNG SUITE
    // =====================================================
    @PostMapping(value = "/run-suite",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> runSuite(
            @RequestBody Map<String, String> body) {

        Map<String, Object> payload = new LinkedHashMap<>();
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();

        try {
            String token = body.get("token");

            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Authorization token required"));
            }

            // 🔥 CLEAN + STORE
            TokenManager.setToken(token);

            File xmlFile = PostmanToRestAssuredGenerator.latestGeneratedXML;
            if (xmlFile == null || !xmlFile.exists()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "XML not found"));
            }

            File projectDir = new File(System.getProperty("user.dir"));

            RuntimeTestCompiler.CompileResult result =
                RuntimeTestCompiler.compileAllTests(projectDir);

            if (!result.success) {
                return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Compilation failed"));
            }

            ClassLoader testCL =
                RuntimeTestCompiler.buildTestClassLoader(projectDir, originalCL);
            Thread.currentThread().setContextClassLoader(testCL);

            TestNG testNG = new TestNG();
            testNG.setUseDefaultListeners(false);
            testNG.setVerbose(0);
            testNG.setTestSuites(List.of(xmlFile.getAbsolutePath()));
            testNG.run();

            ExtentmanagerSs.flushAndReset();

            return ResponseEntity.ok(
                Map.of("status", testNG.getStatus())
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }
    }


    // =====================================================
    // HELPERS
    // =====================================================
    private static String extractTokenFromBody(String raw) {
        if (raw == null) return "";
        raw = raw.trim();
        if (raw.startsWith("token="))
            return raw.substring(6);
        return raw;
    }

    private static List<File> getXmlFiles() {
        File dir = new File(System.getProperty("user.dir"));
        File[] files = dir.listFiles(
                (d, n) -> n.endsWith(".xml") && !n.startsWith("testng"));
        return files == null ? List.of() : List.of(files);
    }
}
