package mobius.directVCGen.formula;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javafe.ast.MethodDecl;
import javafe.ast.RoutineDecl;
import mobius.directVCGen.formula.annotation.AAnnotation;
import mobius.directVCGen.formula.jmlTranslator.struct.MethodProperties;
import mobius.directVCGen.vcgen.DirectVCGen;
import mobius.directVCGen.vcgen.struct.ExcpPost;
import mobius.directVCGen.vcgen.struct.Post;
import mobius.directVCGen.vcgen.struct.VCEntry;

import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LineNumberGen;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;

import escjava.sortedProver.Lifter.QuantVariableRef;
import escjava.sortedProver.Lifter.Term;



public class Util {
  
  /**
   * 
   * @param decl
   * @return The name of the Annotations version of the method
   */
  public static String getMethodName(RoutineDecl decl) {
    if (decl instanceof MethodDecl) {
      return decl.parent.id + "Annotations." + decl.id();
    }
    else {
      return decl.parent.id + "Annotations._init_";
    }
  }
  
  public static InstructionHandle findLastInstruction(List<LineNumberGen> list) {
    InstructionHandle baseih = list.get(0).getInstruction();
    for (LineNumberGen lng: list) {
      if (lng.getInstruction().getPosition() <
          baseih.getPosition()) {
        baseih = lng.getInstruction();
      }
    }
    
    InstructionHandle ih = baseih;
    if(ih.getPrev() != null) {
      ih = ih.getPrev();
    }
    // first we find the first goto
    while (!(ih.getInstruction() instanceof GotoInstruction)) {
      System.out.println(ih);
      ih = ih.getNext();
    }
    final GotoInstruction bi =  (GotoInstruction) ih.getInstruction();
    return bi.getTarget();
  }
  
  public static boolean belongs(LocalVariableGen local, List<LineNumberGen> lines) {
    
    for (LineNumberGen line: lines) {
      final int linePc = line.getLineNumber().getStartPC();
      final int localPc = local.getStart().getPosition();
      if ((linePc >= localPc) &&
          (line.getLineNumber().getStartPC() <= localPc + local.getStart().getPosition())) {
        return true;
      }
    }
    return false;
  }
  public static LineNumberGen getLineNumberFromLine(MethodGen met, int lineNum) {
    final LineNumberGen [] tab = met.getLineNumbers();
    if (tab.length == 0) {
      return null;
    }
    LineNumberGen min = tab[0];
    int oldspan = Math.abs(min.getSourceLine() - lineNum);
    
    for (LineNumberGen line: tab) {
      final int span = (Math.abs(line.getSourceLine() - lineNum));
      if (span  > 0) {
        if (span < oldspan) {
          min = line;
          oldspan = span;
        }
      }
    }
    return min;
  }
  public static List<LineNumberGen> getLineNumbers(MethodGen met, int lineNum) {
    final List<LineNumberGen> res = new Vector<LineNumberGen>();
    final LineNumberGen first = Util.getLineNumberFromLine(met, lineNum);
    final LineNumberGen [] tab = met.getLineNumbers();
    
    for (LineNumberGen line: tab) {
      if (line.getLineNumber().getLineNumber() == first.getLineNumber().getLineNumber()) {
        res.add(line);
      }
    }
    return res;
  }
  
  /**
   * @deprecated
   * @param lines
   * @return
   */
  public static List<LocalVariableGen> getValidVariables(MethodGen met, List<LineNumberGen> lines) {
    final List<LocalVariableGen> res = new Vector<LocalVariableGen>();
    final LocalVariableGen[] lvt = met.getLocalVariables();
    int skip = met.getArgumentNames().length; // we skip the n first variables
   
    for (LocalVariableGen local: lvt) {
      if (skip > 0) {
        skip--;
      }
      else if (Util.belongs(local, lines)) {
        
        res.add(local);
      }
    }
    return res;
  }
  
  public static Term getAssertion(RoutineDecl meth, 
                                  AAnnotation annot, 
                                  List<QuantVariableRef> variables) {
    final Term res;
    if (DirectVCGen.fByteCodeTrick) {
      final String methname = Util.getMethodName(meth);
      final Term[] tab = new Term[annot.fArgs.size()];
      int i = 0;
      for (QuantVariableRef qvr: annot.fArgs) {
        tab[i] = qvr;//variables.get(qvr);
        i++;
      }
      
      res = Expression.sym(methname + ".mk_" + annot.fName, tab);
    }
    else {
      res = annot.formula;
    }
    return res;
  }
  // TODO: add comments
  public static Post getExcpPost(final Term typ, final VCEntry vce) {
    Post res = null;
    Post nottypeof = null;
    for (ExcpPost p: vce.lexcpost) {
      final QuantVariableRef var = vce.fExcPost.getRVar();
      final Post typeof = new Post(var, Logic.assignCompat(Heap.var, var, p.type));
      nottypeof = new Post(var, Logic.not(Logic.assignCompat(Heap.var, var, p.type)));
      
      if (Type.isSubClassOrEq(typ, p.type)) {
        
        if (res == null) {
          res = p.post;
          //res = Post.implies(typeof, p.post);
        }
        else {
          res = Post.and(Post.implies(nottypeof, res), p.post);
          //res = Post.and(Post.implies(typeof, p.post), res);
        }
        return res;
      }
      else {

        if (res == null) {
          res = Post.implies(typeof, p.post);
        }
        else {
          res = Post.and(Post.implies(nottypeof, res),
                         Post.implies(typeof, p.post));
        }
      }
    }
    final Post ex = vce.fExcPost;
    res = Post.and(res, Post.implies(nottypeof, ex));
    return res;
  }
  /**
   * This method returns a valid new object (with all the necessary properties)
   * to use while creating a new exception.
   * @param type the type of the exception 
   * @param post the current post condition
   * @return the post condition newly formed 
   */
  public static Term getNewExcpPost(final Term type, final VCEntry post) {
    final Post p = Util.getExcpPost(type, post);
    final QuantVariableRef e = Expression.rvar(Ref.sort);
    final QuantVariableRef heap = Heap.newVar();
    
    return Logic.forall(heap,
             Logic.forall(e,
                          Logic.implies(Heap.newObject(Heap.var, type, heap, e),
                                        p.substWith(e).subst(Heap.var, heap))));
  }
  public static Term mkNewEnv(QuantVariableRef newHeap, final Term post) {
    final Term h = Logic.forall(newHeap, post);
    return h;
  }
  public static Term mkNewEnv(Term post) {
    final QuantVariableRef heap = Heap.newVar();
    return mkNewEnv(heap, post.subst(Heap.var, heap));
  }
  public static Term mkNewEnv(Post post) {
    return mkNewEnv(post.getPost());
  }
  
  public static Term substVarWithVal(final Post fPost, final Term var, final Term val) {
    return fPost.subst(var, val);
  }
  
  public static Term[] getNormalPostconditionArgs(RoutineDecl fMeth) {
    Term[] tab;
    final LinkedList<Term> args = new LinkedList<Term> ();
    args.add(Heap.varPre); 
    for (QuantVariableRef qvr:Lookup.getInst().getPreconditionArgs(fMeth)) {
      if (!qvr.equals(Heap.var)) {
        args.add(Expression.old(qvr));
      }
      else {
        args.add(qvr);
      }
    }
    
    QuantVariableRef qvr = Lookup.getNormalPostcondition(fMeth).getRVar();
    
    if (!isVoid(fMeth)) {
      args.addFirst(Expression.normal(Expression.some(qvr)));
    }
    else {
      args.addFirst(Expression.normal(Expression.none()));
    }
    tab = args.toArray(new Term [args.size()]);
    return tab;
  }

  public static boolean isVoid(RoutineDecl fMeth) {
    if (fMeth instanceof MethodDecl) {
      final MethodDecl md = (MethodDecl) fMeth;
      return javafe.tc.Types.isVoidType(md.returnType);
      
    }
    else {
      return true;
    }
  }
  public static Term[] getExcPostconditionArgs(RoutineDecl fMeth) {
    final Term[] tab = getNormalPostconditionArgs(fMeth);
    tab[0] = Expression.sym("Exception", 
                           new Term [] {Lookup.getExceptionalPostcondition(fMeth).getRVar()});
    return tab;
  }
  
  public static List<QuantVariableRef> buildArgs(final MethodProperties prop) {
    final List<QuantVariableRef> args = new LinkedList<QuantVariableRef>();
    // olds
    
    for (QuantVariableRef qvr: prop.fArgs) {
      if (qvr.qvar.name.equals("this")) {
        continue;
      }
      args.add(Expression.old(qvr));  
    }
    
    // new :)
    args.addAll(prop.fArgs);
    args.addAll(prop.getLocalVars());
    return args;
  }
}
