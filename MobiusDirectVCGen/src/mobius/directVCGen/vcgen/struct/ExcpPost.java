/**
 * 
 */
package mobius.directVCGen.vcgen.struct;

import javafe.ast.Type;

/**
 * A structure to represent the postcondition associated with a given exception.
 * @author J. Charles and B. Grégoire
 */
public class ExcpPost {
	/** the type of the exception to which correspond the postcondition */
	public final Type excp;
	/** the post condition that is verified if the specified exception is triggered*/
	public final Post post;
	
	/**
	 * Construct an exceptional postcondition from an exception type
	 * and a given postcondition
	 * @param excp the type of the exception
	 * @param p the postcondition associated with it
	 */
	public ExcpPost (Type excp, Post p) {
		this.excp = excp;
		this.post = p;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "( " + excp + ", " + post + ")";
	}

}