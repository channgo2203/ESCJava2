package mobius.directVCGen.formula;

import escjava.sortedProver.Lifter.Term;
import javafe.ast.Stmt.Annotation;

public class Assert implements Annotation {

	/**
	 * FOL-Term that should be asserted in the VCGen at this point
	 */
	public Term formula;
	
}