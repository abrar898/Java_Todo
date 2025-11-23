import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * TodoApp.java
 *
 * Features:
 *  - Left sidebar (YouTube-like) with nav buttons and dynamic categories
 *  - Add/Edit/Delete categories (CRUD)
 *  - Add/Edit/Delete tasks (CRUD)
 *  - Priority colors (High/Medium/Low)
 *  - Search and filter (All / Active / Completed)
 *  - Pie chart (Completed vs Pending) per selected category & priority
 *  - Nice modern UI (rounded panels, subtle shadow, Segoe UI)
 *
 * Compile: javac TodoApp.java
 * Run:     java TodoApp
 */
public class TodoApp extends JFrame {
    // ---- Model ----
    static class Task implements Serializable {
        String title;
        String notes;
        LocalDate due; // nullable
        boolean done;
        String category;      // e.g. "Work", "Personal"
        String priority;      // "High","Medium","Low"
        long createdAt = System.currentTimeMillis();

        Task(String title, String notes, LocalDate due, String category, String priority) {
            this.title = title == null ? "" : title.trim();
            this.notes = notes == null ? "" : notes.trim();
            this.due = due;
            this.category = (category == null || category.isBlank()) ? "General" : category.trim();
            this.priority = (priority == null || priority.isBlank()) ? "Medium" : priority;
            this.done = false;
        }
    }

    // Persistence path
    private static final Path SAVE_PATH = Paths.get(System.getProperty("user.home"), ".swing-todo.dat");

    // UI State
    private final DefaultListModel<Task> masterModel = new DefaultListModel<>();
    private final DefaultListModel<Task> viewModel = new DefaultListModel<>();
    private final JList<Task> list = new JList<>(viewModel);

    // Category model & UI
    private final DefaultListModel<String> categoryModel = new DefaultListModel<>();
    private JList<String> categoryList;

    // Controls
    private final JTextField addField = new JTextField();
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> filterBox = new JComboBox<>(new String[]{"All", "Active", "Completed"});
    private final JButton addBtn = new JButton("Add");
    private final JButton editBtn = new JButton("Edit");
    private final JButton delBtn = new JButton("Delete");
    private final JCheckBox showDueTodayFirst = new JCheckBox("Prioritize today");
    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, d MMM");

    // Chart panel
    private final ChartPanel chartPanel = new ChartPanel();

    public TodoApp() {
        super("To-Do — Clean");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 640));
        setLocationByPlatform(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        installNimbus();
        setIconImage(makeAppIcon());
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 14));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(248, 250, 253));
        setContentPane(root);

        // LEFT SIDEBAR (YouTube-like)
        JPanel leftBar = buildLeftSidebar();
        root.add(leftBar, BorderLayout.WEST);

        // HEADER
        JPanel header = buildHeader();
        root.add(header, BorderLayout.NORTH);

        // CENTER: tasks list area
        JPanel center = buildCenterArea();
        root.add(center, BorderLayout.CENTER);

        // RIGHT: chart & quick controls
        JPanel right = buildRightPanel();
        root.add(right, BorderLayout.EAST);

        // Footer tip
        JLabel tip = new JLabel("Tip: Double-click a task to edit · Drag to reorder · Delete key removes selected");
        tip.setHorizontalAlignment(SwingConstants.CENTER);
        tip.setBorder(new EmptyBorder(8, 12, 12, 12));
        tip.setForeground(new Color(100, 110, 130));
        root.add(tip, BorderLayout.SOUTH);

        // Interactions wiring
        addBtn.addActionListener(e -> onAdd());
        addField.addActionListener(e -> onAdd());
        editBtn.addActionListener(e -> onEdit());
        delBtn.addActionListener(e -> onDelete());

        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int idx = list.locationToIndex(e.getPoint());
                if (idx < 0) return;
                if (e.getClickCount() == 2) { list.setSelectedIndex(idx); onEdit(); return; }
                Rectangle cellBounds = list.getCellBounds(idx, idx);
                Point rel = new Point(e.getX() - cellBounds.x, e.getY() - cellBounds.y);
                TaskCellRenderer r = (TaskCellRenderer) list.getCellRenderer()
                        .getListCellRendererComponent(list, viewModel.get(idx), idx, false, false);
                Rectangle cb = r.checkboxBounds();
                if (cb.contains(rel)) {
                    Task t = viewModel.get(idx);
                    int confirm = JOptionPane.showConfirmDialog(TodoApp.this,
                            t.done ? "Mark this task as not completed?" : "Mark this task as completed?",
                            "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        t.done = !t.done;
                        save();
                        refreshView();
                        updateCategoriesAndChart();
                    }
                } else {
                    list.setSelectedIndex(idx);
                }
            }
        });

        // Drag to reorder
        list.setDragEnabled(true);
        list.setDropMode(DropMode.INSERT);
        list.setTransferHandler(new ReorderHandler());

        // Filtering & search
        filterBox.addActionListener(e -> refreshView());
        showDueTodayFirst.addActionListener(e -> refreshView());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshView(); }
            public void removeUpdate(DocumentEvent e) { refreshView(); }
            public void changedUpdate(DocumentEvent e) { refreshView(); }
        });

        // Category selection
        categoryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshView();
                updateChart();
            }
        });

        // Keyboard delete
        list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "del");
        list.getActionMap().put("del", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { onDelete(); }
        });

        // Load data
        load();
        if (categoryModel.isEmpty()) {
            categoryModel.addElement("All Categories");
            categoryModel.addElement("General");
        }
        // ensure 'All Categories' selected
        if (categoryList.getSelectedIndex() == -1) categoryList.setSelectedIndex(0);

        refreshView();
        updateCategoriesAndChart();
    }

    private void beautifyScrollBar(JScrollPane scroll) {
    scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(180, 180, 200);
            trackColor = new Color(240, 240, 240);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setVisible(false);
            return btn;
        }
    });

    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
}

    private int hoverIndex = -1;

private void enableHoverEffect(JList<String> list) {
    list.addMouseMotionListener(new MouseMotionAdapter() {
        @Override
        public void mouseMoved(MouseEvent e) {
            int index = list.locationToIndex(e.getPoint());
            if (index != hoverIndex) {
                hoverIndex = index;
                list.repaint();
            }
        }
    });

    list.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseExited(MouseEvent e) {
            hoverIndex = -1;
            list.repaint();
        }
    });

    list.setCellRenderer(new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {

            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            lbl.setBorder(new EmptyBorder(8, 10, 8, 10));
            lbl.setOpaque(true);

            if (isSelected) {
                lbl.setBackground(new Color(30, 144, 255));   // Blue
                lbl.setForeground(Color.WHITE);
            } else if (index == hoverIndex) {
                lbl.setBackground(new Color(220, 235, 255));  // Light blue hover
                lbl.setForeground(Color.BLACK);
            } else {
                lbl.setBackground(Color.WHITE);
                lbl.setForeground(Color.BLACK);
            }

            return lbl;
        }
    });
}
private JPanel buildLeftSidebar() {
    JPanel left = new JPanel(new BorderLayout());
    left.setPreferredSize(new Dimension(240, 0));
    left.setBorder(new EmptyBorder(12, 12, 12, 6));
    left.setBackground(new Color(245, 247, 250));

    // Top nav buttons panel
    JPanel nav = new JPanel(new GridLayout(0, 1, 6, 6));
    nav.setOpaque(false);

    // Create buttons with hover effect
    JButton allBtn = makeHoverButton("🏠  All Tasks");
    JButton activeBtn = makeHoverButton("⏳  Active");
    JButton completedBtn = makeHoverButton("✅  Completed");
    JButton chartBtn = makeHoverButton("📊  Chart");
    JButton manageCatBtn = makeHoverButton("⚙️  Manage Categories");

    allBtn.addActionListener(e -> { filterBox.setSelectedItem("All"); categoryList.setSelectedIndex(0); refreshView(); });
    activeBtn.addActionListener(e -> { filterBox.setSelectedItem("Active"); categoryList.setSelectedIndex(0); refreshView(); });
    completedBtn.addActionListener(e -> { filterBox.setSelectedItem("Completed"); categoryList.setSelectedIndex(0); refreshView(); });
    chartBtn.addActionListener(e -> { showChartDialog(); });
    manageCatBtn.addActionListener(e -> { showManageCategoriesDialog(); });

    nav.add(allBtn); 
    nav.add(activeBtn); 
    nav.add(completedBtn); 
    nav.add(chartBtn); 
    nav.add(manageCatBtn);

    left.add(nav, BorderLayout.NORTH);

    // Category section wrap
    JPanel cwrap = new RoundedPanel(12, new Color(255,255,255,230));
    cwrap.setLayout(new BorderLayout(6, 6));
    cwrap.setBorder(new EmptyBorder(12, 12, 12, 12));

    JLabel lbl = new JLabel("Categories");
    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 13f));
    cwrap.add(lbl, BorderLayout.NORTH);

    // Category list
    categoryList = new JList<>(categoryModel);
    categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    categoryList.setVisibleRowCount(8);
    categoryList.setFixedCellHeight(34);
    categoryList.setBorder(new EmptyBorder(6, 6, 6, 6));
    categoryList.setBackground(new Color(255, 255, 255, 0));

    // Hover & selection effect
    enableHoverEffect(categoryList);

    JScrollPane catScroll = new JScrollPane(categoryList);
    catScroll.setBorder(BorderFactory.createEmptyBorder());
    beautifyScrollBar(catScroll);
    cwrap.add(catScroll, BorderLayout.CENTER);

    // Category Buttons
    JPanel catBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
    catBtns.setOpaque(false);
    JButton addCat = new JButton("＋");
    JButton editCat = new JButton("✎");
    JButton delCat = new JButton("🗑");
    addCat.setToolTipText("Add category");
    editCat.setToolTipText("Edit selected category");
    delCat.setToolTipText("Delete selected category");
    catBtns.add(addCat); 
    catBtns.add(editCat); 
    catBtns.add(delCat);
    cwrap.add(catBtns, BorderLayout.SOUTH);

    // Add/Edit/Delete category actions (same as your current code)
    addCat.addActionListener(e -> { /* ... */ });
    editCat.addActionListener(e -> { /* ... */ });
    delCat.addActionListener(e -> { /* ... */ });

    left.add(cwrap, BorderLayout.CENTER);
    return left;
}
private JButton makeHoverButton(String text) {
    JButton btn = new JButton(text);
    btn.setFocusPainted(false);
    btn.setBorderPainted(false);
    btn.setContentAreaFilled(true);
    btn.setOpaque(true);
    btn.setBackground(Color.WHITE);
    btn.setForeground(Color.BLACK);
    btn.setHorizontalAlignment(SwingConstants.LEFT);
    btn.setBorder(new EmptyBorder(8, 12, 8, 12));

    btn.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            btn.setBackground(new Color(30, 144, 255)); // Blue hover
            btn.setForeground(Color.WHITE);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            btn.setBackground(Color.WHITE);
            btn.setForeground(Color.BLACK);
        }
    });
    return btn;
}



    private JButton makeSidebarButton(String text) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8,12,8,12));
        b.setBackground(new Color(255,255,255,0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JPanel buildHeader() {
        JPanel header = new GradientPanel();
        header.setLayout(new GridBagLayout());
        header.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("To-Do");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));

        JLabel subtitle = new JLabel("Organize your day • Clean & modern");
        subtitle.setForeground(new Color(235, 244, 255));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(3));
        left.add(subtitle);

        JPanel addBar = new RoundedPanel(12, new Color(255,255,255,230));
        addBar.setLayout(new BorderLayout(8, 8));
        addBar.setBorder(new EmptyBorder(8, 8, 8, 8));

        addField.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        addField.setOpaque(false);
        addField.setFont(addField.getFont().deriveFont(14f));
        addField.setToolTipText("Add a task (press Enter)");

        addBtn.setFocusPainted(false);
        addBtn.setBorder(new RoundedBorder(12));
        addBtn.setBackground(new Color(60, 120, 250));
        addBtn.setForeground(Color.WHITE);

        addBar.add(addField, BorderLayout.CENTER);
        addBar.add(addBtn, BorderLayout.EAST);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.weightx = 0.6; c.fill = GridBagConstraints.HORIZONTAL;
        header.add(left, c);
        c.gridx = 1; c.weightx = 1; c.insets = new Insets(0, 16, 0, 0); header.add(addBar, c);
        return header;
    }

    private JPanel buildCenterArea() {
        JPanel centerWrap = new RoundedPanel(14, new Color(255,255,255));
        centerWrap.setBorder(new EmptyBorder(14,14,14,14));
        centerWrap.setLayout(new BorderLayout(12,12));
        centerWrap.setOpaque(false);

        JLabel listTitle = new JLabel("Your Tasks");
        listTitle.setFont(listTitle.getFont().deriveFont(Font.BOLD, 16f));
        listTitle.setBorder(new EmptyBorder(0,4,8,0));

        JPanel listTop = new JPanel(new BorderLayout());
        listTop.setOpaque(false);
        listTop.add(listTitle, BorderLayout.WEST);
        listTop.add(buildListToolbar(), BorderLayout.EAST);

        list.setCellRenderer(new TaskCellRenderer());
        list.setFixedCellHeight(-1);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setOpaque(false);

        JScrollPane sp = new JScrollPane(list);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        beautifyScrollBar(sp);

        centerWrap.add(listTop, BorderLayout.NORTH);
        centerWrap.add(sp, BorderLayout.CENTER);

        JPanel container = new JPanel(new GridBagLayout());
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(12,12,12,12));
        container.add(centerWrap, new GridBagConstraints());
        return container;
    }

    private JPanel buildListToolbar() {
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);

        searchField.setColumns(18);
        searchField.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(12), new EmptyBorder(6,10,6,10)));
        searchField.setToolTipText("Search tasks");

        filterBox.setBorder(new RoundedBorder(12));
        filterBox.setToolTipText("Filter tasks");

        showDueTodayFirst.setOpaque(false);

        stylePillButton(editBtn);
        stylePillButton(delBtn);

        tools.add(new JLabel("Search:"));
        tools.add(searchField);
        tools.add(new JLabel("Filter:"));
        tools.add(filterBox);
        tools.add(showDueTodayFirst);
        tools.add(editBtn);
        tools.add(delBtn);
        return tools;
    }

    private JPanel buildRightPanel() {
        JPanel rightOuter = new JPanel(new GridBagLayout());
        rightOuter.setOpaque(false);
        rightOuter.setBorder(new EmptyBorder(12, 6, 12, 12));

        JPanel rightPanel = new RoundedPanel(12, new Color(255,255,255,230));
        rightPanel.setLayout(new BorderLayout(8,8));
        rightPanel.setBorder(new EmptyBorder(12,12,12,12));
        JLabel lbl = new JLabel("Stats");
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 13f));
        rightPanel.add(lbl, BorderLayout.NORTH);

        rightPanel.add(chartPanel, BorderLayout.CENTER);
        rightOuter.add(rightPanel, new GridBagConstraints());
        return rightOuter;
    }

    private void stylePillButton(JButton b) {
        b.setFocusPainted(false);
        b.setBorder(new RoundedBorder(12));
        b.setBackground(new Color(245, 247, 252));
        b.setForeground(new Color(40, 50, 70));
    }

    // ---------------- Actions ----------------

    private void onAdd() {
        String titleText = addField.getText().trim();
        Task base = new Task(titleText,"",null,"General","Medium");
        Task t = showTaskDialog(base, true);
        if (t != null) {
            masterModel.add(0, t);
            save();
            addField.setText("");
            refreshView();
            updateCategoriesAndChart();
        }
    }

    private void onEdit() {
        int idx = list.getSelectedIndex();
        if (idx < 0) return;
        Task original = viewModel.get(idx);
        Task copy = new Task(original.title, original.notes, original.due, original.category, original.priority);
        copy.done = original.done;
        Task edited = showTaskDialog(copy, false);
        if (edited != null) {
            original.title = edited.title;
            original.notes = edited.notes;
            original.due = edited.due;
            original.category = edited.category;
            original.priority = edited.priority;
            original.done = edited.done;
            save();
            refreshView();
            updateCategoriesAndChart();
        }
    }

    private void onDelete() {
        int idx = list.getSelectedIndex();
        if (idx < 0) return;
        Task toRemove = viewModel.get(idx);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected task?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            for (int i = 0; i < masterModel.size(); i++) {
                if (masterModel.get(i) == toRemove) { masterModel.remove(i); break; }
            }
            save();
            refreshView();
            updateCategoriesAndChart();
        }
    }

    private void refreshView() {
        viewModel.clear();
        String q = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String filter = (String) filterBox.getSelectedItem();

        List<Task> items = Collections.list(masterModel.elements());
        items.sort((a,b) -> {
            int pToday = Boolean.compare(isToday(b.due), isToday(a.due));
            if (showDueTodayFirst.isSelected() && pToday != 0) return pToday;
            int pDone = Boolean.compare(a.done, b.done);
            if (pDone != 0) return pDone;
            return Long.compare(b.createdAt, a.createdAt);
        });

        String selectedCategory = "All Categories";
        if (categoryList.getSelectedValue() != null) selectedCategory = categoryList.getSelectedValue();

        for (Task t : items) {
            if (filter.equals("Active") && t.done) continue;
            if (filter.equals("Completed") && !t.done) continue;
            if (!q.isEmpty()) {
                String blob = (t.title + "\n" + t.notes + "\n" + t.category).toLowerCase(Locale.ROOT);
                if (!blob.contains(q)) continue;
            }
            if (!selectedCategory.equals("All Categories") && !t.category.equals(selectedCategory)) continue;
            viewModel.addElement(t);
        }
        list.repaint();
    }

    private boolean isToday(LocalDate d) {
        return d != null && d.equals(LocalDate.now());
    }

    private Task showTaskDialog(Task task, boolean isNew) {
        JTextField title = new JTextField(task.title, 28);
        JTextArea notes = new JTextArea(task.notes, 4, 28);
        JTextField due = new JTextField(task.due == null ? "" : task.due.toString(), 12);

        // category dropdown from categoryModel
        JComboBox<String> categoryBox = new JComboBox<>();
        for (int i = 0; i < categoryModel.size(); i++) categoryBox.addItem(categoryModel.get(i));
        if (categoryBox.getItemCount() == 0) categoryBox.addItem("General");
        categoryBox.setSelectedItem(task.category == null ? "General" : task.category);

        JComboBox<String> priorityBox = new JComboBox<>(new String[]{"High","Medium","Low"});
        priorityBox.setSelectedItem(task.priority == null ? "Medium" : task.priority);

        JCheckBox done = new JCheckBox("Completed", task.done);

        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(8,8,0,8));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; panel.add(new JLabel("Title"), c);
        c.gridx = 1; c.gridy = 0; panel.add(title, c);

        c.gridx = 0; c.gridy = 1; panel.add(new JLabel("Notes"), c);
        c.gridx = 1; c.gridy = 1; c.fill = GridBagConstraints.BOTH; c.weightx = 1; c.weighty = 1;
        panel.add(new JScrollPane(notes), c);

        c.fill = GridBagConstraints.HORIZONTAL; c.weighty = 0;
        c.gridx = 0; c.gridy = 2; panel.add(new JLabel("Due (YYYY-MM-DD)"), c);
        c.gridx = 1; c.gridy = 2; panel.add(due, c);

        c.gridx = 0; c.gridy = 3; panel.add(new JLabel("Category"), c);
        c.gridx = 1; c.gridy = 3; panel.add(categoryBox, c);

        c.gridx = 0; c.gridy = 4; panel.add(new JLabel("Priority"), c);
        c.gridx = 1; c.gridy = 4; panel.add(priorityBox, c);

        c.gridx = 1; c.gridy = 5; panel.add(done, c);

        int res = JOptionPane.showConfirmDialog(this, panel, isNew ? "Add Task" : "Edit Task", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String newTitle = title.getText().trim();
            if (newTitle.isEmpty()) return null;
            LocalDate parsedDue = null;
            if (!due.getText().trim().isEmpty()) {
                try { parsedDue = LocalDate.parse(due.getText().trim()); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid date. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            }
            String cat = categoryBox.getSelectedItem() == null ? "General" : categoryBox.getSelectedItem().toString();
            String pr = (String) priorityBox.getSelectedItem();
            Task out = new Task(newTitle, notes.getText(), parsedDue, cat, pr);
            out.done = done.isSelected();
            return out;
        }
        return null;
    }

    // ---------------- Persistence ----------------
    private void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(SAVE_PATH))) {
            List<Task> data = Collections.list(masterModel.elements());
            oos.writeObject(new ArrayList<>(data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(SAVE_PATH)) return;
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(SAVE_PATH))) {
            List<Task> data = (List<Task>) ois.readObject();
            masterModel.clear();
            for (Task t : data) masterModel.addElement(t);
            // build categories from loaded tasks
            Set<String> cats = new TreeSet<>();
            cats.add("All Categories");
            for (int i = 0; i < masterModel.size(); i++) cats.add(masterModel.get(i).category);
            categoryModel.clear();
            for (String c : cats) categoryModel.addElement(c);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------- Renderers & Drag ----------------

    private class TaskCellRenderer extends JPanel implements ListCellRenderer<Task> {
        private final JCheckBox cb = new JCheckBox();
        private final JLabel title = new JLabel();
        private final JLabel notes = new JLabel();
        private final JLabel due = new JLabel();
        private final JLabel meta = new JLabel(); // shows category / priority
        private final JPanel badge = new JPanel();

        private final int pad = 12;

        TaskCellRenderer() {
            setLayout(new GridBagLayout());
            setOpaque(false);
            setBorder(new EmptyBorder(6, 8, 6, 8));
            cb.setOpaque(false);

            title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
            notes.setFont(notes.getFont().deriveFont(12f));
            notes.setForeground(new Color(90, 100, 120));

            due.setFont(due.getFont().deriveFont(Font.BOLD, 11f));
            due.setBorder(BorderFactory.createEmptyBorder(2,8,2,8));
            due.setOpaque(true);
            due.setBackground(new Color(238, 245, 255));
            due.setForeground(new Color(60, 120, 250));

            meta.setFont(meta.getFont().deriveFont(11f));
            meta.setForeground(new Color(120, 120, 120));

            badge.setPreferredSize(new Dimension(10, 10));
            badge.setOpaque(true);
        }

        public Rectangle checkboxBounds() {
            return new Rectangle(12, 18, 22, 22);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Task> lst, Task task, int index, boolean isSelected, boolean cellHasFocus) {
            removeAll();

            JPanel card = new RoundedPanel(12, Color.WHITE);
            card.setLayout(new GridBagLayout());
            card.setBorder(new EmptyBorder(pad, pad, pad, pad));

            cb.setSelected(task.done);

            String ttl = task.title;
            if (task.done) ttl = "<html><strike>" + esc(ttl) + "</strike></html>";
            else ttl = esc(ttl);
            title.setText(ttl);

            if (task.notes != null && !task.notes.isBlank()) {
                notes.setText("<html><div style='width:420px;'>" + esc(task.notes) + "</div></html>");
            } else {
                notes.setText("");
            }

            if (task.due != null) {
                LocalDate d = task.due;
                String label = d.equals(LocalDate.now()) ? "Today" : DATE_FMT.format(d);
                due.setText(label);
                due.setVisible(true);
            } else {
                due.setVisible(false);
            }

            String metaText = task.category + " · " + task.priority;
            meta.setText(metaText);

            // priority color
            Color pColor = switch (task.priority) {
                case "High" -> new Color(255, 220, 220);
                case "Medium" -> new Color(255, 250, 220);
                default -> new Color(230, 255, 230);
            };
            Color dot = switch (task.priority) {
                case "High" -> new Color(220, 60, 60);
                case "Medium" -> new Color(210, 150, 30);
                default -> new Color(60, 160, 60);
            };
            badge.setBackground(dot);
            card.setBackground(pColor);

            if (task.done) {
                title.setForeground(new Color(140, 150, 165));
            } else {
                title.setForeground(new Color(30, 35, 45));
            }

            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(2, 2, 2, 2);

            c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.NORTHWEST; card.add(cb, c);

            c.gridx = 1; c.gridy = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; card.add(title, c);

            c.gridx = 2; c.gridy = 0; c.anchor = GridBagConstraints.NORTHEAST; card.add(due, c);

            c.gridx = 1; c.gridy = 1; c.weightx = 1; c.insets = new Insets(2, 2, 0, 2); card.add(notes, c);

            c.gridx = 1; c.gridy = 2; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(6,2,0,2);
            JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            metaRow.setOpaque(false);
            metaRow.add(badge);
            metaRow.add(meta);
            card.add(metaRow, c);

            if (isSelected) {
                card.setBackground(new Color(245, 248, 255));
            }

            setLayout(new BorderLayout());
            add(card, BorderLayout.CENTER);
            return this;
        }

        private String esc(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        }
    }

    // Drag & drop reorder between indices of viewModel -> reflect into masterModel
    private class ReorderHandler extends TransferHandler {
        private int fromIndex = -1;

        @Override public int getSourceActions(JComponent c) { return MOVE; }

        @Override protected Transferable createTransferable(JComponent c) {
            fromIndex = list.getSelectedIndex();
            return new StringSelection("item");
        }

        @Override public boolean canImport(TransferHandler.TransferSupport info) {
            return info.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override public boolean importData(TransferSupport support) {
            if (!support.isDrop()) return false;
            int toIndex = ((JList.DropLocation) support.getDropLocation()).getIndex();
            if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return false;
            Task moving = viewModel.get(fromIndex);

            int mFrom = indexOfRef(masterModel, moving);
            if (mFrom < 0) return false;
            masterModel.remove(mFrom);

            int masterInsertIndex = masterModel.size();
            if (toIndex < viewModel.size()) {
                Task anchor = viewModel.get(toIndex);
                masterInsertIndex = indexOfRef(masterModel, anchor);
            }
            if (masterInsertIndex < 0) masterInsertIndex = masterModel.size();

            masterModel.add(masterInsertIndex, moving);
            save();
            refreshView();
            updateCategoriesAndChart();
            return true;
        }

        private int indexOfRef(DefaultListModel<Task> model, Task ref) {
            for (int i = 0; i < model.size(); i++) if (model.get(i) == ref) return i;
            return -1;
        }
    }

    // ---------------- Chart Panel ----------------

    private class ChartPanel extends JPanel {
        private String priorityFilter = "All"; // All / High / Medium / Low
        private JComboBox<String> priorityCombo;

        ChartPanel() {
            setPreferredSize(new Dimension(260, 260));
            setLayout(new BorderLayout(6,6));
            setOpaque(false);

            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            JLabel lbl = new JLabel("Completion Chart");
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 13f));
            head.add(lbl, BorderLayout.WEST);

            priorityCombo = new JComboBox<>(new String[]{"All","High","Medium","Low"});
            priorityCombo.setSelectedIndex(0);
            priorityCombo.addActionListener(e -> { priorityFilter = (String) priorityCombo.getSelectedItem(); updateChartData(); });
            head.add(priorityCombo, BorderLayout.EAST);

            add(head, BorderLayout.NORTH);
        }

        void updateChartData() { repaint(); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            String selCat = "All Categories";
            if (categoryList.getSelectedValue() != null) selCat = categoryList.getSelectedValue();

            int completed = 0, notCompleted = 0;
            for (int i = 0; i < masterModel.size(); i++) {
                Task t = masterModel.get(i);
                if (!selCat.equals("All Categories") && !t.category.equals(selCat)) continue;
                if (!priorityFilter.equals("All") && !t.priority.equals(priorityFilter)) continue;
                if (t.done) completed++; else notCompleted++;
            }
            int total = completed + notCompleted;
            int w = getWidth(), h = getHeight();
            if (total == 0) {
                g2.setColor(new Color(240,240,245));
                g2.fillRoundRect(6, 36, w-12, h-48, 12, 12);
                g2.setColor(new Color(110,110,120));
                g2.drawString("No tasks in selection", 20, h/2);
                g2.dispose();
                return;
            }

            int size = Math.min(w, h-40) - 40;
            int x = 20, y = 36, arcW = size, arcH = size;
            double completedAngle = (double) completed / total * 360.0;

            int start = 90;
            g2.setColor(new Color(64,120,255));
            g2.fillArc(x,y,arcW,arcH,start,(int)Math.round(-completedAngle));
            g2.setColor(new Color(220,230,250));
            g2.fillArc(x,y,arcW,arcH,start + (int)Math.round(-completedAngle),(int)Math.round(-(360.0 - completedAngle)));

            g2.setColor(Color.WHITE);
            g2.fillOval(x + arcW/6, y + arcH/6, arcW*2/3, arcH*2/3);
            g2.setColor(new Color(40, 50, 70));
            String main = completed + " / " + total;
            Font f = g2.getFont().deriveFont(Font.BOLD, 16f);
            g2.setFont(f);
            int sw = g2.getFontMetrics().stringWidth(main);
            g2.drawString(main, x + arcW/2 - sw/2, y + arcH/2 + 6);

            int lx = x + arcW + 10;
            int ly = y + 6;
            g2.setFont(g2.getFont().deriveFont(12f));
            g2.setColor(new Color(64,120,255));
            g2.fillRect(lx, ly, 12, 12);
            g2.setColor(new Color(60,60,60));
            g2.drawString("Completed: " + completed, lx + 18, ly + 10);

            g2.setColor(new Color(220,230,250));
            g2.fillRect(lx, ly + 20, 12, 12);
            g2.setColor(new Color(60,60,60));
            g2.drawString("Pending: " + notCompleted, lx + 18, ly + 30);

            g2.dispose();
        }
    }

    private void updateChart() { chartPanel.updateChartData(); }

    // ---------------- Helpers ----------------

    private void updateCategoriesAndChart() {
        Set<String> cats = new TreeSet<>();
        cats.add("All Categories");
        for (int i = 0; i < masterModel.size(); i++) cats.add(masterModel.get(i).category);
        // preserve 'General' and existing categories
        for (String c : new ArrayList<>(Collections.list(categoryModel.elements()))) cats.add(c);

        categoryModel.clear();
        for (String c : cats) categoryModel.addElement(c);

        if (categoryModel.size() > 0 && categoryModel.contains("All Categories") && categoryList.getSelectedIndex() == -1)
            categoryList.setSelectedIndex(0);

        chartPanel.updateChartData();
    }

    private void showManageCategoriesDialog() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        JList<String> tmp = new JList<>(categoryModel);
        tmp.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tmp.setVisibleRowCount(8);
        p.add(new JScrollPane(tmp), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(1,3,6,6));
        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton del = new JButton("Delete");
        buttons.add(add); buttons.add(edit); buttons.add(del);
        p.add(buttons, BorderLayout.SOUTH);

        JDialog d = new JDialog(this, "Manage Categories", true);
        d.getContentPane().add(p);
        d.setSize(380, 320);
        d.setLocationRelativeTo(this);

        add.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(d, "New category:");
            if (name != null && !name.trim().isEmpty() && !categoryModel.contains(name.trim())) {
                categoryModel.addElement(name.trim());
                updateCategoriesAndChart();
            }
        });

        edit.addActionListener(e -> {
            int i = tmp.getSelectedIndex();
            if (i <= 0) { JOptionPane.showMessageDialog(d, "Select a category to edit"); return; }
            String cur = categoryModel.get(i);
            String name = JOptionPane.showInputDialog(d, "Edit category:", cur);
            if (name != null && !name.trim().isEmpty()) {
                String old = cur;
                categoryModel.set(i, name.trim());
                for (int t = 0; t < masterModel.size(); t++) {
                    Task task = masterModel.get(t);
                    if (task.category.equals(old)) task.category = name.trim();
                }
                save();
                refreshView();
                updateCategoriesAndChart();
            }
        });

        del.addActionListener(e -> {
            int i = tmp.getSelectedIndex();
            if (i <= 0) { JOptionPane.showMessageDialog(d, "Select a category to delete"); return; }
            String cat = categoryModel.get(i);
            int confirm = JOptionPane.showConfirmDialog(d, "Delete '" + cat + "'? Tasks will move to 'General'.", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                for (int t = 0; t < masterModel.size(); t++) {
                    Task task = masterModel.get(t);
                    if (task.category.equals(cat)) task.category = "General";
                }
                categoryModel.remove(i);
                save();
                refreshView();
                updateCategoriesAndChart();
            }
        });

        d.setVisible(true);
    }

    private void showChartDialog() {
        JDialog d = new JDialog(this, "Task Statistics", true);
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.add(new ChartPanel(), BorderLayout.CENTER);
        d.getContentPane().add(p);
        d.setSize(520, 420);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private int indexOfRef(DefaultListModel<Task> model, Task ref) {
        for (int i = 0; i < model.size(); i++) if (model.get(i) == ref) return i;
        return -1;
    }

    // ---------------- UI Utilities ----------------

    static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg;
        RoundedPanel(int radius, Color bg) { this.radius = radius; this.bg = bg; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius*2, radius*2);
            g2.setColor(new Color(0,0,0,12));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius*2, radius*2);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final int radius;
        RoundedBorder(int radius) { this.radius = radius; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0,0,0,40));
            g2.drawRoundRect(x, y, width-1, height-1, radius*2, radius*2);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(8,12,8,12); }
        @Override public Insets getBorderInsets(Component c, Insets insets) { return getBorderInsets(c); }
    }

    static class GradientPanel extends JPanel {
        GradientPanel() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth(), h = getHeight();
            Color c1 = new Color(64, 120, 255);
            Color c2 = new Color(100, 80, 220);
            GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void beautifyScrollBar(JScrollPane sp) {
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = new Color(220, 225, 238);
                trackColor = new Color(245, 247, 252);
            }
            @Override protected JButton createDecreaseButton(int orientation) { return hiddenButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return hiddenButton(); }
            private JButton hiddenButton() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); b.setVisible(false); return b; }
        });
    }

    private void installNimbus() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            UIManager.put("TextComponent.arc", 14);
            UIManager.put("Button.arc", 14);
            UIManager.put("Component.focusWidth", 1);
        } catch (Exception ignored) {}
    }

    private Image makeAppIcon() {
        int s = 64;
        Image img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(64,120,255));
        g.fillRoundRect(0,0,s,s,20,20);
        g.setColor(Color.WHITE);
        g.fillOval(14,14,36,36);
        g.setColor(new Color(64,120,255));
        g.fillOval(22,22,20,20);
        g.dispose();
        return img;
    }

    // ---------------- Chart/Update helpers ----------------

    private void updateCategoriesAndChart() {
        Set<String> cats = new TreeSet<>();
        cats.add("All Categories");
        for (int i = 0; i < masterModel.size(); i++) cats.add(masterModel.get(i).category);
        for (String c : Collections.list(categoryModel.elements())) cats.add(c);
        categoryModel.clear();
        for (String c : cats) categoryModel.addElement(c);
        if (categoryModel.size() > 0 && categoryList.getSelectedIndex() == -1) categoryList.setSelectedIndex(0);
        chartPanel.updateChartData();
    }

    // ---------------- Main ----------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TodoApp app = new TodoApp();
            app.setVisible(true);
        });
    }
}



