public class ModChecks5 {

	public int i;

	public int j; //@ in i;

	//@ modifies i;
	void m() {
		j = 0;
		i = 0;
	}
}


// FIXME - test multiple items in an in
// in referring to super class
// static in
// maps
