package Todo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class TaskListPanel extends JPanel {
    private TaskStore store;
    private JTable table;
    private TaskTableModel model;

    public TaskListPanel(TaskStore store) {
        this.store = store;
        setLayout(new BorderLayout());

        model = new TaskTableModel(store.getTasks());
        table = new JTable(model);

        table.setRowHeight(35);
        table.setFillsViewportHeight(true);

        // Alternating row colors & tooltips
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table,
                                                           Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) c.setBackground(row % 2 == 0 ? new Color(240, 248, 255) : Color.WHITE);
                else c.setBackground(new Color(200, 230, 255));
                if (value != null) setToolTipText(value.toString());
                return c;
            }
        });

        // Delete button column
        if (table.getColumnCount() > 3) {
            table.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
            table.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox(), store, this));
        }

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public JTable getTable() { return table; }

    public void refresh(TaskStore store) {
        this.store = store;
        model.setTasks(store.getTasks());
        model.fireTableDataChanged();
    }

    // Table Model
    private static class TaskTableModel extends AbstractTableModel {
        private String[] cols = {"Title", "Description", "Date", "Delete"};
        private List<Task> tasks;

        public TaskTableModel(List<Task> tasks) { this.tasks = tasks; }

        public void setTasks(List<Task> tasks) { this.tasks = tasks; }

        public int getColumnCount() { return cols.length; }
        public int getRowCount() { return tasks != null ? tasks.size() : 0; }
        public String getColumnName(int col) { return cols[col]; }

        public Object getValueAt(int row, int col) {
            Task t = tasks.get(row);
            switch (col) {
                case 0: return t.getTitle();
                case 1: return t.getDescription();
                case 2: return t.getCreatedAt();
                case 3: return "Delete";
                default: return "";
            }
        }

        public boolean isCellEditable(int row, int col) { return col == 3; }
    }

    // Delete Button Renderer
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText((value == null) ? "" : value.toString());
            setBackground(new Color(220, 60, 60));
            setForeground(Color.WHITE);
            return this;
        }
    }

    // Delete Button Editor
    private static class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private boolean clicked;
        private TaskStore store;
        private TaskListPanel panel;
        private int row;

        public ButtonEditor(JCheckBox checkBox, TaskStore store, TaskListPanel panel) {
            super(checkBox);
            this.store = store;
            this.panel = panel;
            button = new JButton();
            button.setOpaque(true);
            button.setBackground(new Color(220, 60, 60));
            button.setForeground(Color.WHITE);

            button.addActionListener((ActionEvent e) -> {
    if (clicked) {
        if (row >= 0 && row < store.getTasks().size()) {
            Task task = store.getTasks().get(row);
            store.removeTask(task); // remove from list & file
            SwingUtilities.invokeLater(() -> {
                panel.refresh(store);   // refresh table so button disappears
            });
            Notification.show("Task deleted!");
        }
    }
    clicked = false;
});

        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            this.row = row;
            button.setText((value == null) ? "" : value.toString());
            clicked = true;
            return button;
        }

        public Object getCellEditorValue() { return ""; }

        public boolean stopCellEditing() {
            clicked = false;
            return super.stopCellEditing();
        }

        protected void fireEditingStopped() { super.fireEditingStopped(); }
    }

    public Task getSelectedTask() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSelectedTask'");
    }
}
