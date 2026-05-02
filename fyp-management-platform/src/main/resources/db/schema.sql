-- ============================================================
-- FYP Management Platform — Database Schema
-- Schema: fyp (isolated from existing public schema tables)
-- Run this on Supabase SQL Editor
-- ============================================================

CREATE SCHEMA IF NOT EXISTS fyp;

-- ============================================================
-- CORE USER TABLES
-- ============================================================

CREATE TABLE IF NOT EXISTS fyp.users (
    user_id       UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role          VARCHAR(50) NOT NULL CHECK (role IN ('STUDENT','SUPERVISOR','EXAMINER','ADMIN','INDUSTRY_PARTNER')),
    is_active     BOOLEAN DEFAULT TRUE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    failed_login_attempts INT DEFAULT 0,
    locked_until  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.students (
    student_id  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID NOT NULL UNIQUE REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    department  VARCHAR(255),
    cgpa        NUMERIC(3,2) DEFAULT 0.0,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.supervisors (
    supervisor_id    UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id          UUID NOT NULL UNIQUE REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    employee_id      VARCHAR(100),
    research_area    TEXT,
    slots_available  INT DEFAULT 5,
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.examiners (
    examiner_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID NOT NULL UNIQUE REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    expertise   TEXT[],
    is_external BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.admins (
    admin_id    UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID NOT NULL UNIQUE REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    permissions TEXT[],
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.industry_partners (
    partner_id    UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id       UUID NOT NULL UNIQUE REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    company_name  VARCHAR(255),
    contact_email VARCHAR(255),
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- AUTH — OTP TOKENS
-- ============================================================

CREATE TABLE IF NOT EXISTS fyp.otp_tokens (
    token_id   UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    otp_code   VARCHAR(6) NOT NULL,
    purpose    VARCHAR(50) DEFAULT 'EMAIL_VERIFY' CHECK (purpose IN ('EMAIL_VERIFY','PASSWORD_RESET')),
    expires_at TIMESTAMPTZ NOT NULL,
    is_used    BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- PROJECT LIFECYCLE
-- ============================================================

CREATE TABLE IF NOT EXISTS fyp.project_proposals (
    proposal_id       UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title             VARCHAR(500) NOT NULL,
    abstract          TEXT,
    objectives        TEXT,
    methodology       TEXT,
    expected_outcomes TEXT,
    description       TEXT,
    submission_date   DATE,
    status            VARCHAR(50) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PENDING','APPROVED','REJECTED')),
    rejection_comment TEXT,
    student_id        UUID NOT NULL REFERENCES fyp.students(student_id),
    supervisor_id     UUID REFERENCES fyp.supervisors(supervisor_id),
    created_at        TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.projects (
    project_id  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    start_date  DATE,
    end_date    DATE,
    repo_url    VARCHAR(500),
    status      VARCHAR(50) DEFAULT 'INITIATED' CHECK (status IN ('INITIATED','IN_PROGRESS','UNDER_REVIEW','COMPLETED','TERMINATED')),
    proposal_id UUID NOT NULL UNIQUE REFERENCES fyp.project_proposals(proposal_id),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.milestones (
    milestone_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    deadline     TIMESTAMPTZ,
    weightage    INT DEFAULT 10 CHECK (weightage >= 0 AND weightage <= 100),
    status       VARCHAR(50) DEFAULT 'PENDING' CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','OVERDUE')),
    project_id   UUID NOT NULL REFERENCES fyp.projects(project_id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.deliverables (
    file_id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    file_name        VARCHAR(500) NOT NULL,
    file_type        VARCHAR(50),
    upload_timestamp TIMESTAMPTZ DEFAULT NOW(),
    file_path        TEXT NOT NULL,
    milestone_id     UUID NOT NULL REFERENCES fyp.milestones(milestone_id) ON DELETE CASCADE,
    student_id       UUID NOT NULL REFERENCES fyp.students(student_id),
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.progress_reports (
    report_id    UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    work_done    TEXT,
    issues_faced TEXT,
    next_steps   TEXT,
    submitted_at TIMESTAMPTZ DEFAULT NOW(),
    project_id   UUID NOT NULL REFERENCES fyp.projects(project_id) ON DELETE CASCADE,
    student_id   UUID NOT NULL REFERENCES fyp.students(student_id),
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- GRADING
-- ============================================================

CREATE TABLE IF NOT EXISTS fyp.rubrics (
    rubric_id    UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    total_points INT DEFAULT 100,
    version      VARCHAR(50) DEFAULT '1.0',
    name         VARCHAR(255),
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.rubric_criteria (
    criterion_id   UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    criterion_name VARCHAR(255) NOT NULL,
    max_score      INT NOT NULL,
    description    TEXT,
    rubric_id      UUID NOT NULL REFERENCES fyp.rubrics(rubric_id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.grades (
    grade_id     UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    letter_grade VARCHAR(5),
    is_published BOOLEAN DEFAULT FALSE,
    project_id   UUID NOT NULL REFERENCES fyp.projects(project_id) ON DELETE CASCADE,
    rubric_id    UUID REFERENCES fyp.rubrics(rubric_id),
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.grade_entries (
    entry_id     UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    score        INT NOT NULL,
    comments     TEXT,
    graded_at    TIMESTAMPTZ DEFAULT NOW(),
    grade_id     UUID NOT NULL REFERENCES fyp.grades(grade_id) ON DELETE CASCADE,
    grader_id    UUID NOT NULL REFERENCES fyp.users(user_id),
    criterion_id UUID REFERENCES fyp.rubric_criteria(criterion_id),
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- ASSIGNMENTS
-- ============================================================

CREATE TABLE IF NOT EXISTS fyp.supervisor_assignments (
    assignment_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    project_id    UUID NOT NULL REFERENCES fyp.projects(project_id) ON DELETE CASCADE,
    supervisor_id UUID NOT NULL REFERENCES fyp.supervisors(supervisor_id),
    assigned_at   TIMESTAMPTZ DEFAULT NOW(),
    created_at    TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(project_id, supervisor_id)
);

CREATE TABLE IF NOT EXISTS fyp.examiner_assignments (
    assignment_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    project_id    UUID NOT NULL REFERENCES fyp.projects(project_id) ON DELETE CASCADE,
    examiner_id   UUID NOT NULL REFERENCES fyp.examiners(examiner_id),
    assigned_at   TIMESTAMPTZ DEFAULT NOW(),
    created_at    TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(project_id, examiner_id)
);

-- ============================================================
-- COMMUNICATION
-- ============================================================

CREATE TABLE IF NOT EXISTS fyp.discussion_boards (
    board_id   UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    is_private BOOLEAN DEFAULT FALSE,
    project_id UUID NOT NULL UNIQUE REFERENCES fyp.projects(project_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.discussion_threads (
    thread_id  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    topic      VARCHAR(500) NOT NULL,
    is_locked  BOOLEAN DEFAULT FALSE,
    is_pinned  BOOLEAN DEFAULT FALSE,
    board_id   UUID NOT NULL REFERENCES fyp.discussion_boards(board_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.posts (
    post_id         UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content         TEXT NOT NULL,
    timestamp       TIMESTAMPTZ DEFAULT NOW(),
    thread_id       UUID NOT NULL REFERENCES fyp.discussion_threads(thread_id) ON DELETE CASCADE,
    author_id       UUID NOT NULL REFERENCES fyp.users(user_id),
    attachment_path TEXT,
    is_deleted      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.meeting_requests (
    request_id    UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    proposed_time TIMESTAMPTZ NOT NULL,
    location      VARCHAR(255),
    agenda        TEXT,
    status        VARCHAR(50) DEFAULT 'REQUESTED' CHECK (status IN ('REQUESTED','CONFIRMED','DECLINED','RESCHEDULED')),
    counter_time  TIMESTAMPTZ,
    student_id    UUID NOT NULL REFERENCES fyp.students(student_id),
    supervisor_id UUID NOT NULL REFERENCES fyp.supervisors(supervisor_id),
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.notifications (
    notif_id     UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    message      TEXT NOT NULL,
    type         VARCHAR(50) CHECK (type IN ('PROPOSAL','GRADE','MEETING','DEADLINE','FEEDBACK','SYSTEM')),
    is_read      BOOLEAN DEFAULT FALSE,
    recipient_id UUID NOT NULL REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- ADMIN
-- ============================================================

CREATE TABLE IF NOT EXISTS fyp.audit_logs (
    log_id     UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID REFERENCES fyp.users(user_id) ON DELETE SET NULL,
    action     VARCHAR(255) NOT NULL,
    ip_address VARCHAR(50),
    timestamp  TIMESTAMPTZ DEFAULT NOW(),
    details    TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS fyp.semester_deadlines (
    deadline_id   UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    semester      VARCHAR(100) NOT NULL,
    deadline_type VARCHAR(100) NOT NULL,
    due_date      TIMESTAMPTZ NOT NULL,
    description   TEXT,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- INDUSTRY
-- ============================================================

CREATE TABLE IF NOT EXISTS fyp.industry_problems (
    problem_id      UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    domain          VARCHAR(255),
    required_skills TEXT[],
    contact_email   VARCHAR(255),
    partner_id      UUID NOT NULL REFERENCES fyp.industry_partners(partner_id),
    adopted_by      UUID REFERENCES fyp.students(student_id),
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- SUPERVISOR FEEDBACK
-- ============================================================

CREATE TABLE IF NOT EXISTS fyp.feedback (
    feedback_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content     TEXT NOT NULL,
    report_id   UUID REFERENCES fyp.progress_reports(report_id),
    project_id  UUID REFERENCES fyp.projects(project_id),
    supervisor_id UUID NOT NULL REFERENCES fyp.supervisors(supervisor_id),
    student_id  UUID NOT NULL REFERENCES fyp.students(student_id),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_users_email        ON fyp.users(email);
CREATE INDEX IF NOT EXISTS idx_users_role         ON fyp.users(role);
CREATE INDEX IF NOT EXISTS idx_proposals_student  ON fyp.project_proposals(student_id);
CREATE INDEX IF NOT EXISTS idx_proposals_status   ON fyp.project_proposals(status);
CREATE INDEX IF NOT EXISTS idx_projects_status    ON fyp.projects(status);
CREATE INDEX IF NOT EXISTS idx_milestones_project ON fyp.milestones(project_id);
CREATE INDEX IF NOT EXISTS idx_notifs_recipient   ON fyp.notifications(recipient_id, is_read);
CREATE INDEX IF NOT EXISTS idx_audit_user         ON fyp.audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_posts_thread       ON fyp.posts(thread_id);
CREATE INDEX IF NOT EXISTS idx_otp_user           ON fyp.otp_tokens(user_id, is_used);

-- ============================================================
-- ENABLE ROW LEVEL SECURITY (advisable on Supabase)
-- (Disable RLS for now since we use service-role JDBC connection)
-- ============================================================

-- Grant usage on fyp schema to postgres role (already has it)
GRANT USAGE ON SCHEMA fyp TO postgres;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA fyp TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA fyp TO postgres;

-- ============================================================
-- SAMPLE ADMIN USER (password: Admin@1234)
-- Hash generated with BCrypt cost=12
-- ============================================================

INSERT INTO fyp.users (name, email, password_hash, role, is_active, is_email_verified)
VALUES (
    'System Administrator',
    'admin@fyp.edu.pk',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj2KUgHXMRGC',
    'ADMIN',
    TRUE,
    TRUE
) ON CONFLICT (email) DO NOTHING;

INSERT INTO fyp.admins (user_id, permissions)
SELECT user_id, ARRAY['ALL'] FROM fyp.users WHERE email = 'admin@fyp.edu.pk'
ON CONFLICT (user_id) DO NOTHING;
