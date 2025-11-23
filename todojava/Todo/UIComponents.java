package Todo;

import javax.swing.*;
import java.awt.*;

public class UIComponents {

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(245, 245, 245)); // Light gray card
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BorderLayout());
        panel.setOpaque(true);
        panel.setPreferredSize(new Dimension(400, 80));
        return panel;
    }

    public static JButton createBlueButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(33, 150, 243)); // Bluish color
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(30, 120, 220));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(33, 150, 243));
            }
        });
        return button;
    }

    public static JLabel createTitleLabel(String text, int fontSize) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        label.setForeground(Color.WHITE);
        return label;
    }

    public static JPanel createHeader(String title) {
        JPanel header = new JPanel();
        header.setBackground(new Color(33, 150, 243)); // Blue header
        header.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        header.setLayout(new BorderLayout());
        JLabel label = createTitleLabel(title, 28);
        header.add(label, BorderLayout.CENTER);
        return header;
    }
}
