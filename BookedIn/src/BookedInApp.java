import java.sql.*;
import javax.swing.JOptionPane;

public class BookedInApp {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/BookedIN";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    
    private static Connection connection = null;
    
    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Load the MySQL JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                // Create connection
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("Database connection established");
            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(null, 
                    "Database driver not found. Please add MySQL connector to your classpath.", 
                    "Database Error", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, 
                    "Could not connect to database. Please check your database settings.\nError: " + e.getMessage(), 
                    "Database Error", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
        return connection;
    }
    
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("Database connection closed");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Validate user login credentials
    public static boolean validateLogin(String username, String password) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            rs = stmt.executeQuery();
            
            boolean valid = rs.next();
            return valid;
        } catch (SQLException e) {
            System.err.println("Error validating login: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                // Don't close the connection here
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Get user information by username
    public static ResultSet getUserInfo(String username) {
        String query = "SELECT * FROM users WHERE username = ?";
        
        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Error retrieving user information: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }
    
    // Load statistics for a user
    public static ResultSet getUserStatistics(String username) {
        String query = "SELECT " +
                      "(SELECT COUNT(*) FROM books) AS total_books, " +
                      "(SELECT COUNT(*) FROM borrowed_books WHERE username = ?) AS borrowed_books, " +
                      "(SELECT title FROM books WHERE id IN " +
                      "  (SELECT book_id FROM book_views WHERE username = ? ORDER BY view_date DESC LIMIT 1)) " +
                      "AS last_viewed_book, " +
                      "(SELECT author FROM books WHERE id IN " +
                      "  (SELECT book_id FROM book_views WHERE username = ? ORDER BY view_date DESC LIMIT 1)) " +
                      "AS last_viewed_author";
        
        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            pstmt.setString(3, username);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Error loading statistics: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }
    
    // Load popular books
    public static ResultSet getPopularBooks() {
        String query = "SELECT b.id, b.title, b.author, COUNT(bb.book_id) as borrow_count " +
                      "FROM books b " +
                      "LEFT JOIN borrowed_books bb ON b.id = bb.book_id " +
                      "GROUP BY b.id " +
                      "ORDER BY borrow_count DESC " +
                      "LIMIT 5";
        
        try {
            Connection conn = getConnection();
            Statement stmt = conn.createStatement();
            return stmt.executeQuery(query);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Error loading popular books: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }
    
    // Load user's borrowed books with due dates
    public static ResultSet getUserBorrowedBooks(String username) {
        String query = "SELECT b.title, b.author, bb.borrow_date, bb.due_date, " +
                      "CASE WHEN bb.due_date < CURRENT_DATE THEN 'Overdue' ELSE 'On time' END AS status " +
                      "FROM borrowed_books bb " +
                      "JOIN books b ON bb.book_id = b.id " +
                      "WHERE bb.username = ? AND bb.return_date IS NULL " +
                      "ORDER BY bb.due_date ASC";
        
        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Error loading borrowed books: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }
    
    // Search books by title, author, or genre
    public static ResultSet searchBooks(String searchTerm, String sortBy) {
        String query = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? OR genre LIKE ?";
        
        // Add sorting
        if (sortBy != null && !sortBy.isEmpty()) {
            query += " ORDER BY " + sortBy;
        }
        
        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Error searching books: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }
    
    // Get book details by ID
    public static ResultSet getBookDetails(int bookId) {
        String query = "SELECT * FROM books WHERE id = ?";
        
        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, bookId);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Error loading book details: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }
    
    // Get book reviews
    public static ResultSet getBookReviews(int bookId) {
        String query = "SELECT r.username, r.rating, r.content, r.review_date " +
                      "FROM book_reviews r " +
                      "WHERE r.book_id = ? " +
                      "ORDER BY r.review_date DESC";
        
        try {
            Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, bookId);
            return pstmt.executeQuery();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Error loading book reviews: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }
    
    // Add a new book review
    public static boolean addBookReview(int bookId, String username, int rating, String content) {
        String query = "INSERT INTO book_reviews (book_id, username, rating, content, review_date) " +
                      "VALUES (?, ?, ?, ?, CURRENT_DATE())";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, bookId);
            pstmt.setString(2, username);
            pstmt.setInt(3, rating);
            pstmt.setString(4, content);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, 
                "Error adding review: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }
    
    // Checkout a book
    public static boolean checkoutBook(int bookId, String username) {
        // First check if book is available
        String checkQuery = "SELECT available FROM books WHERE id = ?";
        String updateBookQuery = "UPDATE books SET available = 0 WHERE id = ?";
        String borrowQuery = "INSERT INTO borrowed_books (book_id, username, borrow_date, due_date) " +
                            "VALUES (?, ?, CURRENT_DATE(), DATE_ADD(CURRENT_DATE(), INTERVAL 14 DAY))";
        
        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement updateBookStmt = null;
        PreparedStatement borrowStmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // Check if book is available
            checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setInt(1, bookId);
            rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getBoolean("available")) {
                // Update book status to unavailable
                updateBookStmt = conn.prepareStatement(updateBookQuery);
                updateBookStmt.setInt(1, bookId);
                updateBookStmt.executeUpdate();
                
                // Create borrow record
                borrowStmt = conn.prepareStatement(borrowQuery);
                borrowStmt.setInt(1, bookId);
                borrowStmt.setString(2, username);
                borrowStmt.executeUpdate();
                
                conn.commit(); // Commit transaction
                return true;
            } else {
                conn.rollback();
                return false; // Book not available
            }
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            
            JOptionPane.showMessageDialog(null, 
                "Error checking out book: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
            
        } finally {
            try {
                if (rs != null) rs.close();
                if (checkStmt != null) checkStmt.close();
                if (updateBookStmt != null) updateBookStmt.close();
                if (borrowStmt != null) borrowStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    // Return a book
    public static boolean returnBook(int bookId, String username) {
        String updateBookQuery = "UPDATE books SET available = 1 WHERE id = ?";
        String updateBorrowQuery = "UPDATE borrowed_books SET return_date = CURRENT_DATE() " +
                                  "WHERE book_id = ? AND username = ? AND return_date IS NULL";
        
        Connection conn = null;
        PreparedStatement updateBookStmt = null;
        PreparedStatement updateBorrowStmt = null;
        
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // Update book status to available
            updateBookStmt = conn.prepareStatement(updateBookQuery);
            updateBookStmt.setInt(1, bookId);
            updateBookStmt.executeUpdate();
            
            // Update borrow record with return date
            updateBorrowStmt = conn.prepareStatement(updateBorrowQuery);
            updateBorrowStmt.setInt(1, bookId);
            updateBorrowStmt.setString(2, username);
            int rowsAffected = updateBorrowStmt.executeUpdate();
            
            if (rowsAffected > 0) {
                conn.commit(); // Commit transaction
                return true;
            } else {
                conn.rollback();
                return false; // Book not found in borrowed books
            }
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            
            JOptionPane.showMessageDialog(null, 
                "Error returning book: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
            
        } finally {
            try {
                if (updateBookStmt != null) updateBookStmt.close();
                if (updateBorrowStmt != null) updateBorrowStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
    
}

