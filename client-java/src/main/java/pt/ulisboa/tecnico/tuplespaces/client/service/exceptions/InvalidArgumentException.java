package pt.ulisboa.tecnico.tuplespaces.client.service.exceptions;

import io.grpc.StatusRuntimeException;

public class InvalidArgumentException extends ClientServiceException {
    public InvalidArgumentException(StatusRuntimeException grpcException) {
        super("Invalid request parameters. Please check the inputs. (Details: "
              + grpcException.getStatus().getDescription() + ")", grpcException);
    }
}
