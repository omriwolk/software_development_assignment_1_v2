import java.util.*;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("=== STUDENT SELECTION ===");
            System.out.println("1) Select student by ID");
            System.out.println("0) Exit");
            System.out.print("Choice: ");
            int choice = scanner.nextInt();
            if (choice == 0) break;
            if (choice != 1) continue;

            System.out.print("Enter Student ID: ");
            int studentId = scanner.nextInt();

            try {
                SGC.Student student = new SGC.Student(studentId);
                studentMenu(student, scanner);
            } catch (SQLException e) {
                System.out.println("Error loading student: " + e.getMessage());
            }
        }

        System.out.println("Exiting program.");
    }

    private static void studentMenu(SGC.Student student, Scanner scanner) {
        while (true) {
            System.out.println("\n=== STUDENT MENU ===");
            System.out.println("1) Assessment report");
            System.out.println("2) Module report");
            System.out.println("3) Degree report");
            System.out.println("4) Back");
            System.out.print("Choice: ");
            int choice = scanner.nextInt();

            switch (choice) {
                case 1 -> assessmentReport(student, scanner);
                case 2 -> moduleReport(student, scanner);
                case 3 -> degreeReport(student);
                case 4 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void assessmentReport(SGC.Student student, Scanner scanner) {
        System.out.print("Enter Assessment ID (0 for all): ");
        int aid = scanner.nextInt();

        System.out.printf("%-12s %-15s %-10s %-12s %-12s %-8s%n",
                "AssessmentID", "Type", "ModuleID", "AwardedMarks", "MaxMarks", "Weight%");
        System.out.println("----------------------------------------------------------------");

        for (SGC.Module m : student.getModules()) {
            for (SGC.Assessment a : m.getAssessments()) {
                if (aid != 0 && a.getId() != aid) continue;
                System.out.printf("%-12d %-15s %-10d %-12s %-12s %-8.1f%n",
                        a.getId(),
                        a.getType(),
                        a.getModuleId(),
                        a.getAwardedMarks() != null ? a.getAwardedMarks() : "N/A",
                        a.getMaxMarks() != null ? a.getMaxMarks() : "N/A",
                        a.getWeighting()
                );
            }
        }
    }

    private static void moduleReport(SGC.Student student, Scanner scanner) {
        System.out.print("Enter Module ID (0 for all): ");
        int mid = scanner.nextInt();

        System.out.printf("%-10s %-6s %-12s %-10s %-10s%n",
                "ModuleID", "Level", "ModuleScore", "Completed", "TotalAssess");
        System.out.println("-----------------------------------------------------");

        for (SGC.Module m : student.getModules()) {
            if (mid != 0 && m.getId() != mid) continue;

            long completed = m.getAssessments().stream().filter(a -> !a.isIncomplete()).count();

            System.out.printf("%-10d %-6d %-12.2f %-10d %-10d%n",
                    m.getId(),
                    m.getLevel(),
                    m.getModuleScore(),
                    completed,
                    m.getAssessments().size()
            );
        }
    }

    private static void degreeReport(SGC.Student student) {
        double score = student.getDegreeScore();
        String classification = student.getDegreeClassification();

        System.out.println("+----------------+----------------------+");
        System.out.printf("| %-14s | %-20.2f |\n", "Degree Score", score);
        System.out.printf("| %-14s | %-20s |\n", "Classification", classification);
        System.out.println("+----------------+----------------------+");
    }
}