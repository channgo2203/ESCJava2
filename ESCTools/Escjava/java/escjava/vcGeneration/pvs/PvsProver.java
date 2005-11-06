package escjava.vcGeneration.pvs;

import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import escjava.vcGeneration.*;

public class PvsProver implements ProverType {

    public TVisitor visitor() {
        return new TPvsVisitor();
    }

    public String getProof(String proofName, String declns, String vc) {
        // generate declarations
        StringBuffer s = new StringBuffer();

        s.append(proofName + " : CONJECTURE\n"); // let's be modest...

        s.append("ForAll(\n");

        s.append(declns);

        s.append(") :\n\n");

        // add the proof
        s.append(vc);

        return s.toString();
    }

    public String getVariableInfo(VariableInfo caller) {

        if (caller.type != TNode.$Type) {
            if (caller.def == null)
                pvsRename(caller);

            return caller.def;
        } else {
            /*

             When variable is a type, we first have to check if it's in the type table.
             If yes, we take the name here (it's the case with predefined types like INTYPE, integer, Reference etc...

             Else it's normally a user defined Type

             */

            Set keySet = TNode.typesName.keySet();
            Iterator iter = keySet.iterator();
            TypeInfo tiTemp = null;
            String keyTemp = null;

            while (iter.hasNext()) {

                try {
                    keyTemp = (String) iter.next();
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
                tiTemp = (TypeInfo) TNode.typesName.get(keyTemp);

                if (tiTemp.old.equals(caller.old)) {
                    return getTypeInfo(tiTemp);
                }

            }

            TDisplay
                    .warn(
                            this,
                            "getTypeInfo()",
                            "Considering "
                                    + caller.old
                                    + " as a user defined type, or a not (yet) handled variable.");

            pvsRename(caller);

            return caller.def;
        }

    }

    /*@
     @ ensures pvs != null;
     @*/
    private void pvsRename(VariableInfo caller) {

        // definitions of different regexp used.
        Pattern p1 = Pattern.compile("\\|(.*):(.*)\\.(.*)\\|");
        Matcher m1 = p1.matcher(caller.old);

        // <variables not handled>
        Pattern p2 = Pattern.compile("Unknown tag <.*>");
        Matcher m2 = p2.matcher(caller.old);

        Pattern p3 = Pattern.compile("\\|brokenObj<.*>\\|");
        Matcher m3 = p3.matcher(caller.old);
        // </variables not handled>

        String name = null;
        String line = null;
        String column = null;

        int i = 0;

        //System.out.println("Performing regular expression matching against "+old+" ");

        /*
         This case is done first because some variables not handled are "%Type" ones
         and thus otherwise they will be matched by the next if construct
         */
        if (m2.matches() || m3.matches() || caller.old.equals("brokenObj")) { // variable is not handled yet, ask David Cok about
            // some things
            caller.def = "%NotHandled";
        }

        // <case 1>
        /*
         If variable's a type, we must prefix his renaming by userDef?.
         Why ?
         Because if the user defined a type named 'Reference', otherwise we will use is as it
         in the proof and it will interfer with our predefined types.

         Since '?' isn't a valid character in the java notation and is Ok for pvs,
         we use it to make the difference.
         */
        else if (caller.type == TNode.$Type) {
            TDisplay.warn(this, "pvsRename()", "Considering " + caller.old
                    + " as a user defined type.");

            // renaming done here
            caller.def = "userDef?" + caller.old;
        }
        // </case 1>

        // <case 2>, capturing |y:8.31|
        else if (m1.matches()) {
            //@ assert m.groupCount() == 3;

            if (m1.groupCount() != 3)
                TDisplay.err(this, "pvsRename()", "m.groupCount() != 3");

            for (i = 1; i <= m1.groupCount(); i++) {

                if (m1.start(i) == -1 || m1.end(i) == -1)
                    TDisplay.err(this, "pvsRename()",
                            "Return value of regex matching is -1");
                else {

                    String temp = caller.old.substring(m1.start(i), m1.end(i));
                    //@ assert temp != null;

                    switch (i) {
                    case 1:
                        name = temp;
                        //@ assert name != null;
                        break;
                    case 2:
                        line = temp;
                        //@ assert line != null;
                        break;
                    case 3:
                        column = temp;
                        //@ assert column != null;
                        break;
                    default:
                        TDisplay.err(this, "pvsRename()",
                                "Switch call incorrect, switch on value " + i);
                        break;
                    }
                } // no error in group

            } // checking all group

            /* renaming done here, if you want the way it's done
             (for pattern like |y:8.31|)
             do it here. */
            caller.def = name + "_" + line + "_" + column;

        } // </case 2>
        else {
            caller.def = caller.old; // FIXME handle everything
        } // regexp didn't match
    }

    public String getTypeInfo(TypeInfo caller) {
        if (caller.def == null)
            pvsRename(caller);
        return caller.def;
    }

    /*@
     @ ensures pvs != null;
     @*/
    private void pvsRename(TypeInfo caller) {

        if (caller.old.equals("null"))
            caller.def = "Reference";
        else {
            // common rules here //fixme, be more specific maybe
            if (caller.old.startsWith("java.")) //check if in the form java.x.y 
                caller.def = caller.old.replace('.', '_');
            else {
                TDisplay.warn(this, "pvsRename()", "Type not handled  : "
                        + caller.old);
                TDisplay
                        .warn(this, "pvsRename()",
                                "Considering it as a user defined type... ie ReferenceType");
                caller.def = "ReferenceType";
            }
        }
    }

    public void init() {
        // Predefined types

        TNode.$Reference = TNode.addType("%Reference", "Reference");
        TNode.$Time = TNode.addType("%Time", "Time");
        TNode.$Type = TNode.addType("%Type", "ReferenceType");
        TNode.$boolean = TNode.addType("boolean", "Boolean");
        TNode.$char = TNode.addType("char", "T_char");
        TNode.$DOUBLETYPE = TNode.addType("DOUBLETYPE", "ContinuousNumber"); // fixme, is it JavaNumber or BaseType ?
        TNode.$double = TNode.addType("double", "ContinuousNumber"); //fixme
        TNode.$Field = TNode.addType("%Field", "Field"); // fixme there's a lot of different fields in the pvs logic, I need to capture that
        TNode.$INTTYPE = TNode.addType("INTTYPE", "T_int"); //fixme like DOUBLETYPE
        TNode.$integer = TNode.addType("integer", "DiscreteNumber"); //fixme
        TNode.$float = TNode.addType("float", "ContinuousNumber");
        TNode.$Path = TNode.addType("%Path", "Path"); // used to modelize different ways
        // of terminating a function
        //$String = addType("String" "String"); fixme, does this type appears in original proof ?

        // Predefined variables name
        // variables used by the old proof system and that we still need
        TNode.addName("ecReturn", "%Path", "preDef?ecReturn");
        TNode.addName("ecThrow", "%Path", "preDef?ecThrow");
        TNode.addName("XRES", "%Reference", "preDef?XRes");
    }
    
    public void rewrite(TNode tree) {
        TProofSimplifier psvi = new TProofSimplifier();
        tree.accept(psvi);
    }
}
