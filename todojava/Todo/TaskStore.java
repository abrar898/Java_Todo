// package Todo;

// import java.io.*;
// import java.util.ArrayList;
// import java.util.List;

// public class TaskStore implements Serializable {
//     private List<Task> tasks;
//     private static final String FILE_NAME = "tasks.dat";

//     public TaskStore() {
//         tasks = loadTasks(); // load saved tasks at startup
//     }

//     public List<Task> getTasks() {
//         return tasks;
//     }

//     public void addTask(Task task) {
//         tasks.add(task);
//         saveTasks();
//     }

//     public void removeTask(Task task) {
//         tasks.remove(task);
//         saveTasks();
//     }

//     // Save tasks to file
//     private void saveTasks() {
//         try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
//             oos.writeObject(tasks);
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }

//     // Load tasks from file
//     @SuppressWarnings("unchecked")
//     private List<Task> loadTasks() {
//         File file = new File(FILE_NAME);
//         if (!file.exists()) return new ArrayList<>();
//         try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
//             return (List<Task>) ois.readObject();
//         } catch (IOException | ClassNotFoundException e) {
//             e.printStackTrace();
//             return new ArrayList<>();
//         }
//     }
// }
package Todo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TaskStore {
    private List<Task> tasks;
    private final File file = new File("tasks.dat");

    public TaskStore() {
        tasks = loadTasks();
    }

    public List<Task> getTasks() { return tasks; }

    public void addTask(Task task) {
        tasks.add(task);
        saveTasks();
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        saveTasks(); // persist changes
    }

    private List<Task> loadTasks() {
        if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Task>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void saveTasks() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(tasks);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
