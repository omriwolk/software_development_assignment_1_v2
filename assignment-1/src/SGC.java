import java.sql.*;        // Import all SQL classes (Connection, ResultSet, etc.)
import java.util.*;       // Import collections like List, Map, ArrayList, HashMap

public class SGC {        // Main class containing ALL system logic

    // =========================
    // ABSTRACT CLASS: Assessment
    // =========================
    public static abstract class Assessment {   // Abstract base class (cannot be instantiated)

        protected int assessmentID;      // Unique ID of the assessment
        protected String type;           // Type (Exam, Coursework, etc.)
        protected int moduleID;          // Module this assessment belongs to
        protected Double awardedMarks;   // Marks student received (nullable)
        protected double weight;         // Weight (%) of this assessment

        public Assessment(int assessmentID, String type, int moduleID,
                          Double awardedMarks, double weight) {

            this.assessmentID = assessmentID;     // Assign ID
            this.type = type;                     // Assign type
            this.moduleID = moduleID;             // Assign module ID
            this.awardedMarks = awardedMarks;     // Assign marks (may be null)
            this.weight = weight;                 // Assign weight
        }

        public abstract double getGrade();        // Must be implemented by subclasses
        public abstract double getContribution(); // Must be implemented by subclasses

        public int getAssessmentID() { return assessmentID; } // Getter
        public String getType() { return type; }              // Getter
        public int getModuleID() { return moduleID; }         // Getter
        public Double getAwardedMarks() { return awardedMarks; } // Getter
        public double getWeight() { return weight; }          // Getter
    }

    // =========================
    // EXAM
    // =========================
    public static class Exam extends Assessment {  // Exam is a specific type of Assessment

        private Double maxMarks;  // Maximum possible marks for exam

        public Exam(int assessmentID, int moduleID,
                    Double maxMarks, Double awardedMarks, double weight) {

            super(assessmentID, "Exam", moduleID, awardedMarks, weight);
            // Calls parent constructor with type fixed as "Exam"

            this.maxMarks = maxMarks; // Store max marks
        }

        public Double getMaxMarks() { return maxMarks; } // Getter

        @Override
        public double getGrade() {
            if (awardedMarks == null || maxMarks == null) return 0.0;
            // If missing data → grade is 0

            return (awardedMarks / maxMarks) * 100.0;
            // Convert raw score into percentage
        }

        @Override
        public double getContribution() {
            return getGrade() * (weight / 100.0);
            // Weighted contribution toward module score
        }
    }

    // =========================
    // COURSEWORK
    // =========================
    public static class Coursework extends Assessment {

        public Coursework(int assessmentID, String type,
                          int moduleID, Double awardedMarks, double weight) {

            super(assessmentID, type, moduleID, awardedMarks, weight);
            // Uses given type (e.g., "CW1", "Project")
        }

        @Override
        public double getGrade() {
            return awardedMarks == null ? 0.0 : awardedMarks;
            // Coursework is already a percentage → return directly
        }

        @Override
        public double getContribution() {
            return getGrade() * (weight / 100.0);
            // Apply weighting
        }
    }

    // =========================
    // MODULE (with caching)
    // =========================
    public static class Module {

        private int moduleID;                // Unique module ID
        private int level;                  // Level (5 or 6)
        private List<Assessment> assessments = new ArrayList<>();
        // List of assessments in this module

        private Double cachedScore = null;  // Cache for performance

        public Module(int moduleID, int level) {
            this.moduleID = moduleID;  // Store ID
            this.level = level;        // Store level
        }

        public double getModuleScore() {

            if (cachedScore != null) return cachedScore;
            // If already calculated → return cached value

            double total = 0.0; // Sum of contributions

            for (Assessment a : assessments) {
                if (a.getAwardedMarks() == null) {
                    cachedScore = 0.0;
                    return cachedScore;
                    // If ANY assessment missing → module = 0
                }
                total += a.getContribution(); // Add weighted contribution
            }

            cachedScore = total; // Save result in cache
            return cachedScore;
        }

        private void invalidateCache() {
            cachedScore = null; // Reset cache when data changes
        }

        public void addAssessment(Assessment a) {
            assessments.add(a);   // Add assessment
            invalidateCache();    // Invalidate cache
        }

        public List<Assessment> getAssessments() {
            return Collections.unmodifiableList(assessments);
            // Return read-only list (safe design)
        }

        public Assessment getAssessmentByID(int id) {
            for (Assessment a : assessments) {
                if (a.getAssessmentID() == id) return a; // Find match
            }
            return null; // Not found
        }

        public int getModuleID() { return moduleID; } // Getter
        public int getLevel() { return level; }       // Getter
    }

    // =========================
    // DEGREE CLASS ENUM
    // =========================
    public enum DegreeClass {

        FAIL(0, 40, "Fail - You shall NOT PASS!"),
        THIRD(40, 50, "Third Class"),
        LOWER_SECOND(50, 60, "Second Class Second Division"),
        UPPER_SECOND(60, 70, "Second Class First Division"),
        FIRST(70, 100, "First Class - You bow to no one!");

        private final double min;        // Lower bound
        private final double max;        // Upper bound
        private final String description;// Text description

        DegreeClass(double min, double max, String description) {
            this.min = min;
            this.max = max;
            this.description = description;
        }

        public String getDescription() { return description; }

        public static DegreeClass fromScore(double score) {
            for (DegreeClass dc : values()) {
                if (score >= dc.min && score < dc.max) return dc;
                // Find matching range
            }
            return FIRST; // Edge case (score = 100)
        }
    }

    // =========================
    // DEGREE CALCULATOR
    // =========================
    public static class DegreeCalculator {

        public static double calculateDegreeScore(List<Module> modules) {

            List<Module> valid = removeLowest(modules);
            // Drop lowest module

            double l5 = avg(valid, 5); // Avg Level 5
            double l6 = avg(valid, 6); // Avg Level 6

            return (l5 * 0.3) + (l6 * 0.7);
            // Apply weighting rule
        }

        public static DegreeClass classify(double score) {
            return DegreeClass.fromScore(score); // Convert to classification
        }

        private static List<Module> removeLowest(List<Module> modules) {
            if (modules.isEmpty()) return modules;

            List<Module> copy = new ArrayList<>(modules); // Copy list
            copy.sort(Comparator.comparingDouble(Module::getModuleScore));
            // Sort ascending

            copy.remove(0); // Remove lowest
            return copy;
        }

        private static double avg(List<Module> modules, int level) {
            double sum = 0;
            int count = 0;

            for (Module m : modules) {
                if (m.getLevel() == level) {
                    sum += m.getModuleScore(); // Add score
                    count++;                  // Count module
                }
            }

            return count == 0 ? 0 : sum / count; // Avoid division by zero
        }
    }

    // =========================
    // STUDENT
    // =========================
    public static class Student {

        private int studentID;              // Student ID
        private List<Module> modules = new ArrayList<>();

        public Student(int studentID) {
            this.studentID = studentID; // Store ID
        }

        public double getDegreeScore() {
            return DegreeCalculator.calculateDegreeScore(modules);
        }

        public DegreeClass getDegreeClass() {
            return DegreeCalculator.classify(getDegreeScore());
        }

        public Module getModuleByID(int id) {
            for (Module m : modules) {
                if (m.getModuleID() == id) return m;
            }
            return null;
        }

        public int getStudentID() { return studentID; }
        public List<Module> getModules() { return modules; }
    }

    // =========================
    // DATABASE
    // =========================

    private static final String DB_PATH =
            "/home/omri/software_development/assignment-1/university.db";
    // Path to SQLite DB

    public static List<Integer> loadAllStudentIDs() throws SQLException {

        List<Integer> ids = new ArrayList<>(); // Store results

        String query = "SELECT DISTINCT StudentID FROM StudentEnrolment";
        // Get all unique student IDs

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ids.add(rs.getInt("StudentID")); // Add each ID
            }
        }

        return ids; // Return list
    }

    public static Student loadStudent(int studentID) throws SQLException {

        Student student = new Student(studentID); // Create student object

        String query = """
            SELECT 
                m.ModuleID,
                m.Level,
                a.AssessmentID,
                a.AssessmentType,
                a.MaximumMarks,
                a.AwardedMarks,
                s.Weighting
            FROM StudentEnrolment se
            JOIN Module m ON se.ModuleID = m.ModuleID
            JOIN Assessment a ON a.ModuleID = m.ModuleID AND a.StudentID = se.StudentID
            JOIN AssessmentStructure s ON s.AssessmentID = a.AssessmentID
            WHERE se.StudentID = ?
        """;
        // Complex join to gather ALL needed data

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, studentID); // Bind parameter

            ResultSet rs = stmt.executeQuery();

            Map<Integer, Module> moduleMap = new HashMap<>();
            // Prevent duplicate modules

            while (rs.next()) {

                int moduleID = rs.getInt("ModuleID");
                int level = rs.getInt("Level");

                Module module = moduleMap.computeIfAbsent(
                        moduleID, id -> new Module(id, level)
                );
                // Create module if not already exists

                int aid = rs.getInt("AssessmentID");
                String type = rs.getString("AssessmentType");

                Double awarded = rs.getObject("AwardedMarks") != null
                        ? rs.getDouble("AwardedMarks") : null;

                Double max = rs.getObject("MaximumMarks") != null
                        ? rs.getDouble("MaximumMarks") : null;

                double weight = rs.getDouble("Weighting");

                Assessment a = "Exam".equalsIgnoreCase(type)
                        ? new Exam(aid, moduleID, max, awarded, weight)
                        : new Coursework(aid, type, moduleID, awarded, weight);
                // Create correct subclass

                module.addAssessment(a); // Add to module
            }

            student.getModules().addAll(moduleMap.values());
            // Attach modules to student
        }

        return student; // Return fully built student object
    }
}