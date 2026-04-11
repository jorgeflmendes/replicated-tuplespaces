import re
import sys

from .command_processor import CommandProcessor
from .service import ClientService

HOST_PORT_PATTERN = r"^[a-zA-Z0-9.-]+:\d+$"


def main(args=None):
    args = sys.argv[1:] if args is None else args

    if len(args) < 2:
        print("Argument(s) missing!", file=sys.stderr)
        print("Usage: python client_main.py <host:port> <client_id>", file=sys.stderr)
        return

    host_port = args[0]
    if not re.match(HOST_PORT_PATTERN, host_port):
        print("Usage: python client_main.py <host:port> <client_id>", file=sys.stderr)
        return

    try:
        client_id = int(args[1])
    except ValueError:
        print("Usage: python client_main.py <host:port> <client_id>", file=sys.stderr)
        return

    debug_mode = len(args) > 2 and args[2] == "-debug"
    parser = CommandProcessor(ClientService(host_port, client_id, debug_mode))
    parser.parse_input()


if __name__ == "__main__":
    main()
