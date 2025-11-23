package Todo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;

public class TaskDialog extends JDialog {
    private JTextField titleField;
    private JTextArea descArea;
    private JTextField dateField;
    private Task task;

    public TaskDialog(JFrame parent, Task task) {
        super(parent, true);
        setTitle(task == null ? "Add Task" : "Edit Task");
        this.task = task;
        setSize(500, 400);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(new Color(173, 216, 230)); // bluish bg
        setLayout(new BorderLayout(10, 10));

        // Panel for fields
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(173, 216, 230));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Title
        panel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        titleField = new JTextField(task != null ? task.getTitle() : "");
        panel.add(titleField, gbc);

        // Description
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1;
        descArea = new JTextArea(task != null ? task.getDescription() : "", 5, 20);
        JScrollPane scroll = new JScrollPane(descArea);
        panel.add(scroll, gbc);

        // Date
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        dateField = new JTextField(task != null ? task.getDate() : LocalDate.now().toString());
        panel.add(dateField, gbc);

        // Save Button
        JButton saveBtn = UIComponents.createBlueButton("Save");
        saveBtn.addActionListener(e -> {
            if (titleField.getText().isEmpty()) {
                Notification.show("Title cannot be empty!");
                return;
            }
            if (task != null) { // Editing existing task
                task.setTitle(titleField.getText());
                task.setDescription(descArea.getText());
                task.setDate(dateField.getText());
            } else { // Adding new task
                this.task = new Task(titleField.getText(), descArea.getText());
            }
            dispose(); // Close dialog immediately
        });

        add(panel, BorderLayout.CENTER);
        JPanel bottom = new JPanel();
        bottom.add(saveBtn);
        bottom.setBackground(new Color(173, 216, 230));
        add(bottom, BorderLayout.SOUTH);

        // Handle X button to close dialog and return to home page
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose(); // just close the dialog
            }
        });
    }

    public Task getTask() {
        return task;
    }
}
