// @(#)$Id$

// Copyright (C) 1998, 1999 Iowa State University

// This file is part of JML

// JML is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2, or (at your option)
// any later version.

// JML is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with JML; see the file COPYING.  If not, write to
// the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.

package java.lang;

import java.io.*;
import java.util.Properties;

/** JML's specification of java.lang.System.
 *
 * @version $Revision$
 * @author Gary T. Leavens
 */
public final class System {

    private /*@ pure @*/ System();

    public final static /*@ non_null @*/ InputStream in;
        /*@ public initially in != null; @*/ 
    public final static /*@ non_null @*/ PrintStream out;
        /*@ public initially out != null; @*/ 
    public final static /*@ non_null @*/ PrintStream err;
        /*@ public initially err != null; @*/ 

    //@ assignable in;  // strangely enough, in is like a read-only var.
    //@ ensures in == i;
    public static void setIn(InputStream i);

    //@ assignable out;
    //@ ensures out == o;
    public static void setOut(PrintStream o);

    //@ assignable err;
    //@ ensures err == e;
    public static void setErr(PrintStream e);

    //@ public model static SecurityManager SystemSecurityManager;
    //@ public static represents SystemSecurityManager <- getSecurityManager();

    /*@  public normal_behavior
      @    requires s == null;
      @    assignable \nothing;
      @    ensures \not_modified(SystemSecurityManager);
      @ also
      @  public behavior
      @    requires s != null;
      @    assignable SystemSecurityManager;
      @    ensures  SystemSecurityManager == s;
      @    signals (SecurityException) (* if the change is not permitted *);
      @*/
    public static void setSecurityManager(SecurityManager s);

    //@ ensures \result == SystemSecurityManager;
    public /*@ pure @*/ static SecurityManager getSecurityManager();

    //@ assignable \nothing;
    public /*@ pure @*/ static native long currentTimeMillis();

    /*@  public exceptional_behavior
      @    requires src == null || dest == null;
      @    assignable \nothing;
      @    signals (NullPointerException);
      @ also
      @  public exceptional_behavior
      @    requires !(src == null || dest == null);
      @    requires !(src.getClass().isArray()) || !(dest.getClass().isArray());
      @    assignable \nothing;
      @    signals (ArrayStoreException);
      @ also
      @  public exceptional_behavior
      @    requires !(src == null || dest == null);
      @    requires !(!(src.getClass().isArray()) || !(dest.getClass().isArray()));
      @    requires (srcPos < 0 || destPos < 0 || length < 0
      @                  || srcPos + length > ((Object[])src).length
      @                  || destPos + length > ((Object[])dest).length);
      @    assignable \nothing;
      @    signals (ArrayIndexOutOfBoundsException);
      @ also
      @  public normal_behavior
      @    requires !(src == null || dest == null);
      @    requires !(!(src.getClass().isArray()) || !(dest.getClass().isArray()));
      @    requires !(    srcPos < 0 || destPos < 0 || length < 0 );
	   {| requires \elemtype(\typeof(src)) <: \type(Object) &&
			\elemtype(\typeof(dest)) <: \type(Object) &&
      @                 srcPos + length <= ((Object[])src).length
      @                && destPos + length <= ((Object[])dest).length;
      @   {|
      @      old Object [] sa = (Object[]) src;
      @      old Object [] da = (Object[]) dest;
      @      assignable da[destPos .. destPos + length - 1];
      @      ensures (\forall int i; 0 <= i && i < length;
      @                        \old(sa[(int)(srcPos+i)]) == da[(int)(destPos+i)]);
      @   |}
	   also
		requires \elemtype(\typeof(src)) == \type(int) &&
			 \elemtype(\typeof(dest)) == \type(int) &&
      @                 srcPos + length <= ((int[])src).length
      @                && destPos + length <= ((int[])dest).length;
      @   {|
      @      old int [] sa = (int[]) src;
      @      old int [] da = (int[]) dest;
      @      assignable da[destPos .. destPos + length - 1];
      @      ensures (\forall int i; 0 <= i && i < length;
      @                        sa[(int)(srcPos+i)] == da[(int)(destPos+i)]);
      @   |}
	   |}
      @*/
    //@ implies_that
    //@   requires length >= 0 && 0 <= srcPos && 0 <= destPos;
    public static native void arraycopy(Object src,
                                        int srcPos,
                                        Object dest,
                                        int destPos,
                                        int length);

    public /*@ pure @*/ static native int identityHashCode(Object x);

    //@ public model static Properties SystemProperties;
    //@  static represents SystemProperties <- getProperties();

    /*@ public behavior
      @    ensures \result != null && \result.equals(SystemProperties);
      @    signals (SecurityException) (* if access is not permitted *);
      @*/
    public /*@ pure @*/ static /*@ non_null @*/ Properties getProperties();

    /*@ public behavior
      @    assignable SystemProperties;
      @    signals (SecurityException) (* if access is not permitted *);
      @*/
    public static void setProperties(/*@ non_null @*/ Properties props);


    //@    signals (SecurityException) (* if access is not permitted *);
    public /*@ pure @*/ static
        String getProperty(/*@ non_null @*/ String key);

    //@     ensures def != null ==> \result != null;
    //@    signals (SecurityException) (* if access is not permitted *);
    public /*@ pure @*/ static
        String getProperty(/*@ non_null @*/ String key, String def);    

    /*@ public behavior
      @    assignable SystemProperties;
      @    signals (SecurityException) (* if access is not permitted *);
      @*/
    public static String setProperty(/*@ non_null @*/ String key,
                                     /*@ non_null @*/ String value);

    /** @deprecated use java.lang.System.getProperty. */
    public static /*@ pure @*/ String getenv(String name);

    /*@ public behavior
      @    diverges true;
      @    assignable \nothing;
      @    ensures false;
      @    signals (SecurityException) (* if exiting is not permitted *);
      @*/
    //@ implies_that
    //@    ensures false;
    public static void exit(int status);

    public static void gc();

    public static void runFinalization();

    /** @deprecated this is unsafe. */
    public static void runFinalizersOnExit(boolean value);

    public static void load(/*@ non_null @*/ String filename);

    public static void loadLibrary(/*@ non_null @*/ String libname);

    public /*@ pure @*/ static native String mapLibraryName(String libname);

    static Class getCallerClass();
}
