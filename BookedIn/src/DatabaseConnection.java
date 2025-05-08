import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Database connection constants
    private static final String DB_URL = "jdbc:mysql://localhost:3306/BookedIN";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    
    // Keep track of the active connection
    private static Connection activeConnection = null;
    
    /**
     * Get a database connection
     */
    public static synchronized Connection getConnection() throws SQLException {
        try {
            // Check if connection exists and is valid
            if (activeConnection != null && !activeConnection.isClosed() && activeConnection.isValid(5)) {
                return activeConnection;
            }
            
            // Create a new connection if needed
            Class.forName("com.mysql.cj.jdbc.Driver");
            activeConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Database connected");
            return activeConnection;
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database driver not found", e);
        }
    }
    
    /**
     * Close the active connection
     */
    public static synchronized void closeConnection() {
        if (activeConnection != null) {
            try {
                if (!activeConnection.isClosed()) {
                    activeConnection.close();
                }
                activeConnection = null;
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}