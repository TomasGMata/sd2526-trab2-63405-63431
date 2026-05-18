package sd2526.trab.impl.rest.servers;

import sd2526.trab.api.java.Messages;
import sd2526.trab.impl.java.servers.JavaZohoMessages;

public class RestZohoMessagesResource extends RestMessagesResource {

    public static void setCleanState(boolean clean) {
        JavaZohoMessages.setCleanState(clean);
    }

    @Override
    synchronized Messages impl() {
        if (impl == null)
            impl = JavaZohoMessages.getInstance();
        return impl;
    }
}