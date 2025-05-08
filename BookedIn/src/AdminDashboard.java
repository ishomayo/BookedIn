import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class AdminDashboard extends JFrame implements DatabaseEventListener {

    private Timer refreshTimer;

    // Database connection
    private Connection connection;
    
    private String adminUsername;
    private JTabbedPane tabbedPane;
    private JPanel dashboardPanel, librariansPanel, catalogPanel, membersPanel, reportsPanel;
    
    // Dashboard stats components
    private JLabel totalBooksLabel, borrowedBooksLabel, overdueBooksLabel;
    private JLabel activeMembersLabel, librariansCountLabel;

    // Tables
    private JTable booksTable, membersTable, librariansTable, reportsTable;
    private DefaultTableModel booksTableModel, membersTableModel, librariansTableModel, reportsTableModel;
    
    // Search components
    private JTextField bookSearchField, memberSearchField, librarianSearchField;
    private JComboBox<String> bookCategoryFilter;
    
    // For reports
    private JComboBox<String> reportTypeComboBox;
    private JButton generateReportButton;
    
    /**
     * Constructor for the AdminDashboard
     */
    public AdminDashboard(String username) {
        this.adminUsername = username;

        // Establish database connection
        try {
            connection = DatabaseConnection.getConnection();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not connect to database: " + e.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            dispose();
            return;
        }

        // Set up the main frame
        setTitle("BookedIn - Admin Dashboard");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        setResizable(false);

        // Create main panel with dark theme
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(20, 20, 20));

        // Header panel
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Content panel with tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(30, 30, 30));
        tabbedPane.setForeground(Color.black);

        // Create tab panels
        dashboardPanel = createDashboardPanel();
        librariansPanel = createLibrariansPanel();
        catalogPanel = createCatalogPanel();
        membersPanel = createMembersPanel();

        // Add tabs
        tabbedPane.addTab("Dashboard", dashboardPanel);
        tabbedPane.addTab("Librarians", librariansPanel);
        tabbedPane.addTab("Book Catalog", catalogPanel);
        tabbedPane.addTab("Members", membersPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Footer panel
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Load initial data
        loadDashboardData();
        loadLibrariansData();
        loadBooksData();
        loadMembersData();

        // Register for database events
        DatabaseEventManager eventManager = DatabaseEventManager.getInstance();
        eventManager.addListener(DatabaseEventManager.EVENT_BOOK_CHECKOUT, this);
        eventManager.addListener(DatabaseEventManager.EVENT_BOOK_RETURN, this);
        eventManager.addListener(DatabaseEventManager.EVENT_DATA_CHANGED, this);

        // Set up a timer for periodic refresh (every 30 seconds)
        refreshTimer = new Timer(30000, e -> refreshData());
        refreshTimer.start();

        // Add window listener to close connection and unregister events
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Stop the refresh timer
                if (refreshTimer != null && refreshTimer.isRunning()) {
                    refreshTimer.stop();
                }
                // Unregister from events
                DatabaseEventManager eventManager = DatabaseEventManager.getInstance();
                eventManager.removeListener(DatabaseEventManager.EVENT_BOOK_CHECKOUT, AdminDashboard.this);
                eventManager.removeListener(DatabaseEventManager.EVENT_BOOK_RETURN, AdminDashboard.this);
                eventManager.removeListener(DatabaseEventManager.EVENT_DATA_CHANGED, AdminDashboard.this);
            }
        });
    }
    
    /**
     * Implement the event handler method for database events
     */
    @Override
    public void onDatabaseEvent(DatabaseEventManager.DatabaseEvent event) {
        String eventType = event.getEventType();
        
        // Use SwingUtilities.invokeLater to ensure UI updates happen on the EDT
        SwingUtilities.invokeLater(() -> {
            System.out.println("AdminDashboard received event: " + eventType);
            
            if (eventType.equals(DatabaseEventManager.EVENT_BOOK_RETURN) ||
                eventType.equals(DatabaseEventManager.EVENT_BOOK_CHECKOUT)) {
                // Refresh relevant data
                loadDashboardData();
                loadBooksData();
            }
            else if (eventType.equals(DatabaseEventManager.EVENT_DATA_CHANGED)) {
                // Generic data change event, refresh all data
                refreshData();
            }
        });
    }

    /**
     * Refresh all dashboard data
     */
    private void refreshData() {
        loadDashboardData();
        loadLibrariansData();
        loadBooksData();
        loadMembersData();
    }

    /**
     * Create the header panel
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(20, 20, 20));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Left side - Title
        JLabel titleLabel = new JLabel("BookedIn Administration");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Right side - Admin info and logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(new Color(20, 20, 20));

        // Get admin's full name from database
        String adminName = getAdminName();

        JLabel adminLabel = new JLabel("Admin: " + adminName);
        adminLabel.setForeground(Color.LIGHT_GRAY);

        JButton profileButton = new JButton("Profile");
        profileButton.setBackground(new Color(60, 60, 60));
        profileButton.setForeground(Color.black);
        profileButton.setFocusPainted(false);

        profileButton.addActionListener(e -> {
            showProfileDialog();
        });

        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(new Color(60, 60, 60));
        logoutButton.setForeground(Color.black);
        logoutButton.setFocusPainted(false);

        logoutButton.addActionListener(e -> {
		    int option = JOptionPane.showConfirmDialog(this,
		        "Are you sure you want to logout?", 
		        "Logout Confirmation", JOptionPane.YES_NO_OPTION);

		    if (option == JOptionPane.YES_OPTION) {
		        // Close the database connection before disposing the dashboard
		        DatabaseConnection.closeConnection();
		        System.out.println("Connection closed during logout");
		        
		        // Dispose of the current dashboard
		        dispose();
		        
		        // Show a new login screen
		        SwingUtilities.invokeLater(() -> {
		            JFrame loginFrame = new JFrame("BookedIn - Login");
		            loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		            DatabaseSetup.CustomLoginScreen screen = new DatabaseSetup.CustomLoginScreen();
		            loginFrame.setSize(screen.getSize());
		            loginFrame.setLayout(new BorderLayout());
		            loginFrame.add(screen.getContentPane());
		            loginFrame.setLocationRelativeTo(null);
		            loginFrame.setResizable(false);
		            loginFrame = new DatabaseSetup.CustomLoginScreen();;
		            loginFrame.setVisible(true);
		        });
		    }
		});

        rightPanel.add(adminLabel);
        rightPanel.add(Box.createRigidArea(new Dimension(15, 0)));
        rightPanel.add(profileButton);
        rightPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        rightPanel.add(logoutButton);

        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Get admin's name from database
     */
    private String getAdminName() {
        try {
            String sql = "SELECT full_name FROM users WHERE username = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, adminUsername);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("full_name");
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return adminUsername;
    }

    /**
     * Show the admin profile dialog
     */
    private void showProfileDialog() {
        try {
            // Get admin info
            String sql = "SELECT * FROM users WHERE username = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, adminUsername);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Error loading admin profile", "Profile Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String fullName = rs.getString("full_name");
            String email = rs.getString("email");
            String registrationDate = rs.getString("registration_date");

            rs.close();
            stmt.close();

            // Create profile dialog
            JDialog dialog = new JDialog(this, "Admin Profile", true);
            dialog.setSize(400, 500);
            dialog.setLocationRelativeTo(this);
            dialog.setResizable(false);
            dialog.setUndecorated(true);
            
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBackground(new Color(30, 30, 30));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

            JLabel titleLabel = new JLabel("Admin Profile");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Username field (read-only)
            JPanel usernamePanel = createFormField("Username:", 100);
            JTextField usernameField = (JTextField) usernamePanel.getComponent(1);
            usernameField.setText(adminUsername);
            usernameField.setEditable(false);
            usernameField.setBackground(new Color(50, 50, 50));

            // Full name field
            JPanel namePanel = createFormField("Full Name:", 100);
            JTextField nameField = (JTextField) namePanel.getComponent(1);
            nameField.setText(fullName);

            // Email field
            JPanel emailPanel = createFormField("Email:", 100);
            JTextField emailField = (JTextField) emailPanel.getComponent(1);
            emailField.setText(email);

            // Registration date field (read-only)
            JPanel regDatePanel = createFormField("Joined:", 100);
            JTextField regDateField = (JTextField) regDatePanel.getComponent(1);
            regDateField.setText(registrationDate);
            regDateField.setEditable(false);
            regDateField.setBackground(new Color(50, 50, 50));

            // Change password fields
            JPanel currentPwdPanel = createFormField("Current Password:", 100);
            JPasswordField currentPwdField = new JPasswordField(20);
            currentPwdField.setPreferredSize(new Dimension(300, 25));
            currentPwdField.setBackground(new Color(60, 60, 60));
            currentPwdField.setForeground(Color.WHITE);
            currentPwdField.setCaretColor(Color.WHITE);
            currentPwdPanel.remove(1); // Remove the default text field
            currentPwdPanel.add(currentPwdField);

            JPanel newPwdPanel = createFormField("New Password:", 100);
            JPasswordField newPwdField = new JPasswordField(20);
            newPwdField.setPreferredSize(new Dimension(300, 25));
            newPwdField.setBackground(new Color(60, 60, 60));
            newPwdField.setForeground(Color.WHITE);
            newPwdField.setCaretColor(Color.WHITE);
            newPwdPanel.remove(1); // Remove the default text field
            newPwdPanel.add(newPwdField);

            JPanel confirmPwdPanel = createFormField("Confirm Password:", 100);
            JPasswordField confirmPwdField = new JPasswordField(20);
            confirmPwdField.setPreferredSize(new Dimension(300, 25));
            confirmPwdField.setBackground(new Color(60, 60, 60));
            confirmPwdField.setForeground(Color.WHITE);
            confirmPwdField.setCaretColor(Color.WHITE);
            confirmPwdPanel.remove(1); // Remove the default text field
            confirmPwdPanel.add(confirmPwdField);

            // Buttons panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(new Color(30, 30, 30));
            buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JButton cancelButton = new JButton("Exit");
            cancelButton.setBackground(new Color(60, 60, 60));
            cancelButton.setForeground(Color.black);

            JButton saveButton = new JButton("Save Changes");
            saveButton.setBackground(new Color(0, 102, 204));
            saveButton.setForeground(Color.black);

            buttonPanel.add(cancelButton);
            buttonPanel.add(saveButton);

            // Add all panels to main panel
            mainPanel.add(titleLabel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            mainPanel.add(usernamePanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            mainPanel.add(namePanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            mainPanel.add(emailPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            mainPanel.add(regDatePanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            mainPanel.add(new JSeparator());
            mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
            mainPanel.add(currentPwdPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            mainPanel.add(newPwdPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            mainPanel.add(confirmPwdPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            mainPanel.add(buttonPanel);

            dialog.add(new JScrollPane(mainPanel));

            // Add action listeners
            cancelButton.addActionListener(e -> dialog.dispose());

            saveButton.addActionListener(e -> {
                String newFullName = nameField.getText().trim();
                String newEmail = emailField.getText().trim();
                String currentPwd = new String(currentPwdField.getPassword());
                String newPwd = new String(newPwdField.getPassword());
                String confirmPwd = new String(confirmPwdField.getPassword());

                if (newFullName.isEmpty() || newEmail.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Full Name and Email cannot be empty", "Missing Information",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Check if we're changing the password
                boolean changingPassword = !currentPwd.isEmpty() || !newPwd.isEmpty() || !confirmPwd.isEmpty();

                if (changingPassword) {
                    if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "All password fields are required to change password",
                                "Missing Password Information", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    if (!newPwd.equals(confirmPwd)) {
                        JOptionPane.showMessageDialog(dialog, "New password and confirmation do not match",
                                "Password Mismatch", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // Verify current password
                    try {
                        String checkSql = "SELECT * FROM users WHERE username = ? AND password = ?";
                        PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                        checkStmt.setString(1, adminUsername);
                        checkStmt.setString(2, currentPwd);
                        ResultSet checkRs = checkStmt.executeQuery();

                        if (!checkRs.next()) {
                            JOptionPane.showMessageDialog(dialog, "Current password is incorrect", "Invalid Password",
                                    JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        checkRs.close();
                        checkStmt.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        return;
                    }
                }

                // Update profile
                try {
                    String updateSql;
                    PreparedStatement updateStmt;

                    if (changingPassword) {
                        updateSql = "UPDATE users SET full_name = ?, email = ?, password = ? WHERE username = ?";
                        updateStmt = connection.prepareStatement(updateSql);
                        updateStmt.setString(1, newFullName);
                        updateStmt.setString(2, newEmail);
                        updateStmt.setString(3, newPwd);
                        updateStmt.setString(4, adminUsername);
                    } else {
                        updateSql = "UPDATE users SET full_name = ?, email = ? WHERE username = ?";
                        updateStmt = connection.prepareStatement(updateSql);
                        updateStmt.setString(1, newFullName);
                        updateStmt.setString(2, newEmail);
                        updateStmt.setString(3, adminUsername);
                    }

                    updateStmt.executeUpdate();
                    updateStmt.close();

                    JOptionPane.showMessageDialog(dialog, "Profile updated successfully!", "Profile Updated",
                            JOptionPane.INFORMATION_MESSAGE);

                    dialog.dispose();

                    // Refresh the header with the new name
                    refreshHeaderName(newFullName);

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Error updating profile: " + ex.getMessage(), "Update Error",
                            JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });

            dialog.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading admin profile: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Refresh the header with the updated admin name
     */
    private void refreshHeaderName(String newName) {
        JPanel headerPanel = (JPanel) ((BorderLayout) getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
        JPanel rightPanel = (JPanel) ((BorderLayout) headerPanel.getLayout()).getLayoutComponent(BorderLayout.EAST);
        JLabel adminLabel = (JLabel) rightPanel.getComponent(0);
        adminLabel.setText("Admin: " + newName);
    }

    /**
     * Create a form field with a label and text field
     */
    private JPanel createFormField(String labelText, int labelWidth) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        panel.setBackground(new Color(30, 30, 30));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setPreferredSize(new Dimension(labelWidth, 25));

        JTextField textField = new JTextField(20);
        textField.setPreferredSize(new Dimension(300, 25));
        textField.setBackground(new Color(60, 60, 60));
        textField.setForeground(Color.WHITE);
        textField.setCaretColor(Color.WHITE);
        textField.setBorder(
                BorderFactory.createCompoundBorder(textField.getBorder(), BorderFactory.createEmptyBorder(0, 5, 0, 5)));

        panel.add(label);
        panel.add(textField);

        return panel;
    }

    /**
     * Create the dashboard panel with absolute positioning
     */
    private JPanel createDashboardPanel() {
        // Create a panel with null layout for absolute positioning
        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(30, 30, 30));
        
        // Define the header section
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(30, 30, 30));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        titlePanel.setBounds(0, 0, 1180, 60); // Position at top, full width
        
        JLabel titleLabel = new JLabel("Admin Dashboard Overview");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel dateLabel = new JLabel(new SimpleDateFormat("EEEE, MMMM d, yyyy").format(new Date()));
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        dateLabel.setForeground(Color.LIGHT_GRAY);
        
        titlePanel.add(titleLabel, BorderLayout.WEST);
        titlePanel.add(dateLabel, BorderLayout.EAST);
        
        panel.add(titlePanel);
        
        // Define the stats cards section with absolute positioning
        int cardWidth = 215;
        int cardHeight = 150;
        int cardSpacing = 20;
        int startY = 70; // Start below header
        int startX = 20; // Left margin
        
        // Total Books Card
        JPanel booksCard = createStatsCard("Total Books", "0");
        booksCard.setBounds(startX, startY, cardWidth, cardHeight);
        panel.add(booksCard);
        // Store the value label reference for database updates
        totalBooksLabel = findValueLabel(booksCard);
        
        // Borrowed Books Card
        JPanel borrowedCard = createStatsCard("Books Checked Out", "0");
        borrowedCard.setBounds(startX + cardWidth + cardSpacing, startY, cardWidth, cardHeight);
        panel.add(borrowedCard);
        borrowedBooksLabel = findValueLabel(borrowedCard);
        
        // Overdue Card
        JPanel overdueCard = createStatsCard("Overdue Books", "0");
        overdueCard.setBounds(startX + (cardWidth + cardSpacing) * 2, startY, cardWidth, cardHeight);
        panel.add(overdueCard);
        overdueBooksLabel = findValueLabel(overdueCard);
        
        // Active Members Card
        JPanel membersCard = createStatsCard("Active Members", "0");
        membersCard.setBounds(startX + (cardWidth + cardSpacing) * 3, startY, cardWidth, cardHeight);
        panel.add(membersCard);
        activeMembersLabel = findValueLabel(membersCard);
        
        // Librarians Card
        JPanel librariansCard = createStatsCard("Librarians", "0");
        librariansCard.setBounds(startX + (cardWidth + cardSpacing) * 4, startY, cardWidth, cardHeight);
        panel.add(librariansCard);
        librariansCountLabel = findValueLabel(librariansCard);
        
        // Quick actions panel
        JPanel quickActionsPanel = new JPanel();
        quickActionsPanel.setLayout(new BoxLayout(quickActionsPanel, BoxLayout.X_AXIS));
        quickActionsPanel.setBackground(new Color(30, 30, 30));
        quickActionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        quickActionsPanel.setBounds(startX, startY + cardHeight + 20, 1160, 50);
        
        JButton addLibrarianButton = createActionButton("Add Librarian", new Color(0, 120, 215));
        JButton systemConfigButton = createActionButton("System Settings", new Color(0, 153, 51));
        JButton backupButton = createActionButton("Backup Database", new Color(204, 102, 0));
        JButton generateReportButton = createActionButton("Generate Reports", new Color(102, 0, 204));
        
        addLibrarianButton.addActionListener(e -> showAddLibrarianDialog());
        systemConfigButton.addActionListener(e -> showSystemConfigDialog());
        backupButton.addActionListener(e -> backupDatabase());
        generateReportButton.addActionListener(e -> tabbedPane.setSelectedIndex(4));
        
        quickActionsPanel.add(addLibrarianButton);
        quickActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        quickActionsPanel.add(systemConfigButton);
        quickActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        quickActionsPanel.add(backupButton);
        quickActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        quickActionsPanel.add(generateReportButton);
        
        panel.add(quickActionsPanel);
        
        // System Overview Panel
        JPanel overviewPanel = new JPanel(new BorderLayout());
        overviewPanel.setBackground(new Color(40, 40, 40));
        overviewPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        overviewPanel.setBounds(startX, startY + cardHeight + 80, 560, 350);
        
        JLabel overviewLabel = new JLabel("System Overview");
        overviewLabel.setFont(new Font("Arial", Font.BOLD, 16));
        overviewLabel.setForeground(Color.WHITE);
        overviewPanel.add(overviewLabel, BorderLayout.NORTH);
        
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBackground(new Color(40, 40, 40));
        
        // Add some system statistics
        String[] statLabels = {
            "System Status: ", 
            "Database Size: ", 
            "Last Backup: ", 
            "Database Version: ",
            "System Uptime: "
        };
        
        String[] statValues = {
            "Online - Running normally",
            "2.3 MB",
            "Never",
            "MySQL 8.0.34",
            "3 days, 7 hours, 22 minutes"
        };
        
        for (int i = 0; i < statLabels.length; i++) {
            JPanel statRow = new JPanel(new BorderLayout());
            statRow.setBackground(new Color(40, 40, 40));
            statRow.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            
            JLabel labelText = new JLabel(statLabels[i]);
            labelText.setForeground(Color.LIGHT_GRAY);
            
            JLabel valueText = new JLabel(statValues[i]);
            valueText.setForeground(Color.WHITE);
            
            statRow.add(labelText, BorderLayout.WEST);
            statRow.add(valueText, BorderLayout.EAST);
            
            statsPanel.add(statRow);
            statsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        
        overviewPanel.add(statsPanel, BorderLayout.CENTER);
        panel.add(overviewPanel);
        
        // Recent Activities Panel
        JPanel activitiesPanel = new JPanel(new BorderLayout());
        activitiesPanel.setBackground(new Color(40, 40, 40));
        activitiesPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        activitiesPanel.setBounds(startX + 580, startY + cardHeight + 80, 560, 350);
        
        JLabel activitiesLabel = new JLabel("Recent System Activities");
        activitiesLabel.setFont(new Font("Arial", Font.BOLD, 16));
        activitiesLabel.setForeground(Color.WHITE);
        activitiesPanel.add(activitiesLabel, BorderLayout.NORTH);
        
        DefaultListModel<String> activityModel = new DefaultListModel<>();
        activityModel.addElement("[System] Database backup completed successfully (2 days ago)");
        activityModel.addElement("[Admin] User 'jenelyn' role changed to librarian (3 days ago)");
        activityModel.addElement("[System] System update completed (5 days ago)");
        activityModel.addElement("[Admin] New librarian account 'jenelyn' created (5 days ago)");
        activityModel.addElement("[Admin] Database maintenance performed (1 week ago)");
        activityModel.addElement("[System] System restarted due to scheduled maintenance (1 week ago)");
        
        JList<String> activityList = new JList<>(activityModel);
        activityList.setBackground(new Color(40, 40, 40));
        activityList.setForeground(Color.WHITE);
        activityList.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        activityList.setSelectionBackground(new Color(60, 60, 70));
        
        JScrollPane activityScroll = new JScrollPane(activityList);
        activityScroll.setBackground(new Color(40, 40, 40));
        activityScroll.setBorder(BorderFactory.createEmptyBorder());
        
        activitiesPanel.add(activityScroll, BorderLayout.CENTER);
        panel.add(activitiesPanel);
        
        // Load initial data from database
        loadDashboardData();
        
        return panel;
    }

    /**
     * Helper method to find the value label in a stats card
     */
    private JLabel findValueLabel(JPanel card) {
        Component[] components = card.getComponents();
        // Third component should be the value label (after title and vertical spacer)
        if (components.length >= 3 && components[2] instanceof JLabel) {
            return (JLabel) components[2];
        }
        
        // Fallback
        return new JLabel("0");
    }

    /**
     * Create a stats card with a consistent style
     */
    private JPanel createStatsCard(String title, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(40, 40, 40));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.LIGHT_GRAY);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Add some vertical space between the title and value
        Component verticalStrut = Box.createVerticalStrut(15);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        card.add(titleLabel);
        card.add(verticalStrut);
        card.add(valueLabel);
        
        return card;
    }
    
    /**
     * Create a styled button for dashboard actions
     */
    private JButton createActionButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(150, 35));
        button.setMaximumSize(new Dimension(150, 35));
        return button;
    }
    
    /**
     * Load dashboard data from database
     */
    private void loadDashboardData() {
        try {
            // Get total books count
            String booksSql = "SELECT COUNT(*) FROM books";
            PreparedStatement booksStmt = connection.prepareStatement(booksSql);
            ResultSet booksRs = booksStmt.executeQuery();

            if (booksRs.next()) {
                totalBooksLabel.setText(String.valueOf(booksRs.getInt(1)));
            }

            booksRs.close();
            booksStmt.close();

            // Get checked out books count
            String borrowedSql = "SELECT COUNT(*) FROM borrowed_books WHERE return_date IS NULL";
            PreparedStatement borrowedStmt = connection.prepareStatement(borrowedSql);
            ResultSet borrowedRs = borrowedStmt.executeQuery();

            if (borrowedRs.next()) {
                borrowedBooksLabel.setText(String.valueOf(borrowedRs.getInt(1)));
            }

            borrowedRs.close();
            borrowedStmt.close();

            // Get overdue books count
            String overdueSql = "SELECT COUNT(*) FROM borrowed_books WHERE return_date IS NULL AND due_date < CURRENT_DATE()";
            PreparedStatement overdueStmt = connection.prepareStatement(overdueSql);
            ResultSet overdueRs = overdueStmt.executeQuery();

            if (overdueRs.next()) {
                overdueBooksLabel.setText(String.valueOf(overdueRs.getInt(1)));
            }

            overdueRs.close();
            overdueStmt.close();

            // Get active members count
            String membersSql = "SELECT COUNT(*) FROM users WHERE role = 'member'";
            PreparedStatement membersStmt = connection.prepareStatement(membersSql);
            ResultSet membersRs = membersStmt.executeQuery();

            if (membersRs.next()) {
                activeMembersLabel.setText(String.valueOf(membersRs.getInt(1)));
            }

            membersRs.close();
            membersStmt.close();
            
            // Get librarians count
            String librariansSql = "SELECT COUNT(*) FROM users WHERE role = 'librarian'";
            PreparedStatement librariansStmt = connection.prepareStatement(librariansSql);
            ResultSet librariansRs = librariansStmt.executeQuery();

            if (librariansRs.next()) {
                librariansCountLabel.setText(String.valueOf(librariansRs.getInt(1)));
            }

            librariansRs.close();
            librariansStmt.close();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading dashboard data: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Create the librarians panel for managing librarian accounts
     */
    private JPanel createLibrariansPanel() {
        // Use absolute positioning for better control
        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(30, 30, 30));
        
        // Get the full width available for the panel
        int panelWidth = 1200; // Estimated panel width
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 30, 30));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        headerPanel.setBounds(0, 0, panelWidth, 60);
        
        JLabel titleLabel = new JLabel("Manage Librarians");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(30, 30, 30));
        
        JButton addButton = new JButton("Add New Librarian");
        addButton.setBackground(new Color(0, 102, 204));
        addButton.setForeground(Color.black);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBackground(new Color(60, 60, 60));
        refreshButton.setForeground(Color.black);
        
        buttonPanel.add(addButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(refreshButton);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel);
        
        // Search panel with centered positioning
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        searchPanel.setBackground(new Color(30, 30, 30));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        searchPanel.setBounds(0, 60, panelWidth, 50);
        
        JLabel searchLabel = new JLabel("Search librarians:");
        searchLabel.setForeground(Color.WHITE);
        searchLabel.setPreferredSize(new Dimension(120, 25));
        
        librarianSearchField = new JTextField();
        librarianSearchField.setPreferredSize(new Dimension(300, 25));
        
        JButton searchButton = new JButton("Search");
        searchButton.setBackground(new Color(60, 60, 60));
        searchButton.setForeground(Color.black);
        searchButton.setPreferredSize(new Dimension(80, 25));
        
        searchPanel.add(searchLabel);
        searchPanel.add(librarianSearchField);
        searchPanel.add(searchButton);
        
        panel.add(searchPanel);
        
        // Librarians table with centered positioning
        String[] columns = { "Username", "Full Name", "Email", "Join Date", "Last Login", "Status", "Actions" };
        
        librariansTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only actions column is editable
            }
        };
        
        librariansTable = new JTable(librariansTableModel);
        librariansTable.setBackground(new Color(40, 40, 40));
        librariansTable.setForeground(Color.WHITE);
        librariansTable.setGridColor(new Color(60, 60, 60));
        librariansTable.getTableHeader().setBackground(new Color(50, 50, 50));
        librariansTable.getTableHeader().setForeground(Color.BLACK);
        librariansTable.setRowHeight(30); // Increased row height
        
        // Set preferred column widths
        TableColumnModel columnModel = librariansTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(100);  // Username
        columnModel.getColumn(1).setPreferredWidth(150);  // Full Name
        columnModel.getColumn(2).setPreferredWidth(150);  // Email
        columnModel.getColumn(3).setPreferredWidth(100);  // Join Date
        columnModel.getColumn(4).setPreferredWidth(150);  // Last Login
        columnModel.getColumn(5).setPreferredWidth(100);  // Status
        columnModel.getColumn(6).setPreferredWidth(100);  // Actions
        
        // Setup the "Actions" column with buttons
        TableColumn actionsColumn = librariansTable.getColumnModel().getColumn(6);
        actionsColumn.setCellRenderer(new ButtonRenderer());
        actionsColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane scrollPane = new JScrollPane(librariansTable);
        scrollPane.getViewport().setBackground(new Color(40, 40, 40));
        
        // Calculate values for centered table
        int tableWidth = 1100;        // Desired table width
        int tableHeight = 490;       // Desired table height
        int tableY = 130;            // Y position (from top)
        int tableX = (panelWidth - tableWidth) / 2;  // Center horizontally
        
        scrollPane.setBounds(tableX, tableY, tableWidth, tableHeight);
        
        panel.add(scrollPane);
        
        // Add action listeners
        addButton.addActionListener(e -> showAddLibrarianDialog());
        
        refreshButton.addActionListener(e -> loadLibrariansData());
        
        // Configure search functionality
        searchButton.addActionListener(e -> {
            String searchText = librarianSearchField.getText().trim();
            filterLibrariansTable(searchText);
        });
        
        return panel;
    }
    
    /**
     * Load librarian data from database
     */
    private void loadLibrariansData() {
        librariansTableModel.setRowCount(0);

        try {
            String sql = "SELECT username, full_name, email, registration_date, last_login " +
                         "FROM users WHERE role = 'librarian' ORDER BY full_name";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                String fullName = rs.getString("full_name");
                String email = rs.getString("email");
                String joinDate = rs.getString("registration_date");
                String lastLogin = rs.getString("last_login");
                
                // For status, we'll just use "Active" for now
                String status = "Active";

                librariansTableModel.addRow(new Object[] { 
                    username, fullName, email, joinDate, 
                    (lastLogin != null ? lastLogin : "Never"), status, "Edit/Remove" 
                });
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading librarians data: " + e.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Filter librarians table based on search text
     */
    private void filterLibrariansTable(String searchText) {
        librariansTableModel.setRowCount(0);

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT username, full_name, email, registration_date, last_login ")
                      .append("FROM users WHERE role = 'librarian' ");

            if (!searchText.isEmpty()) {
                sqlBuilder.append("AND (LOWER(username) LIKE ? OR LOWER(full_name) LIKE ? OR LOWER(email) LIKE ?) ");
            }

            sqlBuilder.append("ORDER BY full_name");

            PreparedStatement stmt = connection.prepareStatement(sqlBuilder.toString());

            if (!searchText.isEmpty()) {
                String searchPattern = "%" + searchText.toLowerCase() + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
                stmt.setString(3, searchPattern);
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                String fullName = rs.getString("full_name");
                String email = rs.getString("email");
                String joinDate = rs.getString("registration_date");
                String lastLogin = rs.getString("last_login");
                
                // For status, we'll just use "Active" for now
                String status = "Active";

                librariansTableModel.addRow(new Object[] { 
                    username, fullName, email, joinDate, 
                    (lastLogin != null ? lastLogin : "Never"), status, "Edit/Remove" 
                });
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error filtering librarians: " + e.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Show dialog to add a new librarian
     */
    private void showAddLibrarianDialog() {
        JDialog dialog = new JDialog(this, "Add New Librarian", true);
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(true);
        dialog.setResizable(false);
        
        // Create panel with white border
        JPanel borderPanel = new JPanel(new BorderLayout());
        borderPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        borderPanel.setBackground(new Color(30, 30, 30));
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(30, 30, 30));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Add New Librarian");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Create form fields
        JPanel usernamePanel = createFormField("Username:", 120);
        JTextField usernameField = (JTextField) usernamePanel.getComponent(1);
        
        JPanel passwordPanel = createFormField("Password:", 120);
        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setPreferredSize(new Dimension(300, 30));
        passwordField.setBackground(new Color(60, 60, 60));
        passwordField.setForeground(Color.WHITE);
        passwordField.setCaretColor(Color.WHITE);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                passwordField.getBorder(), 
                BorderFactory.createEmptyBorder(0, 5, 0, 5)));
        passwordPanel.remove(1); // Remove the default text field
        passwordPanel.add(passwordField); // Add the password field
        
        JPanel namePanel = createFormField("Full Name:", 120);
        JTextField nameField = (JTextField) namePanel.getComponent(1);
        
        JPanel emailPanel = createFormField("Email:", 120);
        JTextField emailField = (JTextField) emailPanel.getComponent(1);
        
        JPanel phonePanel = createFormField("Phone:", 120);
        JTextField phoneField = (JTextField) phonePanel.getComponent(1);
        
        // Generate password button
        JPanel generatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        generatePanel.setBackground(new Color(30, 30, 30));
        generatePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton generateButton = new JButton("Generate Password");
        generateButton.setBackground(new Color(60, 60, 60));
        generateButton.setForeground(Color.black);
        
        generatePanel.add(generateButton);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(30, 30, 30));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(new Color(60, 60, 60));
        cancelButton.setForeground(Color.black);
        
        JButton saveButton = new JButton("Add Librarian");
        saveButton.setBackground(new Color(0, 102, 204));
        saveButton.setForeground(Color.black);
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        // Add all panels to main panel with improved spacing
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(usernamePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(passwordPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(generatePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(namePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(emailPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(phonePanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(buttonPanel);
        
        // Add main panel to border panel
        borderPanel.add(mainPanel, BorderLayout.CENTER);
        
        // Add border panel to dialog
        dialog.add(new JScrollPane(borderPanel));
        
        // Add action listeners
        generateButton.addActionListener(e -> {
            String generatedPassword = generateRandomPassword();
            passwordField.setText(generatedPassword);
            JOptionPane.showMessageDialog(dialog, 
                "Generated password: " + generatedPassword + "\n\nMake sure to note this down to share with the librarian.",
                "Password Generated", JOptionPane.INFORMATION_MESSAGE);
        });
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        saveButton.addActionListener(e -> {
            // Validate fields
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String fullName = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            
            // Basic validation
            if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, 
                        "Please fill all required fields (Username, Password, Full Name, Email)",
                        "Missing Information", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Validate email format
            if (!isValidEmail(email)) {
                JOptionPane.showMessageDialog(dialog, 
                        "Please enter a valid email address",
                        "Invalid Email", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Add the librarian to database
            try {
                // Check if username already exists
                String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
                PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                checkStmt.setString(1, username);
                ResultSet checkRs = checkStmt.executeQuery();
                checkRs.next();
                int count = checkRs.getInt(1);
                checkRs.close();
                checkStmt.close();
                
                if (count > 0) {
                    JOptionPane.showMessageDialog(dialog, 
                            "Username already exists. Please choose a different username.",
                            "Username Exists", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Insert new librarian
                String insertSql = "INSERT INTO users (username, password, full_name, email, phone, role, registration_date) " +
                        "VALUES (?, ?, ?, ?, ?, 'librarian', CURRENT_DATE())";
                PreparedStatement insertStmt = connection.prepareStatement(insertSql);
                insertStmt.setString(1, username);
                insertStmt.setString(2, password);
                insertStmt.setString(3, fullName);
                insertStmt.setString(4, email);
                insertStmt.setString(5, phone);
                insertStmt.executeUpdate();
                insertStmt.close();
                
                JOptionPane.showMessageDialog(dialog, 
                        "Librarian added successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Refresh data
                loadDashboardData();
                loadLibrariansData();
                
                dialog.dispose();
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, 
                        "Error adding librarian: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
        
        dialog.setVisible(true);
    }
    
    /**
     * Generate a random password
     */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }
    
    /**
     * Simple email validation
     */
    private boolean isValidEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(regex);
    }
    
    /**
     * Show the system configuration dialog
     */
    private void showSystemConfigDialog() {
        JDialog dialog = new JDialog(this, "System Settings", true);
        dialog.setSize(600, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setUndecorated(true);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(30, 30, 30));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("System Configuration");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(30, 30, 30));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        
        // Create tabs for different settings
        JTabbedPane settingsTabs = new JTabbedPane();
        settingsTabs.setBackground(new Color(40, 40, 40));
        settingsTabs.setForeground(Color.BLACK);
        
        // General Settings Panel
        JPanel generalPanel = new JPanel();
        generalPanel.setLayout(new BoxLayout(generalPanel, BoxLayout.Y_AXIS));
        generalPanel.setBackground(new Color(40, 40, 40));
        generalPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Database Settings Panel
        JPanel dbPanel = new JPanel();
        dbPanel.setLayout(new BoxLayout(dbPanel, BoxLayout.Y_AXIS));
        dbPanel.setBackground(new Color(40, 40, 40));
        dbPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // System Settings Panel
        JPanel systemPanel = new JPanel();
        systemPanel.setLayout(new BoxLayout(systemPanel, BoxLayout.Y_AXIS));
        systemPanel.setBackground(new Color(40, 40, 40));
        systemPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Add some example settings to each panel
        
        // General settings
        JPanel libraryNamePanel = createSettingPanel("Library Name:", "BookedIn Library");
        JPanel maxBooksPanel = createSettingPanel("Max Books Per Member:", "5");
        JPanel loanDaysPanel = createSettingPanel("Default Loan Period (days):", "14");
        JPanel fineRatePanel = createSettingPanel("Fine Rate Per Day ($):", "0.50");
        
        generalPanel.add(libraryNamePanel);
        generalPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        generalPanel.add(maxBooksPanel);
        generalPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        generalPanel.add(loanDaysPanel);
        generalPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        generalPanel.add(fineRatePanel);
        generalPanel.add(Box.createVerticalGlue());
        
        // Database settings
        JPanel dbHostPanel = createSettingPanel("Database Host:", "localhost");
        JPanel dbPortPanel = createSettingPanel("Database Port:", "3306");
        JPanel dbNamePanel = createSettingPanel("Database Name:", "BookedIN");
        JPanel dbUserPanel = createSettingPanel("Database Username:", "root");
        JPanel dbPassPanel = createSettingPanel("Database Password:", "••••••••");
        
        dbPanel.add(dbHostPanel);
        dbPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        dbPanel.add(dbPortPanel);
        dbPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        dbPanel.add(dbNamePanel);
        dbPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        dbPanel.add(dbUserPanel);
        dbPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        dbPanel.add(dbPassPanel);
        dbPanel.add(Box.createVerticalGlue());
        
        // System settings
        JPanel backupDirPanel = createSettingPanel("Backup Directory:", "C:/BookedInBackups");
        JPanel autoBackupPanel = createSettingPanel("Auto Backup Frequency:", "Daily");
        JPanel logLevelPanel = createSettingPanel("Log Level:", "Info");
        JPanel themeModePanel = createSettingPanel("Theme Mode:", "Dark");
        
        systemPanel.add(backupDirPanel);
        systemPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        systemPanel.add(autoBackupPanel);
        systemPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        systemPanel.add(logLevelPanel);
        systemPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        systemPanel.add(themeModePanel);
        systemPanel.add(Box.createVerticalGlue());
        
        // Add panels to tabs
        settingsTabs.addTab("General", generalPanel);
        settingsTabs.addTab("Database", dbPanel);
        settingsTabs.addTab("System", systemPanel);
        
        contentPanel.add(settingsTabs);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(30, 30, 30));
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(new Color(60, 60, 60));
        cancelButton.setForeground(Color.black);
        
        JButton saveButton = new JButton("Save Settings");
        saveButton.setBackground(new Color(0, 102, 204));
        saveButton.setForeground(Color.black);
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        
        // Add action listeners
        cancelButton.addActionListener(e -> dialog.dispose());
        
        saveButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(dialog, 
                    "Settings saved successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            dialog.dispose();
        });
        
        dialog.setVisible(true);
    }
    
    /**
     * Create a setting panel with label and text field
     */
    private JPanel createSettingPanel(String labelText, String value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 40, 40));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        
        JTextField field = new JTextField(value);
        field.setPreferredSize(new Dimension(200, 25));
        field.setBackground(new Color(60, 60, 60));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        
        panel.add(label, BorderLayout.WEST);
        panel.add(field, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Backup the database
     */
    private void backupDatabase() {
        // Create a progress dialog
        JDialog progressDialog = new JDialog(this, "Database Backup", true);
        progressDialog.setSize(400, 150);
        progressDialog.setLocationRelativeTo(this);
        progressDialog.setResizable(false);
        progressDialog.setUndecorated(true);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(30, 30, 30));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel statusLabel = new JLabel("Backing up database...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(350, 20));
        progressBar.setStringPainted(true);
        progressBar.setBackground(new Color(60, 60, 60));
        progressBar.setForeground(new Color(0, 102, 204));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(progressBar);
        
        progressDialog.add(mainPanel);
        
        // Use a swing timer to simulate backup progress
        Timer timer = new Timer(50, null);
        timer.addActionListener(new ActionListener() {
            private int progress = 0;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                progress += 1;
                progressBar.setValue(progress);
                
                if (progress == 30) {
                    statusLabel.setText("Compressing database files...");
                } else if (progress == 60) {
                    statusLabel.setText("Writing backup file...");
                } else if (progress == 90) {
                    statusLabel.setText("Finalizing backup...");
                }
                
                if (progress >= 100) {
                    timer.stop();
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(AdminDashboard.this, 
                            "Database backup completed successfully!\nBackup saved to: C:/BookedInBackups/backup_" + 
                            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".sql",
                            "Backup Complete", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        
        // Start the timer and show the dialog
        SwingUtilities.invokeLater(() -> timer.start());
        progressDialog.setVisible(true);
    }
    
    /**
     * Create the catalog panel for managing books
     */
    private JPanel createCatalogPanel() {
        // Use absolute positioning for better control
        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(30, 30, 30));
        
        // Get the full width available for the panel
        int panelWidth = 1200; // Estimated panel width
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 30, 30));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        headerPanel.setBounds(0, 0, panelWidth, 60);
        
        JLabel titleLabel = new JLabel("Book Catalog");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(30, 30, 30));
        
        JButton addButton = new JButton("Add New Book");
        addButton.setBackground(new Color(0, 102, 204));
        addButton.setForeground(Color.black);
        
        JButton addCopyButton = new JButton("Add Copy");
        addCopyButton.setBackground(new Color(0, 153, 51));
        addCopyButton.setForeground(Color.black);
        
        JButton manageGenresButton = new JButton("Manage Genres");
        manageGenresButton.setBackground(new Color(102, 0, 204));
        manageGenresButton.setForeground(Color.black);
        
        buttonPanel.add(addButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(addCopyButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(manageGenresButton);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel);
        
        // Search & Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        filterPanel.setBackground(new Color(30, 30, 30));
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        filterPanel.setBounds(0, 60, panelWidth, 50);
        
        JLabel filterLabel = new JLabel("Filter by genre:");
        filterLabel.setForeground(Color.WHITE);
        filterLabel.setPreferredSize(new Dimension(90, 25));
        
        bookCategoryFilter = new JComboBox<>(new String[] { "All Genres" });
        bookCategoryFilter.setBackground(new Color(60, 60, 60));
        bookCategoryFilter.setForeground(Color.WHITE);
        bookCategoryFilter.setPreferredSize(new Dimension(150, 25));
        
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(Color.WHITE);
        searchLabel.setPreferredSize(new Dimension(60, 25));
        
        bookSearchField = new JTextField(20);
        bookSearchField.setPreferredSize(new Dimension(200, 25));
        
        JButton searchButton = new JButton("Search");
        searchButton.setBackground(new Color(60, 60, 60));
        searchButton.setForeground(Color.BLACK);
        searchButton.setPreferredSize(new Dimension(80, 25));
        
        filterPanel.add(filterLabel);
        filterPanel.add(bookCategoryFilter);
        filterPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        filterPanel.add(searchLabel);
        filterPanel.add(bookSearchField);
        filterPanel.add(searchButton);
        
        panel.add(filterPanel);
        
        // Books table with proper spacing and centered
        String[] columns = { "ISBN", "Title", "Author", "Genre", "Publication Year", "Copies", "Available Copies", "Actions" };
        
        booksTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Only actions column is editable
            }
        };
        
        booksTable = new JTable(booksTableModel);
        booksTable.setBackground(new Color(40, 40, 40));
        booksTable.setForeground(Color.WHITE);
        booksTable.setGridColor(new Color(60, 60, 60));
        booksTable.getTableHeader().setBackground(new Color(50, 50, 50));
        booksTable.getTableHeader().setForeground(Color.BLACK);
        booksTable.setRowHeight(30); // Increased row height
        
        // Set preferred column widths
        TableColumnModel columnModel = booksTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(120);  // ISBN
        columnModel.getColumn(1).setPreferredWidth(200);  // Title
        columnModel.getColumn(2).setPreferredWidth(150);  // Author
        columnModel.getColumn(3).setPreferredWidth(120);  // Genre
        columnModel.getColumn(4).setPreferredWidth(120);  // Publication Year
        columnModel.getColumn(5).setPreferredWidth(70);   // Copies
        columnModel.getColumn(6).setPreferredWidth(120);  // Available Copies
        columnModel.getColumn(7).setPreferredWidth(100);  // Actions
        
        // Setup the "Actions" column with buttons
        TableColumn actionsColumn = booksTable.getColumnModel().getColumn(7);
        actionsColumn.setCellRenderer(new ButtonRenderer());
        actionsColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane scrollPane = new JScrollPane(booksTable);
        scrollPane.getViewport().setBackground(new Color(40, 40, 40));
        
        // Calculate values for centered table
        int tableWidth = 1100;        // Desired table width
        int tableHeight = 520;        // Desired table height
        int tableY = 120;             // Y position (from top)
        int tableX = (panelWidth - tableWidth) / 2;  // Center horizontally
        
        scrollPane.setBounds(tableX, tableY, tableWidth, tableHeight);
        
        panel.add(scrollPane);
        
        // Add action listeners for buttons
        // (These would connect to the LibrarianDashboard's methods, which we're not implementing here)
        
        // Load genre categories from database
        loadBookGenres();
        
        return panel;
    }
    
    /**
     * Load book genres
     */
    private void loadBookGenres() {
        try {
            String sql = "SELECT DISTINCT genre FROM books ORDER BY genre";
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            // Clear existing items but keep "All Genres"
            bookCategoryFilter.removeAllItems();
            bookCategoryFilter.addItem("All Genres");

            while (rs.next()) {
                String genre = rs.getString("genre");
                if (genre != null && !genre.isEmpty()) {
                    bookCategoryFilter.addItem(genre);
                }
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Load books data
     */
    private void loadBooksData() {
        booksTableModel.setRowCount(0);

        try {
            String sql = "SELECT isbn, title, author, genre, year, "
                    + "(SELECT COUNT(*) FROM books b2 WHERE b2.isbn = b.isbn) AS copies, "
                    + "(SELECT COUNT(*) FROM books b2 WHERE b2.isbn = b.isbn AND b2.available = true) AS available "
                    + "FROM books b " + "GROUP BY isbn ORDER BY title";

            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String isbn = rs.getString("isbn");
                String title = rs.getString("title");
                String author = rs.getString("author");
                String genre = rs.getString("genre");
                int year = rs.getInt("year");
                int copies = rs.getInt("copies");
                int available = rs.getInt("available");

                booksTableModel
                        .addRow(new Object[] { isbn, title, author, genre, year, copies, available, "Edit/Delete" });
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading books data: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Create the members panel
     */
    private JPanel createMembersPanel() {
        // Use absolute positioning for better control
        JPanel panel = new JPanel(null);
        panel.setBackground(new Color(30, 30, 30));
        
        // Get the full width available for the panel
        int panelWidth = 1200; // Estimated panel width
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 30, 30));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        headerPanel.setBounds(0, 0, panelWidth, 60);
        
        JLabel titleLabel = new JLabel("Library Members");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(30, 30, 30));
        
        JButton addButton = new JButton("Add New Member");
        addButton.setBackground(new Color(0, 102, 204));
        addButton.setForeground(Color.black);
        
        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBackground(new Color(60, 60, 60));
        refreshButton.setForeground(Color.black);
        
        buttonPanel.add(addButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(refreshButton);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        panel.add(headerPanel);
        
        // Search panel with centered positioning
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        searchPanel.setBackground(new Color(30, 30, 30));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        searchPanel.setBounds(0, 60, panelWidth, 50);
        
        JLabel searchLabel = new JLabel("Search members:");
        searchLabel.setForeground(Color.WHITE);
        searchLabel.setPreferredSize(new Dimension(120, 25));
        
        memberSearchField = new JTextField();
        memberSearchField.setPreferredSize(new Dimension(300, 25));
        
        JButton searchButton = new JButton("Search");
        searchButton.setBackground(new Color(60, 60, 60));
        searchButton.setForeground(Color.black);
        searchButton.setPreferredSize(new Dimension(80, 25));
        
        searchPanel.add(searchLabel);
        searchPanel.add(memberSearchField);
        searchPanel.add(searchButton);
        
        panel.add(searchPanel);
        
        // Members table with centered positioning
        String[] columns = { "Member ID", "Name", "Email", "Phone", "Join Date", "Books Borrowed", "Status", "Actions" };
        
        membersTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Only actions column is editable
            }
        };
        
        membersTable = new JTable(membersTableModel);
        membersTable.setBackground(new Color(40, 40, 40));
        membersTable.setForeground(Color.WHITE);
        membersTable.setGridColor(new Color(60, 60, 60));
        membersTable.getTableHeader().setBackground(new Color(50, 50, 50));
        membersTable.getTableHeader().setForeground(Color.BLACK);
        membersTable.setRowHeight(30); // Increased row height
        
        // Set preferred column widths
        TableColumnModel columnModel = membersTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(100);  // Member ID
        columnModel.getColumn(1).setPreferredWidth(150);  // Name
        columnModel.getColumn(2).setPreferredWidth(150);  // Email
        columnModel.getColumn(3).setPreferredWidth(100);  // Phone
        columnModel.getColumn(4).setPreferredWidth(100);  // Join Date
        columnModel.getColumn(5).setPreferredWidth(100);  // Books Borrowed
        columnModel.getColumn(6).setPreferredWidth(120);  // Status
        columnModel.getColumn(7).setPreferredWidth(100);  // Actions
        
        // Setup the "Actions" column with buttons
        TableColumn actionsColumn = membersTable.getColumnModel().getColumn(7);
        actionsColumn.setCellRenderer(new ButtonRenderer());
        actionsColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane scrollPane = new JScrollPane(membersTable);
        scrollPane.getViewport().setBackground(new Color(40, 40, 40));
        
        // Calculate values for centered table
        int tableWidth = 1100;         // Desired table width
        int tableHeight = 490;        // Desired table height
        int tableY = 130;             // Y position (from top)
        int tableX = (panelWidth - tableWidth) / 2;  // Center horizontally
        
        scrollPane.setBounds(tableX, tableY, tableWidth, tableHeight);
        
        panel.add(scrollPane);
        
        // Add action listeners for buttons
        refreshButton.addActionListener(e -> loadMembersData());
        
        // Configure search functionality
        searchButton.addActionListener(e -> {
            String searchText = memberSearchField.getText().trim();
            filterMembersTable(searchText);
        });
        
        return panel;
    }
    
    /**
     * Load members data
     */
    private void loadMembersData() {
        membersTableModel.setRowCount(0);

        try {
            String sql = "SELECT u.username, u.full_name, u.email, " + "'-' AS phone, " +
                    "u.registration_date, " +
                    "(SELECT COUNT(*) FROM borrowed_books bb WHERE bb.username = u.username AND bb.return_date IS NULL) AS borrowed, " +
                    "CASE WHEN EXISTS (SELECT 1 FROM borrowed_books bb WHERE bb.username = u.username AND bb.return_date IS NULL AND bb.due_date < CURRENT_DATE()) " +
                    "THEN 'Overdue Books' ELSE 'Active' END AS status " + "FROM users u " + "WHERE u.role = 'member' " +
                    "ORDER BY u.full_name";

            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                String fullName = rs.getString("full_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                String joinDate = rs.getString("registration_date");
                int borrowed = rs.getInt("borrowed");
                String status = rs.getString("status");

                membersTableModel.addRow(
                        new Object[] { username, fullName, email, phone, joinDate, borrowed, status, "View/Edit" });
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading members data: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Show librarian details dialog
     */
    private void showLibrarianDetailsDialog(String username) {
        try {
            // Get librarian info
            String sql = "SELECT * FROM users WHERE username = ? AND role = 'librarian'";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Librarian not found", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String fullName = rs.getString("full_name");
            String email = rs.getString("email");
            String registrationDate = rs.getString("registration_date");
            String lastLogin = rs.getString("last_login");
          
         
            // Create details dialog
            JDialog dialog = new JDialog(this, "Librarian Details", true);
            dialog.setSize(500, 500);
            dialog.setLocationRelativeTo(this);
            dialog.setResizable(false);
            dialog.setUndecorated(true);
            
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBackground(new Color(30, 30, 30));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            JLabel titleLabel = new JLabel("Librarian Details");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Basic information panel
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setBackground(new Color(40, 40, 40));
            infoPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.GRAY), "Basic Information", 
                    TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14), Color.WHITE));
            infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Add basic info fields
            JPanel usernamePanel = createDetailField("Username:", username);
            JPanel namePanel = createDetailField("Full Name:", fullName);
            JPanel emailPanel = createDetailField("Email:", email);
            JPanel joinedPanel = createDetailField("Joined:", registrationDate);
            JPanel lastLoginPanel = createDetailField("Last Login:", (lastLogin != null) ? lastLogin : "Never");
            
            infoPanel.add(usernamePanel);
            infoPanel.add(namePanel);
            infoPanel.add(emailPanel);
            infoPanel.add(joinedPanel);
            infoPanel.add(lastLoginPanel);
           
            // Buttons panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(new Color(30, 30, 30));
            buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JButton editButton = new JButton("Edit Librarian");
            editButton.setBackground(new Color(0, 102, 204));
            editButton.setForeground(Color.BLACK);
            
            JButton closeButton = new JButton("Close");
            closeButton.setBackground(new Color(60, 60, 60));
            closeButton.setForeground(Color.BLACK);
            
            buttonPanel.add(editButton);
            buttonPanel.add(closeButton);
            
            // Add all elements to main panel
            mainPanel.add(titleLabel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            mainPanel.add(infoPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            mainPanel.add(buttonPanel);
            
            dialog.add(new JScrollPane(mainPanel));
            
            // Add action listeners
            editButton.addActionListener(e -> {
                dialog.dispose();
                showEditLibrarianDialog(username);
            });
            
            closeButton.addActionListener(e -> dialog.dispose());
            
            dialog.setVisible(true);
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading librarian details: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Create a detail field with label and value
     */
    private JPanel createDetailField(String label, String value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 40, 40));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(Color.LIGHT_GRAY);
        
        JLabel valueComponent = new JLabel(value);
        valueComponent.setForeground(Color.WHITE);
        
        panel.add(labelComponent, BorderLayout.WEST);
        panel.add(valueComponent, BorderLayout.EAST);
        
        return panel;
    }

    /**
     * Show edit librarian dialog
     */
    private void showEditLibrarianDialog(String username) {
        try {
            // Get librarian info
            String sql = "SELECT * FROM users WHERE username = ? AND role = 'librarian'";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Librarian not found", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String fullName = rs.getString("full_name");
            String email = rs.getString("email");
            String registrationDate = rs.getString("registration_date");

            rs.close();
            stmt.close();

            // Create edit dialog
            JDialog dialog = new JDialog(this, "Edit Librarian", true);
            dialog.setSize(500, 550);
            dialog.setLocationRelativeTo(this);
            dialog.setUndecorated(true);
            dialog.setResizable(false);
            
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBackground(new Color(30, 30, 30));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            
            JLabel titleLabel = new JLabel("Edit Librarian");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
            titleLabel.setForeground(Color.WHITE);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Create form fields
            JPanel usernamePanel = createFormField("Username:", 120);
            JTextField usernameField = (JTextField) usernamePanel.getComponent(1);
            usernameField.setText(username);
            usernameField.setEditable(false);
            usernameField.setBackground(new Color(50, 50, 50));
            
            JPanel namePanel = createFormField("Full Name:", 120);
            JTextField nameField = (JTextField) namePanel.getComponent(1);
            nameField.setText(fullName);
            
            JPanel emailPanel = createFormField("Email:", 120);
            JTextField emailField = (JTextField) emailPanel.getComponent(1);
            emailField.setText(email);
            
            JPanel registrationPanel = createFormField("Joined:", 120);
            JTextField registrationField = (JTextField) registrationPanel.getComponent(1);
            registrationField.setText(registrationDate);
            registrationField.setEditable(false);
            registrationField.setBackground(new Color(50, 50, 50));
            
            // Reset password section
            JPanel passwordPanel = new JPanel();
            passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.Y_AXIS));
            passwordPanel.setBackground(new Color(40, 40, 40));
            passwordPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.GRAY), "Reset Password", 
                    TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14), Color.WHITE));
            passwordPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JPanel newPasswordPanel = createFormField("New Password:", 120);
            JPasswordField newPasswordField = new JPasswordField(20);
            newPasswordField.setPreferredSize(new Dimension(300, 25));
            newPasswordField.setBackground(new Color(60, 60, 60));
            newPasswordField.setForeground(Color.WHITE);
            newPasswordField.setCaretColor(Color.WHITE);
            newPasswordPanel.remove(1); // Remove the default text field
            newPasswordPanel.add(newPasswordField); // Add the password field
            
            JPanel confirmPanel = createFormField("Confirm Password:", 120);
            JPasswordField confirmField = new JPasswordField(20);
            confirmField.setPreferredSize(new Dimension(300, 25));
            confirmField.setBackground(new Color(60, 60, 60));
            confirmField.setForeground(Color.WHITE);
            confirmField.setCaretColor(Color.WHITE);
            confirmPanel.remove(1); // Remove the default text field
            confirmPanel.add(confirmField); // Add the password field
            
            JPanel generatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            generatePanel.setBackground(new Color(40, 40, 40));
            
            JButton generateButton = new JButton("Generate Password");
            generateButton.setBackground(new Color(60, 60, 60));
            generateButton.setForeground(Color.BLACK);
            
            generatePanel.add(generateButton);
            
            passwordPanel.add(newPasswordPanel);
            passwordPanel.add(confirmPanel);
            passwordPanel.add(generatePanel);
            
            // Status section
            JPanel statusPanel = new JPanel();
            statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
            statusPanel.setBackground(new Color(40, 40, 40));
            statusPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(Color.GRAY), "Account Status", 
                    TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 14), Color.WHITE));
            statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JRadioButton activeRadio = new JRadioButton("Active");
            activeRadio.setSelected(true);
            activeRadio.setBackground(new Color(40, 40, 40));
            activeRadio.setForeground(Color.WHITE);
            
            JRadioButton suspendedRadio = new JRadioButton("Suspended");
            suspendedRadio.setBackground(new Color(40, 40, 40));
            suspendedRadio.setForeground(Color.WHITE);
            
            ButtonGroup statusGroup = new ButtonGroup();
            statusGroup.add(activeRadio);
            statusGroup.add(suspendedRadio);
            
            statusPanel.add(activeRadio);
            statusPanel.add(suspendedRadio);
            
            // Buttons panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(new Color(30, 30, 30));
            buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JButton cancelButton = new JButton("Cancel");
            cancelButton.setBackground(new Color(60, 60, 60));
            cancelButton.setForeground(Color.BLACK);
            
            JButton saveButton = new JButton("Save Changes");
            saveButton.setBackground(new Color(0, 102, 204));
            saveButton.setForeground(Color.BLACK);
            
            buttonPanel.add(cancelButton);
            buttonPanel.add(saveButton);
            
            // Add all panels to main panel
            mainPanel.add(titleLabel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            mainPanel.add(usernamePanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            mainPanel.add(namePanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            mainPanel.add(emailPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            mainPanel.add(registrationPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            mainPanel.add(passwordPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            mainPanel.add(statusPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            mainPanel.add(buttonPanel);
            
            dialog.add(new JScrollPane(mainPanel));
            
            // Add action listeners
            generateButton.addActionListener(e -> {
                String generatedPassword = generateRandomPassword();
                newPasswordField.setText(generatedPassword);
                confirmField.setText(generatedPassword);
                JOptionPane.showMessageDialog(dialog, 
                    "Generated password: " + generatedPassword + "\n\nMake sure to note this down to share with the librarian.",
                    "Password Generated", JOptionPane.INFORMATION_MESSAGE);
            });
            
            cancelButton.addActionListener(e -> dialog.dispose());
            
            saveButton.addActionListener(e -> {
                // Validate fields
                String newFullName = nameField.getText().trim();
                String newEmail = emailField.getText().trim();
                String newPassword = new String(newPasswordField.getPassword());
                String confirmPassword = new String(confirmField.getPassword());
                boolean isActive = activeRadio.isSelected();
                
                // Basic validation
                if (newFullName.isEmpty() || newEmail.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Full Name and Email are required fields",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Check if we're changing password
                boolean isChangingPassword = !newPassword.isEmpty() || !confirmPassword.isEmpty();
                
                if (isChangingPassword) {
                    if (!newPassword.equals(confirmPassword)) {
                        JOptionPane.showMessageDialog(dialog, 
                            "Password and confirmation do not match",
                            "Password Mismatch", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                
                // Update librarian information
                try {
                    String updateSql;
                    PreparedStatement updateStmt;
                    
                    if (isChangingPassword) {
                        updateSql = "UPDATE users SET full_name = ?, email = ?, " +
                                    "password = ?, account_status = ? WHERE username = ?";
                        updateStmt = connection.prepareStatement(updateSql);
                        updateStmt.setString(1, newFullName);
                        updateStmt.setString(2, newEmail);
                        updateStmt.setString(3, newPassword);
                        updateStmt.setString(4, isActive ? "active" : "suspended");
                        updateStmt.setString(5, username);
                    } else {
                        updateSql = "UPDATE users SET full_name = ?, email = ?, " +
                                    "account_status = ? WHERE username = ?";
                        updateStmt = connection.prepareStatement(updateSql);
                        updateStmt.setString(1, newFullName);
                        updateStmt.setString(2, newEmail);
                        updateStmt.setString(3, isActive ? "active" : "suspended");
                        updateStmt.setString(4, username);
                    }
                    
                    updateStmt.executeUpdate();
                    updateStmt.close();
                    
                    JOptionPane.showMessageDialog(dialog, 
                        "Librarian information updated successfully!",
                        "Update Successful", JOptionPane.INFORMATION_MESSAGE);
                    
                    // Refresh the table
                    loadLibrariansData();
                    
                    dialog.dispose();
                    
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Error updating librarian: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            });
            
            dialog.setVisible(true);
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading librarian data: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Show confirmation dialog and remove librarian if confirmed
     */
    private void removeLibrarian(String username, String fullName) {
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to remove librarian '" + fullName + "'?\n" +
                "This action cannot be undone.",
                "Confirm Removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                
                
                    // Warn about active transactions and ask for confirmation again
                    int secondConfirm = JOptionPane.showConfirmDialog(this, 
                            "Are you absolutely sure you want to proceed with removal?",
                            "Active Transactions Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    
                    if (secondConfirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                
                
                // Delete the librarian
                String deleteSql = "DELETE FROM users WHERE username = ? AND role = 'librarian'";
                PreparedStatement deleteStmt = connection.prepareStatement(deleteSql);
                deleteStmt.setString(1, username);
                int affected = deleteStmt.executeUpdate();
                deleteStmt.close();
                
                if (affected > 0) {
                    JOptionPane.showMessageDialog(this, 
                            "Librarian '" + fullName + "' has been removed successfully.",
                            "Librarian Removed", JOptionPane.INFORMATION_MESSAGE);
                    
                    // Refresh the data
                    loadLibrariansData();
                    loadDashboardData();
                } else {
                    JOptionPane.showMessageDialog(this, 
                            "Failed to remove librarian. The account may have already been deleted.",
                            "Removal Failed", JOptionPane.ERROR_MESSAGE);
                }
                
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, 
                        "Database error: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Filter members table based on search text
     */
    private void filterMembersTable(String searchText) {
        membersTableModel.setRowCount(0);

        try {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT u.username, u.full_name, u.email, ").append("'-' AS phone, ")
                    .append("u.registration_date, ")
                    .append("(SELECT COUNT(*) FROM borrowed_books bb WHERE bb.username = u.username AND bb.return_date IS NULL) AS borrowed, ")
                    .append("CASE WHEN EXISTS (SELECT 1 FROM borrowed_books bb WHERE bb.username = u.username AND bb.return_date IS NULL AND bb.due_date < CURRENT_DATE()) ")
                    .append("THEN 'Overdue Books' ELSE 'Active' END AS status ")
                    .append("FROM users u WHERE u.role = 'member' ");

            if (!searchText.isEmpty()) {
                sqlBuilder.append(
                        "AND (LOWER(u.username) LIKE ? OR LOWER(u.full_name) LIKE ? OR LOWER(u.email) LIKE ?) ");
            }

            sqlBuilder.append("ORDER BY u.full_name");

            PreparedStatement stmt = connection.prepareStatement(sqlBuilder.toString());

            if (!searchText.isEmpty()) {
                String searchPattern = "%" + searchText.toLowerCase() + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
                stmt.setString(3, searchPattern);
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                String fullName = rs.getString("full_name");
                String email = rs.getString("email");
                String phone = rs.getString("phone");
                String joinDate = rs.getString("registration_date");
                int borrowed = rs.getInt("borrowed");
                String status = rs.getString("status");

                membersTableModel.addRow(
                        new Object[] { username, fullName, email, phone, joinDate, borrowed, status, "View/Edit" });
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error filtering members: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Create the footer panel
     */
    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(new Color(20, 20, 20));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel versionLabel = new JLabel("BookedIn Library Management System | Version 1.0.0");
        versionLabel.setForeground(Color.GRAY);
        footerPanel.add(versionLabel, BorderLayout.WEST);
        
        JLabel copyrightLabel = new JLabel("© 2024 BookedIn Inc. All rights reserved.");
        copyrightLabel.setForeground(Color.GRAY);
        footerPanel.add(copyrightLabel, BorderLayout.EAST);
        
        return footerPanel;
    }

    /**
     * Custom table cell renderer for buttons
     */
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(new Color(60, 60, 60));
            setForeground(Color.WHITE);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setText(value.toString());
            return this;
        }
    }

    /**
     * Custom table cell editor for buttons
     */
    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;
        private JTable currentTable;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setBackground(new Color(60, 60, 60));
            button.setForeground(Color.WHITE);
            
            button.addActionListener(e -> fireEditingStopped());
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            currentRow = row;
            currentTable = table;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Determine which table is being edited and perform the appropriate action
                if (currentTable == librariansTable) {
                    handleLibrarianAction(currentRow);
                } else if (currentTable == booksTable) {
                    handleBookAction(currentRow);
                } else if (currentTable == membersTable) {
                    handleMemberAction(currentRow);
                }
            }
            isPushed = false;
            return label;
        }
        
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
        
        /**
         * Handle librarian table button click
         */
        /**
         * Handle librarian table button click
         */
        private void handleLibrarianAction(int row) {
            String username = (String) librariansTable.getValueAt(row, 0);
            String fullName = (String) librariansTable.getValueAt(row, 1);
            
            // Create a popup menu with options
            JPopupMenu menu = new JPopupMenu();
            menu.setBackground(Color.black);
            JMenuItem editItem = new JMenuItem("Edit Librarian");
            JMenuItem viewItem = new JMenuItem("View Details");
            JMenuItem deleteItem = new JMenuItem("Remove Librarian");
            
            editItem.addActionListener(e -> {
                // Show edit dialog
                showEditLibrarianDialog(username);
            });
            
            viewItem.addActionListener(e -> {
                // Show details dialog
                showLibrarianDetailsDialog(username);
            });
            
            deleteItem.addActionListener(e -> {
                removeLibrarian(username, fullName);
            });
            
            menu.add(viewItem);
            menu.add(editItem);
            menu.add(new JSeparator());
            menu.add(deleteItem);
            
            // Display the popup menu at button location
            menu.show(button, 0, button.getHeight());
        }
        
        /**
         * Handle book table button click
         */
        private void handleBookAction(int row) {
            String isbn = (String) booksTable.getValueAt(row, 0);
            String title = (String) booksTable.getValueAt(row, 1);
            
            // Create a popup menu with options
            JPopupMenu menu = new JPopupMenu();
            JMenuItem editItem = new JMenuItem("Edit Book");
            JMenuItem viewItem = new JMenuItem("View Details");
            JMenuItem addCopyItem = new JMenuItem("Add Copy");
            JMenuItem deleteItem = new JMenuItem("Delete Book");
            
            menu.add(viewItem);
            menu.add(editItem);
            menu.add(addCopyItem);
            menu.add(new JSeparator());
            menu.add(deleteItem);
            
            // Add action listeners
            viewItem.addActionListener(e -> {
                JOptionPane.showMessageDialog(button, "View details for book: " + title);
            });
            
            editItem.addActionListener(e -> {
                JOptionPane.showMessageDialog(button, "Edit book: " + title);
            });
            
            addCopyItem.addActionListener(e -> {
                JOptionPane.showMessageDialog(button, "Add copy of book: " + title);
            });
            
            deleteItem.addActionListener(e -> {
                JOptionPane.showMessageDialog(button, "Delete book: " + title);
            });
            
            // Display the popup menu at button location
            menu.show(button, 0, button.getHeight());
        }
        
        /**
         * Handle member table button click
         */
        private void handleMemberAction(int row) {
            String username = (String) membersTable.getValueAt(row, 0);
            String fullName = (String) membersTable.getValueAt(row, 1);
            
            // Create a popup menu with options
            JPopupMenu menu = new JPopupMenu();
            JMenuItem viewItem = new JMenuItem("View Profile");
            JMenuItem borrowedItem = new JMenuItem("View Borrowed Books");
            JMenuItem editItem = new JMenuItem("Edit Member");
            JMenuItem blockItem = new JMenuItem("Block Member");
            JMenuItem deleteItem = new JMenuItem("Delete Member");
            
            menu.add(viewItem);
            menu.add(borrowedItem);
            menu.add(editItem);
            menu.add(blockItem);
            menu.add(new JSeparator());
            menu.add(deleteItem);
            
            // Add action listeners
            viewItem.addActionListener(e -> {
                JOptionPane.showMessageDialog(button, "View profile for: " + fullName);
            });
            
            borrowedItem.addActionListener(e -> {
                JOptionPane.showMessageDialog(button, "View borrowed books for: " + fullName);
            });
            
            editItem.addActionListener(e -> {
                JOptionPane.showMessageDialog(button, "Edit member: " + fullName);
            });
            
            blockItem.addActionListener(e -> {
                JOptionPane.showMessageDialog(button, "Block member: " + fullName);
            });
            
            deleteItem.addActionListener(e -> {
                JOptionPane.showMessageDialog(button, "Delete member: " + fullName);
            });
            
            // Display the popup menu at button location
            menu.show(button, 0, button.getHeight());
        }
    }
    
}