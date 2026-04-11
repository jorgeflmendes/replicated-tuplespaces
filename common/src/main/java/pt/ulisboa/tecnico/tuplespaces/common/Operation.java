package pt.ulisboa.tecnico.tuplespaces.common;

public class Operation {
    private final long timestamp; // Store the timestamp
    private final Type type;
    private final String pattern;
    private final String id;

    public enum Type {
        READ,
        PUT,
        TAKE,
        GETTUPLESPACESTATE
    }

    public Operation(String id, Type type, String pattern) {
        this.id = id;
        this.type = type;
        this.pattern = pattern;
        this.timestamp = System.nanoTime();
    }

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Type getType() {
        return type;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return type.toString() + " <" + pattern + ">";
    }
}
