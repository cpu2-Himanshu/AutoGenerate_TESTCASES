package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

import javax.tools.*;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class RuntimeTestCompiler {

    public static class CompileResult {
        public final boolean success;
        public final String output;

        public CompileResult(boolean success, String output) {
            this.success = success;
            this.output = output;
        }
    }

    public static CompileResult compileAllTests(File projectDir) {
        try {
            System.out.println("=== RuntimeTestCompiler.compileAllTests() START ===");
            System.out.println("Project directory: " + projectDir.getAbsolutePath());
            
            File testSrc = new File(projectDir, "src/test/java");
            File runtimeTestSrc = new File(projectDir, "runtime-generated-tests");
            File outDir = new File(projectDir, "target/test-classes");
            File mainOut = new File(projectDir, "target/classes");

            System.out.println("testSrc exists: " + testSrc.exists() + " | " + testSrc.getAbsolutePath());
            System.out.println("runtimeTestSrc exists: " + runtimeTestSrc.exists() + " | " + runtimeTestSrc.getAbsolutePath());
            System.out.println("outDir exists: " + outDir.exists() + " | " + outDir.getAbsolutePath());
            System.out.println("mainOut exists: " + mainOut.exists() + " | " + mainOut.getAbsolutePath());

            // NOTE: We no longer return early just because .class files exist in target/test-classes.
            // If generated .java files are present we should attempt to compile them so updates take effect.

            // DEVELOPMENT MODE: Compile test sources at runtime (requires JDK)
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            System.out.println("JavaCompiler available: " + (compiler != null));
            if (compiler == null) {
                System.out.println("ERROR: JavaCompiler is NULL");
                throw new IllegalStateException(
                    "JavaCompiler not available. Application must run with JDK (javac), not JRE."
                );
            }

            outDir.mkdirs();
            mainOut.mkdirs();
            System.out.println("Created directories: " + outDir.exists() + ", " + mainOut.exists());

            // Check both standard test source dir and runtime-generated dir
            if (!testSrc.exists() && !runtimeTestSrc.exists()) {
                System.out.println("ERROR: No test source directories found");
                return new CompileResult(false,
                        "Test source folder not found: neither " + testSrc.getAbsolutePath() + 
                        " nor " + runtimeTestSrc.getAbsolutePath() + " exist");
            }
            
            // Collect .java files from both directories
            List<File> javaFiles = new ArrayList<>();
            
            if (testSrc.exists()) {
                System.out.println("Scanning testSrc for .java files...");
                try (var stream = Files.walk(testSrc.toPath())) {
                    stream.filter(p -> p.toString().endsWith(".java"))
                          .map(p -> p.toFile())
                          .forEach(javaFiles::add);
                }
                System.out.println("Found " + javaFiles.size() + " .java files in testSrc");
            }
            
            if (runtimeTestSrc.exists()) {
                System.out.println("Scanning runtimeTestSrc for .java files...");
                int beforeCount = javaFiles.size();
                try (var stream = Files.walk(runtimeTestSrc.toPath())) {
                    stream.filter(p -> p.toString().endsWith(".java"))
                          .map(p -> p.toFile())
                          .forEach(javaFiles::add);
                }
                System.out.println("Found " + (javaFiles.size() - beforeCount) + " .java files in runtimeTestSrc");
            }

            System.out.println("Total .java files to compile: " + javaFiles.size());
            if (javaFiles.isEmpty()) {
                System.out.println("ERROR: No .java files found");
                return new CompileResult(false,
                        "No test .java files found under " + testSrc.getAbsolutePath());
            }
            
            // Print all files to be compiled
            System.out.println("Files to compile:");
            for (File f : javaFiles) {
                System.out.println("  - " + f.getAbsolutePath());
            }

            DiagnosticCollector<JavaFileObject> diagnostics =
                    new DiagnosticCollector<>();

            StandardJavaFileManager fm =
                    compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);

            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outDir));
            System.out.println("Set CLASS_OUTPUT to: " + outDir.getAbsolutePath());

            // BUILD COMPLETE CLASSPATH
            // For Spring Boot fat JAR, dependencies live inside BOOT-INF/lib. The JavaCompiler
            // requires filesystem paths to JAR files, so when running from the fat JAR we extract
            // those dependency JAR entries into a temporary directory and include them on -classpath.
            List<String> classpathEntries = new ArrayList<>();

            // Always include main classes and the target test-classes output dir
            classpathEntries.add(mainOut.getAbsolutePath());
            classpathEntries.add(outDir.getAbsolutePath());

            // Try to find the actual JAR file path
            // When running from Spring Boot fat JAR, we need to extract BOOT-INF/lib dependencies
            String jarPath = null;
            try {
                // Method 1: Extract from code source location (nested:/.../demo.jar!/BOOT-INF/classes/!)
                String codeSource = RuntimeTestCompiler.class.getProtectionDomain()
                        .getCodeSource().getLocation().toString();
                System.out.println("Code source location: " + codeSource);
                
                if (codeSource.startsWith("nested:") || codeSource.contains(".jar")) {
                    // Extract the actual jar file path from nested URL
                    // Format: nested:/path/to/demo.jar!/BOOT-INF/classes/!
                    String path = codeSource;
                    if (path.startsWith("nested:")) {
                        path = path.substring(7); // Remove "nested:"
                    }
                    if (path.startsWith("file:")) {
                        path = path.substring(5); // Remove "file:"
                    }
                    // Extract path up to .jar
                    int jarIndex = path.indexOf(".jar");
                    if (jarIndex > 0) {
                        path = path.substring(0, jarIndex + 4); // Include .jar
                        File jarFile = new File(path);
                        if (jarFile.exists()) {
                            jarPath = jarFile.getAbsolutePath();
                            System.out.println("Extracted JAR path from code source: " + jarPath);
                        }
                    }
                }
                
                // Method 2: Check system property (works when launched with java -jar)
                if (jarPath == null) {
                    String javaCommand = System.getProperty("sun.java.command");
                    System.out.println("sun.java.command: " + javaCommand);
                    if (javaCommand != null) {
                        String[] parts = javaCommand.split("\\s+");
                        for (String part : parts) {
                            if (part.endsWith(".jar")) {
                                File jarFile = new File(part);
                                if (!jarFile.isAbsolute()) {
                                    jarFile = new File(System.getProperty("user.dir"), part);
                                }
                                if (jarFile.exists()) {
                                    jarPath = jarFile.getAbsolutePath();
                                    System.out.println("Found JAR from sun.java.command: " + jarPath);
                                    break;
                                }
                            }
                        }
                    }
                }
                
                // Method 3: If not found, try to find .jar file in current directory
                if (jarPath == null) {
                    File currentDir = new File(System.getProperty("user.dir"));
                    File[] jarFiles = currentDir.listFiles((dir, name) -> name.endsWith(".jar"));
                    if (jarFiles != null && jarFiles.length > 0) {
                        jarPath = jarFiles[0].getAbsolutePath();
                        System.out.println("Found JAR in current directory: " + jarPath);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error finding JAR path: " + e.getMessage());
                e.printStackTrace();
            }

            if (jarPath != null) {
                // Running from fat JAR. Extract BOOT-INF/lib/*.jar and BOOT-INF/classes/ entries to temp dir.
                try {
                    System.out.println("Opening JAR file: " + jarPath);
                    File tmp = Files.createTempDirectory("runtest-deps").toFile();
                    File classesDir = new File(tmp, "classes");
                    classesDir.mkdirs();
                    tmp.deleteOnExit();
                    classesDir.deleteOnExit();
                    
                    java.util.jar.JarFile jf = new java.util.jar.JarFile(jarPath);
                    try {
                        var entries = jf.entries();
                        int extractedJarCount = 0;
                        int extractedClassCount = 0;
                        
                        while (entries.hasMoreElements()) {
                            var e = entries.nextElement();
                            String name = e.getName();
                            
                            // Extract dependency JARs from BOOT-INF/lib/
                            if (name.startsWith("BOOT-INF/lib/") && name.endsWith(".jar")) {
                                System.out.println("Extracting dependency: " + name);
                                var is = jf.getInputStream(e);
                                File out = new File(tmp, new File(name).getName());
                                try (var fos = Files.newOutputStream(out.toPath())) {
                                    is.transferTo(fos);
                                }
                                classpathEntries.add(out.getAbsolutePath());
                                extractedJarCount++;
                            }
                            // Extract application classes from BOOT-INF/classes/
                            else if (name.startsWith("BOOT-INF/classes/") && !e.isDirectory()) {
                                String relativePath = name.substring("BOOT-INF/classes/".length());
                                File outFile = new File(classesDir, relativePath);
                                outFile.getParentFile().mkdirs();
                                
                                var is = jf.getInputStream(e);
                                try (var fos = Files.newOutputStream(outFile.toPath())) {
                                    is.transferTo(fos);
                                }
                                extractedClassCount++;
                            }
                        }
                        
                        System.out.println("Extracted " + extractedJarCount + " dependency JARs to: " + tmp.getAbsolutePath());
                        System.out.println("Extracted " + extractedClassCount + " application class files to: " + classesDir.getAbsolutePath());
                        
                        // Add the extracted classes directory to classpath (before everything else so it takes precedence)
                        classpathEntries.add(0, classesDir.getAbsolutePath());
                    } finally {
                        try { jf.close(); } catch (Exception ex) { /* ignore */ }
                    }
                } catch (Exception ex) {
                    System.out.println("ERROR: Failed to extract BOOT-INF contents: " + ex.getMessage());
                    ex.printStackTrace();
                    // Fall back to system classpath
                    classpathEntries.add(System.getProperty("java.class.path"));
                }
            } else {
                System.out.println("Not running from JAR - using system classpath");
                // Development mode - include current system classpath
                classpathEntries.add(System.getProperty("java.class.path"));
            }

            String classpath = String.join(File.pathSeparator, classpathEntries.stream()
                    .filter(Objects::nonNull).collect(Collectors.toList()));

            System.out.println("Full classpath entries: ");
            for (String cp : classpathEntries) {
                System.out.println("  cp: " + cp);
            }

            List<String> options = List.of(
                    "-classpath", classpath,
                    "-encoding", "UTF-8",
                    "-source", "17",
                    "-target", "17"
            );

            System.out.println("Compiler options: " + options);

            Iterable<? extends JavaFileObject> units =
                    fm.getJavaFileObjectsFromFiles(javaFiles);

            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fm, diagnostics, options, null, units);

            System.out.println("Starting compilation task...");
            boolean ok = task.call();
            System.out.println("Compilation result: " + (ok ? "SUCCESS" : "FAILED"));
            fm.close();

            if (!ok) {
                System.out.println("Compilation diagnostics:");
                StringBuilder sb = new StringBuilder("Compilation failed:\n");
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    String msg = "[" + d.getKind() + "] " +
                            d.getSource() + ":" +
                            d.getLineNumber() + " -> " +
                            d.getMessage(null) + "\n";
                    System.out.println("  " + msg);
                    sb.append(msg);
                }
                System.out.println("=== RuntimeTestCompiler.compileAllTests() END (FAILED) ===");
                return new CompileResult(false, sb.toString());
            }

            System.out.println("=== RuntimeTestCompiler.compileAllTests() END (SUCCESS) ===");
            return new CompileResult(true,
                    "Compilation successful (" + javaFiles.size() + " files).");

        } catch (Exception e) {
            System.out.println("=== RuntimeTestCompiler.compileAllTests() EXCEPTION ===");
            System.out.println("Exception: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();
            return new CompileResult(false,
                    "Compiler error: " + e.getMessage());
        }
    }

    // =====================================================
    // LOAD COMPILED TEST CLASSES
    // =====================================================
    public static ClassLoader buildTestClassLoader(File projectDir, ClassLoader parent)
            throws Exception {
        ClassLoader cl = parent != null ? parent : Thread.currentThread().getContextClassLoader();

        List<URL> urls = new ArrayList<>();

        // Always add the freshly compiled test output and main output dirs (created during compileAllTests)
        File testClasses = new File(projectDir, "target/test-classes");
        File mainClasses = new File(projectDir, "target/classes");
        if (testClasses.exists()) {
            urls.add(testClasses.toURI().toURL());
        }
        if (mainClasses.exists()) {
            urls.add(mainClasses.toURI().toURL());
        }

        // Resolve the running fat JAR path (same logic as compileAllTests)
        String jarPath = null;
        try {
            String codeSource = RuntimeTestCompiler.class.getProtectionDomain()
                    .getCodeSource().getLocation().toString();
            if (codeSource.startsWith("nested:")) {
                codeSource = codeSource.substring(7);
            }
            if (codeSource.startsWith("file:")) {
                codeSource = codeSource.substring(5);
            }
            int jarIndex = codeSource.indexOf(".jar");
            if (jarIndex > 0) {
                File jf = new File(codeSource.substring(0, jarIndex + 4));
                if (jf.exists()) {
                    jarPath = jf.getAbsolutePath();
                }
            }

            if (jarPath == null) {
                String javaCommand = System.getProperty("sun.java.command");
                if (javaCommand != null) {
                    for (String part : javaCommand.split("\\s+")) {
                        if (part.endsWith(".jar")) {
                            File jf = new File(part);
                            if (!jf.isAbsolute()) {
                                jf = new File(System.getProperty("user.dir"), part);
                            }
                            if (jf.exists()) {
                                jarPath = jf.getAbsolutePath();
                                break;
                            }
                        }
                    }
                }
            }

            if (jarPath == null) {
                File currentDir = new File(System.getProperty("user.dir"));
                File[] jarFiles = currentDir.listFiles((dir, name) -> name.endsWith(".jar"));
                if (jarFiles != null && jarFiles.length > 0) {
                    jarPath = jarFiles[0].getAbsolutePath();
                }
            }
        } catch (Exception ignored) {
        }

        // If running from fat JAR, extract BOOT-INF/lib and BOOT-INF/classes to temp and add to URLClassLoader
        if (jarPath != null) {
            File tmp = Files.createTempDirectory("runtest-loader").toFile();
            File classesDir = new File(tmp, "classes");
            classesDir.mkdirs();
            tmp.deleteOnExit();
            classesDir.deleteOnExit();

            java.util.jar.JarFile jf = new java.util.jar.JarFile(jarPath);
            try {
                var entries = jf.entries();
                while (entries.hasMoreElements()) {
                    var e = entries.nextElement();
                    String name = e.getName();
                    if (name.startsWith("BOOT-INF/lib/") && name.endsWith(".jar")) {
                        var is = jf.getInputStream(e);
                        File out = new File(tmp, new File(name).getName());
                        try (var fos = Files.newOutputStream(out.toPath())) {
                            is.transferTo(fos);
                        }
                        urls.add(out.toURI().toURL());
                    } else if (name.startsWith("BOOT-INF/classes/") && !e.isDirectory()) {
                        String relativePath = name.substring("BOOT-INF/classes/".length());
                        File outFile = new File(classesDir, relativePath);
                        outFile.getParentFile().mkdirs();
                        var is = jf.getInputStream(e);
                        try (var fos = Files.newOutputStream(outFile.toPath())) {
                            is.transferTo(fos);
                        }
                    }
                }
            } finally {
                try { jf.close(); } catch (Exception ignored) {}
            }

            // Add extracted classes directory first, then the fat JAR itself
            urls.add(0, classesDir.toURI().toURL());
            urls.add(new File(jarPath).toURI().toURL());
        }

        if (urls.isEmpty()) {
            // Fallback to parent if nothing was added
            return cl;
        }

        return new URLClassLoader(urls.toArray(new URL[0]), parent);
    }
}