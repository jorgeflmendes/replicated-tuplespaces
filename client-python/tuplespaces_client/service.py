import sys

import grpc

from .exceptions import handle_grpc_exception
from .protobuf import load_generated_sources

TupleSpaces_pb2, TupleSpaces_pb2_grpc = load_generated_sources()


class ClientService:
    DELAY_1 = "delay1"
    DELAY_2 = "delay2"
    DELAY_3 = "delay3"

    def __init__(self, host_port, client_id, debug_mode):
        self.channel = grpc.insecure_channel(host_port)
        self.stub = TupleSpaces_pb2_grpc.TupleSpacesStub(self.channel)
        self.client_id = client_id
        self.debug_mode = debug_mode

    def put(self, tuple_data, delay1, delay2, delay3):
        metadata = self.create_metadata(delay1, delay2, delay3)

        try:
            self.debug(f"Sending put request with tuple {tuple_data}")
            request = TupleSpaces_pb2.PutRequest(newTuple=tuple_data)
            self.stub.put(request, metadata=metadata)
            self.debug("Put response received")
        except grpc.RpcError as exception:
            self.debug(f"Server returned an error ({exception}), translating it")
            handle_grpc_exception(exception)

    def read(self, pattern, delay1, delay2, delay3):
        metadata = self.create_metadata(delay1, delay2, delay3)

        try:
            self.debug(f"Sending read request with pattern {pattern}")
            request = TupleSpaces_pb2.ReadRequest(searchPattern=pattern)
            response = self.stub.read(request, metadata=metadata)
            self.debug(f"Read response received: {response.result}")
            return response.result
        except grpc.RpcError as exception:
            self.debug(f"Server returned an error ({exception}), translating it")
            handle_grpc_exception(exception)

    def take(self, pattern, delay1, delay2, delay3):
        metadata = self.create_metadata(delay1, delay2, delay3)

        try:
            self.debug(f"Sending take request with pattern {pattern}")
            request = TupleSpaces_pb2.TakeRequest(pattern=pattern, clientId=self.client_id)
            response = self.stub.take(request, metadata=metadata)
            self.debug(f"Take response received: {response.result}")
            return response.result
        except grpc.RpcError as exception:
            self.debug(f"Server returned an error ({exception}), translating it")
            handle_grpc_exception(exception)

    def get_tuple_spaces_state(self):
        try:
            self.debug("Requesting tuple spaces state")
            request = TupleSpaces_pb2.getTupleSpacesStateRequest()
            response = self.stub.getTupleSpacesState(request)
            self.debug(f"State response received: {response.tuple}")
            return response.tuple
        except grpc.RpcError as exception:
            self.debug(f"Server returned an error ({exception}), translating it")
            handle_grpc_exception(exception)

    def terminate(self):
        self.channel.close()

    def create_metadata(self, delay1, delay2, delay3):
        return [
            (self.DELAY_1, str(delay1)),
            (self.DELAY_2, str(delay2)),
            (self.DELAY_3, str(delay3)),
        ]

    def debug(self, message):
        if self.debug_mode:
            print(f"[Debug] {message}", file=sys.stderr)
