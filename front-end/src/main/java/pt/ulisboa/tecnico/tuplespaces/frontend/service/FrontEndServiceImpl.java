package pt.ulisboa.tecnico.tuplespaces.frontend.service;

import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesBackendGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesBackendGrpc.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesBackendOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.common.Operation;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.collector.GetTupleSpacesStateBackendResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.collector.PutBackendResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.collector.ReadBackendResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.collector.take.TakeBackendResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.collector.take.EnterBackendResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.observer.GetTupleSpacesStateBackendResponseObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.observer.PutBackendResponseObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.observer.ReadResponseBackendObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.observer.take.TakeBackendResponseObserver;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.observer.take.EnterBackendResponseObserver;

import java.util.*;

public class FrontEndServiceImpl extends TupleSpacesGrpc.TupleSpacesImplBase {
    static final String INDIVIDUAL_DELAY_KEY = "delay";
    private final boolean debugMode;

    // manages operations related to the front-end
    private final FrontEndOperationManager operationManager;
    // communication channels with backend services
    private final List<ManagedChannel> channels;
    // asynchronous stubs for backend communication
    private final List<TupleSpacesBackendStub> asyncStubs;

    public FrontEndServiceImpl(List<String> hostPorts, boolean debugMode) {
        this.debugMode = debugMode;
        this.operationManager = new FrontEndOperationManager(debugMode);

        this.channels = new ArrayList<>();
        this.asyncStubs = new ArrayList<>();

        for (String hostPort : hostPorts) {
            // create a managed gRPC channel for the given host and port
            ManagedChannel channel = ManagedChannelBuilder.forTarget(hostPort)
                    .usePlaintext()
                    .build();

            this.channels.add(channel);

            // create an asynchronous stub for communication with the backend
            TupleSpacesBackendStub asyncStub = TupleSpacesBackendGrpc.newStub(channel);
            this.asyncStubs.add(asyncStub);
        }
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        // generate a unique operation ID
        String uuid = UUID.randomUUID().toString();

        // log the PUT operation in the operation manager
        this.operationManager.logOperation(uuid, Operation.Type.PUT, request.getNewTuple(), request.getClientId());

        // immediately send an empty confirmation response to the client (his request will be handled later by the backend)
        try {
            this.operationManager.debug("Sending an empty success PUT response to client " + request.getClientId());
            responseObserver.onNext(PutResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            System.err.println("Error while attempting to send a response to the client " + request.getClientId()+ ": " + e.getMessage());
            return;
        }

        // wait until it is safe to perform the backend part of the operation (manage constraints)
        this.operationManager.debug("Checking if there are any constraints regarding sending the PUT request from client " + request.getClientId() + " to the replicas.\n");
        this.operationManager.waitUntilCanPerformOperation(request.getClientId(), uuid);

        try {
            // send the PUT request to each backend
            PutBackendResponseCollector collector = new PutBackendResponseCollector();
            for (int index = 0; index < asyncStubs.size(); index++) {
                String delay = getDelayFromContext(index);

                TupleSpacesBackendStub stubWithMeta = createStubWithMetadata(index, INDIVIDUAL_DELAY_KEY, delay);

                PutBackendRequest requestBackend = PutBackendRequest.newBuilder()
                        .setNewTuple(request.getNewTuple())
                        .setClientId(request.getClientId())
                        .setOperationId(uuid)
                        .build();

                this.operationManager.debug("Sending PUT request from client " + request.getClientId() + " to replica " + index);
                stubWithMeta.putBackend(requestBackend, new PutBackendResponseObserver(collector, this.debugMode));
            }

            // wait until all backend responses are received
            this.operationManager.debug("Waiting for all responses from the replicas to the PUT request from client " + request.getClientId() );
            collector.waitUntilAllReceived(asyncStubs.size());
            this.operationManager.debug("All responses from the replicas received regarding the PUT request from client " + request.getClientId() );

            // remove the operation from the operation manager after completion
            this.operationManager.removeOperation(request.getClientId(), uuid);
        } catch (StatusRuntimeException e) {
            // since the response was sent to the client at the beginning, it's not possible to send an error later
            System.err.println("At least 1 error received while waiting for responses from replicas to a PUT request: " + e.getMessage());
        }
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        // generate a unique operation ID for the request
        String uuid = UUID.randomUUID().toString();

        // log the read operation
        this.operationManager.logOperation(uuid, Operation.Type.READ, request.getSearchPattern(), request.getClientId());

        // wait until it's safe to perform the operation
        this.operationManager.debug("Checking if there are any constraints regarding sending the READ request from client " + request.getClientId() + " to the replicas");
        this.operationManager.waitUntilCanPerformOperation(request.getClientId(), uuid);

        try {
            // send the READ request to each backend
            ReadBackendResponseCollector collector = new ReadBackendResponseCollector();
            for (int index = 0; index < asyncStubs.size(); index++) {
                String delay = getDelayFromContext(index);

                TupleSpacesBackendStub stubWithMeta = createStubWithMetadata(index, INDIVIDUAL_DELAY_KEY, delay);
                ReadBackendRequest requestBackend = ReadBackendRequest.newBuilder()
                        .setClientId(request.getClientId())
                        .setOperationId(uuid)
                        .setSearchPattern(request.getSearchPattern())
                        .build();

                this.operationManager.debug("Sending READ request from client " + request.getClientId() + " to replica " + index);
                stubWithMeta.readBackend(requestBackend, new ReadResponseBackendObserver(collector, this.debugMode));
            }

            // wait for 1 backend response to be collected
            this.operationManager.debug("Waiting for at least 1 replica in response to the READ request from client " + request.getClientId());
            collector.waitUntilAllReceived(1);

            String result = collector.getResponses().get(0).getResult();

            // send the result back to the client
            try {
                this.operationManager.debug("Sending a READ response to client " + request.getClientId());
                responseObserver.onNext(ReadResponse.newBuilder().setResult(result).build());
                responseObserver.onCompleted();
            } catch (StatusRuntimeException e) {
                System.err.println("Error while attempting to send a response to the client " + request.getClientId()+ ": " + e.getMessage());
                return;
            }

            // remove the operation from the operation manager after completion
            this.operationManager.removeOperation(request.getClientId(), uuid);
        } catch (StatusRuntimeException e) {
            // handle errors from backend replicas and forward to the client with a custom error message
            System.err.println("3 errors received while waiting for responses from replicas to a READ request from client " + request.getClientId());
            Status status = e.getStatus();
            responseObserver.onError(status.withDescription("Server side error: " + e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        // generate a random id for the operation
        String uuid = UUID.randomUUID().toString();

        // log the operation
        this.operationManager.logOperation(uuid, Operation.Type.TAKE, request.getPattern(), request.getClientId());

        // wait until the operation can be performed
        this.operationManager.debug("Checking if there are any constraints regarding sending the TAKE request from client " + request.getClientId() + " to the replicas.\n");
        this.operationManager.waitUntilCanPerformOperation(request.getClientId(), uuid);

        // get the voter set for decision-making (who participates in the consensus)
        Set<Integer> voterSet = getVoterSet(request.getClientId());

        // create the stubs with the delay metadata for each backend
        TupleSpacesBackendStub[] stubsWithMeta = new TupleSpacesBackendStub[3];
        String[] delays = new String[3];
        int index;
        for (index = 0; index < asyncStubs.size(); index++) {
            delays[index] = getDelayFromContext(index);
            stubsWithMeta[index] = createStubWithMetadata(index, INDIVIDUAL_DELAY_KEY, delays[index]);
        }

        List<String> commonTuples = new ArrayList<>();
        try {
            // enter critical section: wait until common tuples are found
            while (commonTuples.isEmpty()) {
                EnterBackendResponseCollector checkCollector = new EnterBackendResponseCollector();

                // send the enter request to each voter in the set
                for (Integer voter : voterSet) {
                    EnterBackendRequest requestCheck = EnterBackendRequest.newBuilder()
                            .setPattern(request.getPattern())
                            .setClientId(request.getClientId())
                            .setOperationId(uuid)
                            .build();

                    this.operationManager.debug("Sending the ENTER request (1st part of the TAKE request) from client " + request.getClientId() + " to voting replica " + voter);
                    stubsWithMeta[voter].enterBackend(requestCheck, new EnterBackendResponseObserver(checkCollector, this.debugMode));
                }

                // wait for responses from all voters
                this.operationManager.debug("Waiting for all responses from the voter set of replicas to the ENTER request (part of the TAKE request) from client " + request.getClientId());
                checkCollector.waitUntilAllReceived(voterSet.size());

                // check if common tuples exist among the responses
                commonTuples = new ArrayList<>();
                for (EnterBackendResponse response : checkCollector.getResponses()) {
                    if (commonTuples.isEmpty()) {
                        commonTuples.addAll(response.getTupleList());
                    } else {
                        commonTuples.retainAll(response.getTupleList());
                    }
                }

                if (commonTuples.isEmpty()) {
                    this.operationManager.debug("The intersection of the responses from the replicas to the ENTER request from client " + request.getClientId() + " is empty");
                }
            }
        } catch (StatusRuntimeException e) {
            System.err.println("Error while sending the request to the server: " + e.getMessage());
            Status status = e.getStatus();
            responseObserver.onError(status.withDescription("Server side error: " + e.getMessage()).asRuntimeException());
        }

        // choose the first common tuple
        String chosenTuple = commonTuples.get(0);
        this.operationManager.debug("The chosen tuple regarding the ENTER request (part of the TAKE request) from client " + request.getClientId() + " is " + chosenTuple);

        // send the response to the client with the chosen tuple
        try {
            this.operationManager.debug("Sending a TAKE response with the chosen tuple to client " + request.getClientId());
            responseObserver.onNext(TakeResponse.newBuilder().setResult(chosenTuple).build());
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            System.err.println("Error while sending the response to the client: " + e.getMessage());
        }

        // send the TAKE request to each backend replica (phase 2)
        try {
            TakeBackendResponseCollector takeCollector = new TakeBackendResponseCollector();
            for (index = 0; index < asyncStubs.size(); index++) {
                boolean isVoter = voterSet.contains(index);

                TakeBackendRequest requestBackend = TakeBackendRequest.newBuilder()
                        .setTuple(chosenTuple)
                        .setOperationId(uuid)
                        .setClientId(request.getClientId())
                        .setIsVoter(isVoter)
                        .build();

                this.operationManager.debug("Sending the final part of the TAKE request from client " + request.getClientId() + " to replica " + index);
                stubsWithMeta[index].takeBackend(requestBackend, new TakeBackendResponseObserver(takeCollector, this.debugMode));
            }

            // wait for all take responses from the backends
            this.operationManager.debug("Waiting for all responses from replicas to the final part of the TAKE request from client " + request.getClientId());
            takeCollector.waitUntilAllReceived(asyncStubs.size());

            // remove the operation after it's completed
            this.operationManager.removeOperation(request.getClientId(), uuid);
        } catch (StatusRuntimeException e) {
            // since the response was sent to the client at the middle, it's not possible to send an error later
            System.err.println("One or more errors occurred while waiting for responses from backend replicas during the TAKE request: " + e.getMessage());
        }
    }

    @Override
    public void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
        try {
            // generate a random id for the operation
            String uuid = UUID.randomUUID().toString();

            // log the operation in the operation manager
            this.operationManager.logOperation(uuid, Operation.Type.GETTUPLESPACESTATE, "", request.getClientId());

            // wait until the operation can be performed for the client
            this.operationManager.debug("Checking if there are any constraints regarding sending the getTupleSpacesState request from client " + request.getClientId() + " to the replicas");
            this.operationManager.waitUntilCanPerformOperation(request.getClientId(), uuid);

            // send the GETTUPLESPACESSTATE request to each backend
            GetTupleSpacesStateBackendResponseCollector collector = new GetTupleSpacesStateBackendResponseCollector();
            int index = 0;
            for (TupleSpacesBackendStub stub : asyncStubs) {
                getTupleSpacesStateBackendRequest backendRequest = getTupleSpacesStateBackendRequest
                        .newBuilder()
                        .setOperationId(uuid)
                        .setClientId(request.getClientId())
                        .build();

                this.operationManager.debug("Sending the GETTUPLESPACESSTATE request from client " + request.getClientId() + " to replica " + index);
                stub.getTupleSpacesStateBackend(backendRequest, new GetTupleSpacesStateBackendResponseObserver(collector, this.debugMode));
                index++;
            }

            // wait for all backend responses to be received
            this.operationManager.debug("Waiting for all responses from replicas to the GETTUPLESPACESSTATE request from client " + request.getClientId());
            collector.waitUntilAllReceived(asyncStubs.size());

            // collect all the tuples from the backend responses
            List<String> tuples = new ArrayList<>();
            collector.getResponses().forEach(response -> tuples.addAll(response.getTupleList()));

            // send the result (all collected tuples) to the client
            this.operationManager.debug("Sending a GETTUPLESPACESSTATE response to client " + request.getClientId());
            responseObserver.onNext(getTupleSpacesStateResponse.newBuilder().addAllTuple(tuples).build());
            responseObserver.onCompleted();

            // remove the operation from the operation manager after completion
            this.operationManager.removeOperation(request.getClientId(), uuid);
        } catch (StatusRuntimeException e) {
            // handle any errors that occur and forward them to the client with a custom description
            System.err.println("At least 1 error received while waiting for responses from replicas to a GETTUPLESPACESSTATE request");
            Status status = e.getStatus();
            responseObserver.onError(status.withDescription("Server side error: " + e.getMessage()).asRuntimeException());
        }
    }

    private static Set<Integer> getVoterSet(int clientId) {
        return Set.of(clientId % 3, (clientId + 1) % 3);
    }

    private String getDelayFromContext(Integer serverId) {
        // map serverId to corresponding delay
        return switch (serverId) {
            case 0 -> FrontEndInterceptor.CONTEXT_DELAY_1.get();
            case 1 -> FrontEndInterceptor.CONTEXT_DELAY_2.get();
            case 2 -> FrontEndInterceptor.CONTEXT_DELAY_3.get();
            default -> "0";
        };
    }

    private TupleSpacesBackendStub createStubWithMetadata(Integer serverNum, String key, String value) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value);

        // return the stub with metadata interceptor attached
        return this.asyncStubs.get(serverNum)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }
}
