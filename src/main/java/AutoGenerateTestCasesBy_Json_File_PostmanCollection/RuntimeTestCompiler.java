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
            // 🔥 HARD CHECK: MUST RUN ON JDK
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException(
                        "JavaCompiler not available. Application must run with JDK (javac), not JRE."
                );
            }

            File testSrc = new File(projectDir, "src/test/java");
            File outDir = new File(projectDir, "target/test-classes");
            File mainOut = new File(projectDir, "target/classes");

            outDir.mkdirs();
            mainOut.mkdirs();

            if (!testSrc.exists()) {
                return new CompileResult(false,
                        "Test source folder not found: " + testSrc.getAbsolutePath());
            }

            List<File> javaFiles;
            try (var stream = Files.walk(testSrc.toPath())) {
                javaFiles = stream
                        .filter(p -> p.toString().endsWith(".java"))
                        .map(p -> p.toFile())
                        .collect(Collectors.toList());
            }

            if (javaFiles.isEmpty()) {
                return new CompileResult(false,
                        "No test .java files found under " + testSrc.getAbsolutePath());
            }

            DiagnosticCollector<JavaFileObject> diagnostics =
                    new DiagnosticCollector<>();

            StandardJavaFileManager fm =
                    compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8);

            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outDir));

            // 🔥 BUILD COMPLETE CLASSPATH (THIS IS THE KEY FIX)
            String classpath =
                    System.getProperty("java.class.path")
                            + File.pathSeparator + mainOut.getAbsolutePath()
                            + File.pathSeparator + outDir.getAbsolutePath();

            List<String> options = List.of(
                    "-classpath", classpath,
                    "-encoding", "UTF-8",
                    "-source", "17",
                    "-target", "17"
            );

            Iterable<? extends JavaFileObject> units =
                    fm.getJavaFileObjectsFromFiles(javaFiles);

            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fm, diagnostics, options, null, units);

            boolean ok = task.call();
            fm.close();

            if (!ok) {
                StringBuilder sb = new StringBuilder("Compilation failed:\n");
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    sb.append("[").append(d.getKind()).append("] ")
                            .append(d.getSource()).append(":")
                            .append(d.getLineNumber()).append(" -> ")
                            .append(d.getMessage(null)).append("\n");
                }
                return new CompileResult(false, sb.toString());
            }

            return new CompileResult(true,
                    "Compilation successful (" + javaFiles.size() + " files).");

        } catch (Exception e) {
            return new CompileResult(false,
                    "Compiler error: " + e.getMessage());
        }
    }

    // =====================================================
    // LOAD COMPILED TEST CLASSES
    // =====================================================
    public static ClassLoader buildTestClassLoader(File projectDir, ClassLoader parent)
            throws Exception {

        File testClasses = new File(projectDir, "target/test-classes");
        File mainClasses = new File(projectDir, "target/classes");

        URL[] urls = new URL[]{
                testClasses.toURI().toURL(),
                mainClasses.toURI().toURL()
        };

        return new URLClassLoader(urls, parent);
    }
}
