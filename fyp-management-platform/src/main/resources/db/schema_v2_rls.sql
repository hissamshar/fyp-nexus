-- ============================================================
-- FYP Management Platform — V2 Schema (REST API + RLS + Supabase Auth)
-- Schema: fyp
-- Run this on Supabase SQL Editor
-- WARNING: THIS IS A FRESH SCHEMA. IT WILL WIPE EXISTING DATA.
-- ============================================================

DROP SCHEMA IF EXISTS fyp CASCADE;
CREATE SCHEMA fyp;

-- ============================================================
-- CORE USER TABLES
-- ============================================================

-- fyp.users is now an extension of Supabase's auth.users table.
CREATE TABLE fyp.users (
    user_id       UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    role          VARCHAR(50) NOT NULL CHECK (role IN ('STUDENT','SUPERVISOR','EXAMINER','ADMIN','INDUSTRY_PARTNER')),
    is_active     BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- Trigger to automatically create a profile in fyp.users when a user signs up via Supabase Auth
CREATE OR REPLACE FUNCTION fyp.handle_new_user() 
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO fyp.users (user_id, name, email, role)
  VALUES (
      new.id, 
      COALESCE(new.raw_user_meta_data->>'name', 'Unknown User'),
      new.email,
      COALESCE(new.raw_user_meta_data->>'role', 'STUDENT')
  );
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE fyp.handle_new_user();

-- ... (Rest of tables: students, supervisors, examiners, etc.)
CREATE TABLE fyp.students (
    student_id  UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID NOT NULL UNIQUE REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    department  VARCHAR(255),
    cgpa        NUMERIC(3,2) DEFAULT 0.0,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE fyp.supervisors (
    supervisor_id    UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id          UUID NOT NULL UNIQUE REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    employee_id      VARCHAR(100),
    research_area    TEXT,
    slots_available  INT DEFAULT 5,
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE fyp.examiners (
    examiner_id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID NOT NULL UNIQUE REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    expertise   TEXT[],
    is_external BOOLEAN DEFAULT FALSE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE fyp.admins (
    admin_id    UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID NOT NULL UNIQUE REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    permissions TEXT[],
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE fyp.industry_partners (
    partner_id    UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id       UUID NOT NULL UNIQUE REFERENCES fyp.users(user_id) ON DELETE CASCADE,
    company_name  VARCHAR(255),
    contact_email VARCHAR(255),
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================

-- Enable RLS on all tables
ALTER TABLE fyp.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE fyp.students ENABLE ROW LEVEL SECURITY;
ALTER TABLE fyp.supervisors ENABLE ROW LEVEL SECURITY;
ALTER TABLE fyp.examiners ENABLE ROW LEVEL SECURITY;
ALTER TABLE fyp.admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE fyp.industry_partners ENABLE ROW LEVEL SECURITY;

-- Global Read Access for authenticated users (simplified for now, can restrict later)
CREATE POLICY "Anyone can read user profiles" ON fyp.users FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Users can update their own profile" ON fyp.users FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Anyone can read students" ON fyp.students FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Students can update their own student profile" ON fyp.students FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Anyone can read supervisors" ON fyp.supervisors FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Anyone can read examiners" ON fyp.examiners FOR SELECT USING (auth.role() = 'authenticated');
CREATE POLICY "Anyone can read industry partners" ON fyp.industry_partners FOR SELECT USING (auth.role() = 'authenticated');

-- Grant usage to authenticated role
GRANT USAGE ON SCHEMA fyp TO authenticated;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA fyp TO authenticated;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA fyp TO authenticated;

-- Grant usage to anon role (for unauthenticated signups/reads if necessary)
GRANT USAGE ON SCHEMA fyp TO anon;
GRANT SELECT ON fyp.users TO anon;
