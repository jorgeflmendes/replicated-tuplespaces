package pt.ulisboa.tecnico.tuplespaces.client.service.exceptions;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class GrpcExceptionHandler {
    public static ClientServiceException handleGrpcException(StatusRuntimeException exception) {
        Status.Code statusCode = exception.getStatus().getCode();

        switch (statusCode) {
            case UNAVAILABLE:
                return new ServiceUnavailableException(exception);
            case INVALID_ARGUMENT:
                return new InvalidArgumentException(exception);
            case PERMISSION_DENIED:
                return new PermissionDeniedException(exception);
            case DEADLINE_EXCEEDED:
                return new TimeoutException(exception);
            case INTERNAL:
                return new InternalServerErrorException(exception);
            default:
                return new ClientServiceException("An error occurred with status: " + statusCode, exception);
        }
    }

}
