package com.sun.jmx.mbeanserver;

import java.io.PrintWriter;
import java.io.StringWriter;
import javax.management.*;
import com.sun.jmx.trace.Trace;

class DynamicMetaDataImpl extends BaseMetaDataImpl {
    private static final String dbgTag = "DynamicMetaDataImpl";
    
    public DynamicMetaDataImpl() {
        
    }
    
    public void testCompliance(Class c) throws NotCompliantMBeanException {
        if (DynamicMBean.class.isAssignableFrom(c)) return;
        throw new NotCompliantMBeanException("Only DynamicMBeans are supported by this implementation");
    }
    
    public MBeanInfo getMBeanInfo(Object moi) throws IntrospectionException {
        try {
            return (MBeanInfo)((javax.management.DynamicMBean)(DynamicMBean)moi).getMBeanInfo();
        } catch (RuntimeMBeanException r) {
            throw r;
        } catch (RuntimeErrorException r) {
            throw r;
        } catch (RuntimeException r) {
            debugX("getMBeanInfo", r);
            throw new RuntimeMBeanException((RuntimeException)r, "Runtime Exception thrown by getMBeanInfo method of Dynamic MBean");
        } catch (Error e) {
            debugX("getMBeanInfo", e);
            throw new RuntimeErrorException((Error)e, "Error thrown by getMBeanInfo method of Dynamic MBean");
        }
    }
    
    public Object getAttribute(Object instance, String attribute) throws MBeanException, AttributeNotFoundException, ReflectionException {
        if (attribute == null) {
            final RuntimeException r = new IllegalArgumentException("Attribute name cannot be null");
            throw new RuntimeOperationsException(r, "Exception occured trying to invoke the getter on the MBean");
        }
        try {
            return ((javax.management.DynamicMBean)(DynamicMBean)instance).getAttribute(attribute);
        } catch (RuntimeOperationsException r) {
            throw r;
        } catch (RuntimeErrorException r) {
            throw r;
        } catch (RuntimeException e) {
            debugX("getAttribute", e);
            throw new RuntimeMBeanException(e, "RuntimeException" + " thrown by the getAttribute method of the DynamicMBean" + " for the attribute " + attribute);
        } catch (Error e) {
            debugX("getAttribute", e);
            throw new RuntimeErrorException((Error)e, "Error" + " thrown by the getAttribute method of the DynamicMBean " + " for the attribute " + attribute);
        }
    }
    
    public AttributeList getAttributes(Object instance, String[] attributes) throws ReflectionException {
        if (attributes == null) {
            throw new RuntimeOperationsException(new IllegalArgumentException("Attributes cannot be null"), "Exception occured trying to invoke the getter on the MBean");
        }
        try {
            return ((javax.management.DynamicMBean)(DynamicMBean)instance).getAttributes(attributes);
        } catch (RuntimeOperationsException r) {
            throw r;
        } catch (RuntimeErrorException r) {
            throw r;
        } catch (RuntimeException e) {
            debugX("getAttributes", e);
            throw new RuntimeOperationsException(e, "RuntimeException thrown by the getAttributes method of the DynamicMBean");
        } catch (Error e) {
            debugX("getAttributes", e);
            throw new RuntimeErrorException((Error)e, "Error thrown by the getAttributes method of the DynamicMBean");
        }
    }
    
    public AttributeList setAttributes(Object instance, AttributeList attributes) throws ReflectionException {
        try {
            return ((javax.management.DynamicMBean)(DynamicMBean)instance).setAttributes(attributes);
        } catch (RuntimeOperationsException r) {
            throw r;
        } catch (RuntimeErrorException r) {
            throw r;
        } catch (RuntimeException e) {
            debugX("setAttributes", e);
            throw new RuntimeOperationsException(e, "RuntimeException thrown by the setAttributes method of the Dynamic MBean");
        } catch (Error e) {
            debugX("setAttributes", e);
            throw new RuntimeErrorException((Error)e, "Error thrown by the setAttributes method of the Dynamic MBean");
        }
    }
    
    public Object setAttribute(Object instance, Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        if (attribute == null) {
            final RuntimeException r = new IllegalArgumentException("Attribute name cannot be null");
            throw new RuntimeOperationsException(r, "Exception occured trying to invoke the setter on the MBean");
        }
        try {
            ((javax.management.DynamicMBean)(DynamicMBean)instance).setAttribute(attribute);
            return attribute.getValue();
        } catch (RuntimeOperationsException r) {
            throw r;
        } catch (RuntimeErrorException r) {
            throw r;
        } catch (RuntimeException e) {
            debugX("setAttribute", e);
            throw new RuntimeMBeanException(e, "RuntimeException thrown by the setAttribute " + attribute + "method of the Dynamic MBean");
        } catch (Error e) {
            debugX("setAttribute", e);
            throw new RuntimeErrorException((Error)e, "Error thrown by the setAttribute " + attribute + "method of the Dynamic MBean");
        }
    }
    
    public Object invoke(Object instance, String operationName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        if (operationName == null) {
            final RuntimeException r = new IllegalArgumentException("Operation name  cannot be null");
            throw new RuntimeOperationsException(r, "Exception occured trying to invoke the operation on the MBean");
        }
        try {
            return (((javax.management.DynamicMBean)(DynamicMBean)instance).invoke(operationName, params, signature));
        } catch (ReflectionException e) {
            debugX("invoke", e);
            throw e;
        } catch (MBeanException e) {
            debugX("invoke", e);
            throw e;
        } catch (RuntimeOperationsException r) {
            throw r;
        } catch (RuntimeErrorException r) {
            throw r;
        } catch (RuntimeException e) {
            debugX("invoke", e);
            throw new RuntimeMBeanException(e, "RuntimeException thrown by the invoke method of the Dynamic MBean");
        } catch (Error e) {
            debugX("invoke", e);
            throw new RuntimeErrorException((Error)e, "Error thrown by the invoke method of the Dynamic MBean");
        }
    }
    
    private static boolean isTraceOn() {
        return Trace.isSelected(Trace.LEVEL_TRACE, Trace.INFO_MBEANSERVER);
    }
    
    private static void trace(String clz, String func, String info) {
        Trace.send(Trace.LEVEL_TRACE, Trace.INFO_MBEANSERVER, clz, func, info);
    }
    
    private static void trace(String func, String info) {
        trace(dbgTag, func, info);
    }
    
    private static boolean isDebugOn() {
        return Trace.isSelected(Trace.LEVEL_DEBUG, Trace.INFO_MBEANSERVER);
    }
    
    private static void debug(String clz, String func, String info) {
        Trace.send(Trace.LEVEL_DEBUG, Trace.INFO_MBEANSERVER, clz, func, info);
    }
    
    private static void debug(String func, String info) {
        debug(dbgTag, func, info);
    }
    
    private static void debugX(String func, Throwable e) {
        if (isDebugOn()) {
            final StringWriter s = new StringWriter();
            e.printStackTrace(new PrintWriter(s));
            final String stack = s.toString();
            debug(dbgTag, func, "Exception caught in " + func + "(): " + e);
            debug(dbgTag, func, stack);
        }
    }
}
