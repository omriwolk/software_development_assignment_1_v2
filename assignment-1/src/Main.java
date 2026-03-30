import java.util.*; // Scanner, List, ArrayList, Comparator
import java.sql.*;   // Placeholder if you integrate JDBC later
import java.io.*;    // For CSV export prompts

// Main class is strictly UI logic
public class Main {

    private static final Scanner scanner = new Scanner(System.in); // Scanner for console input

    public static void main(String[] args) throws SQLException {
        while (true) {
            // === STUDENT SELECTION MENU ===
            System.out.println("\n=== STUDENT SELECTION ===");
            System.out.println("1) Select student by ID");
            System.out.println("2) Select all students");
            System.out.println("0) Exit");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim(); // Read user input

            List<SGC.Student> selectedStudents = new ArrayList<>(); // List to hold selected students

            if (choice.equals("0")) break; // Exit program
            else if (choice.equals("1")) { // Select a single student
                System.out.print("Enter Student ID: ");
                int sid = Integer.parseInt(scanner.nextLine().trim());
                try {
                    selectedStudents.add(new SGC.Student(sid)); // Load student from SGC
                } catch (IllegalArgumentException e) {
                    System.out.println("Student not found."); // Invalid ID handling
                    continue;
                }
            } else if (choice.equals("2")) { // Select all students
                selectedStudents = SGC.loadAllStudents(); // Load all students from SGC
                if (selectedStudents.isEmpty()) {
                    System.out.println("No students found.");
                    continue;
                }
            } else {
                System.out.println("Invalid choice."); // Invalid menu choice
                continue;
            }

            // === REPORT MENU LOOP ===
            while (true) {
                System.out.println("\n=== REPORT MENU ===");
                System.out.println("1) Assessment Report");
                System.out.println("2) Module Report");
                System.out.println("3) Degree Report");
                System.out.println("0) Back");
                System.out.print("Choose an option: ");
                String reportChoice = scanner.nextLine().trim();

                if (reportChoice.equals("0")) break; // Go back to student selection

                switch (reportChoice) {
                    case "1":
                        assessmentReport(selectedStudents); // Call assessment report
                        break;
                    case "2":
                        moduleReport(selectedStudents); // Call module report
                        break;
                    case "3":
                        if (selectedStudents.size() > 1)
                            degreeReport(selectedStudents); // Only for multiple students
                        else
                            System.out.println("Degree report filtering by category only works for multiple students.");
                        break;
                    default:
                        System.out.println("Invalid option."); // Invalid report menu choice
                }
            }
        }

        System.out.println("Goodbye!"); // Exit message
    }

    // === ASSESSMENT REPORT ===
    private static void assessmentReport(List<SGC.Student> students) {
        System.out.println("\n=== ASSESSMENT REPORT ===");
        System.out.println("1) Select Assessment by ID");
        System.out.println("2) Print all assessments");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            // User selects specific assessment ID
            System.out.print("Enter Assessment ID: ");
            int aid = Integer.parseInt(scanner.nextLine().trim());
            List<SGC.Assessment> found = new ArrayList<>();
            for (SGC.Student s : students) {
                for (SGC.Assessment a : s.getAllAssessments()) {
                    if (a.getId() == aid) found.add(a);
                }
            }
            if (found.isEmpty()) System.out.println("Assessment not found.");
            else SGC.printAssessments(found, scanner);
        } else if (choice.equals("2")) {
            // Print all assessments for selected students
            List<SGC.Assessment> all = new ArrayList<>();
            for (SGC.Student s : students) all.addAll(s.getAllAssessments());

            if (!all.isEmpty()) {
                // Prompt to rank by grade
                System.out.print("Rank by grade? (y/n): ");
                String rankChoice = scanner.nextLine().trim();
                if (rankChoice.equalsIgnoreCase("y"))
                    all.sort(Comparator.comparingDouble(SGC.Assessment::getContributionToModuleScore).reversed());
            }

            SGC.printAssessments(all, scanner); // Print all assessments
        } else System.out.println("Invalid choice.");
    }

    // === MODULE REPORT ===
    private static void moduleReport(List<SGC.Student> students) {
        System.out.println("\n=== MODULE REPORT ===");
        System.out.println("1) Select Module by ID");
        System.out.println("2) Print all modules");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            System.out.print("Enter Module ID: ");
            int mid = Integer.parseInt(scanner.nextLine().trim());
            List<SGC.Module> found = new ArrayList<>();
            for (SGC.Student s : students) {
                SGC.Module m = s.getModuleById(mid);
                if (m != null) found.add(m);
            }
            if (found.isEmpty()) System.out.println("Module not found.");
            else SGC.printModules(found, students, scanner);
        } else if (choice.equals("2")) {
            List<SGC.Module> allModules = new ArrayList<>();
            for (SGC.Student s : students) allModules.addAll(s.getModules());

            // Prompt to rank by numeric grade
            System.out.print("Rank by grade? (y/n): ");
            String rankChoice = scanner.nextLine().trim();
            if (rankChoice.equalsIgnoreCase("y"))
                allModules.sort(Comparator.comparingDouble(SGC.Module::calculateModuleScore).reversed());

            SGC.printModules(allModules, students, scanner);
        } else System.out.println("Invalid choice.");
    }

    // === DEGREE REPORT ===
    private static void degreeReport(List<SGC.Student> students) {
        // Only for multiple students
        System.out.println("\nSelect Grade Category:");
        System.out.println("1) First");
        System.out.println("2) Upper Second");
        System.out.println("3) Lower Second");
        System.out.println("4) Third");
        System.out.println("5) Fail");
        System.out.print("Choice: ");
        String catChoice = scanner.nextLine().trim();

        double[] thresholds = {70, 60, 50, 40}; // Grade thresholds
        List<SGC.Student> filtered = new ArrayList<>();
        for (SGC.Student s : students) {
            double score = s.calculateDegreeScore();
            switch (catChoice) {
                case "1": if (score >= thresholds[0]) filtered.add(s); break;
                case "2": if (score >= thresholds[1] && score < thresholds[0]) filtered.add(s); break;
                case "3": if (score >= thresholds[2] && score < thresholds[1]) filtered.add(s); break;
                case "4": if (score >= thresholds[3] && score < thresholds[2]) filtered.add(s); break;
                case "5": if (score < thresholds[3]) filtered.add(s); break;
                default: System.out.println("Invalid choice."); return;
            }
        }

        if (filtered.isEmpty()) System.out.println("No students in this category.");
        else SGC.printDegrees(filtered, scanner);
    }
}