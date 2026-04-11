import time

from .exceptions import ClientServiceException


class CommandProcessor:
    SPACE = " "
    BGN_TUPLE = "<"
    END_TUPLE = ">"
    SUCCESS = "OK"
    PUT = "put"
    READ = "read"
    TAKE = "take"
    GET_TUPLE_SPACES_STATE = "getTupleSpacesState"
    SLEEP = "sleep"
    EXIT = "exit"

    def __init__(self, client_service):
        self.client_service = client_service

    def parse_input(self):
        exit_flag = False

        while not exit_flag:
            try:
                line = input("> ").strip()
            except EOFError:
                break

            if not line:
                print()
                continue

            split = line.split(self.SPACE)
            command = split[0]

            if command == self.PUT:
                self.put(split)
            elif command == self.READ:
                self.read(split)
            elif command == self.TAKE:
                self.take(split)
            elif command == self.GET_TUPLE_SPACES_STATE:
                self.get_tuple_spaces_state()
            elif command == self.SLEEP:
                self.sleep(split)
            elif command == self.EXIT:
                exit_flag = True
            else:
                self.print_usage()

            if not exit_flag:
                print()

        self.terminate()

    def put(self, split):
        if not self.input_is_valid(split):
            self.print_usage()
            return

        tuple_data = split[1]
        delays = self.extract_delays(split, 2)

        try:
            self.client_service.put(tuple_data, delays[0], delays[1], delays[2])
            print(self.SUCCESS)
        except ClientServiceException as exception:
            print(exception)

    def read(self, split):
        if not self.input_is_valid(split):
            self.print_usage()
            return

        tuple_data = split[1]
        delays = self.extract_delays(split, 2)

        try:
            result = self.client_service.read(tuple_data, delays[0], delays[1], delays[2])
            print(self.SUCCESS)
            print(str(result))
        except ClientServiceException as exception:
            print(exception)

    def take(self, split):
        if not self.input_is_valid(split):
            self.print_usage()
            return

        tuple_data = split[1]
        delays = self.extract_delays(split, 2)

        try:
            result = self.client_service.take(tuple_data, delays[0], delays[1], delays[2])
            print(self.SUCCESS)
            print(str(result))
        except ClientServiceException as exception:
            print(exception)

    def get_tuple_spaces_state(self):
        try:
            result = self.client_service.get_tuple_spaces_state()
            print(self.SUCCESS)
            print(self.tuple_spaces_as_java(result))
        except ClientServiceException as exception:
            print(exception)

    def sleep(self, split):
        if len(split) < 2 or not split[1].isdigit():
            self.print_usage()
            return

        time.sleep(int(split[1]))
        print(self.SUCCESS)

    def terminate(self):
        self.client_service.terminate()

    def print_usage(self):
        print(
            "Usage:\n"
            "- put <element[,more_elements]> [delay1] [delay2] [delay3]\n"
            "- read <element[,more_elements]> [delay1] [delay2] [delay3]\n"
            "- take <element[,more_elements]> [delay1] [delay2] [delay3]\n"
            "- getTupleSpacesState\n"
            "- sleep <integer>\n"
            "- exit\n"
        )

    def input_is_valid(self, input_data):
        return (
            len(input_data) >= 2
            and input_data[1].startswith(self.BGN_TUPLE)
            and input_data[1].endswith(self.END_TUPLE)
        )

    def tuple_spaces_as_java(self, tuple_space):
        return "[" + ", ".join(tuple_space) + "]"

    def extract_delays(self, split, start_idx):
        delays = [0] * 3

        for index in range(3):
            if start_idx + index < len(split):
                delays[index] = self.parse_int_safe(split[start_idx + index])

        return delays

    def parse_int_safe(self, value):
        try:
            return int(value)
        except ValueError:
            return 0
