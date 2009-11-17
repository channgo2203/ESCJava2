package java.awt;

import sun.awt.DebugHelper;

abstract class AttributeValue {
    private final int value;
    private final String[] names;
    private static final DebugHelper dbg = DebugHelper.create(AttributeValue.class);
    
    protected AttributeValue(int value, String[] names) {
        
        if (dbg.on) {
            dbg.assertion(value >= 0 && names != null && value < names.length);
        }
        this.value = value;
        this.names = names;
    }
    
    public int hashCode() {
        return value;
    }
    
    public String toString() {
        return names[value];
    }
}
