package sd2526.trab.impl.java.servers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.google.gson.Gson;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.impl.utils.ServerConfig;
import sd2526.trab.impl.utils.zoho.Zoho;
import sd2526.trab.impl.utils.zoho.records.ZohoMessage;

public class JavaZohoMessages implements Messages {

    private static final Logger Log = Logger.getLogger(JavaZohoMessages.class.getName());
    private static final Gson gson = new Gson();
    private static final String DOMAIN = System.getenv().getOrDefault("DOMAIN", "ourorg2");
    private static final AtomicLong counter = new AtomicLong(0);

    private static JavaZohoMessages instance;

    public static synchronized JavaZohoMessages getInstance() {
        if (instance == null) instance = new JavaZohoMessages();
        return instance;
    }

    private JavaZohoMessages() {}

    private long generateId() {
        String digits = DOMAIN.replaceAll("[^0-9]", "");
        long prefix = digits.isEmpty() ? 2L : Long.parseLong(digits);
        return prefix * 100_000L + counter.incrementAndGet();
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        if (!ServerConfig.isValidSecret(pwd))
            return Result.error(Result.ErrorCode.FORBIDDEN);
        try {
            long id = generateId();
            msg.setId(String.valueOf(id));
            String json = gson.toJson(msg);
            boolean ok = Zoho.getInstance().sendMessage(String.valueOf(id), json);
            return ok ? Result.ok(String.valueOf(id)) : Result.error(Result.ErrorCode.INTERNAL_ERROR);
        } catch (Exception e) {
            Log.warning("postMessage error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> deleteMessage(String user, String mid, String pwd) {
        if (!ServerConfig.isValidSecret(pwd))
            return Result.error(Result.ErrorCode.FORBIDDEN);
        try {
            for (ZohoMessage email : Zoho.getInstance().listMessages()) {
                if (mid.equals(email.subject())) {
                    boolean ok = Zoho.getInstance().deleteMessage(email.folderId(), email.messageId());
                    return ok ? Result.ok() : Result.error(Result.ErrorCode.INTERNAL_ERROR);
                }
            }
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } catch (Exception e) {
            Log.warning("deleteMessage error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    public Result<Void> removeFromInbox(String user, String mid, String pwd) {
        return deleteMessage(user, mid, pwd);
    }

    
    public Result<Void> postToLocalInboxes(String secret, List<String> localRecipients, Message msg) {
        if (!ServerConfig.isValidSecret(secret))
            return Result.error(Result.ErrorCode.FORBIDDEN);
        try {
            String mid = msg.getId();
            String json = gson.toJson(msg);
            boolean ok = Zoho.getInstance().sendMessage(mid, json);
            return ok ? Result.ok() : Result.error(Result.ErrorCode.INTERNAL_ERROR);
        } catch (Exception e) {
            Log.warning("postToLocalInboxes error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String user, String pwd) {
        if (!ServerConfig.isValidSecret(pwd))
            return Result.error(Result.ErrorCode.FORBIDDEN);
        try {
            List<String> ids = new ArrayList<>();
            for (ZohoMessage email : Zoho.getInstance().listMessages()) {
                try {
                    Long.parseLong(email.subject());
                    ids.add(email.subject());
                } catch (NumberFormatException ignored) {}
            }
            return Result.ok(ids);
        } catch (Exception e) {
            Log.warning("getAllInboxMessages error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Message> getInboxMessage(String user, String mid, String pwd) {
        if (!ServerConfig.isValidSecret(pwd))
            return Result.error(Result.ErrorCode.FORBIDDEN);
        try {
            for (ZohoMessage email : Zoho.getInstance().listMessages()) {
                if (mid.equals(email.subject())) {
                    String content = Zoho.getInstance().getMessageContent(
                        email.folderId(), email.messageId());
                    if (content == null) return Result.error(Result.ErrorCode.INTERNAL_ERROR);
                    Message msg = gson.fromJson(content, Message.class);
                    return msg != null ? Result.ok(msg) : Result.error(Result.ErrorCode.INTERNAL_ERROR);
                }
            }
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } catch (Exception e) {
            Log.warning("getInboxMessage error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> removeInboxMessage(String user, String mid, String pwd) {
        return deleteMessage(user, mid, pwd);
    }

    @Override
    public Result<List<String>> searchInbox(String user, String pwd, String query) {
        if (!ServerConfig.isValidSecret(pwd))
            return Result.error(Result.ErrorCode.FORBIDDEN);
        try {
            List<String> ids = new ArrayList<>();
            for (ZohoMessage email : Zoho.getInstance().listMessages()) {
                if (email.subject() != null && email.subject().contains(query)) {
                    ids.add(email.subject());
                }
            }
            return Result.ok(ids);
        } catch (Exception e) {
            Log.warning("searchInbox error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }
}