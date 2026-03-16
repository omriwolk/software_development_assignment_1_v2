// Import JDBC classes used to talk to the database
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Import date-time class to store task creation time
import java.time.OffsetDateTime;

// Repository class responsible for all database operations
// AutoCloseable allows it to be used in try-with-resources
public class TaskRepository implements AutoCloseable {

    // Database connection string for SQLite
    private static final String DB_URL = "jdbc:sqlite:app.db";

    // Connection object used for all database operations
    private final Connection connection;

    // Constructor that opens a connection to the database
    public TaskRepository() throws SQLException {
        this.connection = DriverManager.getConnection(DB_URL);
    }

    // Creates the tasks table if it does not already exist
    public void createTable() throws SQLException {

        // SQL statement defining the table structure
        String sql = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,  -- unique task id
                title TEXT NOT NULL CHECK (trim(title) <> ''),                   -- task title
                is_done INTEGER NOT NULL DEFAULT 0,    -- completion flag (0=false, 1=true)
                created_at TEXT NOT NULL               -- creation timestamp
            );
            """;

        // Create a statement object to execute SQL
        try (Statement statement = connection.createStatement()) {

            // Execute the SQL command
            statement.execute(sql);
        }
    }

    // Inserts a new task into the database and returns the generated id
    public int insertTask(String title) throws SQLException {

        // Handle empty titles
        if (title == null || title.trim().isEmpty()) {
            System.out.println("Title cannot be empty.");
            return -1;
        }

        // SQL query with placeholders (?) for values
        String sql = "INSERT INTO tasks(title, is_done, created_at) VALUES (?, ?, ?);";

        // Prepare statement and request generated keys (auto-increment id)
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Fill placeholders in the SQL query
            statement.setString(1, title);                         // task title
            statement.setInt(2, 0);                                 // default: not done
            statement.setString(3, OffsetDateTime.now().toString());// current timestamp

            // Execute the insert operation
            statement.executeUpdate();

            // Retrieve generated keys (the auto-increment id)
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                // If a key was returned
                if (generatedKeys.next()) {

                    // Return the new task id
                    return generatedKeys.getInt(1);
                }
            }
        }

        // If no key was returned something went wrong
        throw new SQLException("No ID returned after task insert.");
    }

    // Lists all tasks stored in the database
    public void listTasks(Boolean done) throws SQLException {

        // SQL query selecting tasks ordered by newest first
        String sql;

        if (done == null) {
            sql = "SELECT id, title, is_done, created_at FROM tasks ORDER BY id ASC;";
        } else {
            sql = "SELECT id, title, is_done, created_at FROM tasks WHERE is_done = ? ORDER BY id ASC;";
        }

        // Prepare statement
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // Set the parameter if filtering
            if (done != null) {
                statement.setInt(1, done ? 1 : 0);
            }

            // Prepare statement and execute query
            try (ResultSet resultSet = statement.executeQuery()) {

                // Loop through each row returned by the query
                while (resultSet.next()) {

                    // Read values from the current row
                    int id = resultSet.getInt("id");
                    String title = resultSet.getString("title");

                    // Convert integer (0/1) to Boolean
                    Boolean is_done = resultSet.getInt("is_done") == 1;

                    // Read creation timestamp
                    String createdAt = resultSet.getString("created_at");

                    // Print formatted output to the console
                    System.out.printf("[%d] %s | done=%s | created_at=%s%n", id, title, is_done, createdAt);
                }
            }
        }
    }

    // Updates a task's completion status
    public void markDone(int id, Boolean done) throws SQLException {

        // SQL update query
        String sql = "UPDATE tasks SET is_done = ? WHERE id = ?;";

        // Prepare statement
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // Convert Boolean to SQLite integer (1 or 0)
            statement.setInt(1, done ? 1 : 0);

            // Specify which task to update
            statement.setInt(2, id);

            // Execute the update
            statement.executeUpdate();
        }
    }

    // Prints a specific task based on its id
    public void printTaskById(int id) throws SQLException {

        // SQL query to find task by id
        String sql = "SELECT id, title, is_done, created_at FROM tasks WHERE id = ?;";

        // Prepare statement
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // Fill placeholder with task id
            statement.setInt(1, id);

            // Execute query
            try (ResultSet resultSet = statement.executeQuery()) {

                // If a row was found
                if (resultSet.next()) {

                    // Print task details
                    System.out.printf("[%d] %s | done=%s | created_at=%s%n",
                            resultSet.getInt("id"),
                            resultSet.getString("title"),
                            resultSet.getInt("is_done") == 1,
                            resultSet.getString("created_at"));
                } else {

                    // If no task with this id exists
                    System.out.println("Task not found for id=" + id);
                }
            }
        }
    }

    // Searches tasks whose title contains the given keyword
    public void searchTasks(String keyword) throws SQLException {

        // SQL query using LIKE for partial matching
        String sql = """
        SELECT id, title, is_done, created_at
        FROM tasks
        WHERE title LIKE ?
        ORDER BY id ASC;
        """;

        // Prepare statement
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // Add wildcards before and after keyword
            statement.setString(1, "%" + keyword + "%");

            // Execute query
            try (ResultSet resultSet = statement.executeQuery()) {

                // Iterate through results
                while (resultSet.next()) {

                    int id = resultSet.getInt("id");
                    String title = resultSet.getString("title");
                    Boolean isDone = resultSet.getInt("is_done") == 1;
                    String createdAt = resultSet.getString("created_at");

                    System.out.printf("[%d] %s | done=%s | created_at=%s%n", id, title, isDone, createdAt);

                }
            }
        }
    }

    // Renames a task
    public void renameTask(int id, String newTitle) throws SQLException {

        // SQL update statement
        String sql = "UPDATE tasks SET title = ? WHERE id = ?;";

        // Prepare statement
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // Set new title
            statement.setString(1, newTitle);

            // Set task id
            statement.setInt(2, id);

            // Execute update
            statement.executeUpdate();
        }
    }

    // Deletes a task from the database
    public void deleteTask(int id) throws SQLException {

        // SQL delete command
        String sql = "DELETE FROM tasks WHERE id = ?;";

        // Prepare statement
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            // Specify task id
            stmt.setInt(1, id);

            // Execute deletion
            stmt.executeUpdate();
        }
    }

    // Automatically called when the repository is closed
    @Override
    public void close() throws SQLException {

        // Close the database connection
        connection.close();
    }
}