import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class SignUpScreen extends JFrame {
    
    private JTextField fullNameField;
    private JTextField usernameField;
    private JTextField emailField;
    private JTextField phoneNumberField;
    private JPasswordField passwordField;
    private JPasswordField repeatPasswordField;
    
    public SignUpScreen() {
        setTitle("BookedIn - Sign Up");
        setSize(600, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main panel with dark theme
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(20, 20, 20));
        
        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(20, 20, 20));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("BookedIn");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("A Digital Library Management System");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(Color.LIGHT_GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        headerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Sign Up Form Panel
        JPanel formContainer = new JPanel();
        formContainer.setLayout(new GridBagLayout());
        formContainer.setBackground(new Color(20, 20, 20));
        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(new Color(30, 30, 30));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        JLabel signUpTitle = new JLabel("Sign Up");
        signUpTitle.setFont(new Font("Arial", Font.BOLD, 18));
        signUpTitle.setForeground(Color.WHITE);
        signUpTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Full Name field
        JPanel namePanel = createFormFieldPanel("Full Name:", fullNameField = new JTextField(20));
        
        // Username field
        JPanel usernamePanel = createFormFieldPanel("Username:", usernameField = new JTextField(20));
        
        // Email field
        JPanel emailPanel = createFormFieldPanel("Email:", emailField = new JTextField(20));
        
        // Phone Number field
        JPanel phonePanel = createFormFieldPanel("Phone Number:", phoneNumberField = new JTextField(20));
        
        // Password field
        JPanel passwordPanel = createFormFieldPanel("Password:", passwordField = new JPasswordField(20));
        
        // Repeat Password field
        JPanel repeatPanel = createFormFieldPanel("Repeat Password:", repeatPasswordField = new JPasswordField(20));
        
        // Buttons Panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonsPanel.setBackground(new Color(30, 30, 30));
        
        JButton signUpButton = new JButton("Sign Up");
        signUpButton.setBackground(new Color(0, 102, 204));
        signUpButton.setForeground(Color.WHITE);
        signUpButton.setFocusPainted(false);
        signUpButton.setPreferredSize(new Dimension(120, 30));
        
        JButton backButton = new JButton("Back");
        backButton.setBackground(new Color(60, 60, 60));
        backButton.setForeground(Color.WHITE);
        backButton.setFocusPainted(false);
        backButton.setPreferredSize(new Dimension(120, 30));
        
        buttonsPanel.add(signUpButton);
        buttonsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonsPanel.add(backButton);
        
        // Already have account panel
        JPanel existingAccountPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        existingAccountPanel.setBackground(new Color(30, 30, 30));
        
        JLabel haveAccountLabel = new JLabel("Already have an account?");
        haveAccountLabel.setForeground(Color.LIGHT_GRAY);
        
        JButton loginLink = new JButton("Login");
        loginLink.setBorderPainted(false);
        loginLink.setContentAreaFilled(false);
        loginLink.setForeground(new Color(0, 149, 246));
        loginLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        existingAccountPanel.add(haveAccountLabel);
        existingAccountPanel.add(loginLink);
        
        // Add action listeners
        signUpButton.addActionListener((ActionEvent e) -> {
            try {
                if (validateFields()) {
                    registerUser();
                    JOptionPane.showMessageDialog(this, "Registration successful! You can now login.");
                    dispose();
                    new LoginScreen().setVisible(true);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        backButton.addActionListener((ActionEvent e) -> {
            dispose();
            new WelcomeScreen().setVisible(true);
        });
        
        loginLink.addActionListener((ActionEvent e) -> {
            dispose();
            new LoginScreen().setVisible(true);
        });
        
        formPanel.add(signUpTitle);
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        formPanel.add(namePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(usernamePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(emailPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(phonePanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(passwordPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(repeatPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        formPanel.add(buttonsPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(existingAccountPanel);
        
        formContainer.add(formPanel);
        mainPanel.add(formContainer, BorderLayout.CENTER);
        
        add(mainPanel);
    }
    
    private JPanel createFormFieldPanel(String labelText, JTextField textField) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(new Color(30, 30, 30));
        
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setPreferredSize(new Dimension(120, 25));
        
        panel.add(label);
        panel.add(textField);
        
        return panel;
    }
    
    private boolean validateFields() {
        if (fullNameField.getText().trim().isEmpty() ||
                usernameField.getText().trim().isEmpty() ||
                emailField.getText().trim().isEmpty() ||
                phoneNumberField.getText().trim().isEmpty() ||
                passwordField.getPassword().length == 0 ||
                repeatPasswordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "All fields are required", 
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        String email = emailField.getText().trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address", 
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        String phone = phoneNumberField.getText().trim();
        if (!phone.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this, "Phone number must be 10 digits", 
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        String password = new String(passwordField.getPassword());
        String repeatPassword = new String(repeatPasswordField.getPassword());
        
        if (!password.equals(repeatPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match", 
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    private void registerUser() throws Exception {
        try {
            Connection conn = BookedInApp.getConnection();
            String query = "INSERT INTO members (full_name, username, email, phone, password) VALUES (?, ?, ?, ?, ?)";
            
            PreparedStatement pst = conn.prepareStatement(query);
            pst.setString(1, fullNameField.getText().trim());
            pst.setString(2, usernameField.getText().trim());
            pst.setString(3, emailField.getText().trim());
            pst.setString(4, phoneNumberField.getText().trim());
            pst.setString(5, new String(passwordField.getPassword())); // In a real app, hash the password
            
            pst.executeUpdate();
            pst.close();
        } catch (Exception e) {
            throw new Exception("Registration failed: " + e.getMessage());
        }
    }
}