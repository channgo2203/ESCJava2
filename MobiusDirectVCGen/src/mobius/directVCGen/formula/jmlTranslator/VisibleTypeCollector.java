package mobius.directVCGen.formula.jmlTranslator;



import mobius.directVCGen.formula.*;
import mobius.directVCGen.formula.annotation.AAnnotation;
import mobius.directVCGen.formula.annotation.AnnotationDecoration;
import mobius.directVCGen.formula.annotation.Assume;
import mobius.directVCGen.formula.annotation.Cut;
import mobius.directVCGen.formula.annotation.Set;
import mobius.directVCGen.vcgen.struct.Post;

import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;
import javafe.ast.ASTNode;
import javafe.ast.BinaryExpr;
import javafe.ast.BlockStmt;
import javafe.ast.ClassDecl;
import javafe.ast.ConstructorDecl;
import javafe.ast.DoStmt;
import javafe.ast.Expr;
import javafe.ast.FieldAccess;
import javafe.ast.ForStmt;
import javafe.ast.FormalParaDecl;
import javafe.ast.IfStmt;
import javafe.ast.InstanceOfExpr;
import javafe.ast.JavafePrimitiveType;
import javafe.ast.LiteralExpr;
import javafe.ast.LocalVarDecl;
import javafe.ast.MethodDecl;
import javafe.ast.ModifierPragma;
import javafe.ast.PrimitiveType;
import javafe.ast.RoutineDecl;
import javafe.ast.Stmt;
import javafe.ast.ThisExpr;
import javafe.ast.TryCatchStmt;
import javafe.ast.UnaryExpr;
import javafe.ast.VarDeclStmt;
import javafe.ast.VariableAccess;
import javafe.ast.WhileStmt;
import escjava.ast.AnOverview;
import escjava.ast.ArrayRangeRefExpr;
import escjava.ast.CondExprModifierPragma;
import escjava.ast.Condition;
import escjava.ast.DecreasesInfo;
import escjava.ast.DefPred;
import escjava.ast.DefPredApplExpr;
import escjava.ast.DefPredLetExpr;
import escjava.ast.DependsPragma;
import escjava.ast.EscPrimitiveType;
import escjava.ast.EverythingExpr;
import escjava.ast.ExprDeclPragma;
import escjava.ast.ExprModifierPragma;
import escjava.ast.ExprStmtPragma;
import escjava.ast.GCExpr;
import escjava.ast.GhostDeclPragma;
import escjava.ast.GuardExpr;
import escjava.ast.GuardedCmd;
import escjava.ast.IdExprDeclPragma;
import escjava.ast.IdentifierModifierPragma;
import escjava.ast.ImportPragma;
import escjava.ast.LockSetExpr;
import escjava.ast.MapsExprModifierPragma;
import escjava.ast.ModelConstructorDeclPragma;
import escjava.ast.ModelDeclPragma;
import escjava.ast.ModelMethodDeclPragma;
import escjava.ast.ModelProgamModifierPragma;
import escjava.ast.ModelTypePragma;
import escjava.ast.ModifiesGroupPragma;
import escjava.ast.NamedExprDeclPragma;
import escjava.ast.NaryExpr;
import escjava.ast.NestedModifierPragma;
import escjava.ast.NotModifiedExpr;
import escjava.ast.NotSpecifiedExpr;
import escjava.ast.NothingExpr;
import escjava.ast.NowarnPragma;
import escjava.ast.ParsedSpecs;
import escjava.ast.ReachModifierPragma;
import escjava.ast.RefinePragma;
import escjava.ast.ResExpr;
import escjava.ast.SetCompExpr;
import escjava.ast.SetStmtPragma;
import escjava.ast.SimpleModifierPragma;
import escjava.ast.SimpleStmtPragma;
import escjava.ast.SkolemConstantPragma;
import escjava.ast.Spec;
import escjava.ast.StillDeferredDeclPragma;
import escjava.ast.TagConstants;
import escjava.ast.VarDeclModifierPragma;
import escjava.ast.VarExprModifierPragma;
import escjava.ast.VisitorArgResult;
import escjava.ast.WildRefExpr;
import escjava.sortedProver.Lifter.QuantVariableRef;
import escjava.sortedProver.Lifter.Term;



public class VisibleTypeCollector extends VisitorArgResult {

	java.util.Set<String> typeSet;
	
	public VisibleTypeCollector(){
		typeSet = new HashSet<String>();
	}
	
	@Override
	public Object visitASTNode(ASTNode x, Object prop) {
		Object o = null;
		int max = x.childCount();
		for(int i = 0; i < max; i++) {
			Object child = x.childAt(i);
			if(child instanceof ASTNode) {
				o = ((ASTNode) child).accept(this, prop);
			}
		}
		return o;
	}
	
	@Override
	public /*@non_null*/ Object visitClassDecl(/*@non_null*/ ClassDecl x, Object o) {
		//should never be called
		return visitTypeDecl(x, o);
	}
	

	public /*@non_null*/ Object visitRoutineDecl(/*@non_null*/ RoutineDecl x, Object o) {
		typeSet.add(x.parent.id.toString()); //add own class type into set
		((Properties) o).put("assign", new Boolean(false));
		visitASTNode(x, o); //return value not needfull
		((Properties) o).put("visibleTypeSet", typeSet); //put set into properties once for each routine
		return null;
	}
	
	@Override
	public /*@non_null*/ Object visitMethodDecl(/*@non_null*/ MethodDecl x, Object o) {
		return visitRoutineDecl(x, o);
	}
	
	@Override
	public /*@non_null*/ Object visitConstructorDecl(/*@non_null*/ ConstructorDecl x, Object o) {
		return visitRoutineDecl(x, o);
	}
	
	@Override
	public /*@non_null*/ Object visitFormalParaDecl(/*@non_null*/ FormalParaDecl x, Object o) {
		return null;
	}
	
	@Override
	public Object visitAnOverview(AnOverview x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	 public /*@non_null*/ Object visitLiteralExpr(/*@non_null*/ LiteralExpr x, Object o) {
			return null;
	}
	 
	 
	 @Override
	 //nur argumente mit lokalen vars. die lokalen vars sind fieldaccess
	 public /*@non_null*/ Object visitVariableAccess(/*@non_null*/ VariableAccess x, Object o) {		 
		 if (((Boolean) ((Properties) o).get("assign")).booleanValue())
		 {
			 javafe.ast.Type type = (javafe.ast.Type) x.getDecorations()[1]; //javafetypes global def. als fetypes
			 if (!(type instanceof PrimitiveType)) //sonst alle nehmen? was ist mit errortypes?
			 {
				 typeSet.add(type.toString());
			 }
			//alle primitivetypes nicht nehmen oder nur JavafePrimitivetype????
		 }
			return null;
	}
	 
	 @Override
	 public /*@non_null*/ Object visitFieldAccess(/*@non_null*/ FieldAccess x, Object o) {		 
		 	
		 	javafe.ast.Type type = (javafe.ast.Type) x.od.type();
			if (!(type instanceof PrimitiveType)&&(((Boolean) ((Properties) o).get("assign")).booleanValue()))
			{
				typeSet.add(type.toString());
			}
			
			((Properties) o).put("assign", new Boolean(false));
		 	((Expr)x.od.childAt(0)).accept(this,o);
		 	
			return null;
	}
	 
		 
	 @Override
	 public /*@non_null*/ Object visitNaryExpr(/*@non_null*/ NaryExpr x, Object o) {
			return null;
	}
	 
	 @Override
	 public /*@non_null*/ Object visitInstanceOfExpr(/*@non_null*/ InstanceOfExpr x, Object o) {
			return null;
	 }
	 
	 @Override
	 public Object  visitThisExpr(ThisExpr x, Object o) {
			return null;
	 }
	 
	
	 
	@Override
	public Object visitArrayRangeRefExpr(ArrayRangeRefExpr x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitCondExprModifierPragma(CondExprModifierPragma x, Object o) {
		// TODO Auto-generated method stub
		//return null;
		return visitASTNode(x, o);
	}

	@Override
	public Object visitCondition(Condition x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitDecreasesInfo(DecreasesInfo x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitDefPred(DefPred x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitDefPredApplExpr(DefPredApplExpr x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Object visitDefPredLetExpr(DefPredLetExpr x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitDependsPragma(DependsPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitEscPrimitiveType(EscPrimitiveType x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitEverythingExpr(EverythingExpr x, Object o) {
		// TODO Auto-generated method stub
		//return null;
		return visitASTNode(x, o);
	}

	@Override
	public Object visitExprDeclPragma(ExprDeclPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitExprModifierPragma(ExprModifierPragma x, Object o) {
		return null;
	}
		
	@Override
	public Object visitExprStmtPragma(ExprStmtPragma x, Object o) {
		// TODO Auto-generated method stub
		return visitASTNode(x, o);
	}

	@Override
	public Object visitGCExpr(GCExpr x, Object o) {
		return visitASTNode(x, o);
	}

	@Override
	public Object visitGhostDeclPragma(GhostDeclPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitGuardExpr(GuardExpr x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitGuardedCmd(GuardedCmd x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIdExprDeclPragma(IdExprDeclPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitIdentifierModifierPragma(IdentifierModifierPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitImportPragma(ImportPragma x, Object o) {
		// TODO Auto-generated method stub
		//return null;
		return visitASTNode(x, o);
	}

	@Override
	public Object visitLockSetExpr(LockSetExpr x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitMapsExprModifierPragma(MapsExprModifierPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitModelConstructorDeclPragma(ModelConstructorDeclPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitModelDeclPragma(ModelDeclPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitModelMethodDeclPragma(ModelMethodDeclPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitModelProgamModifierPragma(ModelProgamModifierPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitModelTypePragma(ModelTypePragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitModifiesGroupPragma(ModifiesGroupPragma x, Object o) {
		// TODO Auto-generated method stub
		//return null;
		return null; //visitASTNode(x, o);
	}

	@Override
	public Object visitNamedExprDeclPragma(NamedExprDeclPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNestedModifierPragma(NestedModifierPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNotModifiedExpr(NotModifiedExpr x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNotSpecifiedExpr(NotSpecifiedExpr x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNothingExpr(NothingExpr x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitNowarnPragma(NowarnPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitParsedSpecs(ParsedSpecs x, Object o) {
		// TODO Auto-generated method stub
		//return visitASTNode(x, o); //generates a stack overflow... but should be used
		return null;
	}

	@Override
	public Object visitReachModifierPragma(ReachModifierPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitRefinePragma(RefinePragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitResExpr(ResExpr x, Object o) {
			return null;
	}

	@Override
	public Object visitSetCompExpr(SetCompExpr x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitSetStmtPragma(SetStmtPragma x, Object o) {
		return null;
	}

	@Override
	public Object visitSimpleModifierPragma(SimpleModifierPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitSimpleStmtPragma(SimpleStmtPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitSkolemConstantPragma(SkolemConstantPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitSpec(Spec x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitStillDeferredDeclPragma(StillDeferredDeclPragma x, Object o) {
		return null;
	}

	@Override
	public Object visitVarDeclModifierPragma(VarDeclModifierPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visitVarExprModifierPragma(VarExprModifierPragma x, Object o) {
		// TODO Auto-generated method stub
		return null;//visitASTNode(x, o); 
	}

	@Override
	public Object visitWildRefExpr(WildRefExpr x, Object o) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	@Override
	/**
	 * TagConstants.ASSIGN = 75
	 * TagConstants.ASGMUL = 76
	 * TagConstants.ASGDIV = 77
	 * TagConstants.ASGREM = 78
	 * TagConstants.ASGADD = 79
	 * TagConstants.ASGSUB = 80		
	 * TagConstants.ASGLSHIFT = 81
	 * TagConstants.ASGRSHIFT = 82			
     * TagConstants.ASGURSHIFT = 83
	 * TagConstants.ASGBITAND = 84
	 * --> Relevant operation in range [75..84]
	 */
	public Object visitBinaryExpr(BinaryExpr expr, Object o){
		if ((expr.op >= 75) && (expr.op <= 84))
		{
			((Properties) o).put("assign", new Boolean(false));
			expr.right.accept(this, o); 
			((Properties) o).put("assign", new Boolean(true));
			expr.left.accept(this,o); 
			return null;
		}
		else
		return visitExpr(expr, o); //for all other operations	
	}
	
	@Override
	public /*@non_null*/ Object visitUnaryExpr(/*@non_null*/ UnaryExpr x, Object o) {
		((Properties) o).put("assign", new Boolean(true));
	    return visitExpr(x, o);
	  }

}