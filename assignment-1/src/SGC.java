import java.sql.*;
import java.util.*;

// Main SGC class containing all entities
public class SGC {

    private static final String DB_PATH = "/home/omri/software_development/assignment-1/university.db";

    /** Superclass for objects with an ID */
    public static abstract class IDObject {
        protected int id;

        public IDObject(int id) { this.id = id; }

        public int getId() { return id; }

        protected void validateId(String tableName, String idColumn) throws SQLException {
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT COUNT(*) FROM " + tableName + " WHERE " + idColumn + " = ?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (!rs.next() || rs.getInt(1) == 0) {
                    throw new SQLException("ID " + id + " not found in table " + tableName);
                }
            }
        }
    }

    /** Assessment class */
    public static class Assessment extends IDObject {
        private String type;
        private int moduleId;
        private Double maxMarks;
        private Double awardedMarks;
        private double weighting;

        public Assessment(int id, String type, int moduleId, Double maxMarks, Double awardedMarks, double weighting) {
            super(id);
            this.type = type;
            this.moduleId = moduleId;
            this.maxMarks = maxMarks;
            this.awardedMarks = awardedMarks;
            this.weighting = weighting;
        }

        public String getType() { return type; }
        public int getModuleId() { return moduleId; }
        public Double getMaxMarks() { return maxMarks; }
        public Double getAwardedMarks() { return awardedMarks; }
        public double getWeighting() { return weighting; }

        // Calculate grade according to type
        public double getGrade() {
            if (awardedMarks == null) return 0.0;
            if (type.equalsIgnoreCase("Exam") && maxMarks != null && maxMarks != 0)
                return (awardedMarks / maxMarks) * 100.0;
            return awardedMarks;
        }

        public boolean isIncomplete() { return awardedMarks == null; }
    }

    /** Module class */
    public static class Module extends IDObject {
        private int level;
        private List<Assessment> assessments = new ArrayList<>();

        public Module(int id, int level) {
            super(id);
            this.level = level;
        }

        public int getLevel() { return level; }
        public List<Assessment> getAssessments() { return assessments; }
        public void addAssessment(Assessment a) { assessments.add(a); }

        // Module grade calculation
        public double getModuleScore() {
            if (assessments.stream().anyMatch(Assessment::isIncomplete)) return 0.0;
            double total = 0.0;
            for (Assessment a : assessments) {
                total += a.getGrade() * a.getWeighting() / 100.0;
            }
            return total;
        }

        public boolean hasAllAssessmentsCompleted() {
            return assessments.stream().allMatch(a -> !a.isIncomplete());
        }
    }

    /** Student class */
    public static class Student extends IDObject {
        private List<Module> modules = new ArrayList<>();

        public Student(int id) throws SQLException {
            super(id);
            validateId("StudentEnrolment", "StudentID");
            loadModulesFromDB();
        }

        public List<Module> getModules() { return modules; }

        private void loadModulesFromDB() throws SQLException {
            Map<Integer, Module> moduleMap = new HashMap<>();
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {

                // Load student's modules
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT ModuleID FROM StudentEnrolment WHERE StudentID = ?")) {
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        int mid = rs.getInt("ModuleID");
                        Module m = new Module(mid, getModuleLevel(conn, mid));
                        modules.add(m);
                        moduleMap.put(mid, m);
                    }
                }

                // Load assessments and weightings
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT a.AssessmentID, a.AssessmentType, a.ModuleID, a.MaximumMarks, a.AwardedMarks, s.Weighting " +
                                "FROM Assessment a JOIN AssessmentStructure s ON a.AssessmentID = s.AssessmentID " +
                                "WHERE a.StudentID = ?")) {
                    ps.setInt(1, id);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        int aid = rs.getInt("AssessmentID");
                        String type = rs.getString("AssessmentType");
                        int mid = rs.getInt("ModuleID");
                        Double max = rs.getObject("MaximumMarks") != null ? rs.getDouble("MaximumMarks") : null;
                        Double awarded = rs.getObject("AwardedMarks") != null ? rs.getDouble("AwardedMarks") : null;
                        double weight = rs.getDouble("Weighting");

                        Assessment a = new Assessment(aid, type, mid, max, awarded, weight);
                        if (moduleMap.containsKey(mid)) moduleMap.get(mid).addAssessment(a);
                    }
                }
            }
        }

        private int getModuleLevel(Connection conn, int moduleId) throws SQLException {
            try (PreparedStatement ps = conn.prepareStatement("SELECT Level FROM Module WHERE ModuleID = ?")) {
                ps.setInt(1, moduleId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt("Level");
                throw new SQLException("Module " + moduleId + " not found");
            }
        }

        // Degree score calculation with lowest module dropped
        public double getDegreeScore() {
            if (modules.isEmpty()) return 0.0;
            // List of module scores
            List<Module> completedModules = new ArrayList<>(modules);
            completedModules.sort(Comparator.comparingDouble(Module::getModuleScore));
            // Drop lowest module
            completedModules.remove(0);

            double level5Avg = completedModules.stream()
                    .filter(m -> m.getLevel() == 5)
                    .mapToDouble(Module::getModuleScore)
                    .average().orElse(0.0);

            double level6Avg = completedModules.stream()
                    .filter(m -> m.getLevel() == 6)
                    .mapToDouble(Module::getModuleScore)
                    .average().orElse(0.0);

            return level5Avg * 0.3 + level6Avg * 0.7;
        }

        public String getDegreeClassification() {
            double score = getDegreeScore();
            if (score < 40) return "Fail";
            else if (score < 50) return "Third class";
            else if (score < 60) return "Second class 2nd division";
            else if (score < 70) return "Second class 1st division";
            else return "First class";
        }

        public double getModuleScore(int moduleId) {
            return modules.stream()
                    .filter(m -> m.getId() == moduleId)
                    .mapToDouble(Module::getModuleScore)
                    .findFirst().orElse(0.0);
        }
    }
}