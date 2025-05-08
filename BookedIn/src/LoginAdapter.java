import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/**
 * LoginAdapter class provides a modified LoginScreen that uses DatabaseSetup
 * instead of BookedInApp for database operations. This fixes the connection issues.
 */
public class LoginAdapter extends JFrame {

    private LoginScreen loginScreen;
    
    public LoginAdapter() {
        // Setup window
        setTitle("BookedIn - Login");
        setSize(350, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Create a custom login screen
        loginScreen = new LoginScreen() {
            // Override the login button's action listener
            @Override
            public void initializeLoginButton() {
                loginButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String username = usernameField.getText();
                        String password = new String(passwordField.getPassword());
                        
                        if (username.isEmpty() || password.isEmpty()) {
                            JOptionPane.showMessageDialog(LoginAdapter.this, 
                                "Username and password cannot be empty", 
                                "Login Error", 
                                JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        
                        // Use DatabaseSetup instead of BookedInApp
                        if (DatabaseSetup.validateLogin(username, password)) {
                            dispose(); // Close login screen
                            SwingUtilities.invokeLater(() -> {
                                new MemberDashboard(username).setVisible(true);
                            });
                        } else {
                            JOptionPane.showMessageDialog(LoginAdapter.this, 
                                "Invalid username or password", 
                                "Login Error", 
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
            }
        };
        
        // Copy the content from the login screen
        setContentPane(loginScreen.getContentPane());
    }
    
    // Method to show registration dialog
    public void showRegistration() {
        SwingUtilities.invokeLater(() -> {
            if (loginScreen != null) {
                loginScreen.showRegistrationDialog();
            }
        });
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            new LoginAdapter().setVisible(true);
        });
    }
}