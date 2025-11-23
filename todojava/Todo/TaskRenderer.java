package Todo;

import javax.swing.*;
import java.awt.*;

public class TaskRenderer extends JPanel implements ListCellRenderer<Task> {
    private JLabel titleLabel = new JLabel();
    private JLabel descLabel = new JLabel();

    public TaskRenderer() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(Color.BLACK);
        descLabel.setForeground(Color.DARK_GRAY);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Task> list, Task task, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        removeAll();
        JPanel card = UIComponents.createCardPanel();
        card.setBackground(isSelected ? new Color(200, 230, 255) : new Color(245, 245, 245)); // Light gray card
        card.setLayout(new BorderLayout());
        titleLabel.setText(task.getTitle());
        descLabel.setText("<html><body style='width: 300px'>" + task.getDescription() + "</body></html>");
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(descLabel, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(card, BorderLayout.CENTER);
        setBackground(Color.WHITE);

        return this;
    }
}
