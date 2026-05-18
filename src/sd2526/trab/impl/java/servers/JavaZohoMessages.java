package sd2526.trab.impl.java.servers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.gson.Gson;

import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.java.Result;
import sd2526.trab.impl.api.java.AdminMessages;
import sd2526.trab.impl.java.clients.Clients;
import sd2526.trab.impl.utils.IP;
import sd2526.trab.impl.utils.zoho.Zoho;
import sd2526.trab.impl.utils.zoho.records.ZohoMessage;

public class JavaZohoMessages implements Messages, AdminMessages {

    private static final Logger Log = Logger.getLogger(JavaZohoMessages.class.getName());
    private static final Gson gson = new Gson();
    private static final String DOMAIN = System.getenv().getOrDefault("DOMAIN", "ourorg2");

    private static JavaZohoMessages instance;
    private static boolean cleanState = false;

    public static void setCleanState(boolean clean) {
        cleanState = clean;
    }

    public static synchronized JavaZohoMessages getInstance() {
        if (instance == null) instance = new JavaZohoMessages();
        return instance;
    }

    private JavaZohoMessages() {
        Log.info("JavaZohoMessages starting, DOMAIN=" + DOMAIN + ", cleanState=" + cleanState);
        if (cleanState) {
            try {
                Log.info("cleanState=true: clearing Zoho inbox...");
                for (ZohoMessage email : Zoho.getInstance().listMessages()) {
                    Log.info("Deleting existing Zoho message: subject=" + email.subject()
                            + ", folderId=" + email.folderId() + ", messageId=" + email.messageId());
                    Zoho.getInstance().deleteMessage(email.folderId(), email.messageId());
                }
                Log.info("Zoho inbox cleared.");
            } catch (Exception e) {
                Log.warning("Failed to clear Zoho inbox: " + e.getMessage());
            }
        }
    }

    private Result<Void> validateUser(String user, String pwd) {
        try {
            var name = user.contains("@") ? user.split("@")[0] : user;
            var domain = user.contains("@") ? user.split("@")[1] : IP.domain();
            Log.info("validateUser: user=" + user + ", resolvedName=" + name + ", resolvedDomain=" + domain);
            var result = Clients.UsersClient.get(domain).getUser(name, pwd);
            Log.info("validateUser result: " + result);
            return result.isOK() ? Result.ok() : Result.error(Result.ErrorCode.FORBIDDEN);
        } catch (Exception e) {
            Log.warning("validateUser error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private String decodeZohoContent(String content) {
        if (content == null)
            return null;

        String cleaned = content
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("(?i)<div[^>]*>", "")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)<p[^>]*>", "")
                .replaceAll("(?i)<[^>]+>", "")
                .replace("&quot;", "\"")
                .replace("&#34;", "\"")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&nbsp;", " ")
                .trim();

        int begin = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (begin >= 0 && end > begin)
            cleaned = cleaned.substring(begin, end + 1).trim();

        return cleaned;
    }

    private Message parseStoredMessage(String content) {
        String cleaned = decodeZohoContent(content);
        Log.info("Zoho parsed content candidate=" + cleaned);

        if (cleaned == null || cleaned.isBlank())
            return null;

        return gson.fromJson(cleaned, Message.class);
    }

    @Override
    public Result<String> postMessage(String pwd, Message msg) {
        Log.info("Zoho postMessage: sender=" + (msg == null ? null : msg.getSender())
                + ", destinations=" + (msg == null ? null : msg.getDestination())
                + ", id=" + (msg == null ? null : msg.getId()));

        if (msg == null || msg.getSender() == null || msg.getDestination() == null || msg.getDestination().isEmpty())
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        var check = validateUser(msg.getSender(), pwd);
        if (!check.isOK()) return Result.error(check.error());

        try {
            if (msg.getId() == null)
                msg.setId("%s+local".formatted(DOMAIN));

            Message copy = new Message(msg);
            String json = gson.toJson(copy);
            Log.info("Zoho postMessage sending: subject=" + copy.getId() + ", json=" + json);

            boolean ok = Zoho.getInstance().sendMessage(copy.getId(), json);
            Log.info("Zoho postMessage send result=" + ok);

            return ok ? Result.ok(copy.getId()) : Result.error(Result.ErrorCode.INTERNAL_ERROR);
        } catch (Exception e) {
            Log.warning("postMessage error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<List<String>> getAllInboxMessages(String user, String pwd) {
        var check = validateUser(user, pwd);
        if (!check.isOK()) return Result.error(check.error());

        try {
            List<String> ids = new ArrayList<>();
            for (ZohoMessage email : Zoho.getInstance().listMessages()) {
                Log.info("Zoho inbox entry: subject=" + email.subject()
                        + ", folderId=" + email.folderId()
                        + ", messageId=" + email.messageId());

                String subject = email.subject();
                if (subject != null && !subject.isBlank())
                    ids.add(subject);
            }
            Log.info("Zoho getAllInboxMessages returning ids=" + ids);
            return Result.ok(ids);
        } catch (Exception e) {
            Log.warning("getAllInboxMessages error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Message> getInboxMessage(String user, String mid, String pwd) {
        var check = validateUser(user, pwd);
        if (!check.isOK()) return Result.error(check.error());

        try {
            for (ZohoMessage email : Zoho.getInstance().listMessages()) {
                Log.info("Zoho getInboxMessage checking subject=" + email.subject() + " against mid=" + mid);
                if (mid.equals(email.subject())) {
                    String content = Zoho.getInstance().getMessageContent(email.folderId(), email.messageId());
                    Log.info("Zoho getInboxMessage raw content=" + content);

                    if (content == null)
                        return Result.error(Result.ErrorCode.INTERNAL_ERROR);

                    Message msg = parseStoredMessage(content);
                    Log.info("Zoho getInboxMessage parsed msg=" + msg);

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
    public Result<List<String>> searchInbox(String user, String pwd, String query) {
        var check = validateUser(user, pwd);
        if (!check.isOK()) return Result.error(check.error());

        try {
            String q = query == null ? "" : query.toUpperCase();
            List<String> ids = new ArrayList<>();

            for (ZohoMessage email : Zoho.getInstance().listMessages()) {
                String subject = email.subject();
                String content = Zoho.getInstance().getMessageContent(email.folderId(), email.messageId());

                boolean match = false;

                if (subject != null && subject.toUpperCase().contains(q))
                    match = true;

                if (!match && content != null) {
                    Message msg = null;
                    try {
                        msg = parseStoredMessage(content);
                    } catch (Exception ignored) {
                    }

                    if (msg != null) {
                        String mSubject = msg.getSubject();
                        String mContents = msg.getContents();
                        match = (mSubject != null && mSubject.toUpperCase().contains(q))
                                || (mContents != null && mContents.toUpperCase().contains(q));
                    }
                }

                if (match && subject != null && !subject.isBlank())
                    ids.add(subject);
            }

            Log.info("Zoho searchInbox query=" + query + ", result=" + ids);
            return Result.ok(ids);
        } catch (Exception e) {
            Log.warning("searchInbox error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> remotePostMessage(Message msg) {
        Log.info("Zoho remotePostMessage: id=" + (msg == null ? null : msg.getId())
                + ", sender=" + (msg == null ? null : msg.getSender())
                + ", destinations=" + (msg == null ? null : msg.getDestination()));

        if (msg == null || msg.getId() == null || msg.getDestination() == null || msg.getDestination().isEmpty())
            return Result.error(Result.ErrorCode.BAD_REQUEST);

        try {
            Message copy = new Message(msg);
            String json = gson.toJson(copy);
            Log.info("Zoho remotePostMessage sending: subject=" + copy.getId() + ", json=" + json);

            boolean ok = Zoho.getInstance().sendMessage(copy.getId(), json);
            Log.info("Zoho remotePostMessage send result=" + ok);

            return ok ? Result.ok() : Result.error(Result.ErrorCode.INTERNAL_ERROR);
        } catch (Exception e) {
            Log.warning("remotePostMessage error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> remoteDeleteMessage(String mid) {
        Log.info("Zoho remoteDeleteMessage: mid=" + mid);
        try {
            for (ZohoMessage email : Zoho.getInstance().listMessages()) {
                Log.info("Zoho remoteDeleteMessage checking subject=" + email.subject());
                if (mid.equals(email.subject())) {
                    boolean ok = Zoho.getInstance().deleteMessage(email.folderId(), email.messageId());
                    Log.info("Zoho remoteDeleteMessage delete result=" + ok);
                    return ok ? Result.ok() : Result.error(Result.ErrorCode.INTERNAL_ERROR);
                }
            }
            return Result.ok();
        } catch (Exception e) {
            Log.warning("remoteDeleteMessage error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> remoteDeleteUserInbox(String name) {
        Log.info("Zoho remoteDeleteUserInbox: name=" + name);
        try {
            for (ZohoMessage email : Zoho.getInstance().listMessages()) {
                Log.info("Zoho remoteDeleteUserInbox deleting subject=" + email.subject());
                Zoho.getInstance().deleteMessage(email.folderId(), email.messageId());
            }
            return Result.ok();
        } catch (Exception e) {
            Log.warning("remoteDeleteUserInbox error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> removeInboxMessage(String name, String mid, String pwd) {
        Log.info("Zoho removeInboxMessage: name=" + name + ", mid=" + mid);
        return deleteMessage(name, mid, pwd);
    }

    @Override
    public Result<Void> deleteMessage(String name, String mid, String pwd) {
        Log.info("Zoho deleteMessage: name=" + name + ", mid=" + mid);

        var check = validateUser(name, pwd);
        if (!check.isOK()) return Result.error(check.error());

        try {
            for (ZohoMessage email : Zoho.getInstance().listMessages()) {
                Log.info("Zoho deleteMessage checking subject=" + email.subject());
                if (mid.equals(email.subject())) {
                    boolean ok = Zoho.getInstance().deleteMessage(email.folderId(), email.messageId());
                    Log.info("Zoho deleteMessage result=" + ok);
                    return ok ? Result.ok() : Result.error(Result.ErrorCode.INTERNAL_ERROR);
                }
            }
            return Result.error(Result.ErrorCode.NOT_FOUND);
        } catch (Exception e) {
            Log.warning("deleteMessage error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }
}