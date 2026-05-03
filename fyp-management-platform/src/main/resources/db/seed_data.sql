-- Sync existing auth.users into fyp.users
INSERT INTO fyp.users (user_id, name, email, role)
SELECT 
    id, 
    COALESCE(raw_user_meta_data->>'name', 'Unknown'), 
    email, 
    COALESCE(raw_user_meta_data->>'role', 'STUDENT')
FROM auth.users
ON CONFLICT (user_id) DO NOTHING;

DO $$ 
DECLARE
    student1 UUID;
    student2 UUID;
    supervisor1 UUID;
    examiner1 UUID;
    admin1 UUID;
    proj1 UUID;
BEGIN
    SELECT user_id INTO student1 FROM fyp.users WHERE email = 'student1@nu.edu.pk';
    SELECT user_id INTO student2 FROM fyp.users WHERE email = 'student2@nu.edu.pk';
    SELECT user_id INTO supervisor1 FROM fyp.users WHERE email = 'supervisor1@nu.edu.pk';
    SELECT user_id INTO examiner1 FROM fyp.users WHERE email = 'examiner1@nu.edu.pk';
    SELECT user_id INTO admin1 FROM fyp.users WHERE email = 'admin1@nu.edu.pk';

    IF student1 IS NULL THEN
        RAISE EXCEPTION 'student1 not found in fyp.users! Did you register them?';
    END IF;

    -- Insert role-specific profiles
    INSERT INTO fyp.students (user_id, department, cgpa) VALUES (student1, 'Computer Science', 3.8) ON CONFLICT DO NOTHING;
    INSERT INTO fyp.students (user_id, department, cgpa) VALUES (student2, 'Software Engineering', 3.5) ON CONFLICT DO NOTHING;
    INSERT INTO fyp.supervisors (user_id, employee_id, research_area, slots_available) VALUES (supervisor1, 'EMP001', 'Artificial Intelligence', 5) ON CONFLICT DO NOTHING;
    INSERT INTO fyp.examiners (user_id, expertise, is_external) VALUES (examiner1, ARRAY['Machine Learning', 'Data Science'], FALSE) ON CONFLICT DO NOTHING;
    INSERT INTO fyp.admins (user_id, permissions) VALUES (admin1, ARRAY['ALL']) ON CONFLICT DO NOTHING;

    -- Insert a Dummy Project & Proposal
    INSERT INTO fyp.projects (title, abstract, objectives, methodology, supervisor_id, status)
    VALUES (
        'AI based Timetable Generator',
        'An AI tool to generate timetables automatically without overlaps.',
        '1. Automate timetable generation. 2. Handle constraints seamlessly.',
        'Genetic Algorithms and Constraint Satisfaction Models.',
        (SELECT supervisor_id FROM fyp.supervisors WHERE user_id = supervisor1),
        'ACTIVE'
    ) RETURNING project_id INTO proj1;

    INSERT INTO fyp.project_proposals (title, abstract, objectives, methodology, student_id, status, project_id)
    VALUES (
        'AI based Timetable Generator',
        'An AI tool to generate timetables automatically without overlaps.',
        '1. Automate timetable generation. 2. Handle constraints seamlessly.',
        'Genetic Algorithms and Constraint Satisfaction Models.',
        (SELECT student_id FROM fyp.students WHERE user_id = student1),
        'APPROVED',
        proj1
    );

    -- Insert Milestones & Deadlines
    INSERT INTO fyp.milestones (project_id, name, description, due_date, status, weightage)
    VALUES (proj1, 'Sessional 1 Delivery', 'First evaluation covering architecture.', NOW() + INTERVAL '14 days', 'PENDING', 20.0);

    INSERT INTO fyp.semester_deadlines (semester, deadline_type, due_date, description)
    VALUES ('Spring 2026', 'Final Proposal', NOW() + INTERVAL '7 days', 'Submit your final proposal to supervisors.');

END $$;
