package pt.ulisboa.tecnico.tuplespaces.client.service.exceptions;

import io.grpc.StatusRuntimeException;

public class InternalServerErrorException extends ClientServiceException {
    public InternalServerErrorException(StatusRuntimeException grpcException) {
        super("Internal server error occurred. Please try again later. (Details: "
              + grpcException.getStatus().getDescription() + ")", grpcException);
    }
}
