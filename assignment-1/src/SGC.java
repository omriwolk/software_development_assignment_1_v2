// ==========================
// File: SGC.java
// Purpose: Student, Module, Assessment system with grading and DB access
// ==========================

// Import JDBC classes for database access
import java.sql.*;

// Import utilities for lists
import java.util.*;

// ==========================
// Console colors for terminal output
// ==========================
class ConsoleColors {
    public static final String RESET  = "\u001B[0m";   // Reset color to default
    public static final String RED    = "\u001B[31m";  // Fail
    public static final String ORANGE = "\u001B[38;5;208m";  // True orange using 256-color ANSI
    public static final String PURPLE = "\u001B[35m";  // Lower second
    public static final String BLUE   = "\u001B[34m";  // Upper second
    public static final String GREEN  = "\u001B[32m";  // First class
}

// ==========================
// Abstract base class for objects with an ID
// ==========================
abstract class IDObject {
    protected int id; // Field storing the ID

    // Constructor: sets ID and validates it in DB
    public IDObject(int id) throws SQLException {
        this.id = id;
        validateId(); // Calls subclass validation
    }

    // Getter for ID
    public int getId() { return id; }

    // Abstract method for validation, must be implemented by subclasses
    protected abstract void validateId() throws SQLException;
}

// ==========================
// Base class for assessments
// ==========================
abstract class Assessment extends IDObject {
    protected String type;         // Type of assessment
    protected int moduleId;        // Module ID
    protected double maxMarks;     // Maximum marks
    protected Double awardedMarks; // Student marks (nullable)
    protected double weighting;    // Weight in module
    protected int studentId;       // Student ID

    // Constructor
    public Assessment(int assessmentId, int studentId) throws SQLException {
        super(assessmentId);      // Validate assessment exists
        this.studentId = studentId;
        loadFromDB();             // Load data from database
    }

    // Load assessment data from database
    protected void loadFromDB() throws SQLException {
        // SQL query: select type, module, max, and awarded marks
        String query = "SELECT AssessmentType, ModuleID, MaximumMarks, AwardedMarks " +
                "FROM Assessment WHERE AssessmentID = ? AND StudentID = ?";
        try (Connection conn = DBConnection.getConnection();       // Open DB connection
             PreparedStatement stmt = conn.prepareStatement(query)) { // Prepare query
            stmt.setInt(1, id);               // Set AssessmentID
            stmt.setInt(2, studentId);       // Set StudentID
            ResultSet rs = stmt.executeQuery();// Execute query
            if (rs.next()) {                  // If record exists
                type = rs.getString("AssessmentType"); // Store type
                moduleId = rs.getInt("ModuleID");     // Store module ID
                maxMarks = rs.getDouble("MaximumMarks"); // Store max marks
                awardedMarks = rs.getObject("AwardedMarks") != null
                        ? rs.getDouble("AwardedMarks") // Store marks if exists
                        : null; // null if not attempted
            }
        }

        // Load weighting from AssessmentStructure table
        String weightQuery = "SELECT Weighting FROM AssessmentStructure " +
                "WHERE AssessmentID = ? AND ModuleID = ?";
        try (Connection conn = DBConnection.getConnection();       // Open DB connection
             PreparedStatement stmt = conn.prepareStatement(weightQuery)) {
            stmt.setInt(1, id);          // Set AssessmentID
            stmt.setInt(2, moduleId);    // Set ModuleID
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) weighting = rs.getDouble("Weighting"); // Store weighting
            else weighting = 0;           // Default to 0 if missing
        }
    }

    // Validate assessment exists in DB
    @Override
    protected void validateId() throws SQLException {
        if (!DBValidator.exists("Assessment", "AssessmentID", id))
            throw new IllegalArgumentException("Assessment ID " + id + " not found!");
    }

    // Check if student has completed assessment
    public boolean isCompleted() { return awardedMarks != null; }

    // Calculate contribution to module score
    public double getContributionToModuleScore() {
        if (!isCompleted()) return 0;            // Missing assessment counts 0
        return (awardedMarks / maxMarks) * weighting; // Weighted contribution
    }

    // Getter for module ID
    public int getModuleId() { return moduleId; }
}

// ==========================
// Exam subclass
// Exams can have varying total marks
// ==========================
class Exam extends Assessment {
    public Exam(int assessmentId, int studentId) throws SQLException {
        super(assessmentId, studentId);
    }
}

// ==========================
// Presentation subclass
// Rubric out of 100
// ==========================
class Presentation extends Assessment {
    public Presentation(int assessmentId, int studentId) throws SQLException {
        super(assessmentId, studentId);
        this.maxMarks = 100; // fixed rubric
    }
}

// ==========================
// Report subclass
// Rubric out of 100
// ==========================
class Report extends Assessment {
    public Report(int assessmentId, int studentId) throws SQLException {
        super(assessmentId, studentId);
        this.maxMarks = 100; // fixed rubric
    }
}

// ==========================
// Module class
// ==========================
class Module extends IDObject {
    private int level;                    // Module level
    private List<Assessment> assessments = new ArrayList<>();

    public Module(int moduleId, int studentId) throws SQLException {
        super(moduleId);           // Validate module ID
        loadFromDB();              // Load module info
        loadAssessments(studentId);// Load assessments for student
    }

    // Load module level
    private void loadFromDB() throws SQLException {
        String query = "SELECT Level FROM Module WHERE ModuleID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) level = rs.getInt("Level");
        }
    }

    // Load all assessments for module/student
    private void loadAssessments(int studentId) throws SQLException {
        String query = "SELECT AssessmentID, AssessmentType FROM Assessment WHERE ModuleID = ? AND StudentID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.setInt(2, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int aid = rs.getInt("AssessmentID");        // Get assessment ID
                String atype = rs.getString("AssessmentType"); // Get type
                switch (atype) {
                    case "Exam" -> assessments.add(new Exam(aid, studentId));
                    case "Presentation" -> assessments.add(new Presentation(aid, studentId));
                    case "Report" -> assessments.add(new Report(aid, studentId));
                }
            }
        }
    }

    // Validate module exists
    @Override
    protected void validateId() throws SQLException {
        if (!DBValidator.exists("Module", "ModuleID", id))
            throw new IllegalArgumentException("Module ID " + id + " not found!");
    }

    public int getLevel() { return level; }              // Getter for level
    public List<Assessment> getAssessments() { return assessments; } // Getter

    // Check if all assessments are completed
    public boolean allAssessmentsCompleted() {
        for (Assessment a : assessments) if (!a.isCompleted()) return false;
        return true;
    }

    // Calculate total module score
    public double calculateModuleScore() {
        if (!allAssessmentsCompleted()) return 0; // incomplete = 0
        double total = 0;
        for (Assessment a : assessments) total += a.getContributionToModuleScore();
        return total;
    }

    // Get module grade as string with color
    public String getModuleGrade() {
        if (!allAssessmentsCompleted()) return "Fail - You shall NOT PASS!";
        return GradeClassifier.classify(calculateModuleScore());
    }
}

// ==========================
// Student class
// ==========================
class Student extends IDObject {
    private List<Module> modules = new ArrayList<>();

    public Student(int id) throws SQLException {
        super(id);            // Validate student ID
        loadModules();        // Load enrolled modules
    }

    // Load all modules student is enrolled in
    private void loadModules() throws SQLException {
        String query = "SELECT ModuleID FROM StudentEnrolment WHERE StudentID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) modules.add(new Module(rs.getInt("ModuleID"), id));
        }
    }

    @Override
    protected void validateId() throws SQLException {
        if (!DBValidator.exists("StudentEnrolment", "StudentID", id))
            throw new IllegalArgumentException("Student ID " + id + " not found!");
    }

    public List<Module> getModules() { return modules; } // Getter

    // Calculate degree score with weights: level 5=30%, level6=70%
    public double calculateDegreeScore() {
        double total = 0, weightSum = 0;
        for (Module m : modules) {
            if (!m.allAssessmentsCompleted()) continue; // skip incomplete modules
            if (m.getLevel() == 5) { total += m.calculateModuleScore() * 0.3; weightSum += 0.3; }
            else if (m.getLevel() == 6) { total += m.calculateModuleScore() * 0.7; weightSum += 0.7; }
        }
        return weightSum == 0 ? 0 : total / weightSum;
    }

    // Get degree grade with color
    public String getDegreeGrade() {
        return GradeClassifier.classify(calculateDegreeScore());
    }
}

// ==========================
// Database connection helper
// ==========================
class DBConnection {
    private static final String URL = "jdbc:sqlite:/home/omri/software_development/assignment-1/university.db";

    // Returns a new connection
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}

// ==========================
// DB validation helper
// ==========================
class DBValidator {
    // Check if a given ID exists in a table
    public static boolean exists(String table, String column, int id) throws SQLException {
        String query = "SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // true if record exists
        }
    }
}

// ==========================
// Grade classifier
// Converts numeric score to literal + color
// ==========================
class GradeClassifier {
    public static String classify(double score) {
        if (score < 40) return ConsoleColors.RED + "Fail - You shall NOT PASS!" + ConsoleColors.RESET;
        else if (score < 50) return ConsoleColors.ORANGE + "Third Class Honours" + ConsoleColors.RESET;
        else if (score < 60) return ConsoleColors.PURPLE + "Lower Second Class Honours" + ConsoleColors.RESET;
        else if (score < 70) return ConsoleColors.BLUE + "Upper Second Class Honours" + ConsoleColors.RESET;
        else return ConsoleColors.GREEN + "First Class Honours - You bow to no one!" + ConsoleColors.RESET;
    }
}