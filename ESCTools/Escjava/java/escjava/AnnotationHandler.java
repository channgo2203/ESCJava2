// This class is generated as part os the 2003 Revision of the ESC Tools
// Author: David Cok


package escjava;

import javafe.ast.*;
import javafe.util.ErrorSet;
import javafe.util.Location;
import escjava.ast.*;
import escjava.ast.TagConstants;
import escjava.ast.Modifiers;
import escjava.translate.GetSpec;
import escjava.tc.FlowInsensitiveChecks;
import javafe.tc.Types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.LinkedList;

/** This class handles the desugaring of annotations.

*/
public class AnnotationHandler {

    public AnnotationHandler() {}

    protected TypeDecl td = null;

    /** This must be called on a compilation unit to get the model imports
	listed, so that type names used in annotations can be found, and to
	get model methods put into the class's signature.
	It is called as part of EscSrcReader, a subclass of SrcReader, 
	defined in EscTypeReader.
    */
    public void handlePragmas(CompilationUnit cu) {
	if (cu == null) return;
	// move any model imports into the list of imports
	for (int i = 0; i < cu.lexicalPragmas.size(); ++i) {
		LexicalPragma p = cu.lexicalPragmas.elementAt(i);
		if (p instanceof ImportPragma) 
			cu.imports.addElement(((ImportPragma)p).decl);
	}

	TypeDeclVec elems = cu.elems;
	for (int i=0; i<elems.size(); ++i) {
	    TypeDecl td = elems.elementAt(i);
	    handleTypeDecl(td);
	}
    }

    /** After parsing, but before type checking, we need to convert model
	methods to regular methods, so that
	names are resolved correctly; also need to set ACC_PURE bits correctly
	in all classes so that later checks get done correctly.
    */ // FIXME - possibly should put these in GhostEnv??
    public void handleTypeDecl(TypeDecl td) {
	handlePragmas(td);
	for (int j=0; j<td.elems.size(); ++j) {
	    TypeDeclElem tde = td.elems.elementAt(j);
	    // Handle nested types
	    if (tde instanceof TypeDecl) {
		handleTypeDecl((TypeDecl)tde);
	    }
	    // move any model methods into the list of methods
	    if (tde instanceof ModelMethodDeclPragma) {
		handlePragmas(tde);
		ModelMethodDeclPragma mmp = (ModelMethodDeclPragma)tde;
		td.elems.setElementAt(((ModelMethodDeclPragma)tde).decl,j);
	    }
	    if (tde instanceof ModelConstructorDeclPragma) {
		handlePragmas(tde);
		ModelConstructorDeclPragma mmp = (ModelConstructorDeclPragma)tde;
		if (mmp.id == null) {
		   // An error reported already - improper name cf. EscPragmaParser
		} else if (mmp.id.id != td.id) {
		    ErrorSet.error(mmp.id.getStartLoc(),"A constructor-like declaration has an id which is not the same as the id of the enclosing type: " + mmp.id.id + " vs. " + td.id, td.locId);
		} else {
		    td.elems.setElementAt(((ModelConstructorDeclPragma)tde).decl,j);
		}
	    }
	    // handle PURE pragmas
	    if (tde instanceof MethodDecl ||
		tde instanceof ConstructorDecl) {
		handlePragmas(tde);
	    }
	}
    }

    public void handlePragmas(TypeDeclElem tde) {
	ModifierPragmaVec mpp = 
	    (tde instanceof TypeDecl) ? ((TypeDecl)tde).pmodifiers:
	    (tde instanceof RoutineDecl) ? ((RoutineDecl)tde).pmodifiers :
	    null;

	if (mpp != null) for (int i=0; i<mpp.size(); ++i) {
	    ModifierPragma m = mpp.elementAt(i);
	    if (m.getTag() == TagConstants.PURE) {
		tde.setModifiers(tde.getModifiers() | Modifiers.ACC_PURE);
	    }
	}
    }

    //-----------------------------------------------------------------------
/*
    public void process(TypeDecl td) {
	this.td = td;

	for (int i=0; i<td.elems.size(); ++i) {
	    TypeDeclElem tde = td.elems.elementAt(i);
	    process(tde);
        }
    }
*/

    public void process(TypeDeclElem tde) {
	int tag = tde.getTag();
	switch (tag) {
// What about initially, monitored_by, readable_if clauses ??? FIXME
// What about nested classes ??? FIXME
// What about redundant clauses ??? FIXME


	    case TagConstants.CONSTRUCTORDECL:
	    case TagConstants.METHODDECL:
		process((RoutineDecl)tde);
		break;

	    case TagConstants.FIELDDECL:
		break;

	    case TagConstants.GHOSTDECLPRAGMA:
	    case TagConstants.MODELDECLPRAGMA:
	    case TagConstants.INVARIANT:
	    case TagConstants.INVARIANT_REDUNDANTLY:
	    case TagConstants.CONSTRAINT:
	    case TagConstants.REPRESENTS:
	    case TagConstants.AXIOM:
	    case TagConstants.DEPENDS:
		(new CheckPurity()).visitNode((ASTNode)tde);
		break;

	    default:
		//System.out.println("TAG " + tag + " " + TagConstants.toString(tag) + " " + tde.getClass() );
	}

    }

    protected void process(RoutineDecl tde) {
	ModifierPragmaVec pmodifiers = tde.pmodifiers;
	//System.out.println("   Mods " + Modifiers.toString(tde.modifiers));
	if (pmodifiers != null) {
	    for (int i = 0; i<pmodifiers.size(); ++i) {
		ModifierPragma mp = pmodifiers.elementAt(i);
		(new CheckPurity()).visitNode((ASTNode)mp);
	    }
	}
    }

    //-----------------------------------------------------------------------
    // Desugaring is done as a last stage of type-checking.  The desugar
    // methods below may presume that all expressions are type-checked.
    // As a result, any constructed expressions must have type information
    // inserted.

    public void desugar(RoutineDecl tde) {
	if ((tde.modifiers & Modifiers.ACC_DESUGARED) != 0) return;

	// Now desugar this routine itself

	ModifierPragmaVec pmodifiers = tde.pmodifiers;
	Identifier id =
		tde instanceof MethodDecl ?
			((MethodDecl)tde).id
		: tde.getParent().id;
	javafe.util.Info.out("Desugaring specifications for " + tde.parent.id + "." + id);
	try { // Just for safety's sake
	    tde.pmodifiers = desugarAnnotations(pmodifiers,tde);
	} catch (Exception e) {
	    tde.pmodifiers = ModifierPragmaVec.make();
	    ErrorSet.error(tde.getStartLoc(),
		"Internal error while desugaring annotations: " + e);
	    e.printStackTrace();
	}
	tde.modifiers |=  Modifiers.ACC_DESUGARED;

	if (Main.options().desugaredSpecs) {
	  System.out.println("Desugared specifications for " + tde.parent.id + "." + id);
	    printSpecs(tde);
	}
    }
    static public void printSpecs(RoutineDecl tde) {
	  if (tde.pmodifiers != null)
	   for (int i = 0; i<tde.pmodifiers.size(); ++i) {
	    ModifierPragma mp = tde.pmodifiers.elementAt(i);
		printSpec(mp);
	   }
    }
    static public void printSpec(ModifierPragma mp) {
	    System.out.print("   " + 
		escjava.ast.TagConstants.toString(mp.getTag()) + " "  );
	    if (mp instanceof ExprModifierPragma) {
		ExprModifierPragma mpe = (ExprModifierPragma)mp;
		print(mpe.expr);
	    } else if (mp instanceof CondExprModifierPragma) {
		CondExprModifierPragma mpe = (CondExprModifierPragma)mp;
		print(mpe.expr);
		if (mpe.cond != null) {
		    System.out.print(" if ");
		    print(mpe.cond);
		}
	    } else if (mp instanceof VarExprModifierPragma) {
		VarExprModifierPragma mpe = (VarExprModifierPragma)mp;
		System.out.print("(" + Types.toClassTypeSig(mpe.arg.type).getExternalName()
		    + (mpe.arg.id == TagConstants.ExsuresIdnName ? "" :
			" " + mpe.arg.id.toString()) + ")");
		print(mpe.expr);
	    }
	    System.out.println("");
    }

    protected ModifierPragmaVec desugarAnnotations(ModifierPragmaVec pm,
					    RoutineDecl tde) {
	if (pm == null) {
	    pm = ModifierPragmaVec.make();
	}

	ModifierPragmaVec newpm = ModifierPragmaVec.make();

	boolean isPure = escjava.tc.FlowInsensitiveChecks.isPure(tde);
	boolean isConstructor = tde instanceof ConstructorDecl;

	// Get non_null specs
	ModifierPragmaVec nonnullBehavior = getNonNull(tde);

	javafe.util.Set overrideSet = null;
	if (!isConstructor) overrideSet = escjava.tc.FlowInsensitiveChecks.getDirectOverrides((MethodDecl)tde);
	boolean overrides = !isConstructor && !overrideSet.isEmpty();
	

	if (!overrides && nonnullBehavior.size()==0) {
	    // Add a default 'requires true' clause if there are no
	    // specs at all and the routine is not overriding anything
//if (tde instanceof MethodDecl)
//System.out.println("QQQ " +  ((MethodDecl)tde).id + " " + overrides + " " + nonnullBehavior.size() + " " + pm.size());
	    boolean doit = pm.size() == 0;
	    if (!doit) {
		// Need to determine if there are any clause specs
		doit = true;
		ModifierPragma mpp = pm.elementAt(pm.size()-1);
		if (mpp instanceof ParsedSpecs) {
//System.out.println("QRR " + ((ParsedSpecs)mpp).specs.specs.size());
		    doit = ((ParsedSpecs)mpp).specs.specs.size() == 0;
		}
		else doit = false;
// FIXME - why do we get ExprModifierPragmas here (e.g. test8)
//System.out.println("QT " + mpp.getClass());
	    }
	    if (doit) {
		ExprModifierPragma e = ExprModifierPragma.make(
			TagConstants.REQUIRES, T, Location.NULL);
		newpm.addElement(e);
		newpm.addElement(defaultModifies(Location.NULL,T));
	    }
	}


	RoutineDecl previousDecl = null;
	int pos = 0;
	while (pos < pm.size()) {
	    ModifierPragma p = pm.elementAt(pos++);
	    if (p.getTag() == TagConstants.PARSEDSPECS) {
		ParsedSpecs ps = (ParsedSpecs)p;
		previousDecl = ps.decl;
		if (overrides && ps.specs.initialAlso == null && ps.specs.specs.size() != 0) {
		    ErrorSet.caution(ps.getStartLoc(),"JML requires a specification to begin with 'also' when the method overrides other methods" ,((MethodDecl)overrideSet.elements().nextElement()).locType);
		}
		if (!overrides && ps.specs.initialAlso != null) {
		    ErrorSet.caution(ps.specs.initialAlso.getStartLoc(),
			"No initial also expected since there are no overridden or refined methods");
		}
		break;
	    }
	}
	while (pos < pm.size()) {
	    ModifierPragma p = pm.elementAt(pos++);
	    if (p.getTag() == TagConstants.PARSEDSPECS) {
		ParsedSpecs ps = (ParsedSpecs)p;
		if (ps.specs.initialAlso == null && ps.specs.specs.size() != 0) {
		    ErrorSet.caution(ps.getStartLoc(),
			"JML requires a specification to begin with 'also' when the declaration refines a previous declaration",previousDecl.locId);
		}
		previousDecl = ps.decl;
	    }
	}


	ParsedRoutineSpecs accumulatedSpecs = new ParsedRoutineSpecs();
	pos = 0;
	while (pos < pm.size()) {
	    ModifierPragma p = pm.elementAt(pos++);
	    if (p.getTag() == TagConstants.PARSEDSPECS) {
		ParsedRoutineSpecs ps = ((ParsedSpecs)p).specs;
		ParsedRoutineSpecs newps = new ParsedRoutineSpecs();
		deNest(ps.specs,nonnullBehavior,newps.specs);
		deNest(ps.impliesThat,nonnullBehavior, newps.impliesThat);
		deNest(ps.examples,nonnullBehavior, newps.examples);
		accumulatedSpecs.specs.addAll(newps.specs);
		accumulatedSpecs.impliesThat.addAll(newps.impliesThat);
		accumulatedSpecs.examples.addAll(newps.examples);
	    } else {
		newpm.addElement(p);
	    }
	}
	ModifierPragmaVec r = desugar(accumulatedSpecs.specs);
	// accumulatedSpecs.impliesThat = desugar(accumulatedSpecs.impliesThat);
	// accumulatedSpecs.examples = desugar(accumulatedSpecs.examples); // FIXME - not doing this because we are not doing anything with the result.
	newpm.append(r);
	return newpm;
/*
	do {

	pos = deNest(false,pos,pm,results,nonnullBehavior,isPure,isConstructor);
	if (pm.elementAt(pos).getTag() == TagConstants.SUBCLASSING_CONTRACT) {
	    ++pos;
	    pos = sc_section(pos,pm,accumulatedBehavior.subclassingContracts);
	}
	if (pm.elementAt(pos).getTag() == TagConstants.IMPLIES_THAT) {
	    ++pos;
	    pos = deNest(false,pos,pm,accumulatedBehavior.implications,new Behavior(),isPure,isConstructor);
	}
	if (pm.elementAt(pos).getTag() == TagConstants.FOR_EXAMPLE) {
	    ++pos;
	    pos = deNest(true,pos,pm,accumulatedBehavior.examples,new Behavior(),isPure,isConstructor);
	}
	if (pm.elementAt(pos).getTag() == TagConstants.ALSO_REFINE) {
	    ++pos;
	    continue;
	}
	while (pm.elementAt(pos).getTag() != TagConstants.END) {
	    ErrorSet.error(pm.elementAt(pos).getStartLoc(),
				"Out of place annotation");
	    ++pos;
	}
	break;
	} while(true);
*/
/*	
	// Now have to further desugar the annotations that Escjava uses
	// FIXME - adding model programs may require altering this loop
	// Always do the incorporation of the precondition into the
	// postconditions - otherwise desugaring of inheritance does not
	// work soundly.
	{
	    boolean defaultIsModifiesNothing =
		isConstructor && ( ((RoutineDecl)tde).implicit ||
		    javafe.tc.TypeSig.getSig(tde.parent) == javafe.tc.Types.javaLangObject())
		 || escjava.tc.FlowInsensitiveChecks.isPure(tde) ;

	    ArrayList orList = new ArrayList(); // Set of spec-cases to be
				// ored together to form the final clause
	    for (Iterator ii = results.iterator(); ii.hasNext();) {
		Object o = ii.next();
		Behavior b = (Behavior)o;
		b.defaultIsModifiesNothing = defaultIsModifiesNothing;
		accumulatedBehavior.combine(b,orList);
	    }
	    if (orList.isEmpty()) {
		// No groups
		// This should not happen
		accumulatedBehavior.requires = null;
	    } else {
		// Multiple spec cases - have to combine them
		ArrayList newList = new ArrayList();
		boolean trueItem = false;
		Iterator i = orList.iterator();
		while (i.hasNext()) {
		    ArrayList a = (ArrayList)i.next();
		    ExprModifierPragma e = andLabeled(a);
		    if (e != null) newList.add(e);
		    else trueItem = true;
		}
		accumulatedBehavior.requires = new ArrayList();
		if (!trueItem) {
		    ExprModifierPragma ee = or(newList);
		    accumulatedBehavior.requires.add(ee);
		} else {
		    // Need to make a default true requires clause so that
		    // the translation to a VC works correctly
		    ExprModifierPragma ee = 
			ExprModifierPragma.make(TagConstants.REQUIRES,
				Behavior.T, Location.NULL);
		    accumulatedBehavior.requires.add(ee);
		}
	    }
	}

	// End

	if (accumulatedBehavior.requires != null) 
	    newpm.addAll(accumulatedBehavior.requires);

	Iterator ii = accumulatedBehavior.modifies.iterator();
	while (ii.hasNext()) {
	    CondExprModifierPragma e = (CondExprModifierPragma)ii.next();
	    newpm.add(e);
	}
	ii = accumulatedBehavior.ensures.iterator();
	while (ii.hasNext()) {
	    ExprModifierPragma e = (ExprModifierPragma)ii.next();
	    if (e.expr != Behavior.T) newpm.add(e);
	}
	ii = accumulatedBehavior.when.iterator();
	while (ii.hasNext()) {
	    ExprModifierPragma e = (ExprModifierPragma)ii.next();
	    if (e.expr != Behavior.T) newpm.add(e);
	}
	ii = accumulatedBehavior.diverges.iterator();
	while (ii.hasNext()) {
	    ExprModifierPragma e = (ExprModifierPragma)ii.next();
	    if (e.expr != Behavior.T) newpm.add(e);
	}
	ii = accumulatedBehavior.signals.iterator();
	while (ii.hasNext()) {
	    VarExprModifierPragma e = (VarExprModifierPragma)ii.next();
	    newpm.add(e);
	}
	ii = accumulatedBehavior.extras.iterator();
	while (ii.hasNext()) {
	    ModifierPragma e = (ModifierPragma)ii.next();
	    newpm.add(e);
	}


	ModifierPragma[] out = new ModifierPragma[newpm.size()];
	return ModifierPragmaVec.make((ModifierPragma[])(newpm.toArray(out)));
*/
    }

	// NOTE: If we do desugaring after typechecking, we need to put in 
	// all of the types for the expressions we construct.  If we do
	// desugaring before typechecking, we do not, or we risk not 
	// checking any of the details of that expression. Currently
	// desugaring comes first. 

    public ModifierPragmaVec getNonNull(RoutineDecl rd) {
	ModifierPragmaVec result = ModifierPragmaVec.make(2);
	FormalParaDeclVec args = rd.args;

	// Check that non_null on parameters is allowed
	if (rd instanceof MethodDecl) {
	    MethodDecl md = (MethodDecl)rd;
	    // Need to check all overrides, because we may not have processed a
	    // given direct override yet, removing its spurious non_null
	    javafe.util.Set overrides = FlowInsensitiveChecks.getAllOverrides(md);
	    if (overrides != null && !overrides.isEmpty()) {
		for (int i=0; i<args.size(); ++i) {
		    FormalParaDecl arg = args.elementAt(i);
		    ModifierPragma m = GetSpec.findModifierPragma(arg,TagConstants.NON_NULL);
		    if (m != null) { // overriding method has non_null for parameter i
			MethodDecl smd = FlowInsensitiveChecks.getSuperNonNullStatus(md,i,overrides);
			if (smd != null) { // overridden method does not have non_null for i
			    FormalParaDecl sf = smd.args.elementAt(i);
			    ErrorSet.caution(m.getStartLoc(),
				    "The non_null annotation is ignored because this method overrides a method declaration in which this parameter is not declared non_null: ",sf.getStartLoc());
			    GetSpec.removeModifierPragma(arg,TagConstants.NON_NULL);
			}
		    }
		}
	    }
	}

	// Handle non_null on any parameter
	for (int i=0; i<args.size(); ++i) {
	    FormalParaDecl arg = args.elementAt(i);
	    ModifierPragma m = findModifierPragma(arg.pmodifiers,TagConstants.NON_NULL);
	    if (m == null) continue;
	    int locNN = m.getStartLoc();
	    result.addElement(
		ExprModifierPragma.make(TagConstants.REQUIRES,
			NonNullExpr.make(arg,locNN),
			locNN)
		);
	}

	// Handle non_null on the result
	ModifierPragma m = findModifierPragma(rd.pmodifiers,TagConstants.NON_NULL);
	if (m != null) {
	    int locNN = m.getStartLoc();
	    Expr r = ResExpr.make(locNN);
	    javafe.tc.FlowInsensitiveChecks.setType(r, ((MethodDecl)rd).returnType);
	    Expr n = LiteralExpr.make(TagConstants.NULLLIT, null, locNN);
	    javafe.tc.FlowInsensitiveChecks.setType(n, Types.nullType);
	    Expr e = BinaryExpr.make(TagConstants.NE, r, n, locNN);
	    javafe.tc.FlowInsensitiveChecks.setType(e, Types.booleanType);
	    ExprModifierPragma emp = ExprModifierPragma.make(TagConstants.ENSURES, e, locNN);
	    emp.errorTag = TagConstants.CHKNONNULLRESULT;
	    result.addElement(emp);
	}
	return result;
    }

/*
public static void psprint(ArrayList ps) {
	if (ps == null) {System.out.println("PS IS NULL"); return;}
System.out.println("START PS " + ps.size());
Iterator i = ps.iterator();
while (i.hasNext()) {
	Object o = i.next();
	System.out.println("ELEM " + o.getClass());
	if (o instanceof ModifierPragmaVec) psprint((ModifierPragmaVec)o);
}
System.out.println("END PS");
}

public static void psprint(ModifierPragmaVec mp) {
if (mp == null) { System.out.println("MPV IS NULL"); return; }
System.out.println("MPV " + mp.size());
for (int i=0; i<mp.size(); ++i) {
ModifierPragma m = mp.elementAt(i);
System.out.println("MPV-ELEM " + TagConstants.toString(m.getTag()));
if (m instanceof ParsedSpecs) {
	ArrayList a = ((ParsedSpecs)m).specs.specs;
	psprint(a);
}
}
System.out.println("END_MPV");
}
*/

	// Argument is an ArrayList of ModifierPragmaVec corresponding to
	// also-connected de-nested specification cases
	// result is a single ModifierPragmaVec with all the requires
	// clauses combined and all the other clauses guarded by the 
	// relevant precondition
    public ModifierPragmaVec desugar(ArrayList ps) {
	ArrayList requiresList = new ArrayList();
	ModifierPragmaVec resultList = ModifierPragmaVec.make();
	resultList.addElement(null); // replaced below
	Iterator i = ps.iterator();
	while (i.hasNext()) {
	    ModifierPragmaVec m = (ModifierPragmaVec)i.next();
	    desugar(m,requiresList,resultList);
	}
	// combine all of the requires
	ExprModifierPragma requires = or(requiresList);
	resultList.setElementAt(requires,0);
	if (requires == null) resultList.removeElementAt(0);
	return resultList;
    }

	// requiresList is an ArrayList of ModifierPragma
    public void desugar(ModifierPragmaVec m,ArrayList requiresList,
				ModifierPragmaVec resultList){
	GenericVarDeclVec foralls = GenericVarDeclVec.make();
	// First collect all the requires clauses together
	int pos = 0;
	ArrayList list = new ArrayList();
	while (pos < m.size()) {
	    ModifierPragma mp = m.elementAt(pos++);
	    int tag = mp.getTag();
	    if (tag == TagConstants.NO_WACK_FORALL) foralls.addElement(
			((VarDeclModifierPragma)mp).decl );
	    if (tag != TagConstants.REQUIRES &&
		tag != TagConstants.PRECONDITION) continue;
	    if (((ExprModifierPragma)mp).expr.getTag() 
			== TagConstants.NOTSPECIFIEDEXPR) continue;
	    list.add(forallWrap(foralls,mp));
	}
	ExprModifierPragma conjunction = and(list);
	boolean reqIsTrue = conjunction == null || isTrue(conjunction.expr);
	boolean reqIsFalse = conjunction != null && isFalse(conjunction.expr);
	Expr reqexpr = conjunction==null? null : conjunction.expr;
//System.out.println("REQ " + reqexpr);
	Expr req = T;
	if (reqexpr != null) {
	    ExprVec arg = ExprVec.make(new Expr[]{reqexpr});
	    //req = UnaryExpr.make(TagConstants.PRE, reqexpr, Location.NULL);

	    req = NaryExpr.make(Location.NULL,
				reqexpr.getStartLoc(),TagConstants.PRE,
				Identifier.intern("\\old"),arg);
	    javafe.tc.FlowInsensitiveChecks.setType(req, Types.booleanType);
	}

	if (reqIsTrue && m.size() == 0) return;

	requiresList.add(reqIsTrue? 
		ExprModifierPragma.make(TagConstants.REQUIRES,T,Location.NULL) 
		: andLabeled(list)); 

	// Now transform each non-requires pragma
	boolean foundDiverges = false;
	boolean foundModifies = false;
	pos = 0;
	while (pos < m.size()) {
	    ModifierPragma mp = m.elementAt(pos++);
	    int tag = mp.getTag();
	    if (tag == TagConstants.REQUIRES ||
		tag == TagConstants.PRECONDITION) continue;
	    switch (tag) {
		case TagConstants.DIVERGES:
		    foundDiverges = true;
		    // fall-through
		case TagConstants.ENSURES:
		case TagConstants.POSTCONDITION:
		case TagConstants.WHEN:
		{
		    ExprModifierPragma mm = (ExprModifierPragma)mp;
		    if (mm.expr.getTag() == TagConstants.NOTSPECIFIEDEXPR)
			break;
		    if (!reqIsTrue) mm.expr = implies(req,mm.expr);
		    resultList.addElement(mm);
		    break;
		}

		case TagConstants.SIGNALS:
		case TagConstants.EXSURES:
		{
		    VarExprModifierPragma mm = (VarExprModifierPragma)mp;
		    if (mm.expr.getTag() == TagConstants.NOTSPECIFIEDEXPR)
			break;
		    if (!reqIsTrue) mm.expr = implies(req,mm.expr);
		    resultList.addElement(mm);
		    break;
		}
		case TagConstants.MODIFIES:
		case TagConstants.MODIFIABLE:
		case TagConstants.ASSIGNABLE:
		    foundModifies = true;
		    // fall-through
		case TagConstants.WORKING_SPACE:
		case TagConstants.DURATION:
		  {
		    CondExprModifierPragma mm = (CondExprModifierPragma)mp;
		    if (mm.expr != null &&
		        mm.expr.getTag() == TagConstants.NOTSPECIFIEDEXPR)
			break;
		    mm.cond = and(mm.cond,req);
		    resultList.addElement(mm);
		    break;
		  }

		case TagConstants.ACCESSIBLE:
		case TagConstants.CALLABLE:
		case TagConstants.MEASURED_BY:
		case TagConstants.MODEL_PROGRAM:
			// Remember to skip if not specified
			// FIXME - not yet handled
		    break;

		case TagConstants.NO_WACK_FORALL:
		case TagConstants.OLD:
		    // These are handled elsewhere and don't get put into
		    // the pragma list.
		    break;

		case TagConstants.MONITORED_BY:
		    ErrorSet.error(mp.getStartLoc(),"monitored_by is obsolete and only applies to fields");
		    break;

		case TagConstants.MONITORED:
		    ErrorSet.error(mp.getStartLoc(),"monitored only applies to fields");
		    break;

		default:
		    ErrorSet.error(mp.getStartLoc(),"Unknown kind of pragma for a routine declaration: " + TagConstants.toString(tag));
		    break;
	    }
	}
	if (!foundDiverges) {
	    // lightweight default - req ==> true which is true
	    // heavyweight default - req ==> false which is !req
	    // The lightweight default need not be added since it does
	    // not need any verification.
/* FIXME - don't need this for now, but need to be sure that when it is 
added, it doesn't change whether a routine appears to have a spec or not.
	    resultList.addElement(ExprModifierPragma.make(
		TagConstants.DIVERGES,implies(req,AnnotationHandler.F),
			Location.NULL));
*/
// FIXME - Null location above and below needs to be fixed.
// Also other use of defaultModifies
// Diverges expression depends on lightweight or heavyweight
	}
	if (!foundModifies) {
	    resultList.addElement(defaultModifies(Location.NULL,req));
	}
    }
    public final static CondExprModifierPragma defaultModifies(int loc, 
				Expr req) {
	boolean nothing = true;
	return CondExprModifierPragma.make(
		TagConstants.MODIFIES,
		nothing ? (Expr)NothingExpr.make(loc) :
			    (Expr)EverythingExpr.make(loc),
		loc,req);
    }


    public ModifierPragma forallWrap(GenericVarDeclVec foralls, 
					ModifierPragma mp) {
	if (mp instanceof ExprModifierPragma) {
		((ExprModifierPragma)mp).expr = 
			forallWrap(foralls, ((ExprModifierPragma)mp).expr) ;
	}
	return mp;
    }

    public Expr forallWrap(GenericVarDeclVec foralls, Expr e) {
	if (foralls.size() == 0) return e;
	int loc = foralls.elementAt(0).getStartLoc();
	int endLoc = foralls.elementAt(foralls.size()-1).getStartLoc();
	return QuantifiedExpr.make(loc,endLoc,TagConstants.FORALL,
		foralls,e,null);
    }

    public void deNest(ArrayList ps, ModifierPragmaVec prefix, ArrayList deNestedSpecs) {
	if (ps.size() == 0 && prefix.size() != 0) {
	    deNestedSpecs.add(prefix);
	} else {
	    Iterator i = ps.iterator();
	    while (i.hasNext()) {
		ModifierPragmaVec m = (ModifierPragmaVec)i.next();
		deNest(m,prefix,deNestedSpecs);
	    }
	}
    }

    //@ requires (* m.size() > 0 *);
    // Uses the fact that if there is a nesting it is the last element of
    // the ModifierPragmaVec
    public void deNest(ModifierPragmaVec m, ModifierPragmaVec prefix, ArrayList deNestedSpecs) {
	ModifierPragma last = m.elementAt(m.size()-1);
	if (last instanceof NestedModifierPragma) {
	    m.removeElementAt(m.size()-1);
	    ModifierPragmaVec newprefix = prefix.copy();
	    newprefix.append(m);
	    m.addElement(last);
	    ArrayList list = ((NestedModifierPragma)last).list;
	    deNest(list,newprefix,deNestedSpecs);
	} else {
	    ModifierPragmaVec mm = prefix.copy();
	    mm.append(m);
	    deNestedSpecs.add(mm);
	}
    }
/*
    public int deNest(boolean exampleMode, int pos, ModifierPragmaVec pm, ArrayList results, Behavior cb, boolean isPure, boolean isConstructor) {
	Behavior currentBehavior = cb.copy(); // new Behavior();
	LinkedList commonBehavior = new LinkedList();
	ModifierPragma m = null;
	boolean terminate = false;
	while (!terminate) {
	    m = pm.elementAt(pos++);
	    int t = m.getTag();
	    //System.out.println("GOT TAG " + TagConstants.toString(t));
	    switch (t) {
		case TagConstants.BEHAVIOR:
		    if (exampleMode)
			ErrorSet.error("Behavior keyword should not be used in examples section - use example");
		    currentBehavior = cb.copy(); //new Behavior();
		    currentBehavior.isLightweight = false;
		    break;
		case TagConstants.EXAMPLE:
		    if (!exampleMode)
			ErrorSet.error("Example keyword should not be used outside the examples section - use behavior");
		    currentBehavior = new Behavior();
		    currentBehavior.isLightweight = false;
		    break;

		case TagConstants.NORMAL_BEHAVIOR:
		    if (exampleMode)
			ErrorSet.error("normal_behavior keyword should not be used in examples section - use normal_example");
		    currentBehavior = cb.copy(); //new Behavior();
		    currentBehavior.isLightweight = false;
		    currentBehavior.isNormal = true;
		    // set a false signals clause
		    currentBehavior.signals.add(Behavior.defaultSignalFalse(
				m.getStartLoc()));
		    break;

		case TagConstants.NORMAL_EXAMPLE:
		    if (!exampleMode)
			ErrorSet.error("normal_example keyword should not be used outside the examples section - use normal_behavior");
		    currentBehavior = new Behavior();
		    currentBehavior.isLightweight = false;
		    currentBehavior.isNormal = true;
		    // set a false signals clause
		    currentBehavior.signals.add(Behavior.defaultSignalFalse(
				m.getStartLoc()));
		    break;

		case TagConstants.EXCEPTIONAL_BEHAVIOR:
		    if (exampleMode)
			ErrorSet.error("exceptional_behavior keyword should not be used in examples section - use exceptional_example");
		    currentBehavior = cb.copy(); // new Behavior();
		    currentBehavior.isLightweight = false;
		    currentBehavior.isExceptional = true;
		    // set a false ensures clause
		    currentBehavior.ensures.add(Behavior.ensuresFalse(m.getStartLoc()));
		    break;

		case TagConstants.EXCEPTIONAL_EXAMPLE:
		    if (!exampleMode)
			ErrorSet.error("exceptional_example keyword should not be used outside the examples section - use exceptional_behavior");
		    currentBehavior = new Behavior();
		    currentBehavior.isLightweight = false;
		    currentBehavior.isExceptional = true;
		    // set a false ensures clause
		    currentBehavior.ensures.add(Behavior.ensuresFalse(m.getStartLoc()));
		    break;

                // All redundant tokens should not exist in the AST
                // anymore; they are represented with redundant fields in
                // the AST nodes.
		case TagConstants.DIVERGES_REDUNDANTLY:
		case TagConstants.ENSURES_REDUNDANTLY:
		case TagConstants.EXSURES_REDUNDANTLY:
		case TagConstants.REQUIRES_REDUNDANTLY:
		case TagConstants.SIGNALS_REDUNDANTLY:
                    assert false : "Redundant keywords should not be in AST!";
                    break;

		case TagConstants.REQUIRES:
		case TagConstants.ALSO_REQUIRES:
		case TagConstants.PRECONDITION: {
		    if (currentBehavior == null) {
			ErrorSet.error(m.getStartLoc(),"Missing also");
			if (commonBehavior.isEmpty()) {
			    currentBehavior = cb.copy(); // new Behavior();
			} else {
			    currentBehavior = ((Behavior)commonBehavior.getFirst()).copy();
			}
		    }
		    ExprModifierPragma e = (ExprModifierPragma)m;
		    if (e.expr.getTag() != TagConstants.NOTSPECIFIEDEXPR)
			currentBehavior.requires.add(e);
		    break;
		}
		    
		case TagConstants.ENSURES:
		case TagConstants.ALSO_ENSURES:
		case TagConstants.POSTCONDITION: {
		    if (currentBehavior == null) {
			ErrorSet.error(m.getStartLoc(),"Missing also");
			if (commonBehavior.isEmpty()) {
			    currentBehavior = cb.copy(); // new Behavior();
			} else {
			    currentBehavior = ((Behavior)commonBehavior.getFirst()).copy();
			}
		    }
		    if (currentBehavior.isExceptional) {
			ErrorSet.error(m.getStartLoc(),
			   "This type of annotation is not permitted in an excpetional_behavior clause");
		    }
		    ExprModifierPragma e = (ExprModifierPragma)m;
		    if (e.expr.getTag() != TagConstants.NOTSPECIFIEDEXPR)
			currentBehavior.ensures.add(e);
		    break;
		 }

		case TagConstants.DIVERGES:
		    if (currentBehavior == null) {
			ErrorSet.error(m.getStartLoc(),"Missing also");
			if (commonBehavior.isEmpty()) {
			    currentBehavior = cb.copy(); // new Behavior();
			} else {
			    currentBehavior = ((Behavior)commonBehavior.getFirst()).copy();
			}
		    }
		    ExprModifierPragma e = (ExprModifierPragma)m;
		    if (e.expr.getTag() != TagConstants.NOTSPECIFIEDEXPR)
			currentBehavior.diverges.add(e);
		    break;

		case TagConstants.EXSURES:
		case TagConstants.ALSO_EXSURES:
		case TagConstants.SIGNALS:
		    if (currentBehavior == null) {
			ErrorSet.error(m.getStartLoc(),"Missing also");
			if (commonBehavior.isEmpty()) {
			    currentBehavior = cb.copy(); //  new Behavior();
			} else {
			    currentBehavior = ((Behavior)commonBehavior.getFirst()).copy();
			}
		    }
		    if (currentBehavior.isNormal) {
			ErrorSet.error(m.getStartLoc(),
			   "This type of annotation is not permitted in an normal_behavior clause");
		    }
		    if (((VarExprModifierPragma)m).expr.getTag() != TagConstants.NOTSPECIFIEDEXPR)
			currentBehavior.signals.add(m);
		    break;

		case TagConstants.ASSIGNABLE:
		case TagConstants.MODIFIABLE:
		case TagConstants.MODIFIES:
		case TagConstants.ALSO_MODIFIES: {
		    if (currentBehavior == null) {
			ErrorSet.error(m.getStartLoc(),"Missing also");
			if (commonBehavior.isEmpty()) {
			    currentBehavior = cb.copy(); // new Behavior();
			} else {
			    currentBehavior = ((Behavior)commonBehavior.getFirst()).copy();
			}
		    }
			// null value indicates an informal annotation
		    if (((CondExprModifierPragma)m).expr == null ||
		    	((CondExprModifierPragma)m).expr.getTag() != 
				TagConstants.NOTSPECIFIEDEXPR)
			currentBehavior.modifies.add(m);
		    if (isPure && !isConstructor) {
			CondExprModifierPragma cm = 
				(CondExprModifierPragma)m;
			if (! ( cm.expr instanceof NothingExpr &&
				cm.cond == null)) {
			    ErrorSet.error(m.getStartLoc(),
			     "A pure method may not have an assignable clause");
			}
		    }
			// FIXME - for constructors, should check that 
			//  the assignable clause has only the allowed stuff.
		    break;
		}

		case TagConstants.WHEN:
		    if (currentBehavior == null) {
			ErrorSet.error(m.getStartLoc(),"Missing also");
			if (commonBehavior.isEmpty()) {
			    currentBehavior = cb.copy(); //new Behavior();
			} else {
			    currentBehavior = ((Behavior)commonBehavior.getFirst()).copy();
			}
		    }
		    if (((ExprModifierPragma)m).expr.getTag() != 
					TagConstants.NOTSPECIFIEDEXPR)
			currentBehavior.when.add(m);
		    break;

		case TagConstants.DURATION:
		case TagConstants.WORKING_SPACE:
		    if (currentBehavior == null) {
			ErrorSet.error(m.getStartLoc(),"Missing also");
			if (commonBehavior.isEmpty()) {
			    currentBehavior = cb.copy(); //new Behavior();
			} else {
			    currentBehavior = ((Behavior)commonBehavior.getFirst()).copy();
			}
		    }
		    if (((CondExprModifierPragma)m).expr.getTag() != 
					TagConstants.NOTSPECIFIEDEXPR) {
			if (t == TagConstants.DURATION)
			    currentBehavior.duration.add(m);
			else
			    currentBehavior.workingSpace.add(m);
		    }
		    break;

		case TagConstants.OPENPRAGMA:
		    if (currentBehavior == null) {
			ErrorSet.error(m.getStartLoc(),"Missing also");
			if (commonBehavior.isEmpty()) {
			    currentBehavior = cb.copy(); // new Behavior();
			} else {
			    currentBehavior = ((Behavior)commonBehavior.getFirst()).copy();
			}
		    }
		    currentBehavior.openPragma = m;
		    commonBehavior.addFirst(currentBehavior.copy());
		    break;

		case TagConstants.ALSO:
		    if (currentBehavior != null) 
			results.add(currentBehavior);
		    if (commonBehavior.isEmpty()) {
			currentBehavior = cb.copy(); // new Behavior();
		    } else {
			currentBehavior = ((Behavior)commonBehavior.getFirst()).copy();
		    }
		    break;

		case TagConstants.CLOSEPRAGMA:
		    if (currentBehavior != null) 
			results.add(currentBehavior);
		    if (commonBehavior.isEmpty()) {
			ErrorSet.error(m.getStartLoc(),
			    "Encountered |} without a matching {|");
		    } else {
			commonBehavior.removeFirst();
			currentBehavior = null;
		    }
		    break;

		case TagConstants.MODEL_PROGRAM:
		    if (currentBehavior == null && !currentBehavior.isEmpty()) {
			ErrorSet.error(m.getStartLoc(),"Missing also");
			if (commonBehavior.isEmpty()) {
			    currentBehavior = cb.copy(); // new Behavior();
			} else {
			    currentBehavior = ((Behavior)commonBehavior.getFirst()).copy();
			}
		    }
		    if (!commonBehavior.isEmpty()) {
			ErrorSet.error(m.getStartLoc(),
			     "A model program may not be nested");
		    }
		    // FIXME - the model programs aren't saved anywhere
		    currentBehavior = null;
		    break;

		case TagConstants.SUBCLASSING_CONTRACT:
		    if (exampleMode)
			ErrorSet.error(m.getStartLoc(),
			      "Misplaced subclassing_contract clause");
		    --pos;
		    terminate = true;
		    break; 

		case TagConstants.IMPLIES_THAT:
		    if (exampleMode) 
			ErrorSet.error("Did not expect implies_that after examples section");
		    // fall through
		case TagConstants.FOR_EXAMPLE:
		case TagConstants.ALSO_REFINE:
		case TagConstants.END:
		    --pos;
		    terminate = true;
		    break; 

		case TagConstants.SPEC_PUBLIC:
		case TagConstants.SPEC_PROTECTED:
		case TagConstants.PURE:
		case TagConstants.NON_NULL:
		case TagConstants.HELPER:
		case TagConstants.INSTANCE:
		    if (currentBehavior == null) 
				currentBehavior = new Behavior();
		    currentBehavior.extras.add(m);
		    continue; 

	 	case TagConstants.GHOST:
		case TagConstants.MODEL:
		    break;

	        default:

		    ErrorSet.caution(m.getStartLoc(),
			"Desugaring does not support "
			+ TagConstants.toString(m.getTag()));

		    currentBehavior.extras.add(m);
		    break;
            }
        } 
	if (currentBehavior != null) {
	    if (currentBehavior.isEmpty()) {
	    } else {
		results.add(currentBehavior);
	    }
	}
	if (!commonBehavior.isEmpty()) {
	    ModifierPragma openPragma = ((Behavior)commonBehavior.getFirst()).openPragma;
	    ErrorSet.error(openPragma.getStartLoc(),"No closing |} for this {|");
	}
	return pos;
    }
*/
/*
    public int sc_section(int pos, ModifierPragmaVec pm, ArrayList results) {
	while (pos < pm.size()) {
	    ModifierPragma m = pm.elementAt(pos++);
	    switch (m.getTag()) {
		case TagConstants.ACCESSIBLE:
		case TagConstants.CALLABLE:
		case TagConstants.MEASURED_BY:
		    results.add(m);
		    break;

		case TagConstants.IMPLIES_THAT:
		case TagConstants.FOR_EXAMPLE:
		case TagConstants.END:
		case TagConstants.PURE:
		case TagConstants.NON_NULL:
		    return pos-1;

		default:
		    ErrorSet.error("Did not expect this annotation in a subclassing_contract");
	    }
	}
	return pos;
    }
*/
    /** Produces an expression which is the conjunction of the two expressions.
	If either input is null, the other is returned.  If either input is
	literally true or false, the appropriate constant folding is performed.
    */
    static public Expr and(Expr e1, Expr e2) {
	if (e1 == null || isTrue(e1)) return e2;
	if (e2 == null || isTrue(e2)) return e1;
	if (isFalse(e1)) return e1;
	if (isFalse(e2)) return e2;
	Expr e = BinaryExpr.make(TagConstants.AND,e1,e2,e1.getStartLoc());
	javafe.tc.FlowInsensitiveChecks.setType(e,Types.booleanType);
	return e;
    }

    /** Produces an ExprModifierPragma whose expression is the conjunction 
	of the expressions in the input pragmas.
	If either input is null, the other is returned.  If either input is
	literally true or false, the appropriate constant folding is performed.
    */
    static public ExprModifierPragma and(ExprModifierPragma e1, ExprModifierPragma e2) {
	if (e1 == null || isTrue(e1.expr)) return e2;
	if (e2 == null || isTrue(e2.expr)) return e1;
	if (isFalse(e1.expr)) return e1;
	if (isFalse(e2.expr)) return e2;
	Expr e = BinaryExpr.make(TagConstants.AND,e1.expr,e2.expr,e1.getStartLoc());
	javafe.tc.FlowInsensitiveChecks.setType(e,Types.booleanType);
	return ExprModifierPragma.make(
			e1.getTag(),e,e1.getStartLoc());
    }

    /** Produces an ExprModifierPragma whose expression is the conjunction of
	all of the expressions in the ExprModifierPragmas in the argument.
	If the argument is empty, null is returned.  Otherwise, some
	object is returned, though its expression might be a literal.
    */
    static public ExprModifierPragma and(/*@ non_null */ ArrayList a) {
	if (a.size() == 0) {
	    return null;
	} else if (a.size() == 1) {
	    return (ExprModifierPragma)a.get(0);
	} else {
	    ExprModifierPragma e = null;
	    Iterator i = a.iterator();
	    while (i.hasNext()) {
		e = and(e,(ExprModifierPragma)i.next());
	    }
	    return e;
	}
    }

    /** The same as and(ArrayList), but produces labelled expressions within
	the conjunction so that error messages come out with useful locations.
    */
    static public ExprModifierPragma andLabeled(/*@ non_null */ ArrayList a) {
	if (a.size() == 0) {
	    return null;
	} else {
	    Expr e = null;
	    int floc = Location.NULL;
	    Iterator i = a.iterator();
	    while (i.hasNext()) {
		ExprModifierPragma emp = (ExprModifierPragma)i.next();
		int loc = emp.getStartLoc();
		if (floc == Location.NULL) floc = loc;
		boolean nn = emp.expr instanceof NonNullExpr;
		Expr le = LabelExpr.make(
				emp.getStartLoc(), emp.getEndLoc(), false, 
				escjava.translate.GC.makeLabel(
					nn?"NonNull":"Pre",loc,Location.NULL),
				emp.expr);
		javafe.tc.FlowInsensitiveChecks.setType(le,Types.booleanType);
		if (!isTrue(emp.expr)) e = and(e, le);
		else if (e == null) e = le;
		javafe.tc.FlowInsensitiveChecks.setType(e,Types.booleanType);
	    }
	    return ExprModifierPragma.make(TagConstants.REQUIRES,
			e, floc);
	}
    }
/*
    static public Expr or(Expr e1, Expr e2) {
	if (e1 == null || isFalse(e1)) return e2;
	if (e2 == null || isFalse(e2)) return e1;
	if (isTrue(e1)) return e1;
	if (isTrue(e2)) return e2;
	Expr e = BinaryExpr.make(TagConstants.OR,e1,e2,e1.getStartLoc());
	javafe.tc.FlowInsensitiveChecks.setType(e,Types.booleanType);
	return e;
    }
*/
    /** Produces an ExprModifierPragma whose expression is the disjunction 
	of the expressions in the input pragmas.
	If either input is null, the other is returned.  If either input is
	literally true or false, the appropriate constant folding is performed.
    */
    static public ExprModifierPragma or(ExprModifierPragma e1, ExprModifierPragma e2) {
	if (e1 == null || isFalse(e1.expr)) return e2;
	if (e2 == null || isFalse(e2.expr)) return e1;
	if (isTrue(e1.expr)) return e1;
	if (isTrue(e2.expr)) return e2;
	Expr e = BinaryExpr.make(TagConstants.OR,e1.expr,e2.expr,e1.getStartLoc());
	javafe.tc.FlowInsensitiveChecks.setType(e,Types.booleanType);
	return ExprModifierPragma.make(
			e1.getTag(),e,e1.getStartLoc());
    }

    /** Produces an ExprModifierPragma whose expression is the disjunction of
	all of the expressions in the ExprModifierPragmas in the argument.
	If the argument is empty, null is returned.  Otherwise, some
	object is returned, though its expression might be a literal.
    */
    static public ExprModifierPragma or(/*@ non_null */ ArrayList a) {
	if (a.size() == 0) {
	    return null;
	} else if (a.size() == 1) {
	    return (ExprModifierPragma)a.get(0);
	} else {
	    ExprModifierPragma e = null;
	    Iterator i = a.iterator();
	    while (i.hasNext()) {
		e = or(e,(ExprModifierPragma)i.next());
	    }
	    return e;
	}
    }

    /** Produces an expression which is the implication of the two expressions.
	Neither input may be null.  If either input is
	literally true or false, the appropriate constant folding is performed.
    */
    static public Expr implies(/*@ non_null */Expr e1, /*@ non_null */ Expr e2) {
	if (isTrue(e1)) return e2;
	if (isTrue(e2)) return e2; // Use e2 instead of T to keep location info 
	if (isFalse(e1)) return T;
	Expr e = BinaryExpr.make(TagConstants.IMPLIES,e1,e2,e2.getStartLoc());
	javafe.tc.FlowInsensitiveChecks.setType(e,Types.booleanType);
	return e;
    }

    /** Returns true if the argument is literally true, and returns
	false if it is not a literal or is literally false. */
    static boolean isTrue(/*@ non_null */ Expr e) {
	return e == T || 
	    (e instanceof LiteralExpr && 
		((LiteralExpr)e).value.equals(T.value));
    }

    /** Returns true if the argument is literally false, and returns
	false if it is not a literal or is literally true. */
    static boolean isFalse(/*@ non_null */ Expr e) {
	return e == F || 
	    (e instanceof LiteralExpr && 
		((LiteralExpr)e).value.equals(F.value));
    }

    public final static LiteralExpr T = 
	    (LiteralExpr)FlowInsensitiveChecks.setType(LiteralExpr.make(
	    TagConstants.BOOLEANLIT, Boolean.TRUE, Location.NULL),
	    Types.booleanType);
    public final static LiteralExpr F = 
	    (LiteralExpr)FlowInsensitiveChecks.setType(LiteralExpr.make(
	    TagConstants.BOOLEANLIT, Boolean.FALSE, Location.NULL),
	    Types.booleanType);

/*
    static public class Behavior {

	public final static LiteralExpr T = 
		(LiteralExpr)FlowInsensitiveChecks.setType(LiteralExpr.make(
		TagConstants.BOOLEANLIT, Boolean.TRUE, Location.NULL),
		Types.booleanType);
	public final static LiteralExpr F = 
		(LiteralExpr)FlowInsensitiveChecks.setType(LiteralExpr.make(
		TagConstants.BOOLEANLIT, Boolean.FALSE, Location.NULL),
		Types.booleanType);

	public final static CondExprModifierPragma defaultModifies(int loc, boolean nothing) {
			return CondExprModifierPragma.make(
				TagConstants.MODIFIES,
				nothing ? (Expr)NothingExpr.make(loc) :
					    (Expr)EverythingExpr.make(loc),
				loc,null);
	}

	public final static ExprModifierPragma ensuresFalse(int loc) {
			return ExprModifierPragma.make(
			    TagConstants.ENSURES,
			    Behavior.F,
			    loc);
	}

	public final static VarExprModifierPragma defaultSignalFalse( int loc) {
		VarExprModifierPragma v = VarExprModifierPragma.make(
			    TagConstants.SIGNALS,
			    null, // Interpreted as java.lang.Exception _
			    Behavior.F,
			    loc);
		return v;
	}

	public boolean isLightweight = true;
	public boolean isNormal = false;
	public boolean isExceptional = false;
	public boolean defaultIsModifiesNothing = false;
	public ModifierPragma openPragma = null;

	public boolean isEmpty() {
		return requires.size() == 0
			&& ensures.size() == 0
			&& diverges.size() == 0 
			&& modifies.size() == 0 
			&& when.size() == 0 
			&& duration.size() == 0 
			&& workingSpace.size() == 0 
			&& extras.size() == 0 
			&& signals.size() == 0;
	}

	public ArrayList requires = new ArrayList(); // of ExprModifierPragma 
	public ArrayList ensures = new ArrayList(); // of ExprModifierPragma
	public ArrayList when = new ArrayList(); // of ExprModifierPragma
	public ArrayList duration = new ArrayList(); // of CondExprModifierPragma
	public ArrayList workingSpace = new ArrayList(); // of CondExprModifierPragma
	public ArrayList diverges = new ArrayList(); // of ExprModifierPragma
	public ArrayList signals = new ArrayList(); // of VarExprModifierPragma 
	public ArrayList modifies = new ArrayList();//of CondExprModifierPragma 
	public ArrayList modelPrograms = new ArrayList(); // of ModelProgramModifierPragmas
	public ArrayList subclassingContracts = new ArrayList(); // of ModifierPragmas
	public ArrayList implications = new ArrayList();
	public ArrayList examples = new ArrayList();

	public ArrayList extras = new ArrayList(); // of ModifierPragma 

	public Behavior copy() {
		Behavior b = new Behavior();
		b.isLightweight = isLightweight;
		b.isNormal = isNormal;
		b.isExceptional = isExceptional;
		b.openPragma = openPragma;
		b.requires = new ArrayList(requires);
		// We have to make copies of the ensures pragmas because
		// if the routine result is declared non_null, then
		// the fabricated ensures clause stating this needs to be
		// duplicated so that the implication can be inserted back
		// into the pragma
		b.ensures = new ArrayList();
		Iterator i = ensures.iterator();
		while (i.hasNext()) {
		    ExprModifierPragma m = (ExprModifierPragma)(i.next());
		    ExprModifierPragma mm = 
			ExprModifierPragma.make(m.getTag(),
				m.expr,m.loc);
		    mm.errorTag = m.errorTag;
		    b.ensures.add(mm);
		}
		b.when = new ArrayList(when);
		b.duration = new ArrayList(duration);
		b.workingSpace = new ArrayList(workingSpace);
		b.diverges = new ArrayList(diverges);
		b.signals = new ArrayList(signals);
		b.modifies = new ArrayList(modifies);
		return b;
	}
	public void combine(Behavior b, ArrayList orlist) {
	    if (b == null) return;

	    // The only default that is not trivially satisfied is the
	    // default for diverges in a heavyweight spec, which is
	    // 'diverges false'.
	    // But ESC/Java also needs the modifies default which is
	    // 'modifies \everything', since otherwise it presumes 
	    // locations are not modified.

	    if (!b.isLightweight && b.diverges.size() == 0)
		b.diverges.add(ExprModifierPragma.make(
				TagConstants.DIVERGES,
				Behavior.F,Location.NULL));


	    // The requires statements combine as an OR of the groups
	    // of requires clauses; each group is an AND of its components.
	    // However, if possible, we leave the components separate.
	    // Note that an empty list of requires clauses is equivalent
	    // to TRUE.

	    // req = the composite requires for this group (in b) of annotations
	    ExprModifierPragma reqclause = and(b.requires);
	    boolean reqIsTrue = reqclause == null || isTrue(reqclause.expr);
	    boolean reqIsFalse = reqclause != null && isFalse(reqclause.expr);
	    Expr reqexpr = reqclause==null? null : reqclause.expr;

	    // If the composite requires from the argument(b) are
	    // literally false, then none of the subsequent clauses are
	    // useful.  To save work, we omit them entirely.
	    if (reqIsFalse) {
		// However, we do have to add an explicitly false requires
		// clause so that we can retain the location information
		ArrayList a = new ArrayList();
		a.add(reqclause);
		orlist.add(a);
		return;
	    }

	    // Form the composite requires clause for all the groups
	    // If there is only one non-false group, then we retain the
	    // ArrayList of requires clauses from that group, so that the
	    // location information about unsatisfied requirements is as
	    // useful as possible to the user.  If there is more than one
	    // non-false group, then the groups have to be combined into
	    // a single requires annotation containing a disjunction of the
	    // conjunctions resulting from each group.

	    // b.requires is not null, but it might be empty; if it is empty
	    // it effectively means a true precondition for that group,
	    // which means that the result of the OR will be true
	    orlist.add(b.requires); 
				// orlist is an ArrayList of ArrayList of
					// ExprModifierPragma objects

	    Expr req = Behavior.T;
	    if (reqexpr != null) {
		ExprVec arg = ExprVec.make(new Expr[]{reqexpr});
		req = NaryExpr.make(Location.NULL,
				    reqexpr.getStartLoc(),TagConstants.PRE,
				    Identifier.intern("\\old"),arg);
		javafe.tc.FlowInsensitiveChecks.setType(req,
				    Types.booleanType);
	    }

	    // Add in all the annotations from the argument, taking care
	    // to guard them with the precondition

	    if (b.modifies.size() == 0) {
		b.modifies.add(Behavior.defaultModifies(Location.NULL,
					b.defaultIsModifiesNothing));
	    }
	    Iterator i = b.modifies.iterator();
	    while (i.hasNext()) {
		CondExprModifierPragma m = (CondExprModifierPragma)i.next();
		m.cond = and(m.cond,req);
		modifies.add(m);
	    }
	    i = b.ensures.iterator();
	    while (i.hasNext()) {
		ExprModifierPragma m = (ExprModifierPragma)i.next();
		if (!reqIsTrue) m.expr = implies(req,m.expr);
		if (isFalse(m.expr)) { ensures.clear(); }
		ensures.add(m);
	    }
	    i = b.when.iterator();
	    while (i.hasNext()) {
		ExprModifierPragma m = (ExprModifierPragma)i.next();
		if (!reqIsTrue) m.expr = implies(req,m.expr);
		if (isFalse(m.expr)) { when.clear(); }
		when.add(m);
	    }
	    i = b.duration.iterator();
	    while (i.hasNext()) {
		CondExprModifierPragma m = (CondExprModifierPragma)i.next();
		m.cond = and(m.cond,req);
		duration.add(m);
	    }
	    i = b.workingSpace.iterator();
	    while (i.hasNext()) {
		CondExprModifierPragma m = (CondExprModifierPragma)i.next();
		m.cond = and(m.cond,req);
		workingSpace.add(m);
	    }
	    i = b.diverges.iterator();
	    while (i.hasNext()) {
		ExprModifierPragma m = (ExprModifierPragma)i.next();
		m.expr = implies(req,m.expr);
		if (isFalse(m.expr)) { diverges.clear(); }
		diverges.add(m);
	    }
	    i = b.signals.iterator();
	    while (i.hasNext()) {
		VarExprModifierPragma m = (VarExprModifierPragma)i.next();
		if (!reqIsTrue) m.expr = implies(req,m.expr);
		signals.add(m);
	    }
	    extras.addAll(b.extras);
	}

	public void print() {
	    // FIXME - expand to print all of the fields
	    Iterator i = ensures.iterator();
	    while (i.hasNext()) {
		ExprModifierPragma m = (ExprModifierPragma)i.next();
		printSpec(m);
	    }
	}
    }
*/
    static public class CheckPurity {

	public void visitNode(ASTNode x) {
	    if (x == null) return;
	    switch (x.getTag()) {
		case TagConstants.METHODINVOCATION:
		    MethodInvocation m = (MethodInvocation)x;
		    if (Main.options().checkPurity &&
		        !escjava.tc.FlowInsensitiveChecks.isPure(m.decl)) {
			ErrorSet.error(m.locId,
			    "Method " + m.id + " is used in an annotation" +
			    " but is not pure (" + 
			    Location.toFileLineString(m.decl.loc) + ")");
		    }
		    break;
		case TagConstants.NEWINSTANCEEXPR:
		    NewInstanceExpr c = (NewInstanceExpr)x;
		    if (Main.options().checkPurity &&
		        !escjava.tc.FlowInsensitiveChecks.isPure(c.decl)) {
			ErrorSet.error(c.loc,
			    "Constructor is used in an annotation" +
			    " but is not pure (" + 
			    Location.toFileLineString(c.decl.loc) + ")");
		    }
		    break;
		case TagConstants.WACK_DURATION:
		case TagConstants.WACK_WORKING_SPACE:
		case TagConstants.SPACE:
		    // The argument of these built-in functions is not
		    // evaluated, so it need not be pure.
		    return;
	    }
	    {
		    int n = x.childCount();
		    for (int i = 0; i < n; ++i) {
			if (x.childAt(i) instanceof ASTNode)
				visitNode((ASTNode)x.childAt(i));
		    }
	    }
	}

    }



	// FIXME - This functionality is duplicated elsewhere, e.g. in
	// translate/GetSpec.  Should be unified and cleaned up in a common
	// utility.
    public static ModifierPragma findModifierPragma(ModifierPragmaVec v, int tag) {
	if (v == null) return null;
	for (int i=0; i<v.size(); ++i) {
		if (v.elementAt(i).getTag() == tag) return v.elementAt(i);
	}
	return null;
    }

    static private void print(Expr e) {
	if (e != null) PrettyPrint.inst.print(System.out,0,e);
    }

    static public class NonNullExpr extends BinaryExpr {

	static NonNullExpr make(FormalParaDecl arg, int locNN) {
	    NonNullExpr e = new NonNullExpr();
	    int loc = arg.getStartLoc();
	    Expr v = VariableAccess.make(arg.id, loc, arg);
	    javafe.tc.FlowInsensitiveChecks.setType(v, arg.type);
	    Expr n = LiteralExpr.make(TagConstants.NULLLIT, null, locNN);
	    javafe.tc.FlowInsensitiveChecks.setType(n, Types.nullType);
	    e.op = TagConstants.NE;
	    e.left = v;
	    e.right = n;
	    e.locOp = locNN;
	    javafe.tc.FlowInsensitiveChecks.setType(e,Types.booleanType);
	    return e;
	}
    }

    //----------------------------------------------------------------------
    // Parsing the sequence of ModifierPragmas for each method of a 
    // compilation unit happens as a part of the original parsing and
    // refinement processing.

    static NestedPragmaParser specparser = new NestedPragmaParser();

    public void parseAllRoutineSpecs(CompilationUnit ccu) {
	specparser.parseAllRoutineSpecs(ccu);
    }
}
		




/** The routines in this class parse a sequence of ModifierPragma that 
    occur prior to a method or constructor declaration.  These consist
    of lightwieght or heavywieght specifications, possibly nested or
    with consecutive spec-cases separated by 'also'.  The parsing of the
    compilation unit simply produces a flat sequence of such ModifierPragmas,
    since they may occur in separate annotation comments and the Javafe
    parser does not provide mechanisms to associate them together.
    However, we do need to determine the nesting structure of the sequence
    of pragmas because the forall and old pragmas introduce new variable
    declarations that may be used in subsequent pragmas.  This parsing into
    the nested structure (and checking of it) needs to be completed prior
    to type checking so that the variable references are properly 
    determined.  The ultimate desugaring then happens after typechecking.
 */    

class NestedPragmaParser {
 
    /** Parses the sequence of pragma modifiers for each routine in 
	the CompilationUnit,
	replacing the existing sequence with the parsed one in each case.
    */
    public void parseAllRoutineSpecs(CompilationUnit ccu) {
        TypeDeclVec v = ccu.elems;
        for (int i=0; i<v.size(); ++i) {
            parseAllRoutineSpecs(v.elementAt(i));
        }
    }
 
    public void parseAllRoutineSpecs(TypeDecl td) {
        TypeDeclElemVec v = td.elems;
        for (int i=0; i<v.size(); ++i) {
            TypeDeclElem tde = v.elementAt(i);
            if (tde instanceof RoutineDecl) {
                parseRoutineSpecs((RoutineDecl)tde);
            } else if (tde instanceof ModelMethodDeclPragma) {
                parseRoutineSpecs( ((ModelMethodDeclPragma)tde).decl );
            } else if (tde instanceof ModelConstructorDeclPragma) {
                parseRoutineSpecs( ((ModelConstructorDeclPragma)tde).decl );
            } else if (tde instanceof TypeDecl) {
                parseAllRoutineSpecs((TypeDecl)tde);
            }
        }
    }
 
    public void parseRoutineSpecs(RoutineDecl rd) {
        ModifierPragmaVec pm = rd.pmodifiers;
        if (pm == null || pm.size() == 0) {
	    ParsedRoutineSpecs pms = new ParsedRoutineSpecs();
	    pms.modifiers.addElement(ParsedSpecs.make(rd,pms));
	    rd.pmodifiers = pms.modifiers;
	    return;
	}

	// We add this (internal use only) END pragma so that we don't have
	// to continually check the value of pos vs. the size of the array
	pm.addElement(SimpleModifierPragma.make(TagConstants.END,
			pm.size() == 0 ? Location.NULL :
			pm.elementAt(pm.size()-1).getStartLoc()));

        ParsedRoutineSpecs pms = new ParsedRoutineSpecs();
        int pos = 0;
        if (pm.elementAt(0).getTag() == TagConstants.ALSO) {
                pms.initialAlso = pm.elementAt(0);
                ++pos;
        }
        pos = parseAlsoSeq(pos,pm,1,null,pms.specs);
        if (pm.elementAt(pos).getTag() == TagConstants.IMPLIES_THAT) {
	    ++pos;
            pos = parseAlsoSeq(pos,pm,1,null,pms.impliesThat);
        }
        if (pm.elementAt(pos).getTag() == TagConstants.FOR_EXAMPLE) {
	    ++pos;
            pos = parseAlsoSeq(pos,pm,2,null,pms.examples);
        }
        while (true) {
            ModifierPragma mp = pm.elementAt(pos);
            int tag = mp.getTag();
            if (tag == TagConstants.END) break;
            if (!isRoutineModifier(tag)) {
                int loc = Location.NULL;
                if (pms.modifiers.size() > 0)
                    loc = pms.modifiers.elementAt(0).getStartLoc();
                ErrorSet.error(mp.getStartLoc(),
                    "Unexpected or out of order pragma (expected a simple routine modifier)",loc);
            } else {
                pms.modifiers.addElement(mp);
            }
	    ++pos;
        }
	pms.modifiers.addElement(ParsedSpecs.make(rd,pms));
	rd.pmodifiers = pms.modifiers;
    }
 
    static public boolean isRoutineModifier(int tag) {
        return tag == TagConstants.PURE ||
                tag == TagConstants.HELPER ||
                tag == TagConstants.GHOST || // Actually should not occur
                tag == TagConstants.MODEL ||
                tag == TagConstants.NON_NULL;
    }
 
    // behaviorMode == 0 : nested call
    // behaviorMode == 1 : outer call - non-example mode, model programs allowed
    // behaviorMode == 2 : outer call - example mode
    // The behaviorMode is used to determine which behavior/example keywords
    // are valid - but this is only needed on the outermost nesting level.
    // The behaviorTag is used to determine whether signals or ensures clauses
    // are permitted; 0 means either are ok; not valid on outermost call
    public int parseAlsoSeq(int pos, ModifierPragmaVec pm, 
		    int behaviorMode, ModifierPragma behavior, ArrayList result) {
        while(true) {
          ModifierPragmaVec mpv = ModifierPragmaVec.make();
	  if (behaviorMode != 0) {
	    ModifierPragma mp = pm.elementAt(pos);
	    behavior = mp;
	    int behaviorTag = mp.getTag();
	    ++pos;
	    switch (behaviorTag) {
		case TagConstants.MODEL_PROGRAM:
		    mpv.addElement(mp);
		    result.add(mpv);
		    if (pm.elementAt(pos).getTag() != TagConstants.ALSO) break;
		    ++pos;
		    continue;

		case TagConstants.BEHAVIOR:
		    if (behaviorMode == 2) ErrorSet.error(mp.getStartLoc(),
			"Behavior keywords may not be in the for_example section");
		    break;
		case TagConstants.NORMAL_BEHAVIOR:
		    if (behaviorMode == 2) ErrorSet.error(mp.getStartLoc(),
			"Behavior keywords may not be in the for_example section");
		    mpv.addElement(VarExprModifierPragma.make(
			    TagConstants.SIGNALS,
			    FormalParaDecl.make(0,null,
				TagConstants.ExsuresIdnName,
				Types.javaLangException(),mp.getStartLoc()),
			    AnnotationHandler.F,
			    mp.getStartLoc()));
		    break;
		case TagConstants.EXCEPTIONAL_BEHAVIOR:
		    if (behaviorMode == 2) ErrorSet.error(mp.getStartLoc(),
			"Behavior keywords may not be in the for_example section");
		    mpv.addElement(
			ExprModifierPragma.make(TagConstants.ENSURES,
				AnnotationHandler.F, mp.getStartLoc()));
		    break;
		case TagConstants.EXAMPLE:
		    if (behaviorMode == 1) ErrorSet.error(mp.getStartLoc(),
			"Example keywords may be used only in the for_example section");
		    break;
		case TagConstants.NORMAL_EXAMPLE:
		    if (behaviorMode == 1) ErrorSet.error(mp.getStartLoc(),
			"Example keywords may be used only in the for_example section");
		    mpv.addElement(VarExprModifierPragma.make(
			    TagConstants.SIGNALS,
			    FormalParaDecl.make(0,null,
				TagConstants.ExsuresIdnName,
				Types.javaLangException(),mp.getStartLoc()),
			    AnnotationHandler.F,
			    mp.getStartLoc()));
		    break;
		case TagConstants.EXCEPTIONAL_EXAMPLE:
		    if (behaviorMode == 1) ErrorSet.error(mp.getStartLoc(),
			"Example keywords may be used only in the for_example section");
		    mpv.addElement(
			ExprModifierPragma.make(TagConstants.ENSURES,
				AnnotationHandler.F, mp.getStartLoc()));
		    break;
		default:
		    // lightweight
		    --pos;
		    behavior = null;
	      }
	    }
	    pos = parseSeq(pos,pm,0,behavior,mpv);
            if (mpv.size() != 0) result.add(mpv);
	    else if (behaviorMode == 0) {
		ErrorSet.error(pm.elementAt(pos).getStartLoc(),
			"JML does not allow empty clause sequences");
	    }
            if (pm.elementAt(pos).getTag() != TagConstants.ALSO) break;
            ++pos;
        }
	if (behaviorMode != 0) {
	    while (pm.elementAt(pos).getTag() == TagConstants.CLOSEPRAGMA) {
		ErrorSet.error(pm.elementAt(pos).getStartLoc(),
			"There is no opening {| to match this closing |}");
		++pos;
	    }
	}
	return pos;
    }

 
    //@ requires (* pm.elementAt(pm.size()-1).getTag() == TagConstants.END *);
    public int parseSeq(int pos, ModifierPragmaVec pm, 
			int behaviorMode, ModifierPragma behavior, 
			ModifierPragmaVec result) {
	int behaviorTag = behavior==null? 0 : behavior.getTag();
	//System.out.println("STARTING " + behaviorMode + " " + behaviorTag);
        while (true) {
            ModifierPragma mp = pm.elementAt(pos);
	    int loc = mp.getStartLoc();
            int tag = mp.getTag();
            if (isRoutineModifier(tag)) return pos;
	    //System.out.println("TAG " + TagConstants.toString(tag));
            switch (tag) {
                case TagConstants.END:
                case TagConstants.IMPLIES_THAT:
                case TagConstants.FOR_EXAMPLE:
                case TagConstants.ALSO:
                case TagConstants.CLOSEPRAGMA:
                    return pos;

		case TagConstants.MODEL_PROGRAM:
		    if (behaviorMode == 0) ErrorSet.error(mp.getStartLoc(),
			"Model programs may not be nested");
		    if (behaviorMode == 2) ErrorSet.error(mp.getStartLoc(),
			"Model programs may not be in the examples section");
// FIXME - parse model program
		    break;

		case TagConstants.BEHAVIOR:
		case TagConstants.NORMAL_BEHAVIOR:
		case TagConstants.EXCEPTIONAL_BEHAVIOR:
		case TagConstants.EXAMPLE:
		case TagConstants.NORMAL_EXAMPLE:
		case TagConstants.EXCEPTIONAL_EXAMPLE:
		    if (behaviorMode == 0) ErrorSet.error(mp.getStartLoc(),
			"Misplaced " + TagConstants.toString(tag) + " keyword");
		    ++pos;
		    break;
 
                case TagConstants.OPENPRAGMA:
                  {
                    int openLoc = loc;
                    ++pos;
		    ArrayList s = new ArrayList();
                    pos = parseAlsoSeq(pos,pm,0,behavior,s);
                    if (pm.elementAt(pos).getTag() != TagConstants.CLOSEPRAGMA) {
                        ErrorSet.error(pm.elementAt(pos).getStartLoc(),
                            "Expected a closing |}",openLoc);
                    } else {
			++pos;
		    }
		    if (s.size() == 0) {
			ErrorSet.error(openLoc,
				"JML does not allow an empty clause sequence");
		    } else {
			result.addElement(NestedModifierPragma.make(s));
		    }
                  }
		  break;

		// Any clause keyword ends up in the default (as well as
		// anything unrecognized).  We do that because there are
		// so many clause keywords.  However, that means that we
		// have to be sure to have the list of keywords in 
		// isRoutineModifier correct.
                default:
		    if (
			(((behaviorTag == TagConstants.NORMAL_BEHAVIOR ||
			behaviorTag == TagConstants.NORMAL_EXAMPLE) &&
			(tag == TagConstants.SIGNALS ||
			 tag == TagConstants.EXSURES))) 
			||
			(((behaviorTag == TagConstants.EXCEPTIONAL_BEHAVIOR ||
			behaviorTag == TagConstants.EXCEPTIONAL_EXAMPLE) &&
			(tag == TagConstants.ENSURES ||
			 tag == TagConstants.POSTCONDITION))) 
			) {
			ErrorSet.error(loc,"A " + TagConstants.toString(tag) +
			    " clause is not allowed in a " +
			    TagConstants.toString(behaviorTag) + " section",
			    behavior.getStartLoc());
		    } else {
			result.addElement(mp);
		    }
		    ++pos;
            }
        }
    }
}
// FIXME - things not checked
//	There should be no clauses after a |} (only |} only also or END or simple mods)
//	The order of clauses is not checked
//	JML only allows forall, old, requires prior to a nesting.
