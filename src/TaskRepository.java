// Import JDBC classes used to talk to the database
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


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

    //Map the SQL table rows into a Task object
    private Task mapRow(ResultSet rs) throws SQLException {
        return new Task(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getInt("is_done") ==1,
                rs.getString("created_at")
        );
    }

    // Creates the tasks table if it does not already exist
    public void createTable() throws SQLException {

        // SQL statement defining the table structure
        String sql = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,  -- unique task id
                title TEXT NOT NULL CHECK (trim(title) <> ''),                   -- task title
                is_done  INTEGER NOT NULL CHECK (is_done IN (0, 1)) DEFAULT 0,    -- completion flag (0=false, 1=true)
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
    public Task insertTask(String title) throws SQLException {

        // Handle empty titles
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }

        //Get timestamp and convert to a string
        String createdAt = OffsetDateTime.now().toString();

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
            try (ResultSet keys = statement.getGeneratedKeys()) {

                // If a key was returned
                if (keys.next()) {

                    int id = keys.getInt(1);

                    // Return a new task
                    return new Task(id, title, false, createdAt);
                }
            }
        }

        // If no key was returned something went wrong
        throw new SQLException("No ID returned after task insert.");
    }

    // Lists all tasks stored in the database
    public List<Task> listTasks(Boolean done) throws SQLException {

        //Create empty list
        List<Task> tasks = new ArrayList<>();


        // Define the SQL query
        String sql;
        if (done == null) {
            sql = "SELECT id, title, is_done, created_at FROM tasks ORDER BY id ASC;";
        }
        else {
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
                    tasks.add(mapRow(resultSet));
                }
            }
        }
        return tasks;
    }

    // Updates a task's completion status
    public Task markDone(int id, Boolean done) throws SQLException {

        // SQL update query
        String sql = "UPDATE tasks SET is_done = ? WHERE id = ?;";

        // Prepare statement
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // Convert Boolean to SQLite integer (1 or 0)
            statement.setInt(1, done ? 1 : 0);

            // Specify which task to update
            statement.setInt(2, id);

            // Number of rows affected
            int rowsUpdated = statement.executeUpdate();

            // Handle non existent ids
            if (rowsUpdated == 0) {
                throw new SQLException("No task found with id=" + id);
            }
        }

        // Return the updated Task
        return getTaskById(id);
    }

    // Prints a specific task based on its id
    public Task getTaskById(int id) throws SQLException {

        // SQL query to find task by id
        String sql = "SELECT id, title, is_done, created_at FROM tasks WHERE id = ?;";

        // Prepare statement
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // Fill placeholder with task id
            statement.setInt(1, id);

            // Execute query
            try (ResultSet resultSet = statement.executeQuery()) {

                // If a row was found, return the task
                if (resultSet.next()) {

                    return mapRow(resultSet);
                }

                // If no task with this id exists
                throw new SQLException("Task not found for id=" + id);
                }
            }
        }

    // Searches tasks whose title contains the given keyword
    public List<Task> searchTasks(String keyword) throws SQLException {

        //Create empty list
        List<Task> tasks = new ArrayList<>();


        // Define the SQL query
        String sql = """
        SELECT * FROM tasks
        WHERE title LIKE ?
        ORDER BY id ASC
        """;

        // Prepare statement
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            // Add wildcards before and after keyword for partial matching
            statement.setString(1, "%" + keyword + "%");

            // Prepare statement and execute query
            try (ResultSet resultSet = statement.executeQuery()) {

                // Loop through each row returned by the query
                while (resultSet.next()) {

                    // Read values from the current row
                    tasks.add(mapRow(resultSet));
                }
            }
        }
        return tasks;
    }

    // Renames a task
    public Task renameTask(int id, String newTitle) throws SQLException {

    // SQL update query
        String sql = "UPDATE tasks SET title = ? WHERE id = ?;";

        // Prepare statement
        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            //Set new title
            statement.setString(1, newTitle);

            // Specify which task to update
            statement.setInt(2, id);

            // Number of rows affected
            int rowsUpdated = statement.executeUpdate();

            // Handle non existent ids
            if (rowsUpdated == 0) {
                throw new SQLException("No task found with id=" + id);
            }
        }

        // Return the updated Task
        return getTaskById(id);
    }

    // Deletes a task from the database
    public Task popTask(int id) throws SQLException {

        //fetch the task to delete and return
        Task taskToDelete = getTaskById(id);

        // SQL delete command
        String sql = "DELETE FROM tasks WHERE id = ?;";

        // Prepare statement
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            // Specify task id
            stmt.setInt(1, id);

            // Execute deletion
            stmt.executeUpdate();
        }

        return taskToDelete;
    }

    // Automatically called when the repository is closed
    @Override
    public void close() throws SQLException {

        // Close the database connection
        connection.close();
    }
}