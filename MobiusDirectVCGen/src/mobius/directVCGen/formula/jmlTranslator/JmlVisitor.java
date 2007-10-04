package mobius.directVCGen.formula.jmlTranslator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import javafe.ast.ASTNode;
import javafe.ast.ArrayRefExpr;
import javafe.ast.BinaryExpr;
import javafe.ast.BlockStmt;
import javafe.ast.ClassDecl;
import javafe.ast.ConstructorDecl;
import javafe.ast.DoStmt;
import javafe.ast.FieldAccess;
import javafe.ast.ForStmt;
import javafe.ast.FormalParaDecl;
import javafe.ast.Identifier;
import javafe.ast.IfStmt;
import javafe.ast.InstanceOfExpr;
import javafe.ast.LiteralExpr;
import javafe.ast.LocalVarDecl;
import javafe.ast.MethodDecl;
import javafe.ast.ModifierPragma;
import javafe.ast.NewInstanceExpr;
import javafe.ast.RoutineDecl;
import javafe.ast.SkipStmt;
import javafe.ast.Stmt;
import javafe.ast.ThisExpr;
import javafe.ast.TryCatchStmt;
import javafe.ast.UnaryExpr;
import javafe.ast.VarDeclStmt;
import javafe.ast.VariableAccess;
import javafe.ast.WhileStmt;
import mobius.directVCGen.formula.Expression;
import mobius.directVCGen.formula.Heap;
import mobius.directVCGen.formula.Logic;
import mobius.directVCGen.formula.Lookup;
import mobius.directVCGen.formula.Ref;
import mobius.directVCGen.formula.Type;
import mobius.directVCGen.formula.annotation.AAnnotation;
import mobius.directVCGen.formula.annotation.AnnotationDecoration;
import mobius.directVCGen.formula.annotation.Assume;
import mobius.directVCGen.formula.annotation.Cut;
import mobius.directVCGen.formula.annotation.Set;
import mobius.directVCGen.vcgen.struct.Post;
import escjava.ast.AnOverview;
import escjava.ast.CondExprModifierPragma;
import escjava.ast.EverythingExpr;
import escjava.ast.ExprDeclPragma;
import escjava.ast.ExprModifierPragma;
import escjava.ast.ExprStmtPragma;
import escjava.ast.GCExpr;
import escjava.ast.ImportPragma;
import escjava.ast.ModifiesGroupPragma;
import escjava.ast.NaryExpr;
import escjava.ast.NothingExpr;
import escjava.ast.ParsedSpecs;
import escjava.ast.QuantifiedExpr;
import escjava.ast.ResExpr;
import escjava.ast.SetStmtPragma;
import escjava.ast.TagConstants;
import escjava.ast.TypeExpr;
import escjava.ast.VarExprModifierPragma;
import escjava.sortedProver.Lifter.QuantVariable;
import escjava.sortedProver.Lifter.QuantVariableRef;
import escjava.sortedProver.Lifter.Term;
import escjava.tc.Types;


/**
 * @author Claudia Brauchli (claudia@vis.ethz.ch)
 * @author Hermann Lehner (hermann.lehner@inf.ethz.ch)
 * 
 */
public class JmlVisitor extends BasicJMLTranslator {
  /** global properties of a class. */
  final GlobalProperties fGlobal = new GlobalProperties();
  
  /** Reference to JML Expression Translator. */
  private final JmlExprToFormula fTranslator;
  
  /** the subset checking option. */
  private boolean fDoSubsetChecking;
  
  /**
   * Visitor that translates JML Constructs to FOL by using JmlExprToFormula to
   * translate expressions.
   */
  public JmlVisitor() {
    this(false);
     
  }

  /**
   * Visitor that translates JML Constructs to FOL by using JmlExprToFormula to
   * translate expressions.
   * @param doSubsetChecking if the subset checking has to be done
   */
  public JmlVisitor(final boolean doSubsetChecking) {
    fDoSubsetChecking = doSubsetChecking;
    
    fTranslator = new JmlExprToFormula(this);
     
  }



  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitClassDecl(javafe.ast.ClassDecl, java.lang.Object)
   */
  @Override
  public final Object visitClassDecl(final /*@non_null*/ ClassDecl x, final Object o) {
    fGlobal.put("classId", x.id);
    
    //Use default properties to start with.
    return visitTypeDecl(x, null);
  }

  
  
  
  /* (non-Javadoc 
   * @see javafe.ast.VisitorArgResult#visitRoutineDecl(javafe.ast.RoutineDecl, java.lang.Object)
   */
  @Override
  public final Object visitRoutineDecl(final /*@non_null*/ RoutineDecl x, final Object o) {
    final MethodProperties prop = (MethodProperties) o;
    x.accept(new VisibleTypeCollector(), o); 
    prop.put("method", x);
    prop.put("isHelper", Boolean.FALSE);
    prop.put("routinebegin", Boolean.TRUE);
    prop.put("nothing", Boolean.FALSE);
    
    boolean hasPost = false;
    int tag;
    // Check, if method has at least one postcondition/exceptional postcondition or is a helper method
    for (int i = 0; i < x.pmodifiers.size(); i++) {
      tag = x.pmodifiers.elementAt(i).getTag();
      if ((tag == TagConstants.ENSURES) | (tag == TagConstants.POSTCONDITION) | (tag == TagConstants.POSTCONDITION_REDUNDANTLY)) {
        hasPost = true;
      }
      else if (x.pmodifiers.elementAt(i).getTag() == TagConstants.HELPER) {
        ((Properties) o).put("isHelper", Boolean.TRUE);
      }
    }
    
    // If method is not decorated with any postcondition, we add a dummy postcondition node "//@ ensures true;"
    if (!hasPost) {
      final LiteralExpr litEx = LiteralExpr.make(TagConstants.BOOLEANLIT, Boolean.TRUE, 0);
      final ExprModifierPragma postc = ExprModifierPragma.make(TagConstants.ENSURES, litEx, 0);  //FIXME: cbr: which loc? (here set to 0)
      x.pmodifiers.addElement(postc);
    }
    
    // Add dummy exceptional postcondition to Lookup hash map   
    Lookup.exceptionalPostconditions.put(x, new Post(Expression.rvar(Ref.sort), Logic.True())); 
    prop.put("firstExPost", Boolean.TRUE);
    
    visitASTNode(x, o);
    doAssignable(o);
    
    if (!((Boolean) ((Properties) o).get("isHelper")).booleanValue()) {
      invPredToPreconditions(o);
      invPredToPostconditions(o);
      invPredToExceptionalPostconditions(o);    
    }  
    return null;
  }

  
  
  

  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitMethodDecl(javafe.ast.MethodDecl, java.lang.Object)
   */
  @Override
  public final Object visitMethodDecl(final /*@non_null*/ MethodDecl x, final Object o) {
    final MethodProperties prop = new MethodProperties();
    prop.fResult =  Expression.rvar(Expression.getResultVar(x));
    visitRoutineDecl(x, o);
    
    if (((Boolean)((Properties)o).get("isHelper")).booleanValue() == Boolean.FALSE) {
      final Term constraints = Lookup.constraints.get(x.getParent());
      addToPostcondition(constraints, o);
      addToExceptionalPostcondition(constraints, o);
    }  
    return prop;
  }

  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitConstructorDecl(javafe.ast.ConstructorDecl, java.lang.Object)
   */
  @Override
  public final Object visitConstructorDecl(final /*@non_null*/ ConstructorDecl x, final Object o) {
    ((Properties) o).put("isConstructor", Boolean.TRUE);
    visitRoutineDecl(x, o);
    ((Properties) o).put("isConstructor", Boolean.FALSE);
 
    if (((Boolean)((Properties)o).get("isHelper")).booleanValue() == Boolean.FALSE) {
      Term initially = (Term) ((Properties)o).get("initiallyFOL");
      addToPostcondition(initially, o);
      addToExceptionalPostcondition(initially, o);
    } 
    return null;
  }

  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitFormalParaDecl(javafe.ast.FormalParaDecl, java.lang.Object)
   */
  @Override
  public final Object visitFormalParaDecl(final /*@non_null*/ FormalParaDecl x, final Object o) {
    return this.fTranslator.genericVarDecl(x, o);
  }

  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitLiteralExpr(javafe.ast.LiteralExpr, java.lang.Object)
   */
  @Override
  public final Object visitLiteralExpr(final /*@non_null*/ LiteralExpr x, final Object o) {
    if (fGlobal.interesting) {
      return this.fTranslator.literal(x, o);
    }
    else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitVariableAccess(javafe.ast.VariableAccess, java.lang.Object)
   */
  @Override
  public final Object visitVariableAccess(final /*@non_null*/ VariableAccess x, final Object o) {
    if (fGlobal.interesting) {
      return this.fTranslator.variableAccess(x, o);
    }
    else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitFieldAccess(javafe.ast.FieldAccess, java.lang.Object)
   */
  @Override
  public final Object visitFieldAccess(final /*@non_null*/ FieldAccess x, final Object o) {
    if (fGlobal.interesting) {
      return this.fTranslator.fieldAccess(x, o);
    }
    else {
      return null;
    }
  }
  
  
  
  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitLocalVarDecl(javafe.ast.LocalVarDecl, java.lang.Object)
   */
  @Override
  public Object visitLocalVarDecl(final /*@non_null*/ LocalVarDecl x, final Object o) {
    
    if (((Boolean) ((Properties) o).get("quantifier")).booleanValue()) {
      HashSet<QuantVariable> qVarsSet = (HashSet) ((Properties) o).get("quantVars");
      final QuantVariable qvar = Expression.var(x);
      qVarsSet.add(qvar);
      ((Properties) o).put("quantVars", qVarsSet);
    }   
    return null;
  }

  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitNaryExpr(escjava.ast.NaryExpr, java.lang.Object)
   */
  @Override
  public final Object visitNaryExpr(final /*@non_null*/ NaryExpr x, final Object o) {
    if (fGlobal.interesting) {
      if (x.op == TagConstants.PRE) {
        return this.fTranslator.oldExpr(x, o);
      }
      else if (x.op == TagConstants.FRESH) {
        return this.fTranslator.freshExpr(x, o);
      }
      else if (x.op == TagConstants.TYPEOF) {
        final Term exprTerm = (Term) visitGCExpr(x, o);
        return Type.of(Heap.var, exprTerm);
      }
      else {
        return visitGCExpr(x, o);
      }
    }
    else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitInstanceOfExpr(javafe.ast.InstanceOfExpr, java.lang.Object)
   */
  @Override
  public final Object visitInstanceOfExpr(final /*@non_null*/ InstanceOfExpr x, final Object o) {
    if (fGlobal.interesting) {
      return this.fTranslator.instanceOfExpr(x, o);
    }
    else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitThisExpr(javafe.ast.ThisExpr, java.lang.Object)
   */
  @Override
  public final Object visitThisExpr(final /*@non_null*/ ThisExpr x, final Object o) {
    if (fGlobal.interesting) {
      return this.fTranslator.thisLiteral(x, o);
    }
    else {
      return null;
    }
  }

  
  public /*@non_null*/ Object visitArrayRefExpr(/*@non_null*/ ArrayRefExpr x, Object o) {
    final Term var = (Term) x.array.accept(this, o); 
    final Term idx = (Term) x.index.accept(this, o);
    
    return Heap.selectArray(Heap.var, var, idx, Type.getSort(x));
  }
  

  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitCondExprModifierPragma(escjava.ast.CondExprModifierPragma, java.lang.Object)
   */
  @Override
  public final Object visitCondExprModifierPragma(final /*@non_null*/ CondExprModifierPragma x, final Object o) {
    
    if (x.getTag() == TagConstants.ASSIGNABLE) {
      if (x.expr instanceof FieldAccess) {
        final HashSet<QuantVariableRef[]> fAssignableSet = (HashSet<QuantVariableRef[]>) ((Properties) o).get("assignableSet");
        final FieldAccess var = (FieldAccess) x.expr;
        final QuantVariableRef targetVar = (QuantVariableRef) var.od.accept(this, o);
        final QuantVariableRef fieldVar = Expression.rvar(var.decl);
        final QuantVariableRef[] qvars = {targetVar, fieldVar};
        fAssignableSet.add(qvars);
        ((Properties) o).put("assignableSet", fAssignableSet);    
      } 
      else if (x.expr instanceof NothingExpr) {
        ((Properties) o).put("nothing", Boolean.TRUE);
      }
    }
    
    return visitASTNode(x, o);
  }
 
  
 

  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitEverythingExpr(escjava.ast.EverythingExpr, java.lang.Object)
   */
  @Override
  public final Object visitEverythingExpr(final /*@non_null*/ EverythingExpr x, final Object o) {
    return visitASTNode(x, o);
  }

  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitExprDeclPragma(escjava.ast.ExprDeclPragma, java.lang.Object)
   */
  @Override
  public final Object visitExprDeclPragma(final /*@non_null*/ ExprDeclPragma x, final Object o) {
    
    fGlobal.interesting = true;
    Term t;
    
    if (x.tag == TagConstants.INITIALLY) {
      fGlobal.put("doSubsetChecking", Boolean.TRUE); // to collect all fields in initially to do the subset check
      final Term initiallyFOL = (Term) x.expr.accept(this, o);
      fGlobal.put("doSubsetChecking", Boolean.FALSE);
      t = (Term) ((Properties) o).get("initiallyFOL");
      boolean initIsValid = doSubsetChecking(o);
      if (initIsValid) {
        if (initiallyFOL != null) { 
          if (t != null) {
            t = Logic.and(t, initiallyFOL);
          }
          else {
            t = initiallyFOL;
          }
        }
        ((Properties) o).put("initiallyFOL", t);
      }
      else if (!initIsValid) {
        System.out.println("Initially error (subset check)! The following term was not conjoined to the overall type initially term: " + initiallyFOL.toString() + "\n");
      }
    }
    else if (x.tag == TagConstants.INVARIANT) { 
      fGlobal.put("doSubsetChecking", Boolean.TRUE);
      t = (Term) x.expr.accept(this, o);
      fGlobal.put("doSubsetChecking", Boolean.FALSE);
      addToInv(x, t, o);
    }
    else if (x.tag == TagConstants.CONSTRAINT) {
      fGlobal.put("doSubsetChecking", Boolean.TRUE); 
      t = (Term) x.expr.accept(this, o);
      fGlobal.put("doSubsetChecking", Boolean.FALSE);
      constrToConstraints(x, t, o);
    }
    return null;
  }
  
  
  /**
   * @param x constraint node containing the parents class declaration
   * @param t translated term to conjoin the class constraints
   * @param o object containing the flag for subset checking
   */
  public void constrToConstraints(ExprDeclPragma x, Term t, Object o) {
    boolean constIsValid = true;
    Term constTerm = t;
    
    final Term allConst = Lookup.constraints.get(x.parent);
    
    if (((Boolean) ((Properties) o).get("dsc")).booleanValue()) { 
      constIsValid = doSubsetChecking(o);
    }
    if (constIsValid && allConst != null) {
      constTerm = Logic.and(allConst, constTerm); 
    }
    else if (constIsValid) {
      Lookup.constraints.put(x.parent, constTerm); 
    }
    else if (!constIsValid) {
      System.out.println("Constraint error (subset check)! The following term was not conjoined to the overall type constraints: " + t.toString() + "\n");
    }
  }
  

  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitExprModifierPragma(escjava.ast.ExprModifierPragma, java.lang.Object)
   */
  @Override
  public final Object visitExprModifierPragma(final /*@non_null*/ ExprModifierPragma x, final Object o) {
    fGlobal.interesting = true;
    Term t = (Term)visitASTNode(x, o);
    t = Logic.boolToPred(t);
    switch (x.getTag()) {
      case TagConstants.REQUIRES:
        addToPrecondition(t, o);
        break;
      case TagConstants.ENSURES:
      case TagConstants.POSTCONDITION:
      case TagConstants.POSTCONDITION_REDUNDANTLY:
        addToPostcondition(t, o);
        break;
      default:
        break;
    }
    return null;
  }

  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitVarExprModifierPragma(escjava.ast.VarExprModifierPragma, java.lang.Object)
   */
  @Override
  public final Object visitVarExprModifierPragma(final /*@non_null*/ VarExprModifierPragma x, final Object o) {
    fGlobal.interesting = true;

    final RoutineDecl currentRoutine = (RoutineDecl) ((Properties) o).get("method");
    final Post allExPosts = Lookup.exceptionalPostconditions.get(currentRoutine);
    final QuantVariableRef commonExceptionVar = allExPosts.getRVar();

    final Term typeOfException = Type.translate(x.arg.type);
    final QuantVariableRef newExceptionVar = Expression.rvar(x.arg);

    Term newExPost = (Term)x.expr.accept(this, o);
    newExPost = newExPost.subst(newExceptionVar, commonExceptionVar);
    final Term guard = Logic.assignCompat(Heap.var, commonExceptionVar, typeOfException);
    final Term result = Logic.Safe.implies(guard, newExPost);
    addToExceptionalPostcondition(result, o);
    return null;
  }

  // Save values of all arguments as ghost variables. Now we also have the argument's value of the pre-state, not only of post-state
  /**
   * @param annos Vector of AAnotations, here Annotations = Assignments
   * @param o Properties Object holding routines declaration
   */
  public void argsToGhost(Vector<AAnnotation> annos, Object o) {  
    final RoutineDecl m = (RoutineDecl) ((Properties) o).get("method");
    for (final FormalParaDecl p: m.args.toArray()) {
      final Term t1 = Expression.rvar(p);
      final Term t2 = Expression.old(p);
      final Set.Assignment assignment = new Set.Assignment((QuantVariableRef) t2, t1);
      annos.add(new Set((QuantVariableRef) t2, assignment)); 
    }
  }
  
  /**
   * @param x BlockStmt holding all statements of one entire block
   * @param annos Collection of statement pragmas as annotations
   * @param o Object as Properties object
   */
  public void handleStmt(final BlockStmt x, final Vector<AAnnotation> annos, final Object o) {
    Term inv = null;
    Term t = null;
    Set.Assignment assignment = null;
    boolean interesting;
   
    for (final Stmt s: x.stmts.toArray()) {
      interesting = false;
      if (s instanceof ExprStmtPragma) { //Asserts, Assumes and Loop Invariants
        interesting = true; 
        fGlobal.interesting = true;
        t = (Term)s.accept(this, o);
        switch (s.getTag()) {
          case javafe.parser.TagConstants.ASSERT:
            annos.add(new Cut(t));
            break;
          case TagConstants.ASSUME:
            annos.add(new Assume(t));
            break;
          case TagConstants.LOOP_INVARIANT:
          case TagConstants.LOOP_INVARIANT_REDUNDANTLY:
          case TagConstants.MAINTAINING:
          case TagConstants.MAINTAINING_REDUNDANTLY:
            if (inv != null) {
              inv = Logic.and(inv, t);
            }
            else {
              inv = t;
            }
            break;
          default:
            break;
        }
      }
      else {
        if (s instanceof VarDeclStmt) { // Ghost var declarations
          for (final ModifierPragma p: ((VarDeclStmt) s).decl.pmodifiers.toArray()) {
            if (p.getTag() == TagConstants.GHOST) {
              interesting = true;
              break;
            }
          }
          if (interesting) {
            fGlobal.interesting = true;
            final Set ghostVar = (Set) s.accept(this, o);
            annos.add(ghostVar);
          }
        }
        else {
          if (s instanceof SetStmtPragma) {
            interesting = true;
            fGlobal.interesting = true;
            assignment = (Set.Assignment)s.accept(this, o);
            final Set newSet = new Set();
            newSet.assignment = assignment;
            final Iterator iter = annos.iterator();
   
            while (iter.hasNext()) { 
              final AAnnotation annot = (AAnnotation) iter.next();
              if (annot instanceof Set) {
                final Set existingSet = (Set) annot;
                if (existingSet.declaration.equals(newSet.assignment.var)) {
                  annos.remove(existingSet);
                  newSet.declaration = existingSet.declaration;
                  break;
                }
              }
            }
            annos.add(newSet);
          }
        }
      }
      if (interesting) {
        x.stmts.removeElement(s);
      } 
      else { // Put annotations to next Java Stmt
        fGlobal.interesting = false;
        if (!annos.isEmpty()) {
          AnnotationDecoration.inst.setAnnotPre(s, annos);
          annos.clear();
        }
        if (inv != null) { // Add inv as invariant to next Loop Stmt
          if (s instanceof WhileStmt || 
              s instanceof ForStmt || 
              s instanceof DoStmt) {
            AnnotationDecoration.inst.setInvariant(s, inv);
            inv = null;
          }
        }
        if (s instanceof WhileStmt ||  // Visit body of Loop Stmt
            s instanceof ForStmt || 
            s instanceof DoStmt || 
            s instanceof BlockStmt || 
            s instanceof TryCatchStmt ||
            s instanceof IfStmt) {
          s.accept(this, o);
        }
      }
    }
  }
  
  
  
  
  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitBlockStmt(javafe.ast.BlockStmt, java.lang.Object)
   */
  @Override
  public final Object visitBlockStmt(final /*@non_null*/ BlockStmt x, final Object o) {
    final Vector<AAnnotation> annos = new Vector<AAnnotation>();
    
    //Save argument's values in prestate as ghosts at beginning of each routine (in annos)
    if (((Boolean)((Properties) o).get("routinebegin")).booleanValue()) {
      ((Properties) o).put("routinebegin", Boolean.FALSE);
      argsToGhost(annos, o);
    }

    handleStmt(x, annos, o);
    
    // If there is no more Stmt, we generate a dummy SkipStmt to add last Stmt Pragma as precondition
    if (!annos.isEmpty()) { 
      final SkipStmt skipStmt = SkipStmt.make(0); //FIXME cbr: which location?
      AnnotationDecoration.inst.setAnnotPre(skipStmt, annos);
      x.stmts.addElement(skipStmt);
    }    
    return null;
  }

  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitVarDeclStmt(javafe.ast.VarDeclStmt, java.lang.Object)
   */
  @Override
  public final Object visitVarDeclStmt(final /*@non_null*/ VarDeclStmt x, final Object o) {
    //It's only called if we have a ghost variable declaration with maybe a set stmt
    final Set ghostVar = new Set();
    if (x.decl.init != null) {
      ghostVar.assignment = new Set.Assignment();
      ghostVar.assignment.expr = (Term) x.decl.init.accept(this, o);
      ghostVar.assignment.var = Expression.rvar(x.decl);
    }
    ghostVar.declaration = Expression.rvar(x.decl); 
    return ghostVar;
 }

  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitExprStmtPragma(escjava.ast.ExprStmtPragma, java.lang.Object)
   */
  @Override
  public final Object visitExprStmtPragma(final /*@non_null*/ ExprStmtPragma x, final Object o) {
    return visitASTNode(x, o);
  }

  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitGCExpr(escjava.ast.GCExpr, java.lang.Object)
   */
  @Override
  public final Object visitGCExpr(final /*@non_null*/ GCExpr x, final Object o) {
    
    if (x instanceof TypeExpr) { 
      final String name = Types.printName(((TypeExpr)x).type);
      return Expression.rvar(name, Type.sort);
    }
    return visitASTNode(x, o);
  }


  
  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitImportPragma(escjava.ast.ImportPragma, java.lang.Object)
   */
  @Override
  public final Object visitImportPragma(final /*@non_null*/ ImportPragma x, final Object o) {
    return visitASTNode(x, o);
  }



  
  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitModifiesGroupPragma(escjava.ast.ModifiesGroupPragma, java.lang.Object)
   */
  @Override
  public final Object visitModifiesGroupPragma(final /*@non_null*/ ModifiesGroupPragma x, final Object o) {
    return visitASTNode(x, o);
  }



  
  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitParsedSpecs(escjava.ast.ParsedSpecs, java.lang.Object)
   */
  @Override
  public final Object visitParsedSpecs(final /*@non_null*/ ParsedSpecs x, final Object o) {
    //FIXME hel: what's up here?
    //return visitASTNode(x, o); //generates a stack overflow... but should be used
    return null;
  }

  
  public /*@non_null*/ Object visitQuantifiedExpr(/*@non_null*/ QuantifiedExpr x, Object o) {
    return fTranslator.quantifier(x, o);
  }
  

  
  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitResExpr(escjava.ast.ResExpr, java.lang.Object)
   */
  @Override
  public final Object visitResExpr(final /*@non_null*/ ResExpr x, final Object o) {
    final MethodProperties prop = (MethodProperties) o;
    if (fGlobal.interesting) {
      return this.fTranslator.resultLiteral(x, prop);
    }
   return null;
  }

  

  
  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitSetStmtPragma(escjava.ast.SetStmtPragma, java.lang.Object)
   */
  public final Object visitSetStmtPragma(final /*@non_null*/ SetStmtPragma x, final Object o) {
    final Set.Assignment res = new Set.Assignment();
    if (x.target instanceof VariableAccess) {
      res.var = (QuantVariableRef) x.target.accept(this, o);
      res.expr = (Term) x.value.accept(this, o);
    }
    return res;
  }

  
 

  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitNewInstanceExpr(escjava.ast.NewInstanceExpr, java.lang.Object)
   */
  public /*@non_null*/ Object visitNewInstanceExpr(/*@non_null*/ NewInstanceExpr x, Object o) {
    final String name = Types.printName(x.type);
    return Expression.rvar(name, Type.typeToSort(x.type)); // Ref.sort);
    //return visitExpr(x, o);
  }
  
  public /*@non_null*/ Object visitUnaryExpr(/*@non_null*/ UnaryExpr x, Object o) {
    ((Properties) o).put("unaryOp", (int) x.op);
    return visitExpr(x, o);
  }
  
  
  /* (non-Javadoc)
   * @see javafe.ast.VisitorArgResult#visitBinaryExpr(javafe.ast.BinaryExpr, java.lang.Object)
   */
  public final Object visitBinaryExpr(final /*@non_null*/ BinaryExpr expr, final Object o) {
    if (fGlobal.interesting) {
      switch(expr.op) {
        case TagConstants.EQ: 
          return this.fTranslator.eq(expr, o);
        case TagConstants.OR: 
          return this.fTranslator.or(expr, o);
        case TagConstants.AND: 
          return this.fTranslator.and(expr, o);
        case TagConstants.NE:
          return this.fTranslator.ne(expr, o);
        case TagConstants.GE: 
          return this.fTranslator.ge(expr, o);
        case TagConstants.GT: 
          return this.fTranslator.gt(expr, o);
        case TagConstants.LE: 
          return this.fTranslator.le(expr, o);
        case TagConstants.LT:  
          return this.fTranslator.lt(expr, o);
        case TagConstants.BITOR: 
          return this.fTranslator.bitor(expr, o);
        case TagConstants.BITXOR: 
          return this.fTranslator.bitxor(expr, o);
        case TagConstants.BITAND: 
          return this.fTranslator.bitand(expr, o);
        case TagConstants.LSHIFT:
          return this.fTranslator.lshift(expr, o);
        case TagConstants.RSHIFT: 
          return this.fTranslator.rshift(expr, o);
        case TagConstants.URSHIFT:
          return this.fTranslator.urshift(expr, o);
        case TagConstants.ADD: 
          return this.fTranslator.add(expr, o);
        case TagConstants.SUB: 
          return this.fTranslator.sub(expr, o);
        case TagConstants.DIV: 
          return this.fTranslator.div(expr, o);
        case TagConstants.MOD: 
          return this.fTranslator.mod(expr, o);
        case TagConstants.STAR: 
          return this.fTranslator.star(expr, o);
        case TagConstants.ASSIGN:
          return this.fTranslator.assign(expr, o);
        case TagConstants.ASGMUL: 
          return this.fTranslator.asgmul(expr, o);
        case TagConstants.ASGDIV: 
          return this.fTranslator.asgdiv(expr, o);
        case TagConstants.ASGREM: 
          return this.fTranslator.asgrem(expr, o);
        case TagConstants.ASGADD: 
          return this.fTranslator.asgadd(expr, o);
        case TagConstants.ASGSUB: 
          return this.fTranslator.asgsub(expr, o);
        case TagConstants.ASGLSHIFT: 
          return this.fTranslator.asglshift(expr, o);
        case TagConstants.ASGRSHIFT: 
          return this.fTranslator.asgrshift(expr, o);
        case TagConstants.ASGURSHIFT: 
          return this.fTranslator.asgurshif(expr, o);
        case TagConstants.ASGBITAND: 
          return this.fTranslator.asgbitand(expr, o);
          // jml specific operators 
        case TagConstants.IMPLIES: 
          return this.fTranslator.implies(expr, o);
        case TagConstants.EXPLIES:
          return this.fTranslator.explies(expr, o);
        case TagConstants.IFF: // equivalence (equality)
          return this.fTranslator.iff(expr, o);
        case TagConstants.NIFF:    // discrepance (xor)
          return this.fTranslator.niff(expr, o);
        case TagConstants.SUBTYPE: 
          return this.fTranslator.subtype(expr, o);
        case TagConstants.DOTDOT: 
          return this.fTranslator.dotdot(expr, o);
  
        default:
          throw new IllegalArgumentException("Unknown construct :" +
                                             TagConstants.toString(expr.op) +
                                             " " +  expr);
      }
    } 
    else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see escjava.ast.VisitorArgResult#visitAnOverview(escjava.ast.AnOverview, java.lang.Object)
   */
  public final Object visitAnOverview(final /*@non_null*/ AnOverview x, final Object o) {
    return null;
  }  

  /**
   * @param o Properties object also containing all modifiable types.
   * 
   */
  public void invPredToPreconditions(final /*@non_null*/ Object o) {
     
    final QuantVariableRef x = Expression.rvar(Ref.sort);
    final QuantVariableRef type = Expression.rvar(Type.sort);
    final QuantVariable[] vars = {x.qvar, type.qvar};
    final Term invTerm = Logic.inv(Heap.var, x, type);
    final Term typeOfTerm = Logic.assignCompat(Heap.var, x, type);
    final Term allocTerm = Logic.isAlive(Heap.var, x);
    Term andTerm = Logic.and(allocTerm, typeOfTerm);
    if (((Boolean)((Properties)o).get("isConstructor")).booleanValue()) { 
      Term notEQThis = Logic.not(Logic.equals(x, Ref.varThis));
      andTerm = Logic.and(andTerm, notEQThis);
    }    
    final Term implTerm = Logic.implies(andTerm, invTerm);
    final Term forAllTerm = Logic.forall(vars, implTerm);
    addToPrecondition(forAllTerm, o);
  }


  public Term invPostPred(final /*@non_null*/ Object o) {
    final QuantVariableRef x = Expression.rvar(Ref.sort);
    final QuantVariableRef type = Expression.rvar(Type.sort);
    final QuantVariable[] vars = {x.qvar, type.qvar}; 
    final Term invTerm = Logic.inv(Heap.var, x, type);
    final Term typeOfTerm = Logic.assignCompat(Heap.var, x, type);
    final Term allocTerm = Logic.isAlive(Heap.var, x);
    Term andTerm = Logic.and(allocTerm, typeOfTerm);
    final java.util.Set<Type> visSet = fGlobal.visibleTypeSet;
    if (!visSet.isEmpty()) {
      final Term visibleTerm = Logic.isVisibleIn(type, o);
      andTerm = Logic.and(andTerm, visibleTerm);
    }
    
    final Term implTerm = Logic.implies(andTerm, invTerm);
    final Term forAllTerm = Logic.forall(vars, implTerm);
    return forAllTerm;
  }
  
  
  public void invPredToPostconditions(final /*@non_null*/ Object o) { 
    this.addToPostcondition(this.invPostPred(o), o);
  }

  
  private void invPredToExceptionalPostconditions(Object o) {
    this.addToExceptionalPostcondition(this.invPostPred(o), o);
  }
  

  
  /**
   * @param o containing all field access of the invariant and the class id
   * @return boolean value whether the subset checking of the invariant fields was successfull 
   */
  @SuppressWarnings("unchecked")
  public boolean doSubsetChecking(final Object o) {
    final MethodProperties prop = (MethodProperties) o;
    final java.util.Set<FieldAccess> subSet = (HashSet) prop.get("subsetCheckingSet");
    FieldAccess fa;
    final Identifier parentId = (Identifier) ((Properties)o).get("classId");
    Identifier typeId;
    boolean result = true;
    final Iterator iter = subSet.iterator();
    while (iter.hasNext()) {   
      fa = (FieldAccess)iter.next();
      typeId = fa.decl.parent.id;
      if (!parentId.equals(typeId)) {
        System.out.println("Subset checking: failed! " +
            "The field \"" + fa.id + 
            "\" is a field of class " + typeId + 
            " and not as expected of class " + parentId + "!");  
        result = false;
      }
    }
    prop.put("subsetCheckingSet", new HashSet<FieldAccess>()); //empty set
    return result;
  }
  
  /**
   * Adds a Term to the routines postcondition describing 
   * all assignable variables.
   * @param o Properties object also containing all assignable variables
   */
  public void doAssignable(final Object o) {
    final HashSet assignableSet    = (HashSet) ((Properties) o).get("assignableSet");
    
    if (((Boolean) ((Properties) o).get("nothing")).booleanValue() | !assignableSet.isEmpty())
    {
      Term forAllTerm = null;
      final QuantVariableRef targetVar = Expression.rvar(Ref.sort); 
      final QuantVariableRef fieldVar = Expression.rvar(Ref.sort);
                  // FIXME: Why sortRef is not available?
      final Term equalsTerm = 
          // FIXME jgc: here there is a type mistake fieldVar.qvar 
          // is supposed to be the name of the field, 
          // not a variable ref or fieldVar if you prefer
          //  Logic.equals(Heap.select(Heap.varPre, (Term) targetVar, fieldVar.qvar), 
            //         Heap.select(Heap.var, (Term) targetVar, fieldVar.qvar)); 
                        //gibt noch kein any
        Logic.True();
      final QuantVariable[] vars = {targetVar.qvar, fieldVar.qvar}; 
      Term assigTerm = Logic.not(Logic.isAlive(Heap.varPre, targetVar));
      if (!assignableSet.isEmpty()) {
        assigTerm = Logic.or(assigTerm, 
                             Logic.isAssignable((Term) targetVar, 
                                                fieldVar, o));       
      }
      assigTerm = Logic.or(assigTerm, equalsTerm);
      assigTerm = Logic.implies(Logic.isFieldOf(Heap.var, targetVar, fieldVar), assigTerm);
      forAllTerm = Logic.forall(vars, assigTerm);
      addToPostcondition(forAllTerm, o);
      addToExceptionalPostcondition(forAllTerm, o);
    } 
  }

  
  /**
   * Adds a given Term to preconditions of a given method.
   * @param folTerm to add to preconditions in Lookup hash map
   * @param o Properties object contains the concerning method
   */
  public void addToPrecondition(final Term folTerm, final Object o) {
    if (folTerm != null) {
      final RoutineDecl rd = (RoutineDecl)((Properties) o).get("method");
      Term allPres = Lookup.preconditions.get(rd);
      
      if (allPres == null) {
        allPres = folTerm;
      }
      else {
        allPres = Logic.Safe.and(allPres, folTerm);
      }
      Lookup.preconditions.put(rd, allPres);
    }
  }  
  
  
  /**
   * Adds a given Term to postconditions of a given method. 
   * @param folTerm to add to postconditions in Lookup hash map
   * @param o Properties object contains the concerning method
   */
  public void addToPostcondition(final Term folTerm, final Object o) {
    final MethodProperties prop = (MethodProperties) o;
    if (folTerm != null) {
      final Post folPost = new Post(folTerm);
      final RoutineDecl rd = (RoutineDecl)((Properties) o).get("method");
      Post allPosts = Lookup.postconditions.get(rd);
      
      if (allPosts == null) {
        final QuantVariableRef result = prop.fResult;
        Lookup.postconditions.put(rd, new Post(result, folTerm));
      }
      else {
        allPosts = Post.and(allPosts, folPost);
        Lookup.postconditions.put(rd, allPosts);
      }
    }
  }
  
  
  
  /**
   * Adds a given Term to exceptional postconditions of a given method 
   * @param folTerm to add to exceptional postconditions in Lookup hash map
   * @param o Properties object contains the concerning method
   */
  public void addToExceptionalPostcondition(final Term folTerm, final Object o) {
    if (folTerm != null) {
      Post folPost = new Post(folTerm);
      final RoutineDecl rd = (RoutineDecl)((Properties) o).get("method");
      Post allExPosts = Lookup.exceptionalPostconditions.get(rd);
      
      if ((Boolean) ((Properties)o).get("firstExPost") == Boolean.TRUE) {
        ((Properties)o).put("firstExPost", Boolean.FALSE);
        allExPosts = new Post(allExPosts.getRVar(), folTerm);
      }
      else {
        allExPosts = Post.and(allExPosts, folPost);
      }
      Lookup.exceptionalPostconditions.put(rd, allExPosts);
    }
  } 
  
  

  /**
   * @param x invariant node containing the parents class declaration
   * @param t translated term to conjoin the class invariants
   * @param o object containing the flag for subset checking
   */
  public void addToInv(ExprDeclPragma x, Term t, Object o) {
    if (t != null) {
      boolean invIsValid = true;
      Term invTerm = t;
      final Term allInvs = Lookup.invariants.get(x.parent);
      
      if (((Boolean) ((Properties) o).get("dsc")).booleanValue()) { 
        invIsValid = doSubsetChecking(o);
      }
      if (invIsValid && allInvs != null) {
        invTerm = Logic.and(allInvs, invTerm); 
      }
      else if (invIsValid) {
        Lookup.invariants.put(x.parent, invTerm); 
      }
      else if (!invIsValid) {
        System.out.println("Invariant error (subset check)! The following term was not conjoined to the overall type invariant: " + t.toString() + "\n");
      }
    }
   
  }
  
  

}
