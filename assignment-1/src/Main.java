import java.sql.SQLException;   // For handling database errors
import java.util.*;             // For Scanner, List, Set, HashSet

public class Main {

    // ANSI escape codes for colors
    private static final String RED = "\u001B[31m";
    private static final String ORANGE = "\u001B[38;5;208m";
    private static final String PURPLE = "\u001B[35m";
    private static final String BLUE = "\u001B[34m";
    private static final String GREEN = "\u001B[32m";
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in); // Scanner for user input

        try {
            while (true) { // Main loop for selecting students

                // Load all student IDs from DB
                List<Integer> allStudentIDs = SGC.loadAllStudentIDs();
                if (allStudentIDs.isEmpty()) {
                    System.out.println("No students found in the database.");
                    break;
                }

                // Show student range in prompt
                int minID = Collections.min(allStudentIDs);
                int maxID = Collections.max(allStudentIDs);
                System.out.printf("\nEnter Student ID (0 to exit, range: %d-%d): ", minID, maxID);

                String sInput = sc.nextLine().trim();
                if (sInput.equals("0")) break; // exit
                int studentID;
                try {
                    studentID = Integer.parseInt(sInput);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input.");
                    continue;
                }
                if (!allStudentIDs.contains(studentID)) {
                    System.out.println("Student ID not found.");
                    continue;
                }

                // Load selected student
                SGC.Student student = SGC.loadStudent(studentID);

                // Report menu loop
                boolean back = false;
                while (!back) {

                    // Show menu
                    System.out.println("\n1) Assessment  2) Module  3) Degree");
                    System.out.print("Choice (0 to go back): ");
                    String menuInput = sc.nextLine().trim();
                    int choice;
                    try {
                        choice = Integer.parseInt(menuInput);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input.");
                        continue;
                    }

                    if (choice == 0) break; // go back to student selection

                    // =========================
                    // 1) ASSESSMENT REPORT
                    // =========================
                    if (choice == 1) {

                        // Collect all assessments
                        List<SGC.Assessment> allAssessments = new ArrayList<>();
                        for (SGC.Module m : student.getModules()) allAssessments.addAll(m.getAssessments());
                        if (allAssessments.isEmpty()) {
                            System.out.println("No assessments found for this student.");
                            continue;
                        }

                        // Show assessment ID range
                        int minAid = allAssessments.stream().mapToInt(SGC.Assessment::getAssessmentID).min().orElse(0);
                        int maxAid = allAssessments.stream().mapToInt(SGC.Assessment::getAssessmentID).max().orElse(0);
                        System.out.printf("Enter Assessment ID (0 to go back, range: %d-%d): ", minAid, maxAid);
                        int aID;
                        try {
                            aID = Integer.parseInt(sc.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input.");
                            continue;
                        }
                        if (aID == 0) continue; // back

                        // Find selected assessment
                        SGC.Assessment selected = null;
                        for (SGC.Module m : student.getModules()) {
                            selected = m.getAssessmentByID(aID);
                            if (selected != null) break;
                        }
                        if (selected == null) {
                            System.out.println("Assessment not found.");
                            continue;
                        }

                        // Print header
                        System.out.printf("\n%-10s %-12s %-12s %-14s %-12s %-8s\n",
                                "Student", "Assessment", "Type", "Awarded Mark", "Max Mark", "Weight");

                        // Get max marks if exam
                        String maxMarksStr = "-";
                        if (selected instanceof SGC.Exam) {
                            maxMarksStr = String.format("%.1f", ((SGC.Exam) selected).getMaxMarks());
                        }

                        // Print assessment row
                        System.out.printf("%-10d %-12d %-12s %-14.1f %-12s %-8.1f\n",
                                student.getStudentID(),
                                selected.getAssessmentID(),
                                selected.getType(),
                                selected.getAwardedMarks() != null ? selected.getAwardedMarks() : 0.0,
                                maxMarksStr,
                                selected.getWeight()
                        );
                    }

                    // =========================
                    // 2) MODULE REPORT
                    // =========================
                    else if (choice == 2) {

                        // Show module ranges by level
                        Map<Integer, List<Integer>> levelMap = new TreeMap<>();
                        for (SGC.Module m : student.getModules()) {
                            levelMap.computeIfAbsent(m.getLevel(), k -> new ArrayList<>()).add(m.getModuleID());
                        }
                        for (var e : levelMap.entrySet()) {
                            int minMod = Collections.min(e.getValue());
                            int maxMod = Collections.max(e.getValue());
                            System.out.printf("Level %d: %d-%d\n", e.getKey(), minMod, maxMod);
                        }

                        // Prompt for module ID
                        System.out.print("Enter Module ID (0 to go back): ");
                        int modID;
                        try {
                            modID = Integer.parseInt(sc.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input.");
                            continue;
                        }
                        if (modID == 0) continue; // back

                        // Find module
                        SGC.Module module = student.getModuleByID(modID);
                        if (module == null) {
                            System.out.println("Module not found.");
                            continue;
                        }

                        // Print module report table header
                        System.out.printf("\n%-10s %-10s %-7s %-10s\n",
                                "Student", "Module", "Level", "Grade");
                        System.out.printf("%-10d %-10d %-7d %-10.2f\n",
                                student.getStudentID(),
                                module.getModuleID(),
                                module.getLevel(),
                                module.getModuleScore()
                        );
                    }

                    // =========================
                    // 3) DEGREE REPORT
                    // =========================
                    else if (choice == 3) {

                        // Compute score and classification
                        double score = student.getDegreeScore();
                        String classification = student.getDegreeClass();
                        String color = RESET;

                        // Determine color
                        if (score < 40) color = RED;
                        else if (score < 50) color = ORANGE;
                        else if (score < 60) color = PURPLE;
                        else if (score < 70) color = BLUE;
                        else color = GREEN;

                        // Print degree report header
                        System.out.printf("\n%-10s %-12s %-30s\n", "Student", "Score", "Classification");
                        // Print degree row
                        System.out.printf("%-10d %-12.2f %s%s%s\n",
                                student.getStudentID(),
                                score,
                                BOLD,
                                color + classification + RESET,
                                RESET
                        );
                    }

                    // Invalid option
                    else {
                        System.out.println("Invalid choice.");
                    }
                }
            }
        } catch (SQLException e) {
            // Handle DB errors
            System.out.println("Database error: " + e.getMessage());
        }

        sc.close(); // Close scanner
    }
}