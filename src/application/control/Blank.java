package application.control;

import java.io.Serializable;

public class Blank implements Serializable {
	private String str;
	private int wrong;

	Blank(String str){
		this.str = str;
		wrong=0;
	}

	public void increaseWrong(){
		wrong++;
	}

	public void decreaseWrong(){
		wrong--;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public String getStr() {
		return str;
	}

	public int getWrong(){
		return wrong;
	}
}
