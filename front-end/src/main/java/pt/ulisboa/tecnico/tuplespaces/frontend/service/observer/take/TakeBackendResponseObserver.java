package pt.ulisboa.tecnico.tuplespaces.frontend.service.observer.take;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesBackendOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.collector.take.TakeBackendResponseCollector;

public class TakeBackendResponseObserver implements StreamObserver<TakeBackendResponse> {
    TakeBackendResponseCollector collector;
    boolean debugMode;

    public TakeBackendResponseObserver(TakeBackendResponseCollector collector, boolean debugMode) {
        this.collector = collector;
        this.debugMode = debugMode;
    }

    @Override
    public void onNext(TakeBackendResponse r) {
        if (debugMode) {
            System.out.println("[DEBUG] Received a take response from one of the backend servers.");
        }

        collector.addResponse(r);
    }

    @Override
    public void onError(Throwable throwable) {
        collector.addError((StatusRuntimeException) throwable);
    }

    @Override
    public void onCompleted() {
        // Currently, no action is required here, but this method could be used
    }
}
