import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
	
public class DatabaseSetup {
	public static final String DB_URL = "jdbc:mysql://localhost:3306/";
	public static final String DB_NAME = "BookedIN";
	public static final String DB_USER = "root";
	public static final String DB_PASSWORD = "";
	private static JFrame loginScreen = null;

	private static Connection connection = null;

	public static Connection getConnection() {
	    try {
	        return DatabaseConnection.getConnection();
	    } catch (SQLException e) {
	        JOptionPane.showMessageDialog(null,
	                "Could not connect to database. Please check your database settings.\nError: " + e.getMessage(),
	                "Database Error", JOptionPane.ERROR_MESSAGE);
	        e.printStackTrace();
	        return null;
	    }
	}

	private static void createDatabase() {
		try {
			Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
			Statement stmt = conn.createStatement();
			
			stmt.executeUpdate("DROP DATABASE IF EXISTS " + DB_NAME);
			stmt.executeUpdate("CREATE DATABASE " + DB_NAME);
			System.out.println("Database created successfully");

			// Close this initial connection
			stmt.close();
			conn.close();

		} catch (SQLException e) {
			showError("Error creating database", e);
		}
	}

	private static void createTables() {
		try {
			Connection conn = getConnection();
			Statement stmt = conn.createStatement();

			// Users Table
			// Users Table
	        String createUsersTable = "CREATE TABLE users (" + "username VARCHAR(50) PRIMARY KEY,"
	                + "password VARCHAR(100) NOT NULL," + "full_name VARCHAR(100) NOT NULL,"
	                + "email VARCHAR(100) NOT NULL UNIQUE,"
	                + "phone VARCHAR(20)," 
	                + "role ENUM('admin', 'librarian', 'member') DEFAULT 'member'," + "registration_date DATE NOT NULL,"
	                + "last_login DATETIME" + ")";
	        stmt.executeUpdate(createUsersTable);
	        System.out.println("Users table created");

			// Books Table
			String createBooksTable = "CREATE TABLE books (" + "id INT AUTO_INCREMENT PRIMARY KEY,"
					+ "title VARCHAR(200) NOT NULL," + "author VARCHAR(100) NOT NULL," + "isbn VARCHAR(20),"
					+ "year INT," + "genre VARCHAR(50)," + "description TEXT," + "publisher VARCHAR(100),"
					+ "location VARCHAR(50)," + "available BOOLEAN DEFAULT TRUE," + "cover_image VARCHAR(200),"
					+ "date_added DATE NOT NULL" + ")";
			stmt.executeUpdate(createBooksTable);
			System.out.println("Books table created");

			// Borrowed Books Table
			String createBorrowedBooksTable = "CREATE TABLE borrowed_books (" + "id INT AUTO_INCREMENT PRIMARY KEY,"
					+ "book_id INT NOT NULL," + "username VARCHAR(50) NOT NULL," + "borrow_date DATE NOT NULL,"
					+ "due_date DATE NOT NULL," + "return_date DATE,"
					+ "FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,"
					+ "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE" + ")";
			stmt.executeUpdate(createBorrowedBooksTable);
			System.out.println("Borrowed Books table created");

			// Book Reviews Table
			String createBookReviewsTable = "CREATE TABLE book_reviews (" + "id INT AUTO_INCREMENT PRIMARY KEY,"
					+ "book_id INT NOT NULL," + "username VARCHAR(50) NOT NULL,"
					+ "rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5)," + "content TEXT,"
					+ "review_date DATE NOT NULL," + "FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,"
					+ "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE,"
					+ "UNIQUE (book_id, username)" + ")";
			stmt.executeUpdate(createBookReviewsTable);
			System.out.println("Book Reviews table created");

			// Book Views (Recently Viewed) Table
			String createBookViewsTable = "CREATE TABLE book_views (" + "id INT AUTO_INCREMENT PRIMARY KEY,"
					+ "book_id INT NOT NULL," + "username VARCHAR(50) NOT NULL," + "view_date DATETIME NOT NULL,"
					+ "FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,"
					+ "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE" + ")";
			stmt.executeUpdate(createBookViewsTable);
			System.out.println("Book Views table created");

			// Waitlist Table
			String createWaitlistTable = "CREATE TABLE waitlist (" + "id INT AUTO_INCREMENT PRIMARY KEY,"
					+ "book_id INT NOT NULL," + "username VARCHAR(50) NOT NULL," + "request_date DATETIME NOT NULL,"
					+ "status ENUM('waiting', 'notified', 'expired') DEFAULT 'waiting'," + "notification_date DATETIME,"
					+ "FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,"
					+ "FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE,"
					+ "UNIQUE (book_id, username)" + ")";
			stmt.executeUpdate(createWaitlistTable);
			System.out.println("Waitlist table created");

			// Create indexes for performance
			stmt.executeUpdate("CREATE INDEX idx_books_title_author ON books(title, author)");
			stmt.executeUpdate("CREATE INDEX idx_borrowed_username ON borrowed_books(username)");
			stmt.executeUpdate("CREATE INDEX idx_borrowed_book_id ON borrowed_books(book_id)");
			stmt.executeUpdate("CREATE INDEX idx_borrowed_due_date ON borrowed_books(due_date)");
			stmt.executeUpdate("CREATE INDEX idx_book_views_username ON book_views(username)");
			System.out.println("Indexes created");

		} catch (SQLException e) {
			showError("Error creating tables", e);
		}
	}

	private static void insertSampleData() {
	    try {
	        Connection conn = getConnection();
	        Statement stmt = conn.createStatement();

	        // Check if sample data already exists
	        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
	        rs.next();
	        int userCount = rs.getInt(1);

	        if (userCount > 0) {
	            System.out.println("Sample data already exists. Skipping insertion.");
	            return;
	        }

	        // First, update the users table to include the phone column if it doesn't exist
	        try {
	            stmt.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20)");
	            System.out.println("Added phone column to users table");
	        } catch (SQLException e) {
	            // If the column already exists or there's another issue
	            System.out.println("Note: " + e.getMessage());
	        }

	        // Insert sample users with phone numbers
	        String insertUsers = "INSERT INTO users (username, password, full_name, email, phone, role, registration_date) VALUES "
	                + "('admin', 'admin123', 'Admin User', 'admin@bookedin.com', '+639171234567', 'admin', CURRENT_DATE()), "
	                + "('eugene', 'password', 'Eugene Esguerra', 'eugene@example.com', '+639189876543', 'member', CURRENT_DATE()), "
	                + "('jenelyn', 'password', 'Jenelyn Cruz', 'jenelyn@example.com', '+639157654321', 'librarian', CURRENT_DATE()),"
	                + "('test', 'test', 'test test', 'test@yahoo.com', '+639157654321', 'librarian', CURRENT_DATE())";;
	        stmt.executeUpdate(insertUsers);
	        System.out.println("Sample users inserted");

	        // Insert sample books
	        String insertBooks = "INSERT INTO books (title, author, isbn, year, genre, description, publisher, location, available, date_added) VALUES "
	                + "('Physics 1st Ed.', 'Jenelyn Cruz', '9781234567897', 2019, 'Science/Educational', "
	                + "'A comprehensive introduction to physics principles and concepts.', 'Science Publishers', 'Second Floor, Shelf B4', true, CURRENT_DATE()),"
	                + "('The Great Gatsby', 'F. Scott Fitzgerald', '9780743273565', 1925, 'Fiction/Classic', "
	                + "'The story of eccentric millionaire Jay Gatsby and his passion for the beautiful Daisy Buchanan.', 'Scribner', 'First Floor, Shelf A2', true, CURRENT_DATE()),"
	                + "('To Kill a Mockingbird', 'Harper Lee', '9780061120084', 1960, 'Fiction/Classic', "
	                + "'The story of racial injustice and the destruction of innocence.', 'HarperCollins', 'First Floor, Shelf A1', true, CURRENT_DATE()),"
	                + "('1984', 'George Orwell', '9780451524935', 1949, 'Fiction/Dystopian', "
	                + "'A dystopian social science fiction novel and cautionary tale about totalitarianism.', 'Penguin Books', 'First Floor, Shelf A3', true, CURRENT_DATE()),"
	                + "('The Hobbit', 'J.R.R. Tolkien', '9780547928227', 1937, 'Fiction/Fantasy', "
	                + "'The classic prelude to The Lord of the Rings trilogy.', 'Houghton Mifflin', 'First Floor, Shelf C1', true, CURRENT_DATE()),"
	                + "('Database Systems', 'Thomas Connolly', '9780321523068', 2014, 'Computer Science', "
	                + "'A comprehensive introduction to database systems and design.', 'Pearson', 'Second Floor, Shelf B1', true, CURRENT_DATE()),"
	                + "('Introduction to Algorithms', 'Thomas H. Cormen', '9780262033848', 2009, 'Computer Science', "
	                + "'A comprehensive introduction to algorithms.', 'MIT Press', 'Second Floor, Shelf B2', true, CURRENT_DATE())";
	        stmt.executeUpdate(insertBooks);
	        System.out.println("Sample books inserted");

	        // Insert sample borrowed books for Eugene
	        String borrowBooks = "INSERT INTO borrowed_books (book_id, username, borrow_date, due_date) VALUES "
	                + "(1, 'eugene', DATE_SUB(CURRENT_DATE(), INTERVAL 5 DAY), DATE_ADD(CURRENT_DATE(), INTERVAL 9 DAY)), "
	                + "(2, 'eugene', DATE_SUB(CURRENT_DATE(), INTERVAL 8 DAY), DATE_ADD(CURRENT_DATE(), INTERVAL 6 DAY)), "
	                + "(3, 'eugene', DATE_SUB(CURRENT_DATE(), INTERVAL 14 DAY), DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY))";
	        stmt.executeUpdate(borrowBooks);

	        // Update these books as not available
	        stmt.executeUpdate("UPDATE books SET available = false WHERE id IN (1, 2, 3)");
	        System.out.println("Sample borrowed books inserted");

	        // Insert sample book reviews
	        String insertReviews = "INSERT INTO book_reviews (book_id, username, rating, content, review_date) VALUES "
	                + "(1, 'eugene', 5, 'Excellent book for beginners!', CURRENT_DATE()), "
	                + "(2, 'eugene', 4, 'A classic that still resonates today.', CURRENT_DATE()), "
	                + "(4, 'eugene', 5, 'A profound and disturbing vision of the future.', CURRENT_DATE()), "
	                + "(1, 'jenelyn', 5, 'As the author, I think it turned out pretty well!', CURRENT_DATE())";
	        stmt.executeUpdate(insertReviews);
	        System.out.println("Sample book reviews inserted");

	        // Insert sample recently viewed books
	        String insertViews = "INSERT INTO book_views (book_id, username, view_date) VALUES "
	                + "(1, 'eugene', NOW()), " + "(4, 'eugene', DATE_SUB(NOW(), INTERVAL 1 HOUR)), "
	                + "(5, 'eugene', DATE_SUB(NOW(), INTERVAL 3 HOUR))";
	        stmt.executeUpdate(insertViews);
	        System.out.println("Sample book views inserted");

	    } catch (SQLException e) {
	        showError("Error inserting sample data", e);
	    }
	}

	private static void showError(String message, SQLException e) {
		System.err.println(message + ": " + e.getMessage());
		JOptionPane.showMessageDialog(null, message + ": " + e.getMessage(), "Database Error",
				JOptionPane.ERROR_MESSAGE);
		e.printStackTrace();
	}

	// Add authentication methods to match BookedInApp for login compatibility
	public static boolean validateLogin(String username, String password) {
		String query = "SELECT * FROM users WHERE username = ? AND password = ?";

		try {
			Connection conn = getConnection();
			PreparedStatement pstmt = conn.prepareStatement(query);

			pstmt.setString(1, username);
			pstmt.setString(2, password); // In a real app, use password hashing

			ResultSet rs = pstmt.executeQuery();
			boolean result = rs.next(); // Returns true if a user was found

			rs.close();
			pstmt.close();
			return result;

		} catch (SQLException e) {
			JOptionPane.showMessageDialog(null, "Login validation failed: " + e.getMessage(), "Database Error",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
			return false;
		}
	}

	// Welcome Screen class included in the same file
	static class WelcomeScreen extends JFrame {
	    private JFrame loginScreen;

	    public WelcomeScreen() {
	        setTitle("BookedIn");
	        setSize(600, 350);
	        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        setLocationRelativeTo(null);
	        setUndecorated(true);
	        
	        // Add a subtle border to frame
	        getRootPane().setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 80), 1));
	        
	        
	        
	        // Add window drag capability
	        addWindowDragCapability();
	        
	        // Main panel with dark theme
	        JPanel mainPanel = new JPanel();
	        mainPanel.setLayout(new BorderLayout());
	        mainPanel.setBackground(new Color(25, 25, 30)); // Slightly less harsh dark background
	        
	        // Add close button to top-right
	        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
	        topPanel.setOpaque(false);
	        JButton closeButton = new JButton("×");
	        closeButton.setFont(new Font("Arial", Font.BOLD, 18));
	        closeButton.setForeground(new Color(180, 180, 180));
	        closeButton.setBackground(null);
	        closeButton.setBorderPainted(false);
	        closeButton.setFocusPainted(false);
	        closeButton.setContentAreaFilled(false);
	        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
	        closeButton.addActionListener(e -> {
	            dispose();
	            System.exit(0);
	        });
	        topPanel.add(closeButton);
	        mainPanel.add(topPanel, BorderLayout.NORTH);
	        
	        // Logo Panel
	        JPanel logoPanel = new JPanel();
	        logoPanel.setBackground(new Color(25, 25, 30));
	        logoPanel.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
	        
	        JLabel titleLabel = new JLabel("BookedIn");
	        titleLabel.setFont(new Font("Arial", Font.BOLD, 42));
	        titleLabel.setForeground(Color.WHITE);
	        
	        JLabel subtitleLabel = new JLabel("A Digital Library Management System");
	        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
	        subtitleLabel.setForeground(new Color(180, 180, 190));
	        
	        logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
	        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
	        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
	        logoPanel.add(titleLabel);
	        logoPanel.add(Box.createRigidArea(new Dimension(0, 15)));
	        logoPanel.add(subtitleLabel);
	        
	        // Buttons Panel
	        JPanel buttonsPanel = new JPanel();
	        buttonsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 30, 20));
	        buttonsPanel.setBackground(new Color(25, 25, 30));
	        
	        JButton loginButton = createStyledButton("Login", new Color(200, 30, 30));
	        JButton signUpButton = createStyledButton("Sign Up", new Color(200, 30, 30));
	        
	        loginButton.addActionListener((ActionEvent e) -> {
	            dispose();
	            // Create a modified version of LoginScreen that uses DatabaseSetup instead of BookedInApp
	            loginScreen = new JFrame("BookedIn - Login");
	            CustomLoginScreen screen = new CustomLoginScreen();
	            
	            // Pass the frame reference to the CustomLoginScreen
	            screen.setParentFrame(loginScreen);
	            
	            loginScreen.setSize(screen.getSize());
	            loginScreen.setLayout(new BorderLayout());
	            loginScreen.add(screen.getContentPane());
	            loginScreen.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	            loginScreen.setLocationRelativeTo(null);
	            loginScreen.setResizable(false);
	            loginScreen.setUndecorated(true);
	            loginScreen.setVisible(true);
	        });
	        
	        signUpButton.addActionListener((ActionEvent e) -> {
	            dispose();
	            // Create a modified version of LoginScreen that uses DatabaseSetup
	            loginScreen = new JFrame("BookedIn - Login") {
	                {
	                    LoginScreen screen = new CustomLoginScreen();
	                    setSize(screen.getSize());
	                    setLayout(new BorderLayout());
	                    add(screen.getContentPane());
	                    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	                    setLocationRelativeTo(null);
	                    setResizable(false);
	                    setUndecorated(true);
	                    
	                    // Show registration dialog after rendering
	                    SwingUtilities.invokeLater(() -> {
	                        if (screen instanceof CustomLoginScreen) {
	                            ((CustomLoginScreen) screen).showRegistrationDialog();
	                        }
	                    });
	                }
	            };
	            loginScreen.setVisible(true);
	        });
	        
	        buttonsPanel.add(loginButton);
	        buttonsPanel.add(signUpButton);
	        
	        JPanel centerPanel = new JPanel(new GridBagLayout());
	        centerPanel.setBackground(new Color(25, 25, 30));
	        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 40, 0));
	        centerPanel.add(buttonsPanel);
	        
	        mainPanel.add(logoPanel, BorderLayout.CENTER);
	        mainPanel.add(centerPanel, BorderLayout.SOUTH);
	        
	        add(mainPanel);
	    }
	    
	    private JButton createStyledButton(String text, Color baseColor) {
	        JButton button = new JButton(text);
	        button.setFont(new Font("Arial", Font.BOLD, 14));
	        button.setForeground(Color.WHITE);
	        button.setBackground(baseColor);
	        button.setFocusPainted(false);
	        button.setBorderPainted(false);
	        button.setPreferredSize(new Dimension(130, 45));
	        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
	        
	        // Add hover effect
	        button.addMouseListener(new java.awt.event.MouseAdapter() {
	            @Override
	            public void mouseEntered(java.awt.event.MouseEvent evt) {
	                button.setBackground(new Color(
	                    Math.min(baseColor.getRed() + 20, 255),
	                    Math.min(baseColor.getGreen() + 20, 255),
	                    Math.min(baseColor.getBlue() + 20, 255)
	                ));
	            }
	            
	            @Override
	            public void mouseExited(java.awt.event.MouseEvent evt) {
	                button.setBackground(baseColor);
	            }
	        });
	        
	        return button;
	    }
	    
	    private void addWindowDragCapability() {
	        final Point[] dragPoint = {new Point()};
	        
	        addMouseListener(new java.awt.event.MouseAdapter() {
	            @Override
	            public void mousePressed(java.awt.event.MouseEvent e) {
	                dragPoint[0].x = e.getX();
	                dragPoint[0].y = e.getY();
	            }
	        });
	        
	        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
	            @Override
	            public void mouseDragged(java.awt.event.MouseEvent e) {
	                Point location = getLocation();
	                setLocation(location.x + e.getX() - dragPoint[0].x, 
	                            location.y + e.getY() - dragPoint[0].y);
	            }
	        });
	    }
	}

	// Custom login screen that uses DatabaseSetup instead of BookedInApp
	static class CustomLoginScreen extends LoginScreen {
	    private JFrame parentFrame; // Add reference to parent frame
	    
	    public CustomLoginScreen() {
	        super(); // Call the parent constructor
	        // Replace the login button's action listener
	        loginButton.removeActionListener(loginButton.getActionListeners()[0]);
	        // Modify the loginButton action listener in CustomLoginScreen class
	        loginButton.addActionListener(new ActionListener() {
	            @Override
	            public void actionPerformed(ActionEvent e) {
	                String username = usernameField.getText();
	                String password = new String(passwordField.getPassword());
	                if (username.isEmpty() || password.isEmpty()) {
	                    JOptionPane.showMessageDialog(CustomLoginScreen.this, "Username and password cannot be empty",
	                            "Login Error", JOptionPane.ERROR_MESSAGE);
	                    return;
	                }
	                // Use DatabaseSetup instead of BookedInApp
	                if (DatabaseSetup.validateLogin(username, password)) {
	                    // Close the login screen properly
	                    if (parentFrame != null) {
	                        parentFrame.dispose(); // Dispose the parent JFrame
	                    }
	                    
	                    // Initialize the event manager if not already done
	                    DatabaseSetup.initEventManager();
	                    // Determine user role and open appropriate dashboard
	                    try {
	                        Connection conn = DatabaseSetup.getConnection();
	                        String query = "SELECT role FROM users WHERE username = ?";
	                        PreparedStatement pstmt = conn.prepareStatement(query);
	                        pstmt.setString(1, username);
	                        ResultSet rs = pstmt.executeQuery();
	                        if (rs.next()) {
	                            String role = rs.getString("role");
	                            final String finalUsername = username;
	                            SwingUtilities.invokeLater(() -> {
	                                // Check if the role is admin and open the AdminDashboard
	                                if (role.equals("admin")) {
	                                    new AdminDashboard(finalUsername).setVisible(true);
	                                } else if (role.equals("librarian")) {
	                                    new LibrarianDashboard(finalUsername).setVisible(true);
	                                } else {
	                                    new MemberDashboard(finalUsername).setVisible(true);
	                                }
	                            });
	                        }
	                        rs.close();
	                        pstmt.close();
	                    } catch (SQLException ex) {
	                        ex.printStackTrace();
	                        // Default to member dashboard if role lookup fails
	                        final String finalUsername = username;
	                        SwingUtilities.invokeLater(() -> {
	                            new MemberDashboard(finalUsername).setVisible(true);
	                        });
	                    }
	                } else {
	                    JOptionPane.showMessageDialog(CustomLoginScreen.this, "Invalid username or password",
	                            "Login Error", JOptionPane.ERROR_MESSAGE);
	                }
	            }
	        });
	    }
	    
	    // Setter for parent frame reference
	    public void setParentFrame(JFrame frame) {
	        this.parentFrame = frame;
	    }
	}

	public static void initEventManager() {
		// Simply get the instance to initialize it
		DatabaseEventManager.getInstance();
		System.out.println("Database event manager initialized");
	}

	// Modify the main method to initialize the event manager
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		createDatabase();
		createTables();
		insertSampleData();

		// Initialize the event manager
		initEventManager();

		System.out.println("Database setup completed successfully!");

		// Launch the welcome screen after database setup
		SwingUtilities.invokeLater(() -> {
			new WelcomeScreen().setVisible(true);
		});
	}

}