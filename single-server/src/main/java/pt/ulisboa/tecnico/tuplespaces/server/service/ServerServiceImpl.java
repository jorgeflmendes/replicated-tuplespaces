package pt.ulisboa.tecnico.tuplespaces.server.service;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesBackendGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesBackendOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.common.Operation;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ServerServiceImpl extends TupleSpacesBackendGrpc.TupleSpacesBackendImplBase {
    private final boolean debugMode;
    // contains the replica's state and its locks
    private final ServerState serverState;
    // manages operations related to the server
    private final ServerOperationManager operationManager;

    public ServerServiceImpl(boolean debugMode) {
        this.debugMode = debugMode;
        this.serverState = new ServerState();
        this.operationManager = new ServerOperationManager(debugMode);
    }

    @Override
    public void putBackend(PutBackendRequest request, StreamObserver<PutBackendResponse> responseObserver) {
        // log the PUT operation before processing
        this.operationManager.logOperation(request.getOperationId(), Operation.Type.PUT, request.getNewTuple(), request.getClientId());

        // wait for any conditions that must be met before performing the operation
        this.operationManager.debug("Checking if there are any constraints regarding the execution of the PUT request from client " + request.getClientId() + " at this moment");
        this.operationManager.waitUntilCanPerformOperation(request.getClientId(), request.getOperationId());

        // retrieve the delay value from context
        int delay = parseIntSafe(ServerInterceptor.CONTEXT_DELAY.get());

        // simulate processing delay
        this.operationManager.debug("Applying a delay of " + delay + " seconds for the PUT request from client " + request.getClientId());
        this.applyDelay(delay);

        String requestedTuple = request.getNewTuple();

        // validate the tuple format before inserting it
        if (!tupleIsValid(requestedTuple)) {
            String errorMessage = "Invalid tuple format!";
            System.err.println(errorMessage);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(errorMessage).asRuntimeException());
            return;
        }

        // store the tuple in the server state
        this.operationManager.debug("Executing the PUT request from client " + request.getClientId() + " in the \"database\"");
        serverState.put(requestedTuple);

        // send a success response back to the frontend
        this.operationManager.debug("Sending an empty success response for the PUT to the frontend regarding the request from client " + request.getClientId());
        PutBackendResponse response = PutBackendResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        // remove the operation from the log since it has been processed
        this.operationManager.removeOperation(request.getClientId(), request.getOperationId());
    }

    public void readBackend(ReadBackendRequest request, StreamObserver<ReadBackendResponse> responseObserver) {
        // log the READ operation before processing
        this.operationManager.logOperation(request.getOperationId(), Operation.Type.READ, request.getSearchPattern(), request.getClientId());

        // wait for any conditions that must be met before performing the operation
        this.operationManager.debug("Checking if there are any constraints regarding the execution of the READ request from client " + request.getClientId() + " at this moment");
        this.operationManager.waitUntilCanPerformOperation(request.getClientId(), request.getOperationId());

        // retrieve the delay value from context
        int delay = parseIntSafe(ServerInterceptor.CONTEXT_DELAY.get());

        // simulate processing delay
        this.operationManager.debug("Applying a delay of " + delay + " seconds for the READ request from client " + request.getClientId());
        this.applyDelay(delay);

        // validate the search pattern
        if (!tupleIsValid(request.getSearchPattern())) {
            String errorMessage = "invalid search pattern format!";
            System.err.println(errorMessage);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(errorMessage).asRuntimeException());
            return;
        }

        // read the tuple from the server state
        this.operationManager.debug("Executing the READ request from client " + request.getClientId() + " in the \"database\"");
        String resultTuple = serverState.read(request.getSearchPattern());

        // send a success response with the requested tuple to the frontend
        this.operationManager.debug("Sending a response for the READ to the frontend regarding the request from client " + request.getClientId() + " (" + resultTuple + ")");
        ReadBackendResponse response = ReadBackendResponse.newBuilder().setResult(resultTuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        // remove the operation from the log since it's completed
        this.operationManager.removeOperation(request.getClientId(), request.getOperationId());
    }

    @Override
    public void enterBackend(EnterBackendRequest request, StreamObserver<EnterBackendResponse> responseObserver) {
        // first step of the TAKE (this part will only occur if this replica is part of the request's voter set)

        // log the ENTER operation before processing
        this.operationManager.logOperation(request.getOperationId(), Operation.Type.TAKE, request.getPattern(), request.getClientId());

        // wait for any conditions that must be met before performing the operation
        this.operationManager.debug("Checking if there are any constraints regarding the execution of the ENTER request from client " + request.getClientId() + " at this moment");
        this.operationManager.waitUntilCanPerformOperation(request.getClientId(), request.getOperationId());

        // retrieve the delay value from context
        int delay = parseIntSafe(ServerInterceptor.CONTEXT_DELAY.get());

        // simulate processing delay
        this.operationManager.debug("Applying a delay of " + delay + " seconds for the ENTER request from client " + request.getClientId());
        this.applyDelay(delay);

        // lock the pattern ("voted := true" according to the algorithm in the lectures slides)
        this.operationManager.debug("Locking the pattern " + request.getPattern() + " due to the ENTER request from client " + request.getClientId());
        this.serverState.lockPattern(request.getPattern(), request.getOperationId());

        // get matching tuples. if empty, the function will stay in wait
        this.operationManager.debug("Searching for compatible tuples (ENTER request from client " + request.getClientId() + ") in the \"database\"");
        List<String> matchingTuples = this.serverState.getMatchingTuples(request.getPattern());

        // send success response to the frontend indicating auth. to enter critical section for the specified pattern
        this.operationManager.debug("Sending a response for the ENTER to the frontend regarding the request from client " + request.getClientId() + " (" + matchingTuples + ")");
        EnterBackendResponse response = EnterBackendResponse.newBuilder().addAllTuple(matchingTuples).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        // the operation will only be considered complete at the end of the next step (TAKE)
    }

    @Override
    public void takeBackend(TakeBackendRequest request, StreamObserver<TakeBackendResponse> responseObserver) {
        // second step of the TAKE (all replicas execute this step)

        // log the TAKE operation before processing (it was probably already registered in the previous step [ENTER] if it is part of the request's voter set)
        this.operationManager.logOperation(request.getOperationId(), Operation.Type.TAKE, request.getTuple(), request.getClientId());

        // wait for any conditions that must be met before performing the operation (identical ids do not wait for themselves)
        this.operationManager.debug("Checking if there are any constraints regarding the execution of the final TAKE request from client " + request.getClientId() + " at this moment");
        this.operationManager.waitUntilCanPerformOperation(request.getClientId(), request.getOperationId());

        // validate the tuple format
        if (!tupleIsValid(request.getTuple())) {
            String errorMessage = "Invalid tuple format!";
            System.err.println(errorMessage);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(errorMessage).asRuntimeException());
            return;
        }

        // proceed with the take operation
        this.operationManager.debug("Executing the final TAKE request from client " + request.getClientId() + " in the \"database\"");
        serverState.take(request.getTuple());

        // send an empty success response to the frontend
        this.operationManager.debug("Sending an empty success response for the final TAKE to the frontend regarding the request from client " + request.getClientId());
        TakeBackendResponse response = TakeBackendResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        // remove the lock if it is part of the request's voter set
        if (request.getIsVoter()) {
            this.operationManager.debug("Removing lock related to the ENTER part of the TAKE from client " + request.getClientId() + " because this server was a voter");
            this.serverState.unlockPattern(request.getOperationId());
        }

        // remove the operation from the log since it's completed
        this.operationManager.removeOperation(request.getClientId(), request.getOperationId());
    }

    @Override
    public void getTupleSpacesStateBackend(getTupleSpacesStateBackendRequest request, StreamObserver<getTupleSpacesStateBackendResponse> responseObserver) {
        // log the GETTUPLESPACESSTATE operation before processing
        this.operationManager.logOperation(request.getOperationId(), Operation.Type.GETTUPLESPACESTATE, "", request.getClientId());

        // wait for any conditions that must be met before performing the operation
        this.operationManager.debug("Checking if there are any constraints regarding the execution of the GETTUPLESPACESTATE request from client " + request.getClientId() + " at this moment");
        this.operationManager.waitUntilCanPerformOperation(request.getClientId(), request.getOperationId());

        // get the tuple spaces state from the server state
        this.operationManager.debug("Executing the GETTUPLESPACESTATE request from client " + request.getClientId() + " in the \"database\"");
        List<String> tupleList = serverState.getState();

        // send a success response with the server tuple spaces state
        this.operationManager.debug("Sending a response for the GETTUPLESPACESTATE to the frontend regarding the request from client " + request.getClientId());
        getTupleSpacesStateBackendResponse response = getTupleSpacesStateBackendResponse.newBuilder().addAllTuple(tupleList).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        // remove the operation from the log since it's completed
        this.operationManager.removeOperation(request.getClientId(), request.getOperationId());
    }

    private boolean tupleIsValid(String tuple) {
        final String BGN_TUPLE = "<";
        final String END_TUPLE = ">";

        return tuple.startsWith(BGN_TUPLE) && tuple.endsWith(END_TUPLE);
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void applyDelay(int delay) {
        try {
            TimeUnit.SECONDS.sleep(delay);
        } catch (InterruptedException e) {
            System.err.println("thread was interrupted during delay, continuing...");
        }
    }
}
