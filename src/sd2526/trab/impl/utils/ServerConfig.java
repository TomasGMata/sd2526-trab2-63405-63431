package sd2526.trab.impl.utils;

public class ServerConfig {

    public static final String SECRET_HEADER = "X-Server-Secret";
    
    private static String serverSecret = "";

    public static void setSecret(String secret) {
        serverSecret = secret == null ? "" : secret;
    }

    public static String getSecret() {
        return serverSecret;
    }

    public static boolean isValidSecret(String secret) {
        return serverSecret.equals(secret);
    }
}