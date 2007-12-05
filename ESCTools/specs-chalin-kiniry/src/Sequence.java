// $Id$

/**
 * The abstract model class for Sequences.
 *
 * All of our model classes have referential equality, are pure,
 * functional, and executable.  All have been extensively tested with
 * JML/Junit and have been ESCed with ESC/Java2.0a8.
 *
 * @author Patrice Chalin
 * @author Joe Kiniry
 * @author Radu grigore
 */

public abstract class Sequence
{
  /*
   * This invariant ensures that there is at most one Sequence with a
   * given head and tail.
   */
  /*@ public static invariant
    @   (\forall Sequence p, q;; p == q <==> 
    @           p.isEmpty() && q.isEmpty() ||
    @		(!p.isEmpty() && !q.isEmpty() && 
    @            p.head() == q.head() && 
    @		 p.tail() == q.tail())
    @	);
    @*/

  /*@ public model \bigint length;
    @              in objectState;
    @*/

  //@ public  invariant 0 <= length;
  //@ public  invariant_redundantly isEmpty() <==> 0 == length;
  //@ protected represents length <- elts.length();

  //  @review kiniry/chalin - think about whether we want to keep this, as
  //  it is in the current model classes, and if so, whether to use a ghost
  //  or a model field.
  //  protected Class elementType = Object.class;

  // TBC
  //@ protected invariant (* Each Sequence object is uniquely determined by its elts.*);
  //  the only subclasses of this class are EmptySequence and NonEmptySequence

  //------------------------------------------------------------------------------
  // Cache

  // TODO : rename chain to cache

  /**
   * The chain of all Sequences.
   */
  /*@ private static invariant
    @   (* chain contains all Sequences and only Sequences, that is ... *);
    @ private static invariant
    @   (\forall Object o;; 
    @     Pair.isMember(chain, o) <==> (o instanceof Sequence));
    @ private static invariant isSequenceChain(chain);
    @*/
  protected static /* null */ Pair chain = null;

  /**
   * The elements in this Sequence.
   *
   * @todo chalin/kiniry - consider using Pair instead of Cons
   */
  //@ private invariant Pair.isChain(elts);
  protected /* null */ Pair elts;

  //------------------------------------------------------------------------------
  // Queries

  /*@ public normal_behavior
    @   ensures \result <==> length == 0;
    @ also
    @ protected normal_behavior
    @   ensures \result <==> elts == null;
    @*/
  public /*@ pure @*/ boolean isEmpty() {
    return elts == null;
  }

  /*@ public normal_behavior
    @   ensures \result == length;
    @ public pure model \bigint length() {
    @   return elts.length();
    @ }
    @*/

  //  True iff 'o' is in this list.
  //  public pure boolean has(Object o);

  //------------------------------------------------------------------------------
  // Selectors

  /*@ public normal_behavior
    @  requires !isEmpty();
    @  modifies \nothing;
    @ also
    @ public exceptional_behavior
    @  requires isEmpty();
    @  modifies \nothing;
    @  signals_only RuntimeException;
    @*/
  public abstract /* observationally_pure */ Object head();

  /*@ public normal_behavior
    @  requires !isEmpty();
    @  modifies \nothing;
    @ also
    @ public exceptional_behavior
    @  requires isEmpty();
    @  modifies \nothing;
    @  signals_only RuntimeException;
    @*/
  public abstract /*@ non_null @*/ /* observationally_pure */ Sequence tail();

  //  Returns the item at index 'i'.
  //@ public pure model Object itemAt(\bigint i);

  //  Returns the last item in this sequence.
  //@ public pure model Object last();

  //------------------------------------------------------------------------------
  // Constructors and factory methods

  /*@ protected normal_behavior
    @   modifies \nothing;
    @*/
  protected Sequence() {
    ;
  }

  /*@ protected normal_behavior
    @   requires Pair.isChain(elts);
    @   modifies elts;
    @*/
  protected Sequence(Pair e) {
    elts = e;
  }

//   /*@ public normal_behavior
//     @   ensures \result.isEmpty();
//     @*/
//   public static /*@ pure non_null @*/ Sequence empty() {
//     return EmptySequence.empty();
//   }

  /*@ public normal_behavior
    @   ensures \result.head() == o;
    @   ensures \result.tail() == this;
    @   ensures !\result.isEmpty();
    @*/
  public /*@ pure non_null @*/ Sequence append(Object o) {
    return getCachedAndOrMake(Pair.make(o, elts));
  }

  /*@ also
    @ public normal_behavior
    @   ensures \result == (this == o);
    @*/
  public /*@ pure @*/ boolean equals(Object o) {
    return this == o;
  }

  //- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
  // Helpers methods

  /*@ protected normal_behavior
    @  requires Pair.isChain(elts);
    @  requires getCached(chain, elts) != null;
    @  modifies \nothing;
    @  ensures  \result == getCached(chain, elts);
    @ also
    @ protected normal_behavior
    @  requires Pair.isChain(elts);
    @  requires getCached(chain, elts) == null;
    @  modifies chain;
    @  ensures  \fresh(\result);
    @  ensures  \result.elts == elts;
    @*/
  protected static /*@ non_null @*/ /* non_pure */ Sequence
    getCachedAndOrMake(/* null */ Pair elts) 
  {
    Sequence result = getCached(chain, elts);
    if (result == null) {
      //@ assert elts != null;
      result = new NonEmptySequence(elts);
      cache(result);
    }
    return(result);
  }

  //   requires (\exists Sequence s;; s.elts == elts);
  //   ensures  (\exists Sequence s; s.elts == elts; \result == s);
  //   ensures_redundantly \result != null;
  // also
  // protected normal_behavior
  //   requires !(\exists Sequence s;; s.elts == elts);
  //   requires getCached(chain, elts) == null;
  //   ensures  \result == null;
  // also
  // protected normal_behavior

  /*@ public normal_behavior
    @  ensures \result == 
    @    (c == null || 
    @     (c.first() instanceof Sequence &&
    @      (c.second() == null || 
    @       c.second() instanceof Pair && 
    @       isSequenceChain((Pair)c.second()))));
    @  ensures_redundantly \result ==> Pair.isChain(c);
    @*/
  public static /*@ pure @*/ boolean isSequenceChain(Pair c) {
    return c == null || 
      (c.first() instanceof Sequence &&
       (c.second() == null || 
        c.second() instanceof Pair && 
        isSequenceChain((Pair)c.second())));
  }

  /*@ protected normal_behavior
    @  requires isSequenceChain(chain);
    @  requires Pair.isChain(elts);
    @  ensures
    @    \result == (chain == null
    @		    ? null
    @		    : (((Sequence)chain.first()).elts == elts
    @			? (Sequence)chain.first()
    @			: getCached((Pair)(chain.second()), elts)));
    @*/
  protected static /*@ pure @*/ /* null */ Sequence 
    getCached(/* null */ Pair chain,
              /* null */ Pair elts)
  {
    if (chain == null)
      return null;

    Sequence k = (Sequence)chain.first();
    return k.elts == elts
      ? k
      : getCached((Pair)(chain.second()), elts);
  }

  /*@ protected normal_behavior
    @   modifies chain;
    @   ensures getCached(chain, s.elts) == s;
    @ also
    @ private normal_behavior
    @   modifies chain;
    @   ensures chain != null;
    @   ensures chain.first() == s;
    @   ensures chain.second() == \old(chain);
    @*/
  protected static void cache(/*@ non_null @*/ Sequence s) {
      chain = Pair.make(s, chain);
  }
}