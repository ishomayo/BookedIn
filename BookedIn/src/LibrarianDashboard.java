import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class LibrarianDashboard extends JFrame implements DatabaseEventListener {

	private Timer refreshTimer;

	// Database connection constants
	private static final String DB_URL = "jdbc:mysql://localhost:3306/BookedIN";
	private static final String DB_USER = "root";
	private static final String DB_PASSWORD = "";
	
	private DefaultTableModel activitiesTableModel;

	private String librarianUsername;
	private JTabbedPane tabbedPane;
	private JPanel dashboardPanel, circulationPanel, catalogPanel, membersPanel;
	private Connection connection;

	// Dashboard stats components
	private JLabel totalBooksLabel, borrowedBooksLabel, overdueBooksLabel;
	private JLabel activeMembersLabel;

	// Tables
	private JTable booksTable, membersTable, borrowedBooksTable;
	private DefaultTableModel booksTableModel, membersTableModel, borrowedBooksTableModel;

	// Search components
	private JTextField bookSearchField, memberSearchField;
	private JComboBox<String> bookCategoryFilter;

	/**
	 * Constructor for the LibrarianDashboard
	 */
	public LibrarianDashboard(String username) {
		this.librarianUsername = username;

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
		setTitle("BookedIn - Librarian Dashboard");
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
		circulationPanel = createCirculationPanel();
		catalogPanel = createCatalogPanel();
		membersPanel = createMembersPanel();

		// Add tabs
		tabbedPane.addTab("Dashboard", dashboardPanel);
		tabbedPane.addTab("Circulation", circulationPanel);
		tabbedPane.addTab("Book Catalog", catalogPanel);
		tabbedPane.addTab("Members", membersPanel);

		mainPanel.add(tabbedPane, BorderLayout.CENTER);

		// Footer panel
		JPanel footerPanel = createFooterPanel();
		mainPanel.add(footerPanel, BorderLayout.SOUTH);

		add(mainPanel);

		// Load initial data
		loadDashboardData();
		loadBooksData();
		loadMembersData();
		loadBorrowedBooksData();

		// Register for database events
		DatabaseEventManager eventManager = DatabaseEventManager.getInstance();
		eventManager.addListener(DatabaseEventManager.EVENT_BOOK_CHECKOUT, this);
		eventManager.addListener(DatabaseEventManager.EVENT_BOOK_RETURN, this);
		eventManager.addListener(DatabaseEventManager.EVENT_BOOK_RENEWAL, this);
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
				eventManager.removeListener(DatabaseEventManager.EVENT_BOOK_CHECKOUT, LibrarianDashboard.this);
				eventManager.removeListener(DatabaseEventManager.EVENT_BOOK_RETURN, LibrarianDashboard.this);
				eventManager.removeListener(DatabaseEventManager.EVENT_BOOK_RENEWAL, LibrarianDashboard.this);
				eventManager.removeListener(DatabaseEventManager.EVENT_DATA_CHANGED, LibrarianDashboard.this);
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
	        System.out.println("LibrarianDashboard received event: " + eventType);
	        
	        if (eventType.equals(DatabaseEventManager.EVENT_BOOK_RETURN)) {
	            // A book was returned, refresh relevant data
	            refreshCirculationData();
	            
	            // Show a notification if desired
	            Map<String, Object> data = (Map<String, Object>) event.getData();
	            if (data != null) {
	                String username = (String) data.get("username");
	                int bookId = (int) data.get("bookId");
	                showBookReturnNotification(username, bookId);
	            }
	        }
	        else if (eventType.equals(DatabaseEventManager.EVENT_BOOK_CHECKOUT)) {
	            // A book was checked out, refresh relevant data
	            refreshCirculationData();
	        }
	        else if (eventType.equals(DatabaseEventManager.EVENT_BOOK_RENEWAL)) {
	            // A book loan was renewed, refresh relevant data
	            refreshCirculationData();
	        }
	        else if (eventType.equals(DatabaseEventManager.EVENT_DATA_CHANGED)) {
	            // Generic data change event, refresh all data
	            refreshData();
	        }
	    });
	}

	/**
	 * Show a notification about a book return
	 */
	private void showBookReturnNotification(String username, int bookId) {
	    try {
	        // Get book title
	        String bookTitle = "";
	        String query = "SELECT title FROM books WHERE id = ?";
	        PreparedStatement pstmt = connection.prepareStatement(query);
	        pstmt.setInt(1, bookId);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            bookTitle = rs.getString("title");
	        }
	        rs.close();
	        pstmt.close();
	        
	        // Create and show the notification
	        JDialog notification = new JDialog(this, "Book Return Notification", false);
	        notification.setSize(300, 150);
	        notification.setLocationRelativeTo(this);
	        
	        JPanel panel = new JPanel(new BorderLayout());
	        panel.setBackground(new Color(40, 40, 40));
	        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
	        
	        JLabel messageLabel = new JLabel("<html>Book returned by " + username + ":<br>" + bookTitle + "</html>");
	        messageLabel.setForeground(Color.WHITE);
	        
	        JButton okButton = new JButton("OK");
	        okButton.setBackground(new Color(60, 60, 60));
	        okButton.setForeground(Color.WHITE);
	        okButton.addActionListener(e -> notification.dispose());
	        
	        panel.add(messageLabel, BorderLayout.CENTER);
	        
	        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	        buttonPanel.setBackground(new Color(40, 40, 40));
	        buttonPanel.add(okButton);
	        
	        panel.add(buttonPanel, BorderLayout.SOUTH);
	        
	        notification.add(panel);
	        notification.setVisible(true);
	        
	        // Auto-close after 5 seconds
	        Timer closeTimer = new Timer(5000, e -> notification.dispose());
	        closeTimer.setRepeats(false);
	        closeTimer.start();
	        
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}

	/**
	 * Refresh circulation data (borrowed books)
	 */
	private void refreshCirculationData() {
	    loadBorrowedBooksData();
	    loadDashboardData(); // Also refresh dashboard stats
	}

	/**
	 * Refresh all dashboard data
	 */
	private void refreshData() {
	    loadDashboardData();
	    loadBooksData(); 
	    loadMembersData();
	    loadBorrowedBooksData();
	    
	    // Use the directly stored reference
	    if (activitiesTableModel != null) {
	        loadRecentActivities(activitiesTableModel);
	    }
	}


	/**
	 * Connect to the database
	 */
	private void connectToDatabase() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.cj.jdbc.Driver");
		connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
		System.out.println("Connected to database");
	}


	/**
	 * Create the header panel
	 */
	private JPanel createHeaderPanel() {
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBackground(new Color(20, 20, 20));
		headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

		// Left side - Title
		JLabel titleLabel = new JLabel("BookedIn Librarian");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
		titleLabel.setForeground(Color.WHITE);
		headerPanel.add(titleLabel, BorderLayout.WEST);

		// Right side - Librarian info and logout
		JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		rightPanel.setBackground(new Color(20, 20, 20));

		// Get librarian's full name from database
		String librarianName = getLibrarianName();

		JLabel librarianLabel = new JLabel("Librarian: " + librarianName);
		librarianLabel.setForeground(Color.LIGHT_GRAY);

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

		rightPanel.add(librarianLabel);
		rightPanel.add(Box.createRigidArea(new Dimension(15, 0)));
		rightPanel.add(profileButton);
		rightPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		rightPanel.add(logoutButton);

		headerPanel.add(rightPanel, BorderLayout.EAST);

		return headerPanel;
	}

	private void showProfileDialog() {
		try {
			// Get librarian info
			String sql = "SELECT * FROM users WHERE username = ?";
			PreparedStatement stmt = connection.prepareStatement(sql);
			stmt.setString(1, librarianUsername);
			ResultSet rs = stmt.executeQuery();

			if (!rs.next()) {
				JOptionPane.showMessageDialog(this, "Error loading librarian profile", "Profile Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			String fullName = rs.getString("full_name");
			String email = rs.getString("email");
			String registrationDate = rs.getString("registration_date");

			rs.close();
			stmt.close();

			// Create profile dialog
			JDialog dialog = new JDialog(this, "Librarian Profile", true);
			dialog.setSize(400, 500);
			dialog.setLocationRelativeTo(this);
			dialog.setResizable(false);
			dialog.setUndecorated(true);
			
			JPanel mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
			mainPanel.setBackground(new Color(30, 30, 30));
			mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

			JLabel titleLabel = new JLabel("Librarian Profile");
			titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
			titleLabel.setForeground(Color.WHITE);
			titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

			// Username field (read-only)
			JPanel usernamePanel = createFormField("Username:", 100);
			JTextField usernameField = (JTextField) usernamePanel.getComponent(1);
			usernameField.setText(librarianUsername);
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
						checkStmt.setString(1, librarianUsername);
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
						updateStmt.setString(4, librarianUsername);
					} else {
						updateSql = "UPDATE users SET full_name = ?, email = ? WHERE username = ?";
						updateStmt = connection.prepareStatement(updateSql);
						updateStmt.setString(1, newFullName);
						updateStmt.setString(2, newEmail);
						updateStmt.setString(3, librarianUsername);
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
			JOptionPane.showMessageDialog(this, "Error loading librarian profile: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * Refresh the header with the updated librarian name
	 */
	private void refreshHeaderName(String newName) {
		JPanel headerPanel = (JPanel) ((BorderLayout) getContentPane().getLayout())
				.getLayoutComponent(BorderLayout.NORTH);
		JPanel rightPanel = (JPanel) ((BorderLayout) headerPanel.getLayout()).getLayoutComponent(BorderLayout.EAST);
		JLabel librarianLabel = (JLabel) rightPanel.getComponent(0);
		librarianLabel.setText("Librarian: " + newName);
	}

	/**
	 * Custom renderer for buttons in tables
	 */
	private class ButtonRenderer extends JButton implements TableCellRenderer {
		public ButtonRenderer() {
			setOpaque(true);
			setBorderPainted(false);
			setBackground(new Color(60, 60, 60));
			setForeground(Color.WHITE);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			setText(value.toString());
			return this;
		}
	}

	/**
	 * Create the dashboard panel
	 */
	/**
	 * Create the dashboard panel with absolute positioning
	 */
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
	    
	    JLabel titleLabel = new JLabel("Dashboard Overview");
	    titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
	    titleLabel.setForeground(Color.WHITE);
	    
	    JLabel dateLabel = new JLabel(new SimpleDateFormat("EEEE, MMMM d, yyyy").format(new Date()));
	    dateLabel.setFont(new Font("Arial", Font.PLAIN, 14));
	    dateLabel.setForeground(Color.LIGHT_GRAY);
	    
	    titlePanel.add(titleLabel, BorderLayout.WEST);
	    titlePanel.add(dateLabel, BorderLayout.EAST);
	    
	    panel.add(titlePanel);
	    
	    // Define the stats cards section with absolute positioning
	    int cardWidth = 270;
	    int cardHeight = 150; // Taller cards
	    int cardSpacing = 20;
	    int startY = 70; // Start below header
	    int startX = 20; // Left margin
	    
	    // Books Card - using the same structure as before
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
	    
	    // Quick actions panel
	    JPanel quickActionsPanel = new JPanel();
	    quickActionsPanel.setLayout(new BoxLayout(quickActionsPanel, BoxLayout.X_AXIS));
	    quickActionsPanel.setBackground(new Color(30, 30, 30));
	    quickActionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    quickActionsPanel.setBounds(startX, startY + cardHeight + 20, 1160, 50);
	    
	    JButton checkOutButton = createActionButton("Check Out Book", new Color(0, 120, 215));
	    JButton returnBookButton = createActionButton("Return Book", new Color(0, 153, 51));
	    JButton addBookButton = createActionButton("Add New Book", new Color(204, 102, 0));
	    JButton manageMembersButton = createActionButton("Manage Members", new Color(102, 0, 204));
	    
	    checkOutButton.addActionListener(e -> tabbedPane.setSelectedIndex(1));
	    returnBookButton.addActionListener(e -> tabbedPane.setSelectedIndex(1));
	    addBookButton.addActionListener(e -> showAddBookDialog());
	    manageMembersButton.addActionListener(e -> tabbedPane.setSelectedIndex(3));
	    
	    quickActionsPanel.add(checkOutButton);
	    quickActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
	    quickActionsPanel.add(returnBookButton);
	    quickActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
	    quickActionsPanel.add(addBookButton);
	    quickActionsPanel.add(Box.createRigidArea(new Dimension(10, 0)));
	    quickActionsPanel.add(manageMembersButton);
	    
	    panel.add(quickActionsPanel);
	    
	    // Recent activities panel
	    JPanel activitiesPanel = new JPanel(new BorderLayout());
	    activitiesPanel.setBackground(new Color(30, 30, 30));
	    activitiesPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
	    activitiesPanel.setBounds(startX, startY + cardHeight + 60, 1160, 350);
	    
	    JLabel activitiesLabel = new JLabel("Recent Library Activities");
	    activitiesLabel.setFont(new Font("Arial", Font.BOLD, 16));
	    activitiesLabel.setForeground(Color.WHITE);
	    activitiesPanel.add(activitiesLabel, BorderLayout.NORTH);
	    
	    String[] columns = { "Activity", "Member", "Book", "Date", "Time" };
	    DefaultTableModel activitiesModel = new DefaultTableModel(columns, 0) {
	        @Override
	        public boolean isCellEditable(int row, int column) {
	            return false;
	        }
	    };
	    // Store this as a class field
	    this.activitiesTableModel = activitiesModel;

	    JTable activitiesTable = new JTable(activitiesModel);
	    activitiesTable.setBackground(new Color(40, 40, 40));
	    activitiesTable.setForeground(Color.WHITE);
	    activitiesTable.setGridColor(new Color(60, 60, 60));
	    activitiesTable.getTableHeader().setBackground(new Color(50, 50, 50));
	    activitiesTable.getTableHeader().setForeground(Color.BLACK);
	    activitiesTable.setRowHeight(25);
	    
	    JScrollPane scrollPane = new JScrollPane(activitiesTable);
	    scrollPane.getViewport().setBackground(new Color(40, 40, 40));
	    
	    activitiesPanel.add(scrollPane, BorderLayout.CENTER);
	    panel.add(activitiesPanel);
	    
	    // Load initial data from database
	    loadDashboardData();
	    loadRecentActivities(activitiesModel);
	    
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

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
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
		return button;
	}


	/**
	 * Create the circulation panel for checkout/return operations
	 */
	private JPanel createCirculationPanel() {
	    // Main panel with null layout for absolute positioning
	    JPanel panel = new JPanel(null);
	    panel.setBackground(new Color(30, 30, 30));
	    
	    // Get the full width available for the panel
	    int panelWidth = 1200;
	    
	    // Header panel
	    JPanel headerPanel = new JPanel(new BorderLayout());
	    headerPanel.setBackground(new Color(30, 30, 30));
	    headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
	    headerPanel.setBounds(0, 0, panelWidth, 60);
	    
	    JLabel titleLabel = new JLabel("Circulation");
	    titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
	    titleLabel.setForeground(Color.WHITE);
	    headerPanel.add(titleLabel, BorderLayout.WEST);
	    
	    panel.add(headerPanel);
	    
	    // Define panel dimensions and spacing with more height
	    int panelWidth1 = 350;
	    int panelHeight = 350;
	    int panelSpacing = 25;
	    int startY = 70;
	    
	    // Calculate X positions to center the three panels
	    int totalWidth = (panelWidth1 * 3) + (panelSpacing * 2);
	    int startX = (panelWidth - totalWidth) / 2;
	    
	    // Check Out Panel
	    JPanel checkOutPanel = new JPanel();
	    checkOutPanel.setLayout(new BoxLayout(checkOutPanel, BoxLayout.Y_AXIS));
	    checkOutPanel.setBackground(new Color(40, 40, 40));
	    checkOutPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
	    
	    // Set position and size for check out panel - centered
	    JPanel checkOutWrapper = new JPanel(new BorderLayout());
	    checkOutWrapper.setBackground(new Color(40, 40, 40));
	    checkOutWrapper.setBounds(startX, startY, panelWidth1, panelHeight);
	    checkOutWrapper.add(checkOutPanel, BorderLayout.CENTER);
	    
	    JLabel checkOutLabel = new JLabel("Check Out Book");
	    checkOutLabel.setFont(new Font("Arial", Font.BOLD, 16));
	    checkOutLabel.setForeground(Color.WHITE);
	    checkOutLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JPanel memberIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
	    memberIdPanel.setBackground(new Color(40, 40, 40));
	    memberIdPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JLabel memberIdLabel = new JLabel("Member ID:");
	    memberIdLabel.setForeground(Color.WHITE);
	    memberIdLabel.setPreferredSize(new Dimension(100, 25));
	    
	    JTextField memberIdField = new JTextField(15);
	    
	    JButton browseMembers = new JButton("Browse...");
	    browseMembers.setBackground(new Color(60, 60, 60));
	    browseMembers.setForeground(Color.BLACK);
	    browseMembers.setPreferredSize(new Dimension(90, 25));
	    
	    memberIdPanel.add(memberIdLabel);
	    memberIdPanel.add(memberIdField);
	    memberIdPanel.add(Box.createRigidArea(new Dimension(5, 0)));
	    memberIdPanel.add(browseMembers);
	    
	    JPanel bookIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
	    bookIdPanel.setBackground(new Color(40, 40, 40));
	    bookIdPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JLabel bookIdLabel = new JLabel("Book ISBN:");
	    bookIdLabel.setForeground(Color.WHITE);
	    bookIdLabel.setPreferredSize(new Dimension(100, 25));
	    
	    JTextField bookIdField = new JTextField(15);
	    
	    JButton browseBooks = new JButton("Browse...");
	    browseBooks.setBackground(new Color(60, 60, 60));
	    browseBooks.setForeground(Color.BLACK);
	    browseBooks.setPreferredSize(new Dimension(90, 25));
	    
	    bookIdPanel.add(bookIdLabel);
	    bookIdPanel.add(bookIdField);
	    bookIdPanel.add(Box.createRigidArea(new Dimension(5, 0)));
	    bookIdPanel.add(browseBooks);
	    
	    JPanel dueDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
	    dueDatePanel.setBackground(new Color(40, 40, 40));
	    dueDatePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JLabel dueDateLabel = new JLabel("Due Date:");
	    dueDateLabel.setForeground(Color.WHITE);
	    dueDateLabel.setPreferredSize(new Dimension(100, 25));
	    
	    // Create date spinner for due date (default: current date + 14 days)
	    Calendar calendar = Calendar.getInstance();
	    calendar.add(Calendar.DAY_OF_MONTH, 14);
	    Date initialDate = calendar.getTime();
	    
	    SpinnerDateModel dateModel = new SpinnerDateModel(initialDate, null, null, Calendar.DAY_OF_MONTH);
	    JSpinner dueDateSpinner = new JSpinner(dateModel);
	    JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dueDateSpinner, "yyyy-MM-dd");
	    dueDateSpinner.setEditor(dateEditor);
	    dueDateSpinner.setPreferredSize(new Dimension(200, 25));
	    
	    dueDatePanel.add(dueDateLabel);
	    dueDatePanel.add(dueDateSpinner);
	    
	    JButton checkOutButton = new JButton("Check Out");
	    checkOutButton.setBackground(new Color(0, 102, 204));
	    checkOutButton.setForeground(Color.BLACK);
	    checkOutButton.setFocusPainted(false);
	    checkOutButton.setAlignmentX(Component.LEFT_ALIGNMENT);
	    checkOutButton.setPreferredSize(new Dimension(120, 30));
	    checkOutButton.setMaximumSize(new Dimension(120, 30));
	    
	    // Add action listener for check out button
	    checkOutButton.addActionListener(e -> {
	        String memberID = memberIdField.getText().trim();
	        String bookISBN = bookIdField.getText().trim();
	        Date dueDate = (Date) dueDateSpinner.getValue();

	        if (memberID.isEmpty() || bookISBN.isEmpty()) {
	            JOptionPane.showMessageDialog(this, "Please enter both Member ID and Book ISBN", "Missing Information",
	                    JOptionPane.WARNING_MESSAGE);
	            return;
	        }

	        // Check out the book
	        checkOutBook(memberID, bookISBN, dueDate);

	        // Clear fields after check out
	        memberIdField.setText("");
	        bookIdField.setText("");
	        calendar.setTime(new Date());
	        calendar.add(Calendar.DAY_OF_MONTH, 14);
	        dueDateSpinner.setValue(calendar.getTime());
	    });
	    
	    // Improved spacing
	    checkOutPanel.add(Box.createRigidArea(new Dimension(0, 5)));
	    checkOutPanel.add(checkOutLabel);
	    checkOutPanel.add(Box.createRigidArea(new Dimension(0, 15)));
	    checkOutPanel.add(memberIdPanel);
	    checkOutPanel.add(Box.createRigidArea(new Dimension(0, 10)));
	    checkOutPanel.add(bookIdPanel);
	    checkOutPanel.add(Box.createRigidArea(new Dimension(0, 10)));
	    checkOutPanel.add(dueDatePanel);
	    checkOutPanel.add(Box.createRigidArea(new Dimension(0, 15)));
	    checkOutPanel.add(checkOutButton);
	    
	    // Check In Panel - positioned in center
	    JPanel checkInPanel = new JPanel();
	    checkInPanel.setLayout(new BoxLayout(checkInPanel, BoxLayout.Y_AXIS));
	    checkInPanel.setBackground(new Color(40, 40, 40));
	    checkInPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
	    
	    // Wrapper with absolute positioning - centered
	    JPanel checkInWrapper = new JPanel(new BorderLayout());
	    checkInWrapper.setBackground(new Color(40, 40, 40));
	    checkInWrapper.setBounds(startX + panelWidth1 + panelSpacing, startY, panelWidth1, panelHeight);
	    checkInWrapper.add(checkInPanel, BorderLayout.CENTER);
	    
	    JLabel checkInLabel = new JLabel("Return Book");
	    checkInLabel.setFont(new Font("Arial", Font.BOLD, 16));
	    checkInLabel.setForeground(Color.WHITE);
	    checkInLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JPanel bookIsbnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
	    bookIsbnPanel.setBackground(new Color(40, 40, 40));
	    bookIsbnPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JLabel bookIsbnLabel = new JLabel("Book ISBN:");
	    bookIsbnLabel.setForeground(Color.WHITE);
	    bookIsbnLabel.setPreferredSize(new Dimension(100, 25));
	    
	    JTextField bookIsbnField = new JTextField(15);
	    
	    JButton browseBorrowedBooks = new JButton("Browse...");
	    browseBorrowedBooks.setBackground(new Color(60, 60, 60));
	    browseBorrowedBooks.setForeground(Color.BLACK);
	    browseBorrowedBooks.setPreferredSize(new Dimension(90, 25));
	    
	    bookIsbnPanel.add(bookIsbnLabel);
	    bookIsbnPanel.add(bookIsbnField);
	    bookIsbnPanel.add(Box.createRigidArea(new Dimension(5, 0)));
	    bookIsbnPanel.add(browseBorrowedBooks);
	    
	    JPanel conditionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
	    conditionPanel.setBackground(new Color(40, 40, 40));
	    conditionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JLabel conditionLabel = new JLabel("Condition:");
	    conditionLabel.setForeground(Color.WHITE);
	    conditionLabel.setPreferredSize(new Dimension(100, 25));
	    
	    JComboBox<String> conditionCombo = new JComboBox<>(new String[] { "Good", "Fair", "Poor", "Damaged" });
	    conditionCombo.setPreferredSize(new Dimension(150, 25));
	    
	    conditionPanel.add(conditionLabel);
	    conditionPanel.add(conditionCombo);
	    
	    JPanel finePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
	    finePanel.setBackground(new Color(40, 40, 40));
	    finePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JLabel fineLabel = new JLabel("Fine:");
	    fineLabel.setForeground(Color.WHITE);
	    fineLabel.setPreferredSize(new Dimension(100, 25));
	    
	    JSpinner fineSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 1000.0, 0.5));
	    fineSpinner.setPreferredSize(new Dimension(70, 25));
	    
	    JLabel currencyLabel = new JLabel("$");
	    currencyLabel.setForeground(Color.WHITE);
	    
	    finePanel.add(fineLabel);
	    finePanel.add(currencyLabel);
	    finePanel.add(fineSpinner);
	    
	    JButton checkInButton = new JButton("Return Book");
	    checkInButton.setBackground(new Color(0, 102, 204));
	    checkInButton.setForeground(Color.BLACK);
	    checkInButton.setFocusPainted(false);
	    checkInButton.setAlignmentX(Component.LEFT_ALIGNMENT);
	    checkInButton.setPreferredSize(new Dimension(120, 30));
	    checkInButton.setMaximumSize(new Dimension(120, 30));
	    
	    // Add action listener for return book button
	    checkInButton.addActionListener(e -> {
	        String bookISBN = bookIsbnField.getText().trim();
	        String condition = (String) conditionCombo.getSelectedItem();
	        double fine = (Double) fineSpinner.getValue();

	        if (bookISBN.isEmpty()) {
	            JOptionPane.showMessageDialog(this, "Please enter Book ISBN", "Missing Information",
	                    JOptionPane.WARNING_MESSAGE);
	            return;
	        }

	        // Return the book
	        returnBook(bookISBN, condition, fine);

	        // Clear fields after return
	        bookIsbnField.setText("");
	        conditionCombo.setSelectedIndex(0);
	        fineSpinner.setValue(0.0);
	    });
	    
	    // Improved spacing
	    checkInPanel.add(Box.createRigidArea(new Dimension(0, 5)));
	    checkInPanel.add(checkInLabel);
	    checkInPanel.add(Box.createRigidArea(new Dimension(0, 15)));
	    checkInPanel.add(bookIsbnPanel);
	    checkInPanel.add(Box.createRigidArea(new Dimension(0, 10)));
	    checkInPanel.add(conditionPanel);
	    checkInPanel.add(Box.createRigidArea(new Dimension(0, 10)));
	    checkInPanel.add(finePanel);
	    checkInPanel.add(Box.createRigidArea(new Dimension(0, 15)));
	    checkInPanel.add(checkInButton);
	    
	    // Renew Panel - positioned on right
	    JPanel renewPanel = new JPanel();
	    renewPanel.setLayout(new BoxLayout(renewPanel, BoxLayout.Y_AXIS));
	    renewPanel.setBackground(new Color(40, 40, 40));
	    renewPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
	    
	    // Wrapper with absolute positioning - centered
	    JPanel renewWrapper = new JPanel(new BorderLayout());
	    renewWrapper.setBackground(new Color(40, 40, 40));
	    renewWrapper.setBounds(startX + (panelWidth1 + panelSpacing) * 2, startY, panelWidth1, panelHeight);
	    renewWrapper.add(renewPanel, BorderLayout.CENTER);
	    
	    JLabel renewLabel = new JLabel("Renew Book");
	    renewLabel.setFont(new Font("Arial", Font.BOLD, 16));
	    renewLabel.setForeground(Color.WHITE);
	    renewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JPanel renewMemberPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
	    renewMemberPanel.setBackground(new Color(40, 40, 40));
	    renewMemberPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JLabel renewMemberLabel = new JLabel("Member ID:");
	    renewMemberLabel.setForeground(Color.WHITE);
	    renewMemberLabel.setPreferredSize(new Dimension(100, 25));
	    
	    JTextField renewMemberField = new JTextField(15);
	    
	    JButton browseMembersRenew = new JButton("Browse...");
	    browseMembersRenew.setBackground(new Color(60, 60, 60));
	    browseMembersRenew.setForeground(Color.BLACK);
	    browseMembersRenew.setPreferredSize(new Dimension(90, 25));
	    
	    renewMemberPanel.add(renewMemberLabel);
	    renewMemberPanel.add(renewMemberField);
	    renewMemberPanel.add(Box.createRigidArea(new Dimension(5, 0)));
	    renewMemberPanel.add(browseMembersRenew);
	    
	    JPanel renewBookPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
	    renewBookPanel.setBackground(new Color(40, 40, 40));
	    renewBookPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JLabel renewBookLabel = new JLabel("Book ISBN:");
	    renewBookLabel.setForeground(Color.WHITE);
	    renewBookLabel.setPreferredSize(new Dimension(100, 25));
	    
	    JTextField renewBookField = new JTextField(15);
	    
	    JButton browseBooksRenew = new JButton("Browse...");
	    browseBooksRenew.setBackground(new Color(60, 60, 60));
	    browseBooksRenew.setForeground(Color.BLACK);
	    browseBooksRenew.setPreferredSize(new Dimension(90, 25));
	    
	    renewBookPanel.add(renewBookLabel);
	    renewBookPanel.add(renewBookField);
	    renewBookPanel.add(Box.createRigidArea(new Dimension(5, 0)));
	    renewBookPanel.add(browseBooksRenew);
	    
	    JPanel newDueDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
	    newDueDatePanel.setBackground(new Color(40, 40, 40));
	    newDueDatePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JLabel newDueDateLabel = new JLabel("New Due Date:");
	    newDueDateLabel.setForeground(Color.WHITE);
	    newDueDateLabel.setPreferredSize(new Dimension(100, 25));
	    
	    // Create date spinner for new due date
	    Calendar renewCalendar = Calendar.getInstance();
	    renewCalendar.add(Calendar.DAY_OF_MONTH, 14);
	    Date renewInitialDate = renewCalendar.getTime();
	    
	    SpinnerDateModel renewDateModel = new SpinnerDateModel(renewInitialDate, null, null, Calendar.DAY_OF_MONTH);
	    JSpinner renewDueDateSpinner = new JSpinner(renewDateModel);
	    JSpinner.DateEditor renewDateEditor = new JSpinner.DateEditor(renewDueDateSpinner, "yyyy-MM-dd");
	    renewDueDateSpinner.setEditor(renewDateEditor);
	    renewDueDateSpinner.setPreferredSize(new Dimension(200, 25));
	    
	    newDueDatePanel.add(newDueDateLabel);
	    newDueDatePanel.add(renewDueDateSpinner);
	    
	    JButton renewButton = new JButton("Renew Book");
	    renewButton.setBackground(new Color(0, 102, 204));
	    renewButton.setForeground(Color.BLACK);
	    renewButton.setFocusPainted(false);
	    renewButton.setAlignmentX(Component.LEFT_ALIGNMENT);
	    renewButton.setPreferredSize(new Dimension(120, 30));
	    renewButton.setMaximumSize(new Dimension(120, 30));
	    
	    // Add action listener for renew button
	    renewButton.addActionListener(e -> {
	        String memberID = renewMemberField.getText().trim();
	        String bookISBN = renewBookField.getText().trim();
	        Date newDueDate = (Date) renewDueDateSpinner.getValue();

	        if (memberID.isEmpty() || bookISBN.isEmpty()) {
	            JOptionPane.showMessageDialog(this, "Please enter both Member ID and Book ISBN", "Missing Information",
	                    JOptionPane.WARNING_MESSAGE);
	            return;
	        }

	        // Renew the book
	        renewBook(memberID, bookISBN, newDueDate);

	        // Clear fields after renewal
	        renewMemberField.setText("");
	        renewBookField.setText("");
	        renewCalendar.setTime(new Date());
	        renewCalendar.add(Calendar.DAY_OF_MONTH, 14);
	        renewDueDateSpinner.setValue(renewCalendar.getTime());
	    });
	    
	    // Improved spacing
	    renewPanel.add(Box.createRigidArea(new Dimension(0, 5)));
	    renewPanel.add(renewLabel);
	    renewPanel.add(Box.createRigidArea(new Dimension(0, 15)));
	    renewPanel.add(renewMemberPanel);
	    renewPanel.add(Box.createRigidArea(new Dimension(0, 10)));
	    renewPanel.add(renewBookPanel);
	    renewPanel.add(Box.createRigidArea(new Dimension(0, 10)));
	    renewPanel.add(newDueDatePanel);
	    renewPanel.add(Box.createRigidArea(new Dimension(0, 15)));
	    renewPanel.add(renewButton);
	    
	    // Add all wrapper panels to the main panel
	    panel.add(checkOutWrapper);
	    panel.add(checkInWrapper);
	    panel.add(renewWrapper);
	    
	    // Current loans list with absolute positioning - centered
	    JPanel loansPanel = new JPanel(new BorderLayout());
	    loansPanel.setBackground(new Color(30, 30, 30));
	    loansPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
	    
	    // Position loans panel below the operation panels - centered
	    int loansWidth = 1100;
	    int loansX = (panelWidth - loansWidth) / 2;
	    loansPanel.setBounds(loansX, startY + panelHeight, loansWidth, 200);
	    
	    JPanel loansHeaderPanel = new JPanel(new BorderLayout());
	    loansHeaderPanel.setBackground(new Color(30, 30, 30));
	    loansHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
	    
	    JLabel loansLabel = new JLabel("Current Loans");
	    loansLabel.setFont(new Font("Arial", Font.BOLD, 16));
	    loansLabel.setForeground(Color.WHITE);
	    
	    JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	    filterPanel.setBackground(new Color(30, 30, 30));
	    
	    JLabel filterLabel = new JLabel("Filter:");
	    filterLabel.setForeground(Color.WHITE);
	    
	    JComboBox<String> statusFilter = new JComboBox<>(new String[] { "All Loans", "On Time", "Overdue" });
	    statusFilter.setBackground(new Color(60, 60, 60));
	    statusFilter.setForeground(Color.WHITE);
	    
	    JTextField loanSearchField = new JTextField(15);
	    JButton loanSearchButton = new JButton("Search");
	    loanSearchButton.setBackground(new Color(60, 60, 60));
	    loanSearchButton.setForeground(Color.BLACK);
	    
	    filterPanel.add(filterLabel);
	    filterPanel.add(statusFilter);
	    filterPanel.add(Box.createRigidArea(new Dimension(10, 0)));
	    filterPanel.add(loanSearchField);
	    filterPanel.add(loanSearchButton);
	    
	    loansHeaderPanel.add(loansLabel, BorderLayout.WEST);
	    loansHeaderPanel.add(filterPanel, BorderLayout.EAST);
	    
	    loansPanel.add(loansHeaderPanel, BorderLayout.NORTH);
	    
	    String[] columns = { "Loan ID", "Member ID", "Member Name", "Book ISBN", "Book Title", "Checkout Date",
	            "Due Date", "Status", "Actions" };
	    
	    borrowedBooksTableModel = new DefaultTableModel(columns, 0) {
	        @Override
	        public boolean isCellEditable(int row, int column) {
	            return column == 8; // Only actions column is editable
	        }
	    };
	    
	    borrowedBooksTable = new JTable(borrowedBooksTableModel);
	    borrowedBooksTable.setBackground(new Color(40, 40, 40));
	    borrowedBooksTable.setForeground(Color.WHITE);
	    borrowedBooksTable.setGridColor(new Color(60, 60, 60));
	    borrowedBooksTable.getTableHeader().setBackground(new Color(50, 50, 50));
	    borrowedBooksTable.getTableHeader().setForeground(Color.BLACK);
	    borrowedBooksTable.setRowHeight(30);
	    
	    // Set preferred column widths
	    TableColumnModel columnModel = borrowedBooksTable.getColumnModel();
	    columnModel.getColumn(0).setPreferredWidth(70);   // Loan ID
	    columnModel.getColumn(1).setPreferredWidth(100);  // Member ID
	    columnModel.getColumn(2).setPreferredWidth(150);  // Member Name
	    columnModel.getColumn(3).setPreferredWidth(100);  // Book ISBN
	    columnModel.getColumn(4).setPreferredWidth(200);  // Book Title
	    columnModel.getColumn(5).setPreferredWidth(100);  // Checkout Date
	    columnModel.getColumn(6).setPreferredWidth(100);  // Due Date
	    columnModel.getColumn(7).setPreferredWidth(80);   // Status
	    columnModel.getColumn(8).setPreferredWidth(100);  // Actions
	    
	    // Set up renderers for different status colors
	    borrowedBooksTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
	        @Override
	        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
	                boolean hasFocus, int row, int column) {
	            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	            
	            if (isSelected) {
	                c.setBackground(new Color(70, 70, 80));
	            } else {
	                c.setBackground(new Color(40, 40, 40));
	                
	                // Color rows based on status
	                String status = (String) table.getValueAt(row, 7);
	                if (status.equals("Overdue")) {
	                    c.setForeground(new Color(255, 100, 100));
	                } else {
	                    c.setForeground(Color.WHITE);
	                }
	            }
	            
	            // Left-align the text
	            ((DefaultTableCellRenderer) c).setHorizontalAlignment(SwingConstants.LEFT);
	            
	            return c;
	        }
	    });
	    
	    // Setup the "Actions" column with buttons
	    TableColumn actionsColumn = borrowedBooksTable.getColumnModel().getColumn(8);
	    actionsColumn.setCellRenderer(new ButtonRenderer());
	    actionsColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
	    
	    // Add selection listener
	    borrowedBooksTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
	        @Override
	        public void valueChanged(ListSelectionEvent e) {
	            if (!e.getValueIsAdjusting() && borrowedBooksTable.getSelectedRow() >= 0) {
	                int row = borrowedBooksTable.getSelectedRow();
	                String bookISBN = (String) borrowedBooksTable.getValueAt(row, 3);
	                String memberID = (String) borrowedBooksTable.getValueAt(row, 1);
	                
	                // Fill the return book fields
	                bookIsbnField.setText(bookISBN);
	                
	                // Fill the renew book fields
	                renewMemberField.setText(memberID);
	                renewBookField.setText(bookISBN);
	            }
	        }
	    });
	    
	    JScrollPane scrollPane = new JScrollPane(borrowedBooksTable);
	    scrollPane.getViewport().setBackground(new Color(40, 40, 40));
	    
	    loansPanel.add(scrollPane, BorderLayout.CENTER);
	    
	    panel.add(loansPanel);
	    
	    // Configure browse buttons actions
	    browseMembers.addActionListener(e -> showMemberBrowser(memberIdField));
	    browseBooks.addActionListener(e -> showBookBrowser(bookIdField));
	    browseBorrowedBooks.addActionListener(e -> showBorrowedBookBrowser(bookIsbnField));
	    browseMembersRenew.addActionListener(e -> showMemberBrowser(renewMemberField));
	    browseBooksRenew.addActionListener(e -> showBorrowedBookBrowser(renewBookField));
	    
	    // Configure search functionality
	    loanSearchButton.addActionListener(e -> {
	        String searchText = loanSearchField.getText().trim();
	        String statusText = (String) statusFilter.getSelectedItem();
	        filterBorrowedBooksTable(searchText, statusText);
	    });
	    
	    statusFilter.addActionListener(e -> {
	        String searchText = loanSearchField.getText().trim();
	        String statusText = (String) statusFilter.getSelectedItem();
	        filterBorrowedBooksTable(searchText, statusText);
	    });
	    
	    // Adjust panel size to accommodate taller components
	    panel.setPreferredSize(new Dimension(panelWidth, startY + panelHeight + 300));
	    
	    return panel;
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
	    
	    // Add action listeners
	    addButton.addActionListener(e -> showAddBookDialog());
	    
	    addCopyButton.addActionListener(e -> {
	        int selectedRow = booksTable.getSelectedRow();
	        if (selectedRow >= 0) {
	            String isbn = (String) booksTable.getValueAt(selectedRow, 0);
	            String title = (String) booksTable.getValueAt(selectedRow, 1);
	            addBookCopy(isbn, title);
	        } else {
	            JOptionPane.showMessageDialog(this, "Please select a book to add a copy", "No Book Selected",
	                    JOptionPane.WARNING_MESSAGE);
	        }
	    });
	    
	    manageGenresButton.addActionListener(e -> showManageGenresDialog());
	    
	    // Configure search functionality
	    searchButton.addActionListener(e -> {
	        String searchText = bookSearchField.getText().trim();
	        String category = (String) bookCategoryFilter.getSelectedItem();
	        filterBooksTable(searchText, category);
	    });
	    
	    bookCategoryFilter.addActionListener(e -> {
	        String searchText = bookSearchField.getText().trim();
	        String category = (String) bookCategoryFilter.getSelectedItem();
	        filterBooksTable(searchText, category);
	    });
	    
	    // Load genre categories from database
	    loadBookGenres();
	    
	    return panel;
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
	    
	    // Add action listeners
	    addButton.addActionListener(e -> showAddMemberDialog());
	    
	    refreshButton.addActionListener(e -> loadMembersData());
	    
	    // Configure search functionality
	    searchButton.addActionListener(e -> {
	        String searchText = memberSearchField.getText().trim();
	        filterMembersTable(searchText);
	    });
	    
	    return panel;
	}
	
	

	/**
	 * Create the footer panel
	 */
	private JPanel createFooterPanel() {
		JPanel footerPanel = new JPanel(new BorderLayout());
		footerPanel.setBackground(new Color(20, 20, 20));
		footerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

		JLabel copyrightLabel = new JLabel("© 2025 BookedIn Library Management System");
		copyrightLabel.setForeground(Color.LIGHT_GRAY);
		footerPanel.add(copyrightLabel, BorderLayout.WEST);

		JLabel versionLabel = new JLabel("v1.0.0");
		versionLabel.setForeground(Color.LIGHT_GRAY);
		footerPanel.add(versionLabel, BorderLayout.EAST);

		return footerPanel;
	}

	/**
	 * Load recent activities for the dashboard
	 */
	private void loadRecentActivities(DefaultTableModel model) {
		model.setRowCount(0);

		try {
			String sql = "SELECT 'Book Borrowed' as activity, u.username, b.title, bb.borrow_date, TIME(bb.borrow_date) as time "
					+ "FROM borrowed_books bb " + "JOIN users u ON bb.username = u.username "
					+ "JOIN books b ON bb.book_id = b.id " + "WHERE bb.return_date IS NULL " + "UNION "
					+ "SELECT 'Book Returned' as activity, u.username, b.title, bb.return_date, TIME(bb.return_date) as time "
					+ "FROM borrowed_books bb " + "JOIN users u ON bb.username = u.username "
					+ "JOIN books b ON bb.book_id = b.id " + "WHERE bb.return_date IS NOT NULL " + "UNION "
					+ "SELECT 'Book Viewed' as activity, u.username, b.title, bv.view_date, TIME(bv.view_date) as time "
					+ "FROM book_views bv " + "JOIN users u ON bv.username = u.username "
					+ "JOIN books b ON bv.book_id = b.id " + "ORDER BY 4 DESC LIMIT 10";

			PreparedStatement stmt = connection.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a");

			while (rs.next()) {
				String activity = rs.getString("activity");
				String username = rs.getString("username");
				String title = rs.getString("title");
				String date = dateFormat.format(rs.getTimestamp(4));
				String time = timeFormat.format(rs.getTimestamp(4));

				model.addRow(new Object[] { activity, username, title, date, time });
			}

			rs.close();
			stmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get librarian's name from database
	 */
	private String getLibrarianName() {
		try {
			String sql = "SELECT full_name FROM users WHERE username = ?";
			PreparedStatement stmt = connection.prepareStatement(sql);
			stmt.setString(1, librarianUsername);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				return rs.getString("full_name");
			}

			rs.close();
			stmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return librarianUsername;
	}

	/**
	 * Load books data from database
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
			e.printStackTrace();
		}
	}

	/**
	 * Load members data from database
	 */
	private void loadMembersData() {
		membersTableModel.setRowCount(0);

		try {
			String sql = "SELECT u.username, u.full_name, u.email, " + "'-' AS phone, " + // Placeholder for phone (not
																							// in database)
					"u.registration_date, "
					+ "(SELECT COUNT(*) FROM borrowed_books bb WHERE bb.username = u.username AND bb.return_date IS NULL) AS borrowed, "
					+ "CASE WHEN EXISTS (SELECT 1 FROM borrowed_books bb WHERE bb.username = u.username AND bb.return_date IS NULL AND bb.due_date < CURRENT_DATE()) "
					+ "THEN 'Overdue Books' ELSE 'Active' END AS status " + "FROM users u " + "WHERE u.role = 'member' "
					+ "ORDER BY u.full_name";

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
			e.printStackTrace();
		}
	}

	/**
	 * Load borrowed books data from database
	 */
	private void loadBorrowedBooksData() {
		borrowedBooksTableModel.setRowCount(0);

		try {
			String sql = "SELECT bb.id, bb.username, u.full_name, b.isbn, b.title, " + "bb.borrow_date, bb.due_date, "
					+ "CASE WHEN bb.due_date < CURRENT_DATE() THEN 'Overdue' ELSE 'On Time' END AS status "
					+ "FROM borrowed_books bb " + "JOIN users u ON bb.username = u.username "
					+ "JOIN books b ON bb.book_id = b.id " + "WHERE bb.return_date IS NULL " + "ORDER BY bb.due_date";

			PreparedStatement stmt = connection.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				int id = rs.getInt("id");
				String username = rs.getString("username");
				String fullName = rs.getString("full_name");
				String isbn = rs.getString("isbn");
				String title = rs.getString("title");
				String borrowDate = rs.getString("borrow_date");
				String dueDate = rs.getString("due_date");
				String status = rs.getString("status");

				borrowedBooksTableModel.addRow(new Object[] { id, username, fullName, isbn, title, borrowDate, dueDate,
						status, "Return/Renew" });
			}

			rs.close();
			stmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load book genres from database for the filter dropdown
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
	 * Filter books table based on search text and category
	 */
	private void filterBooksTable(String searchText, String category) {
		booksTableModel.setRowCount(0);

		try {
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("SELECT isbn, title, author, genre, year, ")
					.append("(SELECT COUNT(*) FROM books b2 WHERE b2.isbn = b.isbn) AS copies, ")
					.append("(SELECT COUNT(*) FROM books b2 WHERE b2.isbn = b.isbn AND b2.available = true) AS available ")
					.append("FROM books b WHERE 1=1 ");

			if (!searchText.isEmpty()) {
				sqlBuilder.append("AND (LOWER(title) LIKE ? OR LOWER(author) LIKE ? OR LOWER(isbn) LIKE ?) ");
			}

			if (category != null && !category.equals("All Genres")) {
				sqlBuilder.append("AND genre = ? ");
			}

			sqlBuilder.append("GROUP BY isbn ORDER BY title");

			PreparedStatement stmt = connection.prepareStatement(sqlBuilder.toString());

			int paramIndex = 1;
			if (!searchText.isEmpty()) {
				String searchPattern = "%" + searchText.toLowerCase() + "%";
				stmt.setString(paramIndex++, searchPattern);
				stmt.setString(paramIndex++, searchPattern);
				stmt.setString(paramIndex++, searchPattern);
			}

			if (category != null && !category.equals("All Genres")) {
				stmt.setString(paramIndex, category);
			}

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
			e.printStackTrace();
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
			e.printStackTrace();
		}
	}

	/**
	 * Filter borrowed books table based on search text and status
	 */
	private void filterBorrowedBooksTable(String searchText, String statusFilter) {
		borrowedBooksTableModel.setRowCount(0);

		try {
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("SELECT bb.id, bb.username, u.full_name, b.isbn, b.title, ")
					.append("bb.borrow_date, bb.due_date, ")
					.append("CASE WHEN bb.due_date < CURRENT_DATE() THEN 'Overdue' ELSE 'On Time' END AS status ")
					.append("FROM borrowed_books bb ").append("JOIN users u ON bb.username = u.username ")
					.append("JOIN books b ON bb.book_id = b.id ").append("WHERE bb.return_date IS NULL ");

			if (!searchText.isEmpty()) {
				sqlBuilder.append(
						"AND (LOWER(u.username) LIKE ? OR LOWER(u.full_name) LIKE ? OR LOWER(b.title) LIKE ? OR LOWER(b.isbn) LIKE ?) ");
			}

			if (statusFilter.equals("On Time")) {
				sqlBuilder.append("AND bb.due_date >= CURRENT_DATE() ");
			} else if (statusFilter.equals("Overdue")) {
				sqlBuilder.append("AND bb.due_date < CURRENT_DATE() ");
			}

			sqlBuilder.append("ORDER BY bb.due_date");

			PreparedStatement stmt = connection.prepareStatement(sqlBuilder.toString());

			if (!searchText.isEmpty()) {
				String searchPattern = "%" + searchText.toLowerCase() + "%";
				stmt.setString(1, searchPattern);
				stmt.setString(2, searchPattern);
				stmt.setString(3, searchPattern);
				stmt.setString(4, searchPattern);
			}

			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				int id = rs.getInt("id");
				String username = rs.getString("username");
				String fullName = rs.getString("full_name");
				String isbn = rs.getString("isbn");
				String title = rs.getString("title");
				String borrowDate = rs.getString("borrow_date");
				String dueDate = rs.getString("due_date");
				String status = rs.getString("status");

				borrowedBooksTableModel.addRow(new Object[] { id, username, fullName, isbn, title, borrowDate, dueDate,
						status, "Return/Renew" });
			}

			rs.close();
			stmt.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check out a book to a member
	 */
	private void checkOutBook(String memberID, String bookISBN, Date dueDate) {
		try {
			// First, check if the member exists
			String memberSql = "SELECT username FROM users WHERE username = ? AND role = 'member'";
			PreparedStatement memberStmt = connection.prepareStatement(memberSql);
			memberStmt.setString(1, memberID);
			ResultSet memberRs = memberStmt.executeQuery();

			if (!memberRs.next()) {
				JOptionPane.showMessageDialog(this, "Member ID does not exist or is not a valid member",
						"Invalid Member", JOptionPane.WARNING_MESSAGE);
				return;
			}

			memberRs.close();
			memberStmt.close();

			// Check if the book exists and is available
			String bookSql = "SELECT id FROM books WHERE isbn = ? AND available = true LIMIT 1";
			PreparedStatement bookStmt = connection.prepareStatement(bookSql);
			bookStmt.setString(1, bookISBN);
			ResultSet bookRs = bookStmt.executeQuery();

			if (!bookRs.next()) {
				JOptionPane.showMessageDialog(this, "Book ISBN does not exist or no copies are available",
						"Book Not Available", JOptionPane.WARNING_MESSAGE);
				return;
			}

			int bookId = bookRs.getInt("id");
			bookRs.close();
			bookStmt.close();

			// Update the book as unavailable
			String updateBookSql = "UPDATE books SET available = false WHERE id = ?";
			PreparedStatement updateBookStmt = connection.prepareStatement(updateBookSql);
			updateBookStmt.setInt(1, bookId);
			updateBookStmt.executeUpdate();
			updateBookStmt.close();

			// Create the borrowed_books record
			String borrowSql = "INSERT INTO borrowed_books (book_id, username, borrow_date, due_date) VALUES (?, ?, CURRENT_DATE(), ?)";
			PreparedStatement borrowStmt = connection.prepareStatement(borrowSql);
			borrowStmt.setInt(1, bookId);
			borrowStmt.setString(2, memberID);
			borrowStmt.setDate(3, new java.sql.Date(dueDate.getTime()));
			borrowStmt.executeUpdate();
			borrowStmt.close();

			JOptionPane.showMessageDialog(this, "Book checked out successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);

			// Refresh data
			loadDashboardData();
			loadBorrowedBooksData();
			loadBooksData();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Return a book
	 */
	private void returnBook(String bookISBN, String condition, double fine) {
		try {
			// Find the active loan for this book
			String loanSql = "SELECT bb.id, bb.book_id FROM borrowed_books bb " + "JOIN books b ON bb.book_id = b.id "
					+ "WHERE b.isbn = ? AND bb.return_date IS NULL LIMIT 1";
			PreparedStatement loanStmt = connection.prepareStatement(loanSql);
			loanStmt.setString(1, bookISBN);
			ResultSet loanRs = loanStmt.executeQuery();

			if (!loanRs.next()) {
				JOptionPane.showMessageDialog(this, "No active loan found for this book ISBN", "Loan Not Found",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			int loanId = loanRs.getInt("id");
			int bookId = loanRs.getInt("book_id");
			loanRs.close();
			loanStmt.close();

			// Update the borrowed_books record with return date
			String updateLoanSql = "UPDATE borrowed_books SET return_date = CURRENT_DATE() WHERE id = ?";
			PreparedStatement updateLoanStmt = connection.prepareStatement(updateLoanSql);
			updateLoanStmt.setInt(1, loanId);
			updateLoanStmt.executeUpdate();
			updateLoanStmt.close();

			// Update the book as available
			String updateBookSql = "UPDATE books SET available = true WHERE id = ?";
			PreparedStatement updateBookStmt = connection.prepareStatement(updateBookSql);
			updateBookStmt.setInt(1, bookId);
			updateBookStmt.executeUpdate();
			updateBookStmt.close();

			// If the book is damaged, we could add logic to handle that
			if (condition.equals("Damaged") || condition.equals("Poor")) {
				// In a real app, you might update a book_condition table or similar
				JOptionPane.showMessageDialog(this,
						"Book condition is " + condition + ". It may need maintenance or replacement.",
						"Book Condition Warning", JOptionPane.WARNING_MESSAGE);
			}

			// Handle fine if applicable
			if (fine > 0) {
				// In a real app, you would record the fine in a fines table
				JOptionPane.showMessageDialog(this, "A fine of $" + String.format("%.2f", fine) + " has been recorded.",
						"Fine Applied", JOptionPane.INFORMATION_MESSAGE);
			}

			JOptionPane.showMessageDialog(this, "Book returned successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);

			// Refresh data
			loadDashboardData();
			loadBorrowedBooksData();
			loadBooksData();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Renew a book loan
	 */
	private void renewBook(String memberID, String bookISBN, Date newDueDate) {
		try {
			// Find the active loan for this book and member
			String loanSql = "SELECT bb.id FROM borrowed_books bb " + "JOIN books b ON bb.book_id = b.id "
					+ "WHERE b.isbn = ? AND bb.username = ? AND bb.return_date IS NULL LIMIT 1";
			PreparedStatement loanStmt = connection.prepareStatement(loanSql);
			loanStmt.setString(1, bookISBN);
			loanStmt.setString(2, memberID);
			ResultSet loanRs = loanStmt.executeQuery();

			if (!loanRs.next()) {
				JOptionPane.showMessageDialog(this, "No active loan found for this book and member", "Loan Not Found",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			int loanId = loanRs.getInt("id");
			loanRs.close();
			loanStmt.close();

			// Update the due date
			String updateLoanSql = "UPDATE borrowed_books SET due_date = ? WHERE id = ?";
			PreparedStatement updateLoanStmt = connection.prepareStatement(updateLoanSql);
			updateLoanStmt.setDate(1, new java.sql.Date(newDueDate.getTime()));
			updateLoanStmt.setInt(2, loanId);
			updateLoanStmt.executeUpdate();
			updateLoanStmt.close();

			JOptionPane.showMessageDialog(this, "Book loan renewed successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);

			// Refresh data
			loadDashboardData();
			loadBorrowedBooksData();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add a new book to the catalog
	 */
	private void addBook(String isbn, String title, String author, String genre, String publisher, int year,
			String description, int copies) {
		try {
			// Check if the ISBN already exists
			String checkSql = "SELECT COUNT(*) FROM books WHERE isbn = ?";
			PreparedStatement checkStmt = connection.prepareStatement(checkSql);
			checkStmt.setString(1, isbn);
			ResultSet checkRs = checkStmt.executeQuery();
			checkRs.next();
			int count = checkRs.getInt(1);
			checkRs.close();
			checkStmt.close();

			if (count > 0) {
				// ISBN already exists, ask if they want to add copies instead
				int option = JOptionPane.showConfirmDialog(this,
						"A book with this ISBN already exists. Would you like to add copies instead?",
						"ISBN Already Exists", JOptionPane.YES_NO_OPTION);

				if (option == JOptionPane.YES_OPTION) {
					addBookCopy(isbn, title);
				}

				return;
			}

			// Insert book records for the specified number of copies
			for (int i = 0; i < copies; i++) {
				String insertSql = "INSERT INTO books (title, author, isbn, year, genre, description, publisher, available, date_added) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, true, CURRENT_DATE())";
				PreparedStatement insertStmt = connection.prepareStatement(insertSql);
				insertStmt.setString(1, title);
				insertStmt.setString(2, author);
				insertStmt.setString(3, isbn);
				insertStmt.setInt(4, year);
				insertStmt.setString(5, genre);
				insertStmt.setString(6, description);
				insertStmt.setString(7, publisher);
				insertStmt.executeUpdate();
				insertStmt.close();
			}

			JOptionPane.showMessageDialog(this, "Book added successfully with " + copies + " copies!", "Success",
					JOptionPane.INFORMATION_MESSAGE);

			// Refresh data
			loadDashboardData();
			loadBooksData();
			loadBookGenres();

		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this, "Error adding book: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * Add copies of an existing book
	 */
	private void addBookCopy(String isbn, String title) {
		// Prompt for number of copies
		String input = JOptionPane.showInputDialog(this, "Enter number of copies to add for \"" + title + "\":",
				"Add Copies", JOptionPane.QUESTION_MESSAGE);

		if (input == null || input.isEmpty()) {
			return; // User cancelled
		}

		int copies;
		try {
			copies = Integer.parseInt(input);
			if (copies <= 0) {
				JOptionPane.showMessageDialog(this, "Please enter a positive number", "Invalid Input",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Please enter a valid number", "Invalid Input",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		try {
			// Get existing book details
			String bookSql = "SELECT title, author, year, genre, description, publisher FROM books WHERE isbn = ? LIMIT 1";
			PreparedStatement bookStmt = connection.prepareStatement(bookSql);
			bookStmt.setString(1, isbn);
			ResultSet bookRs = bookStmt.executeQuery();

			if (!bookRs.next()) {
				JOptionPane.showMessageDialog(this, "Book with ISBN " + isbn + " not found", "Book Not Found",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			String bookTitle = bookRs.getString("title");
			String author = bookRs.getString("author");
			int year = bookRs.getInt("year");
			String genre = bookRs.getString("genre");
			String description = bookRs.getString("description");
			String publisher = bookRs.getString("publisher");

			bookRs.close();
			bookStmt.close();

			// Insert additional copies
			for (int i = 0; i < copies; i++) {
				String insertSql = "INSERT INTO books (title, author, isbn, year, genre, description, publisher, available, date_added) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, true, CURRENT_DATE())";
				PreparedStatement insertStmt = connection.prepareStatement(insertSql);
				insertStmt.setString(1, bookTitle);
				insertStmt.setString(2, author);
				insertStmt.setString(3, isbn);
				insertStmt.setInt(4, year);
				insertStmt.setString(5, genre);
				insertStmt.setString(6, description);
				insertStmt.setString(7, publisher);
				insertStmt.executeUpdate();
				insertStmt.close();
			}

			JOptionPane.showMessageDialog(this, copies + " copies of \"" + bookTitle + "\" added successfully!",
					"Success", JOptionPane.INFORMATION_MESSAGE);

			// Refresh data
			loadDashboardData();
			loadBooksData();

		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this, "Error adding book copies: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * Update an existing book's details
	 */
	private void updateBook(String isbn, String title, String author, String genre, String publisher, int year,
			String description) {
		try {
			String updateSql = "UPDATE books SET title = ?, author = ?, genre = ?, "
					+ "publisher = ?, year = ?, description = ? WHERE isbn = ?";
			PreparedStatement updateStmt = connection.prepareStatement(updateSql);
			updateStmt.setString(1, title);
			updateStmt.setString(2, author);
			updateStmt.setString(3, genre);
			updateStmt.setString(4, publisher);
			updateStmt.setInt(5, year);
			updateStmt.setString(6, description);
			updateStmt.setString(7, isbn);

			int rowsAffected = updateStmt.executeUpdate();
			updateStmt.close();

			if (rowsAffected > 0) {
				JOptionPane.showMessageDialog(this, "Book updated successfully! " + rowsAffected + " copies updated.",
						"Success", JOptionPane.INFORMATION_MESSAGE);

				// Refresh data
				loadBooksData();
				loadBookGenres();
			} else {
				JOptionPane.showMessageDialog(this, "No books were updated. The ISBN may not exist.", "Update Failed",
						JOptionPane.WARNING_MESSAGE);
			}

		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this, "Error updating book: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * Delete a book (all copies with the same ISBN)
	 */
	private void deleteBook(String isbn) {
		// Check if any copies are currently borrowed
		try {
			String checkSql = "SELECT COUNT(*) FROM borrowed_books bb " + "JOIN books b ON bb.book_id = b.id "
					+ "WHERE b.isbn = ? AND bb.return_date IS NULL";
			PreparedStatement checkStmt = connection.prepareStatement(checkSql);
			checkStmt.setString(1, isbn);
			ResultSet checkRs = checkStmt.executeQuery();
			checkRs.next();
			int borrowedCount = checkRs.getInt(1);
			checkRs.close();
			checkStmt.close();

			if (borrowedCount > 0) {
				JOptionPane.showMessageDialog(this,
						"Cannot delete book because " + borrowedCount + " copies are currently borrowed.",
						"Book in Use", JOptionPane.WARNING_MESSAGE);
				return;
			}

			// Confirm deletion
			int confirm = JOptionPane.showConfirmDialog(this,
					"Are you sure you want to delete all copies of this book? This cannot be undone.",
					"Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

			if (confirm != JOptionPane.YES_OPTION) {
				return;
			}

			// Delete all copies
			String deleteSql = "DELETE FROM books WHERE isbn = ?";
			PreparedStatement deleteStmt = connection.prepareStatement(deleteSql);
			deleteStmt.setString(1, isbn);
			int rowsAffected = deleteStmt.executeUpdate();
			deleteStmt.close();

			JOptionPane.showMessageDialog(this, rowsAffected + " copies of the book deleted successfully!",
					"Deletion Successful", JOptionPane.INFORMATION_MESSAGE);

			// Refresh data
			loadDashboardData();
			loadBooksData();

		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this, "Error deleting book: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * Add a new member
	 */
	private void addMember(String username, String password, String fullName, String email) {
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
				JOptionPane.showMessageDialog(this, "Username already exists. Please choose a different username.",
						"Username Exists", JOptionPane.WARNING_MESSAGE);
				return;
			}

			// Insert new member
			String insertSql = "INSERT INTO users (username, password, full_name, email, role, registration_date) "
					+ "VALUES (?, ?, ?, ?, 'member', CURRENT_DATE())";
			PreparedStatement insertStmt = connection.prepareStatement(insertSql);
			insertStmt.setString(1, username);
			insertStmt.setString(2, password);
			insertStmt.setString(3, fullName);
			insertStmt.setString(4, email);
			insertStmt.executeUpdate();
			insertStmt.close();

			JOptionPane.showMessageDialog(this, "Member added successfully!", "Success",
					JOptionPane.INFORMATION_MESSAGE);

			// Refresh data
			loadDashboardData();
			loadMembersData();

		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this, "Error adding member: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * Update an existing member's details
	 */
	private void updateMember(String username, String password, String fullName, String email) {
		try {
			StringBuilder sqlBuilder = new StringBuilder("UPDATE users SET full_name = ?, email = ?");

			// Only update password if a new one is provided
			if (password != null && !password.isEmpty()) {
				sqlBuilder.append(", password = ?");
			}

			sqlBuilder.append(" WHERE username = ?");

			PreparedStatement updateStmt = connection.prepareStatement(sqlBuilder.toString());
			updateStmt.setString(1, fullName);
			updateStmt.setString(2, email);

			if (password != null && !password.isEmpty()) {
				updateStmt.setString(3, password);
				updateStmt.setString(4, username);
			} else {
				updateStmt.setString(3, username);
			}

			int rowsAffected = updateStmt.executeUpdate();
			updateStmt.close();

			if (rowsAffected > 0) {
				JOptionPane.showMessageDialog(this, "Member updated successfully!", "Success",
						JOptionPane.INFORMATION_MESSAGE);

				// Refresh data
				loadMembersData();
			} else {
				JOptionPane.showMessageDialog(this, "No members were updated. The username may not exist.",
						"Update Failed", JOptionPane.WARNING_MESSAGE);
			}

		} catch (SQLException e) {
			JOptionPane.showMessageDialog(this, "Error updating member: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * Show the add book dialog
	 */
	private void showAddBookDialog() {
		JDialog dialog = new JDialog(this, "Add New Book", true);
		dialog.setSize(500, 650);
		dialog.setLocationRelativeTo(this);
		dialog.setResizable(false);
		dialog.setUndecorated(true);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBackground(new Color(30, 30, 30));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		JLabel titleLabel = new JLabel("Add New Book");
		titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		// Create form fields
		JPanel isbnPanel = createFormField("ISBN:", 100);
		JTextField isbnField = (JTextField) isbnPanel.getComponent(1);

		JPanel titleFieldPanel = createFormField("Title:", 100);
		JTextField bookTitleField = (JTextField) titleFieldPanel.getComponent(1);

		JPanel authorPanel = createFormField("Author:", 100);
		JTextField authorField = (JTextField) authorPanel.getComponent(1);

		JPanel categoryPanel = createFormField("Genre:", 100);
		JComboBox<String> categoryCombo = new JComboBox<>();
		categoryCombo.setPreferredSize(new Dimension(300, 25));
		categoryCombo.setBackground(new Color(60, 60, 60));
		categoryCombo.setForeground(Color.black);

		// Load genres
		try {
			String sql = "SELECT DISTINCT genre FROM books WHERE genre IS NOT NULL AND genre != '' ORDER BY genre";
			PreparedStatement stmt = connection.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();

			categoryCombo.addItem(""); // Empty option

			while (rs.next()) {
				categoryCombo.addItem(rs.getString("genre"));
			}

			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Add an "Other" option and a text field for custom genre
		categoryCombo.addItem("Other");

		JPanel otherGenrePanel = createFormField("Custom Genre:", 100);
		JTextField otherGenreField = (JTextField) otherGenrePanel.getComponent(1);
		otherGenreField.setEnabled(false);

		categoryCombo.addActionListener(e -> {
			String selected = (String) categoryCombo.getSelectedItem();
			otherGenreField.setEnabled("Other".equals(selected));
		});

		categoryPanel.remove(1); // Remove the default text field
		categoryPanel.add(categoryCombo); // Add the combo box

		JPanel publisherPanel = createFormField("Publisher:", 100);
		JTextField publisherField = (JTextField) publisherPanel.getComponent(1);

		JPanel yearPanel = createFormField("Pub. Year:", 100);
		JTextField yearField = (JTextField) yearPanel.getComponent(1);

		JPanel copiesPanel = createFormField("Copies:", 100);
		JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
		copiesSpinner.setPreferredSize(new Dimension(60, 25));
		copiesPanel.remove(1); // Remove the default text field
		copiesPanel.add(copiesSpinner); // Add the spinner

		JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		descPanel.setBackground(new Color(30, 30, 30));
		descPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel descLabel = new JLabel("Description:");
		descLabel.setForeground(Color.WHITE);
		descLabel.setPreferredSize(new Dimension(100, 25));

		descPanel.add(descLabel);

		JTextArea descArea = new JTextArea(5, 25);
		descArea.setLineWrap(true);
		descArea.setWrapStyleWord(true);
		descArea.setBackground(new Color(60, 60, 60));
		descArea.setForeground(Color.WHITE);
		descArea.setCaretColor(Color.WHITE);

		JScrollPane descScroll = new JScrollPane(descArea);
		descScroll.setPreferredSize(new Dimension(300, 100));

		JPanel descWrapperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		descWrapperPanel.setBackground(new Color(30, 30, 30));
		descWrapperPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		descWrapperPanel.add(descScroll);

		// Buttons panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.setBackground(new Color(30, 30, 30));
		buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setBackground(new Color(60, 60, 60));
		cancelButton.setForeground(Color.black);

		JButton saveButton = new JButton("Save Book");
		saveButton.setBackground(new Color(0, 102, 204));
		saveButton.setForeground(Color.black);

		buttonPanel.add(cancelButton);
		buttonPanel.add(saveButton);

		// Add all panels to main panel
		mainPanel.add(titleLabel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
		mainPanel.add(isbnPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		mainPanel.add(titleFieldPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		mainPanel.add(authorPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		mainPanel.add(categoryPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		mainPanel.add(otherGenrePanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		mainPanel.add(publisherPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		mainPanel.add(yearPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		mainPanel.add(copiesPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
		mainPanel.add(descPanel);
		mainPanel.add(descWrapperPanel);
		mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
		mainPanel.add(buttonPanel);

		dialog.add(new JScrollPane(mainPanel));

		// Add action listeners
		cancelButton.addActionListener(e -> dialog.dispose());

		saveButton.addActionListener(e -> {
			// Validate fields
			String isbn = isbnField.getText().trim();
			String title = bookTitleField.getText().trim();
			String author = authorField.getText().trim();
			String publisher = publisherField.getText().trim();
			String yearText = yearField.getText().trim();
			int copies = (Integer) copiesSpinner.getValue();
			String description = descArea.getText().trim();

			// Get genre based on selection
			String genre;
			if (categoryCombo.getSelectedItem().equals("Other")) {
				genre = otherGenreField.getText().trim();
			} else {
				genre = (String) categoryCombo.getSelectedItem();
			}

			if (isbn.isEmpty() || title.isEmpty() || author.isEmpty() || yearText.isEmpty()) {
				JOptionPane.showMessageDialog(dialog,
						"Please fill all required fields (ISBN, Title, Author, Publication Year)",
						"Missing Information", JOptionPane.WARNING_MESSAGE);
				return;
			}

			int year;
			try {
				year = Integer.parseInt(yearText);
				if (year < 0 || year > Calendar.getInstance().get(Calendar.YEAR) + 1) {
					JOptionPane.showMessageDialog(dialog, "Please enter a valid year", "Invalid Year",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(dialog, "Please enter a valid year", "Invalid Year",
						JOptionPane.WARNING_MESSAGE);
				return;
			}

			// Add the book
			addBook(isbn, title, author, genre, publisher, year, description, copies);
			dialog.dispose();
		});

		dialog.setVisible(true);
	}

	/**
	 * Show the manage genres dialog
	 */
	/**
	 * Show the manage genres dialog with white border
	 */
	private void showManageGenresDialog() {
	    JDialog dialog = new JDialog(this, "Manage Book Genres", true);
	    dialog.setSize(400, 500);
	    dialog.setLocationRelativeTo(this);
	    dialog.setUndecorated(true);
	    dialog.setResizable(false);
	    
	    // Create panel with white border
	    JPanel borderPanel = new JPanel(new BorderLayout());
	    borderPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
	    borderPanel.setBackground(new Color(30, 30, 30));
	    
	    JPanel mainPanel = new JPanel(new BorderLayout());
	    mainPanel.setBackground(new Color(30, 30, 30));
	    mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	    
	    JLabel titleLabel = new JLabel("Book Genres");
	    titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
	    titleLabel.setForeground(Color.WHITE);
	    
	    mainPanel.add(titleLabel, BorderLayout.NORTH);
	    
	    // Create list of genres
	    DefaultListModel<String> genreListModel = new DefaultListModel<>();
	    JList<String> genreList = new JList<>(genreListModel);
	    genreList.setBackground(new Color(40, 40, 40));
	    genreList.setForeground(Color.WHITE);
	    genreList.setSelectionBackground(new Color(70, 70, 80));
	    genreList.setSelectionForeground(Color.WHITE);
	    
	    // Load genres from database
	    try {
	        String sql = "SELECT DISTINCT genre FROM books WHERE genre IS NOT NULL AND genre != '' ORDER BY genre";
	        PreparedStatement stmt = connection.prepareStatement(sql);
	        ResultSet rs = stmt.executeQuery();
	        
	        while (rs.next()) {
	            genreListModel.addElement(rs.getString("genre"));
	        }
	        
	        rs.close();
	        stmt.close();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    
	    JScrollPane scrollPane = new JScrollPane(genreList);
	    scrollPane.setPreferredSize(new Dimension(350, 300));
	    
	    mainPanel.add(scrollPane, BorderLayout.CENTER);
	    
	    // Actions panel
	    JPanel actionsPanel = new JPanel(new BorderLayout());
	    actionsPanel.setBackground(new Color(30, 30, 30));
	    actionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
	    
	    JPanel newGenrePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    newGenrePanel.setBackground(new Color(30, 30, 30));
	    
	    JTextField newGenreField = new JTextField(20);
	    newGenreField.setBackground(new Color(60, 60, 60));
	    newGenreField.setForeground(Color.WHITE);
	    newGenreField.setCaretColor(Color.WHITE);
	    
	    JButton addGenreButton = new JButton("Add Genre");
	    addGenreButton.setBackground(new Color(0, 102, 204));
	    addGenreButton.setForeground(Color.black);
	    
	    newGenrePanel.add(newGenreField);
	    newGenrePanel.add(addGenreButton);
	    
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	    buttonPanel.setBackground(new Color(30, 30, 30));
	    
	    JButton deleteButton = new JButton("Delete Genre");
	    deleteButton.setBackground(new Color(204, 51, 51));
	    deleteButton.setForeground(Color.black);
	    
	    JButton closeButton = new JButton("Close");
	    closeButton.setBackground(new Color(60, 60, 60));
	    closeButton.setForeground(Color.black);
	    
	    buttonPanel.add(deleteButton);
	    buttonPanel.add(closeButton);
	    
	    actionsPanel.add(newGenrePanel, BorderLayout.NORTH);
	    actionsPanel.add(buttonPanel, BorderLayout.SOUTH);
	    
	    mainPanel.add(actionsPanel, BorderLayout.SOUTH);
	    
	    // Add to border panel
	    borderPanel.add(mainPanel, BorderLayout.CENTER);
	    dialog.add(borderPanel);
	    
	    // Add action listeners
	    addGenreButton.addActionListener(e -> {
	        String newGenre = newGenreField.getText().trim();
	        if (!newGenre.isEmpty()) {
	            // Check if genre already exists
	            if (genreListModel.contains(newGenre)) {
	                JOptionPane.showMessageDialog(dialog, "This genre already exists", "Duplicate Genre",
	                        JOptionPane.WARNING_MESSAGE);
	                return;
	            }
	            
	            genreListModel.addElement(newGenre);
	            newGenreField.setText("");
	            
	            // This genre will be used when a new book with this genre is added
	            // No need to add it to the database until a book actually uses it
	            JOptionPane.showMessageDialog(dialog,
	                    "New genre added. It will appear in the genre list when you add or edit books.", "Genre Added",
	                    JOptionPane.INFORMATION_MESSAGE);
	                    
	            // Update the genre dropdown in book add/edit dialogs
	            loadBookGenres();
	        }
	    });
	    
	    deleteButton.addActionListener(e -> {
	        int selectedIndex = genreList.getSelectedIndex();
	        if (selectedIndex >= 0) {
	            String selectedGenre = genreListModel.getElementAt(selectedIndex);
	            
	            // Check if the genre is in use
	            try {
	                String sql = "SELECT COUNT(*) FROM books WHERE genre = ?";
	                PreparedStatement stmt = connection.prepareStatement(sql);
	                stmt.setString(1, selectedGenre);
	                ResultSet rs = stmt.executeQuery();
	                rs.next();
	                int count = rs.getInt(1);
	                rs.close();
	                stmt.close();
	                
	                if (count > 0) {
	                    int option = JOptionPane.showConfirmDialog(dialog,
	                            "This genre is used by " + count + " books. Are you sure you want to remove it?\n" +
	                            "Books with this genre will be changed to 'Uncategorized'.",
	                            "Genre in Use", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
	                    
	                    if (option != JOptionPane.YES_OPTION) {
	                        return;
	                    }
	                    
	                    // Update all books with this genre to 'Uncategorized'
	                    String updateSql = "UPDATE books SET genre = 'Uncategorized' WHERE genre = ?";
	                    PreparedStatement updateStmt = connection.prepareStatement(updateSql);
	                    updateStmt.setString(1, selectedGenre);
	                    int rowsUpdated = updateStmt.executeUpdate();
	                    updateStmt.close();
	                    
	                    JOptionPane.showMessageDialog(dialog, 
	                        rowsUpdated + " books have been updated to 'Uncategorized'.",
	                        "Books Updated", JOptionPane.INFORMATION_MESSAGE);
	                }
	                
	                // Remove the genre from the list
	                genreListModel.remove(selectedIndex);
	                
	                // Refresh the book table to show the updated genres
	                loadBooksData();
	                
	                // Refresh the genre dropdown in filters and dialogs
	                loadBookGenres();
	                
	            } catch (SQLException ex) {
	                JOptionPane.showMessageDialog(dialog, "Error checking genre usage: " + ex.getMessage(),
	                        "Database Error", JOptionPane.ERROR_MESSAGE);
	                ex.printStackTrace();
	            }
	        } else {
	            JOptionPane.showMessageDialog(dialog, "Please select a genre to delete", "No Selection",
	                    JOptionPane.WARNING_MESSAGE);
	        }
	    });
	    
	    closeButton.addActionListener(e -> dialog.dispose());
	    
	    dialog.setVisible(true);
	}

	/**
	 * Show dialog to select a member
	 */
	private void showMemberBrowser(JTextField targetField) {
		JDialog dialog = new JDialog(this, "Select Member", true);
		dialog.setSize(600, 400);
		dialog.setLocationRelativeTo(this);
		dialog.setResizable(false);
		dialog.setUndecorated(true);

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(new Color(30, 30, 30));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		// Search panel
		JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		searchPanel.setBackground(new Color(30, 30, 30));

		JLabel searchLabel = new JLabel("Search:");
		searchLabel.setForeground(Color.WHITE);

		JTextField searchField = new JTextField(20);
		searchField.setBackground(new Color(60, 60, 60));
		searchField.setForeground(Color.WHITE);
		searchField.setCaretColor(Color.WHITE);

		JButton searchButton = new JButton("Search");
		searchButton.setBackground(new Color(60, 60, 60));
		searchButton.setForeground(Color.black);

		searchPanel.add(searchLabel);
		searchPanel.add(searchField);
		searchPanel.add(searchButton);

		mainPanel.add(searchPanel, BorderLayout.NORTH);

		// Members table
		String[] columns = { "Member ID", "Name", "Email", "Status" };
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		JTable membersTable = new JTable(model);
		membersTable.setBackground(new Color(40, 40, 40));
		membersTable.setForeground(Color.WHITE);
		membersTable.setGridColor(new Color(60, 60, 60));
		membersTable.getTableHeader().setBackground(new Color(50, 50, 50));
		membersTable.getTableHeader().setForeground(Color.black);
		membersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		membersTable.setRowHeight(25);

		// Load members
		try {
			String sql = "SELECT username, full_name, email, "
					+ "CASE WHEN EXISTS (SELECT 1 FROM borrowed_books bb WHERE bb.username = u.username AND bb.return_date IS NULL AND bb.due_date < CURRENT_DATE()) "
					+ "THEN 'Overdue Books' ELSE 'Active' END AS status "
					+ "FROM users u WHERE u.role = 'member' ORDER BY full_name";
			PreparedStatement stmt = connection.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				model.addRow(new Object[] { rs.getString("username"), rs.getString("full_name"), rs.getString("email"),
						rs.getString("status") });
			}

			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		JScrollPane scrollPane = new JScrollPane(membersTable);
		scrollPane.getViewport().setBackground(new Color(40, 40, 40));

		mainPanel.add(scrollPane, BorderLayout.CENTER);

		// Buttons panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.setBackground(new Color(30, 30, 30));

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setBackground(new Color(60, 60, 60));
		cancelButton.setForeground(Color.black);

		JButton selectButton = new JButton("Select");
		selectButton.setBackground(new Color(0, 102, 204));
		selectButton.setForeground(Color.black);

		buttonPanel.add(cancelButton);
		buttonPanel.add(selectButton);

		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		dialog.add(mainPanel);

		// Add action listeners
		searchButton.addActionListener(e -> {
			String searchText = searchField.getText().trim();
			if (searchText.isEmpty()) {
				return;
			}

			model.setRowCount(0);

			try {
				String sql = "SELECT username, full_name, email, "
						+ "CASE WHEN EXISTS (SELECT 1 FROM borrowed_books bb WHERE bb.username = u.username AND bb.return_date IS NULL AND bb.due_date < CURRENT_DATE()) "
						+ "THEN 'Overdue Books' ELSE 'Active' END AS status "
						+ "FROM users u WHERE u.role = 'member' AND "
						+ "(LOWER(username) LIKE ? OR LOWER(full_name) LIKE ? OR LOWER(email) LIKE ?) "
						+ "ORDER BY full_name";
				PreparedStatement stmt = connection.prepareStatement(sql);

				String searchPattern = "%" + searchText.toLowerCase() + "%";
				stmt.setString(1, searchPattern);
				stmt.setString(2, searchPattern);
				stmt.setString(3, searchPattern);

				ResultSet rs = stmt.executeQuery();

				while (rs.next()) {
					model.addRow(new Object[] { rs.getString("username"), rs.getString("full_name"),
							rs.getString("email"), rs.getString("status") });
				}

				rs.close();
				stmt.close();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		});

		cancelButton.addActionListener(e -> dialog.dispose());

		selectButton.addActionListener(e -> {
			int selectedRow = membersTable.getSelectedRow();
			if (selectedRow >= 0) {
				String memberId = (String) membersTable.getValueAt(selectedRow, 0);
				targetField.setText(memberId);
				dialog.dispose();
			} else {
				JOptionPane.showMessageDialog(dialog, "Please select a member", "No Selection",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		// Double-click to select
		membersTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int selectedRow = membersTable.getSelectedRow();
					if (selectedRow >= 0) {
						String memberId = (String) membersTable.getValueAt(selectedRow, 0);
						targetField.setText(memberId);
						dialog.dispose();
					}
				}
			}
		});

		dialog.setVisible(true);
	}

	/**
	 * Show dialog to select a book
	 */
	private void showBookBrowser(JTextField targetField) {
		JDialog dialog = new JDialog(this, "Select Book", true);
		dialog.setSize(700, 400);
		dialog.setLocationRelativeTo(this);
		dialog.setResizable(false);
		dialog.setUndecorated(true);

		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBackground(new Color(30, 30, 30));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		// Search panel
		JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		searchPanel.setBackground(new Color(30, 30, 30));

		JLabel searchLabel = new JLabel("Search:");
		searchLabel.setForeground(Color.WHITE);

		JTextField searchField = new JTextField(20);
		searchField.setBackground(new Color(60, 60, 60));
		searchField.setForeground(Color.WHITE);
		searchField.setCaretColor(Color.WHITE);

		JButton searchButton = new JButton("Search");
		searchButton.setBackground(new Color(60, 60, 60));
		searchButton.setForeground(Color.black);

		searchPanel.add(searchLabel);
		searchPanel.add(searchField);
		searchPanel.add(searchButton);

		mainPanel.add(searchPanel, BorderLayout.NORTH);

		// Books table
		String[] columns = { "ISBN", "Title", "Author", "Genre", "Year", "Available" };
		DefaultTableModel model = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		JTable booksTable = new JTable(model);
		booksTable.setBackground(new Color(40, 40, 40));
		booksTable.setForeground(Color.WHITE);
		booksTable.setGridColor(new Color(60, 60, 60));
		booksTable.getTableHeader().setBackground(new Color(50, 50, 50));
		booksTable.getTableHeader().setForeground(Color.black);
		booksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		booksTable.setRowHeight(25);

		// Load available books
		try {
			String sql = "SELECT isbn, title, author, genre, year, available FROM books WHERE available = true ORDER BY title";
			PreparedStatement stmt = connection.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				model.addRow(new Object[] { rs.getString("isbn"), rs.getString("title"), rs.getString("author"),
						rs.getString("genre"), rs.getInt("year"), rs.getBoolean("available") ? "Yes" : "No" });
			}

			rs.close();
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		JScrollPane scrollPane = new JScrollPane(booksTable);
		scrollPane.getViewport().setBackground(new Color(40, 40, 40));

		mainPanel.add(scrollPane, BorderLayout.CENTER);

		// Buttons panel
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.setBackground(new Color(30, 30, 30));

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setBackground(new Color(60, 60, 60));
		cancelButton.setForeground(Color.black);

		JButton selectButton = new JButton("Select");
		selectButton.setBackground(new Color(0, 102, 204));
		selectButton.setForeground(Color.black);

		buttonPanel.add(cancelButton);
		buttonPanel.add(selectButton);

		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		dialog.add(mainPanel);

		// Add action listeners
		searchButton.addActionListener(e -> {
			String searchText = searchField.getText().trim();
			if (searchText.isEmpty()) {
				return;
			}

			model.setRowCount(0);

			try {
				String sql = "SELECT isbn, title, author, genre, year, available FROM books "
						+ "WHERE available = true AND "
						+ "(LOWER(isbn) LIKE ? OR LOWER(title) LIKE ? OR LOWER(author) LIKE ?) " + "ORDER BY title";
				PreparedStatement stmt = connection.prepareStatement(sql);

				String searchPattern = "%" + searchText.toLowerCase() + "%";
				stmt.setString(1, searchPattern);
				stmt.setString(2, searchPattern);
				stmt.setString(3, searchPattern);

				ResultSet rs = stmt.executeQuery();

				while (rs.next()) {
					model.addRow(new Object[] { rs.getString("isbn"), rs.getString("title"), rs.getString("author"),
							rs.getString("genre"), rs.getInt("year"), rs.getBoolean("available") ? "Yes" : "No" });
				}

				rs.close();
				stmt.close();
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		});

		cancelButton.addActionListener(e -> dialog.dispose());

		selectButton.addActionListener(e -> {
			int selectedRow = booksTable.getSelectedRow();
			if (selectedRow >= 0) {
				String isbn = (String) booksTable.getValueAt(selectedRow, 0);
				targetField.setText(isbn);
				dialog.dispose();
			} else {
				JOptionPane.showMessageDialog(dialog, "Please select a book", "No Selection",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		// Double-click to select
		booksTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int selectedRow = booksTable.getSelectedRow();
					if (selectedRow >= 0) {
						String isbn = (String) booksTable.getValueAt(selectedRow, 0);
						targetField.setText(isbn);
						dialog.dispose();
					}
				}
			}
		});

		dialog.setVisible(true);
	}

	/**
	 * Show dialog to select a borrowed book
	 */
	/**
	 * Show dialog to select a borrowed book
	 */
	private void showBorrowedBookBrowser(JTextField targetField) {
	    JDialog dialog = new JDialog(this, "Select Borrowed Book", true);
	    dialog.setSize(700, 400);
	    dialog.setLocationRelativeTo(this);
	    dialog.setUndecorated(true);
	    dialog.setResizable(false);

	    JPanel mainPanel = new JPanel(new BorderLayout());
	    mainPanel.setBackground(new Color(30, 30, 30));
	    mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

	    // Search panel
	    JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    searchPanel.setBackground(new Color(30, 30, 30));

	    JLabel searchLabel = new JLabel("Search:");
	    searchLabel.setForeground(Color.WHITE);

	    JTextField searchField = new JTextField(20);
	    searchField.setBackground(new Color(60, 60, 60));
	    searchField.setForeground(Color.WHITE);
	    searchField.setCaretColor(Color.WHITE);

	    JButton searchButton = new JButton("Search");
	    searchButton.setBackground(new Color(60, 60, 60));
	    searchButton.setForeground(Color.BLACK);

	    searchPanel.add(searchLabel);
	    searchPanel.add(searchField);
	    searchPanel.add(searchButton);

	    mainPanel.add(searchPanel, BorderLayout.NORTH);

	    // Borrowed books table
	    String[] columns = { "ISBN", "Title", "Borrowed By", "Borrow Date", "Due Date", "Status" };
	    DefaultTableModel model = new DefaultTableModel(columns, 0) {
	        @Override
	        public boolean isCellEditable(int row, int column) {
	            return false;
	        }
	    };

	    JTable booksTable = new JTable(model);
	    booksTable.setBackground(new Color(40, 40, 40));
	    booksTable.setForeground(Color.WHITE);
	    booksTable.setGridColor(new Color(60, 60, 60));
	    booksTable.getTableHeader().setBackground(new Color(50, 50, 50));
	    booksTable.getTableHeader().setForeground(Color.BLACK);
	    booksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    booksTable.setRowHeight(30);

	    // Load borrowed books
	    try {
	        String sql = "SELECT b.isbn, b.title, u.username, u.full_name, bb.borrow_date, bb.due_date, "
	                + "CASE WHEN bb.due_date < CURRENT_DATE() THEN 'Overdue' ELSE 'On Time' END AS status "
	                + "FROM borrowed_books bb "
	                + "JOIN books b ON bb.book_id = b.id "
	                + "JOIN users u ON bb.username = u.username "
	                + "WHERE bb.return_date IS NULL "
	                + "ORDER BY bb.due_date";
	        PreparedStatement stmt = connection.prepareStatement(sql);
	        ResultSet rs = stmt.executeQuery();

	        while (rs.next()) {
	            model.addRow(new Object[] { 
	                rs.getString("isbn"), 
	                rs.getString("title"),
	                rs.getString("username") + " (" + rs.getString("full_name") + ")", 
	                rs.getString("borrow_date"),
	                rs.getString("due_date"), 
	                rs.getString("status") 
	            });
	        }

	        rs.close();
	        stmt.close();
	    } catch (SQLException e) {
	        JOptionPane.showMessageDialog(dialog, "Error loading borrowed books: " + e.getMessage(),
	                "Database Error", JOptionPane.ERROR_MESSAGE);
	        e.printStackTrace();
	    }

	    JScrollPane scrollPane = new JScrollPane(booksTable);
	    scrollPane.getViewport().setBackground(new Color(40, 40, 40));

	    mainPanel.add(scrollPane, BorderLayout.CENTER);

	    // Buttons panel
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	    buttonPanel.setBackground(new Color(30, 30, 30));

	    JButton cancelButton = new JButton("Cancel");
	    cancelButton.setBackground(new Color(60, 60, 60));
	    cancelButton.setForeground(Color.black);

	    JButton selectButton = new JButton("Select");
	    selectButton.setBackground(new Color(0, 102, 204));
	    selectButton.setForeground(Color.black);

	    buttonPanel.add(cancelButton);
	    buttonPanel.add(selectButton);

	    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

	    dialog.add(mainPanel);

	    // Add action listeners
	    searchButton.addActionListener(e -> {
	        String searchText = searchField.getText().trim();
	        if (searchText.isEmpty()) {
	            return;
	        }

	        model.setRowCount(0);

	        try {
	            String sql = "SELECT b.isbn, b.title, u.username, u.full_name, bb.borrow_date, bb.due_date, "
	                    + "CASE WHEN bb.due_date < CURRENT_DATE() THEN 'Overdue' ELSE 'On Time' END AS status "
	                    + "FROM borrowed_books bb "
	                    + "JOIN books b ON bb.book_id = b.id "
	                    + "JOIN users u ON bb.username = u.username "
	                    + "WHERE bb.return_date IS NULL AND "
	                    + "(LOWER(b.isbn) LIKE ? OR LOWER(b.title) LIKE ? OR LOWER(u.username) LIKE ? OR LOWER(u.full_name) LIKE ?) "
	                    + "ORDER BY bb.due_date";
	            PreparedStatement stmt = connection.prepareStatement(sql);

	            String searchPattern = "%" + searchText.toLowerCase() + "%";
	            stmt.setString(1, searchPattern);
	            stmt.setString(2, searchPattern);
	            stmt.setString(3, searchPattern);
	            stmt.setString(4, searchPattern);

	            ResultSet rs = stmt.executeQuery();

	            while (rs.next()) {
	                model.addRow(new Object[] { 
	                    rs.getString("isbn"), 
	                    rs.getString("title"),
	                    rs.getString("username") + " (" + rs.getString("full_name") + ")", 
	                    rs.getString("borrow_date"),
	                    rs.getString("due_date"), 
	                    rs.getString("status") 
	                });
	            }

	            rs.close();
	            stmt.close();
	        } catch (SQLException ex) {
	            JOptionPane.showMessageDialog(dialog, "Error searching borrowed books: " + ex.getMessage(),
	                    "Database Error", JOptionPane.ERROR_MESSAGE);
	            ex.printStackTrace();
	        }
	    });

	    cancelButton.addActionListener(e -> dialog.dispose());

	    selectButton.addActionListener(e -> {
	        int selectedRow = booksTable.getSelectedRow();
	        if (selectedRow >= 0) {
	            String isbn = (String) booksTable.getValueAt(selectedRow, 0);
	            targetField.setText(isbn);
	            dialog.dispose();
	        } else {
	            JOptionPane.showMessageDialog(dialog, "Please select a book", "No Selection",
	                    JOptionPane.WARNING_MESSAGE);
	        }
	    });

	    // Double-click to select
	    booksTable.addMouseListener(new MouseAdapter() {
	        @Override
	        public void mouseClicked(MouseEvent e) {
	            if (e.getClickCount() == 2) {
	                int selectedRow = booksTable.getSelectedRow();
	                if (selectedRow >= 0) {
	                    String isbn = (String) booksTable.getValueAt(selectedRow, 0);
	                    targetField.setText(isbn);
	                    dialog.dispose();
	                }
	            }
	        }
	    });

	    dialog.setVisible(true);
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
	 * Show the add member dialog
	 */
	/**
	 * Show the dialog to add a new member
	 */
	private void showAddMemberDialog() {
	    JDialog dialog = new JDialog(this, "Add New Member", true);
	    dialog.setSize(500, 650);
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
	    
	    JLabel titleLabel = new JLabel("Add New Member");
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
	    
	    JPanel addressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    addressPanel.setBackground(new Color(30, 30, 30));
	    addressPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JLabel addressLabel = new JLabel("Address:");
	    addressLabel.setForeground(Color.WHITE);
	    addressLabel.setPreferredSize(new Dimension(120, 25));
	    
	    addressPanel.add(addressLabel);
	    
	    JTextArea addressArea = new JTextArea(3, 25);
	    addressArea.setLineWrap(true);
	    addressArea.setBackground(new Color(60, 60, 60));
	    addressArea.setForeground(Color.WHITE);
	    addressArea.setCaretColor(Color.WHITE);
	    
	    JScrollPane addressScroll = new JScrollPane(addressArea);
	    addressScroll.setPreferredSize(new Dimension(300, 80));
	    
	    JPanel addressWrapperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    addressWrapperPanel.setBackground(new Color(30, 30, 30));
	    addressWrapperPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    addressWrapperPanel.add(addressScroll);
	    
	    // Buttons panel
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	    buttonPanel.setBackground(new Color(30, 30, 30));
	    buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
	    
	    JButton cancelButton = new JButton("Cancel");
	    cancelButton.setBackground(new Color(60, 60, 60));
	    cancelButton.setForeground(Color.black);
	    
	    JButton saveButton = new JButton("Save Member");
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
	    mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
	    mainPanel.add(namePanel);
	    mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
	    mainPanel.add(emailPanel);
	    mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
	    mainPanel.add(phonePanel);
	    mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
	    mainPanel.add(addressPanel);
	    mainPanel.add(addressWrapperPanel);
	    mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
	    mainPanel.add(buttonPanel);
	    
	    // Add main panel to border panel
	    borderPanel.add(mainPanel, BorderLayout.CENTER);
	    
	    // Add border panel to dialog
	    dialog.add(new JScrollPane(borderPanel));
	    
	    // Add action listeners
	    cancelButton.addActionListener(e -> dialog.dispose());
	    
	    saveButton.addActionListener(e -> {
	        // Validate fields
	        String username = usernameField.getText().trim();
	        String password = new String(passwordField.getPassword());
	        String fullName = nameField.getText().trim();
	        String email = emailField.getText().trim();
	        String phone = phoneField.getText().trim();
	        String address = addressArea.getText().trim();
	        
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
	        
	        // Add the member to database
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
	            
	            // Insert new member
	            String insertSql = "INSERT INTO users (username, password, full_name, email, phone, address, role, registration_date) " +
	                    "VALUES (?, ?, ?, ?, ?, ?, 'member', CURRENT_DATE())";
	            PreparedStatement insertStmt = connection.prepareStatement(insertSql);
	            insertStmt.setString(1, username);
	            insertStmt.setString(2, password);
	            insertStmt.setString(3, fullName);
	            insertStmt.setString(4, email);
	            insertStmt.setString(5, phone);
	            insertStmt.setString(6, address);
	            insertStmt.executeUpdate();
	            insertStmt.close();
	            
	            JOptionPane.showMessageDialog(dialog, 
	                    "Member added successfully!",
	                    "Success", JOptionPane.INFORMATION_MESSAGE);
	            
	            // Refresh data
	            loadDashboardData();
	            loadMembersData();
	            
	            dialog.dispose();
	            
	        } catch (SQLException ex) {
	            JOptionPane.showMessageDialog(dialog, 
	                    "Error adding member: " + ex.getMessage(),
	                    "Database Error", JOptionPane.ERROR_MESSAGE);
	            ex.printStackTrace();
	        }
	    });
	    
	    dialog.setVisible(true);
	}

	/**
	 * Simple email validation
	 */
	private boolean isValidEmail(String email) {
	    String regex = "^[A-Za-z0-9+_.-]+@(.+)$";
	    return email.matches(regex);
	}

	private class ButtonEditor extends DefaultCellEditor {
		private JButton button;
		private String label;
		private boolean isPushed;
		private int row, column;
		private JTable table;

		public ButtonEditor(JCheckBox checkBox) {
			super(checkBox);
			button = new JButton();
			button.setOpaque(true);
			button.setBorderPainted(false);
			button.setBackground(new Color(60, 60, 60));
			button.setForeground(Color.WHITE);

			button.addActionListener(e -> fireEditingStopped());
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			this.table = table;
			this.row = row;
			this.column = column;

			label = (value == null) ? "" : value.toString();
			button.setText(label);
			isPushed = true;
			return button;
		}

		@Override
		public Object getCellEditorValue() {
			if (isPushed) {
				// Handle button actions based on the table and action
				if (table == booksTable) {
					handleBookAction();
				} else if (table == membersTable) {
					handleMemberAction();
				} else if (table == borrowedBooksTable) {
				}
			}
			isPushed = false;
			return label;
		}

		private void handleBookAction() {
			String isbn = (String) booksTable.getValueAt(row, 0);
			String title = (String) booksTable.getValueAt(row, 1);

			// Create a popup menu with options
			JPopupMenu popupMenu = new JPopupMenu();

			JMenuItem editItem = new JMenuItem("Edit Book");
			editItem.addActionListener(e -> showEditBookDialog(isbn));

			JMenuItem addCopyItem = new JMenuItem("Add Copy");
			addCopyItem.addActionListener(e -> addBookCopy(isbn, title));

			JMenuItem deleteItem = new JMenuItem("Delete Book");
			deleteItem.addActionListener(e -> {
				int confirm = JOptionPane.showConfirmDialog(LibrarianDashboard.this,
						"Are you sure you want to delete this book?", "Confirm Deletion", JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE);

				if (confirm == JOptionPane.YES_OPTION) {
					deleteBook(isbn);
				}
			});

			popupMenu.add(editItem);
			popupMenu.add(addCopyItem);
			popupMenu.add(new JSeparator());
			popupMenu.add(deleteItem);

			// Show the popup menu
			popupMenu.show(button, 0, button.getHeight());
		}

		private void handleMemberAction() {
			String username = (String) membersTable.getValueAt(row, 0);

			// Create a popup menu with options
			JPopupMenu popupMenu = new JPopupMenu();

			JMenuItem editItem = new JMenuItem("Edit Member");
			editItem.addActionListener(e -> showEditMemberDialog(username));

			JMenuItem historyItem = new JMenuItem("Borrowing History");
			historyItem.addActionListener(e -> showBorrowingHistoryDialog(username));

			popupMenu.add(editItem);
			popupMenu.add(historyItem);

			// Show the popup menu
			popupMenu.show(button, 0, button.getHeight());
		}

		/**
		 * Show dialog to edit a book
		 */
		private void showEditBookDialog(String isbn) {
			try {
				// Get book details
				String sql = "SELECT * FROM books WHERE isbn = ? LIMIT 1";
				PreparedStatement stmt = connection.prepareStatement(sql);
				stmt.setString(1, isbn);
				ResultSet rs = stmt.executeQuery();

				if (!rs.next()) {
					return;
				}

				String title = rs.getString("title");
				String author = rs.getString("author");
				String genre = rs.getString("genre");
				String publisher = rs.getString("publisher");
				int year = rs.getInt("year");
				String description = rs.getString("description");

				rs.close();
				stmt.close();

				// Create edit book dialog (similar to add book dialog but with pre-filled
				// values)
				JDialog dialog = new JDialog();
				dialog.setSize(500, 600);
				dialog.setUndecorated(true);
				dialog.setResizable(false);

				dialog.setLocationRelativeTo(LibrarianDashboard.this);

				JPanel mainPanel = new JPanel();
				mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
				mainPanel.setBackground(new Color(30, 30, 30));
				mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

				JLabel titleLabel = new JLabel("Edit Book");
				titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
				titleLabel.setForeground(Color.WHITE);
				titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

				// Create form fields
				JPanel isbnPanel = createFormField("ISBN:", 100);
				JTextField isbnField = (JTextField) isbnPanel.getComponent(1);
				isbnField.setText(isbn);
				isbnField.setEditable(false);
				isbnField.setBackground(new Color(50, 50, 50));

				JPanel titleFieldPanel = createFormField("Title:", 100);
				JTextField bookTitleField = (JTextField) titleFieldPanel.getComponent(1);
				bookTitleField.setText(title);

				JPanel authorPanel = createFormField("Author:", 100);
				JTextField authorField = (JTextField) authorPanel.getComponent(1);
				authorField.setText(author);

				JPanel categoryPanel = createFormField("Genre:", 100);
				JComboBox<String> categoryCombo = new JComboBox<>();
				categoryCombo.setPreferredSize(new Dimension(300, 25));
				categoryCombo.setBackground(new Color(60, 60, 60));
				categoryCombo.setForeground(Color.WHITE);

				// Load genres
				try {
					String genreSql = "SELECT DISTINCT genre FROM books WHERE genre IS NOT NULL AND genre != '' ORDER BY genre";
					PreparedStatement genreStmt = connection.prepareStatement(genreSql);
					ResultSet genreRs = genreStmt.executeQuery();

					categoryCombo.addItem(""); // Empty option

					while (genreRs.next()) {
						categoryCombo.addItem(genreRs.getString("genre"));
					}

					genreRs.close();
					genreStmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}

				// Add "Other" option and a text field for custom genre
				categoryCombo.addItem("Other");

				// Select the current genre if it exists in the combo box
				boolean genreFound = false;
				for (int i = 0; i < categoryCombo.getItemCount(); i++) {
					if (categoryCombo.getItemAt(i).equals(genre)) {
						categoryCombo.setSelectedIndex(i);
						genreFound = true;
						break;
					}
				}

				JPanel otherGenrePanel = createFormField("Custom Genre:", 100);
				JTextField otherGenreField = (JTextField) otherGenrePanel.getComponent(1);

				// If the genre wasn't found in the combo box, select "Other" and set the custom
				// field
				if (!genreFound && genre != null && !genre.isEmpty()) {
					categoryCombo.setSelectedItem("Other");
					otherGenreField.setText(genre);
					otherGenreField.setEnabled(true);
				} else {
					otherGenreField.setEnabled(false);
				}

				categoryCombo.addActionListener(e -> {
					String selected = (String) categoryCombo.getSelectedItem();
					otherGenreField.setEnabled("Other".equals(selected));
				});

				categoryPanel.remove(1); // Remove the default text field
				categoryPanel.add(categoryCombo); // Add the combo box

				JPanel publisherPanel = createFormField("Publisher:", 100);
				JTextField publisherField = (JTextField) publisherPanel.getComponent(1);
				publisherField.setText(publisher != null ? publisher : "");

				JPanel yearPanel = createFormField("Pub. Year:", 100);
				JTextField yearField = (JTextField) yearPanel.getComponent(1);
				yearField.setText(String.valueOf(year));

				JPanel descPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				descPanel.setBackground(new Color(30, 30, 30));
				descPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

				JLabel descLabel = new JLabel("Description:");
				descLabel.setForeground(Color.WHITE);
				descLabel.setPreferredSize(new Dimension(100, 25));

				descPanel.add(descLabel);

				JTextArea descArea = new JTextArea(5, 25);
				descArea.setLineWrap(true);
				descArea.setWrapStyleWord(true);
				descArea.setBackground(new Color(60, 60, 60));
				descArea.setForeground(Color.WHITE);
				descArea.setCaretColor(Color.WHITE);
				descArea.setText(description != null ? description : "");

				JScrollPane descScroll = new JScrollPane(descArea);
				descScroll.setPreferredSize(new Dimension(300, 100));

				JPanel descWrapperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				descWrapperPanel.setBackground(new Color(30, 30, 30));
				descWrapperPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
				descWrapperPanel.add(descScroll);

				// Count copies
				int totalCopies = 0;
				int availableCopies = 0;

				try {
					String copiesSql = "SELECT COUNT(*) AS total, SUM(CASE WHEN available = true THEN 1 ELSE 0 END) AS available "
							+ "FROM books WHERE isbn = ?";
					PreparedStatement copiesStmt = connection.prepareStatement(copiesSql);
					copiesStmt.setString(1, isbn);
					ResultSet copiesRs = copiesStmt.executeQuery();

					if (copiesRs.next()) {
						totalCopies = copiesRs.getInt("total");
						availableCopies = copiesRs.getInt("available");
					}

					copiesRs.close();
					copiesStmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}

				// Copies info panel
				JPanel copiesInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				copiesInfoPanel.setBackground(new Color(30, 30, 30));
				copiesInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

				JLabel copiesLabel = new JLabel("Copies:");
				copiesLabel.setForeground(Color.WHITE);
				copiesLabel.setPreferredSize(new Dimension(100, 25));

				JLabel copiesInfoLabel = new JLabel(totalCopies + " total (" + availableCopies + " available)");
				copiesInfoLabel.setForeground(Color.WHITE);

				JButton addCopyButton = new JButton("Add Copy");
				addCopyButton.setBackground(new Color(0, 102, 204));
				addCopyButton.setForeground(Color.WHITE);
				addCopyButton.setFocusPainted(false);

				copiesInfoPanel.add(copiesLabel);
				copiesInfoPanel.add(copiesInfoLabel);
				copiesInfoPanel.add(Box.createRigidArea(new Dimension(10, 0)));
				copiesInfoPanel.add(addCopyButton);

				// Buttons panel
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				buttonPanel.setBackground(new Color(30, 30, 30));
				buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

				JButton cancelButton = new JButton("Cancel");
				cancelButton.setBackground(new Color(60, 60, 60));
				cancelButton.setForeground(Color.WHITE);

				JButton saveButton = new JButton("Save Changes");
				saveButton.setBackground(new Color(0, 102, 204));
				saveButton.setForeground(Color.WHITE);

				buttonPanel.add(cancelButton);
				buttonPanel.add(saveButton);

				// Add all panels to main panel
				mainPanel.add(titleLabel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
				mainPanel.add(isbnPanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
				mainPanel.add(titleFieldPanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
				mainPanel.add(authorPanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
				mainPanel.add(categoryPanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
				mainPanel.add(otherGenrePanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
				mainPanel.add(publisherPanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
				mainPanel.add(yearPanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
				mainPanel.add(copiesInfoPanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
				mainPanel.add(descPanel);
				mainPanel.add(descWrapperPanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
				mainPanel.add(buttonPanel);

				dialog.add(new JScrollPane(mainPanel));

				// Add action listeners
				addCopyButton.addActionListener(e -> {
					addBookCopy(isbn, title);
					// Update the copies info
					try {
						String copiesSql = "SELECT COUNT(*) AS total, SUM(CASE WHEN available = true THEN 1 ELSE 0 END) AS available "
								+ "FROM books WHERE isbn = ?";
						PreparedStatement copiesStmt = connection.prepareStatement(copiesSql);
						copiesStmt.setString(1, isbn);
						ResultSet copiesRs = copiesStmt.executeQuery();

						if (copiesRs.next()) {
							int newTotal = copiesRs.getInt("total");
							int newAvailable = copiesRs.getInt("available");
							copiesInfoLabel.setText(newTotal + " total (" + newAvailable + " available)");
						}

						copiesRs.close();
						copiesStmt.close();
					} catch (SQLException ex) {
						ex.printStackTrace();
					}
				});

				cancelButton.addActionListener(e -> dialog.dispose());

				saveButton.addActionListener(e -> {
					// Validate fields
					String newTitle = bookTitleField.getText().trim();
					String newAuthor = authorField.getText().trim();
					String newPublisher = publisherField.getText().trim();
					String yearText = yearField.getText().trim();
					String newDescription = descArea.getText().trim();

					// Get genre based on selection
					String newGenre;
					if (categoryCombo.getSelectedItem().equals("Other")) {
						newGenre = otherGenreField.getText().trim();
					} else {
						newGenre = (String) categoryCombo.getSelectedItem();
					}

					if (newTitle.isEmpty() || newAuthor.isEmpty() || yearText.isEmpty()) {
						JOptionPane.showMessageDialog(dialog,
								"Please fill all required fields (Title, Author, Publication Year)",
								"Missing Information", JOptionPane.WARNING_MESSAGE);
						return;
					}

					int newYear;
					try {
						newYear = Integer.parseInt(yearText);
						if (newYear < 0 || newYear > Calendar.getInstance().get(Calendar.YEAR) + 1) {
							JOptionPane.showMessageDialog(dialog, "Please enter a valid year", "Invalid Year",
									JOptionPane.WARNING_MESSAGE);
							return;
						}
					} catch (NumberFormatException ex) {
						JOptionPane.showMessageDialog(dialog, "Please enter a valid year", "Invalid Year",
								JOptionPane.WARNING_MESSAGE);
						return;
					}

					// Update the book
					updateBook(isbn, newTitle, newAuthor, newGenre, newPublisher, newYear, newDescription);
					dialog.dispose();
				});

				dialog.setVisible(true);

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Show dialog to edit a member
		 */
		private void showEditMemberDialog(String username) {
			try {
				// Get member details
				String sql = "SELECT * FROM users WHERE username = ?";
				PreparedStatement stmt = connection.prepareStatement(sql);
				stmt.setString(1, username);
				ResultSet rs = stmt.executeQuery();

				if (!rs.next()) {
					return;
				}

				String fullName = rs.getString("full_name");
				String email = rs.getString("email");
				String registrationDate = rs.getString("registration_date");

				rs.close();
				stmt.close();

				// Create edit member dialog
				JDialog dialog = new JDialog();
				dialog.setSize(500, 450);
				dialog.setUndecorated(true);
				dialog.setResizable(false);
				dialog.setLocationRelativeTo(LibrarianDashboard.this);

				JPanel mainPanel = new JPanel();
				mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
				mainPanel.setBackground(new Color(30, 30, 30));
				mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

				JLabel titleLabel = new JLabel("Edit Member");
				titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
				titleLabel.setForeground(Color.WHITE);
				titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

				// Create form fields
				JPanel usernamePanel = createFormField("Username:", 100);
				JTextField usernameField = (JTextField) usernamePanel.getComponent(1);
				usernameField.setText(username);
				usernameField.setEditable(false);
				usernameField.setBackground(new Color(50, 50, 50));

				JPanel namePanel = createFormField("Full Name:", 100);
				JTextField nameField = (JTextField) namePanel.getComponent(1);
				nameField.setText(fullName);

				JPanel emailPanel = createFormField("Email:", 100);
				JTextField emailField = (JTextField) emailPanel.getComponent(1);
				emailField.setText(email);

				JPanel registrationPanel = createFormField("Joined:", 100);
				JTextField registrationField = (JTextField) registrationPanel.getComponent(1);
				registrationField.setText(registrationDate);
				registrationField.setEditable(false);
				registrationField.setBackground(new Color(50, 50, 50));

				// Password fields for reset
				JPanel passwordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				passwordPanel.setBackground(new Color(30, 30, 30));
				passwordPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

				JLabel passwordLabel = new JLabel("New Password:");
				passwordLabel.setForeground(Color.WHITE);
				passwordLabel.setPreferredSize(new Dimension(100, 25));

				JPasswordField passwordField = new JPasswordField(20);
				passwordField.setPreferredSize(new Dimension(200, 25));
				passwordField.setBackground(new Color(60, 60, 60));
				passwordField.setForeground(Color.WHITE);
				passwordField.setCaretColor(Color.WHITE);

				JButton resetButton = new JButton("Reset");
				resetButton.setBackground(new Color(204, 51, 51));
				resetButton.setForeground(Color.black);

				passwordPanel.add(passwordLabel);
				passwordPanel.add(passwordField);
				passwordPanel.add(resetButton);

				// Count borrowed books
				int borrowedCount = 0;
				int overdueCount = 0;

				try {
					String borrowedSql = "SELECT COUNT(*) AS total, "
							+ "SUM(CASE WHEN due_date < CURRENT_DATE() THEN 1 ELSE 0 END) AS overdue "
							+ "FROM borrowed_books WHERE username = ? AND return_date IS NULL";
					PreparedStatement borrowedStmt = connection.prepareStatement(borrowedSql);
					borrowedStmt.setString(1, username);
					ResultSet borrowedRs = borrowedStmt.executeQuery();

					if (borrowedRs.next()) {
						borrowedCount = borrowedRs.getInt("total");
						overdueCount = borrowedRs.getInt("overdue");
					}

					borrowedRs.close();
					borrowedStmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}

				// Borrowed books info
				JPanel borrowedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
				borrowedPanel.setBackground(new Color(30, 30, 30));
				borrowedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

				JLabel borrowedLabel = new JLabel("Borrowed Books:");
				borrowedLabel.setForeground(Color.WHITE);
				borrowedLabel.setPreferredSize(new Dimension(100, 25));

				JLabel borrowedInfoLabel = new JLabel(borrowedCount + " books (" + overdueCount + " overdue)");
				borrowedInfoLabel.setForeground(Color.WHITE);

				JButton viewButton = new JButton("View");
				viewButton.setBackground(new Color(0, 102, 204));
				viewButton.setForeground(Color.black);

				borrowedPanel.add(borrowedLabel);
				borrowedPanel.add(borrowedInfoLabel);
				borrowedPanel.add(Box.createRigidArea(new Dimension(10, 0)));
				borrowedPanel.add(viewButton);

				// Buttons panel
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				buttonPanel.setBackground(new Color(30, 30, 30));
				buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

				JButton cancelButton = new JButton("Cancel");
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
				mainPanel.add(registrationPanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
				mainPanel.add(borrowedPanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
				mainPanel.add(new JSeparator());
				mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
				mainPanel.add(passwordPanel);
				mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
				mainPanel.add(buttonPanel);

				dialog.add(new JScrollPane(mainPanel));

				// Add action listeners
				viewButton.addActionListener(e -> showBorrowingHistoryDialog(username));

				resetButton.addActionListener(e -> {
					// Generate a random password
					String newPassword = generateRandomPassword();
					passwordField.setText(newPassword);
					JOptionPane.showMessageDialog(dialog,
							"Generated password: " + newPassword
									+ "\n\nMake sure to save changes and inform the member.",
							"Password Generated", JOptionPane.INFORMATION_MESSAGE);
				});

				cancelButton.addActionListener(e -> dialog.dispose());

				saveButton.addActionListener(e -> {
					// Validate fields
					String newFullName = nameField.getText().trim();
					String newEmail = emailField.getText().trim();
					String newPassword = new String(passwordField.getPassword());

					if (newFullName.isEmpty() || newEmail.isEmpty()) {
						JOptionPane.showMessageDialog(dialog, "Full Name and Email cannot be empty",
								"Missing Information", JOptionPane.WARNING_MESSAGE);
						return;
					}

					// Update the member
					updateMember(username, newPassword.isEmpty() ? null : newPassword, newFullName, newEmail);
					dialog.dispose();
				});

				dialog.setVisible(true);

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

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
		 * Show dialog with member details
		 */
		private void showMemberDetailsDialog(String username) {
			try {
				// Get member details
				String sql = "SELECT * FROM users WHERE username = ?";
				PreparedStatement stmt = connection.prepareStatement(sql);
				stmt.setString(1, username);
				ResultSet rs = stmt.executeQuery();

				if (!rs.next()) {
					return;
				}

				String fullName = rs.getString("full_name");
				String email = rs.getString("email");
				String registrationDate = rs.getString("registration_date");
				String lastLogin = rs.getString("last_login");

				rs.close();
				stmt.close();

				// Count borrowed, overdue books and borrowing history
				int currentlyBorrowed = 0;
				int overdue = 0;
				int totalBorrowed = 0;

				try {
					// Currently borrowed books
					String currentSql = "SELECT COUNT(*) AS total, "
							+ "SUM(CASE WHEN due_date < CURRENT_DATE() THEN 1 ELSE 0 END) AS overdue "
							+ "FROM borrowed_books WHERE username = ? AND return_date IS NULL";
					PreparedStatement currentStmt = connection.prepareStatement(currentSql);
					currentStmt.setString(1, username);
					ResultSet currentRs = currentStmt.executeQuery();

					if (currentRs.next()) {
						currentlyBorrowed = currentRs.getInt("total");
						overdue = currentRs.getInt("overdue");
					}

					currentRs.close();
					currentStmt.close();

					// Total borrowing history
					String historySql = "SELECT COUNT(*) FROM borrowed_books WHERE username = ?";
					PreparedStatement historyStmt = connection.prepareStatement(historySql);
					historyStmt.setString(1, username);
					ResultSet historyRs = historyStmt.executeQuery();

					if (historyRs.next()) {
						totalBorrowed = historyRs.getInt(1);
					}

					historyRs.close();
					historyStmt.close();

				} catch (SQLException e) {
					e.printStackTrace();
				}

				// Create member details dialog
				StringBuilder details = new StringBuilder();
				details.append("Member ID: ").append(username).append("\n");
				details.append("Full Name: ").append(fullName).append("\n");
				details.append("Email: ").append(email).append("\n");
				details.append("Registration Date: ").append(registrationDate).append("\n");

				if (lastLogin != null) {
					details.append("Last Login: ").append(lastLogin).append("\n");
				}

				details.append("\n");
				details.append("Currently Borrowed Books: ").append(currentlyBorrowed).append("\n");
				details.append("Overdue Books: ").append(overdue).append("\n");
				details.append("Total Books Borrowed: ").append(totalBorrowed).append("\n");

				JTextArea detailsArea = new JTextArea(details.toString());
				detailsArea.setEditable(false);
				detailsArea.setBackground(new Color(40, 40, 40));
				detailsArea.setForeground(Color.WHITE);
				detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
				detailsArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Show dialog with member's borrowing history
		 */
		private void showBorrowingHistoryDialog(String username) {
			try {
				// Get member name
				String nameSql = "SELECT full_name FROM users WHERE username = ?";
				PreparedStatement nameStmt = connection.prepareStatement(nameSql);
				nameStmt.setString(1, username);
				ResultSet nameRs = nameStmt.executeQuery();

				if (!nameRs.next()) {
					return;
				}

				String fullName = nameRs.getString("full_name");
				nameRs.close();
				nameStmt.close();

				// Create borrowing history dialog
				JDialog dialog = new JDialog();
				dialog.setSize(800, 500);
				dialog.setUndecorated(true);
				dialog.setResizable(false);

				dialog.setLocationRelativeTo(LibrarianDashboard.this);

				JPanel mainPanel = new JPanel(new BorderLayout());
				mainPanel.setBackground(new Color(30, 30, 30));
				mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

				JLabel titleLabel = new JLabel("Borrowing History for " + fullName + " (" + username + ")");
				titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
				titleLabel.setForeground(Color.WHITE);

				mainPanel.add(titleLabel, BorderLayout.NORTH);

				// Create table for borrowing history
				String[] columns = { "Book Title", "ISBN", "Borrow Date", "Due Date", "Return Date", "Status" };
				DefaultTableModel model = new DefaultTableModel(columns, 0) {
					@Override
					public boolean isCellEditable(int row, int column) {
						return false;
					}
				};

				JTable historyTable = new JTable(model);
				historyTable.setBackground(new Color(40, 40, 40));
				historyTable.setForeground(Color.WHITE);
				historyTable.setGridColor(new Color(60, 60, 60));
				historyTable.getTableHeader().setBackground(new Color(50, 50, 50));
				historyTable.getTableHeader().setForeground(Color.WHITE);
				historyTable.setRowHeight(25);

				// Set cell renderer for status column
				historyTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
					@Override
					public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
							boolean hasFocus, int row, int column) {
						Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
								column);

						String status = (String) value;
						if (status.equals("Overdue")) {
							c.setForeground(new Color(255, 100, 100));
						} else if (status.equals("Returned")) {
							c.setForeground(new Color(100, 255, 100));
						} else {
							c.setForeground(Color.WHITE);
						}

						return c;
					}
				});

				// Load borrowing history
				String sql = "SELECT b.title, b.isbn, bb.borrow_date, bb.due_date, bb.return_date, " + "CASE "
						+ "   WHEN bb.return_date IS NOT NULL THEN 'Returned' "
						+ "   WHEN bb.due_date < CURRENT_DATE() THEN 'Overdue' " + "   ELSE 'Borrowed' "
						+ "END AS status " + "FROM borrowed_books bb " + "JOIN books b ON bb.book_id = b.id "
						+ "WHERE bb.username = ? " + "ORDER BY bb.borrow_date DESC";
				PreparedStatement stmt = connection.prepareStatement(sql);
				stmt.setString(1, username);
				ResultSet rs = stmt.executeQuery();

				while (rs.next()) {
					model.addRow(new Object[] { rs.getString("title"), rs.getString("isbn"),
							rs.getString("borrow_date"), rs.getString("due_date"),
							rs.getString("return_date") != null ? rs.getString("return_date") : "-",
							rs.getString("status") });
				}

				rs.close();
				stmt.close();

				JScrollPane scrollPane = new JScrollPane(historyTable);
				scrollPane.getViewport().setBackground(new Color(40, 40, 40));

				mainPanel.add(scrollPane, BorderLayout.CENTER);

				// Buttons panel
				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				buttonPanel.setBackground(new Color(30, 30, 30));

				JButton closeButton = new JButton("Close");
				closeButton.setBackground(new Color(60, 60, 60));
				closeButton.setForeground(Color.black);

				closeButton.addActionListener(e -> dialog.dispose());

				buttonPanel.add(closeButton);

				mainPanel.add(buttonPanel, BorderLayout.SOUTH);

				dialog.add(mainPanel);
				dialog.setVisible(true);

			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

}