import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

// Main program
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try {
            // Load all student IDs
            List<Integer> studentIDs = SGC.loadAllStudentIDs();

            while (true) {
                System.out.println("\n=== STUDENT SELECTION ===");
                System.out.println("Available student IDs:");
                for (int id : studentIDs) System.out.println(id);
                System.out.println("1) Select student by ID");
                System.out.println("0) Exit");
                System.out.print("Choice: ");
                int choice = Integer.parseInt(sc.nextLine());

                if (choice == 0) break;
                if (choice != 1) continue;

                System.out.print("Enter Student ID: ");
                int studentID = Integer.parseInt(sc.nextLine());
                SGC.Student student;
                try {
                    student = SGC.loadStudent(studentID);
                } catch (SQLException e) {
                    System.out.println("Error loading student: " + e.getMessage());
                    continue;
                }

                // Student menu
                boolean studentMenu = true;
                while (studentMenu) {
                    System.out.println("\n--- STUDENT " + student.getStudentID() + " MENU ---");
                    System.out.println("1) Assessment report");
                    System.out.println("2) Module report");
                    System.out.println("3) Degree report");
                    System.out.println("4) Back");
                    System.out.print("Choice: ");
                    int sChoice = Integer.parseInt(sc.nextLine());

                    switch (sChoice) {
                        case 1 -> { // Assessment report
                            System.out.println("Available assessment IDs:");
                            for (SGC.Module m : student.getModules()) {
                                for (SGC.Assessment a : m.getAssessments()) {
                                    System.out.println(a.getAssessmentID());
                                }
                            }
                            System.out.print("Enter Assessment ID or 0 for all: ");
                            int aid = Integer.parseInt(sc.nextLine());
                            System.out.println("ID | Type | Module | Awarded / Max | Weight");
                            for (SGC.Module m : student.getModules()) {
                                for (SGC.Assessment a : m.getAssessments()) {
                                    if (aid == 0 || aid == a.getAssessmentID()) {
                                        System.out.printf("%d | %s | %d | %.1f / %s | %.1f%%\n",
                                                a.getAssessmentID(), a.getType(), a.getModuleID(),
                                                a.getAwardedMarks() != null ? a.getAwardedMarks() : 0.0,
                                                a.getMaxMarks() != null ? a.getMaxMarks() : "-",
                                                a.getWeight());
                                    }
                                }
                            }
                        }
                        case 2 -> { // Module report
                            System.out.println("Available module IDs:");
                            for (SGC.Module m : student.getModules()) System.out.println(m.getModuleID());
                            System.out.print("Enter Module ID or 0 for all: ");
                            int mid = Integer.parseInt(sc.nextLine());
                            System.out.println("ID | Level | Grade");
                            for (SGC.Module m : student.getModules()) {
                                if (mid == 0 || mid == m.getModuleID()) {
                                    System.out.printf("%d | %d | %.1f\n",
                                            m.getModuleID(), m.getLevel(), m.getModuleScore());
                                }
                            }
                        }
                        case 3 -> { // Degree report
                            System.out.printf("Degree grade: %.1f\n", student.getDegreeScore());
                            System.out.println("Classification: " + student.getDegreeClass());
                        }
                        case 4 -> studentMenu = false;
                        default -> System.out.println("Invalid choice");
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}