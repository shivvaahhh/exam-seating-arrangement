-- ============================================================================
-- Smart Exam Hall Seating Arrangement System
-- Comprehensive Seed Data Script (v1.0.0)
-- 1 Shared Admin, 3 Invigilators, 25 Enrolled Students (15 CSE + 10 IT)
-- All passwords are encrypted with standard BCrypt ($2a$10$...)
-- ============================================================================

USE `exam_seating_db`;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Departments
INSERT INTO `departments` (`id`, `department_code`, `department_name`, `description`, `created_at`, `updated_at`) VALUES
(1, 'CSE', 'Computer Science and Engineering', 'Department of Computer Science & Engineering', NOW(), NOW()),
(2, 'IT', 'Information Technology', 'Department of Information Technology', NOW(), NOW()),
(3, 'ECE', 'Electronics and Communication Engineering', 'Department of Electronics & Communication', NOW(), NOW())
ON DUPLICATE KEY UPDATE `department_name`=VALUES(`department_name`);

-- 2. Subjects
INSERT INTO `subjects` (`id`, `subject_code`, `subject_name`, `semester`, `credits`, `department_id`, `created_at`, `updated_at`) VALUES
(1, 'CS601', 'Cloud Computing & Distributed Systems', 6, 4, 1, NOW(), NOW()),
(2, 'CS602', 'Machine Learning Foundations', 6, 4, 1, NOW(), NOW()),
(3, 'CS603', 'Computer Networks & Security', 6, 4, 1, NOW(), NOW()),
(4, 'IT601', 'Web Technologies & Frameworks', 6, 4, 2, NOW(), NOW()),
(5, 'IT602', 'Mobile Application Development', 6, 3, 2, NOW(), NOW()),
(6, 'EC601', 'Digital Signal Processing', 6, 4, 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE `subject_name`=VALUES(`subject_name`);

-- 3. Examination Halls
INSERT INTO `examination_halls` (`id`, `hall_name`, `building_name`, `floor`, `room_number`, `total_capacity`, `number_of_rows`, `number_of_columns`, `effective_capacity`, `active`, `description`, `created_at`, `updated_at`) VALUES
(1, 'Main Hall A', 'Administrative Block', 'Ground Floor', 'G-101', 30, 5, 6, 30, 1, 'Primary examination hall with staggered column seating layout.', NOW(), NOW()),
(2, 'Hall B - North Wing', 'Academic Block 1', '1st Floor', 'AB1-102', 24, 4, 6, 24, 1, 'Standard lecture hall configured for examinations.', NOW(), NOW()),
(3, 'Hall C - Tech Wing', 'Science Complex', '2nd Floor', 'SC-204', 20, 4, 5, 20, 1, 'Science complex examination wing.', NOW(), NOW())
ON DUPLICATE KEY UPDATE `total_capacity`=VALUES(`total_capacity`);

-- 4. Users Table
-- Passwords:
-- admin / admin123 -> $2a$10$w09B.kL8e8f8z7H.Vl8SNePzUvF3Hj3n9dFpLwJkC2m5t9Z8c9X2r
-- invigilator123 -> $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- 2024cse001..2024cse015 -> $2a$10$8s5R5HhPjI8jA...
-- (Note: Spring Boot's BCryptPasswordEncoder validates all bcrypt hashes on authentication)

-- Admin Shared Account
INSERT INTO `users` (`user_id`, `username`, `password_hash`, `email`, `role`, `active`, `must_change_password`, `created_at`, `updated_at`) VALUES
(1, 'admin', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', 'admin@college.edu', 'ROLE_ADMIN', 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `role`='ROLE_ADMIN';

-- Invigilator Accounts (must_change_password = 1)
INSERT INTO `users` (`user_id`, `username`, `password_hash`, `email`, `role`, `active`, `must_change_password`, `created_at`, `updated_at`) VALUES
(2, 'ramesh.sharma@college.edu', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', 'ramesh.sharma@college.edu', 'ROLE_INVIGILATOR', 1, 1, NOW(), NOW()),
(3, 'priya.sundaram@college.edu', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', 'priya.sundaram@college.edu', 'ROLE_INVIGILATOR', 1, 1, NOW(), NOW()),
(4, 'anand.kumar@college.edu', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', 'anand.kumar@college.edu', 'ROLE_INVIGILATOR', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `role`='ROLE_INVIGILATOR';

-- 5. Invigilators Table
INSERT INTO `invigilators` (`id`, `invigilator_code`, `name`, `email`, `phone_number`, `designation`, `active`, `department_id`, `user_id`, `created_at`, `updated_at`) VALUES
(1, 'INV-001', 'Dr. Ramesh Sharma', 'ramesh.sharma@college.edu', '9876500001', 'Associate Professor', 1, 1, 2, NOW(), NOW()),
(2, 'INV-002', 'Prof. Priya Sundaram', 'priya.sundaram@college.edu', '9876500002', 'Assistant Professor', 1, 2, 3, NOW(), NOW()),
(3, 'INV-003', 'Dr. Anand Kumar', 'anand.kumar@college.edu', '9876500003', 'Professor', 1, 3, 4, NOW(), NOW())
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`);

-- 6. Students & Student User Accounts (15 CSE + 10 IT = 25 Students)
-- CSE Students (2024CSE001 to 2024CSE015)
INSERT INTO `users` (`user_id`, `username`, `password_hash`, `email`, `role`, `active`, `must_change_password`, `created_at`, `updated_at`) VALUES
(101, '2024cse001', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse001@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(102, '2024cse002', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse002@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(103, '2024cse003', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse003@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(104, '2024cse004', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse004@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(105, '2024cse005', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse005@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(106, '2024cse006', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse006@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(107, '2024cse007', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse007@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(108, '2024cse008', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse008@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(109, '2024cse009', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse009@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(110, '2024cse010', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse010@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(111, '2024cse011', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse011@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(112, '2024cse012', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse012@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(113, '2024cse013', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse013@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(114, '2024cse014', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse014@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(115, '2024cse015', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024cse015@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
-- IT Students (2024IT001 to 2024IT010)
(116, '2024it001', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024it001@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(117, '2024it002', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024it002@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(118, '2024it003', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024it003@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(119, '2024it004', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024it004@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(120, '2024it005', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024it005@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(121, '2024it006', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024it006@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(122, '2024it007', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024it007@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(123, '2024it008', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024it008@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(124, '2024it009', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024it009@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW()),
(125, '2024it010', '$2a$10$7vj/H0Y2F3w.WwW0MhV9beKk1y9nN5l7wA31x3a8G5b6F2v4k1a2a', '2024it010@college.edu', 'ROLE_STUDENT', 1, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `email`=VALUES(`email`);

INSERT INTO `students` (`id`, `register_number`, `full_name`, `email`, `phone_number`, `semester`, `academic_year`, `status`, `department_id`, `user_id`, `created_at`, `updated_at`) VALUES
(1, '2024CSE001', 'Aarav Sharma', '2024cse001@college.edu', '9876543201', 6, '2026-2027', 'ACTIVE', 1, 101, NOW(), NOW()),
(2, '2024CSE002', 'Aditi Verma', '2024cse002@college.edu', '9876543202', 6, '2026-2027', 'ACTIVE', 1, 102, NOW(), NOW()),
(3, '2024CSE003', 'Ananya Patel', '2024cse003@college.edu', '9876543203', 6, '2026-2027', 'ACTIVE', 1, 103, NOW(), NOW()),
(4, '2024CSE004', 'Arjun Reddy', '2024cse004@college.edu', '9876543204', 6, '2026-2027', 'ACTIVE', 1, 104, NOW(), NOW()),
(5, '2024CSE005', 'Bhavya Nair', '2024cse005@college.edu', '9876543205', 6, '2026-2027', 'ACTIVE', 1, 105, NOW(), NOW()),
(6, '2024CSE006', 'Chetan Kumar', '2024cse006@college.edu', '9876543206', 6, '2026-2027', 'ACTIVE', 1, 106, NOW(), NOW()),
(7, '2024CSE007', 'Deepika Rao', '2024cse007@college.edu', '9876543207', 6, '2026-2027', 'ACTIVE', 1, 107, NOW(), NOW()),
(8, '2024CSE008', 'Gaurav Joshi', '2024cse008@college.edu', '9876543208', 6, '2026-2027', 'ACTIVE', 1, 108, NOW(), NOW()),
(9, '2024CSE009', 'Ishaan Malhotra', '2024cse009@college.edu', '9876543209', 6, '2026-2027', 'ACTIVE', 1, 109, NOW(), NOW()),
(10, '2024CSE010', 'Kavya Singh', '2024cse010@college.edu', '9876543210', 6, '2026-2027', 'ACTIVE', 1, 110, NOW(), NOW()),
(11, '2024CSE011', 'Manish Gupta', '2024cse011@college.edu', '9876543211', 6, '2026-2027', 'ACTIVE', 1, 111, NOW(), NOW()),
(12, '2024CSE012', 'Neha Deshmukh', '2024cse012@college.edu', '9876543212', 6, '2026-2027', 'ACTIVE', 1, 112, NOW(), NOW()),
(13, '2024CSE013', 'Pranav Kulkarni', '2024cse013@college.edu', '9876543213', 6, '2026-2027', 'ACTIVE', 1, 113, NOW(), NOW()),
(14, '2024CSE014', 'Rhea Sen', '2024cse014@college.edu', '9876543214', 6, '2026-2027', 'ACTIVE', 1, 114, NOW(), NOW()),
(15, '2024CSE015', 'Siddharth Roy', '2024cse015@college.edu', '9876543215', 6, '2026-2027', 'ACTIVE', 1, 115, NOW(), NOW()),
(16, '2024IT001', 'Karan Mehta', '2024it001@college.edu', '9876543301', 6, '2026-2027', 'ACTIVE', 2, 116, NOW(), NOW()),
(17, '2024IT002', 'Meera Krishnan', '2024it002@college.edu', '9876543302', 6, '2026-2027', 'ACTIVE', 2, 117, NOW(), NOW()),
(18, '2024IT003', 'Nikhil Pillai', '2024it003@college.edu', '9876543303', 6, '2026-2027', 'ACTIVE', 2, 118, NOW(), NOW()),
(19, '2024IT004', 'Pooja Bhatt', '2024it004@college.edu', '9876543304', 6, '2026-2027', 'ACTIVE', 2, 119, NOW(), NOW()),
(20, '2024IT005', 'Rahul Nambiar', '2024it005@college.edu', '9876543305', 6, '2026-2027', 'ACTIVE', 2, 120, NOW(), NOW()),
(21, '2024IT006', 'Sanjana Menon', '2024it006@college.edu', '9876543306', 6, '2026-2027', 'ACTIVE', 2, 121, NOW(), NOW()),
(22, '2024IT007', 'Tarun Bhatia', '2024it007@college.edu', '9876543307', 6, '2026-2027', 'ACTIVE', 2, 122, NOW(), NOW()),
(23, '2024IT008', 'Varun Kapoor', '2024it008@college.edu', '9876543308', 6, '2026-2027', 'ACTIVE', 2, 123, NOW(), NOW()),
(24, '2024IT009', 'Yashvardhan Jain', '2024it009@college.edu', '9876543309', 6, '2026-2027', 'ACTIVE', 2, 124, NOW(), NOW()),
(25, '2024IT010', 'Zoya Akhtar', '2024it010@college.edu', '9876543310', 6, '2026-2027', 'ACTIVE', 2, 125, NOW(), NOW())
ON DUPLICATE KEY UPDATE `full_name`=VALUES(`full_name`);

-- 7. Examinations
INSERT INTO `examinations` (`id`, `examination_name`, `examination_date`, `start_time`, `end_time`, `session`, `academic_year`, `semester`, `status`, `department_id`, `subject_id`, `created_at`, `updated_at`) VALUES
(1, 'B.Tech Semester VI Final: Cloud Computing', '2026-11-10', '09:30:00', '12:30:00', 'FORENOON', '2026-2027', 6, 'SCHEDULED', 1, 1, NOW(), NOW()),
(2, 'B.Tech Semester VI Final: Web Technologies', '2026-11-12', '09:30:00', '12:30:00', 'FORENOON', '2026-2027', 6, 'SCHEDULED', 2, 4, NOW(), NOW()),
(3, 'B.Tech Semester VI Final: Computer Networks', '2026-11-14', '14:00:00', '17:00:00', 'AFTERNOON', '2026-2027', 6, 'SCHEDULED', 1, 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE `examination_name`=VALUES(`examination_name`);

-- 8. Invigilator Assignments
INSERT INTO `invigilator_assignments` (`id`, `examination_id`, `examination_hall_id`, `invigilator_id`, `assigned_at`, `remarks`) VALUES
(1, 1, 1, 1, NOW(), 'Chief Proctor for Main Hall A'),
(2, 1, 2, 2, NOW(), 'Assistant Proctor for Hall B')
ON DUPLICATE KEY UPDATE `remarks`=VALUES(`remarks`);

SET FOREIGN_KEY_CHECKS = 1;
