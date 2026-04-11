package pt.ulisboa.tecnico.tuplespaces.frontend;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.FrontEndInterceptor;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.FrontEndServiceImpl;

import java.util.List;

public class FrontEndMain {
    public static void main(String[] args) throws Exception {
        // check arguments
        if (args.length < 4) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -Dexec.args=<port> <host1:port1> <host2:port2> <host3:port3>");
            return;
        }

        // try to convert the first argument to a number (a port)
        int frontEndPort;
        try {
            frontEndPort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("The first argument must be an integer (port).");
            return;
        }

        List<String> hostPorts = List.of(args[1], args[2], args[3]);

        // enable debug mode if a last argument is provided and equals "-debug"
        boolean debugMode = args.length == 5 && args[4].equals("-debug");

        // create the gRPC service impl. with the specified delays and debug mode
        final BindableService impl = new FrontEndServiceImpl(hostPorts, debugMode);

        // create a new server to listen to the specified port
        Server server = ServerBuilder.forPort(frontEndPort)
                .addService(ServerInterceptors.intercept(impl, new FrontEndInterceptor())).build();

        // start the server
        server.start();

        // keep the main thread running until the server is terminated
        server.awaitTermination();
    }
}
