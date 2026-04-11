package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.service.ClientService;
import pt.ulisboa.tecnico.tuplespaces.client.service.exceptions.ClientServiceException;

import java.util.Scanner;

public class CommandProcessor {
    private static final String SPACE = " ";
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";
    private static final String SUCCESS = "OK";
    private static final String PUT = "put";
    private static final String READ = "read";
    private static final String TAKE = "take";
    private static final String GET_TUPLE_SPACES_STATE = "getTupleSpacesState";
    private static final String SLEEP = "sleep";
    private static final String EXIT = "exit";

    private final ClientService clientService;

    public CommandProcessor(ClientService clientService) {
        this.clientService = clientService;
    }

    void parseInput() {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String[] split = line.split(SPACE);

            switch (split[0]) {
                case PUT -> this.put(split);
                case READ -> this.read(split);
                case TAKE -> this.take(split);
                case GET_TUPLE_SPACES_STATE -> this.getTupleSpacesState();
                case SLEEP -> this.sleep(split);
                case EXIT -> exit = true;
                default -> this.printUsage();
            }

            if (!exit) {
                System.out.println();
            }
        }

        scanner.close();
        this.clientService.terminate();
    }

    private void put(String[] split) {
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        String tuple = split[1];
        int[] delays = extractDelays(split, 2);

        try {
            this.clientService.put(tuple, delays[0], delays[1], delays[2]);
            System.out.println(SUCCESS);
        } catch (ClientServiceException e) {
            System.out.println(e.getMessage());
        }
    }

    private void read(String[] split) {
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        String tuple = split[1];
        int[] delays = extractDelays(split, 2);

        try {
            String result = this.clientService.read(tuple, delays[0], delays[1], delays[2]);
            System.out.println(SUCCESS);
            System.out.println(result);
        } catch (ClientServiceException e) {
            System.out.println(e.getMessage());
        }
    }

    private void take(String[] split) {
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        String tuple = split[1];
        int[] delays = extractDelays(split, 2);

        try {
            String result = this.clientService.take(tuple, delays[0], delays[1], delays[2]);
            System.out.println(SUCCESS);
            System.out.println(result);
        } catch (ClientServiceException e) {
            System.out.println(e.getMessage());
        }
    }

    private void getTupleSpacesState() {
        try {
            String result = this.clientService.getTupleSpacesState();
            System.out.println(SUCCESS);
            System.out.println(result);
        } catch (ClientServiceException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sleep(String[] split) {
        if (split.length != 2) {
            this.printUsage();
            return;
        }

        try {
            int time = Integer.parseInt(split[1]);
            Thread.sleep(time * 1000);
            System.out.println(SUCCESS);
        } catch (NumberFormatException | InterruptedException e) {
            this.printUsage();
        }
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- put <element[,more_elements]> [delay1] [delay2] [delay3]\n" +
                "- read <element[,more_elements]> [delay1] [delay2] [delay3]\n" +
                "- take <element[,more_elements]> [delay1] [delay2] [delay3]\n" +
                "- getTupleSpacesState\n" +
                "- sleep <integer>\n" +
                "- exit\n");
    }

    private boolean inputIsValid(String[] input) {
        return input.length >= 2 &&
                input[1].startsWith(BGN_TUPLE) &&
                input[1].endsWith(END_TUPLE);
    }

    private int[] extractDelays(String[] split, int startIdx) {
        int[] delays = new int[3];

        for (int i = 0; i < 3; i++) {
            // if the index is within bounds, parse the value. otherwise, default to 0
            delays[i] = (startIdx + i < split.length) ? parseIntSafe(split[startIdx + i]) : 0;
        }

        return delays;
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
