package Todo;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Task implements Serializable {
    private String title;
    private String description;
    private boolean completed;
    private LocalDateTime createdAt;
    private String date; // user-entered date

    public Task(String title, String description) {
        this.title = title;
        this.description = description;
        this.completed = false;
        this.createdAt = LocalDateTime.now();
        this.date = "";
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
