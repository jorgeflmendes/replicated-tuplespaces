import grpc


class ClientServiceException(Exception):
    """Base exception for user-facing client service errors."""

    def __init__(self, message, grpc_exception=None):
        super().__init__(message)
        self.grpc_exception = grpc_exception


class ServiceUnavailableException(ClientServiceException):
    def __init__(self, grpc_exception):
        super().__init__(
            f"Service is currently unavailable. Please try again later. "
            f"(Details: {grpc_exception.details()})",
            grpc_exception,
        )


class InvalidArgumentException(ClientServiceException):
    def __init__(self, grpc_exception):
        super().__init__(
            f"Invalid request parameters. Please check the inputs. "
            f"(Details: {grpc_exception.details()})",
            grpc_exception,
        )


class PermissionDeniedException(ClientServiceException):
    def __init__(self, grpc_exception):
        super().__init__(
            f"You do not have permission to access this resource. "
            f"(Details: {grpc_exception.details()})",
            grpc_exception,
        )


class TimeoutException(ClientServiceException):
    def __init__(self, grpc_exception):
        super().__init__(
            f"Request timeout. Please try again later. "
            f"(Details: {grpc_exception.details()})",
            grpc_exception,
        )


class InternalServerErrorException(ClientServiceException):
    def __init__(self, grpc_exception):
        super().__init__(
            f"Internal server error occurred. Please try again later. "
            f"(Details: {grpc_exception.details()})",
            grpc_exception,
        )


def handle_grpc_exception(exception):
    """Translate a low-level gRPC error into a user-facing client exception."""
    status_code = exception.code()

    if status_code == grpc.StatusCode.UNAVAILABLE:
        raise ServiceUnavailableException(exception)
    if status_code == grpc.StatusCode.INVALID_ARGUMENT:
        raise InvalidArgumentException(exception)
    if status_code == grpc.StatusCode.PERMISSION_DENIED:
        raise PermissionDeniedException(exception)
    if status_code == grpc.StatusCode.DEADLINE_EXCEEDED:
        raise TimeoutException(exception)
    if status_code == grpc.StatusCode.INTERNAL:
        raise InternalServerErrorException(exception)

    raise ClientServiceException(f"An error occurred with status: {status_code}.")
