
public class Task {

    // Unique identifier of the task (cannot be changed after creation)
    private final int id;

    // Title of the task (immutable reference)
    private final String title;

    // Whether the task is completed (true/false)
    private final Boolean done;

    // Timestamp when the task was created (stored as String)
    private final String createdAt;

    // Constructor: used to create a new Task object with all fields
    public Task(int id, String title, Boolean done, String createdAt) {

        // Assign the provided variable to this object's relevant field
        this.id = id;
        this.title = title;
        this.done = done;
        this.createdAt = createdAt;
    }

    // Getter method to retrieve the task fields
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isDone() {
        return done;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // Override toString() to control how the object prints in console/logs
    @Override
    public String toString() {

        // Format output like:
        // [1] Buy milk | done=false | created_at=2026-03-18T10:00:00Z
        return String.format("[%d] %s | done=%s | created_at=%s",
                id,        // %d → integer id
                title,     // %s → task title
                done,      // %s → boolean (true/false)
                createdAt  // %s → timestamp
        );
    }
}
