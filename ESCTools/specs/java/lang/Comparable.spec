// @(#)$Id$

// Copyright (C) 2001, 2002 Iowa State University

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

/** JML's specification of java.lang.Comparable.
 * These are objects with a total ordering that is an equivalence relation.
 * @version $Revision$
 * @author Gary T. Leavens
 */
public interface Comparable {

    /*@   public normal_behavior
      @     requires i < 0;
      @     ensures \result == -1;
      @ also
      @   public normal_behavior
      @     requires i == 0;
      @     ensures \result == 0;
      @ also
      @   public normal_behavior
      @     requires i >= 0;
      @     ensures \result == +1;
      @ implies_that
      @   public model_program {
      @     return ((i < 0) ? -1 : ((i == 0) ? 0 : 1));
      @   }
      @ model pure int sgn(int i);
      @*/

    /*@ public model_program {
      @   boolean x_y_def = true;
      @   try {
      @      x.compareTo(y);
      @   } catch (ClassCastException e) {
      @      x_y_def = false;
      @   }
      @   return x_y_def;
      @ }
      @ also public normal_behavior
      @    ensures \result == definedComparison(y,x);
      @ public model pure boolean
      @ definedComparison(non_null Comparable x, non_null Comparable y);
      @*/

    /*@  public behavior
      @    requires o != null;
      @    ensures (* \result is negative if this is "less than" o *);
      @    ensures (* \result is 0 if this is "equal to" o *);
      @    ensures (* \result is positive if this is "greater than" o *);
      @    signals (ClassCastException)
      @         (* the class of o prohibits it from being compared to this *);
      @ also public exceptional_behavior
      @    requires o == null;
      @    signals (Exception e) e instanceof NullPointerException;
      @ also
      @   public behavior
      @    requires o != null && o instanceof Comparable;
      @    ensures definedComparison((Comparable)o, this);
      @    ensures o == this ==> \result == 0; // reflexive
      @    ensures sgn(\result) == - sgn(((Comparable)o).compareTo(this)); // antisymmetric
      @    signals(ClassCastException)
      @            !definedComparison((Comparable)o, this);
      @*/
    /*@ pure @*/ int compareTo(/*@ non_null @*/ Object o);

    // compareTo is reflexive
    /*+@ public instance invariant
      @   (\forall Comparable x; x != null; x.compareTo(x) == 0);
      @*/

    // compareTo is antisymmetric
    /*+@ public instance invariant
      @   (\forall Comparable x, y; x != null && y != null
      @                             && definedComparison(x, y)
      @                             && definedComparison(y, x);
      @                 sgn(x.compareTo(y)) == -sgn(y.compareTo(x)) );
      @*/

    // compareTo is transitive
    /*+@ public instance invariant
      @     (\forall int n; n == -1 || n == 1;
      @      (\forall Comparable x, y, z;
      @              x != null && y != null && z != null
      @               && definedComparison(x, y) && definedComparison(y, z)
      @               && definedComparison(x, z);
      @              sgn(x.compareTo(y)) == n && sgn(y.compareTo(z)) == n
      @                 ==> sgn(x.compareTo(z)) == n));
      @ public instance invariant
      @     (\forall int n; n == -1 || n == 1;
      @      (\forall Comparable x, y, z;
      @             x != null && y != null && z != null
      @              && definedComparison(x, y) && definedComparison(y, z)
      @              && definedComparison(x, z);
      @             (sgn(x.compareTo(y)) == 0 && sgn(y.compareTo(z)) == n
      @               || sgn(x.compareTo(y)) == n && sgn(y.compareTo(z)) == 0)
      @             ==> sgn(x.compareTo(z)) == n));
      @*/

    // compareTo returning 0 means the other argument
    // is in the same equivalence class
    /*+@ public instance invariant
      @    (\forall Comparable x, y, z;
      @             x != null && y != null && z != null
      @              && definedComparison(x, y) && definedComparison(x, z)
      @              && definedComparison(y, z);
      @             sgn(x.compareTo(y)) == 0
      @             ==> sgn(x.compareTo(z)) == sgn(y.compareTo(z)));
      @*/

}
