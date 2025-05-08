import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MemberDashboard extends JFrame {

	private final String username;
	private JPanel mainContentPanel;
	private CardLayout cardLayout;
	private JLabel userNameLabel;
	private JLabel dateLabel;
	private JPanel sidebarPanel;
	private JPanel profilePanel;

	// Statistics labels
	private JLabel totalBooksCountLabel;
	private JLabel borrowedBooksCountLabel;

	// Tables
	private JTable popularBooksTable;
	private JTable recentlyViewedTable;
	private JTable dueReturnsTable;
	private JTable allBooksTable;
	private JTable myBooksTable;

	// Search components
	private JTextField searchField;
	private JComboBox<String> sortByComboBox;

	public MemberDashboard(String username) {
		this.username = username;
		setTitle("BookedIn");
		setSize(1000, 700);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setUndecorated(true);
		setResizable(false);

		// Main container with BorderLayout
		JPanel mainContainer = new JPanel(new BorderLayout());
		mainContainer.setBackground(new Color(20, 20, 20));

		// Create header
		JPanel headerPanel = createHeaderPanel();
		mainContainer.add(headerPanel, BorderLayout.NORTH);

		// Create sidebar
		sidebarPanel = createSidebarPanel();
		mainContainer.add(sidebarPanel, BorderLayout.WEST);

		// Create main content panel with CardLayout
		cardLayout = new CardLayout();
		mainContentPanel = new JPanel(cardLayout);
		mainContentPanel.setBackground(new Color(20, 20, 20));

		// Create dashboard panel
		JPanel dashboardPanel = createDashboardPanel();
		JPanel booksPanel = createBooksPanel();
		JPanel myBooksPanel = createMyBooksPanel();
		JPanel profilePanel = createProfilePanel();

		// Add panels to card layout
		mainContentPanel.add(dashboardPanel, "Dashboard");
		mainContentPanel.add(booksPanel, "Books");
		mainContentPanel.add(myBooksPanel, "MyBooks");
		mainContentPanel.add(profilePanel, "Profile");

		// Show dashboard by default
		cardLayout.show(mainContentPanel, "Dashboard");

		mainContainer.add(mainContentPanel, BorderLayout.CENTER);

		add(mainContainer);

		// Load user info
		loadUserInfo();

		// Load initial data from database
		loadStatistics();
		loadPopularBooks();
		loadDueReturns();
		loadAllBooks("");
		loadMyBorrowedBooks();
	}
	
	private JPanel createProfilePanel() {
	    JPanel profilePanel = new JPanel(new BorderLayout());
	    profilePanel.setBackground(new Color(20, 20, 20));
	    profilePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	    
	    // Header
	    JLabel headerLabel = new JLabel("My Profile");
	    headerLabel.setFont(new Font("Arial", Font.BOLD, 18));
	    headerLabel.setForeground(Color.WHITE);
	    headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
	    
	    // Main content panel with user info
	    JPanel contentPanel = new JPanel(new BorderLayout());
	    contentPanel.setBackground(new Color(30, 30, 30));
	    contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	    
	    // User info form
	    JPanel formPanel = new JPanel();
	    formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
	    formPanel.setBackground(new Color(30, 30, 30));
	    formPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
	    
	    // User details
	    JPanel detailsPanel = new JPanel(new GridLayout(0, 2, 15, 15));
	    detailsPanel.setBackground(new Color(30, 30, 30));
	    
	    // Username (read-only)
	    JLabel usernameLabel = new JLabel("Username:");
	    usernameLabel.setForeground(Color.WHITE);
	    JTextField usernameField = new JTextField(username);
	    usernameField.setEditable(false);
	    usernameField.setBackground(new Color(40, 40, 40));
	    usernameField.setForeground(Color.WHITE);
	    usernameField.setCaretColor(Color.WHITE);
	    usernameField.setBorder(BorderFactory.createCompoundBorder(
	        BorderFactory.createLineBorder(new Color(60, 60, 60)),
	        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	    
	    // Full Name
	    JLabel nameLabel = new JLabel("Full Name:");
	    nameLabel.setForeground(Color.WHITE);
	    JTextField nameField = new JTextField();
	    nameField.setBackground(new Color(40, 40, 40));
	    nameField.setForeground(Color.WHITE);
	    nameField.setCaretColor(Color.WHITE);
	    nameField.setBorder(BorderFactory.createCompoundBorder(
	        BorderFactory.createLineBorder(new Color(60, 60, 60)),
	        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	    
	    // Email
	    JLabel emailLabel = new JLabel("Email:");
	    emailLabel.setForeground(Color.WHITE);
	    JTextField emailField = new JTextField();
	    emailField.setBackground(new Color(40, 40, 40));
	    emailField.setForeground(Color.WHITE);
	    emailField.setCaretColor(Color.WHITE);
	    emailField.setBorder(BorderFactory.createCompoundBorder(
	        BorderFactory.createLineBorder(new Color(60, 60, 60)),
	        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	    
	    // Phone
	    JLabel phoneLabel = new JLabel("Phone:");
	    phoneLabel.setForeground(Color.WHITE);
	    JTextField phoneField = new JTextField();
	    phoneField.setBackground(new Color(40, 40, 40));
	    phoneField.setForeground(Color.WHITE);
	    phoneField.setCaretColor(Color.WHITE);
	    phoneField.setBorder(BorderFactory.createCompoundBorder(
	        BorderFactory.createLineBorder(new Color(60, 60, 60)),
	        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	    
	    // Add fields to panel
	    detailsPanel.add(usernameLabel);
	    detailsPanel.add(usernameField);
	    detailsPanel.add(nameLabel);
	    detailsPanel.add(nameField);
	    detailsPanel.add(emailLabel);
	    detailsPanel.add(emailField);
	    detailsPanel.add(phoneLabel);
	    detailsPanel.add(phoneField);
	    
	    // Update Profile button
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    buttonPanel.setBackground(new Color(30, 30, 30));
	    
	    JButton updateProfileButton = new JButton("Update Profile");
	    updateProfileButton.setBackground(new Color(0, 122, 204));
	    updateProfileButton.setForeground(Color.WHITE);
	    updateProfileButton.setFocusPainted(false);
	    updateProfileButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
	    
	    updateProfileButton.addActionListener(e -> {
	        updateUserProfile(nameField.getText(), emailField.getText(), phoneField.getText());
	    });
	    
	    buttonPanel.add(updateProfileButton);
	    
	    // Add everything to the form panel
	    formPanel.add(detailsPanel);
	    formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
	    formPanel.add(buttonPanel);
	    
	    // Password change section
	    JPanel passwordPanel = new JPanel();
	    passwordPanel.setLayout(new BoxLayout(passwordPanel, BoxLayout.Y_AXIS));
	    passwordPanel.setBackground(new Color(35, 35, 35));
	    passwordPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	    
	    JLabel passwordHeaderLabel = new JLabel("Change Password");
	    passwordHeaderLabel.setFont(new Font("Arial", Font.BOLD, 16));
	    passwordHeaderLabel.setForeground(Color.WHITE);
	    passwordHeaderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JPanel passwordFieldsPanel = new JPanel(new GridLayout(0, 2, 15, 15));
	    passwordFieldsPanel.setBackground(new Color(35, 35, 35));
	    
	    // Current Password
	    JLabel currentPasswordLabel = new JLabel("Current Password:");
	    currentPasswordLabel.setForeground(Color.WHITE);
	    JPasswordField currentPasswordField = new JPasswordField();
	    currentPasswordField.setBackground(new Color(40, 40, 40));
	    currentPasswordField.setForeground(Color.WHITE);
	    currentPasswordField.setCaretColor(Color.WHITE);
	    currentPasswordField.setBorder(BorderFactory.createCompoundBorder(
	        BorderFactory.createLineBorder(new Color(60, 60, 60)),
	        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	    
	    // New Password
	    JLabel newPasswordLabel = new JLabel("New Password:");
	    newPasswordLabel.setForeground(Color.WHITE);
	    JPasswordField newPasswordField = new JPasswordField();
	    newPasswordField.setBackground(new Color(40, 40, 40));
	    newPasswordField.setForeground(Color.WHITE);
	    newPasswordField.setCaretColor(Color.WHITE);
	    newPasswordField.setBorder(BorderFactory.createCompoundBorder(
	        BorderFactory.createLineBorder(new Color(60, 60, 60)),
	        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	    
	    // Confirm Password
	    JLabel confirmPasswordLabel = new JLabel("Confirm New Password:");
	    confirmPasswordLabel.setForeground(Color.WHITE);
	    JPasswordField confirmPasswordField = new JPasswordField();
	    confirmPasswordField.setBackground(new Color(40, 40, 40));
	    confirmPasswordField.setForeground(Color.WHITE);
	    confirmPasswordField.setCaretColor(Color.WHITE);
	    confirmPasswordField.setBorder(BorderFactory.createCompoundBorder(
	        BorderFactory.createLineBorder(new Color(60, 60, 60)),
	        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
	    
	    // Add password fields
	    passwordFieldsPanel.add(currentPasswordLabel);
	    passwordFieldsPanel.add(currentPasswordField);
	    passwordFieldsPanel.add(newPasswordLabel);
	    passwordFieldsPanel.add(newPasswordField);
	    passwordFieldsPanel.add(confirmPasswordLabel);
	    passwordFieldsPanel.add(confirmPasswordField);
	    
	    // Change Password button
	    JPanel passwordButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    passwordButtonPanel.setBackground(new Color(35, 35, 35));
	    
	    JButton changePasswordButton = new JButton("Change Password");
	    changePasswordButton.setBackground(new Color(0, 122, 204));
	    changePasswordButton.setForeground(Color.WHITE);
	    changePasswordButton.setFocusPainted(false);
	    changePasswordButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
	    
	    changePasswordButton.addActionListener(e -> {
	        char[] currentPass = currentPasswordField.getPassword();
	        char[] newPass = newPasswordField.getPassword();
	        char[] confirmPass = confirmPasswordField.getPassword();
	        changePassword(new String(currentPass), new String(newPass), new String(confirmPass));
	        
	        // Clear password fields for security
	        currentPasswordField.setText("");
	        newPasswordField.setText("");
	        confirmPasswordField.setText("");
	    });
	    
	    passwordButtonPanel.add(changePasswordButton);
	    
	    // Add to password panel
	    passwordPanel.add(passwordHeaderLabel);
	    passwordPanel.add(Box.createRigidArea(new Dimension(0, 15)));
	    passwordPanel.add(passwordFieldsPanel);
	    passwordPanel.add(Box.createRigidArea(new Dimension(0, 15)));
	    passwordPanel.add(passwordButtonPanel);
	    
	    // Add panels to content
	    contentPanel.add(formPanel, BorderLayout.NORTH);
	    contentPanel.add(passwordPanel, BorderLayout.CENTER);
	    
	    // Add to main panel
	    profilePanel.add(headerLabel, BorderLayout.NORTH);
	    profilePanel.add(contentPanel, BorderLayout.CENTER);
	    
	    // Load user data when panel is created
	    loadUserProfile(nameField, emailField, phoneField);
	    
	    return profilePanel;
	}
	
	private void loadUserProfile(JTextField nameField, JTextField emailField, JTextField phoneField) {
	    try {
	        Connection conn = DatabaseSetup.getConnection();
	        String query = "SELECT full_name, email, phone FROM users WHERE username = ?";
	        PreparedStatement pstmt = conn.prepareStatement(query);
	        pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();
	        
	        if (rs.next()) {
	            String fullName = rs.getString("full_name");
	            String email = rs.getString("email");
	            String phone = rs.getString("phone");
	            
	            nameField.setText(fullName != null ? fullName : "");
	            emailField.setText(email != null ? email : "");
	            phoneField.setText(phone != null ? phone : "");
	        }
	        
	        rs.close();
	        pstmt.close();
	    } catch (SQLException e) {
	        e.printStackTrace();
	        JOptionPane.showMessageDialog(this, 
	                "Error loading user profile: " + e.getMessage(), 
	                "Database Error", 
	                JOptionPane.ERROR_MESSAGE);
	    }
	}
	
	private void updateUserProfile(String fullName, String email, String phone) {
	    try {
	        // Basic validation
	        if (fullName.trim().isEmpty()) {
	            JOptionPane.showMessageDialog(this, 
	                    "Full name cannot be empty", 
	                    "Validation Error", 
	                    JOptionPane.ERROR_MESSAGE);
	            return;
	        }
	        
	        // Email validation (simple check)
	        if (!email.trim().isEmpty() && !email.contains("@")) {
	            JOptionPane.showMessageDialog(this, 
	                    "Please enter a valid email address", 
	                    "Validation Error", 
	                    JOptionPane.ERROR_MESSAGE);
	            return;
	        }
	        
	        Connection conn = DatabaseSetup.getConnection();
	        String query = "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE username = ?";
	        PreparedStatement pstmt = conn.prepareStatement(query);
	        pstmt.setString(1, fullName);
	        pstmt.setString(2, email);
	        pstmt.setString(3, phone);
	        pstmt.setString(4, username);
	        
	        int rowsAffected = pstmt.executeUpdate();
	        pstmt.close();
	        
	        if (rowsAffected > 0) {
	            JOptionPane.showMessageDialog(this, 
	                    "Profile updated successfully!", 
	                    "Success", 
	                    JOptionPane.INFORMATION_MESSAGE);
	            
	            // Update the displayed name in the header
	            userNameLabel.setText(fullName);
	            
	            // Update welcome message if on dashboard
	            Component[] components = ((JPanel) ((JPanel) mainContentPanel.getComponent(0)).getComponent(0))
	                    .getComponents();
	            JPanel welcomePanel = (JPanel) ((JPanel) components[0]).getComponent(1);
	            JLabel welcomeLabel = (JLabel) welcomePanel.getComponent(0);
	            welcomeLabel.setText("Welcome back, " + fullName + "!");
	        } else {
	            JOptionPane.showMessageDialog(this, 
	                    "No changes were made to your profile", 
	                    "Information", 
	                    JOptionPane.INFORMATION_MESSAGE);
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	        JOptionPane.showMessageDialog(this, 
	                "Error updating profile: " + e.getMessage(), 
	                "Database Error", 
	                JOptionPane.ERROR_MESSAGE);
	    }
	}
	
	private void changePassword(String currentPassword, String newPassword, String confirmPassword) {
	    // Validation
	    if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
	        JOptionPane.showMessageDialog(this, 
	                "All password fields are required", 
	                "Validation Error", 
	                JOptionPane.ERROR_MESSAGE);
	        return;
	    }
	    
	    if (!newPassword.equals(confirmPassword)) {
	        JOptionPane.showMessageDialog(this, 
	                "New password and confirmation do not match", 
	                "Validation Error", 
	                JOptionPane.ERROR_MESSAGE);
	        return;
	    }
	    
	    // Minimum password length
	    if (newPassword.length() < 6) {
	        JOptionPane.showMessageDialog(this, 
	                "New password must be at least 6 characters long", 
	                "Validation Error", 
	                JOptionPane.ERROR_MESSAGE);
	        return;
	    }
	    
	    try {
	        Connection conn = DatabaseSetup.getConnection();
	        
	        // First verify current password
	        String verifyQuery = "SELECT password FROM users WHERE username = ?";
	        PreparedStatement verifyStmt = conn.prepareStatement(verifyQuery);
	        verifyStmt.setString(1, username);
	        ResultSet rs = verifyStmt.executeQuery();
	        
	        if (rs.next()) {
	            String storedPassword = rs.getString("password");
	            
	            // Check if current password matches
	            // Note: In a real application, passwords should be hashed
	            if (!storedPassword.equals(currentPassword)) {
	                JOptionPane.showMessageDialog(this, 
	                        "Current password is incorrect", 
	                        "Authentication Error", 
	                        JOptionPane.ERROR_MESSAGE);
	                rs.close();
	                verifyStmt.close();
	                return;
	            }
	            
	            // Update password
	            String updateQuery = "UPDATE users SET password = ? WHERE username = ?";
	            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
	            updateStmt.setString(1, newPassword);
	            updateStmt.setString(2, username);
	            
	            int rowsAffected = updateStmt.executeUpdate();
	            updateStmt.close();
	            
	            if (rowsAffected > 0) {
	                JOptionPane.showMessageDialog(this, 
	                        "Password changed successfully!", 
	                        "Success", 
	                        JOptionPane.INFORMATION_MESSAGE);
	            } else {
	                JOptionPane.showMessageDialog(this, 
	                        "Failed to change password", 
	                        "Error", 
	                        JOptionPane.ERROR_MESSAGE);
	            }
	        }
	        
	        rs.close();
	        verifyStmt.close();
	    } catch (SQLException e) {
	        e.printStackTrace();
	        JOptionPane.showMessageDialog(this, 
	                "Error changing password: " + e.getMessage(), 
	                "Database Error", 
	                JOptionPane.ERROR_MESSAGE);
	    }
	}

	private JPanel createHeaderPanel() {
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(new Color(30, 30, 30));
		headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 50, 50)));
		headerPanel.setPreferredSize(new Dimension(getWidth(), 60));

		// App title
		JLabel titleLabel = new JLabel("BookedIn");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

		// User profile panel
		JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		userPanel.setBackground(new Color(30, 30, 30));

		userNameLabel = new JLabel("Eugene Esguerra");
		userNameLabel.setForeground(Color.WHITE);
		userNameLabel.setFont(new Font("Arial", Font.PLAIN, 14));

		// Create a circular avatar
		JPanel avatarPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(new Color(100, 100, 100));
				g2d.fillOval(0, 0, 40, 40);
				g2d.dispose();
			}
		};
		avatarPanel.setPreferredSize(new Dimension(40, 40));

		userPanel.add(userNameLabel);
		userPanel.add(Box.createRigidArea(new Dimension(10, 0)));

		headerPanel.add(titleLabel, BorderLayout.WEST);
		headerPanel.add(userPanel, BorderLayout.EAST);

		return headerPanel;
	}

	private JPanel createSidebarPanel() {
		JPanel sidebar = new JPanel();
		sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
		sidebar.setBackground(new Color(25, 25, 25));
		sidebar.setPreferredSize(new Dimension(180, getHeight()));
		sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(50, 50, 50)));

		// Add some padding at the top
		sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

		// Create navigation buttons
		JButton dashboardButton = createSidebarButton("Dashboard", "Dashboard");
		JButton booksButton = createSidebarButton("Books", "Books");
		JButton myBooksButton = createSidebarButton("My Books", "MyBooks");
		JButton profileButton = createSidebarButton("Profile", "Profile");
		

		// Add buttons to sidebar
		sidebar.add(dashboardButton);
		sidebar.add(Box.createRigidArea(new Dimension(0, 5)));
		sidebar.add(booksButton);
		sidebar.add(Box.createRigidArea(new Dimension(0, 5)));
		sidebar.add(myBooksButton);
		sidebar.add(Box.createRigidArea(new Dimension(0, 5)));
		sidebar.add(profileButton);

		// Add spacing
		sidebar.add(Box.createVerticalGlue());

		// Logout button at bottom
		JButton logoutButton = new JButton("Logout");
		logoutButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		logoutButton.setMaximumSize(new Dimension(180, 40));
		logoutButton.setFont(new Font("Arial", Font.PLAIN, 14));
		logoutButton.setForeground(Color.WHITE);
		logoutButton.setBackground(new Color(25, 25, 25));
		logoutButton.setBorderPainted(false);
		logoutButton.setFocusPainted(false);
		logoutButton.setHorizontalAlignment(SwingConstants.LEFT);
		logoutButton.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

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

		sidebar.add(logoutButton);
		sidebar.add(Box.createRigidArea(new Dimension(0, 20)));

		return sidebar;
	}

	private JButton createSidebarButton(String text, String cardName) {
		JButton button = new JButton(text);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setMaximumSize(new Dimension(180, 40));
		button.setFont(new Font("Arial", Font.PLAIN, 14));
		button.setForeground(Color.WHITE);
		button.setBackground(new Color(25, 25, 25));
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

		button.addActionListener(e -> cardLayout.show(mainContentPanel, cardName));

		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				button.setBackground(new Color(40, 40, 40));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				button.setBackground(new Color(25, 25, 25));
			}
		});

		return button;
	}

	private JPanel createDashboardPanel() {
		JPanel dashboardPanel = new JPanel();
		dashboardPanel.setLayout(new BorderLayout(15, 15));
		dashboardPanel.setBackground(new Color(20, 20, 20));
		dashboardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// Dashboard header
		JPanel dashboardHeader = new JPanel(new BorderLayout());
		dashboardHeader.setBackground(new Color(20, 20, 20));

		JLabel dashboardTitle = new JLabel("Dashboard Overview");
		dashboardTitle.setFont(new Font("Arial", Font.BOLD, 18));
		dashboardTitle.setForeground(Color.WHITE);

		JPanel welcomePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		welcomePanel.setBackground(new Color(20, 20, 20));
		JLabel welcomeLabel = new JLabel("Welcome back, Eugene!");
		welcomeLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		welcomeLabel.setForeground(new Color(200, 200, 200));
		welcomePanel.add(welcomeLabel);

		// Date display on right
		JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		datePanel.setBackground(new Color(20, 20, 20));
		dateLabel = new JLabel("Today is MM/DD/YYYY");
		dateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		dateLabel.setForeground(new Color(200, 200, 200));

		// Add notification bell icon
		JLabel bellIcon = new JLabel("🔔");
		bellIcon.setFont(new Font("Arial", Font.PLAIN, 18));
		bellIcon.setForeground(Color.WHITE);

		datePanel.add(dateLabel);
		datePanel.add(Box.createRigidArea(new Dimension(10, 0)));
		datePanel.add(bellIcon);

		dashboardHeader.add(dashboardTitle, BorderLayout.NORTH);
		dashboardHeader.add(welcomePanel, BorderLayout.WEST);
		dashboardHeader.add(datePanel, BorderLayout.EAST);

		// Statistics cards panel
		JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
		statsPanel.setBackground(new Color(20, 20, 20));

		// Create stat cards
		JPanel totalBooksCard = createStatCard("Total Books", "1,234", "+3 this month");
		JPanel borrowedBooksCard = createStatCard("Books Borrowed", "3", "+2 this month");
		JPanel recentlyViewedCard = createStatCard("Recently Viewed", "Physics 1st Ed.", "Jenelyn Cruz");
		JPanel myBooksCard = createStatCard("My Books", "3", "");

		statsPanel.add(totalBooksCard);
		statsPanel.add(borrowedBooksCard);
		statsPanel.add(recentlyViewedCard);
		statsPanel.add(myBooksCard);

		// Content panels
		JPanel contentPanel = new JPanel(new GridLayout(1, 2, 15, 0));
		contentPanel.setBackground(new Color(20, 20, 20));

		// Popular books panel
		JPanel popularBooksPanel = createContentPanel("Popular Books", createPopularBooksTable());

		// Due returns panel
		JPanel dueReturnsPanel = createContentPanel("Due Returns", createDueReturnsTable());

		contentPanel.add(popularBooksPanel);
		contentPanel.add(dueReturnsPanel);

		// Main layout assembly
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBackground(new Color(20, 20, 20));
		topPanel.add(dashboardHeader, BorderLayout.NORTH);
		topPanel.add(Box.createRigidArea(new Dimension(0, 20)), BorderLayout.CENTER);
		topPanel.add(statsPanel, BorderLayout.SOUTH);

		dashboardPanel.add(topPanel, BorderLayout.NORTH);
		dashboardPanel.add(contentPanel, BorderLayout.CENTER);

		// Update date
		LocalDate currentDate = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
		dateLabel.setText("Today is " + currentDate.format(formatter));

		return dashboardPanel;
	}

	private JPanel createStatCard(String title, String value, String subtitle) {
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(new Color(35, 35, 35));
		card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(new Color(200, 200, 200));
		titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel valueLabel = new JLabel(value);
		valueLabel.setForeground(Color.WHITE);
		valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
		valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel subtitleLabel = new JLabel(subtitle);
		subtitleLabel.setForeground(new Color(150, 150, 150));
		subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		card.add(titleLabel);
		card.add(Box.createRigidArea(new Dimension(0, 5)));
		card.add(valueLabel);
		if (!subtitle.isEmpty()) {
			card.add(Box.createRigidArea(new Dimension(0, 5)));
			card.add(subtitleLabel);
		}

		return card;
	}

	private JPanel createContentPanel(String title, JComponent content) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(new Color(35, 35, 35));
		panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
		titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

		panel.add(titleLabel, BorderLayout.NORTH);
		panel.add(content, BorderLayout.CENTER);

		return panel;
	}

	private JScrollPane createPopularBooksTable() {
		String[] columns = { "", "Title", "Author", "" };
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		popularBooksTable = new JTable(model);
		popularBooksTable.setBackground(new Color(35, 35, 35));
		popularBooksTable.setForeground(Color.WHITE);
		popularBooksTable.setIntercellSpacing(new Dimension(10, 5));
		popularBooksTable.setRowHeight(50);
		popularBooksTable.setShowGrid(false);
		popularBooksTable.setDefaultEditor(Object.class, null); // Make cells non-editable

		popularBooksTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int row = popularBooksTable.getSelectedRow();
					if (row >= 0) {
						String title = (String) popularBooksTable.getValueAt(row, 1);
						if (title != null && !title.isEmpty()) {
							openBookDetails(title);
						}
					}
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(popularBooksTable);
		scrollPane.setBackground(new Color(35, 35, 35));
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getViewport().setBackground(new Color(35, 35, 35));

		return scrollPane;
	}

	private JScrollPane createDueReturnsTable() {
		String[] columns = { "", "Title", "Due Date", "" };
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		dueReturnsTable = new JTable(model);
		dueReturnsTable.setBackground(new Color(35, 35, 35));
		dueReturnsTable.setForeground(Color.WHITE);
		dueReturnsTable.setIntercellSpacing(new Dimension(10, 5));
		dueReturnsTable.setRowHeight(50);
		dueReturnsTable.setShowGrid(false);
		dueReturnsTable.setDefaultEditor(Object.class, null); // Make cells non-editable

		dueReturnsTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int row = dueReturnsTable.getSelectedRow();
					if (row >= 0) {
						String title = (String) dueReturnsTable.getValueAt(row, 1);
						if (title != null && !title.isEmpty()) {
							openBookDetails(title);
						}
					}
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(dueReturnsTable);
		scrollPane.setBackground(new Color(35, 35, 35));
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getViewport().setBackground(new Color(35, 35, 35));

		return scrollPane;
	}

	private JPanel createBooksPanel() {
		JPanel booksPanel = new JPanel(new BorderLayout(15, 15));
		booksPanel.setBackground(new Color(20, 20, 20));
		booksPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// Left sidebar (search and filters)
		JPanel searchSidebar = new JPanel();
		searchSidebar.setLayout(new BoxLayout(searchSidebar, BoxLayout.Y_AXIS));
		searchSidebar.setBackground(new Color(30, 30, 30));
		searchSidebar.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		searchSidebar.setPreferredSize(new Dimension(250, 0)); // Height doesn't need to be set

		// Search field with icon
		JPanel searchFieldPanel = new JPanel(new BorderLayout()) {
		    @Override
		    public Dimension getMaximumSize() {
		        return new Dimension(220, 40); // locks max size
		    }
		};
		searchFieldPanel.setBackground(new Color(40, 40, 40));
		searchFieldPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		searchFieldPanel.setPreferredSize(new Dimension(220, 40)); // locks preferred size
		searchFieldPanel.setMaximumSize(new Dimension(220, 40));
		searchFieldPanel.setMinimumSize(new Dimension(220, 40));

		// Search icon
		JLabel searchIcon = new JLabel("🔍");
		searchIcon.setForeground(new Color(180, 180, 180));
		searchIcon.setPreferredSize(new Dimension(24, 24)); // optional

		// Search field itself
		searchField = new JTextField("Search Books...");
		searchField.setForeground(new Color(180, 180, 180));
		searchField.setBackground(new Color(40, 40, 40));
		searchField.setBorder(BorderFactory.createEmptyBorder());
		searchField.setPreferredSize(new Dimension(180, 30));
		searchField.setMaximumSize(new Dimension(180, 30));
		searchField.setMinimumSize(new Dimension(180, 30));

		// Add search behavior and focus listener (same as before)

		searchFieldPanel.add(searchIcon, BorderLayout.WEST);
		searchFieldPanel.add(searchField, BorderLayout.CENTER);


		// Remove placeholder on focus
		searchField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				if (searchField.getText().equals("Search Books...")) {
					searchField.setText("");
					searchField.setForeground(Color.WHITE);
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (searchField.getText().isEmpty()) {
					searchField.setText("Search Books...");
					searchField.setForeground(new Color(180, 180, 180));
				}
			}
		});

		// Add action listener for search
		searchField.addActionListener(e -> {
			String searchText = searchField.getText();
			if (!searchText.equals("Search Books...")) {
				loadAllBooks(searchText);
			}
		});

		// Sort by dropdown
		JPanel sortByPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		sortByPanel.setBackground(new Color(30, 30, 30));

		JLabel sortByLabel = new JLabel("Sort By:");
		sortByLabel.setForeground(Color.WHITE);

		sortByComboBox = new JComboBox<>(new String[]{"Title", "Author", "Year", "Genre"});
		sortByComboBox.setBackground(new Color(40, 40, 40));
		sortByComboBox.setForeground(Color.WHITE);

		sortByComboBox.addActionListener(e -> {
			String searchText = searchField.getText();
			if (searchText.equals("Search Books...")) {
				searchText = "";
			}
			loadAllBooks(searchText);
		});

		sortByPanel.add(sortByLabel);
		sortByPanel.add(sortByComboBox);

		// Book results container
		JPanel bookResultsPanel = new JPanel();
		bookResultsPanel.setLayout(new BoxLayout(bookResultsPanel, BoxLayout.Y_AXIS));
		bookResultsPanel.setBackground(new Color(30, 30, 30));

		JScrollPane resultsScrollPane = new JScrollPane(bookResultsPanel);
		resultsScrollPane.setBorder(BorderFactory.createEmptyBorder());
		resultsScrollPane.setBackground(new Color(30, 30, 30));
		resultsScrollPane.getViewport().setBackground(new Color(30, 30, 30));

		// Dummy table just to hold the panel reference
		allBooksTable = new JTable();
		allBooksTable.putClientProperty("resultsPanel", bookResultsPanel);

		// --- Sidebar Construction ---
		JPanel searchSidebar1 = new JPanel();
		searchSidebar1.setLayout(new BoxLayout(searchSidebar1, BoxLayout.Y_AXIS));
		searchSidebar1.setBackground(new Color(30, 30, 30));
		searchSidebar1.setPreferredSize(new Dimension(250, 0));  // Fix width
		searchSidebar1.setMaximumSize(new Dimension(250, Integer.MAX_VALUE));

		// --- Search Bar Panel ---
		searchSidebar1.add(searchFieldPanel);
		searchSidebar1.add(Box.createRigidArea(new Dimension(0, 15)));
		searchSidebar1.add(sortByPanel);
		searchSidebar1.add(Box.createRigidArea(new Dimension(0, 15)));
		searchSidebar1.add(new JSeparator());
		searchSidebar1.add(Box.createRigidArea(new Dimension(0, 15)));

		// --- Result List ScrollPane Fix ---
		JPanel resultListWrapper = new JPanel(new BorderLayout());
		resultListWrapper.setPreferredSize(new Dimension(250, 0));
		resultListWrapper.setMaximumSize(new Dimension(250, Integer.MAX_VALUE));
		resultListWrapper.setBackground(new Color(30, 30, 30));
		resultListWrapper.add(resultsScrollPane, BorderLayout.CENTER);

		searchSidebar1.add(resultListWrapper);

		// --- Book Details Panel ---
		JPanel bookDetailsPanel = new JPanel(new BorderLayout());
		bookDetailsPanel.setBackground(new Color(20, 20, 20));

		JPanel defaultView = new JPanel(new GridBagLayout());
		defaultView.setBackground(new Color(30, 30, 30));

		JLabel selectBookLabel = new JLabel("Select a book to view details");
		selectBookLabel.setFont(new Font("Arial", Font.BOLD, 18));
		selectBookLabel.setForeground(new Color(150, 150, 150));

		defaultView.add(selectBookLabel);
		bookDetailsPanel.add(defaultView, BorderLayout.CENTER);

		// --- Final Panel Assembly ---
		booksPanel.add(searchSidebar1, BorderLayout.WEST);
		booksPanel.add(bookDetailsPanel, BorderLayout.CENTER);

		return booksPanel;
	}


	private JPanel createBookResultItem(int bookId, String title, String author) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(new Color(40, 40, 40));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.setMaximumSize(new Dimension(250, 70));
		panel.putClientProperty("bookId", bookId);

		JLabel titleLabel = new JLabel(title);
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel authorLabel = new JLabel(author);
		authorLabel.setForeground(new Color(180, 180, 180));
		authorLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		authorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		panel.add(titleLabel);
		panel.add(Box.createRigidArea(new Dimension(0, 5)));
		panel.add(authorLabel);

		// Add click listener to show book details
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// Record book view
				recordBookView(bookId);
				// Show book details
				showBookDetails(bookId);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				panel.setBackground(new Color(50, 50, 50));
				panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				panel.setBackground(new Color(40, 40, 40));
			}
		});

		return panel;
	}

	private JPanel createBookDetailsView(String title, String author) {
		try {
			// Get book info from database
			Connection conn = DatabaseSetup.getConnection();
			String query = "SELECT * FROM books WHERE title = ?";
			PreparedStatement pstmt = conn.prepareStatement(query);
			pstmt.setString(1, title);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				int id = rs.getInt("id");
				int year = rs.getInt("year");
				String genre = rs.getString("genre");
				String location = rs.getString("location");
				boolean available = rs.getBoolean("available");

				// Create the panel
				JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				panel.setBackground(new Color(30, 30, 30));
				panel.setBorder(new RoundedBorder(20, new Color(30, 30, 30)));
				panel.putClientProperty("bookId", id);

				// Title area
				JPanel titlePanel = new JPanel(new BorderLayout());
				titlePanel.setBackground(new Color(30, 30, 30));
				titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

				JLabel bookTitle = new JLabel(title);
				bookTitle.setFont(new Font("Arial", Font.BOLD, 24));
				bookTitle.setForeground(Color.WHITE);

				JLabel bookAuthor = new JLabel("by " + author);
				bookAuthor.setFont(new Font("Arial", Font.PLAIN, 16));
				bookAuthor.setForeground(new Color(180, 180, 180));

				titlePanel.add(bookTitle, BorderLayout.NORTH);
				titlePanel.add(bookAuthor, BorderLayout.CENTER);

				// Checkout button
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				buttonPanel.setBackground(new Color(30, 30, 30));
				buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));

				JButton checkoutButton = new JButton(available ? "Checkout" : "Join Waitlist");
				checkoutButton.setBackground(available ? new Color(200, 30, 30) : new Color(150, 90, 0));
				checkoutButton.setForeground(Color.WHITE);
				checkoutButton.setFocusPainted(false);
				checkoutButton.setBorderPainted(false);
				checkoutButton.setFont(new Font("Arial", Font.BOLD, 14));
				checkoutButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

				// Add action for checkout/waitlist
				final int bookId = id;
				checkoutButton.addActionListener(e -> {
					if (available) {
						checkoutBook(bookId);
					} else {
						joinWaitlist(bookId);
					}
				});

				buttonPanel.add(checkoutButton);
				titlePanel.add(buttonPanel, BorderLayout.EAST);

				// Book details grid
				JPanel detailsGrid = new JPanel(new GridLayout(2, 2, 20, 20));
				detailsGrid.setBackground(new Color(30, 30, 30));
				detailsGrid.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

				// Book fields
				detailsGrid.add(createDetailField("Publication Year", String.valueOf(year)));
				detailsGrid.add(createDetailField("Genre", genre));
				detailsGrid.add(createDetailField("Status", available ? "Available" : "Checked Out"));
				detailsGrid.add(createDetailField("Location", location));

				panel.add(titlePanel, BorderLayout.NORTH);
				panel.add(detailsGrid, BorderLayout.CENTER);

				rs.close();
				pstmt.close();

				return panel;
			}

			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Fallback to default view if database query fails
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(new Color(30, 30, 30));
		panel.setBorder(new RoundedBorder(20, new Color(30, 30, 30)));

		// Title area
		JPanel titlePanel = new JPanel(new BorderLayout());
		titlePanel.setBackground(new Color(30, 30, 30));
		titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

		JLabel bookTitle = new JLabel(title);
		bookTitle.setFont(new Font("Arial", Font.BOLD, 24));
		bookTitle.setForeground(Color.WHITE);

		JLabel bookAuthor = new JLabel("by " + author);
		bookAuthor.setFont(new Font("Arial", Font.PLAIN, 16));
		bookAuthor.setForeground(new Color(180, 180, 180));

		titlePanel.add(bookTitle, BorderLayout.NORTH);
		titlePanel.add(bookAuthor, BorderLayout.CENTER);

		// Checkout button
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.setBackground(new Color(30, 30, 30));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));

		JButton checkoutButton = new JButton("Checkout");
		checkoutButton.setBackground(new Color(200, 30, 30));
		checkoutButton.setForeground(Color.WHITE);
		checkoutButton.setFocusPainted(false);
		checkoutButton.setBorderPainted(false);
		checkoutButton.setFont(new Font("Arial", Font.BOLD, 14));
		checkoutButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

		buttonPanel.add(checkoutButton);

		titlePanel.add(buttonPanel, BorderLayout.EAST);

		// Book details grid
		JPanel detailsGrid = new JPanel(new GridLayout(2, 2, 20, 20));
		detailsGrid.setBackground(new Color(30, 30, 30));
		detailsGrid.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// Book fields
		detailsGrid.add(createDetailField("Publication Year", "2019"));
		detailsGrid.add(createDetailField("Genre", "Science/Educational"));
		detailsGrid.add(createDetailField("Status", "Available"));
		detailsGrid.add(createDetailField("Location", "Second Floor, Shelf B4"));

		panel.add(titlePanel, BorderLayout.NORTH);
		panel.add(detailsGrid, BorderLayout.CENTER);

		return panel;
	}

	private JPanel createDetailField(String label, String value) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(new Color(35, 35, 35));
		panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		JLabel labelText = new JLabel(label);
		labelText.setForeground(new Color(180, 180, 180));
		labelText.setFont(new Font("Arial", Font.PLAIN, 14));
		labelText.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel valueText = new JLabel(value);
		valueText.setForeground(Color.WHITE);
		valueText.setFont(new Font("Arial", Font.BOLD, 16));
		valueText.setAlignmentX(Component.LEFT_ALIGNMENT);

		panel.add(labelText);
		panel.add(Box.createRigidArea(new Dimension(0, 5)));
		panel.add(valueText);

		return panel;
	}

	private JPanel createMyBooksPanel() {
		JPanel myBooksPanel = new JPanel(new BorderLayout());
		myBooksPanel.setBackground(new Color(20, 20, 20));
		myBooksPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		// Header
		JLabel headerLabel = new JLabel("My Books");
		headerLabel.setFont(new Font("Arial", Font.BOLD, 18));
		headerLabel.setForeground(Color.WHITE);
		headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

		// Books table
		String[] columns = { "Title", "Author", "Borrowed Date", "Due Date", "Status" };
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		myBooksTable = new JTable(model);
		myBooksTable.setBackground(new Color(30, 30, 30));
		myBooksTable.setForeground(Color.WHITE);
		myBooksTable.setGridColor(new Color(50, 50, 50));
		myBooksTable.setRowHeight(40);
		myBooksTable.getTableHeader().setBackground(new Color(40, 40, 40));
		myBooksTable.getTableHeader().setForeground(Color.WHITE);
		myBooksTable.setDefaultEditor(Object.class, null); // Make cells non-editable

		// Custom renderer for status column
		myBooksTable.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());

		// Add book ID column (hidden)
		model.addColumn("ID"); // This will be hidden

		JScrollPane tableScrollPane = new JScrollPane(myBooksTable);
		tableScrollPane.setBackground(new Color(30, 30, 30));
		tableScrollPane.getViewport().setBackground(new Color(30, 30, 30));

		// Return button panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.setBackground(new Color(20, 20, 20));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

		JButton returnButton = new JButton("Return Selected");
		returnButton.setBackground(new Color(0, 122, 204));
		returnButton.setForeground(Color.WHITE);
		returnButton.setFocusPainted(false);
		returnButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

		returnButton.addActionListener(e -> {
			int row = myBooksTable.getSelectedRow();
			if (row >= 0) {
				// Get the book ID from the hidden column (if it exists)
				if (myBooksTable.getColumnCount() > 5) {
					Object idObj = myBooksTable.getValueAt(row, 5);
					if (idObj != null) {
						int bookId = Integer.parseInt(idObj.toString());
						returnBook(bookId);
					}
				} else {
					// Fallback - get book by title
					String title = (String) myBooksTable.getValueAt(row, 0);
					returnBookByTitle(title);
				}
			} else {
				JOptionPane.showMessageDialog(this, "Please select a book to return.", "Selection Required",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});

		JButton renewButton = new JButton("Renew Selected");
		renewButton.setBackground(new Color(60, 60, 60));
		renewButton.setForeground(Color.WHITE);
		renewButton.setFocusPainted(false);
		renewButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

		renewButton.addActionListener(e -> {
			int row = myBooksTable.getSelectedRow();
			if (row >= 0) {
				// Get the book ID from the hidden column (if it exists)
				if (myBooksTable.getColumnCount() > 5) {
					Object idObj = myBooksTable.getValueAt(row, 5);
					if (idObj != null) {
						int bookId = Integer.parseInt(idObj.toString());
						renewBook(bookId);
					}
				} else {
					// Fallback - get book by title
					String title = (String) myBooksTable.getValueAt(row, 0);
					renewBookByTitle(title);
				}
			} else {
				JOptionPane.showMessageDialog(this, "Please select a book to renew.", "Selection Required",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});

		buttonPanel.add(renewButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(returnButton);

		myBooksPanel.add(headerLabel, BorderLayout.NORTH);
		myBooksPanel.add(tableScrollPane, BorderLayout.CENTER);
		myBooksPanel.add(buttonPanel, BorderLayout.SOUTH);

		return myBooksPanel;
	}

	// Custom renderer for status column in My Books table
	private class StatusRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
					column);

			if (value != null) {
				String status = value.toString();

				if (status.equals("Overdue")) {
					label.setForeground(new Color(255, 80, 80));
				} else if (status.equals("On time")) {
					label.setForeground(new Color(80, 255, 80));
				}
			}

			return label;
		}
	}

	// Custom rounded border for panels
	private class RoundedBorder extends AbstractBorder {
		private int radius;
		private Color color;

		public RoundedBorder(int radius, Color color) {
			this.radius = radius;
			this.color = color;
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setColor(color);
			g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
			g2d.dispose();
		}

		@Override
		public Insets getBorderInsets(Component c) {
			return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
		}

		@Override
		public boolean isBorderOpaque() {
			return false;
		}
	}

	// Database connection methods
	private void loadUserInfo() {
		try {
			Connection conn = DatabaseSetup.getConnection();
			String query = "SELECT full_name FROM users WHERE username = ?";
			PreparedStatement pstmt = conn.prepareStatement(query);
			pstmt.setString(1, username);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				String fullName = rs.getString("full_name");
				userNameLabel.setText(fullName);

				// Also update welcome message
				Component[] components = ((JPanel) ((JPanel) mainContentPanel.getComponent(0)).getComponent(0))
						.getComponents();
				JPanel welcomePanel = (JPanel) ((JPanel) components[0]).getComponent(1);
				JLabel welcomeLabel = (JLabel) welcomePanel.getComponent(0);
				welcomeLabel.setText("Welcome back, " + fullName + "!");
			}

			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void loadStatistics() {
		try {
			Connection conn = DatabaseSetup.getConnection();

			// Get total books count
			String totalBooksQuery = "SELECT COUNT(*) AS total_books FROM books";
			Statement totalStmt = conn.createStatement();
			ResultSet totalRs = totalStmt.executeQuery(totalBooksQuery);

			int totalBooks = 0;
			if (totalRs.next()) {
				totalBooks = totalRs.getInt("total_books");
			}
			totalRs.close();
			totalStmt.close();

			// Get borrowed books count
			String borrowedQuery = "SELECT COUNT(*) AS borrowed_books FROM borrowed_books "
					+ "WHERE username = ? AND return_date IS NULL";
			PreparedStatement borrowedStmt = conn.prepareStatement(borrowedQuery);
			borrowedStmt.setString(1, username);
			ResultSet borrowedRs = borrowedStmt.executeQuery();

			int borrowedBooks = 0;
			if (borrowedRs.next()) {
				borrowedBooks = borrowedRs.getInt("borrowed_books");
			}
			borrowedRs.close();
			borrowedStmt.close();

			// Get last viewed book - as a separate query without using LIMIT in a subquery
			String viewQuery = "SELECT b.title, b.author FROM book_views v " + "JOIN books b ON v.book_id = b.id "
					+ "WHERE v.username = ? " + "ORDER BY v.view_date DESC LIMIT 1";
			PreparedStatement viewStmt = conn.prepareStatement(viewQuery);
			viewStmt.setString(1, username);
			ResultSet viewRs = viewStmt.executeQuery();

			String lastViewedBook = null;
			String lastViewedAuthor = null;
			if (viewRs.next()) {
				lastViewedBook = viewRs.getString("title");
				lastViewedAuthor = viewRs.getString("author");
			}
			viewRs.close();
			viewStmt.close();

			// Update UI with statistics
			Component[] components = ((JPanel) ((JPanel) ((JPanel) mainContentPanel.getComponent(0)).getComponent(0))
					.getComponent(2)).getComponents();

			// Total books card (1st card)
			JPanel totalBooksCard = (JPanel) components[0];
			JLabel totalBooksValue = (JLabel) totalBooksCard.getComponent(2);
			totalBooksValue.setText(String.valueOf(totalBooks));

			// Borrowed books card (2nd card)
			JPanel borrowedBooksCard = (JPanel) components[1];
			JLabel borrowedBooksValue = (JLabel) borrowedBooksCard.getComponent(2);
			borrowedBooksValue.setText(String.valueOf(borrowedBooks));

			// Recently viewed card (3rd card)
			JPanel recentlyViewedCard = (JPanel) components[2];
			JLabel recentlyViewedValue = (JLabel) recentlyViewedCard.getComponent(2);
			JLabel recentlyViewedSubtitle = (JLabel) recentlyViewedCard.getComponent(4);

			if (lastViewedBook != null) {
				recentlyViewedValue.setText(lastViewedBook);
				recentlyViewedSubtitle.setText(lastViewedAuthor);
			} else {
				recentlyViewedValue.setText("None yet");
				recentlyViewedSubtitle.setText("");
			}

			// My books card (4th card)
			JPanel myBooksCard = (JPanel) components[3];
			JLabel myBooksValue = (JLabel) myBooksCard.getComponent(2);
			myBooksValue.setText(String.valueOf(borrowedBooks));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void loadPopularBooks() {
		try {
			Connection conn = DatabaseSetup.getConnection();
			String query = "SELECT b.id, b.title, b.author, COUNT(bb.book_id) as borrow_count " + "FROM books b "
					+ "LEFT JOIN borrowed_books bb ON b.id = bb.book_id " + "GROUP BY b.id "
					+ "ORDER BY borrow_count DESC " + "LIMIT 3";

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			// Clear existing data
			DefaultTableModel model = (DefaultTableModel) popularBooksTable.getModel();
			model.setRowCount(0);

			// Add data to table
			while (rs.next()) {
				int id = rs.getInt("id");
				String title = rs.getString("title");
				String author = rs.getString("author");
				int borrowCount = rs.getInt("borrow_count");

				model.addRow(new Object[] { "📚", title, author, "(" + borrowCount + " Borrows)" });
			}

			// Fill remaining rows if needed
			while (model.getRowCount() < 3) {
				model.addRow(new Object[] { "", "", "", "" });
			}

			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void loadDueReturns() {
		try {
			Connection conn = DatabaseSetup.getConnection();
			String query = "SELECT b.id, b.title, bb.due_date " + "FROM borrowed_books bb "
					+ "JOIN books b ON bb.book_id = b.id " + "WHERE bb.username = ? AND bb.return_date IS NULL "
					+ "ORDER BY bb.due_date ASC " + "LIMIT 3";

			PreparedStatement pstmt = conn.prepareStatement(query);
			pstmt.setString(1, username);
			ResultSet rs = pstmt.executeQuery();

			// Clear existing data
			DefaultTableModel model = (DefaultTableModel) dueReturnsTable.getModel();
			model.setRowCount(0);

			// Current date for comparison
			LocalDate currentDate = LocalDate.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

			// Add data to table
			while (rs.next()) {
				int id = rs.getInt("id");
				String title = rs.getString("title");
				Date dueDate = rs.getDate("due_date");
				LocalDate dueDateLocal = ((java.sql.Date) dueDate).toLocalDate();

				String dueDateDisplay;
				if (dueDateLocal.equals(currentDate)) {
					dueDateDisplay = "Due: Today";
				} else if (dueDateLocal.equals(currentDate.plusDays(1))) {
					dueDateDisplay = "Due: Tomorrow";
				} else {
					dueDateDisplay = "Due: " + dueDateLocal.format(formatter);
				}

				// Add a warning label if overdue
				String timeDisplay = "5:00 PM";
				if (dueDateLocal.isBefore(currentDate)) {
					dueDateDisplay = "OVERDUE";
					timeDisplay = "❗";
				}

				model.addRow(new Object[] { "📚", title, dueDateDisplay, timeDisplay });
			}

			// Fill remaining rows if needed
			while (model.getRowCount() < 3) {
				model.addRow(new Object[] { "", "", "", "" });
			}

			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void loadAllBooks(String searchQuery) {
		try {
			Connection conn = DatabaseSetup.getConnection();

			// Get the sort column
			String sortBy = (String) sortByComboBox.getSelectedItem();
			String sortColumn;
			switch (sortBy) {
			case "Author":
				sortColumn = "author";
				break;
			case "Year":
				sortColumn = "year DESC";
				break;
			case "Genre":
				sortColumn = "genre";
				break;
			default:
				sortColumn = "title";
				break;
			}

			// Create query
			String query = "SELECT id, title, author FROM books";

			// Add search condition if provided
			if (searchQuery != null && !searchQuery.isEmpty()) {
				query += " WHERE title LIKE ? OR author LIKE ? OR genre LIKE ?";
			}

			// Add sorting
			query += " ORDER BY " + sortColumn;

			PreparedStatement pstmt = conn.prepareStatement(query);

			// Set search parameters if needed
			if (searchQuery != null && !searchQuery.isEmpty()) {
				String searchPattern = "%" + searchQuery + "%";
				pstmt.setString(1, searchPattern);
				pstmt.setString(2, searchPattern);
				pstmt.setString(3, searchPattern);
			}

			ResultSet rs = pstmt.executeQuery();

			// Get the results panel
			JPanel resultsPanel = (JPanel) allBooksTable.getClientProperty("resultsPanel");
			resultsPanel.removeAll();

			// Add books to the panel
			boolean hasResults = false;
			while (rs.next()) {
				hasResults = true;
				int bookId = rs.getInt("id");
				String title = rs.getString("title");
				String author = rs.getString("author");

				JPanel bookItem = createBookResultItem(bookId, title, author);
				resultsPanel.add(bookItem);
				resultsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
			}

			// Show a message if no results
			if (!hasResults) {
				JLabel noResultsLabel = new JLabel("No books found");
				noResultsLabel.setForeground(Color.WHITE);
				noResultsLabel.setFont(new Font("Arial", Font.ITALIC, 14));
				noResultsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
				resultsPanel.add(noResultsLabel);
			}

			// Update UI
			resultsPanel.revalidate();
			resultsPanel.repaint();

			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void loadMyBorrowedBooks() {
		try {
			Connection conn = DatabaseSetup.getConnection();
			String query = "SELECT b.id, b.title, b.author, bb.borrow_date, bb.due_date, "
					+ "CASE WHEN bb.due_date < CURRENT_DATE THEN 'Overdue' ELSE 'On time' END AS status "
					+ "FROM borrowed_books bb " + "JOIN books b ON bb.book_id = b.id "
					+ "WHERE bb.username = ? AND bb.return_date IS NULL " + "ORDER BY bb.due_date ASC";

			PreparedStatement pstmt = conn.prepareStatement(query);
			pstmt.setString(1, username);
			ResultSet rs = pstmt.executeQuery();

			// Clear existing data
			DefaultTableModel model = (DefaultTableModel) myBooksTable.getModel();
			model.setRowCount(0);

			// Format for dates
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");

			// Add data to table
			while (rs.next()) {
				int id = rs.getInt("id");
				String title = rs.getString("title");
				String author = rs.getString("author");
				Date borrowDate = rs.getDate("borrow_date");
				Date dueDate = rs.getDate("due_date");
				String status = rs.getString("status");

				String borrowDateStr = ((java.sql.Date) borrowDate).toLocalDate().format(formatter);
				String dueDateStr = ((java.sql.Date) dueDate).toLocalDate().format(formatter);

				model.addRow(new Object[] { title, author, borrowDateStr, dueDateStr, status, id });
			}

			// If no borrowed books, add a message row
			if (model.getRowCount() == 0) {
				model.addRow(new Object[] { "No books borrowed", "", "", "", "", null });
			}

			// Hide the ID column
			if (myBooksTable.getColumnCount() > 5) {
				myBooksTable.getColumnModel().removeColumn(myBooksTable.getColumnModel().getColumn(5));
			}

			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void recordBookView(int bookId) {
		try {
			Connection conn = DatabaseSetup.getConnection();
			String query = "INSERT INTO book_views (book_id, username, view_date) VALUES (?, ?, NOW())";
			PreparedStatement pstmt = conn.prepareStatement(query);
			pstmt.setInt(1, bookId);
			pstmt.setString(2, username);
			pstmt.executeUpdate();
			pstmt.close();

			// Refresh statistics after viewing
			loadStatistics();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void openBookDetails(String title) {
		try {
			Connection conn = DatabaseSetup.getConnection();
			String query = "SELECT id FROM books WHERE title = ?";
			PreparedStatement pstmt = conn.prepareStatement(query);
			pstmt.setString(1, title);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				int bookId = rs.getInt("id");

				// Record view in database
				recordBookView(bookId);

				// Switch to Books panel and show details
				cardLayout.show(mainContentPanel, "Books");
				showBookDetails(bookId);
			}

			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void showBookDetails(int bookId) {
		try {
			Connection conn = DatabaseSetup.getConnection();

			// Get book details
			String query = "SELECT * FROM books WHERE id = ?";
			PreparedStatement pstmt = conn.prepareStatement(query);
			pstmt.setInt(1, bookId);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				String title = rs.getString("title");
				String author = rs.getString("author");

				// Create and display updated book details
				JPanel bookDetailsPanel = (JPanel) ((JPanel) mainContentPanel.getComponent(1)).getComponent(1);
				bookDetailsPanel.removeAll();
				bookDetailsPanel.add(createBookDetailsView(title, author), BorderLayout.CENTER);
				bookDetailsPanel.revalidate();
				bookDetailsPanel.repaint();
			}

			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void checkoutBook(int bookId) {
	    try {
	        Connection conn = DatabaseSetup.getConnection();

	        // Check if book is available
	        String checkQuery = "SELECT available FROM books WHERE id = ?";
	        PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
	        checkStmt.setInt(1, bookId);
	        ResultSet rs = checkStmt.executeQuery();

	        if (rs.next() && rs.getBoolean("available")) {
	            // Book is available, confirm checkout
	            int option = JOptionPane.showConfirmDialog(this,
	                    "Checkout this book? Due date will be 14 days from today.", "Confirm Checkout",
	                    JOptionPane.YES_NO_OPTION);

	            if (option == JOptionPane.YES_OPTION) {
	                // Start transaction
	                conn.setAutoCommit(false);

	                // Update book status to unavailable
	                String updateBookQuery = "UPDATE books SET available = 0 WHERE id = ?";
	                PreparedStatement updateBookStmt = conn.prepareStatement(updateBookQuery);
	                updateBookStmt.setInt(1, bookId);
	                updateBookStmt.executeUpdate();

	                // Create borrow record
	                String borrowQuery = "INSERT INTO borrowed_books (book_id, username, borrow_date, due_date) "
	                        + "VALUES (?, ?, CURRENT_DATE(), DATE_ADD(CURRENT_DATE(), INTERVAL 14 DAY))";
	                PreparedStatement borrowStmt = conn.prepareStatement(borrowQuery);
	                borrowStmt.setInt(1, bookId);
	                borrowStmt.setString(2, username);
	                borrowStmt.executeUpdate();

	                conn.commit(); // Commit transaction

	                // Fire event to notify other components
	                Map<String, Object> eventData = new HashMap<>();
	                eventData.put("bookId", bookId);
	                eventData.put("username", username);
	                eventData.put("action", "checkout");
	                DatabaseEventManager.getInstance().fireEvent(DatabaseEventManager.EVENT_BOOK_CHECKOUT, eventData);
	                DatabaseEventManager.getInstance().fireEvent(DatabaseEventManager.EVENT_DATA_CHANGED, null);

	                JOptionPane.showMessageDialog(this, "Book checked out successfully!", "Success",
	                        JOptionPane.INFORMATION_MESSAGE);

	                // Refresh the data
	                loadStatistics();
	                loadDueReturns();
	                loadMyBorrowedBooks();

	                // Refresh book details
	                showBookDetails(bookId);

	                updateBookStmt.close();
	                borrowStmt.close();
	            }
	        } else {
	            JOptionPane.showMessageDialog(this, "This book is no longer available.", "Error",
	                    JOptionPane.ERROR_MESSAGE);
	        }

	        rs.close();
	        checkStmt.close();

	    } catch (SQLException e) {
	        e.printStackTrace();
	        JOptionPane.showMessageDialog(this, "Error checking out book: " + e.getMessage(), "Database Error",
	                JOptionPane.ERROR_MESSAGE);
	    }
	}

	private void joinWaitlist(int bookId) {
		try {
			Connection conn = DatabaseSetup.getConnection();

			// Check if user is already on waitlist
			String checkQuery = "SELECT * FROM waitlist WHERE book_id = ? AND username = ? AND status = 'waiting'";
			PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
			checkStmt.setInt(1, bookId);
			checkStmt.setString(2, username);
			ResultSet rs = checkStmt.executeQuery();

			if (rs.next()) {
				JOptionPane.showMessageDialog(this, "You are already on the waitlist for this book.", "Waitlist",
						JOptionPane.INFORMATION_MESSAGE);
			} else {
				// Confirm waitlist join
				int option = JOptionPane.showConfirmDialog(this,
						"Join waitlist for this book? You will be notified when it becomes available.",
						"Confirm Waitlist", JOptionPane.YES_NO_OPTION);

				if (option == JOptionPane.YES_OPTION) {
					// Add user to waitlist
					String waitlistQuery = "INSERT INTO waitlist (book_id, username, request_date, status) "
							+ "VALUES (?, ?, NOW(), 'waiting')";
					PreparedStatement waitlistStmt = conn.prepareStatement(waitlistQuery);
					waitlistStmt.setInt(1, bookId);
					waitlistStmt.setString(2, username);
					waitlistStmt.executeUpdate();

					JOptionPane.showMessageDialog(this, "Added to waitlist successfully!", "Success",
							JOptionPane.INFORMATION_MESSAGE);

					waitlistStmt.close();
				}
			}

			rs.close();
			checkStmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error joining waitlist: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void returnBook(int bookId) {
		try {
			Connection conn = DatabaseSetup.getConnection();

			// Confirm return
			int option = JOptionPane.showConfirmDialog(this, "Return this book?", "Confirm Return",
					JOptionPane.YES_NO_OPTION);

			if (option == JOptionPane.YES_OPTION) {
				// Start transaction
				conn.setAutoCommit(false);

				// Update book status to available
				String updateBookQuery = "UPDATE books SET available = 1 WHERE id = ?";
				PreparedStatement updateBookStmt = conn.prepareStatement(updateBookQuery);
				updateBookStmt.setInt(1, bookId);
				updateBookStmt.executeUpdate();

				// Update borrowed book record with return date
				String updateBorrowQuery = "UPDATE borrowed_books SET return_date = CURRENT_DATE() "
						+ "WHERE book_id = ? AND username = ? AND return_date IS NULL";
				PreparedStatement updateBorrowStmt = conn.prepareStatement(updateBorrowQuery);
				updateBorrowStmt.setInt(1, bookId);
				updateBorrowStmt.setString(2, username);
				int rowsAffected = updateBorrowStmt.executeUpdate();

				if (rowsAffected > 0) {
					conn.commit(); // Commit transaction

					// Fire event to notify other components
					Map<String, Object> eventData = new HashMap<>();
					eventData.put("bookId", bookId);
					eventData.put("username", username);
					eventData.put("action", "return");
					DatabaseEventManager.getInstance().fireEvent(DatabaseEventManager.EVENT_BOOK_RETURN, eventData);
					DatabaseEventManager.getInstance().fireEvent(DatabaseEventManager.EVENT_DATA_CHANGED, null);

					JOptionPane.showMessageDialog(this, "Book returned successfully!", "Success",
							JOptionPane.INFORMATION_MESSAGE);

					// Refresh the data
					loadStatistics();
					loadDueReturns();
					loadMyBorrowedBooks();

					// Check if anyone is waiting for this book
					notifyWaitlistUsers(bookId);
				} else {
					conn.rollback();
					JOptionPane.showMessageDialog(this, "Book not found in your borrowed items.", "Error",
							JOptionPane.ERROR_MESSAGE);
				}

				// Close statements
				updateBookStmt.close();
				updateBorrowStmt.close();
				conn.setAutoCommit(true); // Reset auto-commit
			}

		} catch (SQLException e) {
			e.printStackTrace();
			try {
				Connection conn = DatabaseSetup.getConnection();
				if (conn != null) {
					conn.rollback();
					conn.setAutoCommit(true);
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			JOptionPane.showMessageDialog(this, "Error returning book: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void returnBookByTitle(String title) {
		try {
			Connection conn = DatabaseSetup.getConnection();

			// Get book ID from title
			String query = "SELECT id FROM books WHERE title = ?";
			PreparedStatement pstmt = conn.prepareStatement(query);
			pstmt.setString(1, title);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				int bookId = rs.getInt("id");
				returnBook(bookId);
			} else {
				JOptionPane.showMessageDialog(this, "Book not found: " + title, "Error", JOptionPane.ERROR_MESSAGE);
			}

			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error returning book: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void notifyWaitlistUsers(int bookId) {
		try {
			Connection conn = DatabaseSetup.getConnection();

			// Find next user in waitlist
			String findQuery = "SELECT username FROM waitlist " + "WHERE book_id = ? AND status = 'waiting' "
					+ "ORDER BY request_date ASC LIMIT 1";
			PreparedStatement findStmt = conn.prepareStatement(findQuery);
			findStmt.setInt(1, bookId);
			ResultSet rs = findStmt.executeQuery();

			if (rs.next()) {
				String waitingUser = rs.getString("username");

				// Update waitlist status to notified
				String updateQuery = "UPDATE waitlist SET status = 'notified', notification_date = NOW() "
						+ "WHERE book_id = ? AND username = ? AND status = 'waiting'";
				PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
				updateStmt.setInt(1, bookId);
				updateStmt.setString(2, waitingUser);
				updateStmt.executeUpdate();

				updateStmt.close();
			}

			rs.close();
			findStmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void renewBook(int bookId) {
	    try {
	        Connection conn = DatabaseSetup.getConnection();

	        // Check if book is eligible for renewal (not overdue)
	        String checkQuery = "SELECT due_date FROM borrowed_books "
	                + "WHERE book_id = ? AND username = ? AND return_date IS NULL";
	        PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
	        checkStmt.setInt(1, bookId);
	        checkStmt.setString(2, username);
	        ResultSet rs = checkStmt.executeQuery();

	        if (rs.next()) {
	            Date dueDate = rs.getDate("due_date");
	            Date currentDate = new Date(System.currentTimeMillis());

	            if (dueDate.before(currentDate)) {
	                JOptionPane.showMessageDialog(this,
	                        "This book is overdue and cannot be renewed. Please return it first.", "Renewal Error",
	                        JOptionPane.ERROR_MESSAGE);
	            } else {
	                // Confirm renewal
	                int option = JOptionPane.showConfirmDialog(this, "Renew this book for 14 more days?",
	                        "Confirm Renewal", JOptionPane.YES_NO_OPTION);

	                if (option == JOptionPane.YES_OPTION) {
	                    // Update due date
	                    String renewQuery = "UPDATE borrowed_books SET due_date = DATE_ADD(CURRENT_DATE(), INTERVAL 14 DAY) "
	                            + "WHERE book_id = ? AND username = ? AND return_date IS NULL";
	                    PreparedStatement renewStmt = conn.prepareStatement(renewQuery);
	                    renewStmt.setInt(1, bookId);
	                    renewStmt.setString(2, username);
	                    int rowsAffected = renewStmt.executeUpdate();

	                    if (rowsAffected > 0) {
	                        // Fire event to notify other components
	                        Map<String, Object> eventData = new HashMap<>();
	                        eventData.put("bookId", bookId);
	                        eventData.put("username", username);
	                        eventData.put("action", "renew");
	                        DatabaseEventManager.getInstance().fireEvent(DatabaseEventManager.EVENT_BOOK_RENEWAL, eventData);
	                        DatabaseEventManager.getInstance().fireEvent(DatabaseEventManager.EVENT_DATA_CHANGED, null);
	                        
	                        JOptionPane.showMessageDialog(this, "Book renewed successfully for 14 more days!",
	                                "Success", JOptionPane.INFORMATION_MESSAGE);

	                        // Refresh the data
	                        loadDueReturns();
	                        loadMyBorrowedBooks();
	                    } else {
	                        JOptionPane.showMessageDialog(this, "Failed to renew book.", "Error",
	                                JOptionPane.ERROR_MESSAGE);
	                    }

	                    renewStmt.close();
	                }
	            }
	        } else {
	            JOptionPane.showMessageDialog(this, "Book not found in your borrowed items.", "Error",
	                    JOptionPane.ERROR_MESSAGE);
	        }

	        rs.close();
	        checkStmt.close();

	    } catch (SQLException e) {
	        e.printStackTrace();
	        JOptionPane.showMessageDialog(this, "Error renewing book: " + e.getMessage(), "Database Error",
	                JOptionPane.ERROR_MESSAGE);
	    }
	}

	private void renewBookByTitle(String title) {
		try {
			Connection conn = DatabaseSetup.getConnection();

			// Get book ID from title
			String query = "SELECT id FROM books WHERE title = ?";
			PreparedStatement pstmt = conn.prepareStatement(query);
			pstmt.setString(1, title);
			ResultSet rs = pstmt.executeQuery();

			if (rs.next()) {
				int bookId = rs.getInt("id");
				renewBook(bookId);
			} else {
				JOptionPane.showMessageDialog(this, "Book not found: " + title, "Error", JOptionPane.ERROR_MESSAGE);
			}

			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error renewing book: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		SwingUtilities.invokeLater(() -> {
			new MemberDashboard("eugene").setVisible(true);
		});
	}
}

// For Book and Review classes
class Book {
	private int id;
	private String title;
	private String author;
	private String isbn;
	private int year;
	private String genre;
	private String description;
	private boolean available;
	private String location;
	private int rating;
	private int ratingCount;
	private List<Review> reviews;

	public Book(int id, String title, String author, String isbn, int year, String genre, String description) {
		this.id = id;
		this.title = title;
		this.author = author;
		this.isbn = isbn;
		this.year = year;
		this.genre = genre;
		this.description = description;
		this.available = true;
		this.rating = 0;
		this.ratingCount = 0;
		this.reviews = new ArrayList<>();
	}

	// Getters and setters
	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public String getIsbn() {
		return isbn;
	}

	public int getYear() {
		return year;
	}

	public String getGenre() {
		return genre;
	}

	public String getDescription() {
		return description;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public int getRating() {
		return rating;
	}

	public int getRatingCount() {
		return ratingCount;
	}

	public List<Review> getReviews() {
		return reviews;
	}

	public void addReview(Review review) {
		reviews.add(review);
		// Update average rating
		int sum = 0;
		for (Review r : reviews) {
			sum += r.getRating();
		}
		ratingCount = reviews.size();
		rating = ratingCount > 0 ? sum / ratingCount : 0;
	}
}

class Review {
	private String username;
	private int rating;
	private String content;
	private String date;

	public Review(String username, int rating, String content) {
		this.username = username;
		this.rating = rating;
		this.content = content;
		// Set current date
		this.date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
	}

	// Constructor with date provided (for loading from database)
	public Review(String username, int rating, String content, String date) {
		this.username = username;
		this.rating = rating;
		this.content = content;
		this.date = date;
	}

	// Getters
	public String getUsername() {
		return username;
	}

	public int getRating() {
		return rating;
	}

	public String getContent() {
		return content;
	}

	public String getDate() {
		return date;
	}
}
