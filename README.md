# TupleSpaces

[![Java](https://img.shields.io/badge/Java-17-informational?logo=openjdk)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Build-Maven%203.8%2B-blue?logo=apachemaven)](https://maven.apache.org/)
[![gRPC](https://img.shields.io/badge/RPC-gRPC-00bfa5?logo=grpc)](https://grpc.io/)
[![CI](https://img.shields.io/badge/CI-GitHub_Actions-black?logo=githubactions)](.github/workflows/ci.yml)

Distributed Systems course project for Instituto Superior Tecnico (IST), focused on a replicated TupleSpaces implementation built with Java, gRPC, and a complementary Python client.

`distributed-systems`, `tuplespaces`, `grpc`, `protobuf`, `java`, `python`, `maven`, `ist`, `tecnico-lisboa`

## Overview

This repository is organized as a multi-module Maven project with clear separation between API contracts, shared logic, backend services, and client applications. The system exposes a Linda-style TupleSpaces API and coordinates distributed `take` operations through a quorum-based approach inspired by Maekawa's mutual exclusion algorithm.

### Key capabilities

- TupleSpaces API defined with Protocol Buffers and gRPC.
- Support for `put`, `read`, `take`, and `getTupleSpacesState`.
- Pattern-based tuple matching for blocking `read` and coordinated `take`.
- Replicated backend coordination through a Maekawa-style voter set strategy.
- Java server and front-end services structured as independent modules.
- Java and Python command-line clients for interacting with the system.
- Deterministic build pipeline with GitHub Actions CI.

## Distributed Model

The runtime is split between a client-facing front-end and multiple backend replicas:

- The `front-end` receives client operations and orchestrates replica communication.
- The `single-server` module implements the backend state machine used by each replica.
- `put` is broadcast to all replicas.
- `read` waits until at least one replica returns a matching tuple.
- `take` is executed in two phases:
  phase 1 collects candidate tuples from a voter set;
  phase 2 removes the chosen tuple from all replicas.

For `take`, the front-end computes a voter set and intersects the tuples returned by those replicas before selecting the tuple to remove. This is the core of the repository's Maekawa-style coordination approach.

## Tuple Semantics

The system works over string tuples such as:

```text
<hello,world>
<user,42,active>
<order,123,pending>
```

Supported operations:

- `put` inserts a tuple into the replicated tuple space.
- `read` blocks until a matching tuple is available.
- `take` blocks until a tuple can be safely selected and removed.
- `getTupleSpacesState` returns the current visible tuple collections from the replicas.

### Pattern matching

Tuple matching is pattern-based. Internally, tuple selection relies on Java string pattern matching (`String.matches(...)`), so search expressions can be used to describe classes of tuples rather than only exact values.

Examples:

```text
read <hello,world>
read <user,.*>
take <order,.*,pending>
```

This allows the tuple space to support both exact tuple access and more expressive searches over structured tuple strings.

## Repository Structure

```text
.
|-- contract/        # Protobuf definitions and generated gRPC artifacts
|-- common/          # Shared Java utilities and operation coordination primitives
|-- client-java/     # Java command-line client
|-- client-python/   # Python command-line client package and compatibility entrypoints
|-- single-server/   # TupleSpaces backend server
|-- front-end/       # Front-end aggregation and coordination layer
|-- docs/            # Architecture and developer documentation
|-- tests/           # Command script samples used during manual validation
`-- .github/         # CI workflow, templates, and collaboration metadata
```

## Architecture

The system is split into five primary components:

1. `contract`
   Contains the protobuf service definitions and code generation pipeline for Java and Python.
2. `common`
   Holds shared Java abstractions used across runtime services.
3. `single-server`
   Implements the TupleSpaces backend state and request handling logic.
4. `front-end`
   Exposes the client-facing service and orchestrates backend communication.
5. `client-java` and `client-python`
   Provide interactive command-line interfaces for submitting operations.

More detail is available in [docs/ARCHITECTURE.md](/C:/Users/Jorge/Downloads/T49-Tuplespaces-2025-master/T49-Tuplespaces-2025-master/docs/ARCHITECTURE.md).

### Coordination details

- The front-end generates an operation identifier for every request.
- Backend replicas track pending operations and local locking state.
- Pattern locks prevent conflicting `take` requests from choosing incompatible tuples concurrently.
- The front-end only finalizes a `take` after it finds a non-empty intersection across the voter responses.

## Prerequisites

- Java 17
- Maven 3.8+
- Python 3.10+ for the Python client

Check your environment with:

```bash
javac -version
mvn -version
python --version
```

## Build

Compile all Java modules and generate the gRPC artifacts:

```bash
mvn clean install
```

Run the verification pipeline:

```bash
mvn test
```

## Running the System

### Start the backend server

```bash
cd single-server
mvn exec:java -Dexec.args="3001"
```

### Start the front-end

```bash
cd front-end
mvn exec:java -Dexec.args="2001 localhost:3001 localhost:3002 localhost:3003"
```

### Run the Java client

```bash
cd client-java
mvn exec:java -Dexec.args="localhost:2001 1"
```

### Run the Python client

Install dependencies:

```bash
python -m pip install -r client-python/requirements.txt
```

Run the client:

```bash
python client-python/client_main.py localhost:2001 1
```

## Command Examples

```text
put <hello,world>
read <hello,world>
take <hello,world>
getTupleSpacesState
sleep 2
exit
```

## Documentation

- [Architecture Notes](/C:/Users/Jorge/Downloads/T49-Tuplespaces-2025-master/T49-Tuplespaces-2025-master/docs/ARCHITECTURE.md)

## References

- [Distributed Systems Project Statement](https://github.com/tecnico-distsys/Tuplespaces-2025)
- [gRPC Documentation](https://grpc.io/docs/)
- [Apache Maven](https://maven.apache.org/)
