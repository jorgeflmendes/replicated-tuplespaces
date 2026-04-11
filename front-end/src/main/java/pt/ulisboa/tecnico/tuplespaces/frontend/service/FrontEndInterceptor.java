package pt.ulisboa.tecnico.tuplespaces.frontend.service;

import io.grpc.*;

public class FrontEndInterceptor implements ServerInterceptor {
    // metadata keys for different delays
    static final Metadata.Key<String> DELAY_1 = Metadata.Key.of("delay1", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> DELAY_2 = Metadata.Key.of("delay2", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> DELAY_3 = Metadata.Key.of("delay3", Metadata.ASCII_STRING_MARSHALLER);

    // context keys for storing delay values
    static final Context.Key<String> CONTEXT_DELAY_1 = Context.key("context-delay1");
    static final Context.Key<String> CONTEXT_DELAY_2 = Context.key("context-delay2");
    static final Context.Key<String> CONTEXT_DELAY_3 = Context.key("context-delay3");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            final Metadata requestHeaders,
            ServerCallHandler<ReqT, RespT> next) {

        // retrieving delay values from request headers, defaulting to "0" if null
        String delay1 = requestHeaders.get(DELAY_1);
        String delay2 = requestHeaders.get(DELAY_2);
        String delay3 = requestHeaders.get(DELAY_3);

        if (delay1 == null) delay1 = "0";
        if (delay2 == null) delay2 = "0";
        if (delay3 == null) delay3 = "0";

        // initializing the context with delay values
        Context context = Context.current()
                .withValue(CONTEXT_DELAY_1, delay1)
                .withValue(CONTEXT_DELAY_2, delay2)
                .withValue(CONTEXT_DELAY_3, delay3);

        // intercepting the call with updated context
        return Contexts.interceptCall(context, call, requestHeaders, next);
    }
}
