-- Stride database schema (RQ26).
-- Written in the common subset of MySQL 8 and H2 (MODE=MySQL), so the same file builds both.
-- Applied automatically on first start when the users table does not exist yet.
-- Times are stored as campus local time (Africa/Gaborone) in DATETIME columns.

CREATE TABLE IF NOT EXISTS users (
    id                   INT          NOT NULL AUTO_INCREMENT,
    first_name           VARCHAR(80)  NOT NULL,
    last_name            VARCHAR(80)  NOT NULL,
    email                VARCHAR(190) NOT NULL,
    role                 VARCHAR(20)  NOT NULL,
    student_number       VARCHAR(30)  NULL,
    password_hash        VARCHAR(200) NULL,
    must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           DATETIME     NOT NULL,
    last_login_at        DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('STUDENT', 'LECTURER', 'ADMINISTRATOR'))
);

CREATE TABLE IF NOT EXISTS buildings (
    id          INT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(80)  NOT NULL,
    code        VARCHAR(10)  NOT NULL,
    description VARCHAR(400) NOT NULL,
    access_note VARCHAR(400) NULL,
    map_x       INT          NOT NULL,
    map_y       INT          NOT NULL,
    map_w       INT          NOT NULL,
    map_h       INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_buildings_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS rooms (
    id          INT         NOT NULL AUTO_INCREMENT,
    name        VARCHAR(60) NOT NULL,
    building_id INT         NOT NULL,
    capacity    INT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_rooms_name UNIQUE (name),
    CONSTRAINT fk_rooms_building FOREIGN KEY (building_id) REFERENCES buildings (id),
    CONSTRAINT ck_rooms_capacity CHECK (capacity > 0)
);

CREATE TABLE IF NOT EXISTS modules (
    id          INT          NOT NULL AUTO_INCREMENT,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(600) NULL,
    lecturer_id INT          NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_modules_code UNIQUE (code),
    CONSTRAINT fk_modules_lecturer FOREIGN KEY (lecturer_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS enrolments (
    student_id  INT      NOT NULL,
    module_id   INT      NOT NULL,
    enrolled_at DATETIME NOT NULL,
    PRIMARY KEY (student_id, module_id),
    CONSTRAINT fk_enrol_student FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_enrol_module FOREIGN KEY (module_id) REFERENCES modules (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS timetable_entries (
    id          INT     NOT NULL AUTO_INCREMENT,
    module_id   INT     NOT NULL,
    room_id     INT     NOT NULL,
    day_of_week TINYINT NOT NULL,
    start_time  TIME    NOT NULL,
    end_time    TIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tt_module FOREIGN KEY (module_id) REFERENCES modules (id) ON DELETE CASCADE,
    CONSTRAINT fk_tt_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT ck_tt_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_tt_order CHECK (start_time < end_time)
);

CREATE TABLE IF NOT EXISTS attendance_sessions (
    id                 INT         NOT NULL AUTO_INCREMENT,
    module_id          INT         NOT NULL,
    room_id            INT         NULL,
    timetable_entry_id INT         NULL,
    lecturer_id        INT         NOT NULL,
    session_date       DATE        NOT NULL,
    starts_at          DATETIME    NOT NULL,
    opened_at          DATETIME    NOT NULL,
    closes_at          DATETIME    NOT NULL,
    closed_at          DATETIME    NULL,
    secret             VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_as_module FOREIGN KEY (module_id) REFERENCES modules (id) ON DELETE CASCADE,
    CONSTRAINT fk_as_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT fk_as_entry FOREIGN KEY (timetable_entry_id) REFERENCES timetable_entries (id) ON DELETE SET NULL,
    CONSTRAINT fk_as_lecturer FOREIGN KEY (lecturer_id) REFERENCES users (id)
);

-- One row per student per session. The key on (session_id, student_id) makes the database
-- reject a duplicate check-in as well as the service layer (Section 3.5).
CREATE TABLE IF NOT EXISTS check_ins (
    id                INT          NOT NULL AUTO_INCREMENT,
    session_id        INT          NOT NULL,
    student_id        INT          NOT NULL,
    checked_in_at     DATETIME     NOT NULL,
    method            VARCHAR(10)  NOT NULL,
    status            VARCHAR(10)  NOT NULL,
    confidence        INT          NOT NULL,
    factors           VARCHAR(300) NOT NULL,
    client_ip         VARCHAR(64)  NULL,
    device_hash       VARCHAR(64)  NULL,
    flagged           BOOLEAN      NOT NULL DEFAULT FALSE,
    corrected_by      INT          NULL,
    correction_reason VARCHAR(300) NULL,
    corrected_at      DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_checkin UNIQUE (session_id, student_id),
    CONSTRAINT fk_ci_session FOREIGN KEY (session_id) REFERENCES attendance_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_ci_student FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ci_corrector FOREIGN KEY (corrected_by) REFERENCES users (id),
    CONSTRAINT ck_ci_method CHECK (method IN ('CODE', 'QR', 'MANUAL')),
    CONSTRAINT ck_ci_status CHECK (status IN ('PRESENT', 'LATE', 'ABSENT', 'EXCUSED')),
    CONSTRAINT ck_ci_conf CHECK (confidence BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS announcements (
    id        INT           NOT NULL AUTO_INCREMENT,
    title     VARCHAR(160)  NOT NULL,
    body      VARCHAR(4000) NOT NULL,
    author_id INT           NOT NULL,
    module_id INT           NULL,
    pinned    BOOLEAN       NOT NULL DEFAULT FALSE,
    posted_at DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_an_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_an_module FOREIGN KEY (module_id) REFERENCES modules (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS announcement_reads (
    user_id         INT      NOT NULL,
    announcement_id INT      NOT NULL,
    read_at         DATETIME NOT NULL,
    PRIMARY KEY (user_id, announcement_id),
    CONSTRAINT fk_ar_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ar_ann FOREIGN KEY (announcement_id) REFERENCES announcements (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS assessments (
    id        INT          NOT NULL AUTO_INCREMENT,
    module_id INT          NOT NULL,
    title     VARCHAR(160) NOT NULL,
    kind      VARCHAR(12)  NOT NULL,
    due_at    DATETIME     NOT NULL,
    weight    INT          NOT NULL,
    est_hours INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_asmt_module FOREIGN KEY (module_id) REFERENCES modules (id) ON DELETE CASCADE,
    CONSTRAINT ck_asmt_kind CHECK (kind IN ('ASSIGNMENT', 'TEST', 'EXAM', 'PROJECT', 'LAB')),
    CONSTRAINT ck_asmt_weight CHECK (weight BETWEEN 0 AND 100),
    CONSTRAINT ck_asmt_hours CHECK (est_hours BETWEEN 1 AND 200)
);

CREATE TABLE IF NOT EXISTS resources (
    id        INT          NOT NULL AUTO_INCREMENT,
    module_id INT          NOT NULL,
    title     VARCHAR(160) NOT NULL,
    url       VARCHAR(500) NOT NULL,
    kind      VARCHAR(12)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_res_module FOREIGN KEY (module_id) REFERENCES modules (id) ON DELETE CASCADE
);

-- The college sites Stride links to. Kept in the database so an administrator can fix an
-- address without a code change (defect D3).
CREATE TABLE IF NOT EXISTS college_sites (
    slug        VARCHAR(20)  NOT NULL,
    name        VARCHAR(60)  NOT NULL,
    task        VARCHAR(60)  NOT NULL,
    category    VARCHAR(40)  NOT NULL,
    description VARCHAR(300) NOT NULL,
    keywords    VARCHAR(300) NOT NULL,
    url         VARCHAR(500) NOT NULL,
    sort_order  INT          NOT NULL,
    PRIMARY KEY (slug)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id        INT          NOT NULL AUTO_INCREMENT,
    logged_at DATETIME     NOT NULL,
    actor_id  INT          NULL,
    action    VARCHAR(40)  NOT NULL,
    detail    VARCHAR(500) NOT NULL,
    client_ip VARCHAR(64)  NULL,
    PRIMARY KEY (id)
);
