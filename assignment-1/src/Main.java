import java.sql.SQLException;   // Allows handling database errors
import java.util.*;             // Imports Scanner, List, Collections, etc.

public class Main {             // Entry point of the program

    // =========================
    // ANSI COLORS FOR OUTPUT
    // =========================
    private static final String RED = "\u001B[31m";
    private static final String ORANGE = "\u001B[38;5;208m";
    private static final String PURPLE = "\u001B[35m";
    private static final String BLUE = "\u001B[34m";
    private static final String GREEN = "\u001B[32m";
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        try {

            while (true) {

                List<Integer> ids = SGC.loadAllStudentIDs();

                if (ids.isEmpty()) {
                    System.out.println("No students found.");
                    break;
                }

                int min = Collections.min(ids);
                int max = Collections.max(ids);

                System.out.printf("\nEnter Student ID (0 to exit, range: %d-%d): ", min, max);

                String input = sc.nextLine().trim();

                if (input.equals("0")) break;

                int studentID;

                try {
                    studentID = Integer.parseInt(input);
                } catch (Exception e) {
                    System.out.println("Invalid input.");
                    continue;
                }

                if (!ids.contains(studentID)) {
                    System.out.println("Student ID not found.");
                    continue;
                }

                SGC.Student student = SGC.loadStudent(studentID);

                while (true) {

                    System.out.println("\n1) Assessment  2) Module  3) Degree");
                    System.out.print("Choice (0 to go back): ");

                    int choice;

                    try {
                        choice = Integer.parseInt(sc.nextLine());
                    } catch (Exception e) {
                        System.out.println("Invalid input.");
                        continue;
                    }

                    if (choice == 0) break;

                    // =========================
                    // OPTION 1: ASSESSMENT VIEW
                    // =========================
                    if (choice == 1) {

                        List<SGC.Assessment> all = new ArrayList<>();

                        for (SGC.Module m : student.getModules())
                            all.addAll(m.getAssessments());

                        if (all.isEmpty()) {
                            System.out.println("No assessments found.");
                            continue;
                        }

                        int minA = all.stream()
                                .mapToInt(SGC.Assessment::getAssessmentID)
                                .min().orElse(0);

                        int maxA = all.stream()
                                .mapToInt(SGC.Assessment::getAssessmentID)
                                .max().orElse(0);

                        System.out.printf("Enter Assessment ID (0 to go back, range: %d-%d): ", minA, maxA);

                        int id;

                        try {
                            id = Integer.parseInt(sc.nextLine());
                        } catch (Exception e) {
                            System.out.println("Invalid input.");
                            continue;
                        }

                        if (id == 0) continue;

                        SGC.Assessment selected = null;

                        for (SGC.Module m : student.getModules()) {
                            selected = m.getAssessmentByID(id);
                            if (selected != null) break;
                        }

                        if (selected == null) {
                            System.out.println("Assessment not found.");
                            continue;
                        }

                        String maxMarks = "-";

                        if (selected instanceof SGC.Exam e)
                            maxMarks = String.format("%.1f", e.getMaxMarks());

                        System.out.printf("\n%-10s %-12s %-12s %-14s %-12s %-8s\n",
                                "Student", "Assessment", "Type", "Awarded", "Max", "Weight");

                        System.out.printf("%-10d %-12d %-12s %-14.1f %-12s %-8.1f\n",
                                student.getStudentID(),
                                selected.getAssessmentID(),
                                selected.getType(),
                                selected.getAwardedMarks() != null
                                        ? selected.getAwardedMarks() : 0.0,
                                maxMarks,
                                selected.getWeight()
                        );
                    }

                    // =========================
                    // OPTION 2: MODULE VIEW (UPDATED ✅)
                    // =========================
                    else if (choice == 2) {

                        // Group modules by level (sorted automatically)
                        Map<Integer, List<SGC.Module>> byLevel = new TreeMap<>();

                        for (SGC.Module mod : student.getModules()) {
                            byLevel.computeIfAbsent(mod.getLevel(), k -> new ArrayList<>()).add(mod);
                        }

                        // Show ALL ranges FIRST (above prompt)
                        System.out.println("\nAvailable Modules:");

                        for (Map.Entry<Integer, List<SGC.Module>> entry : byLevel.entrySet()) {

                            int level = entry.getKey();
                            List<SGC.Module> mods = entry.getValue();

                            int minM = mods.stream()
                                    .mapToInt(SGC.Module::getModuleID)
                                    .min().orElse(0);

                            int maxM = mods.stream()
                                    .mapToInt(SGC.Module::getModuleID)
                                    .max().orElse(0);

                            System.out.printf("Level %d: %d - %d\n", level, minM, maxM);
                        }

                        // Clean prompt AFTER ranges
                        System.out.print("\nEnter Module ID (0 to go back): ");

                        int id;

                        try {
                            id = Integer.parseInt(sc.nextLine());
                        } catch (Exception e) {
                            System.out.println("Invalid input.");
                            continue;
                        }

                        if (id == 0) continue;

                        SGC.Module m = student.getModuleByID(id);

                        if (m == null) {
                            System.out.println("Module not found.");
                            continue;
                        }

                        System.out.printf("\n%-10s %-10s %-7s %-10s\n",
                                "Student", "Module", "Level", "Grade");

                        System.out.printf("%-10d %-10d %-7d %-10.2f\n",
                                student.getStudentID(),
                                m.getModuleID(),
                                m.getLevel(),
                                m.getModuleScore()
                        );
                    }

                    // =========================
                    // OPTION 3: DEGREE VIEW
                    // =========================
                    else if (choice == 3) {

                        double score = student.getDegreeScore();

                        SGC.DegreeClass dc = student.getDegreeClass();

                        String color = switch (dc) {
                            case FAIL -> RED;
                            case THIRD -> ORANGE;
                            case LOWER_SECOND -> PURPLE;
                            case UPPER_SECOND -> BLUE;
                            case FIRST -> GREEN;
                        };

                        System.out.printf("\n%-10s %-12s %-30s\n",
                                "Student", "Score", "Classification");

                        System.out.printf("%-10d %-12.2f %s%s%s\n",
                                student.getStudentID(),
                                score,
                                BOLD,
                                color + dc.getDescription() + RESET,
                                RESET
                        );
                    }

                    else {
                        System.out.println("Invalid choice.");
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }

        sc.close();
    }
}