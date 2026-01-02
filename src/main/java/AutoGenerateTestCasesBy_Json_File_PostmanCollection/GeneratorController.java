package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import org.testng.TestNG;
import org.testng.xml.XmlSuite;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST,
		RequestMethod.OPTIONS })
public class GeneratorController {

	private static final Logger log = LoggerFactory.getLogger(GeneratorController.class);

	// =====================================================
	// 1) OPEN EXTENT REPORT
	// =====================================================
	@GetMapping(value = "/report", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<byte[]> openReport() throws Exception {

		File reportFile = new File("target/test-output/AutoGenerateTestCasesBy-Json-File-API-Report.html");

		if (!reportFile.exists()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Report not generated yet".getBytes());
		}

		return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
				.header(HttpHeaders.PRAGMA, "no-cache").header(HttpHeaders.EXPIRES, "0")
				.contentType(MediaType.TEXT_HTML).body(Files.readAllBytes(reportFile.toPath()));
	}

	// =====================================================
	// 2) HEALTH CHECK
	// =====================================================
	@GetMapping("/ping")
	public String ping() {
		return "API WORKING";
	}

	// =====================================================
	// 3) GENERATE TESTS (❌ NO TOKEN HERE)
	// =====================================================
	@RequestMapping(value = "/generate-from-postman", method = {
			RequestMethod.POST }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> generateFromPostman(@RequestParam("file") MultipartFile file,
			@RequestParam(value = "token", required = false) String token, HttpServletRequest request) {

		log.info("[/api/generate-from-postman] RAW headers start");
		Enumeration<String> names = request.getHeaderNames();
		while (names != null && names.hasMoreElements()) {
			String name = names.nextElement();
			log.info("    {} = {}", name, request.getHeader(name));
		}
		log.info("[/api/generate-from-postman] RAW headers end");

		try {
			log.info(
					"[/api/generate-from-postman] Incoming request: originalFilename='{}', size={} bytes, contentType={}, tokenProvided={}",
					file != null ? file.getOriginalFilename() : null, file != null ? file.getSize() : -1,
					file != null ? file.getContentType() : null, token != null && !token.isEmpty());

			// Log some request headers to help debug browser/preflight issues
			try {
				String origin = request.getHeader("Origin");
				String acrh = request.getHeader("Access-Control-Request-Headers");
				String acrm = request.getHeader("Access-Control-Request-Method");
				log.info("[/api/generate-from-postman] request headers: Origin={}, ACR-Method={}, ACR-Headers={}",
						origin, acrm, acrh);
			} catch (Exception ignored) {
			}

			if (file == null || file.isEmpty()) {
				log.warn("[/api/generate-from-postman] No file uploaded or file is empty");
				return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
			}

			// =========================================================
			// ORIGINAL FILE NAME (SOURCE OF TRUTH)
			// =========================================================
			String originalName = file.getOriginalFilename();

			if (originalName == null || !originalName.endsWith(".json")) {
				return ResponseEntity.badRequest().body(Map.of("error", "Invalid JSON file"));
			}

			// sanitize ONCE
			String baseName = originalName.replace(".json", "");
			String safeName = baseName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

			// =========================================================
			// TEMP FILE (CONTENT ONLY)
			// =========================================================
			// Use a writable temp directory to avoid permission issues under systemd
			File uploadDir = new File(System.getProperty("java.io.tmpdir"), "uploads");
			if (!uploadDir.exists() && !uploadDir.mkdirs()) {
				log.error("[/api/generate-from-postman] Failed to create uploads directory at {}",
						uploadDir.getAbsolutePath());
				return ResponseEntity.internalServerError().body(Map.of("error", "Cannot create upload directory"));
			}

			File tempJson = File.createTempFile("upload-", ".json", uploadDir);
			file.transferTo(tempJson);
			log.info("[/api/generate-from-postman] Temp JSON written at {} ({} bytes)", tempJson.getAbsolutePath(),
					tempJson.length());

			// =========================================================
			// GENERATE
			// =========================================================
			String xmlText = PostmanToRestAssuredGenerator.generateTestsAndReturnXML(tempJson, originalName);

			File latest = PostmanToRestAssuredGenerator.latestGeneratedXML;

			if (latest == null || !latest.exists()) {
				log.error("[/api/generate-from-postman] XML generation returned null or missing file for {}",
						originalName);
				return ResponseEntity.internalServerError().body(Map.of("error", "XML generation failed"));
			}

			log.info("[/api/generate-from-postman] XML generated: {} ({} bytes)", latest.getAbsolutePath(),
					latest.length());

			// =========================================================
			// 🔥 RETURN CLEAN NAMES TO UI
			// =========================================================
			return ResponseEntity.ok(Map.of("xml", xmlText, "name", safeName + ".xml", // ✅ JSON-based name
					"package", safeName, // ✅ package name
					"path", latest.getAbsolutePath()));

		} catch (Exception e) {
			log.error("Error generating tests", e);
			e.printStackTrace();

			String message = e.getMessage();
			if (message == null || message.isBlank()) {
				message = e.getClass().getSimpleName();
			}

			StringBuilder diagnostics = new StringBuilder(message);
			for (StackTraceElement el : e.getStackTrace()) {
				diagnostics.append("\n").append(el.toString());
				if (diagnostics.length() > 2000) {
					diagnostics.append("\n... truncated ...");
					break;
				}
			}

			return ResponseEntity.internalServerError().body(Map.of("error", diagnostics.toString()));
		}
	}

	// Handle CORS preflight explicitly to ensure OPTIONS returns quickly
	@RequestMapping(value = "/generate-from-postman", method = RequestMethod.OPTIONS)
	public ResponseEntity<Void> preflightGenerate(HttpServletRequest request) {
		log.info("[/api/generate-from-postman][OPTIONS] preflight from Origin={}", request.getHeader("Origin"));
		return ResponseEntity.ok().build();
	}

	// =====================================================
	// 6) RUN TESTNG SUITE (✅ TOKEN SET ONLY HERE)
	// =====================================================
	@PostMapping(value = "/run-suite", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> runSuite(@RequestBody Map<String, String> body) {

		ClassLoader originalCL = Thread.currentThread().getContextClassLoader();

		try {
			String token = body.get("token");

			if (token == null || token.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Authorization token required"));
			}

			token = token.trim();
			TokenManager.setToken(token);

			File xmlFile = PostmanToRestAssuredGenerator.latestGeneratedXML;

			if (xmlFile == null || !xmlFile.exists()) {
				return ResponseEntity.badRequest().body(Map.of("error", "XML not found"));
			}

			File projectDir = new File(System.getProperty("user.dir"));

			RuntimeTestCompiler.CompileResult result = RuntimeTestCompiler.compileAllTests(projectDir);

			if (!result.success) {
				return ResponseEntity.internalServerError().body(Map.of("error", result.output));
			}

			ClassLoader testCL = RuntimeTestCompiler.buildTestClassLoader(projectDir, originalCL);

			Thread.currentThread().setContextClassLoader(testCL);

			TestNG testNG = new TestNG();
			testNG.setUseDefaultListeners(false);
			testNG.setVerbose(0);
			testNG.setThreadCount(1);
			testNG.setParallel(XmlSuite.ParallelMode.NONE);
			testNG.setTestSuites(List.of(xmlFile.getAbsolutePath()));

			// 🔥 RUN TESTS
			testNG.run();

			// 🔥 FLUSH REPORT
			ExtentmanagerSs.flushAndReset();

			// 🔥 FETCH COUNTERS FROM LISTENER
			Map<String, Integer> summary = ExtentLiistener.getSummary();

			return ResponseEntity.ok(Map.of("status", testNG.getStatus(), "passed", summary.get("passed"), "failed",
					summary.get("failed"), "skipped", summary.get("skipped"), "total", summary.get("total")));

		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
		} finally {
			Thread.currentThread().setContextClassLoader(originalCL);
		}
	}

	// =====================================================
	// 7) LIST XML SUITES (OPEN XML)
	// =====================================================
	@GetMapping(value = "/xml-suites", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<String>> listXmlSuites() {

		List<String> names = getXmlFiles().stream().map(File::getName).toList();

		return ResponseEntity.ok(names);
	}

	// =====================================================
	// 8) OPEN SELECTED XML
	// =====================================================
	@GetMapping(value = "/xml-suites/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> openXmlSuite(@PathVariable String name) {

		File selected = getXmlFiles().stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);

		if (selected == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "XML not found"));
		}

		try {
			// 🔥 VERY IMPORTANT: SET ACTIVE XML
			PostmanToRestAssuredGenerator.latestGeneratedXML = selected;

			String xml = Files.readString(selected.toPath());

			List<String> classes = new ArrayList<>();
			Matcher m = Pattern.compile("<class\\s+name=\"(.*?)\"").matcher(xml);
			while (m.find())
				classes.add(m.group(1));

			return ResponseEntity.ok(Map.of("name", selected.getName(), "path", selected.getAbsolutePath(),
					"totalClasses", classes.size(), "classes", classes, "xml", xml));

		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
		}
	}

	// =====================================================
	// HELPERS
	// =====================================================
	private static List<File> getXmlFiles() {
		File dir = new File(System.getProperty("user.dir"));
		File[] files = dir
				.listFiles((d, n) -> n.endsWith(".xml") && !n.equalsIgnoreCase("pom.xml") && !n.startsWith("testng"));
		return files == null ? List.of() : List.of(files);
	}

}
