// ==========================
// File: SGC.java
// ==========================

import java.sql.*;       // Import JDBC classes for database connection, statements, result sets
import java.util.*;      // Import utility classes like List and ArrayList

// ==========================
// Console colors for grade highlighting
// ==========================
class ConsoleColors {
    public static final String RESET  = "\u001B[0m";    // ANSI code to reset color
    public static final String RED    = "\u001B[31m";   // Red text, used for Fail
    public static final String ORANGE = "\u001B[38;5;208m"; // Orange, Third Class
    public static final String PURPLE = "\u001B[35m";   // Purple, Lower Second
    public static final String BLUE   = "\u001B[34m";   // Blue, Upper Second
    public static final String GREEN  = "\u001B[32m";   // Green, First Class
}

// ==========================
// Base class with ID
// ==========================
abstract class IDObject {
    protected int id; // Field to store the unique ID for the object

    // Constructor assigns the ID and validates it in DB
    public IDObject(int id) throws SQLException {
        this.id = id;       // Store ID
        validateId();       // Ensure object exists in DB
    }

    public int getId() { return id; } // Getter for the ID

    // Subclasses must implement validation
    protected abstract void validateId() throws SQLException;
}

// ==========================
// Assessment base class
// ==========================
abstract class Assessment extends IDObject {

    protected String type;        // Assessment type (Exam, Report, Presentation)
    protected int moduleId;       // Module ID the assessment belongs to
    protected double maxMarks;    // Maximum marks possible
    protected Double awardedMarks; // Marks awarded to student (nullable if not graded)
    protected double weighting;   // Weight of assessment in module
    protected int studentId;      // Student ID

    // Constructor: load assessment from DB
    public Assessment(int id, int studentId) throws SQLException {
        super(id);               // Validate assessment ID
        this.studentId = studentId;  // Store student ID
        loadFromDB();            // Load assessment data from database
    }

    // Load assessment details from database
    protected void loadFromDB() throws SQLException {
        // Query to get assessment type, module, max marks, and awarded marks
        String query = "SELECT AssessmentType, ModuleID, MaximumMarks, AwardedMarks FROM Assessment WHERE AssessmentID=? AND StudentID=?";
        try (Connection conn = DBConnection.getConnection();            // Connect to DB
             PreparedStatement stmt = conn.prepareStatement(query)) {   // Prepare query

            stmt.setInt(1, id);        // Set assessment ID
            stmt.setInt(2, studentId); // Set student ID

            ResultSet rs = stmt.executeQuery(); // Execute query
            if (rs.next()) {                     // If record exists
                type = rs.getString("AssessmentType"); // Store type
                moduleId = rs.getInt("ModuleID");     // Store module ID
                maxMarks = rs.getDouble("MaximumMarks"); // Max marks
                awardedMarks = rs.getObject("AwardedMarks") != null
                        ? rs.getDouble("AwardedMarks")
                        : null;  // Null if not graded yet
            }
        }

        // Query to load weighting for this assessment from module structure
        String wq = "SELECT Weighting FROM AssessmentStructure WHERE AssessmentID=? AND ModuleID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(wq)) {

            stmt.setInt(1, id);        // Set assessment ID
            stmt.setInt(2, moduleId);  // Set module ID

            ResultSet rs = stmt.executeQuery();
            weighting = rs.next() ? rs.getDouble("Weighting") : 0; // Default 0 if missing
        }
    }

    @Override
    protected void validateId() throws SQLException {
        // Throw exception if assessment doesn't exist in DB
        if (!DBValidator.exists("Assessment", "AssessmentID", id))
            throw new IllegalArgumentException("Assessment not found");
    }

    public boolean isCompleted() { return awardedMarks != null; } // True if student has marks

    public double getContributionToModuleScore() {
        if (!isCompleted()) return 0; // If not graded, no contribution
        return (awardedMarks / maxMarks) * weighting; // Weighted score
    }

    public int getModuleId() { return moduleId; } // Getter

    public double getWeighting() { return weighting; } // Getter for weighting
}

// ==========================
// Assessment subtypes
// ==========================
class Exam extends Assessment {
    public Exam(int id, int sid) throws SQLException { super(id, sid); }
}

class Presentation extends Assessment {
    public Presentation(int id, int sid) throws SQLException {
        super(id, sid);
        maxMarks = 100; // Default max marks
    }
}

class Report extends Assessment {
    public Report(int id, int sid) throws SQLException {
        super(id, sid);
        maxMarks = 100; // Default max marks
    }
}

// ==========================
// Module class
// ==========================
class Module extends IDObject {

    private int level;                      // Level of the module (4,5,6)
    private List<Assessment> assessments = new ArrayList<>(); // List of assessments

    public Module(int id, int studentId) throws SQLException {
        super(id);                 // Validate module ID
        loadFromDB();              // Load module data
        loadAssessments(studentId); // Load assessments for student
    }

    private void loadFromDB() throws SQLException {
        String q = "SELECT Level FROM Module WHERE ModuleID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(q)) {

            stmt.setInt(1, id);        // Set module ID
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) level = rs.getInt("Level"); // Store module level
        }
    }

    private void loadAssessments(int sid) throws SQLException {
        String q = "SELECT AssessmentID, AssessmentType FROM Assessment WHERE ModuleID=? AND StudentID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(q)) {

            stmt.setInt(1, id);        // Module ID
            stmt.setInt(2, sid);       // Student ID

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {        // For each assessment
                int aid = rs.getInt("AssessmentID");
                String type = rs.getString("AssessmentType");

                switch (type) {        // Instantiate proper subclass
                    case "Exam" -> assessments.add(new Exam(aid, sid));
                    case "Presentation" -> assessments.add(new Presentation(aid, sid));
                    case "Report" -> assessments.add(new Report(aid, sid));
                }
            }
        }
    }

    @Override
    protected void validateId() throws SQLException {
        if (!DBValidator.exists("Module", "ModuleID", id))
            throw new IllegalArgumentException("Module not found");
    }

    public int getLevel() { return level; }

    public List<Assessment> getAssessments() { return assessments; } // Return list

    public boolean allAssessmentsCompleted() {
        for (Assessment a : assessments)
            if (!a.isCompleted()) return false; // Any incomplete → false
        return true; // All completed
    }

    public double calculateModuleScore() {
        if (!allAssessmentsCompleted()) return 0; // Cannot calculate if incomplete
        double total = 0;
        for (Assessment a : assessments)
            total += a.getContributionToModuleScore(); // Sum contributions
        return total;
    }

    public String getModuleGrade() {
        if (!allAssessmentsCompleted())
            return ConsoleColors.RED + "Fail - You shall NOT PASS!" + ConsoleColors.RESET;
        return GradeClassifier.classify(calculateModuleScore()); // Grade string
    }
}

// ==========================
// Student class
// ==========================
class Student extends IDObject {

    private List<Module> modules = new ArrayList<>(); // Modules enrolled

    public Student(int id) throws SQLException {
        super(id);       // Validate student ID
        loadModules();   // Load all modules
    }

    private void loadModules() throws SQLException {
        String q = "SELECT ModuleID FROM StudentEnrolment WHERE StudentID=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(q)) {

            stmt.setInt(1, id);       // Set student ID
            ResultSet rs = stmt.executeQuery();

            while (rs.next())
                modules.add(new Module(rs.getInt("ModuleID"), id)); // Load module for student
        }
    }

    @Override
    protected void validateId() throws SQLException {
        if (!DBValidator.exists("StudentEnrolment", "StudentID", id))
            throw new IllegalArgumentException("Student not found");
    }

    public List<Module> getModules() { return modules; }

    // Get module by ID for CLI selection
    public Module getModuleById(int moduleId) {
        for (Module m : modules)
            if (m.getId() == moduleId) return m;
        return null; // Not found
    }

    // Get all assessments for this student
    public List<Assessment> getAllAssessments() {
        List<Assessment> all = new ArrayList<>();
        for (Module m : modules)
            all.addAll(m.getAssessments()); // Flatten all assessments
        return all;
    }

    // Degree calculation: weighted by level
    public double calculateDegreeScore() {
        double total = 0, weight = 0;
        for (Module m : modules) {
            if (!m.allAssessmentsCompleted()) continue; // Skip incomplete
            if (m.getLevel() == 5) { total += m.calculateModuleScore() * 0.3; weight += 0.3; }
            else if (m.getLevel() == 6) { total += m.calculateModuleScore() * 0.7; weight += 0.7; }
        }
        return weight == 0 ? 0 : total / weight;
    }

    public String getDegreeGrade() { return GradeClassifier.classify(calculateDegreeScore()); }
}

// ==========================
// DB helpers
// ==========================
class DBConnection {
    private static final String URL = "jdbc:sqlite:/home/omri/software_development/assignment-1/university.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL); // Returns a live DB connection
    }
}

class DBValidator {
    public static boolean exists(String table, String column, int id) throws SQLException {
        String q = "SELECT 1 FROM " + table + " WHERE " + column + "=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(q)) {

            stmt.setInt(1, id); // Set value
            return stmt.executeQuery().next(); // True if row exists
        }
    }
}

// ==========================
// Grade classifier
// ==========================
class GradeClassifier {
    public static String classify(double score) {
        if (score < 40) return ConsoleColors.RED + "Fail - You shall NOT PASS!" + ConsoleColors.RESET;
        else if (score < 50) return ConsoleColors.ORANGE + "Third Class Honours" + ConsoleColors.RESET;
        else if (score < 60) return ConsoleColors.PURPLE + "Lower Second Class Honours" + ConsoleColors.RESET;
        else if (score < 70) return ConsoleColors.BLUE + "Upper Second Class Honours" + ConsoleColors.RESET;
        else return ConsoleColors.GREEN + "First Class Honours" + ConsoleColors.RESET;
    }
}