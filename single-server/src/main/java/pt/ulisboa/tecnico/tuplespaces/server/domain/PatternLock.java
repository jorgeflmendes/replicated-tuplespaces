package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;

class PatternLock {
    private final String pattern;
    private final List<String> tuples;

    public PatternLock(String pattern) {
        this.pattern = pattern;
        this.tuples = new ArrayList<>();
    }

    public String getPattern() {
        return pattern;
    }

    public List<String> getTuples() {
        return tuples;
    }

    public void addValue(String value) {
        tuples.add(value);
    }
}
