package mobius.directVCGen.vcgen.expression;

import javafe.ast.ArrayRefExpr;
import javafe.ast.BinaryExpr;
import javafe.ast.Expr;
import javafe.ast.FieldAccess;
import javafe.ast.ObjectDesignator;
import javafe.ast.VariableAccess;
import mobius.directVCGen.formula.Bool;
import mobius.directVCGen.formula.Expression;
import mobius.directVCGen.formula.Heap;
import mobius.directVCGen.formula.Logic;
import mobius.directVCGen.formula.Lookup;
import mobius.directVCGen.formula.Num;
import mobius.directVCGen.formula.Ref;
import mobius.directVCGen.formula.Type;
import mobius.directVCGen.vcgen.stmt.StmtVCGen;
import mobius.directVCGen.vcgen.struct.Post;
import mobius.directVCGen.vcgen.struct.VCEntry;
import escjava.ast.TagConstants;
import escjava.sortedProver.Lifter.QuantVariableRef;
import escjava.sortedProver.Lifter.Term;

public class BinaryExpressionVCGen extends ABasicExpressionVCGEn{

	public BinaryExpressionVCGen(ExpressionVisitor vis) {
		super(vis);
	}
	
	
	public Post stdBinExpression(int tag, Expr left, Expr right, VCEntry post) {
		
		QuantVariableRef rvar = Expression.rvar(Type.getSort(right));
		QuantVariableRef lvar = Expression.rvar(Type.getSort(left));
		Term formula;
		switch (tag) {
			case TagConstants.EQ:
				formula = Bool.equals(lvar, rvar);
				break;
			case TagConstants.OR:
				formula = Bool.or(lvar, rvar);
				break;
			case TagConstants.AND:
				formula = Bool.and(lvar, rvar);
				break;
			case TagConstants.NE:
				formula = Bool.notEquals(lvar, rvar);
				break;	
			case TagConstants.GE:
				formula = Bool.ge(lvar, rvar);
				break;
			case TagConstants.GT:
				formula = Bool.gt(lvar, rvar);
				break;
			case TagConstants.LE:
				formula = Bool.le(lvar, rvar);
				break;
			case TagConstants.LT:
				formula = Bool.lt(lvar, rvar);
				break;
			case TagConstants.BITOR:
				formula = Expression.bitor(lvar, rvar);
				break;
			case TagConstants.BITXOR:
				formula = Expression.bitxor(lvar, rvar);
				break;
			case TagConstants.BITAND:
				formula = Expression.bitand(lvar, rvar);
				break;
			case TagConstants.LSHIFT:
				formula = Num.lshift(lvar, rvar);
				break;
			case TagConstants.RSHIFT:
				formula = Num.rshift(lvar, rvar);
				break;
			case TagConstants.URSHIFT:
				formula = Num.urshift(lvar, rvar);
				break;
			case TagConstants.ADD:
				formula = Num.add(lvar, rvar);
				break;
			case TagConstants.SUB:
				formula = Num.sub(lvar, rvar);
				break;
			case TagConstants.STAR:
				formula = Num.mul(lvar, rvar);
				break;
				
			default:
				throw new IllegalArgumentException("Unmanaged construct :" +
						TagConstants.toString(tag) +" " +  left + " " + right);
				
		}
		Post rPost = new Post(rvar, post.post.substWith(formula));
		post.post = rPost;
		Post pre = getPre(right, post);
		Post lPost = new Post(lvar, pre.post);
		post.post = lPost;
		pre = getPre(left, post);
		return pre;
	}


	
	public Post excpBinExpression(int tag, Expr left, Expr right, VCEntry post) {

		
		QuantVariableRef rvar = Expression.rvar(Type.getSort(right));
		QuantVariableRef lvar = Expression.rvar(Type.getSort(left));
		Term formula;
		switch (tag) {
			case TagConstants.DIV:
				formula = Num.div(lvar, rvar);
				break;
			case TagConstants.MOD:
				formula = Num.mod(lvar, rvar);
				break;	
			default:
				throw new IllegalArgumentException("Unmanaged construct :" +
						TagConstants.toString(tag) +" " +  left + " " + right);	
		}
		Post rPost = new Post(rvar, Logic.and(
				Logic.implies(Logic.not(Logic.equals(rvar, Num.value(0))), 
						      post.post.substWith(formula)),
				Logic.implies(Logic.equals(rvar, Num.value(0)),
						getNewExcpPost(Type.javaLangArithmeticException(), post))));
		
		post.post = rPost;
		Post pre = getPre(right, post);
		Post lPost = new Post(lvar, pre.post);
		post.post = lPost;
		pre = getPre(left, post);
		return pre;
	}

	/**
	 * Do the weakest precondition calculus of the assign statement.
	 * @param expr the assign statement
	 * @param entry the post condition
	 * @return thw precondition of the assign statement
	 */
	public Post assign(BinaryExpr expr, VCEntry entry) {
		Post pre = assign(expr.left, entry);
		pre = getPre(expr.right, entry);
		return pre;
	}
	
	/**
	 * Do the wp for an assign statement. 
	 * The left element is given as a parameter,
	 * the right element of the assign (the 'value') is the 
	 * variable contained in <code>entry.post.var</code>.
	 * @param left the variable or field to assign
	 * @param entry the current postcondition
	 * @return a postcondition containing the proper assignment wp.
	 */
	public Post assign(Expr left, VCEntry entry) {
		QuantVariableRef val = entry.post.var;
		Post pre;
		if(left instanceof VariableAccess) {
			VariableAccess va = (VariableAccess) left;
			QuantVariableRef var = Expression.rvar(va.decl);
			pre = new Post(val, entry.post.post.subst(var, val));
		}
		else if (left instanceof FieldAccess) { 
			FieldAccess field = (FieldAccess) left;
			ObjectDesignator od = field.od;
			QuantVariableRef f = Expression.rvar(field.decl);
			Lookup.fieldsToDeclare.add(f.qvar);
			switch(od.getTag()) {
				case TagConstants.EXPROBJECTDESIGNATOR: {
					// can be null
					
					QuantVariableRef obj = Expression.rvar(Ref.sort);

					entry.post = new Post(obj, entry.post.post.subst(Heap.var, 
							 Heap.store(Heap.var, obj, f.qvar, val)));
					pre = getPre(od, entry);
					entry.post = new Post(val, pre.post);
					
					break;
				}
				case TagConstants.SUPEROBJECTDESIGNATOR:
					// I believe strongly (gasp) that super is not useful as it is 
					// contained in the field signature...
				case TagConstants.TYPEOBJECTDESIGNATOR: {
					// cannot be null
					
					pre = new Post(f, entry.post.post.subst(Heap.var, Heap.store(Heap.var, f.qvar, val)));
					//entry.post = pre;
					//pre = getPre(od, entry);
					pre = new Post(val, pre.post);
					entry.post = pre;

					break;
				}
				default: 
					throw new IllegalArgumentException("Unknown object designator type ! " + od);
				
			}
			
		}
		else { //(left instanceof ArrayRefExpr){
			ArrayRefExpr arr = (ArrayRefExpr) left;
			QuantVariableRef arrVar = Expression.rvar(Ref.sort);
			// this sort is bad
			System.out.println(Type.getSort(arr));
			//QuantVariableRef val = Expression.rvar(Type.getSort(arr));
			QuantVariableRef idx = Expression.rvar(Num.sortInt);
			QuantVariableRef exc = Expression.rvar(Ref.sort);
			Term tExcp = Logic.forall(exc.qvar, Logic.implies(Logic.equalsNull(arrVar), 
					               		StmtVCGen.getExcpPost(Type.javaLangNullPointerException(), entry).substWith(exc)));
			
			// the normal post
			Term tNormal = 	entry.post.post.subst(Heap.var, Heap.storeArray(Heap.var, arrVar,  idx, val));
			tNormal = Logic.implies(Logic.not(Logic.equalsNull(arrVar)), tNormal);
			Post post;
			post  = new Post(Logic.and(tNormal, tExcp));
			
			post = new Post(idx, post.post);
			entry.post = post;
			post = getPre(arr.index, entry);
		
			post = new Post(arrVar, post.post);
			entry.post = post;
			post = getPre(arr.array, entry);
			
			pre = new Post(val, post.post);
			entry.post = pre;
			
		}
		
				
		return pre;
	}

	/**
	 * Do the wp of an assignement followed by an operation 
	 * @param expr
	 * @param post
	 * @return the corresponding postcondition
	 */
	public Post assignSpecial(BinaryExpr expr, VCEntry post) {
		Expr right = expr.right;
		Expr left = expr.left;
		QuantVariableRef val = post.post.var;
		Post pre = assign(left, post);
		pre = new Post(val, pre);
		post.post = pre;
		switch (expr.op){
			case TagConstants.ASGMUL:
				pre = stdBinExpression(TagConstants.STAR, left, right, post);
				break;
			case TagConstants.ASGDIV:
				pre = excpBinExpression(TagConstants.DIV, left, right, post);
				break;
			case TagConstants.ASGREM:
				pre = excpBinExpression(TagConstants.MOD, left, right, post);
				break;
			case TagConstants.ASGADD:
				pre = stdBinExpression(TagConstants.ADD, left, right, post);
				break;
			case TagConstants.ASGSUB:
				pre = stdBinExpression(TagConstants.SUB, left, right, post);
				break;
			case TagConstants.ASGLSHIFT:
				pre = stdBinExpression(TagConstants.LSHIFT, left, right, post);
				break;
			case TagConstants.ASGRSHIFT:
				pre = stdBinExpression(TagConstants.RSHIFT, left, right, post);
				break;
			case TagConstants.ASGURSHIFT:
				pre = stdBinExpression(TagConstants.URSHIFT, left, right, post);
				break;
			case TagConstants.ASGBITAND:
				pre = stdBinExpression(TagConstants.BITAND, left, right, post);
				break;
			case TagConstants.ASGBITOR:
				pre = stdBinExpression(TagConstants.BITOR, left, right, post);
				break;
			case TagConstants.ASGBITXOR:
				pre = stdBinExpression(TagConstants.BITXOR, left, right, post);
				break;
			default:
				throw new IllegalArgumentException("Unmanaged construct :" +
						TagConstants.toString(expr.op) +" " +  expr);
		}
		return pre;
	}
}
