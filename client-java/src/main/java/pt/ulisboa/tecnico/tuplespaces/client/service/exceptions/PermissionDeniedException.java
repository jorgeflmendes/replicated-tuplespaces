package pt.ulisboa.tecnico.tuplespaces.client.service.exceptions;

import io.grpc.StatusRuntimeException;

public class PermissionDeniedException extends ClientServiceException {
    public PermissionDeniedException(StatusRuntimeException grpcException) {
        super("You do not have permission to access this resource. (Details: "
              + grpcException.getStatus().getDescription() + ")", grpcException);
    }
}
