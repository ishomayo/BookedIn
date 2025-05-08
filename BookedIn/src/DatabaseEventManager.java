import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DatabaseEventManager - A singleton class that manages database events and notifies listeners
 * This class implements the Observer pattern to notify components when database changes occur
 */
public class DatabaseEventManager {
    // Singleton instance
    private static DatabaseEventManager instance;
    
    // Define event types as constants
    public static final String EVENT_BOOK_CHECKOUT = "BOOK_CHECKOUT";
    public static final String EVENT_BOOK_RETURN = "BOOK_RETURN";
    public static final String EVENT_BOOK_RENEWAL = "BOOK_RENEWAL";
    public static final String EVENT_MEMBER_ADDED = "MEMBER_ADDED";
    public static final String EVENT_BOOK_ADDED = "BOOK_ADDED";
    public static final String EVENT_DATA_CHANGED = "DATA_CHANGED"; // Generic event for any data change
    
    // Map to store listeners for different event types
    private Map<String, List<DatabaseEventListener>> listeners;
    
    // Private constructor for singleton
    private DatabaseEventManager() {
        listeners = new HashMap<>();
    }
    
    // Get singleton instance
    public static synchronized DatabaseEventManager getInstance() {
        if (instance == null) {
            instance = new DatabaseEventManager();
        }
        return instance;
    }
    
    /**
     * Add a listener for a specific event type
     * 
     * @param eventType The type of event to listen for
     * @param listener The listener to add
     */
    public void addListener(String eventType, DatabaseEventListener listener) {
        List<DatabaseEventListener> eventListeners = listeners.get(eventType);
        
        if (eventListeners == null) {
            eventListeners = new ArrayList<>();
            listeners.put(eventType, eventListeners);
        }
        
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }
    
    /**
     * Remove a listener for a specific event type
     * 
     * @param eventType The type of event
     * @param listener The listener to remove
     */
    public void removeListener(String eventType, DatabaseEventListener listener) {
        List<DatabaseEventListener> eventListeners = listeners.get(eventType);
        
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }
    
    /**
     * Fire an event to notify all listeners
     * 
     * @param eventType The type of event that occurred
     * @param data Additional data related to the event (can be null)
     */
    public void fireEvent(String eventType, Object data) {
        List<DatabaseEventListener> eventListeners = listeners.get(eventType);
        
        if (eventListeners != null) {
            // Create a new event
            DatabaseEvent event = new DatabaseEvent(eventType, data);
            
            // Notify all listeners
            for (DatabaseEventListener listener : eventListeners) {
                listener.onDatabaseEvent(event);
            }
        }
    }
    
    /**
     * Inner class representing a database event
     */
    public static class DatabaseEvent {
        private String eventType;
        private Object data;
        
        public DatabaseEvent(String eventType, Object data) {
            this.eventType = eventType;
            this.data = data;
        }
        
        public String getEventType() {
            return eventType;
        }
        
        public Object getData() {
            return data;
        }
    }
}

/**
 * Interface for objects that want to listen for database events
 */
interface DatabaseEventListener {
    void onDatabaseEvent(DatabaseEventManager.DatabaseEvent event);
}