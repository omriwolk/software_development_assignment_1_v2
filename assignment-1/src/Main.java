// ==========================
// File: Main.java
// Purpose: Prints student module scores and degree grades with colors
// ==========================

import java.sql.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) { // Open DB connection

            // 1️⃣ Get all student IDs
            List<Integer> studentIds = new ArrayList<>();
            String studentQuery = "SELECT DISTINCT StudentID FROM StudentEnrolment";
            try (PreparedStatement stmt = conn.prepareStatement(studentQuery)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) studentIds.add(rs.getInt("StudentID")); // Add ID to list
            }

            // 2️⃣ Print table header
            System.out.printf("%-10s %-8s %-6s %-12s %-35s%n",
                    "StudentID", "ModuleID", "Level", "Score", "Module Grade");
            System.out.println("================================================================================");

            // 3️⃣ Loop through students
            for (int sid : studentIds) {
                Student student = new Student(sid); // Load student object

                // 4️⃣ Loop through student's modules
                for (Module m : student.getModules()) {
                    System.out.printf("%-10d %-8d %-6d %-12.2f %-35s%n",
                            sid,              // Student ID
                            m.getId(),        // Module ID
                            m.getLevel(),     // Module level
                            m.calculateModuleScore(), // Module score
                            m.getModuleGrade());      // Grade with color
                }

                // 5️⃣ Print degree summary per student
                System.out.printf("%-10d %-8s %-6s %-12.2f %-35s%n",
                        sid,
                        "-", "-",                     // No specific module
                        student.calculateDegreeScore(),
                        "Degree Grade: " + student.getDegreeGrade());
                System.out.println("================================================================================");
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Print DB errors if any
        }
    }
}