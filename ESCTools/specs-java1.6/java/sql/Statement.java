package java.sql;

public interface Statement {
    
    ResultSet executeQuery(String sql) throws SQLException;
    
    int executeUpdate(String sql) throws SQLException;
    
    void close() throws SQLException;
    
    int getMaxFieldSize() throws SQLException;
    
    void setMaxFieldSize(int max) throws SQLException;
    
    int getMaxRows() throws SQLException;
    
    void setMaxRows(int max) throws SQLException;
    
    void setEscapeProcessing(boolean enable) throws SQLException;
    
    int getQueryTimeout() throws SQLException;
    
    void setQueryTimeout(int seconds) throws SQLException;
    
    void cancel() throws SQLException;
    
    SQLWarning getWarnings() throws SQLException;
    
    void clearWarnings() throws SQLException;
    
    void setCursorName(String name) throws SQLException;
    
    boolean execute(String sql) throws SQLException;
    
    ResultSet getResultSet() throws SQLException;
    
    int getUpdateCount() throws SQLException;
    
    boolean getMoreResults() throws SQLException;
    
    void setFetchDirection(int direction) throws SQLException;
    
    int getFetchDirection() throws SQLException;
    
    void setFetchSize(int rows) throws SQLException;
    
    int getFetchSize() throws SQLException;
    
    int getResultSetConcurrency() throws SQLException;
    
    int getResultSetType() throws SQLException;
    
    void addBatch(String sql) throws SQLException;
    
    void clearBatch() throws SQLException;
    
    int[] executeBatch() throws SQLException;
    
    Connection getConnection() throws SQLException;
    int CLOSE_CURRENT_RESULT = 1;
    int KEEP_CURRENT_RESULT = 2;
    int CLOSE_ALL_RESULTS = 3;
    int SUCCESS_NO_INFO = -2;
    int EXECUTE_FAILED = -3;
    int RETURN_GENERATED_KEYS = 1;
    int NO_GENERATED_KEYS = 2;
    
    boolean getMoreResults(int current) throws SQLException;
    
    ResultSet getGeneratedKeys() throws SQLException;
    
    int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException;
    
    int executeUpdate(String sql, int[] columnIndexes) throws SQLException;
    
    int executeUpdate(String sql, String[] columnNames) throws SQLException;
    
    boolean execute(String sql, int autoGeneratedKeys) throws SQLException;
    
    boolean execute(String sql, int[] columnIndexes) throws SQLException;
    
    boolean execute(String sql, String[] columnNames) throws SQLException;
    
    int getResultSetHoldability() throws SQLException;
}
