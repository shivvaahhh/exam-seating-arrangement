-- ============================================================================
-- Smart Exam Hall Seating Arrangement System
-- Comprehensive MySQL Database Schema & Table Definitions (v1.0.0)
-- Compatible with MySQL 8.0+ and MariaDB 10.5+
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `exam_seating_db` 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE `exam_seating_db`;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Users Table (Authentication & RBAC)
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(60) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `email` VARCHAR(120) NOT NULL,
  `role` VARCHAR(30) NOT NULL COMMENT 'ROLE_ADMIN, ROLE_STUDENT, ROLE_INVIGILATOR',
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  `must_change_password` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NULL,
  `updated_at` DATETIME NULL,
  `last_login` DATETIME NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_users_username` (`username`),
  KEY `idx_users_role` (`role`),
  KEY `idx_users_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Departments Table
DROP TABLE IF EXISTS `departments`;
CREATE TABLE `departments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `department_code` VARCHAR(10) NOT NULL,
  `department_name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NULL,
  `updated_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_departments_code` (`department_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Subjects Table
DROP TABLE IF EXISTS `subjects`;
CREATE TABLE `subjects` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `subject_code` VARCHAR(20) NOT NULL,
  `subject_name` VARCHAR(120) NOT NULL,
  `semester` INT NOT NULL,
  `credits` INT NOT NULL DEFAULT 3,
  `department_id` BIGINT NOT NULL,
  `created_at` DATETIME NULL,
  `updated_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subjects_code` (`subject_code`),
  KEY `idx_subjects_dept` (`department_id`),
  KEY `idx_subjects_sem` (`semester`),
  CONSTRAINT `fk_subjects_dept` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Examination Halls Table
DROP TABLE IF EXISTS `examination_halls`;
CREATE TABLE `examination_halls` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `hall_name` VARCHAR(80) NOT NULL,
  `building_name` VARCHAR(80) NOT NULL,
  `floor` VARCHAR(30) DEFAULT NULL,
  `room_number` VARCHAR(30) NOT NULL,
  `total_capacity` INT NOT NULL,
  `number_of_rows` INT NOT NULL,
  `number_of_columns` INT NOT NULL,
  `effective_capacity` INT NOT NULL,
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  `description` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NULL,
  `updated_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_halls_name` (`hall_name`),
  KEY `idx_halls_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Students Table
DROP TABLE IF EXISTS `students`;
CREATE TABLE `students` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `register_number` VARCHAR(30) NOT NULL,
  `full_name` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `phone_number` VARCHAR(20) DEFAULT NULL,
  `semester` INT NOT NULL,
  `academic_year` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE',
  `department_id` BIGINT NOT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME NULL,
  `updated_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_students_reg_no` (`register_number`),
  UNIQUE KEY `uk_students_email` (`email`),
  UNIQUE KEY `uk_students_user_id` (`user_id`),
  KEY `idx_students_dept` (`department_id`),
  KEY `idx_students_sem` (`semester`),
  KEY `idx_students_status` (`status`),
  CONSTRAINT `fk_students_dept` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_students_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Examinations Table
DROP TABLE IF EXISTS `examinations`;
CREATE TABLE `examinations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `examination_name` VARCHAR(120) NOT NULL,
  `examination_date` DATE NOT NULL,
  `start_time` TIME NOT NULL,
  `end_time` TIME NOT NULL,
  `session` VARCHAR(20) NOT NULL COMMENT 'FORENOON, AFTERNOON',
  `academic_year` VARCHAR(20) NOT NULL,
  `semester` INT NOT NULL,
  `status` VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED',
  `department_id` BIGINT NOT NULL,
  `subject_id` BIGINT NOT NULL,
  `created_at` DATETIME NULL,
  `updated_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_exams_date_session` (`examination_date`, `session`),
  KEY `idx_exams_dept_sem` (`department_id`, `semester`),
  KEY `idx_exams_subject` (`subject_id`),
  CONSTRAINT `fk_exams_dept` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_exams_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Examination Registrations Table
DROP TABLE IF EXISTS `examination_registrations`;
CREATE TABLE `examination_registrations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `registration_date` DATETIME NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'REGISTERED' COMMENT 'REGISTERED, CANCELLED',
  `examination_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reg_exam_student` (`examination_id`, `student_id`),
  KEY `idx_reg_student` (`student_id`),
  CONSTRAINT `fk_reg_exam` FOREIGN KEY (`examination_id`) REFERENCES `examinations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_reg_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Invigilators Table
DROP TABLE IF EXISTS `invigilators`;
CREATE TABLE `invigilators` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `invigilator_code` VARCHAR(20) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `phone_number` VARCHAR(20) DEFAULT NULL,
  `designation` VARCHAR(80) NOT NULL,
  `active` TINYINT(1) NOT NULL DEFAULT 1,
  `department_id` BIGINT NOT NULL,
  `user_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME NULL,
  `updated_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invigilators_code` (`invigilator_code`),
  UNIQUE KEY `uk_invigilators_email` (`email`),
  UNIQUE KEY `uk_invigilators_user_id` (`user_id`),
  KEY `idx_invigilators_dept` (`department_id`),
  KEY `idx_invigilators_active` (`active`),
  CONSTRAINT `fk_inv_dept` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_inv_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Invigilator Assignments Table
DROP TABLE IF EXISTS `invigilator_assignments`;
CREATE TABLE `invigilator_assignments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `examination_id` BIGINT NOT NULL,
  `examination_hall_id` BIGINT NOT NULL,
  `invigilator_id` BIGINT NOT NULL,
  `assigned_at` DATETIME NOT NULL,
  `remarks` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assign_exam_hall` (`examination_id`, `examination_hall_id`),
  KEY `idx_assign_invigilator` (`invigilator_id`),
  CONSTRAINT `fk_assign_exam` FOREIGN KEY (`examination_id`) REFERENCES `examinations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_assign_hall` FOREIGN KEY (`examination_hall_id`) REFERENCES `examination_halls` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_assign_inv` FOREIGN KEY (`invigilator_id`) REFERENCES `invigilators` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Seat Allocations Table
DROP TABLE IF EXISTS `seat_allocations`;
CREATE TABLE `seat_allocations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `row_number` INT NOT NULL,
  `column_number` INT NOT NULL,
  `seat_number` VARCHAR(20) NOT NULL,
  `allocated_at` DATETIME NOT NULL,
  `attendance_status` VARCHAR(20) NOT NULL DEFAULT 'NOT_MARKED' COMMENT 'NOT_MARKED, PRESENT, ABSENT, MALPRACTICE',
  `examination_id` BIGINT NOT NULL,
  `examination_hall_id` BIGINT NOT NULL,
  `student_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alloc_exam_hall_seat` (`examination_id`, `examination_hall_id`, `seat_number`),
  UNIQUE KEY `uk_alloc_exam_student` (`examination_id`, `student_id`),
  KEY `idx_alloc_exam_hall` (`examination_id`, `examination_hall_id`),
  KEY `idx_alloc_student` (`student_id`),
  CONSTRAINT `fk_alloc_exam` FOREIGN KEY (`examination_id`) REFERENCES `examinations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_alloc_hall` FOREIGN KEY (`examination_hall_id`) REFERENCES `examination_halls` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_alloc_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
