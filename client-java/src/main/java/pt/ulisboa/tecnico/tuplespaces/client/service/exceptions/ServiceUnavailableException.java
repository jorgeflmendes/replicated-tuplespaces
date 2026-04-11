package pt.ulisboa.tecnico.tuplespaces.client.service.exceptions;

import io.grpc.StatusRuntimeException;

public class ServiceUnavailableException extends ClientServiceException {
    public ServiceUnavailableException(StatusRuntimeException grpcException) {
        super("Service is currently unavailable. Please try again later. (Details: "
              + grpcException.getStatus().getDescription() + ")", grpcException);
    }
}
