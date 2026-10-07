# CLAUDE.md — hmp-admin-api

Spring Boot API for the HMP admin **Doctor Invitations** feature (Admin ADM-003, spec p.113b / c / e).
Frontend: `../hmp-admin-web` (Next.js). Java 21 · Spring Boot 4.0 · JPA · Bean Validation · H2 in memory · Flyway.

## Commands
```bash
./mvnw spring-boot:run   # http://localhost:8080 — fresh H2 + 18 demo invitations on every start
./mvnw verify            # all tests; must pass before any hand-over
docker build -t hmp-admin-api .   # the image Render runs
```
A `java` process can outlive `mvnw` when the run is stopped; free port 8080 before starting again.

## Rules
- Controller → Service → Repository. Controllers only map HTTP.
- Status changes only through `Invitation` methods (they apply the rule and write history together).
  Allowed actions per status live in `InvitationStatus` (`canReissue`, `canRevoke`, `canEdit`) and must match the
  frontend `src/utils/invitationRules.ts`.
- Validation lives in `InvitationRequest`. Its rules, order (required → length → format) and **exact messages**
  are copied in the frontend `src/utils/invitationValidation.ts`. Change both together.
- Every error is a `ProblemDetail` with `code`, plain `detail` and `errors[{field, message}]` via
  `ApiExceptionHandler`. Add new cases to `ErrorCode` and to the web repo `docs/error-messages.md`.
  Never return exception text or stack traces.
- The full mobile number never appears in list responses (`maskedMobile` only). Search matches only
  `mobile_search_digits` (first 3 + last 4).
- Delete = revoke (spec: no hard delete).
- Schema changes = a new Flyway file `V<n>__<what>.sql`; `ddl-auto=validate` checks the entities against it.
- Use the `Clock` bean for "now" (truncated to seconds), never `Instant.now()` directly.
- Tests: business rules in `InvitationTest`; HTTP behaviour in `InvitationApiIntegrationTest`.

## Workflow
Read first, plan and wait for approval, small steps, run `./mvnw verify` after each change, never commit or push
unless asked.
