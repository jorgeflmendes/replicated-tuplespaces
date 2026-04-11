package pt.ulisboa.tecnico.tuplespaces.frontend.service.collector;

import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesBackendOuterClass.*;
import io.grpc.StatusRuntimeException;

import java.util.ArrayList;
import java.util.List;

public class GetTupleSpacesStateBackendResponseCollector {
    private final ArrayList<getTupleSpacesStateBackendResponse> collectedResponses;
    private final ArrayList<StatusRuntimeException> errors;

    public GetTupleSpacesStateBackendResponseCollector() {
        collectedResponses = new ArrayList<>();
        errors = new ArrayList<>();
    }

    public synchronized void addResponse(getTupleSpacesStateBackendResponse response) {
        collectedResponses.add(response);
        notifyAll();
    }

    public synchronized List<getTupleSpacesStateBackendResponse> getResponses() {
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