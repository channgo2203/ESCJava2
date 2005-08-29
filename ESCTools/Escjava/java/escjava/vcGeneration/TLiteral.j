package escjava.vcGeneration;

class TString extends TLiteral{

    protected String value;

    protected TString (String value){
	this.value = value;
	type = $String;
    }

    public /*@ non_null @*/ StringBuffer toDot(){

	StringBuffer r = super.toDot();

	r.append("\\]\\n"+value);
	r.append("\"];\n");
	
	return r;
    }

}

class TBoolean extends TLiteral{

    protected boolean value;

    protected TBoolean (boolean value){
	this.value = value;
	type = $boolean;
    }

    public /*@ non_null @*/ StringBuffer toDot(){

	StringBuffer r = super.toDot();

	r.append("\\]\\n"+Boolean.toString(value));
	r.append("\"];\n");
	
	return r;
    }

}

class TChar extends TLiteral{

    protected char value;

    protected TChar (int value){
	this.value = (char)value;
	type = $char;
    }

    public /*@ non_null @*/ StringBuffer toDot(){

	StringBuffer r = super.toDot();

	r.append("\\]\\n"+Character.toString(value));
	r.append("\"];\n");
	
	return r;
    }

}

class TInt extends TLiteral {
    
    protected int value;
    
    protected TInt(int value){
	this.value = value;
	type = $integer;
    }

    public /*@ non_null @*/ StringBuffer toDot(){

	StringBuffer r = super.toDot();

	r.append("\\]\\n"+Integer.toString(value));
	r.append("\"];\n");
	
	return r;
    }

}

class TFloat extends TLiteral {
    
    protected  float value;
    
    protected TFloat(float value){
	this.value = value;
	type = $float;
    }

    public /*@ non_null @*/ StringBuffer toDot(){

	StringBuffer r = super.toDot();

	r.append("\\]\\n"+Float.toString(value));
	r.append("\"];\n");
	
	return r;
    }

}

class TDouble extends TLiteral {
    
    protected  double value;
    
    protected TDouble(double value){
	this.value = value;
	type = $float;
    }

    public /*@ non_null @*/ StringBuffer toDot(){

	StringBuffer r = super.toDot();

	r.append("\\]\\n"+Double.toString(value));
	r.append("\"];\n");
	
	return r;
    }

}

class TNull extends TLiteral {
    
    protected Object value = null;
    
    protected TNull(){
	type = $Reference;
    }

    public /*@ non_null @*/ StringBuffer toDot(){

	StringBuffer r = super.toDot();

	r.append("\\]\\nnull\"];\n");
	
	return r;
    }

}

