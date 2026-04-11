package pt.ulisboa.tecnico.tuplespaces.frontend.service.collector;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesBackendOuterClass.*;

import java.util.ArrayList;
import java.util.List;

public class ReadBackendResponseCollector {
    ArrayList<ReadBackendResponse> collectedResponses;
    private final ArrayList<StatusRuntimeException> errors;

    public ReadBackendResponseCollector() {
        collectedResponses = new ArrayList<ReadBackendResponse>();
        errors = new ArrayList<>();
    }

    synchronized public void addResponse(ReadBackendResponse response) {
        collectedResponses.add(response);
        notifyAll();
    }

    synchronized public ArrayList<ReadBackendResponse> getResponses() {
        return new ArrayList<>(collectedResponses);
    }

    synchronized public void waitUntilAllReceived(int n) throws StatusRuntimeException {
        while (collectedResponses.size() < n) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Professor said to keep the thread waiting after interruption
                System.err.println("Thread was interrupted, but will continue waiting for responses.");
            }

            if (errors.size() == 3) { // if every server sent an error, send the first error
                throw getErrors().get(0);
            }
        }
    }

    public synchronized void addError(StatusRuntimeException e) {
        errors.add(e);
        notifyAll();
    }

    public synchronized List<StatusRuntimeException> getErrors() {
        return new ArrayList<>(errors);
    }
}
