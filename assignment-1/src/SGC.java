// SGC.java
// Contains all domain classes: Student, Module, Assessment, DB helpers, and GradeClassifier
// Fully annotated for line-by-line understanding

import java.sql.*;          // JDBC classes for database connection & SQL
import java.util.*;         // Collections: List, ArrayList

// ==========================
// Abstract superclass for objects with IDs
// Demonstrates inheritance and encapsulation
// ==========================
abstract class IDObject {
    protected int id; // Encapsulated ID field, accessible to subclasses

    // Constructor assigns ID and validates it in DB
    public IDObject(int id) throws SQLException {
        this.id = id;
        validateId(); // Abstract method implemented by subclasses
    }

    // Getter for ID
    public int getId() { return id; }

    // Abstract method to force subclasses to implement ID validation
    protected abstract void validateId() throws SQLException;
}

// ==========================
// Assessment class
// Each assessment belongs to a module and a student
// ==========================
class Assessment extends IDObject {
    private String type;        // Assessment type: Exam, Report, etc.
    private int moduleId;       // The module it belongs to
    private double maxMarks;    // Maximum marks achievable
    private Double awardedMarks;// Marks actually awarded (nullable)
    private double weighting;   // Weight in module score calculation
    private int studentId;      // The student this assessment belongs to

    // Constructor
    public Assessment(int assessmentId, int studentId) throws SQLException {
        super(assessmentId);    // Validate ID exists in DB
        this.studentId = studentId;
        loadFromDB();           // Load all data from DB
    }

    // Load assessment data from Assessment & AssessmentStructure tables
    private void loadFromDB() throws SQLException {
        // Load assessment info
        String query = "SELECT AssessmentType, ModuleID, MaximumMarks, AwardedMarks " +
                "FROM Assessment WHERE AssessmentID = ? AND StudentID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);              // Set AssessmentID
            stmt.setInt(2, studentId);       // Set StudentID
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                type = rs.getString("AssessmentType");
                moduleId = rs.getInt("ModuleID");
                maxMarks = rs.getDouble("MaximumMarks");
                // Awarded marks may be null if assessment not completed
                awardedMarks = rs.getObject("AwardedMarks") != null
                        ? rs.getDouble("AwardedMarks") : null;
            }
        }

        // Load weighting from AssessmentStructure table
        String weightQuery = "SELECT Weighting FROM AssessmentStructure WHERE AssessmentID = ? AND ModuleID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(weightQuery)) {
            stmt.setInt(1, id);
            stmt.setInt(2, moduleId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) weighting = rs.getDouble("Weighting");
            else weighting = 0; // default to 0 if no weighting found
        }
    }

    // Validate that Assessment ID exists
    @Override
    protected void validateId() throws SQLException {
        if (!DBValidator.exists("Assessment", "AssessmentID", id)) {
            throw new IllegalArgumentException("Assessment ID " + id + " not found!");
        }
    }

    // Check if assessment has been completed
    public boolean isCompleted() { return awardedMarks != null; }

    // Contribution to module score (weighted)
    public double getContributionToModuleScore() {
        if (!isCompleted()) return 0; // missing assessment contributes 0
        return (awardedMarks / maxMarks) * weighting;
    }

    // Getter for module ID
    public int getModuleId() { return moduleId; }
}

// ==========================
// Module class
// ==========================
class Module extends IDObject {
    private int level;                     // Module level: 4,5,6
    private List<Assessment> assessments = new ArrayList<>(); // Assessments for this module

    // Constructor
    public Module(int moduleId, int studentId) throws SQLException {
        super(moduleId);                   // Validate module ID
        loadFromDB();                      // Load module info
        loadAssessments(studentId);        // Load assessments for this student
    }

    // Load module level from Module table
    private void loadFromDB() throws SQLException {
        String query = "SELECT Level FROM Module WHERE ModuleID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) level = rs.getInt("Level");
        }
    }

    // Load assessments for this module and student
    private void loadAssessments(int studentId) throws SQLException {
        String query = "SELECT AssessmentID FROM Assessment WHERE ModuleID = ? AND StudentID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.setInt(2, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                assessments.add(new Assessment(rs.getInt("AssessmentID"), studentId));
            }
        }
    }

    // Validate module ID
    @Override
    protected void validateId() throws SQLException {
        if (!DBValidator.exists("Module", "ModuleID", id)) {
            throw new IllegalArgumentException("Module ID " + id + " not found!");
        }
    }

    // Getter for level
    public int getLevel() { return level; }

    // Getter for assessments
    public List<Assessment> getAssessments() { return assessments; }

    // Check if all assessments are completed
    public boolean allAssessmentsCompleted() {
        for (Assessment a : assessments) if (!a.isCompleted()) return false;
        return true;
    }

    // Calculate module score by summing weighted contributions
    public double calculateModuleScore() {
        if (!allAssessmentsCompleted()) return 0; // fail if missing assessment
        double total = 0;
        for (Assessment a : assessments) total += a.getContributionToModuleScore();
        return total;
    }

    // Get module grade string
    public String getModuleGrade() {
        if (!allAssessmentsCompleted()) return "Fail (missing assessment)";
        return GradeClassifier.classify(calculateModuleScore());
    }
}

// ==========================
// Student class
// ==========================
class Student extends IDObject {
    private List<Module> modules = new ArrayList<>();

    // Constructor
    public Student(int studentId) throws SQLException {
        super(studentId); // Validate student ID
        loadModules();    // Load enrolled modules
    }

    // Load student's modules from StudentEnrolment table
    private void loadModules() throws SQLException {
        String query = "SELECT ModuleID FROM StudentEnrolment WHERE StudentID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) modules.add(new Module(rs.getInt("ModuleID"), id));
        }
    }

    // Validate student ID exists
    @Override
    protected void validateId() throws SQLException {
        if (!DBValidator.exists("StudentEnrolment", "StudentID", id)) {
            throw new IllegalArgumentException("Student ID " + id + " not found!");
        }
    }

    // Getter for modules
    public List<Module> getModules() { return modules; }

    // Calculate degree score (Level 5 = 30%, Level 6 = 70%)
    public double calculateDegreeScore() {
        double total = 0;
        double weightSum = 0;
        for (Module m : modules) {
            if (!m.allAssessmentsCompleted()) continue; // skip incomplete modules
            if (m.getLevel() == 5) { total += m.calculateModuleScore() * 0.3; weightSum += 0.3; }
            else if (m.getLevel() == 6) { total += m.calculateModuleScore() * 0.7; weightSum += 0.7; }
        }
        if (weightSum == 0) return 0; // no level 5/6 modules completed
        return total / weightSum;
    }

    // Get degree grade string
    public String getDegreeGrade() {
        return GradeClassifier.classify(calculateDegreeScore());
    }
}

// ==========================
// Grade classification helper
// ==========================
class GradeClassifier {
    public static String classify(double score) {
        if (score < 40) return "Fail";
        else if (score < 50) return "Third Class";
        else if (score < 60) return "Second Class 2nd Division";
        else if (score < 70) return "Second Class 1st Division";
        else return "First Class";
    }
}

// ==========================
// Database connection helper
// ==========================
class DBConnection {
    private static final String URL = "jdbc:sqlite:/home/omri/software_development/assignment-1/university.db";

    // Get a connection to SQLite DB
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}

// ==========================
// Database validator helper
// ==========================
class DBValidator {
    public static boolean exists(String table, String column, int id) throws SQLException {
        String query = "SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }
}