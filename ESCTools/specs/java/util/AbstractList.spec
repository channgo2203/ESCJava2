// @(#)$Id$

// Copyright (C) 2001 Iowa State University

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


package java.util;

/** JML's specification of java.util.AbstractList.
 * @version $Revision$
 * @author Gary T. Leavens
 */
public abstract class AbstractList extends AbstractCollection implements List {

    /*@ pure @*/ protected AbstractList();

    /*@ also
      @   protected model_program {
      @      add(size(), o); 
      @   }
      @*/
    public boolean add(Object o);

    // specification inherited from Collection
    abstract public /*@ pure @*/ Object get(int index);

    /*@ also
      @   protected exceptional_behavior
      @     requires \typeof(this) == \type(AbstractList);
      @     assignable \nothing;
      @     signals (Exception e) e instanceof UnsupportedOperationException;
      @*/
    public Object set(int index, Object element) throws UnsupportedOperationException;

    /*@ also
      @   protected exceptional_behavior
      @     requires \typeof(this) == \type(AbstractList);
      @     assignable \nothing;
      @     signals (Exception e) e instanceof UnsupportedOperationException;
      @*/
    public void add(int index, Object element) throws UnsupportedOperationException;

    /*@ also
      @   protected exceptional_behavior
      @     requires \typeof(this) == \type(AbstractList);
      @     assignable \nothing;
      @     signals (Exception e) e instanceof UnsupportedOperationException;
      @*/
    public Object remove(int index) throws UnsupportedOperationException;

    // Search Operations

    // specification inherited from List
    public /*@ pure @*/ int indexOf(Object o);

    // specification inherited from List
    public /*@ pure @*/ int lastIndexOf(Object o);

    // Bulk Operations

    /*@ also
      @   protected model_program {
      @      removeRange(0, size()); 
      @   }
      @*/
    public void clear();

    // specification inherited from List
    public boolean addAll(int index, Collection c);

    // Iterators

    // specification inherited from List
    public /*@ pure @*/ Iterator iterator();

    /*@ also
      @   protected model_program {
      @      return listIterator(0);
      @   }
      @*/
    public /*@ pure @*/ ListIterator listIterator();

    // specification inherited from List
    public /*@ pure @*/ ListIterator listIterator(final int index);

    // specification inherited from List
    public /*@ pure @*/ List subList(int fromIndex, int toIndex);

    // Comparison and hashing

    // specification inherited from Object
    public /*@ pure @*/ boolean equals(Object o);

    // specification inherited from Object
    public /*@ pure @*/ int hashCode();

    /*@ protected normal_behavior
      @  requires 0 <= fromIndex && fromIndex <= toIndex
      @              && toIndex <= size();
      @  {|
      @     requires fromIndex == toIndex;
      @     assignable \nothing;
      @   also
      @     requires fromIndex < toIndex && toIndex < size();
      @     assignable objectState;
      @     //FIXME detailed postcondition missing
      @  |}
      @*/
    protected void removeRange(int fromIndex, int toIndex);

    protected transient int modCount;
    //@                       in objectState;

    //@ protected initially modCount == 0;
}

class SubList extends AbstractList {

    SubList(AbstractList list, int fromIndex, int toIndex);

    public Object set(int index, Object element);

    public Object get(int index);

    public int size();

    public void add(int index, Object element);

    public Object remove(int index);

    protected void removeRange(int fromIndex, int toIndex);

    public boolean addAll(Collection c);

    public boolean addAll(int index, Collection c);

    public Iterator iterator();

    public ListIterator listIterator(int index);

    public List subList(int fromIndex, int toIndex);
}



class RandomAccessSubList extends SubList implements RandomAccess {

    RandomAccessSubList(AbstractList list, int fromIndex, int toIndex);

    public List subList(int fromIndex, int toIndex);
}