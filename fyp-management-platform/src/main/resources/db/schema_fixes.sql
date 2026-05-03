-- ============================================================
-- FYP Platform — Schema Fixes (apply after schema_v2_rls.sql)
-- Run in Supabase SQL Editor
-- ============================================================

-- FIX 1: Permissions
GRANT USAGE ON SCHEMA fyp TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA fyp TO authenticated;
GRANT SELECT ON fyp.users TO anon;
ALTER DEFAULT PRIVILEGES IN SCHEMA fyp
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO authenticated;

-- FIX 2: Trigger — create role-specific profile row on signup
CREATE OR REPLACE FUNCTION fyp.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE v_role TEXT;
BEGIN
    v_role := COALESCE(NEW.raw_user_meta_data->>'role', 'STUDENT');

    INSERT INTO fyp.users (user_id, name, email, role, is_active)
    VALUES (NEW.id, COALESCE(NEW.raw_user_meta_data->>'name', 'Unknown'), NEW.email, v_role, TRUE)
    ON CONFLICT (user_id) DO NOTHING;

    IF    v_role = 'STUDENT'          THEN INSERT INTO fyp.students (user_id)          VALUES (NEW.id) ON CONFLICT (user_id) DO NOTHING;
    ELSIF v_role = 'SUPERVISOR'       THEN INSERT INTO fyp.supervisors (user_id)       VALUES (NEW.id) ON CONFLICT (user_id) DO NOTHING;
    ELSIF v_role = 'EXAMINER'         THEN INSERT INTO fyp.examiners (user_id)         VALUES (NEW.id) ON CONFLICT (user_id) DO NOTHING;
    ELSIF v_role = 'ADMIN'            THEN INSERT INTO fyp.admins (user_id)            VALUES (NEW.id) ON CONFLICT (user_id) DO NOTHING;
    ELSIF v_role = 'INDUSTRY_PARTNER' THEN INSERT INTO fyp.industry_partners (user_id) VALUES (NEW.id) ON CONFLICT (user_id) DO NOTHING;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE PROCEDURE fyp.handle_new_user();

-- FIX 2b: Back-fill role rows for existing users
INSERT INTO fyp.students (user_id)          SELECT user_id FROM fyp.users WHERE role = 'STUDENT'          ON CONFLICT (user_id) DO NOTHING;
INSERT INTO fyp.supervisors (user_id)       SELECT user_id FROM fyp.users WHERE role = 'SUPERVISOR'       ON CONFLICT (user_id) DO NOTHING;
INSERT INTO fyp.examiners (user_id)         SELECT user_id FROM fyp.users WHERE role = 'EXAMINER'         ON CONFLICT (user_id) DO NOTHING;
INSERT INTO fyp.admins (user_id)            SELECT user_id FROM fyp.users WHERE role = 'ADMIN'            ON CONFLICT (user_id) DO NOTHING;
INSERT INTO fyp.industry_partners (user_id) SELECT user_id FROM fyp.users WHERE role = 'INDUSTRY_PARTNER' ON CONFLICT (user_id) DO NOTHING;
