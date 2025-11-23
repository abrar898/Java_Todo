package Todo;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class TodoApp extends JFrame {

    private TaskStore store;
    private TaskListPanel taskPanel;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    public TodoApp() {
        super("Professional To-Do App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationByPlatform(true);

        store = new TaskStore(); // Load tasks from file

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        add(mainPanel);

        // Create pages
        JPanel homePage = createHomePage();
        JPanel taskPage = createTaskPanel();

        mainPanel.add(homePage, "home");
        mainPanel.add(taskPage, "tasks");

        cardLayout.show(mainPanel, "home"); // show home page

        setVisible(true);
    }

    // Home Page
    private JPanel createHomePage() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(220, 240, 255));

        JLabel header = new JLabel("Welcome to Your To-Do App");
        header.setFont(new Font("Segoe UI", Font.BOLD, 36));
        header.setForeground(new Color(33, 90, 180));

        JButton addTaskBtn = UIComponents.createBlueButton("Add Task");
        JButton viewTasksBtn = UIComponents.createBlueButton("View Tasks");

        addHoverEffect(addTaskBtn);
        addHoverEffect(viewTasksBtn);

        // Add Task action
        addTaskBtn.addActionListener(e -> {
            TaskDialog dialog = new TaskDialog(this, null);
            dialog.setVisible(true);
            Task task = dialog.getTask();
            if (task != null) {
                store.addTask(task);           // save to file immediately
                taskPanel.refresh(store);      // refresh table
                Notification.show("Task added!");
            }
        });

        // View Tasks action
        viewTasksBtn.addActionListener(e -> {
            taskPanel.refresh(store);          // refresh tasks from file
            cardLayout.show(mainPanel, "tasks");
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(header, gbc);
        gbc.gridy = 1; panel.add(addTaskBtn, gbc);
        gbc.gridy = 2; panel.add(viewTasksBtn, gbc);

        return panel;
    }

    // Task Panel with Delete column
    private JPanel createTaskPanel() {
        taskPanel = new TaskListPanel(store);

        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(new Color(245, 250, 255));

        JButton backBtn = UIComponents.createBlueButton("← Back");
        backBtn.setPreferredSize(new Dimension(120, 40));
        addHoverEffect(backBtn);
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "home"));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(new Color(245, 250, 255));
        topPanel.add(backBtn);

        container.add(topPanel, BorderLayout.NORTH);
        container.add(taskPanel, BorderLayout.CENTER);

        // Table enhancements
        JTable table = taskPanel.getTable();
        table.setRowHeight(35);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) c.setBackground(row % 2 == 0 ? new Color(240,248,255) : Color.WHITE);
                else c.setBackground(new Color(200,230,255));
                if (value != null) setToolTipText(value.toString());
                return c;
            }
        });

        return container;
    }

    // Hover effect
    private void addHoverEffect(JButton button) {
        Color original = button.getBackground();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { button.setBackground(original.darker()); }
            public void mouseExited(java.awt.event.MouseEvent evt) { button.setBackground(original); }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TodoApp::new);
    }
}
