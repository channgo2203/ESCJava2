package java.lang;

public class Exception extends Throwable {
    static final long serialVersionUID = -3387516993124229948L;
    
    public Exception() {
        
    }
    
    public Exception(String message) {
        super(message);
    }
    
    public Exception(String message, Throwable cause) {
        super(message, cause);
    }
    
    public Exception(Throwable cause) {
        super(cause);
    }
}
