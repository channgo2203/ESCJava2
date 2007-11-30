package mobius.directVCGen.formula.annotation;

import java.util.List;

import escjava.sortedProver.Lifter.QuantVariableRef;
import escjava.sortedProver.Lifter.Term;

/**
 * The abstact class that is the mother of all annotations.
 * Every annotations should subclass this class.
 * 
 * @author H. Lehner and J. Charles (julien.charles@inria.fr)
 */
public abstract class AAnnotation {
  /** an undefined id. */
  public static final int undef = 0;
  /** the id of an assert class. */
  public static final int annotAssert = undef + 1;
  /** the id of an assume class. */
  public static final int annotAssume = annotAssert + 1;
  /** the id of a cut class. */
  public static final int annotCut = annotAssume + 1;
  /** the id of a set class. */
  public static final int annotSet = annotCut + 1;

  /** FOL-Term that represents the annotation at that point. */
  public final Term fFormula;
  /** the name of the annotation that is used for the generation. */
  public final String fName;
  /** the arguments of the annotation. */
  public final List<QuantVariableRef> fArgs;
  

  /**
   * Default constructor.
   */
  protected AAnnotation() {
    this(null);
  }

  /**
   * Construct the annotation around the given term.
   * @param term the term which is the formula contained in 
   * the annotation
   * @deprecated use {@link #AAnnotation(String, List, Term)} instead
   */
  public AAnnotation(final Term term) {
    this (null, null, term);
  }
  
  /**
   * Construct the annotation around the given term.
   * @param name the name of the annotation 
   * @param args the arguments of the annotation (variables name...)
   * @param term the term which represents the annotation
   */
  public AAnnotation(final String name, 
                     final List<QuantVariableRef> args,
                     final Term term) {
    fFormula = term;
    if (args == null) {
      throw new NullPointerException();
    }
    fArgs = args;
    fName = name;
  }
  /**
   * Return the ID of the class in order to do a switch.
   * @return an id precising which class the current object is from
   */
  public abstract int getID();

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "" + fFormula;
  }

}
