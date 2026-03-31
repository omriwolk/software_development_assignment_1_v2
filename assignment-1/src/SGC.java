import java.sql.*;       // For SQLite DB access
import java.util.*;      // For Lists, Comparators, etc.

// =========================
// SGC SYSTEM
// =========================
public class SGC {

    // =========================
    // Abstract base class: Assessment
    // =========================
    public static abstract class Assessment {
        protected int assessmentID;      // Unique ID for assessment
        protected String type;           // Type: Exam, Report, Presentation
        protected int moduleID;          // Module this assessment belongs to
        protected Double awardedMarks;   // Marks awarded to student (nullable)
        protected double weight;         // Weight of assessment towards module

        // Constructor
        public Assessment(int assessmentID, String type, int moduleID, Double awardedMarks, double weight) {
            this.assessmentID = assessmentID;
            this.type = type;
            this.moduleID = moduleID;
            this.awardedMarks = awardedMarks;
            this.weight = weight;
        }

        // =========================
        // Abstract methods for polymorphism
        // =========================
        public abstract double getGrade();        // Return raw grade
        public abstract double getContribution(); // Weighted contribution to module

        // =========================
        // Getters
        // =========================
        public int getAssessmentID() { return assessmentID; }
        public String getType() { return type; }
        public int getModuleID() { return moduleID; }
        public Double getAwardedMarks() { return awardedMarks; }
        public double getWeight() { return weight; }
    }

    // =========================
    // Exam class (inherits Assessment)
    // =========================
    public static class Exam extends Assessment {
        private Double maxMarks; // Only exams have maximum marks

        // Constructor
        public Exam(int assessmentID, int moduleID, Double maxMarks, Double awardedMarks, double weight) {
            super(assessmentID, "Exam", moduleID, awardedMarks, weight);
            this.maxMarks = maxMarks;
        }

        // Getter for maxMarks
        public Double getMaxMarks() { return maxMarks; }

        // Compute grade as percentage
        @Override
        public double getGrade() {
            if (awardedMarks == null || maxMarks == null) return 0.0;
            return (awardedMarks / maxMarks) * 100.0;
        }

        // Compute weighted contribution
        @Override
        public double getContribution() {
            return getGrade() * (weight / 100.0);
        }
    }

    // =========================
    // Coursework class (Report / Presentation)
    // =========================
    public static class Coursework extends Assessment {

        // Constructor
        public Coursework(int assessmentID, String type, int moduleID, Double awardedMarks, double weight) {
            super(assessmentID, type, moduleID, awardedMarks, weight);
        }

        // Grade is simply awarded marks
        @Override
        public double getGrade() {
            if (awardedMarks == null) return 0.0;
            return awardedMarks;
        }

        // Weighted contribution
        @Override
        public double getContribution() {
            return getGrade() * (weight / 100.0);
        }
    }

    // =========================
    // Module class
    // =========================
    public static class Module {
        private int moduleID;                 // Unique module ID
        private int level;                    // Level: 4,5,6
        private List<Assessment> assessments = new ArrayList<>(); // All assessments in module

        // Constructor
        public Module(int moduleID, int level) {
            this.moduleID = moduleID;
            this.level = level;
        }

        // =========================
        // Compute module score (sum of weighted contributions)
        // =========================
        public double getModuleScore() {
            // If any assessment is missing marks, module score = 0
            for (Assessment a : assessments) {
                if (a.getAwardedMarks() == null) return 0.0;
            }
            double total = 0.0;
            for (Assessment a : assessments) total += a.getContribution(); // Polymorphic call
            return total;
        }

        // Check if all assessments have marks
        public boolean hasAllAssessmentsCompleted() {
            for (Assessment a : assessments) {
                if (a.getAwardedMarks() == null) return false;
            }
            return true;
        }

        // =========================
        // Helper: get assessment by ID
        // =========================
        public Assessment getAssessmentByID(int id) {
            for (Assessment a : assessments) {
                if (a.getAssessmentID() == id) return a;
            }
            return null; // Not found
        }

        // =========================
        // Getters
        // =========================
        public int getModuleID() { return moduleID; }
        public int getLevel() { return level; }
        public List<Assessment> getAssessments() { return assessments; }
    }

    // =========================
    // Student class
    // =========================
    public static class Student {
        private int studentID;               // Unique student ID
        private List<Module> modules = new ArrayList<>(); // Modules enrolled

        // Constructor
        public Student(int studentID) { this.studentID = studentID; }

        // =========================
        // Helper: get module by ID
        // =========================
        public Module getModuleByID(int id) {
            for (Module m : modules) {
                if (m.getModuleID() == id) return m;
            }
            return null; // Not found
        }

        // =========================
        // Compute degree score
        // =========================
        public double getDegreeScore() {
            List<Module> mods = new ArrayList<>(modules); // Copy modules

            // =====================
            // Remove lowest graded module (any level)
            // =====================
            if (!mods.isEmpty()) {
                mods.sort(Comparator.comparingDouble(Module::getModuleScore)); // Ascending
                mods.remove(0); // Remove lowest
            }

            // =====================
            // Compute weighted averages for level 5 and 6
            // =====================
            double level5Sum = 0.0, level6Sum = 0.0;
            int level5Count = 0, level6Count = 0;

            for (Module m : mods) {
                if (m.getLevel() == 5) { level5Sum += m.getModuleScore(); level5Count++; }
                else if (m.getLevel() == 6) { level6Sum += m.getModuleScore(); level6Count++; }
            }

            double level5Avg = (level5Count == 0) ? 0 : level5Sum / level5Count;
            double level6Avg = (level6Count == 0) ? 0 : level6Sum / level6Count;

            return (level5Avg * 0.3) + (level6Avg * 0.7);
        }

        // =========================
        // Compute degree classification
        // =========================
        public String getDegreeClass() {
            double score = getDegreeScore();
            if (score < 40) return "Fail - You shall NOT PASS!";
            if (score < 50) return "Third Class";
            if (score < 60) return "Second Class Second Division";
            if (score < 70) return "Second Class First Division";
            return "First Class - You bow to no one!";
        }

        // =========================
        // Getters
        // =========================
        public int getStudentID() { return studentID; }
        public List<Module> getModules() { return modules; }
    }

    // =========================
    // Database path
    // =========================
    private static final String DB_PATH = "/home/omri/software_development/assignment-1/university.db";

    // =========================
    // Load all student IDs from DB
    // =========================
    public static List<Integer> loadAllStudentIDs() throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT StudentID FROM StudentEnrolment ORDER BY StudentID");
            while (rs.next()) ids.add(rs.getInt("StudentID"));
        }
        return ids;
    }

    // =========================
    // Load a student by ID
    // =========================
    public static Student loadStudent(int studentID) throws SQLException {
        Student student = new Student(studentID);

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {

            // =====================
            // Load all modules for student
            // =====================
            PreparedStatement psModules = conn.prepareStatement(
                    "SELECT DISTINCT ModuleID FROM StudentEnrolment WHERE StudentID=?");
            psModules.setInt(1, studentID);
            ResultSet rsModules = psModules.executeQuery();

            while (rsModules.next()) {
                int moduleID = rsModules.getInt("ModuleID");

                // Get module level
                PreparedStatement psLevel = conn.prepareStatement(
                        "SELECT Level FROM Module WHERE ModuleID=?");
                psLevel.setInt(1, moduleID);
                ResultSet rsLevel = psLevel.executeQuery();
                int level = rsLevel.next() ? rsLevel.getInt("Level") : 0;

                Module module = new Module(moduleID, level);

                // =====================
                // Load assessments for module
                // =====================
                PreparedStatement psAssess = conn.prepareStatement(
                        "SELECT AssessmentID, AssessmentType, MaximumMarks, AwardedMarks " +
                                "FROM Assessment WHERE StudentID=? AND ModuleID=?");
                psAssess.setInt(1, studentID);
                psAssess.setInt(2, moduleID);
                ResultSet rsAssess = psAssess.executeQuery();

                while (rsAssess.next()) {
                    int aid = rsAssess.getInt("AssessmentID");
                    String type = rsAssess.getString("AssessmentType");
                    Double maxMarks = rsAssess.getObject("MaximumMarks") != null ? rsAssess.getDouble("MaximumMarks") : null;
                    Double awardedMarks = rsAssess.getObject("AwardedMarks") != null ? rsAssess.getDouble("AwardedMarks") : null;

                    // Load weight
                    PreparedStatement psWeight = conn.prepareStatement(
                            "SELECT Weighting FROM AssessmentStructure WHERE AssessmentID=?");
                    psWeight.setInt(1, aid);
                    ResultSet rsWeight = psWeight.executeQuery();
                    double weight = rsWeight.next() ? rsWeight.getDouble("Weighting") : 0.0;

                    Assessment assessment;

                    // Instantiate Exam or Coursework
                    if ("Exam".equalsIgnoreCase(type)) {
                        assessment = new Exam(aid, moduleID, maxMarks, awardedMarks, weight);
                    } else {
                        assessment = new Coursework(aid, type, moduleID, awardedMarks, weight);
                    }

                    module.getAssessments().add(assessment);
                    rsWeight.close();
                }

                student.getModules().add(module);
            }
        }

        return student;
    }
}