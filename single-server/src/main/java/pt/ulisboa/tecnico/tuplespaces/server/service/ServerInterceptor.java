package pt.ulisboa.tecnico.tuplespaces.server.service;

import io.grpc.*;

public class ServerInterceptor implements io.grpc.ServerInterceptor {
    // metadata key for retrieving the delay value
    static final Metadata.Key<String> DELAY = Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);

    // context key for storing the extracted delay value
    static final Context.Key<String> CONTEXT_DELAY = Context.key("context-delay");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next) {

        // retrieving delay value from request headers, defaulting to "0" if null
        String delay = requestHeaders.get(DELAY);
        if (delay == null) delay = "0";

        // initializing the context with the delay value
        Context context = Context.current().withValue(CONTEXT_DELAY, delay);

        // intercepting the call with updated context
        return Contexts.interceptCall(context, call, requestHeaders, next);
    }
}
