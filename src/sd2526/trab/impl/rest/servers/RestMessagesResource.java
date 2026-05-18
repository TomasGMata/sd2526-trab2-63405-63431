package sd2526.trab.impl.rest.servers;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import jakarta.ws.rs.core.Context;
import sd2526.trab.api.Message;
import sd2526.trab.api.java.Messages;
import sd2526.trab.api.rest.RestMessages;
import sd2526.trab.impl.api.java.AdminMessages;
import sd2526.trab.impl.api.rest.RestAdminMessages;
import sd2526.trab.impl.java.clients.Clients;
import sd2526.trab.impl.java.servers.JavaMessages;
import sd2526.trab.impl.utils.ServerConfig;
import sd2526.trab.impl.utils.SyncPoint;
import sd2526.trab.impl.utils.kafka.KafkaPublisher;

public class RestMessagesResource extends RestResource implements RestMessages, RestAdminMessages {

    @Context
    private jakarta.ws.rs.core.HttpHeaders httpHeaders;

    private void validateSecret() {
        String secret = httpHeaders.getHeaderString(ServerConfig.SECRET_HEADER);
        if (!ServerConfig.isValidSecret(secret))
            throw new jakarta.ws.rs.WebApplicationException(
                jakarta.ws.rs.core.Response.Status.FORBIDDEN);
    }

    private static KafkaPublisher publisher;
    private static final Gson gson = new Gson();

    static boolean isGateway = false;
    Messages impl;

    public RestMessagesResource() {}

    synchronized Messages impl() {
        if (impl == null)
            impl = isGateway ? Clients.MessagesClient.get() : JavaMessages.getInstance();
        return impl;
    }

    @Override
    public String postMessage(String pwd, Message msg) {
        if (isGateway || !RestMessagesServer.kafkaAvailable) {
            return super.resultOrThrow(impl().postMessage(pwd, msg));
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("op", "postMessage");
        obj.addProperty("pwd", pwd);
        obj.add("msg", gson.toJsonTree(msg));

        long offset = getPublisher().publish(RestMessagesServer.KAFKA_TOPIC, obj.toString());
        return SyncPoint.getSyncPoint().waitForResult(offset);
    }

    @Override
    public Message getMessage(String name, String mid, String pwd) {
        return super.resultOrThrow(impl().getInboxMessage(name, mid, pwd));
    }

    @Override
    public List<String> getMessages(String name, String pwd, String query) {
        if (query != null && !query.isEmpty())
            return super.resultOrThrow(impl().searchInbox(name, pwd, query));
        else
            return super.resultOrThrow(impl().getAllInboxMessages(name, pwd));
    }

    @Override
    public void removeFromUserInbox(String name, String mid, String pwd) {
        super.resultOrThrow(impl().removeInboxMessage(name, mid, pwd));
    }

    @Override
    public void deleteMessage(String name, String mid, String pwd) {
        if (isGateway || !RestMessagesServer.kafkaAvailable) {
            super.resultOrThrow(impl().deleteMessage(name, mid, pwd));
            return;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("op", "deleteMessage");
        obj.addProperty("pwd", pwd);
        obj.addProperty("mid", mid);
        obj.addProperty("name", name);

        long offset = getPublisher().publish(RestMessagesServer.KAFKA_TOPIC, obj.toString());
        String res = SyncPoint.getSyncPoint().waitForResult(offset);

        if (!"OK".equals(res))
            throw new jakarta.ws.rs.WebApplicationException(
                jakarta.ws.rs.core.Response.Status.FORBIDDEN);
    }

    @Override
    public void remotePostMessage(Message m) {
        validateSecret(); 
        super.resultOrThrow(((AdminMessages) impl()).remotePostMessage(m));
    }

    @Override
    public void remoteDeleteMessage(String mid) {
        validateSecret(); 
        super.resultOrThrow(((AdminMessages) impl()).remoteDeleteMessage(mid));
    }

    @Override
    public void remoteDeleteUserInbox(String name) {
        validateSecret(); 
        super.resultOrThrow(((AdminMessages) impl()).remoteDeleteUserInbox(name));
    }

    private static KafkaPublisher getPublisher() {
        if (publisher == null)
            publisher = KafkaPublisher.createPublisher("kafka:9092");
        return publisher;
    }
}