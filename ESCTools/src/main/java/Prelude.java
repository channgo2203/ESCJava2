package escjava.vcGeneration.coq;

import java.io.*;

/**
 * This class is used to generates the Coq prelude to the specified 
 * path. The prelude is taken from the file 
 * <code>$ESCTOOLS/docs/Escjava2-logics/coq/defs.v</code>.
 * @author J.Charles
 * @version 14/11/2005
 */
public class Prelude {
   private File f;
   
   /**
    * Creates a new instance with the specified path (the name of
    * the file to generate).
    * @param path where to generate the 'prelude'.
    */
   public Prelude(File path) {
	   f = path;
   }

   /**
    * Write the file to disk.
    * @throws IOException if an I/O error occurs.
    */
   public void generate() throws IOException {
	   PrintStream out = new PrintStream(new FileOutputStream(f));
	   

	   out.println("(* Esc Java  - Coq extension  Sorted logic. *)");
	   out.println("");
	   out.println("");
	   out.println("Require Import Bool.");
	   out.println("Require Import ZArith.");
	   out.println("Require Import Zdiv.");
	   out.println("Require Import Zbool.");
	   out.println("Require Import Sumbool.");
	   out.println("Require Import Reals.");
	   out.println("Require Import Classical.");
	   out.println("Require Import String.");
	   out.println("Open Scope Z_scope.");
	   out.println("");
	   out.println("Load \"defs_types.v\".");
	   out.println("Module EscJavaTypesDef <: EscJavaTypes.");
	   out.println("Definition Boolean:= bool.");
	   out.println("Lemma Boolean_dec: forall x y : Boolean, {x = y}+{x <> y}.");
	   out.println("Proof with auto.");
	   out.println("    intros; destruct x; destruct y...");
	   out.println("    right; intro; discriminate.");
	   out.println("Qed.");
	   out.println("");
	   out.println("Definition java_lang_Boolean_TRUE:= true.");
	   out.println("Definition java_lang_Boolean_FALSE:= false.");
	   out.println("");
	   out.println("Definition time := Z.");
	   out.println("Definition time_dec (x: time) (y: time) := (Z_eq_dec x y).");
	   out.println("Module Time <: Arith.");
	   out.println("     Definition t := time.");
	   out.println("    Definition GE := fun (x y : Z) => x >= y.");
	   out.println("    Definition GT := fun (x y : Z) =>  x > y.");
	   out.println("    Definition LE  := fun (x y : Z) => x <= y.");
	   out.println("    Definition LT  := fun (x y : Z) => x < y.");
	   out.println("    Definition Div := ");
	   out.println("        fun (x y : Z) =>(x  / y).");
	   out.println("    Definition Add  := ");
	   out.println("        fun (x y : Z) =>(x  + y).");
	   out.println("    Definition Sub := ");
	   out.println("        fun (x y : Z) =>(x - y).");
	   out.println("    Definition Mul := ");
	   out.println("        fun (x y : Z) =>(x  * y).");
	   out.println("    Definition Neg := ");
	   out.println("        fun (x : Z) =>(0 - x).");
	   out.println("End Time.");
	   out.println("");
	   out.println("");
	   out.println("Definition DiscreteNumber := Z.");
	   out.println("Definition DiscreteNumber_dec (x: DiscreteNumber) (y: DiscreteNumber) := (Z_eq_dec x y).");
	   out.println("Module Discrete <: Arith.");
	   out.println("    Definition t := DiscreteNumber.");
	   out.println("");
	   out.println("    Definition GE : DiscreteNumber -> DiscreteNumber -> Prop := fun (x y : Z) => x >= y.");
	   out.println("    Definition GT : DiscreteNumber -> DiscreteNumber -> Prop := fun (x y : Z) =>  x > y.");
	   out.println("    Definition LE : DiscreteNumber-> DiscreteNumber -> Prop := fun (x y : Z) => x <= y.");
	   out.println("    Definition LT : DiscreteNumber -> DiscreteNumber -> Prop := fun (x y : Z) => x < y.");
	   out.println("");
	   out.println("    Definition Div :  DiscreteNumber -> DiscreteNumber -> DiscreteNumber := ");
	   out.println("        fun (x y : Z) =>(x  / y).");
	   out.println("    Definition Add : DiscreteNumber -> DiscreteNumber -> DiscreteNumber := ");
	   out.println("        fun (x y : Z) =>(x  + y).");
	   out.println("    Definition Sub : DiscreteNumber -> DiscreteNumber -> DiscreteNumber := ");
	   out.println("        fun (x y : Z) =>(x - y).");
	   out.println("    Definition Mul : DiscreteNumber -> DiscreteNumber -> DiscreteNumber := ");
	   out.println("        fun (x y : Z) =>(x  * y).");
	   out.println("    Definition Neg : DiscreteNumber -> DiscreteNumber := ");
	   out.println("        fun (x : Z) =>(0 - x).");
	   out.println("End Discrete.");
	   out.println("");
	   out.println("Module Discrete_bool <: Arith_bool.");
	   out.println("    Definition t := DiscreteNumber.");
	   out.println("    Module decType <: DecType.");
	   out.println("        Definition t := DiscreteNumber.");
	   out.println("        Definition t_dec := DiscreteNumber_dec.");
	   out.println("    End decType.");
	   out.println("");
	   out.println("    Module dec := Dec decType.");
	   out.println("    Definition EQ : DiscreteNumber -> DiscreteNumber -> bool := dec.EQ.");
	   out.println("    Definition GE : DiscreteNumber -> DiscreteNumber -> bool := fun (x y : Z) => Zge_bool x y.");
	   out.println("    Definition GT : DiscreteNumber -> DiscreteNumber -> bool := fun (x y : Z) => Zgt_bool x y.");
	   out.println("    Definition LE : DiscreteNumber -> DiscreteNumber -> bool := fun (x y : Z) =>  Zle_bool x y.");
	   out.println("    Definition LT : DiscreteNumber -> DiscreteNumber -> bool := fun (x y : Z) => Zlt_bool x y.");
	   out.println("");
	   out.println("    Definition EQ_refl:= dec.EQ_refl.");
	   out.println("");
	   out.println("    Definition EQ_eq := dec.EQ_eq.");
	   out.println("    Definition EQ_true := dec.EQ_true.");
	   out.println("    Definition EQ_false := dec.EQ_false.");
	   out.println("    Definition EQ_eq_not_false := dec.EQ_eq_not_false.");
	   out.println("    Definition EQ_exists_eq := dec.EQ_exists_eq.");
	   out.println("    Definition EQ_false_not_eq := dec.EQ_false_not_eq.");
	   out.println("");
	   out.println("End Discrete_bool.");
	   out.println("Definition dzero: DiscreteNumber := 0.");
	   out.println("");
	   out.println("(* TODO: treat ContinuousNumbers, Real,...*)");
	   out.println("Parameter ContinuousNumber : Set.");
	   out.println("Variable czero : ContinuousNumber.");
	   out.println("Parameter RealNumber : Set.");
	   out.println("Variable rzero: RealNumber.");
	   out.println("Parameter BigIntNumber : Set.");
	   out.println("Variable bzero: BigIntNumber.");
	   out.println("");
	   out.println("(* Java numbers are Continuous or Discrete *)");
	   out.println("Inductive javaNumber: Set := ");
	   out.println("| java_discrete: DiscreteNumber -> javaNumber");
	   out.println("| java_continuous: ContinuousNumber -> javaNumber.");
	   out.println("Definition JavaNumber := javaNumber.");
	   out.println("Definition JavaNumber_to_DiscreteNumber (n: JavaNumber): DiscreteNumber :=");
	   out.println("   match n with");
	   out.println("   | java_discrete d => d");
	   out.println("   | _ => dzero");
	   out.println("   end.");
	   out.println("Definition JavaNumber_to_ContinuousNumber (n: JavaNumber) : ContinuousNumber :=");
	   out.println("   match n with");
	   out.println("   | java_continuous c => c");
	   out.println("   | _ => czero");
	   out.println("   end.");
	   out.println("");
	   out.println("(* JMLNumber are JavaNumber, RealNumber, BigIntNumber *)");
	   out.println("Inductive jMLNumber: Set :=");
	   out.println("| jml_java : JavaNumber -> jMLNumber");
	   out.println("| jml_real : RealNumber -> jMLNumber");
	   out.println("| jml_bigint: BigIntNumber -> jMLNumber.");
	   out.println("Definition JMLNumber:= jMLNumber.");
	   out.println("Parameter JMLNumber_to_JavaNumber: JMLNumber -> JavaNumber.");
	   out.println("Parameter JMLNumber_to_RealNumber: JMLNumber -> RealNumber.");
	   out.println("Parameter JMLNumber_to_BigIntNumber: JMLNumber -> BigIntNumber.");
	   out.println("");
	   out.println("(* Numbers are JMLNumbers... don't see the point *)");
	   out.println("Definition Number:= JMLNumber.");
	   out.println("Definition Number_to_JMLNumber: Number -> JMLNumber :=");
	   out.println("   fun v => v.");
	   out.println("");
	   out.println("(* BaseType can be converted to boolean or to JavaNumber *)");
	   out.println("Parameter BaseType: Set.");
	   out.println("Parameter BaseType_to_Boolean: BaseType -> Boolean.");
	   out.println("Parameter BaseType_to_JavaNumber: BaseType -> JavaNumber.");
	   out.println("Variable T_byte: BaseType.");
	   out.println("Variable T_char: BaseType.");
	   out.println("Variable T_short: BaseType.");
	   out.println("Variable T_int: BaseType.");
	   out.println("Variable T_long: BaseType.");
	   out.println("Variable T_float: BaseType.");
	   out.println("Variable T_double: BaseType.");
	   out.println("");
	   out.println("");
	   out.println("Definition RefType := Z.");
	   out.println("Definition RefType_dec (x: RefType) (y: RefType) := (Z_eq_dec x y).");
	   out.println("");
	   out.println("Inductive reference: Set :=");
	   out.println("| var_ref: string -> time -> reference");
	   out.println("| field_ref: reference -> time -> reference");
	   out.println("| elem_ref: reference -> time  -> reference.");
	   out.println("");
	   out.println("Definition Reference := reference.");
	   out.println("");
	   out.println("Definition Reference_dec: forall x y : Reference, {x = y} + {x <> y}.");
	   out.println("Proof with auto.");
	   out.println("  induction x; ");
	   out.println("  induction y;");
	   out.println("  intros; try ( right; intro h; discriminate);");
	   out.println("  try (");
	   out.println("  assert( h:= string_dec s s0); destruct h;");
	   out.println("  [assert( h1:= Z_dec t t0); destruct h1; subst; auto;");
	   out.println("  destruct s1;");
	   out.println("  right;");
	   out.println("  intro h; injection h; intros; subst; omega |");
	   out.println("  right; intro h; injection h; intros; subst; destruct n; auto ]).");
	   out.println("");
	   out.println("assert (h:= IHx y);");
	   out.println("destruct h as [h1 | h2]; subst;");
	   out.println("assert( h1:= Z_dec t t0); [");
	   out.println("destruct h1 as [s| s]; subst; auto; destruct s; ");
	   out.println("right; intro h2; injection h2; intros; subst; auto; omega |");
	   out.println("right; intro h; injection h; intros; subst; destruct h2; auto].");
	   out.println("");
	   out.println("assert (h:= IHx y);");
	   out.println("destruct h as [h1 | h2]; subst;");
	   out.println("assert( h1:= Z_dec t t0); [");
	   out.println("destruct h1 as [s| s]; subst; auto; destruct s; ");
	   out.println("right; intro h2; injection h2; intros; subst; auto; omega |");
	   out.println("right; intro h; injection h; intros; subst; destruct h2; auto].");
	   out.println("Qed.");
	   out.println("Definition null : Reference :=  var_ref EmptyString 0.");
	   out.println("");
	   out.println("");
	   out.println("Variable java_lang_Object: RefType.");
	   out.println("Variable java_lang_reflect_Array: RefType.");
	   out.println("Variable java_lang_Cloneable: RefType.");
	   out.println("Module Types.");
	   out.println("    Parameter subtype: RefType -> RefType-> Prop.");
	   out.println("    Parameter extends: RefType -> RefType -> Prop.");
	   out.println("    Parameter typeof : Reference -> RefType.");
	   out.println("    Parameter elemRefType: RefType -> RefType.");
	   out.println("    Parameter isa: Reference -> RefType -> Prop.");
	   out.println("    Axiom subtypes_includes_extends:");
	   out.println("          forall (t u: RefType), extends t u -> subtype t u.");
	   out.println("    Axiom subtype_is_the_smallest_relation_that_contains_extends:");
	   out.println("          forall (t u: RefType), (subtype t u /\\ (t <> u)) ->");
	   out.println("               exists v: RefType, (extends t v /\\ subtype v u).");
	   out.println("    Axiom java_lang_Object_is_Top:");
	   out.println("         forall (t: RefType), subtype t java_lang_Object.");
	   out.println("    Axiom unique_dynamic_subtype:");
	   out.println("         forall (r: Reference) (t: RefType),");
	   out.println("              isa r t <-> (r = null) \\/ (typeof r = t).");
	   out.println("    Parameter instantiable: RefType -> Prop.");
	   out.println("    Axiom instantiable_def:");
	   out.println("          forall (t: RefType), subtype t java_lang_Object -> instantiable t.");
	   out.println("End Types.");
	   out.println("");
	   out.println("");
	   out.println("Definition LockMap := Z.");
	   out.println("Variable LS: LockMap.");
	   out.println("Variable maxLockSet: Reference.");
	   out.println("Definition LockMap_dec : ");
	   out.println("           forall x y : LockMap, {x = y} + {x <> y} := Z_eq_dec.");
	   out.println("Module Lock.");
	   out.println("    Parameter less : Reference -> Reference -> Prop.");
	   out.println("    Parameter lock: LockMap -> Reference -> LockMap.");
	   out.println("    Parameter release : LockMap -> Reference -> LockMap.");
	   out.println("    Parameter select: LockMap -> Reference -> Prop.");
	   out.println("    Axiom access_definition1: ");
	   out.println("           forall (r: Reference), not (select (lock LS r) r).");
	   out.println("    Axiom access_definition2:");
	   out.println("           forall (r: Reference), select (release (lock LS r) r) r.");
	   out.println("    Axiom ls_has_a_maximum_element:");
	   out.println("           forall (r: Reference), (less r maxLockSet).");
	   out.println("    Axiom null_belongs_to_ls:");
	   out.println("           forall (r: Reference), less null r.");
	   out.println("End Lock.");
	   out.println("");
	   out.println("");
	   out.println("Definition Elem := Reference.");
	   out.println("Definition Field := Reference.");
	   out.println("");
	   out.println("");
	   out.println("Definition Path := string.");
	   out.println("Definition Path_dec (x: Path) (y: Path) := (string_dec x y).");
	   out.println("");
	   out.println("Definition zero: DiscreteNumber := 0%Z.");
	   out.println("");
	   out.println("");
	   out.println("");
	   out.println("Open Scope string_scope.");
	   out.println("Definition ecReturn : Path := \"ecReturn\".");
	   out.println("Definition ecThrow : Path := \"ecThrow\".");
	   out.println("Definition allocNew_ : Reference := var_ref \"allocNew\" 0.");
	   out.println("Definition alloc_ : Reference := var_ref \"alloc\" 0.");
	   out.println("Close Scope string_scope.");
	   out.println("");
	   out.println("Inductive S: Set  := ");
	   out.println("    |   bool_to_S: bool -> S");
	   out.println("    |   DiscreteNumber_to_S : DiscreteNumber -> S");
	   out.println("    |   Reference_to_S: Reference -> S");
	   out.println("    |   Path_to_S: Path -> S");
	   out.println("    |   Time_to_S: time -> S");
	   out.println("    |   RefType_to_S: RefType -> S");
	   out.println("    |    bottom: S.");
	   out.println("");
	   out.println("Coercion bool_to_S : bool >-> S.");
	   out.println("Coercion DiscreteNumber_to_S : DiscreteNumber >-> S.");
	   out.println("Coercion Reference_to_S : Reference >-> S.");
	   out.println("Coercion Time_to_S : time >-> S.");
	   out.println("Coercion Path_to_S : Path >-> S.");
	   out.println("Coercion RefType_to_S : RefType >-> S.");
	   out.println("");
	   out.println("Definition S_to_bool (s:S): bool :=");
	   out.println("match s with ");
	   out.println("| bool_to_S b => b");
	   out.println("| _ => false");
	   out.println("end.");
	   out.println("Coercion S_to_bool : S >-> bool.");
	   out.println("");
	   out.println("Definition S_to_DiscreteNumber (s:S): DiscreteNumber :=");
	   out.println("match s with ");
	   out.println("| DiscreteNumber_to_S b => b");
	   out.println("| _ => 0%Z");
	   out.println("end.");
	   out.println("Coercion S_to_DiscreteNumber : S >-> DiscreteNumber.");
	   out.println("");
	   out.println("");
	   out.println("Definition S_to_Reference (s:S): Reference :=");
	   out.println("match s with ");
	   out.println("| Reference_to_S b => b");
	   out.println("| _ => null");
	   out.println("end.");
	   out.println("Coercion S_to_Reference : S >-> Reference.");
	   out.println("");
	   out.println("Definition emptyPath : Path :=  EmptyString.");
	   out.println("Definition S_to_Path (s:S): Path :=");
	   out.println("match s with ");
	   out.println("| Path_to_S b => b");
	   out.println("| _ => emptyPath");
	   out.println("end.");
	   out.println("Coercion S_to_Path : S >-> Path.");
	   out.println("");
	   out.println("Definition S_to_Time (s:S): time :=");
	   out.println("match s with ");
	   out.println("| Time_to_S b => b");
	   out.println("| _ => 0");
	   out.println("end.");
	   out.println("Coercion S_to_Time : S >-> time.");
	   out.println("Definition S_to_RefType (s:S): RefType :=");
	   out.println("match s with ");
	   out.println("| RefType_to_S b => b");
	   out.println("| _ => 0");
	   out.println("end.");
	   out.println("Coercion S_to_RefType : S >-> RefType.");
	   out.println("");
	   out.println("Definition AnySet := S.");
	   out.println("");
	   out.println("Definition cast : Reference -> RefType -> Reference := ");
	   out.println(" fun (v:Reference)  (t: RefType) => v.");
	   out.println("");
	   out.println("");
	   out.println("");
	   out.println("");
	   out.println("Definition distinctAxiom : ecReturn <> ecThrow.");
	   out.println("Proof.");
	   out.println("   intro.");
	   out.println("   discriminate.");
	   out.println("Qed.");
	   out.println("Hint Immediate distinctAxiom.");
	   out.println("");
	   out.println("");
	   out.println("");
	   out.println("(*Variable is : S -> S -> Prop. *)");
	   out.println("Definition isField (r: Reference ) : Prop :=");
	   out.println("     match r with ");
	   out.println("     | field_ref _ _ => True");
	   out.println("     | _ => False");
	   out.println("     end.");
	   out.println("");
	   out.println("");
	   out.println("Variable asField: Field -> RefType -> Prop.");
	   out.println("Variable asElems: Elem -> Elem.");
	   out.println("Variable asLockSet: LockMap -> LockMap.");
	   out.println("");
	   out.println("Definition eClosedTime (e: Elem) : time :=");
	   out.println("   match e with ");
	   out.println("   | elem_ref _ t => t");
	   out.println("   | _ => zero");
	   out.println("   end.");
	   out.println("");
	   out.println("Definition fClosedTime (f: Field) : time :=");
	   out.println("   match f with ");
	   out.println("   | field_ref _ t => t");
	   out.println("   | _ => zero");
	   out.println("   end.");
	   out.println("");
	   out.println("Fixpoint vAllocTime (r : Reference) {struct r}: time :=");
	   out.println("    match r with");
	   out.println("    | var_ref n t => t");
	   out.println("    | elem_ref r' _ => vAllocTime r'");
	   out.println("    | field_ref r' _ => vAllocTime r'");
	   out.println("    end.");
	   out.println("");
	   out.println("Definition isAllocated: Reference -> time -> Prop := ");
	   out.println("    fun (obj : Reference) (t: time) => vAllocTime obj < t.");
	   out.println("Ltac unfoldEscTime := unfold isAllocated.");
	   out.println("");
	   out.println("");
	   out.println("(* Array Logic *)");
	   out.println("Module array.");
	   out.println("Variable shapeOne: DiscreteNumber -> Reference.");
	   out.println("Variable shapeMore: DiscreteNumber-> Reference -> Reference.");
	   out.println("Variable isNew: Reference -> Prop.");
	   out.println("Variable length: Reference -> DiscreteNumber.");
	   out.println("Axiom lengthAx :");
	   out.println("      forall (a : Reference), (Discrete.LE 0 (length a)).");
	   out.println("Variable select: Reference -> DiscreteNumber -> S.");
	   out.println("Variable store: Reference -> DiscreteNumber -> S -> Reference.");
	   out.println("Axiom select_store1: ");
	   out.println("    forall(var :Reference) (obj : DiscreteNumber)(val :AnySet), (select (store var obj val) obj) = val.");
	   out.println("Axiom select_store2: ");
	   out.println("    forall(var : Reference)(obj1 obj2 :DiscreteNumber) (val :  AnySet), ");
	   out.println("         obj1 <> obj2 -> ");
	   out.println("                 (select (store var obj1 val) obj2) = (select var obj2).");
	   out.println("");
	   out.println("Variable fresh : Reference -> time -> time -> Reference -> Reference -> RefType ->  AnySet (* A *) -> Prop.");
	   out.println("    (* array axioms2 *)");
	   out.println("    Axiom axiom2 : ");
	   out.println("      forall (array: Reference) ( a0:time) (b0:time) (obj : Reference) (n : DiscreteNumber)  (T: RefType)  (v :  AnySet),");
	   out.println("        ((fresh array a0 b0 obj (shapeOne n)  T v) ->");
	   out.println("         (Time.LE a0 (vAllocTime array))) /\\");
	   out.println("        ((fresh array  a0  b0 obj (shapeOne n) T v) ->");
	   out.println("         (isAllocated array  b0)) /\\");
	   out.println("        ((fresh array  a0  b0 obj (shapeOne n) T v) ->");
	   out.println("         (array <> null)) /\\");
	   out.println("        ((fresh array  a0  b0 obj (shapeOne n) T v) ->");
	   out.println("         (Types.typeof array) = T) /\\");
	   out.println("        ((fresh array  a0  b0 obj (shapeOne n) T v) ->");
	   out.println("         (length array) = n) (* /\\");
	   out.println("        ((arrayFresh a  a0  b0 e (arrayShapeOne n) T v) ->");
	   out.println("         forall (i : Reference),");
	   out.println("           (select (select e a) i) = v) *).");
	   out.println("End array.");
	   out.println("");
	   out.println("");
	   out.println("(* The operations on the heap - more or less *)");
	   out.println("Inductive java_heap : Set := ");
	   out.println("| heap_store: java_heap -> Reference ->  S -> java_heap");
	   out.println("| heap_empty: java_heap.");
	   out.println("");
	   out.println("Module refType <: DecType.");
	   out.println("Definition t := Reference.");
	   out.println("Definition t_dec := Reference_dec.");
	   out.println("End refType.");
	   out.println("");
	   out.println("Module ref := Dec refType.");
	   out.println("Definition heap := java_heap.");
	   out.println("Definition store := heap_store.");
	   out.println("Fixpoint select (h: heap) (r: Reference) {struct h} : S :=");
	   out.println("match h with ");
	   out.println("| heap_store h r' v => if (ref.EQ r r') then v else select h r");
	   out.println("| heap_empty => bottom");
	   out.println("end.");
	   out.println("");
	   out.println("");
	   out.println("Definition select_store1: ");
	   out.println("    forall(var: heap) (obj : Reference)(val : AnySet), (select (store var obj val) obj) = val.");
	   out.println("Proof.");
	   out.println("intros.");
	   out.println("simpl.");
	   out.println("rewrite ref.EQ_refl; trivial.");
	   out.println("Qed.");
	   out.println("");
	   out.println("Definition select_store2: ");
	   out.println("    forall(var: heap) (obj1 obj2 :Reference) (val :  AnySet), ");
	   out.println("         obj1 <> obj2 -> ");
	   out.println("                 (select (store var obj1 val) obj2) = (select var obj2).");
	   out.println("Proof.");
	   out.println("intros.");
	   out.println("simpl.");
	   out.println("rewrite ref.EQ_false; trivial.");
	   out.println("intro; subst.");
	   out.println("apply H; trivial.");
	   out.println("Qed.");
	   out.println("");
	   out.println("");
	   out.println("");
	   out.println("");
	   out.println("End EscJavaTypesDef.");
	   out.println("");
	   out.println("");
	   out.println("");

	   out.close();
   }
}

