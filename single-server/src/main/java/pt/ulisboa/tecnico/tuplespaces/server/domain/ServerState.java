package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ServerState {
    private final CopyOnWriteArrayList<String> tuples; // stores the list of tuples
    private final ConcurrentHashMap<String, PatternLock> locks; // maps operation ids to their locked pattern and locked tuples

    public ServerState() {
        this.tuples = new CopyOnWriteArrayList<>();
        this.locks = new ConcurrentHashMap<>();
    }

    public synchronized void put(String tuple) {
        synchronized (tuples) {
            synchronized (locks) {
                // check if any lock pattern matches the tuple and add the tuple to the corresponding lock
                locks.entrySet().stream()
                        .filter(entry -> tuple.matches(entry.getValue().getPattern()))
                        .findFirst()
                        .ifPresent(entry -> entry.getValue().addValue(tuple));
            }

            // add the tuple to the tuples collection and notify all waiting threads
            tuples.add(tuple);
            tuples.notifyAll();
        }
    }

    public String read(String pattern) {
        synchronized (tuples) {
            // wait until a matching tuple is found
            while (getMatchingTuple(pattern) == null) {
                waitFor(tuples);
            }

            return getMatchingTuple(pattern);
        }
    }

    public String take(String tuple) {
        synchronized (this.tuples) {
            // wait until a matching tuple is removed
            while (!this.tuples.remove(tuple)) {
                waitFor(this.tuples);
            }

            return tuple;
        }

        // no need to remove the tuples here as unlockPattern will be called later
    }

    public List<String> getState() {
        synchronized (this.tuples) {
            return new CopyOnWriteArrayList<String>(this.tuples);
        }
    }

    public void lockPattern(String patternToCheck, String operationId) {
        synchronized (locks) {
            // wait until the pattern is available
            while (shouldWaitForPattern(patternToCheck, operationId)) {
                waitFor(locks);
            }

            // lock the pattern for the given operation
            locks.put(operationId, new PatternLock(patternToCheck));
            synchronized (tuples) {
                // add matching tuples to the lock
                tuples.stream()
                        .filter(tuple -> tuple.matches(patternToCheck))
                        .forEach(tuple -> locks.get(operationId).addValue(tuple));
            }
        }
    }


    public void unlockPattern(String operationId) {
        synchronized (locks) {
            locks.remove(operationId); // removes all the locks associated with the given operation
            locks.notifyAll();
        }
    }

    public List<String> getMatchingTuples(String pattern) {
        synchronized (tuples) {
            List<String> matchingTuples;

            // wait until there are matching tuples
            while ((matchingTuples = getMatchingTuplesFromStream(pattern)).isEmpty()) {
                waitFor(tuples);
            }

            return matchingTuples;
        }
    }


    // returns all tuples that match the given pattern
    private List<String> getMatchingTuplesFromStream(String pattern) {
        return tuples.stream()
                .filter(tuple -> tuple.matches(pattern)) // filters elements that match the pattern
                .collect(Collectors.toList());
    }

    // returns the first tuple that matches the given pattern
    private String getMatchingTuple(String pattern) {
        return tuples.stream()
                .filter(tuple -> tuple.matches(pattern)) // filters elements that match the pattern
                .findFirst()
                .orElse(null); // returns null if no match is found
    }

    // checks if a given pattern is locked by another operation and should wait
    private boolean shouldWaitForPattern(String patternToCheck, String operationId) {
        return locks.entrySet().stream()
                .filter(entry -> !operationId.equals(entry.getKey())) // ignores the current operation
                .flatMap(entry -> entry.getValue().getTuples().stream()) // all locked tuples
                .anyMatch(lockedTuple -> lockedTuple.matches(patternToCheck)); // if any locked tuple matches the given pattern
    }


    private void waitFor(Object object) {
        try {
            object.wait();
        } catch (InterruptedException e) {
            // Professor said to do this to keep the thread waiting after interruption.
            System.err.println("Thread was interrupted, but will continue waiting.");
        }
    }
}
