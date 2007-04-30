package mobius.directVCGen.formula;

import escjava.sortedProver.Lifter.QuantVariable;
import escjava.sortedProver.Lifter.QuantVariableRef;
import escjava.sortedProver.Lifter.Term;
import escjava.sortedProver.NodeBuilder.Sort;

public class Heap {
	public static Sort sort = Formula.lf.sortMap;
	public static QuantVariableRef varPre = Expression.rvar("preHeap", sort);
	public static QuantVariableRef var = Expression.rvar("heap", sort);
	
	public static Term store(QuantVariableRef heap, QuantVariable var, Term val) {
		return Formula.lf.mkFnTerm(Formula.lf.symStore, new Term[] {heap, Expression.refFromVar(var), sortToValue(val)});
	}
	public static Term store(QuantVariableRef heap, Term obj, QuantVariable var, Term val) {
		return Formula.lf.mkFnTerm(Formula.lf.symMStore, new Term[] {heap, obj, Expression.refFromVar(var), sortToValue(val)});
	}
	public static Term select(QuantVariableRef heap, QuantVariable var) {
		Term select = Formula.lf.mkFnTerm(Formula.lf.symSelect, 
				new Term[] {heap, Expression.refFromVar(var)});
		return valueToSort(select, var.type);
	}
	public static Term select(QuantVariableRef heap, Term obj, QuantVariable var) {
		Term select = Formula.lf.mkFnTerm(Formula.lf.symMSelect, 
				new Term[] {heap, obj, Expression.refFromVar(var)});
		return valueToSort(select, var.type);
	}
	private static Term valueToSort(Term t, Sort type) {
		if(type == Formula.lf.sortBool) {
			return Formula.lf.mkFnTerm(Formula.lf.symValueToBool,  new Term [] {t});
		}
		else if(type == Formula.lf.sortRef) {
			return Formula.lf.mkFnTerm(Formula.lf.symValueToRef,  new Term [] {t});
		}
		else if(type == Formula.lf.sortInt) {
			return Formula.lf.mkFnTerm(Formula.lf.symValueToInt,  new Term [] {t});
		}
		else if(type == Formula.lf.sortReal) {
			return Formula.lf.mkFnTerm(Formula.lf.symValueToReal,  new Term [] {t});
		}
		else {
			throw new IllegalArgumentException("Bad type " +
					"found: " + type);
		}
	}

	public static Term sortToValue(Term t) {
		if(t.getSort() == Formula.lf.sortBool) {
			return Formula.lf.mkFnTerm(Formula.lf.symBoolToValue,  new Term [] {t});
		}
		else if(t.getSort() == Formula.lf.sortRef) {
			return Formula.lf.mkFnTerm(Formula.lf.symRefToValue,  new Term [] {t});
		}
		else if(t.getSort() == Formula.lf.sortInt) {
			return Formula.lf.mkFnTerm(Formula.lf.symIntToValue,  new Term [] {t});
		}
		else if(t.getSort() == Formula.lf.sortReal) {
			return Formula.lf.mkFnTerm(Formula.lf.symRealToValue,  new Term [] {t});
		}
		else {
			throw new IllegalArgumentException("Bad type " +
					"found: " + t.getSort());
		}
	}
	
	private static int heapc =0;
	public static QuantVariableRef newVar() {
		return Expression.rvar("heap" + (heapc++), Heap.sort);
	}
	public static Term newObject(Term oldheap, Term type, Term heap,  QuantVariableRef e) {
		if(oldheap == null)
			throw new NullPointerException();
		return Formula.lf.mkFnTerm(Formula.lf.symNewObj, new Term[] {oldheap, type, heap, e});
	}
	
}
