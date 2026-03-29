import java.sql.*;                  // JDBC classes: Connection, PreparedStatement, ResultSet, SQLException
import java.util.*;                 // List, ArrayList, Scanner
import java.util.stream.Collectors; // For stream filtering

public class Main {

    public static void main(String[] args) {

        try (Scanner scanner = new Scanner(System.in)) { // Scanner to read console input

            while (true) { // Main program loop

                // ==========================
                // 1️⃣ Prompt user for student ID
                // ==========================
                System.out.print("Enter student ID or 'all' for all students (or 'exit' to quit): ");
                String studentInput = scanner.nextLine().trim(); // Read input
                if (studentInput.equalsIgnoreCase("exit")) break; // Exit program

                List<Student> students = new ArrayList<>(); // List of students to process

                try (Connection conn = DBConnection.getConnection()) { // Open DB connection

                    // If 'all' students requested
                    if (studentInput.equalsIgnoreCase("all")) {
                        String studentQuery = "SELECT DISTINCT StudentID FROM StudentEnrolment"; // SQL
                        try (PreparedStatement stmt = conn.prepareStatement(studentQuery);
                             ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                students.add(new Student(rs.getInt("StudentID"))); // Load student objects
                            }
                        }
                        if (students.isEmpty()) { // Check DB empty
                            System.out.println(ConsoleColors.RED + "No students found in the database." + ConsoleColors.RESET);
                            continue; // Back to main prompt
                        }

                    } else { // Specific student ID
                        try {
                            int sid = Integer.parseInt(studentInput); // Parse ID
                            if (!DBValidator.exists("StudentEnrolment", "StudentID", sid)) { // Check existence
                                System.out.println(ConsoleColors.RED + "Student ID " + sid + " does not exist." + ConsoleColors.RESET);
                                continue;
                            }
                            students.add(new Student(sid));
                        } catch (NumberFormatException e) { // Invalid format
                            System.out.println(ConsoleColors.RED + "Invalid student ID format." + ConsoleColors.RESET);
                            continue;
                        }
                    }

                    // ==========================
                    // 2️⃣ Prompt for report type
                    // ==========================
                    System.out.println("Select report type:");
                    System.out.println("1) Assessment");
                    System.out.println("2) Module");
                    System.out.println("3) Degree");
                    System.out.print("Enter 1, 2, or 3: ");
                    String typeInput = scanner.nextLine().trim();

                    boolean degreeReport = false;   // Flag for full degree report
                    Integer filterModuleID = null;  // Optional module filter
                    Integer filterAssessmentID = null; // Optional assessment filter

                    if (typeInput.equals("1")) { // Assessment
                        System.out.print("Enter AssessmentID: "); // Corrected name
                        String aidInput = scanner.nextLine().trim();
                        try {
                            filterAssessmentID = Integer.parseInt(aidInput);
                        } catch (NumberFormatException e) {
                            System.out.println(ConsoleColors.RED + "Invalid AssessmentID format." + ConsoleColors.RESET);
                            continue;
                        }

                    } else if (typeInput.equals("2")) { // Module
                        System.out.print("Enter ModuleID: ");
                        String midInput = scanner.nextLine().trim();
                        try {
                            filterModuleID = Integer.parseInt(midInput);
                        } catch (NumberFormatException e) {
                            System.out.println(ConsoleColors.RED + "Invalid ModuleID format." + ConsoleColors.RESET);
                            continue;
                        }

                    } else if (typeInput.equals("3")) { // Degree
                        degreeReport = true; // No extra input
                    } else { // Invalid selection
                        System.out.println(ConsoleColors.RED + "Invalid selection. Enter 1, 2, or 3." + ConsoleColors.RESET);
                        continue;
                    }

                    // ==========================
                    // 3️⃣ Print table header
                    // ==========================
                    System.out.printf("%-10s %-10s %-8s %-12s %-12s %-35s%n",
                            "StudentID", "ModuleID", "Level", "AssessID", "Score", "Grade");
                    System.out.println("================================================================================");

                    // ==========================
                    // 4️⃣ Process each student
                    // ==========================
                    for (Student student : students) {
                        List<Module> modules = student.getModules(); // Get all modules
                        List<Module> filteredModules = modules;      // Start with all modules

                        // Apply module filter if needed
                        if (filterModuleID != null) {
                            final Integer moduleFilter = filterModuleID; // final for lambda
                            filteredModules = modules.stream()
                                    .filter(m -> m.getId() == moduleFilter)
                                    .collect(Collectors.toList());
                            if (filteredModules.isEmpty()) {
                                System.out.println(ConsoleColors.RED + "Module ID " + filterModuleID +
                                        " not found for student " + student.getId() + ConsoleColors.RESET);
                                continue; // skip this student
                            }
                        }

                        // If filtering by assessment
                        if (filterAssessmentID != null) {
                            final Integer assessmentFilter = filterAssessmentID;

                            // Check if assessment exists anywhere for this student
                            boolean assessmentExists = modules.stream()
                                    .flatMap(m -> m.getAssessments().stream())
                                    .anyMatch(a -> a.getId() == assessmentFilter);

                            if (!assessmentExists) { // Not found anywhere
                                System.out.println(ConsoleColors.RED + "Assessment ID " + filterAssessmentID +
                                        " not found for student " + student.getId() + ConsoleColors.RESET);
                                continue; // skip this student
                            }

                            // Filter modules to only those that contain the assessment
                            filteredModules = filteredModules.stream()
                                    .filter(m -> m.getAssessments().stream()
                                            .anyMatch(a -> a.getId() == assessmentFilter))
                                    .collect(Collectors.toList());
                        }

                        // ==========================
                        // 5️⃣ Loop modules and assessments
                        // ==========================
                        for (Module m : filteredModules) {
                            List<Assessment> assessments = m.getAssessments();

                            // Filter assessments if needed
                            if (filterAssessmentID != null) {
                                final Integer assessmentFilter = filterAssessmentID;
                                assessments = assessments.stream()
                                        .filter(a -> a.getId() == assessmentFilter)
                                        .collect(Collectors.toList());
                            }

                            // Print assessment rows
                            for (Assessment a : assessments) {
                                double score = a.isCompleted() ? a.getContributionToModuleScore() : 0.0;
                                String grade = a.isCompleted() ? GradeClassifier.classify(score)
                                        : ConsoleColors.RED + "Fail - You shall NOT PASS!" + ConsoleColors.RESET;

                                System.out.printf("%-10d %-10d %-8d %-12d %-12.2f %-35s%n",
                                        student.getId(),
                                        m.getId(),
                                        m.getLevel(),
                                        a.getId(),
                                        score,
                                        grade
                                );
                            }

                            // If degree report, print module summary
                            if (degreeReport) {
                                System.out.printf("%-10d %-10d %-8d %-12s %-12.2f %-35s%n",
                                        student.getId(),
                                        m.getId(),
                                        m.getLevel(),
                                        "-",
                                        m.calculateModuleScore(),
                                        m.getModuleGrade());
                            }
                        }

                        // Print overall degree if requested
                        if (degreeReport) {
                            System.out.printf("%-10d %-10s %-8s %-12s %-12.2f %-35s%n",
                                    student.getId(),
                                    "-", "-", "-",
                                    student.calculateDegreeScore(),
                                    "Degree Grade: " + student.getDegreeGrade());
                            System.out.println("================================================================================");
                        }

                    } // End student loop

                } catch (SQLException e) { // Catch DB errors
                    e.printStackTrace();
                }

            } // End main while loop

        } catch (Exception e) { // Catch-all
            e.printStackTrace();
        }

    } // End main

} // End class Main