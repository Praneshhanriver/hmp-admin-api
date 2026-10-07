# HMP Admin API — Doctor Invitations

Spring Boot REST API behind the **Doctor Invitations** screens of the HMP Telemedicine admin web
(Admin ADM-003, spec p.113b / p.113c / p.113e). Built for the AI Frontend Training, Homework 2.
Frontend: [hmp-admin-web](https://github.com/Praneshhanriver/hmp-admin-web).

**Stack:** Java 21 · Spring Boot 4.0 (Web MVC, Data JPA, Validation, Actuator) · H2 in memory · Flyway · JUnit 5 + MockMvc

## Run it
Needs **Java 21** only. Maven comes with the project (`mvnw`).

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
# → http://localhost:8080/api/v1/admin/doctor-invitations
./mvnw verify                   # all tests
```
Every start creates the schema (Flyway) and **18 demo invitations** (6 Pending · 5 Used · 4 Expired · 3 Revoked) with
dates relative to "now", so the data always looks the same. Stopping the server forgets every change.

Docker (the same image Render runs):
```bash
docker build -t hmp-admin-api .
docker run -p 8080:8080 -e CORS_ALLOWED_ORIGINS=http://localhost:3000 hmp-admin-api
```

| Setting | Default | Meaning |
|---|---|---|
| `PORT` | 8080 | HTTP port (Render sets it) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated web origins allowed to call the API |
| `hmp.invitation.validity` | `P14D` | How long a link works (still TBC in the spec, S12 ▸ INVITATION) |
| `hmp.demo-data.enabled` | `true` | Load the 18 demo invitations into an empty database |

## Endpoints
Base path `/api/v1/admin/doctor-invitations`. Dates are ISO 8601 in Korean time (`2026-09-08T10:00:00+09:00`).

| Method | Path | What it does | Success | Errors |
|---|---|---|---|---|
| GET | `?keyword=&status=&page=1&size=20` | List with search (name, or the **visible** contact digits), status filter, paging (1-based), newest first. Mobile comes masked | 200 page | 400 |
| GET | `/{id}` | Detail + history (oldest first). Full contact, for the edit form | 200 | 404 |
| POST | `` | Issue invitation → Pending, 14-day link | 201 + `Location` | 400, 409 |
| PUT | `/{id}` | Edit a **Pending** invitation; sends a corrected link (re-issue count +1) | 200 | 400, 404, 409 |
| POST | `/{id}/reissue` | New link, back to Pending (not for Used) | 200 | 404, 409 |
| DELETE | `/{id}` | **Delete = revoke** (spec p.113c: no hard delete). Pending only; the row stays as Revoked | 200 | 404, 409 |
| GET | `/actuator/health` | Health check for Render | 200 | — |

Real response (`GET ?size=1`):
```json
{"content":[{"id":18,"doctorName":"Dr. Moon Hye-rin","maskedMobile":"010-****-9090","status":"pending",
  "issuedAt":"2026-10-07T16:12:45+09:00","expiresAt":"2026-10-21T16:12:45+09:00","reissueCount":0}],
 "page":1,"size":1,"totalElements":18,"totalPages":18}
```

Request body (POST / PUT):
```json
{ "doctorName": "Dr. Kim Han-mi", "email": "kim.hanmi@clinic.co.kr", "mobile": "010-1234-5678" }
```
| Field | Rules (same in the frontend form) |
|---|---|
| doctorName | required · 2–50 characters (after trimming) |
| email | required · ≤100 · `name@domain.tld` · stored lower-case · no second Pending invitation, not an already-registered (Used) doctor |
| mobile | required · Korean mobile `01[016789]` + 3–4 + 4 digits, hyphens optional · stored as `010-1234-5678` |

Errors are RFC 7807 `ProblemDetail` with a stable `code` and plain-language `detail` — never stack traces:
```json
{"status":409,"title":"Conflict","code":"INVITATION_ALREADY_PENDING",
 "detail":"An invitation for this email is already waiting to be used. Re-issue it from the list instead.",
 "errors":[{"field":"email","message":"An invitation for this email is already waiting to be used. Re-issue it from the list instead."}]}
```
Every code and message: `ErrorCode.java`, and the table in the web repo `docs/error-messages.md`.

## How it is built
```
src/main/java/com/hmp/admin/
  config/        InvitationProperties (validity, actor, zone), ClockConfig, WebConfig (CORS, ?status= converter)
  common/        ErrorCode, ApiException, ApiExceptionHandler (→ ProblemDetail)
  invitation/    Invitation (entity with the status rules), InvitationHistory, InvitationStatus, HistoryAction,
                 InvitationRequest (validation), InvitationResponses (Summary / Detail / PageResult),
                 InvitationRepository + InvitationSpecifications (search), InvitationService, InvitationController,
                 InvitationExpiryJob (every minute), DemoDataSeeder, MobileNumbers
src/main/resources/db/migration/   V1__create_invitation_tables.sql
```
- Controller → Service → Repository. **Every state change is a method on `Invitation`** (`issue`, `reissue`,
  `edit`, `revoke`, `markUsed`, `expireIfDue`), which also writes the history line, so a rule can't be skipped.
- Contact privacy: the list never returns the full number; search uses a stored `mobile_search_digits`
  (first 3 + last 4), so searching cannot reveal the masked middle digits.
- Expiry: a scheduled job marks Pending invitations past `expiresAt` as Expired, recorded at the expiry moment as a
  system event (no actor).
- `Clock` is a bean, so tests run with a fixed "now".
- No login in the homework: admin actions are recorded as `admin@hmp.co.kr`.

## Tests — 34, all passing (`./mvnw verify`)
| Class | Kind | Covers |
|---|---|---|
| `InvitationApiIntegrationTest` (13) | Full HTTP → DB with MockMvc, fixed clock | create → list → edit → delete → re-issue; every validation message; 409 conflicts; 404; malformed JSON; search by name and visible digits only (digits inside a name are not a contact search); status filter + paging; expiry job |
| `InvitationTest` (6) | Unit | Status rules and history of the entity |
| `MobileNumbersTest` (14) | Unit | Accepted / rejected numbers, normalising, masking |
| `HmpAdminApiApplicationTests` (1) | Start-up | Flyway schema matches the entities; 18 demo rows in every status |

## Deploy (Render, free)
New → Web Service → this repo → Runtime **Docker** → env `CORS_ALLOWED_ORIGINS=https://<your-vercel-app>.vercel.app`
→ Health check path `/actuator/health`. The free service sleeps when idle; the first call can take about a minute,
and a restart brings back the 18 demo invitations.
