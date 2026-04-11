package pt.ulisboa.tecnico.tuplespaces.server.service;

import pt.ulisboa.tecnico.tuplespaces.common.Operation;
import pt.ulisboa.tecnico.tuplespaces.common.OperationManager;

import java.util.PriorityQueue;

public class ServerOperationManager extends OperationManager {
    public ServerOperationManager(boolean debugMode) {
        super(debugMode);
    }

    @Override
    protected String checkConstraints(Integer clientId, String operationToPerformId) {
        PriorityQueue<Operation> ops = super.getOperations(clientId);

        if (ops == null || ops.isEmpty()) {
            return null;
        }

        // check if the operation to perform is the 1st in the queue for the client. if it is, return null
        Operation firstOp = ops.peek();
        if (firstOp.getId().equals(operationToPerformId)) {
            return null;
        }

        // if not (there are still operations in the queue before the operation to perform), return the name of the constraint
        // (operations must be performed sequentially in the replica)
        return "SERVERS_SEQ_ORDER_PER_CLIENT";
    }
}
