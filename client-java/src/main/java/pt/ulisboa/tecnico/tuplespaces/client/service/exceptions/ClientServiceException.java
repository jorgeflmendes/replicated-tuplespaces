package pt.ulisboa.tecnico.tuplespaces.client.service.exceptions;

import io.grpc.StatusRuntimeException;

public class ClientServiceException extends Exception {
    private final StatusRuntimeException grpcException;

    public ClientServiceException(String message, StatusRuntimeException grpcException) {
        super(message);
        this.grpcException = grpcException;
    }

    public StatusRuntimeException getGrpcException() {
        return grpcException;
    }
}
