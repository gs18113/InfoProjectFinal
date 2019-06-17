package application.control;

import java.io.Serializable;

public class Blank implements Serializable {
	String str;
	int wrong;

	Blank(String str){
		this.str = str;
		wrong=0;
	}

	public String getStr() {
		return str;
	}

	public int getWrong(){
		return wrong;
	}
}
