# REVIEW.md

## Purpose
This document records the review criteria applied to selected high-risk parts of the codebase and summarises issues identified and follow-up actions.

## Review scope
The review focused on architectural boundary code where faults are most likely to surface:
- REST controller layer (request validation, response correctness, error handling)
- WebSocket progress streaming/handler layer (event delivery, connection lifecycle, failure handling)
- Route computation/service layer (deterministic logic, boundary conditions)

## Review criteria (checklist)
1. **Correctness & invariants**: logic preserves key invariants (e.g., valid route output, consistent progress semantics).
2. **Input validation**: invalid/malformed inputs are rejected cleanly with predictable responses.
3. **Failure handling**: transport/service failures are handled explicitly; resources are cleaned up.
4. **State consistency**: state updates remain consistent across REST requests and WebSocket events.
5. **Observability**: sufficient signals exist to diagnose faults (structured logs/metrics, not ad-hoc prints).
6. **Testability**: dependencies are injectable/mockable; behaviour is assertable at unit/integration level.
7. **Concurrency safety**: shared state is protected; no reliance on unspecified event ordering.
8. **Clarity & maintainability**: readable naming, minimal duplication, clear separation of concerns.

## Issues identified
- **WebSocket error paths are not naturally exercised by nominal progress streaming** (e.g., transport errors), so they require explicit failure-injection scenarios in tests.
- **Progress events lack systematic sequencing/timestamps**, limiting precise assertions about event ordering and latency.
- **Diagnostics in failure handling are partially ad-hoc**; structured logging would improve repeatability and correlation in CI logs.
- **Qualitative visualisation properties** (clarity/smoothness/interpretability) are not fully automatable and require a minimal, repeatable manual inspection checklist.

## Follow-up actions
- Add sequence numbers and/or timestamps to progress events to support ordering/latency assertions.
- Extend integration/system tests with failure-injection scenarios (transport errors, disconnect/reconnect) and assert progress-stream invariants.
- Replace ad-hoc diagnostics with structured logging where appropriate to improve CI traceability.
- Maintain a short UI inspection checklist for qualitative visualisation requirements.
