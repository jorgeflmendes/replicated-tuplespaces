package pt.ulisboa.tecnico.tuplespaces.server.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ServerStateTest {

    @Test
    void putShouldMakeTupleVisibleInState() {
        ServerState serverState = new ServerState();

        serverState.put("<a>");

        assertIterableEquals(List.of("<a>"), serverState.getState());
    }

    @Test
    void readShouldReturnMatchingTuple() {
        ServerState serverState = new ServerState();
        serverState.put("<value>");

        String result = serverState.read("<value>");

        assertEquals("<value>", result);
    }

    @Test
    void takeShouldWaitUntilTupleBecomesAvailable() {
        ServerState serverState = new ServerState();

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> serverState.take("<late>"));
            Thread.sleep(150);
            serverState.put("<late>");
            assertEquals("<late>", getUnchecked(future));
        });
    }

    @Test
    void lockPatternShouldTrackMatchingTuplesUntilUnlock() {
        ServerState serverState = new ServerState();
        serverState.put("<alpha>");

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            serverState.lockPattern("<alpha>", "op-1");
            List<String> matchingTuples = serverState.getMatchingTuples("<alpha>");
            assertIterableEquals(List.of("<alpha>"), matchingTuples);
            serverState.unlockPattern("op-1");
        });
    }

    private static <T> T getUnchecked(CompletableFuture<T> future) throws InterruptedException, ExecutionException {
        return future.get();
    }
}
