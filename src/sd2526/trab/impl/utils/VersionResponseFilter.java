package sd2526.trab.impl.utils;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class VersionResponseFilter implements ContainerResponseFilter {
    
    public static final String VERSION_HEADER = "X-MESSAGES-VERSION";

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext resp) {
        Long v = VersionHolder.get();
        if (v >= 0)
            resp.getHeaders().add(VERSION_HEADER, String.valueOf(v));
    }
}