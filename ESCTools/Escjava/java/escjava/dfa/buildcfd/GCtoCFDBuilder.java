package escjava.dfa.buildcfd;

import escjava.translate.GC;

import escjava.dfa.cfd.*;
import escjava.dfa.cfd.NodeList.Enumeration;

import escjava.ast.TagConstants;
import escjava.ast.*;
import javafe.ast.*;
import javafe.util.Assert;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;

/**
 * Used to construct a graph from a guarded command.
 */
public class GCtoCFDBuilder {

	/*@ non_null @*/List unreachable;

	public GCtoCFDBuilder() {
		unreachable = new LinkedList();
	}

	/**
	 * Gets the list of graphs created during the translation that are not
	 * reachable from the initial node.
	 * 
	 * @return a list of graphs
	 */
	//@ ensures \result != null;
	//@ ensures \result == this.unreachable;
	public List getUnreachable() {
		return unreachable;
	}

    
    protected CFD constructLoopGraph(LoopCmd loopCommand, GenericVarDeclVec scope) {
        Assert.fail("Loop translation construction no implemented");
        return null;
    }
    
	//@ ensures !(\result.isEmpty());
	public CFD constructGraph(GuardedCmd command, GenericVarDeclVec scope) {
		int tag = command.getTag();
		switch (tag) {
		case TagConstants.SKIPCMD:
		case TagConstants.ASSERTCMD:
		case TagConstants.GETSCMD:
		case TagConstants.SUBGETSCMD:
		case TagConstants.SUBSUBGETSCMD:
		case TagConstants.RESTOREFROMCMD: {
			Node nn = new CodeNode(scope, command);
			CFD cfd = new CFD();
			cfd.createSimpleCFD(nn);
			return cfd;
		}
		case TagConstants.ASSUMECMD: {
			ExprCmd ecmd = (ExprCmd) command;
			Expr pred = ecmd.pred;
			Node assumeNode = new CodeNode(scope, ecmd);
			CFD cfd = new CFD();
			cfd.setInitNode(assumeNode);

			// optimize for assume false
			if (!GC.isFalse(pred)) {
				cfd.setExitNode(assumeNode);
			} // otherwise leave the exit node null

			return cfd;
		}
		case TagConstants.CALL: {
			// plunge into the desugared version of the call
			Call call = (Call)command;
			return constructGraph(call.desugared, scope);
		}
		case TagConstants.LOOPCMD: {
            //  utilize the desugared version of the loop
            LoopCmd loopCommand = (LoopCmd) command;
            return constructLoopGraph(loopCommand, scope);
            //return constructGraph(loopCmd.desugared, scope);
			/*
			 * LoopCmd loopCmd = (LoopCmd) command; int loc = loopCmd.locStart;
			 * PredicateAbstraction pa = (PredicateAbstraction)
			 * PredicateAbstraction.paDecoration.get(loopCmd); assert pa !=
			 * null;
			 * 
			 * if (pa != null && pa.getInvariants().size() > 0) { ExprVec
			 * invariants = pa.getInvariants(); GuardedCmdVec assertsVec =
			 * GuardedCmdVec.make(invariants.size());
			 * 
			 * for (int i = 0; i < invariants.size(); i++) { Expr invariant =
			 * invariants.elementAt(i); ExprCmd invariantAssert =
			 * ExprCmd.make(TagConstants.ASSERTCMD, invariant, loc);
			 * assertsVec.addElement(invariantAssert); }
			 * 
			 * GuardedCmd seqCmd = GC.seq(assertsVec); CFD retv =
			 * constructGraph(seqCmd, scope); return retv; } else { // plunge
			 * into the desugared version of the loop return
			 * constructGraph(loopCmd.desugared, scope); }
			 */
            //return null;
		}
		case TagConstants.DYNINSTCMD: {
			// plunge into the command
			DynInstCmd dc = (DynInstCmd) command;
			return constructGraph(dc.g, scope);
		}
		case TagConstants.RAISECMD: {
                    // a graph with single node, being initial and exceptional
                    // resulting diagram will have no exit node and one exception node
                    CFD raiseCFD = new CFD();
            
                    Node exceptionNode = new ExceptionNode();
                    raiseCFD.setInitNode(exceptionNode);
                    raiseCFD.addExceptionNode(exceptionNode);
            
                    return raiseCFD;
                }
		case TagConstants.CHOOSECMD: {
                    // a diamond structure
                    CmdCmdCmd cc = (CmdCmdCmd) command;
                    GuardedCmd command1 = cc.g1;
                    GuardedCmd command2 = cc.g2;
                    CFD cfd1 = constructGraph(command1, scope);
                    CFD cfd2 = constructGraph(command2, scope);
                    cfd1.orWith(cfd2);
                    return cfd1;
                }
		case TagConstants.TRYCMD: {
                    CmdCmdCmd tryCatchCmd = (CmdCmdCmd) command;
                    GuardedCmd tryCmd = tryCatchCmd.g1;
                    GuardedCmd catchCmd = tryCatchCmd.g2;
                    CFD cfdTry = constructGraph(tryCmd, scope);
                    CFD cfdCatch = constructGraph(catchCmd, scope);
                    Node catchNode = cfdCatch.getInitNode();

            
                    NodeList ens = cfdTry.getExceptionNodes();
			
                    if (ens.isEmpty()) {
                        // no exception node in the try block, 
                        // add the catch block to unreachables
                        unreachable.add(cfdCatch);
                        return cfdTry;
                    } else {
                        // connect the exception nodes to the catch-block
                        for (NodeList.Enumeration e = ens.elements(); e.hasMoreElements();) {
                            ExceptionNode en = (ExceptionNode) e.nextElement();
                            en.connectTo(catchNode);
                        }
                        // join exits of the try and the catch block
                        cfdTry.andExits(cfdTry.getExitNode(), cfdCatch.getExitNode());
                        // the exception nodes of the catch block become exception nodes of the returned graph
                        cfdTry.setExceptionNodes(cfdCatch.getExceptionNodes());
                        return cfdTry;
                    }            
                }
		case TagConstants.SEQCMD: {                    
                    SeqCmd seqCmd = (SeqCmd) command;
                    GuardedCmdVec cmds = seqCmd.cmds;
                    CFD sequence = constructGraph(cmds.elementAt(0), scope);

                    int s = 1;
                    while (s < cmds.size() && sequence.getExitNode() != null) {
                        CFD toAppend = constructGraph(cmds.elementAt(s), scope);
                        sequence.sequence(toAppend);
                        ++s;
                    }

                    // Collect the rest of the sequence into the unreachable nodes
                    // The longest possible sequences will be added to unreachable
                    CFD unreachSeq = null;
                    boolean newChain = true;
                    for (int k = s; k < cmds.size(); k++) {
                        CFD toAppend = constructGraph(cmds.elementAt(k), scope);
                        if (newChain) {
                            unreachSeq = toAppend;
                            unreachable.add(unreachSeq); // add it to the unreachable components
                            newChain = false;
                        } else {
                            if (unreachSeq.getExitNode() == null) {
                                unreachSeq = null; // reset chain
                                newChain = true;
                            } else {
                                unreachSeq.sequence(toAppend);
                            }
                        }

                    }
            

                    return sequence;
                }
		case TagConstants.VARINCMD: {
			VarInCmd vc = (VarInCmd) command;
			GenericVarDeclVec gcVars = vc.v;

			GenericVarDeclVec newScope = scope.copy();
			GenericVarDeclVec cmdScope = gcVars.copy();
			newScope.append(cmdScope);

			return constructGraph(vc.g, newScope);
                }
		default:
			//@ unreachable;
			Assert.fail("Fall thru on " + command);
			return null;

		}
	}
    
    /*================ DBG ============== */
    static Set visitedCmds = null;
    
    static void addToVisitedCmds(Node n) {
        if (visitedCmds.contains(n)) 
            return;
        if (n instanceof CodeNode) {
            CodeNode codeN = (CodeNode) n;
            visitedCmds.add(codeN.getCode());
            
        }
        for (Enumeration en = n.getChildren().elements(); en.hasMoreElements();) {
            Node child = en.nextElement();
            addToVisitedCmds(child);
        }
    }
    
    static int loc = 0;
    static GuardedCmd constructAssume(Expr e) {
        return ExprCmd.make(TagConstants.ASSUMECMD, e, loc++);
    }
    
    static GuardedCmd constructSeq(GuardedCmd c1, GuardedCmd c2) {
        GuardedCmdVec  gv = GuardedCmdVec.make();
        gv.addElement(c1);
        gv.addElement(c2);
        return SeqCmd.make(gv);
    }
    public static void main(String[] args) {
        Set cmds;
        cmds = new HashSet();
        
        GuardedCmd assume1 = constructAssume(GC.truelit); cmds.add(assume1);
        GuardedCmd assume2 = constructAssume(GC.truelit); cmds.add(assume2);
        GuardedCmd assume3 = constructAssume(GC.truelit); cmds.add(assume3);
        GuardedCmd assume4 = constructAssume(GC.truelit); cmds.add(assume4);
        GuardedCmd raise = SimpleCmd.make(TagConstants.RAISECMD, loc++);
        
        GuardedCmd seq2 = constructSeq(assume1, raise);
        GuardedCmd tryBody = constructSeq(seq2, assume4); // assume 4 is unreachable
        GuardedCmd catchBlock = assume2;
        GuardedCmd trycatch =  CmdCmdCmd.make(TagConstants.TRYCMD, tryBody, catchBlock); 
        GuardedCmd seq1 = constructSeq(trycatch, assume3);
               
        GCtoCFDBuilder builder = new GCtoCFDBuilder();
        GuardedCmd program = seq1;
        CFD g = builder.constructGraph(program, null);
        
        visitedCmds = new HashSet();
        
        // Add the unreachable nodes to visited
        // Print unreachable
        System.err.println("Unreachable: ");
        for (Iterator it = builder.unreachable.iterator(); it.hasNext();) {
            CFD ug = (CFD) it.next();
            System.err.println(ug);
            addToVisitedCmds(ug.getInitNode());
        }

        addToVisitedCmds(g.getInitNode());
        
        
        System.err.println("Visited: " + visitedCmds);
        System.err.println("cmds: " + cmds);
        
        OutputStreamWriter ow = new OutputStreamWriter(System.err);
        try {
            ow.write("\n\n=====da dot====\n"); 
            g.printToDot(ow);
            
            // print the unreachable
            for (Iterator it = builder.unreachable.iterator(); it.hasNext();) {
                CFD ug = (CFD) it.next();
                ug.printToDot(ow);
            }
       
            
            ow.flush();
        } catch (IOException e) {
            System.err.println("Can't print to the err output.");
        }
        
        
        //Assert.notFalse(visitedCmds.equals(cmds), "Some commands got lost in the construction.");
        Assert.notFalse(cmds.equals(visitedCmds), "Some commands got lost in the construction.");

    }
    
   
}