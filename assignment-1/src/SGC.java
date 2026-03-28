// ==========================
// File: SGC.java
// Purpose: Defines all classes for Student, Module, Assessment,
// database access, validation, and grade classification
// ==========================

import java.sql.*;        // For JDBC: Connection, PreparedStatement, ResultSet, SQLException
import java.util.*;       // For List, ArrayList

// ==========================
// Abstract superclass for any object with an ID
// Demonstrates inheritance and encapsulation
// ==========================
abstract class IDObject {
    protected int id; // Encapsulated ID field

    // Constructor: assigns ID and validates in DB
    public IDObject(int id) throws SQLException {
        this.id = id;
        validateId(); // Throws exception if ID not in DB
    }

    // Getter for ID
    public int getId() { return id; }

    // Abstract validation method; subclasses implement
    protected abstract void validateId() throws SQLException;
}

// ==========================
// Assessment class: represents an assessment for a student
// ==========================
class Assessment extends IDObject {
    private String type;        // Assessment type (Exam, Report, Presentation)
    private int moduleId;       // Module this assessment belongs to
    private double maxMarks;    // Maximum marks
    private Double awardedMarks;// Awarded marks (nullable)
    private double weighting;   // Weight of this assessment in module
    private int studentId;      // Student who took it

    // Constructor
    public Assessment(int assessmentId, int studentId) throws SQLException {
        super(assessmentId);    // Validate assessment ID
        this.studentId = studentId;
        loadFromDB();           // Load assessment details and weighting
    }

    // Load assessment info from database
    private void loadFromDB() throws SQLException {
        // Query Assessment table
        String query = "SELECT AssessmentType, ModuleID, MaximumMarks, AwardedMarks " +
                "FROM Assessment WHERE AssessmentID = ? AND StudentID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.setInt(2, studentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                type = rs.getString("AssessmentType");
                moduleId = rs.getInt("ModuleID");
                maxMarks = rs.getDouble("MaximumMarks");
                awardedMarks = rs.getObject("AwardedMarks") != null
                        ? rs.getDouble("AwardedMarks")
                        : null; // null means not attempted
            }
        }

        // Query AssessmentStructure table for weighting
        String weightQuery = "SELECT Weighting FROM AssessmentStructure " +
                "WHERE AssessmentID = ? AND ModuleID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(weightQuery)) {
            stmt.setInt(1, id);
            stmt.setInt(2, moduleId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) weighting = rs.getDouble("Weighting");
            else weighting = 0;
        }
    }

    // Validate assessment exists
    @Override
    protected void validateId() throws SQLException {
        if (!DBValidator.exists("Assessment", "AssessmentID", id))
            throw new IllegalArgumentException("Assessment ID " + id + " not found!");
    }

    // Check if assessment is completed (awarded marks exist)
    public boolean isCompleted() { return awardedMarks != null; }

    // Contribution of this assessment to module score
    public double getContributionToModuleScore() {
        if (!isCompleted()) return 0; // Missing assessment contributes 0
        return (awardedMarks / maxMarks) * weighting; // Weighted score
    }

    // Getter for module ID
    public int getModuleId() { return moduleId; }
}

// ==========================
// Module class: represents a module
// ==========================
class Module extends IDObject {
    private int level;                    // Module level (4, 5, 6)
    private List<Assessment> assessments = new ArrayList<>(); // Assessments in module

    // Constructor
    public Module(int moduleId, int studentId) throws SQLException {
        super(moduleId);          // Validate module ID
        loadFromDB();             // Load module details
        loadAssessments(studentId);// Load student's assessments
    }

    // Load module info
    private void loadFromDB() throws SQLException {
        String query = "SELECT Level FROM Module WHERE ModuleID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) level = rs.getInt("Level");
        }
    }

    // Load all assessments for this student and module
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
        if (!DBValidator.exists("Module", "ModuleID", id))
            throw new IllegalArgumentException("Module ID " + id + " not found!");
    }

    // Getter for level
    public int getLevel() { return level; }

    // Getter for assessments
    public List<Assessment> getAssessments() { return assessments; }

    // Check if all assessments completed
    public boolean allAssessmentsCompleted() {
        for (Assessment a : assessments) {
            if (!a.isCompleted()) return false;
        }
        return true;
    }

    // Calculate module score (sum of weighted assessments)
    public double calculateModuleScore() {
        if (!allAssessmentsCompleted()) return 0; // Fail due to missing assessment
        double total = 0;
        for (Assessment a : assessments) total += a.getContributionToModuleScore();
        return total;
    }

    // Module grade as string
    public String getModuleGrade() {
        if (!allAssessmentsCompleted()) return "Fail (missing assessment)";
        return GradeClassifier.classify(calculateModuleScore());
    }
}

// ==========================
// Student class
// ==========================
class Student extends IDObject {
    private List<Module> modules = new ArrayList<>(); // Enrolled modules

    // Constructor
    public Student(int studentId) throws SQLException {
        super(studentId); // Validate ID
        loadModules();    // Load student's modules
    }

    // Load modules the student is enrolled in
    private void loadModules() throws SQLException {
        String query = "SELECT ModuleID FROM StudentEnrolment WHERE StudentID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                modules.add(new Module(rs.getInt("ModuleID"), id));
            }
        }
    }

    // Validate student ID
    @Override
    protected void validateId() throws SQLException {
        if (!DBValidator.exists("StudentEnrolment", "StudentID", id))
            throw new IllegalArgumentException("Student ID " + id + " not found!");
    }

    // Getter for modules
    public List<Module> getModules() { return modules; }

    // Calculate degree score (level 5 = 30%, level 6 = 70%)
    public double calculateDegreeScore() {
        double total = 0;
        double weightSum = 0;
        for (Module m : modules) {
            if (!m.allAssessmentsCompleted()) continue; // skip incomplete modules
            if (m.getLevel() == 5) { total += m.calculateModuleScore() * 0.3; weightSum += 0.3; }
            else if (m.getLevel() == 6) { total += m.calculateModuleScore() * 0.7; weightSum += 0.7; }
        }
        if (weightSum == 0) return 0;
        return total / weightSum;
    }

    // Degree grade
    public String getDegreeGrade() {
        return GradeClassifier.classify(calculateDegreeScore());
    }
}

// ==========================
// Grade classification helper
// ==========================
class GradeClassifier {
    // Converts numeric score to grade string
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
    // SQLite DB URL pointing to your actual database
    private static final String URL = "jdbc:sqlite:/home/omri/software_development/assignment-1/university.db";

    // Returns a new connection
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}

// ==========================
// Database validation helper
// ==========================
class DBValidator {
    // Checks if a given ID exists in a table column
    public static boolean exists(String table, String column, int id) throws SQLException {
        String query = "SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // true if exists
        }
    }
}