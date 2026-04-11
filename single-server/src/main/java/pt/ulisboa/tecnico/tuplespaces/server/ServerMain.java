package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import pt.ulisboa.tecnico.tuplespaces.server.service.ServerServiceImpl;
import pt.ulisboa.tecnico.tuplespaces.server.service.ServerInterceptor;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        // check arguments
        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<port>");
            return;
        }

        // try to convert the argument to a number (a port)
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("The first argument must be an integer (port).");
            return;
        }

        // enable debug mode if a second argument is provided and equals "-debug"
        boolean debugMode = args.length == 2 && args[1].equals("-debug");

        // create the gRPC service impl. with the specified debug mode
        final BindableService impl = new ServerServiceImpl(debugMode);

        // create a new server to listen the specified port
        Server server = ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(impl, new ServerInterceptor())).build();

        // start the server
        server.start();

        // keep the main thread running until the server is terminated
        server.awaitTermination();
    }
}
