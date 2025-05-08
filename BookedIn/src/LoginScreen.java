import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginScreen extends JFrame {
    
    protected JTextField usernameField;
    protected JPasswordField passwordField;
    protected JButton loginButton;
    protected JButton registerButton;
    
    public LoginScreen() {
        // Setup window
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Remove window decorations (title bar, minimize/maximize/close buttons)
        setUndecorated(true);
        
        // Optionally add a custom border to the window
        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(40, 40, 40), 1));
        
        // Add a way to close the window when undecorated
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Allow closing with Escape key
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose();
                    System.exit(0);
                }
            }
        });
        setFocusable(true);
        
        // Main panel with light background
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(20, 20, 20)); // Light gray background
        
        // Create a centered dark box for the login form
        JPanel darkBox = new JPanel(new BorderLayout());
        darkBox.setBackground(new Color(20, 20, 20));
        // Add some rounded corners with a border
        darkBox.setBorder(BorderFactory.createLineBorder(new Color(40, 40, 40), 1));
        
        // Set preferred size for the dark box
        darkBox.setPreferredSize(new Dimension(400, 500));
        
        // Logo panel
        JPanel logoPanel = createLogoPanel();
        darkBox.add(logoPanel, BorderLayout.NORTH);
        
        // Login form - centered in the dark box
        JPanel formPanel = createLoginFormPanel();
        JPanel formContainerPanel = new JPanel(new GridBagLayout());
        formContainerPanel.setBackground(new Color(20, 20, 20));
        formContainerPanel.add(formPanel);
        darkBox.add(formContainerPanel, BorderLayout.CENTER);
        
        // Add close button at the top-right corner
        JPanel closeButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        closeButtonPanel.setOpaque(false);
        JButton closeButton = new JButton("×"); // Unicode multiplication sign as an X
        closeButton.setFont(new Font("Arial", Font.BOLD, 18));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(null);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> {
            dispose();
            System.exit(0);
        });
        closeButtonPanel.add(closeButton);
        
        // Add the close button panel to the top of the main panel
        mainPanel.add(closeButtonPanel, new GridBagConstraints() {
            {
                gridx = 0;
                gridy = 0;
                anchor = GridBagConstraints.NORTHEAST;
                weightx = 1.0;
                weighty = 0.0;
            }
        });
        
        // Add the dark box to the main panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        mainPanel.add(darkBox, gbc);
        
        add(mainPanel);
        
        // Add window drag capability
        addWindowDragListener();
        
        // Initialize buttons with their action listeners
        initializeLoginButton();
        initializeRegisterButton();
    }
    
    // Add the ability to drag the window since we've removed the title bar
    private void addWindowDragListener() {
        final Point dragPoint = new Point();
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragPoint.x = e.getX();
                dragPoint.y = e.getY();
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point location = getLocation();
                setLocation(location.x + e.getX() - dragPoint.x, 
                            location.y + e.getY() - dragPoint.y);
            }
        });
    }
    
    // Method to initialize login button - can be overridden by subclasses
    public void initializeLoginButton() {
        // Add action listener to login button
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                
                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginScreen.this, 
                        "Username and password cannot be empty", 
                        "Login Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Validate login against database
                if (BookedInApp.validateLogin(username, password)) {
                    LoginScreen.this.setVisible(false);
                    LoginScreen.this.dispose();
                    dispose();

                    SwingUtilities.invokeLater(() -> {
                        try {
                            new MemberDashboard(username).setVisible(true);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                } else {
                    JOptionPane.showMessageDialog(LoginScreen.this, 
                        "Invalid username or password", 
                        "Login Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
    
    
    // Method to initialize register button - can be overridden by subclasses
    public void initializeRegisterButton() {
        // Add action listener to register button
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRegistrationDialog();
            }
        });
    }
    
    private JPanel createLogoPanel() {
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(new Color(20, 20, 20));
        logoPanel.setPreferredSize(new Dimension(400, 100));
        logoPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        
        JLabel titleLabel = new JLabel("BookedIn");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel subtitleLabel = new JLabel("Library Management System");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(180, 180, 180));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        logoPanel.add(titleLabel, BorderLayout.CENTER);
        logoPanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        return logoPanel;
    }
    
    private JPanel createLoginFormPanel() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(new Color(20, 20, 20));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 40, 40));
        formPanel.setPreferredSize(new Dimension(350, 350)); // Fixed size for the form
        
        // Username field
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setForeground(Color.WHITE);
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        usernameField = new JTextField();
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameField.setBackground(new Color(40, 40, 40));
        usernameField.setForeground(Color.WHITE);
        usernameField.setCaretColor(Color.WHITE);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        // Password field
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setForeground(Color.WHITE);
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setBackground(new Color(40, 40, 40));
        passwordField.setForeground(Color.WHITE);
        passwordField.setCaretColor(Color.WHITE);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        // Login button
        loginButton = new JButton("Login");
        loginButton.setBackground(new Color(200, 30, 30));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Divider
        JPanel divider = new JPanel();
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setBackground(new Color(60, 60, 60));
        
        // Register text and button
        JPanel registerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        registerPanel.setBackground(new Color(20, 20, 20));
        
        JLabel noAccountLabel = new JLabel("Don't have an account?");
        noAccountLabel.setForeground(new Color(180, 180, 180));
        noAccountLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        
        registerButton = new JButton("Register");
        registerButton.setBackground(null);
        registerButton.setForeground(new Color(200, 30, 30));
        registerButton.setFont(new Font("Arial", Font.BOLD, 12));
        registerButton.setBorderPainted(false);
        registerButton.setContentAreaFilled(false);
        registerButton.setFocusPainted(false);
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        registerPanel.add(noAccountLabel);
        registerPanel.add(registerButton);
        
        // Add components to form panel
        formPanel.add(usernameLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(usernameField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        formPanel.add(passwordLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        formPanel.add(passwordField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        formPanel.add(loginButton);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        formPanel.add(divider);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        formPanel.add(registerPanel);
        
        return formPanel;
    }
    
    public void showRegistrationDialog() {
        JDialog dialog = new JDialog(this, "Register New Account", true);
        dialog.setSize(400, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setUndecorated(true);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(25, 25, 25));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        
        // Title
        JLabel titleLabel = new JLabel("Create New Account");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Create a panel for the title and close button with horizontal layout
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        // Add title to the left of the header
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Create close button
        JButton closeButton = new JButton("×");  // Unicode multiplication sign as an X
        closeButton.setFont(new Font("Arial", Font.BOLD, 18));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBackground(null);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dialog.dispose());  // Close the dialog, not this
        
        // Add close button to the right of the header
        headerPanel.add(closeButton, BorderLayout.EAST);
        
        // Form fields
        JTextField fullNameField = createTextField();
        JTextField emailField = createTextField();
        JTextField usernameRegField = createTextField();
        JPasswordField passwordRegField = createPasswordField();
        JPasswordField confirmPasswordField = createPasswordField();
        
        // Register button
        JButton submitButton = new JButton("Create Account");
        submitButton.setBackground(new Color(200, 30, 30));
        submitButton.setForeground(Color.WHITE);
        submitButton.setFont(new Font("Arial", Font.BOLD, 14));
        submitButton.setFocusPainted(false);
        submitButton.setBorderPainted(false);
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fullName = fullNameField.getText();
                String email = emailField.getText();
                String username = usernameRegField.getText();
                String password = new String(passwordRegField.getPassword());
                String confirmPassword = new String(confirmPasswordField.getPassword());
                
                // Basic validation
                if (fullName.isEmpty() || email.isEmpty() || username.isEmpty() || 
                    password.isEmpty() || confirmPassword.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, 
                        "All fields are required", 
                        "Registration Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (!password.equals(confirmPassword)) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Passwords do not match", 
                        "Registration Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Register user in database using DatabaseSetup instead of BookedInApp
                if (registerUser(username, password, fullName, email)) {
                    JOptionPane.showMessageDialog(dialog, 
                        "Account created successfully!\nYou can now login.", 
                        "Registration Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, 
                        "Registration failed. Username or email may already be in use.", 
                        "Registration Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // Add components to panel
        mainPanel.add(headerPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 25)));
        
        addFormField(mainPanel, "Full Name", fullNameField);
        addFormField(mainPanel, "Email", emailField);
        addFormField(mainPanel, "Username", usernameRegField);
        addFormField(mainPanel, "Password", passwordRegField);
        addFormField(mainPanel, "Confirm Password", confirmPasswordField);
        
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(submitButton);
        
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }
    
    // Method to register a new user
    private boolean registerUser(String username, String password, String fullName, String email) {
        try {
            Connection conn = DatabaseSetup.getConnection();
            String query = "INSERT INTO users (username, password, full_name, email, role, registration_date) " +
                          "VALUES (?, ?, ?, ?, 'member', CURRENT_DATE())";
            
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, password); // In a real app, use password hashing
            pstmt.setString(3, fullName);
            pstmt.setString(4, email);
            
            int rowsAffected = pstmt.executeUpdate();
            pstmt.close();
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBackground(new Color(40, 40, 40));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        return field;
    }
    
    private JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBackground(new Color(40, 40, 40));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        return field;
    }
    
    private void addFormField(JPanel panel, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        panel.add(label);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(field);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
    }
}