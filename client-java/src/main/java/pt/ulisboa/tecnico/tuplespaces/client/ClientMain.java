package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.service.ClientService;

public class ClientMain {
    public static void main(String[] args) {
        System.out.println(ClientMain.class.getSimpleName());

        // check arguments
        if (args.length < 2) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<host:port> <client_id>");
            return;
        }

        // get the host and the port of the server or front-end
        final String host_port = args[0];

        // check if the first argument is in the correct host:port format
        final String hostPortRegex = "^[a-zA-Z0-9.-]+:\\d+$";
        if (!host_port.matches(hostPortRegex)) {
            System.err.println("Argument format invalid!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<host:port> <client_id>");
            return;
        }

        // parse and validate the client ID ensuring it is an integer
        final int client_id;
        try {
            client_id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Client ID must be an integer.");
            return;
        }

        // check if an optional third argument enables debug mode
        boolean debugMode = args.length == 3 && args[2].equals("-debug");

        // initialize the command processor with the client service and begin parsing user input
        ClientService clientService = new ClientService(host_port, client_id, debugMode);
        CommandProcessor parser = new CommandProcessor(clientService);
        parser.parseInput();
    }
}
