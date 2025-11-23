package Todo;

import javax.swing.*;

public class Notification {
    public static void show(String message) {
        JOptionPane.showMessageDialog(null, message, "Notification", JOptionPane.INFORMATION_MESSAGE);
    }
}
