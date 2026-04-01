import java.sql.*;       // For SQLite DB access
import java.util.*;      // For Lists, Comparators, etc.

// =========================
// SGC SYSTEM
// =========================
public class SGC {

    // =========================
    // Abstract base class: Assessment - Cannot be initiated
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
        // Abstract methods - No body, implementations are in subclasses
        // =========================
        public abstract double getGrade();        // Return raw grade

        public abstract double getContribution(); // Weighted contribution to module

        // =========================
        // Getters
        // =========================
        public int getAssessmentID() {
            return assessmentID;
        }

        public String getType() {
            return type;
        }

        public int getModuleID() {
            return moduleID;
        }

        public Double getAwardedMarks() {
            return awardedMarks;
        }

        public double getWeight() {
            return weight;
        }
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
        public Double getMaxMarks() {
            return maxMarks;
        }

        // Compute grade as percentage
        @Override
        public double getGrade() {
            if (awardedMarks == null) {
                return 0.0;
            }

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
            double total = 0.0;
            // If any assessment is missing marks, module score = 0
            for (Assessment a : assessments) {
                if (a.getAwardedMarks() == null) {
                    return 0.0; // Early exit if any mark is missing
                }
                total += a.getContribution();
            }
            return total;
        }

        // =========================
        // Helper: get assessment by ID
        // =========================
        public Assessment getAssessmentByID(int id) {
            for (Assessment a : assessments) {
                if (a.getAssessmentID() == id) {
                    return a;
                }
            }
            return null; // Not found
        }

        // =========================
        // Getters
        // =========================
        public int getModuleID() {
            return moduleID;
        }

        public int getLevel() {
            return level;
        }

        public List<Assessment> getAssessments() {
            return assessments;
        }
    }

    // =========================
    // Student class
    // =========================
    public static class Student {
        private int studentID;               // Unique student ID
        private List<Module> modules = new ArrayList<>(); // Modules enrolled

        // Constructor
        public Student(int studentID) {
            this.studentID = studentID;
        }

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

            // Remove lowest graded module (any level)
            if (!mods.isEmpty()) {
                mods.sort(Comparator.comparingDouble(Module::getModuleScore)); // Ascending
                mods.remove(0); // Remove lowest
            }

            // Compute weighted averages for level 5 and 6
            double level5Sum = 0.0, level6Sum = 0.0;
            int level5Count = 0, level6Count = 0;

            for (Module m : mods) {
                if (m.getLevel() == 5) {
                    level5Sum += m.getModuleScore();
                    level5Count++;
                } else if (m.getLevel() == 6) {
                    level6Sum += m.getModuleScore();
                    level6Count++;
                }
            }

            double level5Avg = level5Sum / level5Count;
            double level6Avg = level6Sum / level6Count;

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
        public int getStudentID() {
            return studentID;
        }

        public List<Module> getModules() {
            return modules;
        }
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

            // Select all unique student IDs from enrolment table, ordered ascending
            ResultSet rs = stmt.executeQuery(
                    "SELECT DISTINCT StudentID FROM StudentEnrolment ORDER BY StudentID"
            );

            while (rs.next()) {
                ids.add(rs.getInt("StudentID"));
            }
        }
        return ids;
    }

    // =========================
    // Load a student by ID (optimized SQL with JOINs)
    // =========================
    public static Student loadStudent(int studentID) throws SQLException {
        // Create a new Student object with the given ID
        Student student = new Student(studentID);

        // Try-with-resources ensures the DB connection is automatically closed
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {

            // =====================
            // Single JOIN query to get modules, assessments, max marks, awarded marks, weight
            // =====================
        /*
        SQL Explanation:
        - StudentEnrolment (e) JOIN Module (m) to get each module's level
        - LEFT JOIN Assessment (a) to get assessment info for this student and module
          (LEFT JOIN ensures we still get modules even if there are no assessments)
        - LEFT JOIN AssessmentStructure (s) to get weighting for each assessment
        - Filter by StudentID
        - ORDER BY ModuleID, AssessmentID to keep consistent ordering
        */
            String sql = """
            SELECT m.ModuleID, m.Level,
                   a.AssessmentID, a.AssessmentType, a.MaximumMarks, a.AwardedMarks,
                   s.Weighting
            FROM StudentEnrolment e
            JOIN Module m ON e.ModuleID = m.ModuleID
            LEFT JOIN Assessment a ON e.StudentID = a.StudentID AND e.ModuleID = a.ModuleID
            LEFT JOIN AssessmentStructure s ON a.AssessmentID = s.AssessmentID
            WHERE e.StudentID = ?
            ORDER BY m.ModuleID, a.AssessmentID
        """;

            // Prepare the SQL statement for execution (prevents SQL injection)
            PreparedStatement ps = conn.prepareStatement(sql);

            // Bind the studentID parameter into the query
            ps.setInt(1, studentID);

            // Execute the query and obtain the result set
            ResultSet rs = ps.executeQuery();

            // Keep track of the current module while iterating through results
            Module currentModule = null;
            int lastModuleID = -1; // Initialize with invalid ID to detect first module

            // Iterate over each row in the result set
            while (rs.next()) {
                // Read the ModuleID from the current row
                int moduleID = rs.getInt("ModuleID");

                // =====================
                // Detect when a new module starts
                // =====================
                if (moduleID != lastModuleID) { // Module changed
                    // Read the module level
                    int level = rs.getInt("Level");

                    // Create a new Module object with the current moduleID and level
                    currentModule = new Module(moduleID, level);

                    // Add the new Module to the student's module list
                    student.getModules().add(currentModule);

                    // Update lastModuleID for next iteration
                    lastModuleID = moduleID;
                }

                // =====================
                // Read assessment data
                // =====================
                int aid = rs.getInt("AssessmentID"); // AssessmentID
                if (rs.wasNull()) continue; // Skip row if no assessment exists for this module

                // Get assessment type: "Exam", "Report", "Presentation", etc.
                String type = rs.getString("AssessmentType");

                // Get maximum marks (nullable for non-Exam assessments)
                Double maxMarks = rs.getObject("MaximumMarks") != null ? rs.getDouble("MaximumMarks") : null;

                // Get awarded marks (nullable if not graded yet)
                Double awardedMarks = rs.getObject("AwardedMarks") != null ? rs.getDouble("AwardedMarks") : null;

                // Get assessment weighting (default to 0.0 if missing)
                double weight = rs.getObject("Weighting") != null ? rs.getDouble("Weighting") : 0.0;

                // =====================
                // Instantiate the correct Assessment subclass
                // =====================
                Assessment assessment;

                // If type is "Exam", use Exam class (with maxMarks)
                if ("Exam".equalsIgnoreCase(type)) {
                    assessment = new Exam(aid, moduleID, maxMarks, awardedMarks, weight);
                }
                // Otherwise, use Coursework (Report, Presentation, etc.)
                else {
                    assessment = new Coursework(aid, type, moduleID, awardedMarks, weight);
                }

                // Add the assessment to the current module's assessment list
                currentModule.getAssessments().add(assessment);
            } // End of result set iteration
        } // Connection is automatically closed here

        // Return the fully loaded Student object with modules and assessments
        return student;
    }
}