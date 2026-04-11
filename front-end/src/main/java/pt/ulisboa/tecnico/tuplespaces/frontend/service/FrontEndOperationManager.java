package pt.ulisboa.tecnico.tuplespaces.frontend.service;

import pt.ulisboa.tecnico.tuplespaces.common.Operation;
import pt.ulisboa.tecnico.tuplespaces.common.OperationManager;

import java.util.PriorityQueue;

public class FrontEndOperationManager extends OperationManager {
    public FrontEndOperationManager(boolean debugMode) {
        super(debugMode);
    }

    @Override
    protected String checkConstraints(Integer clientId, String operationToPerformId) {
        PriorityQueue<Operation> clientOps = super.getOperations(clientId);

        if (clientOps == null || clientOps.isEmpty()) {
            return null;
        }

        // find the operation matching operationToPerformId
        Operation operationToPerform = null;
        for (Operation op : clientOps) {
            if (op.getId().equals(operationToPerformId)) {
                operationToPerform = op;
                break;
            }
        }

        if (operationToPerform == null) {
            return null;
        }

        // check if there is any TAKE operation before a PUT operation (this is the only constraint for the FrontEnd)
        if (operationToPerform.getType() == Operation.Type.PUT) {
            for (Operation op : clientOps) {
                // if the operation to perform is found in the queue without a prior TAKE, there is no constraint
                if (op.getId().equals(operationToPerformId)) {
                    return null;
                }

                // if a TAKE operation is found before the PUT operation, a constraint is present
                if (op.getType() == Operation.Type.TAKE) {
                    return "FRONTEND_PUT_MUST_WAIT_FOR_TAKE_TO FINISH_BEFORE_BEING_SENT_IF_SAME_CLIENT";
                }
            }
        }

        return null;
    }
}
