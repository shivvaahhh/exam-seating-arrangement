package com.example.examseating.config;

import com.example.examseating.dto.AllocationRequestDto;
import com.example.examseating.entity.*;
import com.example.examseating.repository.*;
import com.example.examseating.service.SeatAllocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SubjectRepository subjectRepository;
    private final ExaminationHallRepository hallRepository;
    private final StudentRepository studentRepository;
    private final ExaminationRepository examinationRepository;
    private final ExaminationRegistrationRepository registrationRepository;
    private final SeatAllocationRepository allocationRepository;
    private final SeatAllocationService allocationService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.default-admin.username:admin}")
    private String adminUsername;

    @Value("${app.security.default-admin.password:admin123}")
    private String adminPassword;

    @Value("${app.security.default-admin.email:admin@college.edu}")
    private String adminEmail;

    public DataInitializer(UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           SubjectRepository subjectRepository,
                           ExaminationHallRepository hallRepository,
                           StudentRepository studentRepository,
                           ExaminationRepository examinationRepository,
                           ExaminationRegistrationRepository registrationRepository,
                           SeatAllocationRepository allocationRepository,
                           SeatAllocationService allocationService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.subjectRepository = subjectRepository;
        this.hallRepository = hallRepository;
        this.studentRepository = studentRepository;
        this.examinationRepository = examinationRepository;
        this.registrationRepository = registrationRepository;
        this.allocationRepository = allocationRepository;
        this.allocationService = allocationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking system data initialization...");

        // 1. Create Default Administrator
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = new User(adminUsername, passwordEncoder.encode(adminPassword), adminEmail, "ROLE_ADMIN");
            userRepository.save(admin);
            log.info("Initialized default administrator: {}", adminUsername);
        }

        // 2. Initialize Departments if empty
        Department cse, it, ece, mech, civil;
        if (departmentRepository.count() == 0) {
            cse = departmentRepository.save(new Department("Computer Science & Engineering", "CSE", "Department of Computer Science and Engineering"));
            it = departmentRepository.save(new Department("Information Technology", "IT", "Department of Information Technology"));
            ece = departmentRepository.save(new Department("Electronics & Communication", "ECE", "Department of Electronics and Communication Engineering"));
            mech = departmentRepository.save(new Department("Mechanical Engineering", "MECH", "Department of Mechanical Engineering"));
            civil = departmentRepository.save(new Department("Civil Engineering", "CIVIL", "Department of Civil Engineering"));
            log.info("Initialized 5 academic departments.");
        } else {
            cse = departmentRepository.findByDepartmentCode("CSE").orElse(null);
            it = departmentRepository.findByDepartmentCode("IT").orElse(null);
            ece = departmentRepository.findByDepartmentCode("ECE").orElse(null);
        }

        // 3. Initialize Subjects if empty
        Subject subDbms = null, subWeb = null, subNetworks = null;
        if (subjectRepository.count() == 0 && cse != null && it != null && ece != null) {
            subDbms = subjectRepository.save(new Subject("CS601", "Database Management Systems", cse, 6, 4));
            subjectRepository.save(new Subject("CS602", "Compiler Design", cse, 6, 3));
            subNetworks = subjectRepository.save(new Subject("CS603", "Computer Networks", cse, 6, 3));

            subWeb = subjectRepository.save(new Subject("IT601", "Web Technologies & Frameworks", it, 6, 4));
            subjectRepository.save(new Subject("IT602", "Cloud Computing & DevOps", it, 6, 3));

            subjectRepository.save(new Subject("EC601", "Digital Signal Processing", ece, 6, 4));
            subjectRepository.save(new Subject("EC602", "VLSI Design & Embedded Systems", ece, 6, 3));
            log.info("Initialized academic subjects.");
        } else if (cse != null) {
            subDbms = subjectRepository.findBySubjectCode("CS601").orElse(null);
            subWeb = subjectRepository.findBySubjectCode("IT601").orElse(null);
            subNetworks = subjectRepository.findBySubjectCode("CS603").orElse(null);
        }

        // 4. Initialize Examination Halls if empty
        ExaminationHall hallA = null, hallB = null, hallC = null;
        if (hallRepository.count() == 0) {
            hallA = new ExaminationHall("Main Hall A", "Academic Block 1", "Ground Floor", "AB1-G01", 30, 5, 6);
            hallA.setDescription("Main examination hall equipped with surveillance cameras and ventilation.");
            hallA = hallRepository.save(hallA);

            hallB = new ExaminationHall("Hall B - North Wing", "Academic Block 1", "1st Floor", "AB1-102", 24, 4, 6);
            hallB.setDescription("Standard lecture hall configured for examinations.");
            hallB = hallRepository.save(hallB);

            hallC = new ExaminationHall("Hall C - Tech Wing", "Science Complex", "2nd Floor", "SC-204", 20, 4, 5);
            hallC.setDescription("Science complex examination wing.");
            hallC = hallRepository.save(hallC);

            log.info("Initialized 3 examination halls.");
        } else {
            hallA = hallRepository.findByHallName("Main Hall A").orElse(null);
            hallB = hallRepository.findByHallName("Hall B - North Wing").orElse(null);
        }

        // 5. Initialize Students if empty
        if (studentRepository.count() == 0 && cse != null && it != null && ece != null) {
            List<Student> sampleStudents = new ArrayList<>();

            // 15 CSE students
            for (int i = 1; i <= 15; i++) {
                String reg = String.format("2024CSE%03d", i);
                String name = getSampleName("CSE", i);
                String email = reg.toLowerCase() + "@college.edu";
                String username = reg.toLowerCase();

                User user = new User(username, passwordEncoder.encode("password123"), email, "ROLE_STUDENT");
                user = userRepository.save(user);

                Student s = new Student();
                s.setRegisterNumber(reg);
                s.setFullName(name);
                s.setEmail(email);
                s.setPhoneNumber("98765432" + String.format("%02d", i));
                s.setDepartment(cse);
                s.setSemester(6);
                s.setAcademicYear("2026-2027");
                s.setStatus("ACTIVE");
                s.setUser(user);
                sampleStudents.add(studentRepository.save(s));
            }

            // 10 IT students
            for (int i = 1; i <= 10; i++) {
                String reg = String.format("2024IT%03d", i);
                String name = getSampleName("IT", i);
                String email = reg.toLowerCase() + "@college.edu";
                String username = reg.toLowerCase();

                User user = new User(username, passwordEncoder.encode("password123"), email, "ROLE_STUDENT");
                user = userRepository.save(user);

                Student s = new Student();
                s.setRegisterNumber(reg);
                s.setFullName(name);
                s.setEmail(email);
                s.setPhoneNumber("91234567" + String.format("%02d", i));
                s.setDepartment(it);
                s.setSemester(6);
                s.setAcademicYear("2026-2027");
                s.setStatus("ACTIVE");
                s.setUser(user);
                sampleStudents.add(studentRepository.save(s));
            }

            // Also create a demo student account with easy username "student" / "student123" linked to the first student
            if (!sampleStudents.isEmpty()) {
                Student firstStudent = sampleStudents.get(0);
                User demoUser = new User("student", passwordEncoder.encode("student123"), "student.demo@college.edu", "ROLE_STUDENT");
                demoUser = userRepository.save(demoUser);
                firstStudent.setUser(demoUser);
                studentRepository.save(firstStudent);
                log.info("Created demo student login: username='student', password='student123' (linked to {})", firstStudent.getRegisterNumber());
            }

            log.info("Initialized 25 sample students with individual logins (password: 'password123').");
        }

        // 6. Initialize Examinations if empty
        if (examinationRepository.count() == 0 && subDbms != null && cse != null && subWeb != null && it != null) {
            LocalDate tomorrow = LocalDate.now().plusDays(2);
            LocalDate nextWeek = LocalDate.now().plusDays(7);

            Examination exam1 = new Examination(
                    "B.Tech Semester VI Final: Database Management Systems",
                    subDbms,
                    cse,
                    6,
                    tomorrow,
                    LocalTime.of(9, 30),
                    LocalTime.of(12, 30),
                    "2026-2027",
                    "FORENOON"
            );
            exam1.setStatus("SCHEDULED");
            exam1 = examinationRepository.save(exam1);

            Examination exam2 = new Examination(
                    "B.Tech Semester VI Final: Web Technologies",
                    subWeb,
                    it,
                    6,
                    tomorrow,
                    LocalTime.of(14, 0),
                    LocalTime.of(17, 0),
                    "2026-2027",
                    "AFTERNOON"
            );
            exam2.setStatus("SCHEDULED");
            examinationRepository.save(exam2);

            if (subNetworks != null) {
                Examination exam3 = new Examination(
                        "B.Tech Semester VI Final: Computer Networks",
                        subNetworks,
                        cse,
                        6,
                        nextWeek,
                        LocalTime.of(9, 30),
                        LocalTime.of(12, 30),
                        "2026-2027",
                        "FORENOON"
                );
                exam3.setStatus("SCHEDULED");
                examinationRepository.save(exam3);
            }

            log.info("Initialized scheduled examinations.");

            // Register CSE students for exam 1
            List<Student> cseStudents = studentRepository.findByDepartmentIdAndSemester(cse.getId(), 6);
            for (Student s : cseStudents) {
                registrationRepository.save(new ExaminationRegistration(exam1, s));
            }
            log.info("Registered {} students for exam: {}", cseStudents.size(), exam1.getExaminationName());

            // Run initial automatic seat allocation for exam 1 so that the app immediately has live allocation data!
            if (hallA != null && hallB != null) {
                try {
                    AllocationRequestDto allocReq = new AllocationRequestDto();
                    allocReq.setExaminationId(exam1.getId());
                    allocReq.setHallIds(Arrays.asList(hallA.getId(), hallB.getId()));
                    allocReq.setSortStrategy("DEPT_REGNO");
                    allocReq.setForceRegenerate(false);

                    allocationService.allocateSeats(allocReq);
                    log.info("Successfully pre-allocated seats for demo exam 1!");
                } catch (Exception ex) {
                    log.error("Initial allocation failed: {}", ex.getMessage());
                }
            }
        }

        log.info("Data initialization check completed successfully.");
    }

    private String getSampleName(String dept, int index) {
        String[] firstNames = {
                "Aarav", "Aditi", "Bhavna", "Chirag", "Deepak", "Divya", "Gaurav", "Harsh",
                "Ishaan", "Kavya", "Manish", "Neha", "Pranav", "Pooja", "Rahul", "Riya",
                "Sanjay", "Sneha", "Tanvi", "Varun"
        };
        String[] lastNames = {"Sharma", "Verma", "Patel", "Reddy", "Nair", "Iyer", "Rao", "Kumar", "Singh", "Gupta"};
        String first = firstNames[(index - 1) % firstNames.length];
        String last = lastNames[(index - 1) % lastNames.length];
        return first + " " + last;
    }
}
