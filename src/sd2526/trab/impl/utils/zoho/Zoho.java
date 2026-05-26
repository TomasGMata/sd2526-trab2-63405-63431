package sd2526.trab.impl.utils.zoho;

import java.util.List;
import java.util.logging.Logger;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;

import sd2526.trab.impl.utils.zoho.msgs.ZohoAccount;
import sd2526.trab.impl.utils.zoho.msgs.ZohoAccountReply;
import sd2526.trab.impl.utils.zoho.records.ZohoMessage;
import sd2526.trab.impl.utils.zoho.records.ZohoMessageContentReply;
import sd2526.trab.impl.utils.zoho.records.ZohoMessageListReply;

public class Zoho {

    private static final Logger Log = Logger.getLogger(Zoho.class.getName());
    private static final Gson gson = new Gson();

    static final String MAIL_API_BASE = "https://mail.zoho.eu/api";
    static final String CLIENT_ID     = "1000.G3HZ1EHEYR7C5USO9WX9YLGT3ZNILM";
    static final String CLIENT_SECRET = "54bef89b05bbbc4b2120ebec1b7cd1572d2d24f081";
    static final String REFRESH_TOKEN = "1000.c3fe895ba705017265c77d02b64b4ffe.070a195fe8d053c30d61da8f4239cdb0";

    private static final String ACCOUNTS = "/accounts";

    final OAuth20Service service;
    final ZohoTokenManager tokenManager;

    private String cachedAccountId;
    private String cachedFromAddress;

    static Zoho instance;

    private Zoho() {
        service      = ZohoServiceFactory.buildService(CLIENT_ID, CLIENT_SECRET);
        tokenManager = new ZohoTokenManager(service, REFRESH_TOKEN);
    }

    public synchronized static Zoho getInstance() {
        if (instance == null) instance = new Zoho();
        return instance;
    }

    public ZohoAccount getAccount() throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());
        OAuthRequest request = new OAuthRequest(Verb.GET, MAIL_API_BASE + ACCOUNTS);
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (response.isSuccessful()) {
                var data = gson.fromJson(response.getBody(), ZohoAccountReply.class).data();
                if (data == null || data.isEmpty()) return null;
                return data.get(0);
            } else {
                Log.warning(response.getCode() + "/" + response.getBody());
                return null;
            }
        }
    }

    private synchronized String getAccountId() throws Exception {
        if (cachedAccountId == null) {
            var acc = getAccount();
            if (acc == null) throw new RuntimeException("Cannot get Zoho account");
            cachedAccountId  = acc.accountId();
            cachedFromAddress = acc.primaryEmailAddress();
        }
        return cachedAccountId;
    }

    private synchronized String getFromAddress() throws Exception {
        getAccountId(); // garante que cachedFromAddress está preenchido
        return cachedFromAddress;
    }

    public boolean sendMessage(String subject, String bodyJson) throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());
        String accountId   = getAccountId();
        String fromAddress = getFromAddress();

        OAuthRequest request = new OAuthRequest(Verb.POST,
            MAIL_API_BASE + "/accounts/" + accountId + "/messages");
        request.addHeader("Content-Type", "application/json");

        String payload = gson.toJson(new java.util.HashMap<String, Object>() {{
            put("fromAddress", fromAddress);
            put("toAddress",   fromAddress);
            put("subject",     subject);
            put("content",     bodyJson);
            put("mailFormat",  "plaintext");
        }});
        request.setPayload(payload);
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (!response.isSuccessful())
                Log.warning("sendMessage failed: " + response.getCode() + "/" + response.getBody());
            return response.isSuccessful();
        }
    }
    
    public List<ZohoMessage> listMessages() throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());
        String accountId = getAccountId();

        OAuthRequest request = new OAuthRequest(Verb.GET,
            MAIL_API_BASE + "/accounts/" + accountId + "/messages/view");
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (!response.isSuccessful()) {
                Log.warning("listMessages failed: " + response.getCode() + "/" + response.getBody());
                return List.of();
            }
            var reply = gson.fromJson(response.getBody(), ZohoMessageListReply.class);
            if (reply == null || reply.data() == null) return List.of();
            return reply.data();  // devolve diretamente, sem loop
        }
    }

    public String getMessageContent(String folderId, String messageId) throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());
        String accountId = getAccountId();

        OAuthRequest request = new OAuthRequest(Verb.GET,
            MAIL_API_BASE + "/accounts/" + accountId
            + "/folders/" + folderId
            + "/messages/" + messageId + "/content");
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (!response.isSuccessful()) {
                Log.warning("getMessageContent failed: " + response.getCode() + "/" + response.getBody());
                return null;
            }
            var reply = gson.fromJson(response.getBody(), ZohoMessageContentReply.class);
            if (reply == null || reply.data() == null) return null;
            return reply.data().content();
        }
    }

    public boolean deleteMessage(String folderId, String messageId) throws Exception {
        var accessToken = new OAuth2AccessToken(tokenManager.getValidAccessToken());
        String accountId = getAccountId();

        OAuthRequest request = new OAuthRequest(Verb.DELETE,
            MAIL_API_BASE + "/accounts/" + accountId
            + "/folders/" + folderId
            + "/messages/" + messageId);
        service.signRequest(accessToken, request);

        try (Response response = service.execute(request)) {
            if (!response.isSuccessful())
                Log.warning("deleteMessage failed: " + response.getCode() + "/" + response.getBody());
            return response.isSuccessful();
        }
    }
}