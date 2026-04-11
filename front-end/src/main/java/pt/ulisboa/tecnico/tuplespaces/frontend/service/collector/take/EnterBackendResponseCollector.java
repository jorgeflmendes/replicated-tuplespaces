package pt.ulisboa.tecnico.tuplespaces.frontend.service.collector.take;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesBackendOuterClass.*;

import java.util.ArrayList;
import java.util.List;

public class EnterBackendResponseCollector {
    ArrayList<EnterBackendResponse> collectedResponses;
    private final ArrayList<StatusRuntimeException> errors;

    public EnterBackendResponseCollector() {
        collectedResponses = new ArrayList<EnterBackendResponse>();
        errors = new ArrayList<>();
    }

    synchronized public void addResponse(EnterBackendResponse response) {
        collectedResponses.add(response);
        notifyAll();
    }

    synchronized public ArrayList<EnterBackendResponse> getResponses() {
        return new ArrayList<>(collectedResponses);
    }

    public synchronized void waitUntilAllReceived(int n) throws StatusRuntimeException {
        while (collectedResponses.size() < n) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Professor said to keep the thread waiting after interruption
                System.err.println("Thread was interrupted, but will continue waiting for responses.");
            }

            if (!errors.isEmpty()) { // if there are errors, return false
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
