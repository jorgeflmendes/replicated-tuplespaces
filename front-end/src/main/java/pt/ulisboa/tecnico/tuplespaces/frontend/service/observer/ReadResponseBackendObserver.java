package pt.ulisboa.tecnico.tuplespaces.frontend.service.observer;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesBackendOuterClass.*;
import pt.ulisboa.tecnico.tuplespaces.frontend.service.collector.ReadBackendResponseCollector;

import java.awt.*;

public class ReadResponseBackendObserver implements StreamObserver<ReadBackendResponse> {
    ReadBackendResponseCollector collector;
    boolean debugMode;

    public ReadResponseBackendObserver(ReadBackendResponseCollector collector, boolean debugMode) {
        this.collector = collector;
        this.debugMode = debugMode;
    }

    @Override
    public void onNext(ReadBackendResponse r) {
        if (debugMode) {
            System.out.println("[DEBUG] Received a read response from one of the backend servers.");
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
