# Architecture

## System Layout

The project follows a layered distributed-systems organization:

- `contract` defines the service interface and message schemas through Protocol Buffers.
- `front-end` acts as the public-facing coordination layer.
- `single-server` encapsulates backend state management and request execution.
- `common` contains Java utilities shared across service modules.
- `client-java` and `client-python` offer operational CLI clients.

## Request Flow

1. A client submits an operation such as `put`, `read`, `take`, or `getTupleSpacesState`.
2. The front-end receives the request and coordinates communication with backend replicas.
3. Backend services validate input, apply scheduling/delay rules, and mutate or query state.
4. The result is returned to the client through gRPC.

## Module Boundaries

### contract

- Owns `.proto` files.
- Generates Java and Python gRPC artifacts.
- Centralizes API compatibility.

### common

- Holds shared domain primitives such as operation metadata.
- Avoids cross-module duplication in the Java runtime.

### single-server

- Owns tuple state.
- Handles tuple locking, waiting, and consistency rules.
- Implements backend-facing service methods.

### front-end

- Exposes the service used by clients.
- Aggregates backend responses.
- Applies orchestration logic across replicas.

### Clients

- Provide a command-processing shell.
- Validate user input before invoking remote procedures.
- Surface gRPC failures through higher-level errors.
