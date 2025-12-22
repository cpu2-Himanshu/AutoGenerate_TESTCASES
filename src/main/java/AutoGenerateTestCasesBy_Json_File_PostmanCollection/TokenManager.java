package AutoGenerateTestCasesBy_Json_File_PostmanCollection;

public class TokenManager {

    private static final String TOKEN_KEY = "GLOBAL_API_TOKEN";

    private TokenManager() {}

    // ==============================
    // STORE TOKEN (EXACT VALUE ONLY)
    // ==============================
    public static void setToken(String authToken) {

        if (authToken == null) {
            throw new IllegalArgumentException("Token is null");
        }

        // 🔥 STRICT CLEANING (NO Bearer, NO newline, NO spaces)
        String cleanedToken = authToken
                .replace("\"", "")   // remove quotes
                .replace("\n", "")   // remove newline
                .replace("\r", "")   // remove carriage return
                .trim();              // remove spaces

        if (cleanedToken.isEmpty()) {
            throw new IllegalArgumentException("Token is empty after cleaning");
        }

        // 🔥 STORE EXACT TOKEN
        System.setProperty(TOKEN_KEY, cleanedToken);
//
        // ✅ DEBUG (length will now be EXACT)
//        System.out.println(
//            "[TokenManager][SET] Token stored | value=[" +
//            cleanedToken + "] | length=" + cleanedToken.length()
//        );
    }

    // ==============================
    // READ TOKEN (NO MODIFICATION)
    // ==============================
    public static String getToken() {

        String token = System.getProperty(TOKEN_KEY);

        if (token == null || token.isEmpty()) {
            throw new IllegalStateException(
                "Authorization token NOT received from UI"
            );
        }

     // ✅ DEBUG (print only token length)
        System.out.println(
            "[TokenManager][GET] Token accessed | length=" +
            (token == null ? "null" : token.length())
        );

        return token;
    }

    // ==============================
    // CLEAR TOKEN
    // ==============================
    public static void clear() {
        System.clearProperty(TOKEN_KEY);
     //   System.out.println("[TokenManager] Token cleared");
    }
}
