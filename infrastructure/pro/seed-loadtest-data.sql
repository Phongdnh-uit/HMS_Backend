-- ============================================================================
-- HMS Backend - Load Test Data Seeding Script
-- ============================================================================
-- Purpose: Seed data directly into PostgreSQL for 500 VU load test
-- Target: Docker PostgreSQL container
-- 
-- Data Requirements (from TEST_PROGRESS_TRACKER.md):
--   - 1,000 pre-registered patient accounts
--   - 60 doctor accounts with schedules
--   - 50 nurse accounts
--   - 40 receptionist accounts
--   - 5 admin accounts
--   - 500 available appointment slots
--   - 200 medicine items in inventory
--   - 50 lab test templates
--
-- Usage:
--   docker exec -i <postgres_container> psql -U <user> -d <db> < seed-loadtest-data.sql
-- ============================================================================

-- Disable triggers temporarily for faster inserts
SET session_replication_role = replica;

BEGIN;

-- Enable UUID generation (PostgreSQL 13+ has gen_random_uuid() built-in)
-- For older versions, uncomment: CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- SEED ID STRATEGY
-- ============================================================================
-- We use deterministic UUIDs based on MD5 hash of predictable strings.
-- This ensures:
--   1. Valid UUID format (required by some code)
--   2. Reproducible IDs (same data every run)
--   3. Referential integrity (FKs can reference known IDs)
--
-- Format: md5('prefix-number')::uuid
-- Example: md5('admin-1')::uuid = '4124bc0a-335c-27de-a7c3-64a4e4e3c153'
-- ============================================================================

-- ============================================================================
-- 1. AUTH SERVICE - ACCOUNTS
-- ============================================================================
-- Password: 'Password@123' BCrypt encoded
-- $2a$10$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2
-- Actual entity fields: id, email, password, role (RoleEnum), refreshToken, refreshTokenExpiresAt, emailVerified

-- 1.1 Admin Accounts (5)
INSERT INTO accounts (id, email, password, role, email_verified)
SELECT 
    md5('admin-' || i)::uuid,
    'admin' || i || '@hms.com',
    '$2a$10$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2',
    'ADMIN',
    true
FROM generate_series(1, 5) AS i
ON CONFLICT (id) DO NOTHING;

-- 1.2 Doctor Accounts (60)
INSERT INTO accounts (id, email, password, role, email_verified)
SELECT 
    md5('doctor-' || i)::uuid,
    'doctor' || i || '@hms.com',
    '$2a$10$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2',
    'DOCTOR',
    true
FROM generate_series(1, 60) AS i
ON CONFLICT (id) DO NOTHING;

-- 1.3 Nurse Accounts (50)
INSERT INTO accounts (id, email, password, role, email_verified)
SELECT 
    md5('nurse-' || i)::uuid,
    'nurse' || i || '@hms.com',
    '$2a$10$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2',
    'NURSE',
    true
FROM generate_series(1, 50) AS i
ON CONFLICT (id) DO NOTHING;

-- 1.4 Receptionist Accounts (40)
INSERT INTO accounts (id, email, password, role, email_verified)
SELECT 
    md5('receptionist-' || i)::uuid,
    'receptionist' || i || '@hms.com',
    '$2a$10$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2',
    'RECEPTIONIST',
    true
FROM generate_series(1, 40) AS i
ON CONFLICT (id) DO NOTHING;

-- 1.5 Patient Accounts (1000)
INSERT INTO accounts (id, email, password, role, email_verified)
SELECT 
    md5('patient-' || i)::uuid,
    'patient' || i || '@email.com',
    '$2a$10$N.zmdr9Vg2jO/vR.0VfL4.YVeN7qvg.7qpVU.8BqH.N0P.1Z6xKC2',
    'PATIENT',
    true
FROM generate_series(1, 1000) AS i
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 2. HR SERVICE - DEPARTMENTS
-- ============================================================================
-- Actual entity fields: id, name, description, headDoctorId, location, phoneExtension, status (DepartmentStatus), createdAt, updatedAt, createdBy, updatedBy

INSERT INTO departments (id, name, description, location, phone_extension, status, created_at, updated_at, created_by, updated_by)
VALUES
    (md5('dept-1')::uuid, 'Internal Medicine', 'General internal medicine department', 'Building A, Floor 1', '1001', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (md5('dept-2')::uuid, 'Cardiology', 'Heart and cardiovascular diseases', 'Building A, Floor 2', '1002', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (md5('dept-3')::uuid, 'Orthopedics', 'Bone and joint treatment', 'Building B, Floor 1', '1003', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (md5('dept-4')::uuid, 'Pediatrics', 'Child healthcare', 'Building B, Floor 2', '1004', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (md5('dept-5')::uuid, 'Dermatology', 'Skin conditions treatment', 'Building C, Floor 1', '1005', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (md5('dept-6')::uuid, 'Neurology', 'Brain and nervous system', 'Building C, Floor 2', '1006', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (md5('dept-7')::uuid, 'Ophthalmology', 'Eye care and treatment', 'Building D, Floor 1', '1007', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (md5('dept-8')::uuid, 'ENT', 'Ear, Nose and Throat', 'Building D, Floor 2', '1008', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (md5('dept-9')::uuid, 'Gynecology', 'Women health', 'Building E, Floor 1', '1009', 'ACTIVE', NOW(), NOW(), 'system', 'system'),
    (md5('dept-10')::uuid, 'General Surgery', 'Surgical procedures', 'Building E, Floor 2', '1010', 'ACTIVE', NOW(), NOW(), 'system', 'system')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 3. HR SERVICE - EMPLOYEES
-- ============================================================================
-- Actual entity fields: id, accountId, fullName, role (EmployeeRole), departmentId, specialization, licenseNumber, phoneNumber, address, status (EmployeeStatus), hiredAt, profileImageUrl, createdAt, updatedAt, createdBy, updatedBy

-- 3.1 Doctors (60) - distributed across departments
INSERT INTO employees (id, account_id, full_name, role, department_id, specialization, license_number, phone_number, address, status, hired_at, created_at, updated_at, created_by, updated_by)
SELECT 
    md5('emp-doctor-' || i)::uuid,
    md5('doctor-' || i)::uuid,
    'Dr. ' || (ARRAY['Nguyen', 'Tran', 'Le', 'Pham', 'Hoang', 'Vo', 'Dang', 'Bui', 'Do', 'Ngo'])[1 + (i % 10)] || ' ' ||
    (ARRAY['Van', 'Thi', 'Minh', 'Thanh', 'Duc', 'Hong', 'Quang', 'Anh', 'Khanh', 'Huu'])[1 + ((i/10) % 10)] || ' ' ||
    (ARRAY['An', 'Binh', 'Cuong', 'Dung', 'Em', 'Phong', 'Giang', 'Hai', 'Khoa', 'Linh'])[1 + ((i/100) % 10) + i % 10],
    'DOCTOR',
    md5('dept-' || (1 + (i % 10)))::uuid,
    (ARRAY['Cardiology', 'Neurology', 'Orthopedics', 'Pediatrics', 'Dermatology', 
           'Internal Medicine', 'General Surgery', 'Ophthalmology', 'ENT', 'Gynecology'])[1 + (i % 10)],
    'LIC' || LPAD(i::text, 6, '0'),
    '090' || LPAD((1000000 + i)::text, 7, '0'),
    i || ' Le Loi Street, District ' || (1 + (i % 12)) || ', HCMC',
    'ACTIVE',
    NOW() - INTERVAL '5 years' + (random() * INTERVAL '4 years'),
    NOW(),
    NOW(),
    'system',
    'system'
FROM generate_series(1, 60) AS i
ON CONFLICT (id) DO NOTHING;

-- 3.2 Nurses (50)
INSERT INTO employees (id, account_id, full_name, role, department_id, phone_number, address, status, hired_at, created_at, updated_at, created_by, updated_by)
SELECT 
    md5('emp-nurse-' || i)::uuid,
    md5('nurse-' || i)::uuid,
    'Nurse ' || (ARRAY['Nguyen', 'Tran', 'Le', 'Pham', 'Hoang'])[1 + (i % 5)] || ' ' ||
    (ARRAY['Thi', 'Van', 'Minh', 'Thanh', 'Hong'])[1 + ((i/5) % 5)] || ' ' ||
    (ARRAY['Mai', 'Lan', 'Huong', 'Linh', 'Nga', 'Thao', 'Hang', 'Yen', 'Hoa', 'Trang'])[1 + (i % 10)],
    'NURSE',
    md5('dept-' || (1 + (i % 10)))::uuid,
    '091' || LPAD((2000000 + i)::text, 7, '0'),
    i || ' Nguyen Hue Street, District ' || (1 + (i % 12)) || ', HCMC',
    'ACTIVE',
    NOW() - INTERVAL '3 years' + (random() * INTERVAL '2 years'),
    NOW(),
    NOW(),
    'system',
    'system'
FROM generate_series(1, 50) AS i
ON CONFLICT (id) DO NOTHING;

-- 3.3 Receptionists (40)
INSERT INTO employees (id, account_id, full_name, role, phone_number, address, status, hired_at, created_at, updated_at, created_by, updated_by)
SELECT 
    md5('emp-receptionist-' || i)::uuid,
    md5('receptionist-' || i)::uuid,
    'Receptionist ' || (ARRAY['Nguyen', 'Tran', 'Le', 'Pham'])[1 + (i % 4)] || ' ' ||
    (ARRAY['Kim', 'Thu', 'Nhi', 'Uyen', 'My', 'Trinh', 'Chi', 'Vi'])[1 + (i % 8)],
    'RECEPTIONIST',
    '092' || LPAD((3000000 + i)::text, 7, '0'),
    i || ' Tran Hung Dao Street, District ' || (1 + (i % 12)) || ', HCMC',
    'ACTIVE',
    NOW() - INTERVAL '2 years' + (random() * INTERVAL '1 year'),
    NOW(),
    NOW(),
    'system',
    'system'
FROM generate_series(1, 40) AS i
ON CONFLICT (id) DO NOTHING;

-- 3.4 Admins (5)
INSERT INTO employees (id, account_id, full_name, role, phone_number, address, status, hired_at, created_at, updated_at, created_by, updated_by)
SELECT 
    md5('emp-admin-' || i)::uuid,
    md5('admin-' || i)::uuid,
    'Admin ' || (ARRAY['Nguyen Van Quản', 'Tran Minh Ly', 'Le Thanh Trị', 'Pham Hong Quản', 'Hoang Duc Trị'])[i],
    'ADMIN',
    '093' || LPAD((4000000 + i)::text, 7, '0'),
    i || ' Pasteur Street, District 1, HCMC',
    'ACTIVE',
    NOW() - INTERVAL '5 years',
    NOW(),
    NOW(),
    'system',
    'system'
FROM generate_series(1, 5) AS i
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 4. HR SERVICE - EMPLOYEE SCHEDULES (500 slots for load test)
-- ============================================================================
-- Generate schedules for next 7 days for all doctors
-- Actual entity fields: id, employeeId, workDate, startTime, endTime, status (ScheduleStatus), notes, createdAt, updatedAt, createdBy, updatedBy

INSERT INTO employee_schedules (id, employee_id, work_date, start_time, end_time, status, notes, created_at, updated_at, created_by, updated_by)
SELECT 
    md5('schedule-' || i || '-' || day_offset)::uuid,
    md5('emp-doctor-' || i)::uuid,
    CURRENT_DATE + day_offset,
    '08:00:00'::time,
    '17:00:00'::time,
    'AVAILABLE',
    'Regular work day',
    NOW(),
    NOW(),
    'system',
    'system'
FROM 
    generate_series(1, 60) AS i,
    generate_series(1, 7) AS day_offset
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 5. PATIENT SERVICE - PATIENTS (1000)
-- ============================================================================
-- Actual entity fields: id, accountId, fullName, email, dateOfBirth, gender (Gender), phoneNumber, address, identificationNumber, healthInsuranceNumber, relativeFullName, relativePhoneNumber, relativeRelationship, bloodType, allergies, profileImageUrl, createdAt, updatedAt, createdBy, updatedBy

INSERT INTO patient (id, account_id, full_name, email, date_of_birth, gender, phone_number, address, identification_number, health_insurance_number, relative_full_name, relative_phone_number, relative_relationship, blood_type, allergies, created_at, updated_at, created_by, updated_by)
SELECT 
    md5('patient-' || i)::uuid,
    md5('patient-' || i)::uuid,
    (ARRAY['Nguyen', 'Tran', 'Le', 'Pham', 'Hoang', 'Vo', 'Dang', 'Bui', 'Do', 'Ngo'])[1 + (i % 10)] || ' ' ||
    (ARRAY['Van', 'Thi', 'Minh', 'Thanh', 'Duc', 'Hong', 'Quang', 'Anh', 'Khanh', 'Huu'])[1 + ((i/10) % 10)] || ' ' ||
    (ARRAY['An', 'Binh', 'Cuong', 'Dung', 'Em', 'Phong', 'Giang', 'Hai', 'Khoa', 'Linh'])[1 + (i % 10)],
    'patient' || i || '@email.com',
    DATE '1950-01-01' + (random() * 25000)::integer,
    (ARRAY['MALE', 'FEMALE', 'OTHER'])[1 + (i % 3)],
    '098' || LPAD((5000000 + i)::text, 7, '0'),
    i || ' ' || (ARRAY['Hai Ba Trung', 'Le Van Sy', 'Nguyen Dinh Chieu', 'Cach Mang Thang 8', 'Vo Van Tan'])[1 + (i % 5)] || ' Street, District ' || (1 + (i % 12)) || ', HCMC',
    '0' || LPAD((79000000000 + i)::text, 11, '0'),
    CASE WHEN random() > 0.3 THEN 'INS' || LPAD(i::text, 8, '0') ELSE NULL END,
    (ARRAY['Nguyen Van', 'Tran Thi', 'Le Minh', 'Pham Thanh', 'Hoang Duc'])[1 + (i % 5)] || ' ' || (ARRAY['A', 'B', 'C', 'D', 'E'])[1 + (i % 5)],
    '097' || LPAD((6000000 + i)::text, 7, '0'),
    (ARRAY['Spouse', 'Parent', 'Child', 'Sibling', 'Friend'])[1 + (i % 5)],
    (ARRAY['A+', 'A-', 'B+', 'B-', 'O+', 'O-', 'AB+', 'AB-'])[1 + (i % 8)],
    CASE WHEN random() > 0.7 THEN (ARRAY['Penicillin', 'Aspirin', 'Sulfa drugs', 'Ibuprofen', 'None'])[1 + (i % 5)] ELSE NULL END,
    NOW() - INTERVAL '90 days' + (random() * INTERVAL '80 days'),
    NOW(),
    'system',
    'system'
FROM generate_series(1, 1000) AS i
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 6. MEDICINE SERVICE - CATEGORIES
-- ============================================================================
-- Actual entity fields: id, name, description, createdAt, updatedAt, createdBy, updatedBy

INSERT INTO category (id, name, description, created_at, updated_at, created_by, updated_by)
VALUES
    (md5('cat-1')::uuid, 'Antibiotics', 'Medicines that fight bacterial infections', NOW(), NOW(), 'system', 'system'),
    (md5('cat-2')::uuid, 'Analgesics', 'Pain relievers and fever reducers', NOW(), NOW(), 'system', 'system'),
    (md5('cat-3')::uuid, 'Antihypertensives', 'Blood pressure medications', NOW(), NOW(), 'system', 'system'),
    (md5('cat-4')::uuid, 'Antidiabetics', 'Diabetes management medications', NOW(), NOW(), 'system', 'system'),
    (md5('cat-5')::uuid, 'Antihistamines', 'Allergy medications', NOW(), NOW(), 'system', 'system'),
    (md5('cat-6')::uuid, 'Cardiovascular', 'Heart and blood vessel medications', NOW(), NOW(), 'system', 'system'),
    (md5('cat-7')::uuid, 'Gastrointestinal', 'Digestive system medications', NOW(), NOW(), 'system', 'system'),
    (md5('cat-8')::uuid, 'Vitamins', 'Vitamin and mineral supplements', NOW(), NOW(), 'system', 'system'),
    (md5('cat-9')::uuid, 'Dermatological', 'Skin treatment medications', NOW(), NOW(), 'system', 'system'),
    (md5('cat-10')::uuid, 'Respiratory', 'Respiratory system medications', NOW(), NOW(), 'system', 'system')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 7. MEDICINE SERVICE - MEDICINES (200)
-- ============================================================================
-- Actual entity fields: id, name, activeIngredient, unit, description, quantity, concentration, packaging, purchasePrice, sellingPrice, manufacturer, sideEffects, storageConditions, expiresAt, category (FK), createdAt, updatedAt, createdBy, updatedBy

INSERT INTO medicine (id, name, active_ingredient, unit, description, quantity, concentration, packaging, purchase_price, selling_price, manufacturer, side_effects, storage_conditions, expires_at, category_id, created_at, updated_at, created_by, updated_by)
SELECT 
    md5('med-' || i)::uuid,
    medicine_name,
    active_ing,
    unit_type,
    'Description for ' || medicine_name,
    (100 + random() * 900)::bigint,
    dosage_str,
    'Box of ' || (10 + (i % 5) * 10) || ' ' || unit_type,
    (5000 + random() * 45000)::numeric(10,2),
    (10000 + random() * 90000)::numeric(10,2),
    (ARRAY['Sanofi', 'Pfizer', 'Novartis', 'Roche', 'GSK', 'AstraZeneca', 'J&J', 'Merck', 'Abbott', 'Bayer'])[1 + (i % 10)],
    CASE WHEN random() > 0.5 THEN 'May cause drowsiness, nausea' ELSE 'Generally well tolerated' END,
    'Store in cool, dry place',
    NOW() + INTERVAL '365 days' + (random() * INTERVAL '730 days'),
    md5('cat-' || (1 + (i % 10)))::uuid,
    NOW(),
    NOW(),
    'system',
    'system'
FROM (
    SELECT 
        i,
        (ARRAY[
            'Amoxicillin', 'Paracetamol', 'Ibuprofen', 'Omeprazole', 'Metformin',
            'Lisinopril', 'Amlodipine', 'Losartan', 'Atorvastatin', 'Simvastatin',
            'Cetirizine', 'Loratadine', 'Salbutamol', 'Metoprolol', 'Aspirin',
            'Ciprofloxacin', 'Azithromycin', 'Doxycycline', 'Metronidazole', 'Cephalexin',
            'Prednisone', 'Diclofenac', 'Naproxen', 'Tramadol', 'Gabapentin',
            'Vitamin C', 'Vitamin D', 'Vitamin B12', 'Iron Supplement', 'Calcium',
            'Ranitidine', 'Famotidine', 'Lansoprazole', 'Esomeprazole', 'Domperidone',
            'Fluconazole', 'Clotrimazole', 'Hydrocortisone', 'Betamethasone', 'Mometasone'
        ])[1 + (i % 40)] || ' ' || ((i / 40) + 1) * 100 || 'mg' AS medicine_name,
        (ARRAY[
            'Amoxicillin trihydrate', 'Paracetamol', 'Ibuprofen', 'Omeprazole', 'Metformin HCl',
            'Lisinopril dihydrate', 'Amlodipine besylate', 'Losartan potassium', 'Atorvastatin calcium', 'Simvastatin',
            'Cetirizine HCl', 'Loratadine', 'Salbutamol sulfate', 'Metoprolol tartrate', 'Acetylsalicylic acid',
            'Ciprofloxacin HCl', 'Azithromycin dihydrate', 'Doxycycline hyclate', 'Metronidazole', 'Cephalexin monohydrate',
            'Prednisone', 'Diclofenac sodium', 'Naproxen sodium', 'Tramadol HCl', 'Gabapentin',
            'Ascorbic acid', 'Cholecalciferol', 'Cyanocobalamin', 'Ferrous sulfate', 'Calcium carbonate',
            'Ranitidine HCl', 'Famotidine', 'Lansoprazole', 'Esomeprazole magnesium', 'Domperidone maleate',
            'Fluconazole', 'Clotrimazole', 'Hydrocortisone', 'Betamethasone dipropionate', 'Mometasone furoate'
        ])[1 + (i % 40)] AS active_ing,
        (ARRAY['tablet', 'capsule', 'ml', 'mg', 'sachet'])[1 + (i % 5)] AS unit_type,
        ((i / 40) + 1) * 100 || 'mg' AS dosage_str
    FROM generate_series(1, 200) AS i
) AS med_data
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 8. MEDICAL EXAM SERVICE - LAB TESTS (50 templates)
-- ============================================================================
-- Actual entity fields: id, code, name, category (LabTestCategory), description, price, unit, normalRange, isActive, createdAt, updatedAt, createdBy, updatedBy

INSERT INTO lab_tests (id, code, name, category, description, price, unit, normal_range, is_active, created_at, updated_at, created_by, updated_by)
VALUES
    -- LAB Category (Blood tests, biochemistry)
    (md5('labtest-1')::uuid, 'CBC', 'Complete Blood Count', 'LAB', 'Full blood cell analysis', 150000, 'cells/μL', '4.5-11.0', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-2')::uuid, 'BMP', 'Basic Metabolic Panel', 'LAB', 'Kidney function and electrolytes', 200000, 'mmol/L', 'varies', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-3')::uuid, 'CMP', 'Comprehensive Metabolic Panel', 'LAB', 'Extended metabolic test', 350000, 'mmol/L', 'varies', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-4')::uuid, 'LIPID', 'Lipid Panel', 'LAB', 'Cholesterol and triglycerides', 180000, 'mg/dL', '<200', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-5')::uuid, 'LFT', 'Liver Function Tests', 'LAB', 'Liver enzyme analysis', 220000, 'U/L', 'varies', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-6')::uuid, 'TSH', 'Thyroid Stimulating Hormone', 'LAB', 'Thyroid function', 250000, 'mIU/L', '0.4-4.0', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-7')::uuid, 'HBA1C', 'Hemoglobin A1c', 'LAB', 'Diabetes control marker', 180000, '%', '<5.7', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-8')::uuid, 'FBS', 'Fasting Blood Sugar', 'LAB', 'Glucose level', 80000, 'mg/dL', '70-100', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-9')::uuid, 'UA', 'Urinalysis', 'LAB', 'Urine analysis', 100000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-10')::uuid, 'PT', 'Prothrombin Time', 'LAB', 'Blood clotting test', 150000, 'seconds', '11-13.5', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-11')::uuid, 'BUN', 'Blood Urea Nitrogen', 'LAB', 'Kidney function marker', 120000, 'mg/dL', '7-20', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-12')::uuid, 'CREAT', 'Creatinine', 'LAB', 'Kidney function marker', 120000, 'mg/dL', '0.7-1.3', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-13')::uuid, 'ESR', 'Erythrocyte Sedimentation Rate', 'LAB', 'Inflammation marker', 100000, 'mm/hr', '<20', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-14')::uuid, 'CRP', 'C-Reactive Protein', 'LAB', 'Inflammation marker', 150000, 'mg/L', '<3', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-15')::uuid, 'FERR', 'Ferritin', 'LAB', 'Iron storage', 180000, 'ng/mL', '12-150', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-16')::uuid, 'VITD', 'Vitamin D', 'LAB', '25-hydroxy vitamin D', 300000, 'ng/mL', '30-100', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-17')::uuid, 'VITB12', 'Vitamin B12', 'LAB', 'Cobalamin level', 250000, 'pg/mL', '200-900', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-18')::uuid, 'URIC', 'Uric Acid', 'LAB', 'Gout marker', 120000, 'mg/dL', '3.5-7.2', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-19')::uuid, 'HIV', 'HIV Antibody Test', 'LAB', 'HIV screening', 200000, NULL, 'Negative', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-20')::uuid, 'HBSAG', 'Hepatitis B Surface Antigen', 'LAB', 'Hepatitis B screening', 180000, NULL, 'Negative', true, NOW(), NOW(), 'system', 'system'),
    
    -- IMAGING Category
    (md5('labtest-21')::uuid, 'XRAY_CHEST', 'Chest X-Ray', 'IMAGING', 'Chest radiograph PA view', 250000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-22')::uuid, 'XRAY_SPINE', 'Spine X-Ray', 'IMAGING', 'Spinal radiograph', 300000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-23')::uuid, 'XRAY_KNEE', 'Knee X-Ray', 'IMAGING', 'Knee joint radiograph', 200000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-24')::uuid, 'XRAY_HAND', 'Hand X-Ray', 'IMAGING', 'Hand and wrist radiograph', 180000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-25')::uuid, 'XRAY_ABDOMEN', 'Abdominal X-Ray', 'IMAGING', 'Abdominal plain film', 220000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-26')::uuid, 'USG_ABDOMEN', 'Abdominal Ultrasound', 'IMAGING', 'Upper/lower abdomen scan', 400000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-27')::uuid, 'USG_PELVIS', 'Pelvic Ultrasound', 'IMAGING', 'Pelvic organs scan', 350000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-28')::uuid, 'USG_THYROID', 'Thyroid Ultrasound', 'IMAGING', 'Thyroid gland scan', 300000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-29')::uuid, 'USG_BREAST', 'Breast Ultrasound', 'IMAGING', 'Breast tissue scan', 350000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-30')::uuid, 'ECG', 'Electrocardiogram', 'IMAGING', '12-lead ECG', 200000, NULL, 'Normal sinus rhythm', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-31')::uuid, 'ECHO', 'Echocardiogram', 'IMAGING', 'Heart ultrasound', 800000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-32')::uuid, 'CT_HEAD', 'CT Scan Head', 'IMAGING', 'Brain CT without contrast', 1500000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-33')::uuid, 'CT_CHEST', 'CT Scan Chest', 'IMAGING', 'Chest CT with contrast', 2000000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-34')::uuid, 'CT_ABDOMEN', 'CT Scan Abdomen', 'IMAGING', 'Abdominal CT with contrast', 2500000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-35')::uuid, 'MRI_BRAIN', 'MRI Brain', 'IMAGING', 'Brain MRI with contrast', 3500000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-36')::uuid, 'MRI_SPINE', 'MRI Spine', 'IMAGING', 'Spinal MRI', 3000000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-37')::uuid, 'MRI_KNEE', 'MRI Knee', 'IMAGING', 'Knee joint MRI', 2800000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-38')::uuid, 'MAMMO', 'Mammography', 'IMAGING', 'Breast screening', 500000, NULL, 'BI-RADS 1', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-39')::uuid, 'DEXA', 'Bone Density Scan', 'IMAGING', 'DEXA scan for osteoporosis', 600000, 'T-score', '>-1.0', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-40')::uuid, 'EEG', 'Electroencephalogram', 'IMAGING', 'Brain wave recording', 700000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    
    -- PATHOLOGY Category
    (md5('labtest-41')::uuid, 'BIOPSY_SKIN', 'Skin Biopsy', 'PATHOLOGY', 'Skin tissue analysis', 500000, NULL, 'Benign', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-42')::uuid, 'BIOPSY_LIVER', 'Liver Biopsy', 'PATHOLOGY', 'Liver tissue analysis', 1000000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-43')::uuid, 'BIOPSY_BREAST', 'Breast Biopsy', 'PATHOLOGY', 'Breast tissue analysis', 800000, NULL, 'Benign', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-44')::uuid, 'CYTO_PAP', 'Pap Smear', 'PATHOLOGY', 'Cervical cytology', 200000, NULL, 'NILM', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-45')::uuid, 'CYTO_URINE', 'Urine Cytology', 'PATHOLOGY', 'Urine cell analysis', 250000, NULL, 'Negative', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-46')::uuid, 'FNAC', 'Fine Needle Aspiration', 'PATHOLOGY', 'Cytology from aspiration', 400000, NULL, 'Benign', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-47')::uuid, 'IHC', 'Immunohistochemistry', 'PATHOLOGY', 'Tissue marker analysis', 1200000, NULL, 'varies', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-48')::uuid, 'FROZEN', 'Frozen Section', 'PATHOLOGY', 'Intraoperative pathology', 600000, NULL, 'varies', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-49')::uuid, 'SPERM', 'Semen Analysis', 'PATHOLOGY', 'Sperm count and motility', 300000, 'million/mL', '>15', true, NOW(), NOW(), 'system', 'system'),
    (md5('labtest-50')::uuid, 'STOOL', 'Stool Examination', 'PATHOLOGY', 'Fecal analysis', 100000, NULL, 'Normal', true, NOW(), NOW(), 'system', 'system')
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 9. APPOINTMENT SERVICE - SAMPLE APPOINTMENTS (for historical data)
-- ============================================================================
-- Create 500 past appointments for realistic load test data
-- Actual entity fields: id, patientId, patientName, doctorId, doctorName, doctorDepartment, appointmentTime, status (AppointmentStatus), type (AppointmentType), reason, notes, cancelledAt, cancelReason, queueNumber, priority, priorityReason, createdAt, updatedAt, createdBy, updatedBy

INSERT INTO appointment (id, patient_id, patient_name, doctor_id, doctor_name, doctor_department, appointment_time, status, type, reason, notes, queue_number, priority, created_at, updated_at, created_by, updated_by)
SELECT 
    md5('apt-hist-' || i)::uuid,
    md5('patient-' || (1 + (i % 1000)))::uuid,
    'Patient Name ' || (1 + (i % 1000)),
    md5('emp-doctor-' || (1 + (i % 60)))::uuid,
    'Dr. Name ' || (1 + (i % 60)),
    (ARRAY['Internal Medicine', 'Cardiology', 'Orthopedics', 'Pediatrics', 'Dermatology'])[1 + (i % 5)],
    NOW() - INTERVAL '30 days' + ((i * 30)::text || ' minutes')::interval,
    'COMPLETED',
    (ARRAY['CONSULTATION', 'FOLLOW_UP', 'WALK_IN'])[1 + (i % 3)],
    (ARRAY['Regular checkup', 'Follow-up visit', 'New symptoms', 'Routine examination', 'Health screening'])[1 + (i % 5)],
    'Historical appointment for load test',
    (i % 50) + 1,
    100,
    NOW() - INTERVAL '31 days',
    NOW() - INTERVAL '30 days',
    'system',
    'system'
FROM generate_series(1, 500) AS i
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- 10. VERIFICATION QUERIES
-- ============================================================================

-- Check data counts (Note: Table names use snake_case from entity @Table annotation)
DO $$
DECLARE
    v_accounts integer;
    v_patients integer;
    v_employees integer;
    v_schedules integer;
    v_departments integer;
    v_medicines integer;
    v_categories integer;
    v_lab_tests integer;
    v_appointments integer;
BEGIN
    SELECT COUNT(*) INTO v_accounts FROM accounts;
    SELECT COUNT(*) INTO v_patients FROM patient;
    SELECT COUNT(*) INTO v_employees FROM employees;
    SELECT COUNT(*) INTO v_schedules FROM employee_schedules;
    SELECT COUNT(*) INTO v_departments FROM departments;
    SELECT COUNT(*) INTO v_medicines FROM medicine;
    SELECT COUNT(*) INTO v_categories FROM category;
    SELECT COUNT(*) INTO v_lab_tests FROM lab_tests;
    SELECT COUNT(*) INTO v_appointments FROM appointment;
    
    RAISE NOTICE '============================================';
    RAISE NOTICE 'LOAD TEST DATA SEEDING COMPLETE';
    RAISE NOTICE '============================================';
    RAISE NOTICE 'Accounts: % (target: 1155)', v_accounts;
    RAISE NOTICE 'Patients: % (target: 1000)', v_patients;
    RAISE NOTICE 'Employees: % (target: 155)', v_employees;
    RAISE NOTICE 'Schedules: % (target: 420)', v_schedules;
    RAISE NOTICE 'Departments: % (target: 10)', v_departments;
    RAISE NOTICE 'Medicines: % (target: 200)', v_medicines;
    RAISE NOTICE 'Categories: % (target: 10)', v_categories;
    RAISE NOTICE 'Lab Tests: % (target: 50)', v_lab_tests;
    RAISE NOTICE 'Appointments: % (target: 500)', v_appointments;
    RAISE NOTICE '============================================';
END $$;

COMMIT;

-- Re-enable triggers
SET session_replication_role = DEFAULT;

-- ============================================================================
-- END OF SEED SCRIPT
-- ============================================================================
