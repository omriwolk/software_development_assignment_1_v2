// ==========================
// File: Main.java
// Purpose: Main program to print student module and degree grades
// ==========================

import java.sql.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {

            // 1️⃣ Get all student IDs from StudentEnrolment table
            List<Integer> studentIds = new ArrayList<>();
            String studentQuery = "SELECT DISTINCT StudentID FROM StudentEnrolment";
            try (PreparedStatement stmt = conn.prepareStatement(studentQuery)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) studentIds.add(rs.getInt("StudentID"));
            }

            // 2️⃣ Print table header
            System.out.printf("%-10s %-8s %-6s %-12s %-25s%n",
                    "StudentID", "ModuleID", "Level", "Score", "Module Grade");
            System.out.println("================================================================");

            // 3️⃣ Loop through each student
            for (int sid : studentIds) {
                Student student = new Student(sid); // Load student

                // 4️⃣ Loop through each module
                for (Module m : student.getModules()) {
                    System.out.printf("%-10d %-8d %-6d %-12.2f %-25s%n",
                            sid,
                            m.getId(),
                            m.getLevel(),
                            m.calculateModuleScore(),
                            m.getModuleGrade());
                }

                // 5️⃣ Print degree summary per student
                System.out.printf("%-10d %-8s %-6s %-12.2f %-25s%n",
                        sid,
                        "-", "-", student.calculateDegreeScore(),
                        "Degree Grade: " + student.getDegreeGrade());
                System.out.println("================================================================");
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Print DB errors
        }
    }
}