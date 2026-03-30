import java.sql.*;
import java.util.*;

// SGC: Staffordshire Grading Computation
public class SGC {

    // --- Assessment class ---
    public static class Assessment {
        private int assessmentID;       // Unique assessment ID
        private String type;            // Assessment type (Exam, Report, etc.)
        private int moduleID;           // Module it belongs to
        private Double maxMarks;        // Maximum marks (nullable)
        private Double awardedMarks;    // Awarded marks (nullable)
        private double weight;          // Weighting from AssessmentStructure table

        // Constructor
        public Assessment(int assessmentID, String type, int moduleID,
                          Double maxMarks, Double awardedMarks, double weight) {
            this.assessmentID = assessmentID;
            this.type = type;
            this.moduleID = moduleID;
            this.maxMarks = maxMarks;
            this.awardedMarks = awardedMarks;
            this.weight = weight;
        }

        // --- Getters ---
        public int getAssessmentID() { return assessmentID; }
        public String getType() { return type; }
        public int getModuleID() { return moduleID; }
        public Double getMaxMarks() { return maxMarks; }
        public Double getAwardedMarks() { return awardedMarks; }
        public double getWeight() { return weight; }

        // Calculate contribution of this assessment to module
        public double getContribution() {
            if (awardedMarks == null) return 0.0; // Missing marks = 0
            double grade;
            if ("Exam".equalsIgnoreCase(type)) {
                grade = awardedMarks / maxMarks * 100; // Exam: percentage
            } else {
                grade = awardedMarks; // Other assessments: direct mark
            }
            return grade * (weight / 100.0); // Weighted contribution
        }

        // Get grade for display
        public double getGrade() {
            if (awardedMarks == null) return 0.0;
            if ("Exam".equalsIgnoreCase(type)) return awardedMarks / maxMarks * 100;
            else return awardedMarks;
        }
    }

    // --- Module class ---
    public static class Module {
        private int moduleID;                // Module ID
        private int level;                   // Level (4,5,6)
        private List<Assessment> assessments; // List of assessments

        // Constructor
        public Module(int moduleID, int level) {
            this.moduleID = moduleID;
            this.level = level;
            this.assessments = new ArrayList<>();
        }

        // --- Getters ---
        public int getModuleID() { return moduleID; }
        public int getLevel() { return level; }
        public List<Assessment> getAssessments() { return assessments; }

        // Compute total module score (weighted)
        public double getModuleScore() {
            for (Assessment a : assessments) {
                if (a.getAwardedMarks() == null) return 0.0; // missing assessment -> 0
            }
            double total = 0.0;
            for (Assessment a : assessments) total += a.getContribution();
            return total;
        }

        // Check if all assessments completed
        public boolean hasAllAssessmentsCompleted() {
            for (Assessment a : assessments) if (a.getAwardedMarks() == null) return false;
            return true;
        }

        // Check if module is failed for Level 5/6
        public boolean isFailedLevel5or6() {
            return (level == 5 || level == 6) && getModuleScore() < 40.0;
        }
    }

    // --- Student class ---
    public static class Student {
        private int studentID;          // Student ID
        private List<Module> modules;   // List of modules

        // Constructor
        public Student(int studentID) {
            this.studentID = studentID;
            this.modules = new ArrayList<>();
        }

        // --- Getters ---
        public int getStudentID() { return studentID; }
        public List<Module> getModules() { return modules; }

        // Retrieve module by ID
        public Module getModuleByID(int moduleID) {
            for (Module m : modules) if (m.getModuleID() == moduleID) return m;
            return null;
        }

        // Compute overall degree score
        public double getDegreeScore() {

            // --- Step 0: Immediate fail if any Level 5/6 module failed ---
            for (Module m : modules) if (m.isFailedLevel5or6()) return 0.0;

            List<Module> mods = new ArrayList<>(modules);

            // --- Step 1: remove lowest module score ---
            mods.sort(Comparator.comparingDouble(Module::getModuleScore));
            if (!mods.isEmpty()) mods.remove(0);

            // --- Step 2: split by level ---
            double level5Sum = 0.0; int level5Count = 0;
            double level6Sum = 0.0; int level6Count = 0;

            for (Module m : mods) {
                if (m.getLevel() == 5) { level5Sum += m.getModuleScore(); level5Count++; }
                else if (m.getLevel() == 6) { level6Sum += m.getModuleScore(); level6Count++; }
            }

            // --- Step 3: averages ---
            double level5Avg = (level5Count == 0) ? 0 : level5Sum / level5Count;
            double level6Avg = (level6Count == 0) ? 0 : level6Sum / level6Count;

            // --- Step 4: weighted final score ---
            return (level5Avg * 0.3) + (level6Avg * 0.7);
        }

        // Determine degree classification
        public String getDegreeClass() {
            double score = getDegreeScore();
            if (score == 0.0) return "Fail - You shall NOT PASS!"; // Level5/6 fail
            if (score < 40) return "Fail - You shall NOT PASS!";
            if (score < 50) return "Third class";
            if (score < 60) return "Second class 2nd division";
            if (score < 70) return "Second class 1st division";
            return "First class - You bow to no one!";
        }
    }

    // --- Database path ---
    private static final String DB_PATH = "/home/omri/software_development/assignment-1/university.db";

    // Load all student IDs
    public static List<Integer> loadAllStudentIDs() throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT StudentID FROM StudentEnrolment ORDER BY StudentID");
            while (rs.next()) ids.add(rs.getInt("StudentID"));
        }
        return ids;
    }

    // Load a student with all modules and assessments
    public static Student loadStudent(int studentID) throws SQLException {
        Student student = new Student(studentID);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {

            // --- Load enrolled modules ---
            PreparedStatement psModules = conn.prepareStatement(
                    "SELECT DISTINCT ModuleID FROM StudentEnrolment WHERE StudentID = ?");
            psModules.setInt(1, studentID);
            ResultSet rsModules = psModules.executeQuery();

            while (rsModules.next()) {
                int moduleID = rsModules.getInt("ModuleID");

                // --- Load module level ---
                PreparedStatement psLevel = conn.prepareStatement(
                        "SELECT Level FROM Module WHERE ModuleID = ?");
                psLevel.setInt(1, moduleID);
                ResultSet rsLevel = psLevel.executeQuery();
                int level = rsLevel.next() ? rsLevel.getInt("Level") : 0;
                Module module = new Module(moduleID, level);

                // --- Load assessments ---
                PreparedStatement psAssess = conn.prepareStatement(
                        "SELECT AssessmentID, AssessmentType, MaximumMarks, AwardedMarks " +
                                "FROM Assessment WHERE StudentID = ? AND ModuleID = ?");
                psAssess.setInt(1, studentID);
                psAssess.setInt(2, moduleID);
                ResultSet rsAssess = psAssess.executeQuery();

                while (rsAssess.next()) {
                    int assessmentID = rsAssess.getInt("AssessmentID");
                    String type = rsAssess.getString("AssessmentType");
                    Double maxMarks = rsAssess.getObject("MaximumMarks") != null
                            ? rsAssess.getDouble("MaximumMarks") : null;
                    Double awardedMarks = rsAssess.getObject("AwardedMarks") != null
                            ? rsAssess.getDouble("AwardedMarks") : null;

                    // --- Load weighting ---
                    PreparedStatement psWeight = conn.prepareStatement(
                            "SELECT Weighting FROM AssessmentStructure WHERE AssessmentID = ?");
                    psWeight.setInt(1, assessmentID);
                    ResultSet rsWeight = psWeight.executeQuery();
                    double weight = rsWeight.next() ? rsWeight.getDouble("Weighting") : 0.0;

                    // --- Create assessment object ---
                    Assessment assessment = new Assessment(assessmentID, type, moduleID, maxMarks, awardedMarks, weight);
                    module.getAssessments().add(assessment);

                    rsWeight.close();
                }

                student.getModules().add(module);
            }
        }

        return student;
    }
}