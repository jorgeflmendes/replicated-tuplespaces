package pt.ulisboa.tecnico.tuplespaces.common;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

public abstract class OperationManager {
    private final ConcurrentHashMap<Integer, PriorityQueue<Operation>> operations;
    private final boolean debugMode;

    public OperationManager(boolean debugMode) {
        this.operations = new ConcurrentHashMap<>();
        this.debugMode = debugMode;
    }

    public void debug(String message) {
        if (this.debugMode) {
            System.err.println("[DEBUG] " + message);
        }
    }

    private void logger(String message) {
        if (this.debugMode) {
            System.err.println("[LOGGER] " + message);
        }
    }

    private void constraintLogger(String message) {
        if (this.debugMode) {
            System.err.println("[CONSTRAINT LOGGER] " + message);
        }
    }

    public PriorityQueue<Operation> getOperations(Integer clientId) {
        return this.operations.get(clientId);
    }

    public void logOperation(String operationId, Operation.Type type, String pattern, Integer clientId) {
        Operation op = new Operation(operationId, type, pattern);

        // get or create a priority queue for the client
        PriorityQueue<Operation> clientOps = this.operations.computeIfAbsent(clientId, k ->
                new PriorityQueue<>(Comparator.comparingLong(Operation::getTimestamp)));

        synchronized (clientOps) {
            // check if an operation with the same id already exists
            boolean exists = clientOps.stream().anyMatch(o -> o.getId().equals(operationId));

            if (!exists) {
                clientOps.add(op);
                logger("Operation added: " + operationId + " for client " + clientId + " (" + op + ")");
            }
        }
    }

    public void removeOperation(Integer clientId, String operationId) {
        PriorityQueue<Operation> clientOps = this.operations.get(clientId);

        if (clientOps != null) {
            synchronized (clientOps) {
                // find and remove the operation by id
                boolean removed = clientOps.removeIf(o -> o.getId().equals(operationId));
                if (removed) {
                    logger("Operation removed: " + operationId + " for client " + clientId);
                }
                clientOps.notifyAll();
            }
        }
    }

    public void waitUntilCanPerformOperation(Integer clientId, String operationToPerformId) {
        PriorityQueue<Operation> clientOps = this.operations.get(clientId);

        if (clientOps != null) {
            synchronized (clientOps) {
                String constraint;
                while ((constraint = checkConstraints(clientId, operationToPerformId)) != null) {
                    constraintLogger("Operation with ID " + operationToPerformId + " is waiting to be executed due to the " + constraint + " constraint");
                    waitFor(clientOps);
                }
            }
        }
    }

    private void waitFor(Object object) {
        try {
            synchronized (object) {
                object.wait();
            }
        } catch (InterruptedException e) {
            // thread interruption is ignored to maintain waiting behavior
            System.err.println("Thread was interrupted, but will continue waiting.");
        }
    }

    protected abstract String checkConstraints(Integer clientId, String operationToPerformId);

}
