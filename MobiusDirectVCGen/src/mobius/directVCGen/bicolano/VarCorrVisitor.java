package mobius.directVCGen.bicolano;

import java.util.HashMap;
import java.util.Map;

import javafe.ast.FormalParaDecl;
import javafe.ast.RoutineDecl;
import javafe.ast.VarDeclStmt;
import javafe.util.Location;

import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;

import escjava.sortedProver.Lifter.QuantVariableRef;
import escjava.sortedProver.Lifter.Term;

import mobius.directVCGen.formula.Expression;
import mobius.directVCGen.formula.Ref;
import mobius.directVCGen.vcgen.ABasicVisitor;

public class VarCorrVisitor extends ABasicVisitor {
  private final Map<QuantVariableRef, LocalVariableGen> fVariables = 
    new HashMap<QuantVariableRef, LocalVariableGen>();
  /** the currently treated method. */
  private final MethodGen fMet;
  
  
  private VarCorrVisitor(final RoutineDecl decl, 
                            final MethodGen met) {
    final LocalVariableGen[] tab = met.getLocalVariables();
    fMet = met;
    if (tab.length == 0)
      return;
    fVariables.put(Ref.varThis, tab[0]);
    int i = 1;
    
    for (FormalParaDecl para: decl.args.toArray()) {
      fVariables.put(Expression.rvar(para), tab[i]);
      i++;
    }
  }
  public /*@non_null*/ Object visitVarDeclStmt(final /*@non_null*/ VarDeclStmt x, 
                                               final Object o) {
    int i;
    int line = Location.toLineNumber(x.getStartLoc());
    final LocalVariableGen[] tab = fMet.getLocalVariables();
    for (LocalVariableGen var : tab) {
      
      if (var.getName().equals(x.decl.id.toString())) {
        fVariables.put(Expression.rvar(x.decl), var);
      }
        
    }
    return o;
    
  }

  
  public static Map<QuantVariableRef, Term> getVariables(final RoutineDecl decl, 
                                    final MethodGen met) {
    final VarCorrVisitor vis = new VarCorrVisitor(decl, met);
    decl.accept(vis, null);
    
    VarCorrDecoration.inst.set(decl, vis.fVariables);
 
    return VarCorrDecoration.inst.get(decl);
  }
}