package pt.ulisboa.tecnico.tuplespaces.frontend.service.collector.take;

import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesBackendOuterClass.*;

import java.util.ArrayList;
import java.util.List;

public class TakeBackendResponseCollector {
    ArrayList<TakeBackendResponse> collectedResponses;
    private final ArrayList<StatusRuntimeException> errors;

    public TakeBackendResponseCollector() {
        collectedResponses = new ArrayList<TakeBackendResponse>();
        errors = new ArrayList<>();
    }

    synchronized public void addResponse(TakeBackendResponse response) {
        collectedResponses.add(response);
        notifyAll();
    }

    synchronized public ArrayList<TakeBackendResponse> getResponses() {
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
