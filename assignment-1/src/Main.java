// ==========================
// File: Main.java
// ==========================
import java.util.*;
import java.sql.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.*;

// ==========================
// Main CLI class
// ==========================
public class Main {

    private static final Scanner scanner = new Scanner(System.in); // Scanner for CLI input
    private static final int PAGE_SIZE = 10;                        // Pagination size

    public static void main(String[] args) throws SQLException {
        System.out.println("=== Welcome to SGC CLI ===");

        while (true) {
            System.out.print("\nEnter student ID (or 'exit'): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) break;

            int studentId;
            try {
                studentId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid student ID.");
                continue;
            }

            Student student;
            try {
                student = new Student(studentId); // Load student & modules
            } catch (IllegalArgumentException e) {
                System.out.println("Student not found.");
                continue;
            }

            studentMenu(student); // Enter main student menu
        }
        System.out.println("Goodbye!");
    }

    // ==========================
    // Student main menu
    // ==========================
    private static void studentMenu(Student student) throws SQLException {
        while (true) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1) Assessment Report");
            System.out.println("2) Module Report");
            System.out.println("3) Degree Report");
            System.out.println("0) Back");
            System.out.print("Select option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1": assessmentMenu(student); break;
                case "2": moduleMenu(student); break;
                case "3": degreeMenu(student); break;
                case "0": return; // Back to previous menu
                default: System.out.println("Invalid choice.");
            }
        }
    }

    // ==========================
    // Assessment menu
    // ==========================
    private static void assessmentMenu(Student student) throws SQLException {
        while (true) {
            System.out.println("\n--- Assessment Report ---");
            System.out.println("0) Back");
            System.out.print("Enter Assessment ID: ");
            String input = scanner.nextLine().trim();

            if (input.equals("0")) return; // Back

            int assessmentId;
            try {
                assessmentId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid assessment ID.");
                continue;
            }

            Assessment assessment = null;
            for (Module m : student.getModules()) {
                for (Assessment a : m.getAssessments()) {
                    if (a.getId() == assessmentId) {
                        assessment = a;
                        break;
                    }
                }
            }

            if (assessment == null) {
                System.out.println("Assessment not found.");
                continue;
            }

            printAssessmentTable(List.of(assessment)); // Single-assessment table
            exportAssessmentsCSV(student.getId(), List.of(assessment)); // Optionally export CSV
        }
    }

    // ==========================
    // Module menu
    // ==========================
    private static void moduleMenu(Student student) throws SQLException {
        while (true) {
            System.out.println("\n--- Module Report ---");
            System.out.println("0) Back");
            System.out.println("Available Modules:");
            for (Module m : student.getModules()) {
                System.out.printf("%d) Module %d%n", m.getId(), m.getId());
            }
            System.out.print("Enter Module ID (or 0 for all modules): ");
            String input = scanner.nextLine().trim();

            int moduleId;
            try {
                moduleId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                continue;
            }

            List<Module> selectedModules;
            if (moduleId == 0) selectedModules = student.getModules();
            else {
                Module m = student.getModuleById(moduleId);
                if (m == null) {
                    System.out.println("Module not found.");
                    continue;
                }
                selectedModules = List.of(m);
            }

            // Prompt for numeric grade filter
            System.out.println("Enter minimum score to include (0 = no filter): ");
            double minScore;
            try {
                minScore = Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                minScore = 0;
            }

            // Prompt for sorting
            System.out.println("Sort modules by: 1) Highest 2) Lowest 3) Level");
            String sortChoice = scanner.nextLine().trim();
            selectedModules = sortModules(selectedModules, sortChoice);

            // Collect all assessments from modules, filter by numeric score
            List<Assessment> filteredAssessments = new ArrayList<>();
            double topScore = 0;
            for (Module m : selectedModules) {
                for (Assessment a : m.getAssessments()) {
                    if (a.isCompleted() && a.getContributionToModuleScore() >= minScore) {
                        filteredAssessments.add(a);
                        if (a.getContributionToModuleScore() > topScore) topScore = a.getContributionToModuleScore();
                    }
                }
            }

            if (filteredAssessments.isEmpty()) {
                System.out.println("No matching assessments.");
                continue;
            }

            // Print table with top-module highlight
            paginateAssessments(filteredAssessments, topScore);

            // Export CSV
            exportAssessmentsCSV(student.getId(), filteredAssessments);
        }
    }

    // ==========================
    // Degree menu
    // ==========================
    private static void degreeMenu(Student student) throws SQLException {
        System.out.println("\n--- Degree Report ---");
        double degreeScore = student.calculateDegreeScore();
        String grade = student.getDegreeGrade();
        System.out.printf("Student %d: %.2f - %s%n", student.getId(), degreeScore, grade);

        // Export degree report CSV
        exportDegreeCSV(student.getId(), student);
    }

    // ==========================
    // Helper: print single/multi-assessment table
    // ==========================
    private static void printAssessmentTable(List<Assessment> assessments) {
        paginateAssessments(assessments, null); // Use pagination logic
    }

    // ==========================
    // Pagination and printing
    // ==========================
    private static void paginateAssessments(List<Assessment> assessments, Double topScore) {
        int totalPages = (assessments.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        for (int page = 0; page < totalPages; page++) {
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, assessments.size());
            List<Assessment> sub = assessments.subList(start, end);

            // Header
            System.out.printf("%-10s %-8s %-6s %-9s %-10s %-35s%n",
                    "StudentID","ModuleID","Level","AssessID","Score","Grade");
            System.out.println("================================================================================");

            for (Assessment a : sub) {
                String grade = a.isCompleted() ? GradeClassifier.classify(a.getContributionToModuleScore())
                        : ConsoleColors.RED + "Not Completed" + ConsoleColors.RESET;
                if (topScore != null && a.getContributionToModuleScore() == topScore)
                    grade += " <-- Top Module";
                System.out.printf("%-10d %-8d %-6d %-9d %-10.2f %-35s%n",
                        a.studentId, a.getModuleId(), getModuleLevel(a), a.getId(),
                        a.isCompleted() ? a.getContributionToModuleScore() : 0, grade);
            }

            if (page < totalPages - 1) {
                System.out.print("Press Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    // ==========================
    // Helper: get module level for an assessment
    // ==========================
    private static int getModuleLevel(Assessment a) {
        try {
            Module m = new Module(a.getModuleId(), a.studentId);
            return m.getLevel();
        } catch (SQLException e) { return 0; }
    }

    // ==========================
    // Sorting helper
    // ==========================
    private static List<Module> sortModules(List<Module> modules, String choice) {
        switch (choice) {
            case "1": // Highest
                modules.sort((a,b) -> Double.compare(b.calculateModuleScore(), a.calculateModuleScore()));
                break;
            case "2": // Lowest
                modules.sort(Comparator.comparingDouble(Module::calculateModuleScore));
                break;
            case "3": // By level
                modules.sort(Comparator.comparingInt(Module::getLevel));
                break;
        }
        return modules;
    }

    // ==========================
    // CSV export for assessments
    // ==========================
    private static void exportAssessmentsCSV(int studentId, List<Assessment> assessments) {
        String filename = "Student_" + studentId + "_assessments.csv";
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(filename)))) {
            pw.println("StudentID,ModuleID,Level,AssessmentID,Score,Grade");
            for (Assessment a : assessments) {
                String grade = a.isCompleted() ? GradeClassifier.classify(a.getContributionToModuleScore()) : "Not Completed";
                pw.printf("%d,%d,%d,%d,%.2f,%s%n",
                        a.studentId, a.getModuleId(), getModuleLevel(a), a.getId(),
                        a.isCompleted() ? a.getContributionToModuleScore() : 0, grade);
            }
            System.out.println("Exported to " + filename);
        } catch (IOException e) { System.out.println("Failed to export CSV."); }
    }

    // ==========================
    // CSV export for degree
    // ==========================
    private static void exportDegreeCSV(int studentId, Student student) {
        String filename = "Student_" + studentId + "_degree.csv";
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(Paths.get(filename)))) {
            double score = student.calculateDegreeScore();
            String grade = student.getDegreeGrade();
            pw.println("StudentID,DegreeScore,DegreeGrade");
            pw.printf("%d,%.2f,%s%n", studentId, score, grade);
            System.out.println("Exported degree report to " + filename);
        } catch (IOException e) { System.out.println("Failed to export CSV."); }
    }
}