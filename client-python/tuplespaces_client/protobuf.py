import sys
from pathlib import Path


def load_generated_sources():
    generated_sources = (
        Path(__file__).resolve().parents[2]
        / "contract"
        / "target"
        / "generated-sources"
        / "protobuf"
        / "python"
    )

    generated_path = str(generated_sources)
    if generated_path not in sys.path:
        sys.path.insert(0, generated_path)

    import TupleSpaces_pb2 as tuple_spaces_pb2
    import TupleSpaces_pb2_grpc as tuple_spaces_pb2_grpc

    return tuple_spaces_pb2, tuple_spaces_pb2_grpc
