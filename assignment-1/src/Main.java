import java.sql.SQLException;     // For database exceptions
import java.util.*;               // For List, ArrayList, Scanner, Set, Comparator
import java.io.*;                 // For CSV export

public class Main {

    // --- ANSI escape codes for colors and bold ---
    public static final String RESET = "\033[0m";
    public static final String BOLD = "\033[1m";
    public static final String RED = "\033[31m";
    public static final String ORANGE = "\033[38;5;208m";  // orange
    public static final String PURPLE = "\033[35m";
    public static final String BLUE = "\033[34m";
    public static final String GREEN = "\033[32m";

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in); // Scanner for user input

        try {

            // --- Infinite loop for student selection ---
            while (true) {

                System.out.println("\n=== STUDENT SELECTION ===");

                // --- Load all student IDs from DB ---
                List<Integer> studentIDs = SGC.loadAllStudentIDs();
                if (studentIDs.isEmpty()) {
                    System.out.println("No students found in database!");
                    return;
                }

                // --- Show student ID range ---
                int minID = Collections.min(studentIDs);
                int maxID = Collections.max(studentIDs);
                System.out.printf("Choose Student ID between %d and %d, 0 for ALL, -1 to exit%n", minID, maxID);

                // --- Read input ---
                String input = sc.nextLine().trim();
                if (input.equals("-1")) break; // exit program

                List<SGC.Student> selectedStudents = new ArrayList<>();

                if (input.equals("0")) {
                    // Load all students
                    for (int id : studentIDs) selectedStudents.add(SGC.loadStudent(id));
                } else {
                    // Validate input
                    int sid;
                    try { sid = Integer.parseInt(input); }
                    catch (NumberFormatException e) { System.out.println("Invalid ID"); continue; }

                    if (!studentIDs.contains(sid)) { System.out.println("Invalid ID"); continue; }

                    selectedStudents.add(SGC.loadStudent(sid));
                }

                // --- Report menu loop ---
                boolean back = false;
                while (!back) {
                    System.out.println("\n=== REPORT MENU ===");
                    System.out.println("1) Assessment report");
                    System.out.println("2) Module report");
                    System.out.println("3) Degree report");
                    System.out.println("4) Back");
                    System.out.print("Choice: ");

                    int choice;
                    try { choice = Integer.parseInt(sc.nextLine()); }
                    catch (NumberFormatException e) { System.out.println("Invalid choice"); continue; }

                    switch (choice) {

                        // =========================
                        // 1) Assessment report
                        // =========================
                        case 1 -> {

                            // --- Gather all assessments ---
                            List<SGC.Assessment> allAssessments = new ArrayList<>();
                            for (SGC.Student s : selectedStudents) {
                                for (SGC.Module m : s.getModules()) {
                                    allAssessments.addAll(m.getAssessments());
                                }
                            }
                            if (allAssessments.isEmpty()) {
                                System.out.println("No assessments found.");
                                break;
                            }

                            // --- Determine assessment ID range ---
                            int minAid = allAssessments.stream().mapToInt(SGC.Assessment::getAssessmentID).min().orElse(0);
                            int maxAid = allAssessments.stream().mapToInt(SGC.Assessment::getAssessmentID).max().orElse(0);
                            System.out.printf("Enter Assessment ID between %d and %d, 0 for ALL, -1 for back: ", minAid, maxAid);

                            int aid;
                            try { aid = Integer.parseInt(sc.nextLine()); }
                            catch (NumberFormatException e) { System.out.println("Invalid ID"); break; }
                            if (aid == -1) break;

                            // --- Filter selected assessments ---
                            List<String[]> rows = new ArrayList<>();
                            for (SGC.Student s : selectedStudents) {
                                for (SGC.Module m : s.getModules()) {
                                    for (SGC.Assessment a : m.getAssessments()) {
                                        if (aid == 0 || a.getAssessmentID() == aid) {
                                            rows.add(new String[]{
                                                    String.valueOf(a.getAssessmentID()),
                                                    String.valueOf(s.getStudentID()),
                                                    String.valueOf(a.getModuleID()),
                                                    a.getType(),
                                                    String.format("%.1f", a.getAwardedMarks() != null ? a.getAwardedMarks() : 0.0),
                                                    (a.getMaxMarks() != null ? String.format("%.1f", a.getMaxMarks()) : "-"),
                                                    String.format("%.1f", a.getWeight())
                                            });
                                        }
                                    }
                                }
                            }
                            if (rows.isEmpty()) { System.out.println("No data found."); break; }

                            // --- Determine logical sorting options dynamically ---
                            List<String> sortOptions = new ArrayList<>();
                            if (selectedStudents.size() > 1) sortOptions.add("StudentID");
                            Set<Integer> moduleSet = new HashSet<>();
                            for (String[] r : rows) moduleSet.add(Integer.parseInt(r[2]));
                            if (moduleSet.size() > 1) sortOptions.add("ModuleID");
                            if (rows.size() > 1) sortOptions.addAll(Arrays.asList("AssessmentID", "Grade"));

                            // --- Sorting prompt ---
                            if (!sortOptions.isEmpty()) {
                                System.out.println("Sort by options:");
                                for (int i = 0; i < sortOptions.size(); i++)
                                    System.out.printf("%d) %s%n", i+1, sortOptions.get(i));
                                System.out.print("Choose sorting option: ");
                                String sortInput = sc.nextLine().trim();
                                int sIdx;
                                try { sIdx = Integer.parseInt(sortInput) - 1; } catch (Exception e) { sIdx = -1; }
                                if (sIdx >= 0 && sIdx < sortOptions.size()) {
                                    System.out.print("Ascending (A) or Descending (D)? ");
                                    String ascDesc = sc.nextLine().trim().toUpperCase();
                                    boolean ascending = !ascDesc.equals("D");

                                    String option = sortOptions.get(sIdx);
                                    switch (option) {
                                        case "StudentID" -> rows.sort(Comparator.comparing(r -> Integer.parseInt(r[1])));
                                        case "ModuleID" -> rows.sort(Comparator.comparing(r -> Integer.parseInt(r[2])));
                                        case "AssessmentID" -> rows.sort(Comparator.comparing(r -> Integer.parseInt(r[0])));
                                        case "Grade" -> rows.sort(Comparator.comparing(r -> Double.parseDouble(r[4])));
                                    }
                                    if (!ascending) Collections.reverse(rows);
                                }
                            }

                            // --- Print table ---
                            System.out.println("\nID | Student | Module | Type | Awarded/Max | Weight");
                            for (String[] r : rows) {
                                System.out.printf("%s | %s | %s | %s | %s/%s | %s%%\n",
                                        r[0], r[1], r[2], r[3], r[4], r[5], r[6]);
                            }

                            // --- CSV export prompt ---
                            System.out.print("Export to CSV? (Y/N): ");
                            String exp = sc.nextLine().trim().toUpperCase();
                            if (exp.equals("Y")) {
                                try (PrintWriter pw = new PrintWriter(new File("assessment_report.csv"))) {
                                    pw.println("ID,Student,Module,Type,Awarded,Max,Weight");
                                    for (String[] r : rows) pw.println(String.join(",", r));
                                    System.out.println("CSV exported as assessment_report.csv");
                                } catch (Exception e) { System.out.println("Error exporting CSV: " + e.getMessage()); }
                            }
                        }

                        // =========================
                        // 2) Module report
                        // =========================
                        case 2 -> {

                            // --- Gather all module IDs ---
                            Set<Integer> moduleIDsSet = new HashSet<>();
                            for (SGC.Student s : selectedStudents) {
                                for (SGC.Module m : s.getModules()) {
                                    moduleIDsSet.add(m.getModuleID());
                                }
                            }
                            if (moduleIDsSet.isEmpty()) { System.out.println("No modules found."); break; }

                            // --- Show module ID range ---
                            int minMid = Collections.min(moduleIDsSet);
                            int maxMid = Collections.max(moduleIDsSet);
                            System.out.printf("Enter Module ID between %d and %d, 0 for ALL, -1 for back: ", minMid, maxMid);

                            int mid;
                            try { mid = Integer.parseInt(sc.nextLine()); }
                            catch (NumberFormatException e) { System.out.println("Invalid input"); break; }
                            if (mid == -1) break;

                            // --- Filter modules by selected ID ---
                            List<String[]> rows = new ArrayList<>();
                            for (SGC.Student s : selectedStudents) {
                                for (SGC.Module m : s.getModules()) {
                                    if (mid == 0 || m.getModuleID() == mid) {
                                        rows.add(new String[]{
                                                String.valueOf(m.getModuleID()),
                                                String.valueOf(s.getStudentID()),
                                                String.valueOf(m.getLevel()),
                                                String.format("%.2f", m.getModuleScore())
                                        });
                                    }
                                }
                            }
                            if (rows.isEmpty()) { System.out.println("No data found."); break; }

                            // --- Determine logical sorting options dynamically ---
                            Set<Integer> uniqueModules = new HashSet<>();
                            Set<Integer> uniqueStudents = new HashSet<>();
                            for (String[] r : rows) { uniqueModules.add(Integer.parseInt(r[0])); uniqueStudents.add(Integer.parseInt(r[1])); }
                            List<String> sortOptions = new ArrayList<>();
                            if (uniqueModules.size() > 1) sortOptions.add("ModuleID");
                            if (uniqueStudents.size() > 1) sortOptions.add("StudentID");
                            if (rows.size() > 1) sortOptions.add("ModuleScore");

                            // --- Sorting prompt ---
                            if (!sortOptions.isEmpty()) {
                                System.out.println("Sort by options:");
                                for (int i = 0; i < sortOptions.size(); i++)
                                    System.out.printf("%d) %s%n", i+1, sortOptions.get(i));
                                System.out.print("Choose sorting option: ");
                                String sortInput = sc.nextLine().trim();
                                int sIdx;
                                try { sIdx = Integer.parseInt(sortInput) - 1; } catch (Exception e) { sIdx = -1; }
                                if (sIdx >= 0 && sIdx < sortOptions.size()) {
                                    System.out.print("Ascending (A) or Descending (D)? ");
                                    String ascDesc = sc.nextLine().trim().toUpperCase();
                                    boolean ascending = !ascDesc.equals("D");

                                    String option = sortOptions.get(sIdx);
                                    switch (option) {
                                        case "ModuleID" -> rows.sort(Comparator.comparing(r -> Integer.parseInt(r[0])));
                                        case "StudentID" -> rows.sort(Comparator.comparing(r -> Integer.parseInt(r[1])));
                                        case "ModuleScore" -> rows.sort(Comparator.comparing(r -> Double.parseDouble(r[3])));
                                    }
                                    if (!ascending) Collections.reverse(rows);
                                }
                            }

                            // --- Print table ---
                            System.out.println("\nModule | Student | Level | Grade");
                            for (String[] r : rows) {
                                System.out.printf("%s | %s | %s | %s\n", r[0], r[1], r[2], r[3]);
                            }

                            // --- CSV export ---
                            System.out.print("Export to CSV? (Y/N): ");
                            String exp = sc.nextLine().trim().toUpperCase();
                            if (exp.equals("Y")) {
                                try (PrintWriter pw = new PrintWriter(new File("module_report.csv"))) {
                                    pw.println("Module,Student,Level,Grade");
                                    for (String[] r : rows) pw.println(String.join(",", r));
                                    System.out.println("CSV exported as module_report.csv");
                                } catch (Exception e) { System.out.println("Error exporting CSV: " + e.getMessage()); }
                            }
                        }

                        // =========================
                        // 3) Degree report
                        // =========================
                        case 3 -> {
                            List<String[]> rows = new ArrayList<>();
                            for (SGC.Student s : selectedStudents) {
                                double score = s.getDegreeScore();
                                String classification = s.getDegreeClass();
                                rows.add(new String[]{
                                        String.valueOf(s.getStudentID()),
                                        String.format("%.2f", score),
                                        classification
                                });
                            }
                            if (rows.isEmpty()) { System.out.println("No students found."); break; }

                            // --- Sorting prompt if multiple students ---
                            if (rows.size() > 1) {
                                System.out.println("Sort by: 1=StudentID, 2=DegreeScore");
                                String sortChoice = sc.nextLine().trim();
                                System.out.print("Ascending (A) or Descending (D)? ");
                                String ascDesc = sc.nextLine().trim().toUpperCase();
                                boolean ascending = !ascDesc.equals("D");

                                switch (sortChoice) {
                                    case "1" -> rows.sort(Comparator.comparing(r -> Integer.parseInt(r[0])));
                                    case "2" -> rows.sort(Comparator.comparing(r -> Double.parseDouble(r[1])));
                                }
                                if (!ascending) Collections.reverse(rows);
                            }

                            // --- Print table with bold + colors ---
                            System.out.println("\nStudent | Degree | Classification");
                            for (String[] r : rows) {
                                String cls = r[2];
                                String color = RESET;

                                if (cls.startsWith("Fail")) color = RED;
                                else if (cls.startsWith("Third")) color = ORANGE;
                                else if (cls.startsWith("Second class 2nd")) color = PURPLE;
                                else if (cls.startsWith("Second class 1st")) color = BLUE;
                                else if (cls.startsWith("First")) color = GREEN;

                                System.out.printf("%s | %s | %s%s%s%s\n",
                                        r[0], r[1], BOLD, color, cls, RESET);
                            }

                            // --- CSV export ---
                            System.out.print("Export to CSV? (Y/N): ");
                            String exp = sc.nextLine().trim().toUpperCase();
                            if (exp.equals("Y")) {
                                try (PrintWriter pw = new PrintWriter(new File("degree_report.csv"))) {
                                    pw.println("Student,Degree,Classification");
                                    for (String[] r : rows) pw.println(String.join(",", r));
                                    System.out.println("CSV exported as degree_report.csv");
                                } catch (Exception e) { System.out.println("Error exporting CSV: " + e.getMessage()); }
                            }
                        }

                        // =========================
                        // 4) Back
                        // =========================
                        case 4 -> back = true;

                        default -> System.out.println("Invalid choice");
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
        }

        sc.close(); // Close scanner at the end
    }
}