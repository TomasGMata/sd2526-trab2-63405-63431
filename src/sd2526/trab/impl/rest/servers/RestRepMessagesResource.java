package sd2526.trab.impl.rest.servers;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.impl.api.java.AdminMessages;
import sd2526.trab.impl.api.rest.RestAdminMessages;
import sd2526.trab.impl.java.servers.JavaMessages;
import sd2526.trab.impl.utils.SyncPoint;
import sd2526.trab.impl.utils.kafka.KafkaPublisher;
import sd2526.trab.impl.utils.VersionHolder;
import sd2526.trab.impl.utils.VersionResponseFilter;

@Singleton
public class RestRepMessagesResource extends RestResource
        implements RestMessages, RestAdminMessages {

    static final String VERSION_HEADER = "X-MESSAGES-VERSION";
    private static final Gson gson = new Gson();

    @Context
    private HttpHeaders httpHeaders;

    private static KafkaPublisher publisher;

    private static synchronized KafkaPublisher getPublisher() {
        if (publisher == null)
            publisher = KafkaPublisher.createPublisher("kafka:9092");
        return publisher;
    }

    private Messages impl;

    synchronized Messages impl() {
        if (impl == null) impl = JavaMessages.getInstance();
        return impl;
    }

    // Lê versão mínima do header do cliente
    private long clientVersion() {
        String h = httpHeaders.getHeaderString(VERSION_HEADER);
        if (h == null) return -1;
        try { return Long.parseLong(h.trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    // Publica no Kafka e retorna offset
    private long publish(JsonObject obj) {
        return getPublisher().publish(RestRepMessagesServer.KAFKA_TOPIC, obj.toString());
    }

    @Override
    public String postMessage(String pwd, Message msg) {
        JsonObject obj = new JsonObject();
        obj.addProperty("op", "postMessage");
        obj.addProperty("pwd", pwd);
        obj.add("msg", gson.toJsonTree(msg));

        long offset = publish(obj);
        String result = SyncPoint.getSyncPoint().waitForResult(offset);

        VersionHolder.set(offset);
        if (result == null)
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        return result;
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        SyncPoint.getSyncPoint().waitForVersion(clientVersion());
        VersionHolder.set(SyncPoint.getSyncPoint().getVersion());
        return super.resultOrThrow(impl().getInboxMessage(name, mid, pwd));
    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {
        SyncPoint.getSyncPoint().waitForVersion(clientVersion());
        VersionHolder.set(SyncPoint.getSyncPoint().getVersion());
        if (query != null && !query.isEmpty())
            return super.resultOrThrow(impl().searchInbox(name, pwd, query));
        else
            return super.resultOrThrow(impl().getAllInboxMessages(name, pwd));
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {
        JsonObject obj = new JsonObject();
        obj.addProperty("op", "removeInboxMessage");
        obj.addProperty("name", name);
        obj.addProperty("mid", mid);
        obj.addProperty("pwd", pwd);

        long offset = publish(obj);
        String result = SyncPoint.getSyncPoint().waitForResult(offset);

        VersionHolder.set(offset);
        if (!"OK".equals(result))
            throw new WebApplicationException(
                result != null && result.equals("NOT_FOUND")
                    ? Response.Status.NOT_FOUND
                    : Response.Status.FORBIDDEN);
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        JsonObject obj = new JsonObject();
        obj.addProperty("op", "deleteMessage");
        obj.addProperty("name", name);
        obj.addProperty("mid", mid);
        obj.addProperty("pwd", pwd);

        long offset = publish(obj);
        String result = SyncPoint.getSyncPoint().waitForResult(offset);

        VersionHolder.set(offset);
        if (!"OK".equals(result))
            throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    @Override
    public void remotePostMessage(Message m) {
        super.resultOrThrow(((AdminMessages) impl()).remotePostMessage(m));
    }

    @Override
    public void remoteDeleteMessage(String mid) {
        super.resultOrThrow(((AdminMessages) impl()).remoteDeleteMessage(mid));
    }

    @Override
    public void remoteDeleteUserInbox(String name) {
        super.resultOrThrow(((AdminMessages) impl()).remoteDeleteUserInbox(name));
    }
}