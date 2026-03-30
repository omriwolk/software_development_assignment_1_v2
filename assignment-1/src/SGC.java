import java.util.*; // ArrayList, List, Comparator
import java.io.*;   // For CSV export

// Single class to handle all student, module, assessment data and reporting
public class SGC {

    private static int globalAssessmentID = 1; // Global counter for unique assessment IDs

    // === STUDENT CLASS ===
    public static class Student {
        private int id; // Student ID
        private List<Module> modules; // Modules enrolled

        public Student(int id) {
            this.id = id;
            this.modules = new ArrayList<>();
            loadDummyModules(); // For testing, populate modules
        }

        public int getId() { return id; }

        public List<Module> getModules() { return modules; }

        public Module getModuleById(int mid) {
            for (Module m : modules) if (m.getId() == mid) return m;
            return null;
        }

        public List<Assessment> getAllAssessments() {
            List<Assessment> all = new ArrayList<>();
            for (Module m : modules) all.addAll(m.getAssessments());
            return all;
        }

        public double calculateDegreeScore() {
            double total = 0;
            int count = 0;
            for (Module m : modules) {
                total += m.calculateModuleScore();
                count++;
            }
            return count == 0 ? 0 : total / count;
        }

        // Dummy modules for demonstration
        private void loadDummyModules() {
            // Create 4 modules per student with random grades
            int[] moduleIDs = {4005, 4006, 4007, 6007};
            int[] levels = {4, 4, 4, 6};
            for (int i = 0; i < moduleIDs.length; i++) {
                Module m = new Module(moduleIDs[i], levels[i]);
                // Each module has 2–4 assessments
                int numAssessments = 2 + (int)(Math.random() * 3);
                for (int j = 0; j < numAssessments; j++) {
                    double max = 50 + Math.random() * 100; // Max marks 50–150
                    double score = Math.random() * max;
                    Assessment a = new Assessment(globalAssessmentID++, "AssessmentType", m.getId(), score, max);
                    m.addAssessment(a);
                }
                modules.add(m);
            }
        }
    }

    // === MODULE CLASS ===
    public static class Module {
        private int id;
        private int level;
        private List<Assessment> assessments;

        public Module(int id, int level) {
            this.id = id;
            this.level = level;
            this.assessments = new ArrayList<>();
        }

        public int getId() { return id; }

        public int getLevel() { return level; }

        public List<Assessment> getAssessments() { return assessments; }

        public void addAssessment(Assessment a) { assessments.add(a); }

        public double calculateModuleScore() {
            double totalScore = 0;
            double totalMax = 0;
            for (Assessment a : assessments) {
                totalScore += a.getScore();
                totalMax += a.getMaxScore();
            }
            return totalMax == 0 ? 0 : (totalScore / totalMax) * 100; // Return % score
        }

        public int countMissingAssessments() {
            int missing = 0;
            for (Assessment a : assessments) if (!a.attempted()) missing++;
            return missing;
        }
    }

    // === ASSESSMENT CLASS ===
    public static class Assessment {
        private int id;
        private String type;
        private int moduleId;
        private double score;
        private double maxScore;

        public Assessment(int id, String type, int moduleId, double score, double maxScore) {
            this.id = id;
            this.type = type;
            this.moduleId = moduleId;
            this.score = score;
            this.maxScore = maxScore;
        }

        public int getId() { return id; }
        public int getModuleId() { return moduleId; }
        public double getScore() { return score; }
        public double getMaxScore() { return maxScore; }
        public double getContributionToModuleScore() { return score; }

        public boolean attempted() { return maxScore > 0 && score > 0; }
    }

    // === LOAD ALL STUDENTS ===
    public static List<Student> loadAllStudents() {
        List<Student> students = new ArrayList<>();
        // For demonstration, generate 2 students
        students.add(new Student(1000));
        students.add(new Student(1001));
        return students;
    }

    // === PRINT ASSESSMENTS ===
    public static void printAssessments(List<Assessment> assessments, Scanner scanner) {
        if (assessments.isEmpty()) {
            System.out.println("No assessments to display.");
            return;
        }
        System.out.println("\nAssessmentID | StudentID | ModuleID | Score | Status");
        for (Assessment a : assessments) {
            System.out.printf("%10d | %9d | %8d | %6.2f | %5.2f/%5.2f\n",
                    a.getId(), 1000, a.getModuleId(), a.getContributionToModuleScore(),
                    a.getScore(), a.getMaxScore());
        }
        System.out.print("Export this report to CSV? (y/n): ");
        String choice = scanner.nextLine().trim();
        if (choice.equalsIgnoreCase("y")) exportAssessmentsCSV(assessments);
    }

    private static void exportAssessmentsCSV(List<Assessment> assessments) {
        try (PrintWriter pw = new PrintWriter("assessments.csv")) {
            pw.println("AssessmentID,StudentID,ModuleID,Score,MaxScore");
            for (Assessment a : assessments)
                pw.printf("%d,%d,%d,%.2f,%.2f\n", a.getId(), 1000, a.getModuleId(),
                        a.getContributionToModuleScore(), a.getMaxScore());
            System.out.println("CSV exported to assessments.csv");
        } catch (Exception e) { System.out.println("Error exporting CSV: " + e.getMessage()); }
    }

    // === PRINT MODULES ===
    public static void printModules(List<Module> modules, List<Student> students, Scanner scanner) {
        if (modules.isEmpty()) {
            System.out.println("No modules to display.");
            return;
        }
        System.out.println("\nModuleID | StudentID | Level | NumericGrade | Missing assessments");
        for (Module m : modules) {
            for (Student s : students) {
                if (s.getModules().contains(m)) {
                    System.out.printf("%8d | %9d | %5d | %12.2f | %18d\n",
                            m.getId(), s.getId(), m.getLevel(),
                            m.calculateModuleScore(), m.countMissingAssessments());
                }
            }
        }
        System.out.print("Export this report to CSV? (y/n): ");
        String choice = scanner.nextLine().trim();
        if (choice.equalsIgnoreCase("y")) exportModulesCSV(modules, students);
    }

    private static void exportModulesCSV(List<Module> modules, List<Student> students) {
        try (PrintWriter pw = new PrintWriter("modules.csv")) {
            pw.println("ModuleID,StudentID,Level,NumericGrade,MissingAssessments");
            for (Module m : modules) {
                for (Student s : students) {
                    if (s.getModules().contains(m)) {
                        pw.printf("%d,%d,%d,%.2f,%d\n", m.getId(), s.getId(),
                                m.getLevel(), m.calculateModuleScore(), m.countMissingAssessments());
                    }
                }
            }
            System.out.println("CSV exported to modules.csv");
        } catch (Exception e) { System.out.println("Error exporting CSV: " + e.getMessage()); }
    }

    // === PRINT DEGREES ===
    public static void printDegrees(List<Student> students, Scanner scanner) {
        System.out.println("\nStudentID | DegreeScore | DegreeGrade");
        for (Student s : students) {
            double score = s.calculateDegreeScore();
            String grade;
            if (score >= 70) grade = "First";
            else if (score >= 60) grade = "Upper Second";
            else if (score >= 50) grade = "Lower Second";
            else if (score >= 40) grade = "Third";
            else grade = "Fail - You shall NOT PASS!";
            System.out.printf("%9d | %11.2f | %s\n", s.getId(), score, grade);
        }
        System.out.print("Export this report to CSV? (y/n): ");
        String choice = scanner.nextLine().trim();
        if (choice.equalsIgnoreCase("y")) exportDegreesCSV(students);
    }

    private static void exportDegreesCSV(List<Student> students) {
        try (PrintWriter pw = new PrintWriter("degrees.csv")) {
            pw.println("StudentID,DegreeScore,DegreeGrade");
            for (Student s : students) {
                double score = s.calculateDegreeScore();
                String grade;
                if (score >= 70) grade = "First";
                else if (score >= 60) grade = "Upper Second";
                else if (score >= 50) grade = "Lower Second";
                else if (score >= 40) grade = "Third";
                else grade = "Fail - You shall NOT PASS!";
                pw.printf("%d,%.2f,%s\n", s.getId(), score, grade);
            }
            System.out.println("CSV exported to degrees.csv");
        } catch (Exception e) { System.out.println("Error exporting CSV: " + e.getMessage()); }
    }
}