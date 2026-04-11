package pt.ulisboa.tecnico.tuplespaces.client.service;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.client.service.exceptions.ClientServiceException;
import pt.ulisboa.tecnico.tuplespaces.client.service.exceptions.GrpcExceptionHandler;

import io.grpc.Metadata;

public class ClientService {
    private final ManagedChannel channel;
    private final TupleSpacesGrpc.TupleSpacesBlockingStub stub;
    private final int clientId;
    private final boolean debugMode;

    static final Metadata.Key<String> DELAY_1 = Metadata.Key.of("delay1", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> DELAY_2 = Metadata.Key.of("delay2", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> DELAY_3 = Metadata.Key.of("delay3", Metadata.ASCII_STRING_MARSHALLER);

    public ClientService(String hostPort, int clientId, boolean debugMode) {
        this.channel = ManagedChannelBuilder.forTarget(hostPort).usePlaintext().build();
        this.clientId = clientId;
        this.debugMode = debugMode;
        this.stub = TupleSpacesGrpc.newBlockingStub(channel);
    }

    public void put(String tuple, Integer delay1, Integer delay2, Integer delay3) throws ClientServiceException {
        Metadata metadata = createMetadata(delay1, delay2, delay3);
        TupleSpacesGrpc.TupleSpacesBlockingStub stubWithMeta = this.stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor((metadata)));

        try {
            debug("Sending put request with tuple " + tuple + " to the server...");
            PutRequest request = PutRequest.newBuilder()
                    .setClientId(this.clientId)
                    .setNewTuple(tuple).build();

            PutResponse put = stubWithMeta.put(request);
            debug("Received put response for the put request with tuple " + tuple);
        } catch (StatusRuntimeException e) {
            // map the low-level gRPC exception into a more understandable exception for the CommandProcessor
            System.err.println("Server sent an error (" + e.getMessage() + "), wrapping it in a ClientServiceException...");
            throw GrpcExceptionHandler.handleGrpcException(e);
        }
    }

    public String read(String pattern, Integer delay1, Integer delay2, Integer delay3) throws ClientServiceException {
        Metadata metadata = createMetadata(delay1, delay2, delay3);
        TupleSpacesGrpc.TupleSpacesBlockingStub stubWithMeta = this.stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor((metadata)));

        try {
            debug("Sending read request with pattern " + pattern + " to the server...");
            ReadRequest request = ReadRequest.newBuilder()
                    .setClientId(this.clientId)
                    .setSearchPattern(pattern).build();

            ReadResponse response = stubWithMeta.read(request);
            debug("Received read response for the read request with pattern " + pattern);

            return response.getResult();
        } catch (StatusRuntimeException e) {
            // map the low-level gRPC exception into a more understandable exception for the CommandProcessor
            System.err.println("Server sent an error (" + e.getMessage() + "), wrapping it in a ClientServiceException...");
            throw GrpcExceptionHandler.handleGrpcException(e);
        }
    }

    public String take(String pattern, Integer delay1, Integer delay2, Integer delay3) throws ClientServiceException {
        Metadata metadata = createMetadata(delay1, delay2, delay3);
        TupleSpacesGrpc.TupleSpacesBlockingStub stubWithMeta = this.stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor((metadata)));

        try {
            debug("Sending take request with pattern " + pattern + " to the server...");
            TakeRequest request = TakeRequest.newBuilder().setPattern(pattern)
                    .setClientId(this.clientId).build();

            TakeResponse response = stubWithMeta.take(request);
            debug("Received take response for the take request with pattern " + pattern);
            return response.getResult();
        } catch (StatusRuntimeException e) {
            // map the low-level gRPC exception into a more understandable exception for the CommandProcessor
            System.err.println("Server sent an error (" + e.getMessage() + "), wrapping it in a ClientServiceException...");
            throw GrpcExceptionHandler.handleGrpcException(e);
        }
    }

    public String getTupleSpacesState() throws ClientServiceException {
        try {
            debug("Sending getTupleSpacesState request to the server...");
            getTupleSpacesStateRequest request = getTupleSpacesStateRequest.newBuilder()
                    .setClientId(this.clientId)
                    .build();

            getTupleSpacesStateResponse response = this.stub.getTupleSpacesState(request);
            debug("Received getTupleSpacesState response with tuple spaces state.");
            return response.getTupleList().toString();
        } catch (StatusRuntimeException e) {
            // map the low-level gRPC exception into a more understandable exception for the CommandProcessor
            System.err.println("Server sent an error (" + e.getMessage() + "), wrapping it in a ClientServiceException...");
            throw GrpcExceptionHandler.handleGrpcException(e);
        }
    }

    public void terminate() {
        if (channel != null) {
            channel.shutdown();
        }
    }

    private void debug(String message) {
        if (this.debugMode) {
            System.err.println("[DEBUG] " + message);
        }
    }

    private Metadata createMetadata(Integer delay1, Integer delay2, Integer delay3) {
        Metadata metadata = new Metadata();

        // store delay values as strings in the metadata
        metadata.put(DELAY_1, delay1.toString());
        metadata.put(DELAY_2, delay2.toString());
        metadata.put(DELAY_3, delay3.toString());

        return metadata;
    }
}

