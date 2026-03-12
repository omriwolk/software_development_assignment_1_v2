import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;

public class TaskRepository implements AutoCloseable {
    private static final String DB_URL = "jdbc:sqlite:app.db";

    private final Connection connection;

    public TaskRepository() throws SQLException {
        this.connection = DriverManager.getConnection(DB_URL);
    }

    public void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                is_done INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL
            );
            """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public int insertTask(String title) throws SQLException {
        String sql = "INSERT INTO tasks(title, is_done, created_at) VALUES (?, ?, ?);";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, title);
            statement.setInt(2, 0);
            statement.setString(3, OffsetDateTime.now().toString());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("No ID returned after task insert.");
    }

    public void listTasks() throws SQLException {
        String sql = "SELECT id, title, is_done, created_at FROM tasks ORDER BY id DESC;";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String title = resultSet.getString("title");
                boolean done = resultSet.getInt("is_done") == 1;
                String createdAt = resultSet.getString("created_at");

                System.out.printf("[%d] %s | done=%s | created_at=%s%n", id, title, done, createdAt);
            }
        }
    }

    public void markDone(int id, boolean done) throws SQLException {
        String sql = "UPDATE tasks SET is_done = ? WHERE id = ?;";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, done ? 1 : 0);
            statement.setInt(2, id);
            statement.executeUpdate();
        }
    }

    public void printTaskById(int id) throws SQLException {
        String sql = "SELECT id, title, is_done, created_at FROM tasks WHERE id = ?;";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    System.out.printf("[%d] %s | done=%s | created_at=%s%n",
                            resultSet.getInt("id"),
                            resultSet.getString("title"),
                            resultSet.getInt("is_done") == 1,
                            resultSet.getString("created_at"));
                } else {
                    System.out.println("Task not found for id=" + id);
                }
            }
        }
    }

    public void renameTask(int id, String newTitle) throws SQLException {
        String sql = "UPDATE tasks SET title = ? WHERE id = ?;";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, newTitle);
            statement.setInt(2, id);
            statement.executeUpdate();
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}