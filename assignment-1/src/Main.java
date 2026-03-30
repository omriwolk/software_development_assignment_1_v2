import java.sql.SQLException; // Needed to handle database errors
import java.util.*;           // Needed for List, ArrayList, Scanner, Set, etc.

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in); // Scanner for user input

        try {

            // Infinite loop for program (until user exits)
            while (true) {

                // === STUDENT SELECTION MENU ===
                System.out.println("\n=== STUDENT SELECTION ===");

                // Load all student IDs from database
                List<Integer> studentIDs = SGC.loadAllStudentIDs();

                // Print all available student IDs
                System.out.println("Available student IDs:");
                for (int id : studentIDs) {
                    System.out.println(id); // Print each ID line by line
                }

                // Show input instructions
                System.out.println("\nEnter:");
                System.out.println("- Student ID");         // select one student
                System.out.println("- 0 for ALL students"); // select all students
                System.out.println("- 'exit' to quit");     // exit program
                System.out.print("Choice: ");

                // Read user input as string
                String input = sc.nextLine().trim();

                // If user types "exit" → terminate program
                if (input.equalsIgnoreCase("exit")) break;

                // List to hold selected students
                List<SGC.Student> selectedStudents = new ArrayList<>();

                // If user selects ALL students
                if (input.equals("0")) {
                    // Load each student from DB and add to list
                    for (int id : studentIDs) {
                        selectedStudents.add(SGC.loadStudent(id));
                    }
                } else {
                    // Otherwise, user entered a specific student ID
                    int sid = Integer.parseInt(input); // convert to int
                    selectedStudents.add(SGC.loadStudent(sid)); // load that student
                }

                // === REPORT MENU LOOP ===
                boolean back = false; // flag to go back to student menu

                while (!back) {

                    // Print report options
                    System.out.println("\n=== REPORT MENU ===");
                    System.out.println("1) Assessment report");
                    System.out.println("2) Module report");
                    System.out.println("3) Degree report");
                    System.out.println("4) Back");
                    System.out.print("Choice: ");

                    int choice = Integer.parseInt(sc.nextLine()); // read user choice

                    switch (choice) {

                        // =========================
                        // 1) ASSESSMENT REPORT
                        // =========================
                        case 1 -> {

                            System.out.println("\nAvailable Assessment IDs:");

                            // Use Set to avoid duplicate IDs
                            Set<Integer> ids = new HashSet<>();

                            // Collect all assessment IDs from selected students
                            for (SGC.Student s : selectedStudents) {
                                for (SGC.Module m : s.getModules()) {
                                    for (SGC.Assessment a : m.getAssessments()) {
                                        ids.add(a.getAssessmentID());
                                    }
                                }
                            }

                            // Print all unique assessment IDs
                            for (int id : ids) {
                                System.out.println(id);
                            }

                            // Ask user to select assessment
                            System.out.print("Enter Assessment ID or 0 for ALL: ");
                            int aid = Integer.parseInt(sc.nextLine());

                            // Print table header
                            System.out.println("\nID | Student | Module | Type | Awarded/Max | Weight");

                            // Loop through all selected students
                            for (SGC.Student s : selectedStudents) {
                                for (SGC.Module m : s.getModules()) {
                                    for (SGC.Assessment a : m.getAssessments()) {

                                        // Show either all or specific assessment
                                        if (aid == 0 || a.getAssessmentID() == aid) {

                                            // Print formatted row
                                            System.out.printf("%d | %d | %d | %s | %.1f/%s | %.1f%%\n",
                                                    a.getAssessmentID(), // assessment ID
                                                    s.getStudentID(),    // student ID
                                                    a.getModuleID(),     // module ID
                                                    a.getType(),         // type (Exam, etc.)
                                                    a.getAwardedMarks() != null ? a.getAwardedMarks() : 0.0, // awarded marks
                                                    a.getMaxMarks() != null ? a.getMaxMarks() : "-",         // max marks or "-"
                                                    a.getWeight());      // weight %
                                        }
                                    }
                                }
                            }
                        }

                        // =========================
                        // 2) MODULE REPORT
                        // =========================
                        case 2 -> {

                            System.out.println("\nAvailable Module IDs:");

                            // Use Set to avoid duplicates
                            Set<Integer> moduleIDs = new HashSet<>();

                            // Collect all module IDs
                            for (SGC.Student s : selectedStudents) {
                                for (SGC.Module m : s.getModules()) {
                                    moduleIDs.add(m.getModuleID());
                                }
                            }

                            // Print module IDs
                            for (int id : moduleIDs) {
                                System.out.println(id);
                            }

                            // Ask user for module selection
                            System.out.print("Enter Module ID or 0 for ALL: ");
                            int mid = Integer.parseInt(sc.nextLine());

                            // Print header
                            System.out.println("\nModule | Student | Level | Grade");

                            // Loop through students and modules
                            for (SGC.Student s : selectedStudents) {
                                for (SGC.Module m : s.getModules()) {

                                    // Filter condition
                                    if (mid == 0 || m.getModuleID() == mid) {

                                        // Print module row
                                        System.out.printf("%d | %d | %d | %.2f\n",
                                                m.getModuleID(),   // module ID
                                                s.getStudentID(),  // student ID
                                                m.getLevel(),      // module level
                                                m.getModuleScore() // calculated grade
                                        );
                                    }
                                }
                            }
                        }

                        // =========================
                        // 3) DEGREE REPORT
                        // =========================
                        case 3 -> {

                            // Print header
                            System.out.println("\nStudent | Degree | Classification");

                            // Loop through selected students
                            for (SGC.Student s : selectedStudents) {

                                // Print degree score and classification
                                System.out.printf("%d | %.2f | %s\n",
                                        s.getStudentID(),      // student ID
                                        s.getDegreeScore(),    // degree score
                                        s.getDegreeClass());   // classification
                            }
                        }

                        // =========================
                        // 4) BACK
                        // =========================
                        case 4 -> back = true; // exit report menu

                        // Handle invalid input
                        default -> System.out.println("Invalid choice");
                    }
                }
            }

        } catch (SQLException e) {
            // Catch any database-related errors
            System.out.println("DB Error: " + e.getMessage());
        }

        sc.close(); // Close scanner when program ends
    }
}