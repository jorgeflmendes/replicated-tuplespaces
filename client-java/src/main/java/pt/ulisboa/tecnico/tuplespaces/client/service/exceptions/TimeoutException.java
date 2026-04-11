package pt.ulisboa.tecnico.tuplespaces.client.service.exceptions;

import io.grpc.StatusRuntimeException;

public class TimeoutException extends ClientServiceException {
    public TimeoutException(StatusRuntimeException grpcException) {
        super("Request timeout. Please try again later. (Details: "
              + grpcException.getStatus().getDescription() + ")", grpcException);
    }
}
