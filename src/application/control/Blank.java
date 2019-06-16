package application.control;

import java.io.Serializable;

public class Blank implements Serializable {
	String str;
	Blank(String str){
		this.str = str;
	}

	public String getStr() {
		return str;
	}
}
