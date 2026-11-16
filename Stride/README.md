# Stride 2.0

One sign-in for the college sites students use every day (Gradebook, Moodle, PrimeTimetable,
the digital library and ThutoAcademics), plus the features the Stride report specified:
rotating-QR multi-factor attendance, role-based pages for students, lecturers and administrators,
a database, notices, a study planner, a grounded study assistant, a campus map, five management
reports and single sign-on.

Java 17+, Jakarta Servlet 6.1 / JSP / JSTL 3, Tomcat 11, Maven. Embedded H2 database by default
(nothing to install); MySQL 8 with one setting change.

---

## Run it

### Option A: one click, no Tomcat setup (embedded Tomcat)
1. **File > Open** the `Stride` folder in IntelliJ and let Maven import `pom.xml`.
2. Choose the run configuration **Stride (embedded Tomcat)** (top right) and press Run.
   (Or right-click `src/test/java/com/stride/DevServer.java` and choose **Run 'DevServer.main()'**.)
3. Open http://localhost:8080/

### Option B: your own Tomcat 11 (as before)
1. **Run > Edit Configurations > + > Tomcat Server > Local**, pick Tomcat 11.
2. **Deployment** tab: **+ > Artifact > Stride:war exploded**. Any application context works
   (`/` or `/Stride_war`); links follow the context automatically (this resolves defect D4).
3. Run, then open the URL IntelliJ shows.

The first start creates the database in `~/stride-data/` (your home folder) and loads the sample
data. To start again from fresh sample data, stop Stride and delete that folder.

### Demo accounts (made up, for testing only)

| Role | Email | Password |
|---|---|---|
| Student | student@stride.test | student123 |
| Lecturer | lecturer@stride.test | lecturer123 |
| Administrator | admin@stride.test | admin123 |

Every other sample student (for example `tumelo.molefe@stride.test`) uses `student123`; the other
lecturers (`neo.mokoena@stride.test`, `kagiso.sebina@stride.test`) use `lecturer123`.

### Try the attendance engine (two browser windows)
1. Window 1: sign in as the **lecturer**, open **Take attendance**, press **Open check-in** on a
   class that has not finished (or use **Extra class** at any time). Press **Show on projector**.
2. Window 2 (a private window or another browser, or your phone on the same network): sign in as
   the **student**, open **Check in**, type the code from the projector (or scan the QR with a phone).
3. Back in window 1 the count goes up live. Check in from a second student account using the same
   browser to see the shared-device flag, then **Accept** or **Change record** with a reason.

To reproduce the report's screenshots exactly, run with the clock frozen at the report's moment:
add `-Dstride.clock.fixed=2026-10-29T15:27` to the VM options.

---

## What is in this version (mapped to the report)

| Requirement | What you get |
|---|---|
| RQ01, RQ03, RQ04 | Sign-in, sign-out, every page guarded by one filter |
| **RQ02 (was partly met)** | Different menus, home pages and permissions for Student, Lecturer and Administrator, checked on every request (`AuthorizationFilter`) and again in the services |
| RQ05-RQ09 | Home tiles, safe links, search (now also finds your modules), today's classes, free rooms |
| RQ10, RQ11 | Notices and attendance now come from the database; unread counts; attendance is computed from real check-ins |
| RQ12-RQ14 | Help, text size, friendly error pages (400/403/404/405/500) |
| **RQ15** Open attendance session | Lecturer opens check-in for a timetabled or extra class; projector page with a QR image and a 6-character code that rotates every 30 s (HMAC of a per-session secret and the time window, like an authenticator app) |
| **RQ16** Check in | Student scans the QR or types the code; the code, the session window, the enrolment and duplicates are all validated **before** anything is saved; the database key (session, student) also rejects duplicates |
| **RQ17** Multi-factor confidence | Each check-in gets a 0-100 score from independent signals: rotating code / QR, campus network (configurable IP ranges), device (a hashed device cookie catches one phone checking in several students), optional campus location. Low scores are flagged for the lecturer, never silently accepted |
| **RQ18** Correct attendance | Lecturer changes a record to Present/Late/Absent/Excused with a required reason; accepts flagged check-ins; every change goes to the audit log; students cannot |
| **RQ19** Post announcements | Lecturers post to modules they teach; administrators post to any module or everyone, and can pin |
| **RQ20** Manage users | Create accounts (one-time temporary password, must be changed at first sign-in), edit, reset password, switch off |
| **RQ21** Manage modules | Create modules, assign lecturers, enrol students by pasting student numbers or e-mails |
| **RQ22** Manage timetable | Add/remove classes with clash checks: room, lecturer, shared students, opening hours, room size |
| **RQ23** Study schedule | Rule-based planner: earliest deadline first, whole free hours around your classes, max 2 h/day per assessment, warnings for shortfalls and 48-hour deadline collisions; download to your phone calendar (.ics) |
| **RQ24** Campus map and resources | Schematic SVG campus map with free-room counts and a full text alternative; module pages with resources, assessments and notices |
| **RQ25** Study assistant | Plain-language questions answered only from the student's own data (next class, attendance, deadlines, notices, free rooms, where a room is). Unknown questions get an honest "I can't answer that". Checked against a fixed set of test queries |
| **RQ26** Persistent storage | JDBC data-access classes over H2 (default) or MySQL 8, same schema file, foreign keys and unique keys as in the ERD |
| **RQ27** Single sign-on | OpenID Connect with PKCE (works with Microsoft Entra ID and similar). Off until the college gives client details; see `stride.properties` |
| **RP01-RP05** Reports | Attendance by module, students at risk, check-in assurance, room use, weekly trend: each with a chart, a table and a CSV download for Excel |

Also new: week timetable with calendar download, "My modules" pages, lecturer coursework editing,
an audit log, editable college-site addresses, password change, and a phone-friendly tab bar.

### Defects from the report (Table 5.5)

| Defect | Status |
|---|---|
| D1 missing spaces on the home page | Fixed (the cause was `trim-directive-whitespaces`; now off) and tested |
| D2 contradictory sample notice | Fixed |
| D3 ThutoAcademics address | Administrators can now correct any site address under **College sites**; please confirm the real address |
| D4 README vs context path | Works under any context path; README updated |
| D5 throttle by e-mail only | Now also per source address and a server-wide limit per minute |
| D6 roles not differentiated | Fixed (RQ02 above) |
| D7 no Secure cookie flag | `security.cookie.secure=true` turns on the Secure flag and HSTS when you serve over HTTPS |
| D8 no servlet-level tests | `WebIntegrationTest` drives the real filters, controllers and JSPs on an embedded Tomcat over HTTP |

---

## Configuration (`src/main/resources/stride.properties`)

Every key can be overridden without editing the file: `-Dstride.db.url=...` as a VM option, or an
environment variable such as `STRIDE_DB_URL`. The main settings:

- **Database:** H2 file by default. For MySQL 8 run `src/main/resources/db/mysql/create-database.sql`,
  then set `db.url`, `db.user`, `db.password` (example lines are in the file). Tables and sample
  data are created on first start.
- **Campus network:** `campus.networks` lists the college's IP ranges for the network factor.
  The defaults cover localhost and private networks so the demo works on one machine.
- **Location factor:** set `campus.latitude`, `campus.longitude` and `campus.radius.metres` to switch it on.
- **Attendance:** code lifetime, how long check-in stays open, the late threshold, the flag
  threshold and the at-risk percentage.
- **Security:** PBKDF2 rounds, throttling limits, `security.cookie.secure`.
- **Single sign-on:** `sso.*`.

## Tests

`mvn test` runs 112 JUnit tests:
- `ReportCasesTest`: the 49 cases from the report's Appendix B (T01-T49), now against the real classes and database.
- `AttendanceServiceTest`: opening sessions, valid/invalid/expired codes, QR tokens, duplicates,
  closed sessions, enrolment, off-campus and shared-device flags, corrections and permissions.
- `FeaturesTest`: planner rules, the assistant's fixed test queries, all five reports, notices,
  account administration, timetable clash detection, enrolment, safe links.
- `SecurityPiecesTest`, `OidcClientTest` (against a fake identity provider).
- `WebIntegrationTest`: security headers, redirects, one sign-in error message, CSRF on every
  POST, role enforcement, every page rendering for every role, the complete lecturer-to-student
  check-in flow over HTTP, CSV downloads, the forced password change, sign-out and error pages.

## Structure

```
src/main/java/com/stride/
  config/      App (wires everything), AppConfig, AppListener (start-up)
  db/          Database (connection pool), Jdbc helpers, SchemaInstaller, Seeder (sample data)
  dao/         one data-access class per area; every query is a PreparedStatement
  model/       plain data classes
  service/     the rules: accounts, schedule, attendance engine, confidence scoring, notices,
               curriculum/timetable, study planner, assistant, calendar export
  report/      the five reports and chart geometry
  security/    PasswordHasher, CsrfTokens, LoginThrottle, SignInGuard, RotatingCode,
               NetworkMatcher, OidcClient
  filter/      SecurityHeaders -> Authentication -> Csrf -> Authorization (in that order)
  controller/  one servlet per area; views receive only the data they need
  web/         BaseController, Flash messages
  util/        SessionKeys, Json, QrCodes
src/main/resources/  stride.properties, db/schema.sql, db/mysql/create-database.sql
src/main/webapp/     WEB-INF/views (JSP, no Java code allowed), WEB-INF/tags, assets
src/test/java/       tests, DevServer (embedded Tomcat), sample fixtures
```

## Security practices
- Passwords: salted PBKDF2-HMAC-SHA256 (600,000 rounds), constant-time comparison, equal timing
  for unknown e-mails, automatic re-hash if the round count is raised, NIST-style password rules.
- Sessions: new id at sign-in, HttpOnly + SameSite=Lax cookie, Secure flag by configuration,
  30-minute timeout, account re-checked on every request (switching an account off takes effect at once).
- CSRF token on every POST; sign-out and every change are POST-only.
- Brute force: per e-mail, per address and server-wide limits; wrong attendance codes are limited per student.
- Output escaped with `<c:out>`; stored links must be http(s); CSV downloads are protected against formula injection.
- Content-Security-Policy with no inline script or style; charts and the map are plain SVG.
- Audit log of sign-ins, attendance changes and administrative actions.
- Privacy: device ids are stored only as SHA-256 hashes; location (if switched on) is used once and not stored.

## Before real use
- Serve over HTTPS and set `security.cookie.secure=true`.
- Point `db.url` at the college MySQL server and set `db.seed=false`.
- Replace `campus.networks` with the college's real address ranges.
- Confirm each college site address under **College sites**.
- Ask the college for single sign-on client details, or create real accounts.
- The campus map layout is a sample; edit the `buildings` table to match the real campus.
- Have someone outside the team review the security before real student data is used.
