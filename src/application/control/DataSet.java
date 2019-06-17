package application.control;

import java.io.Serializable;
import java.util.ArrayList;

public class DataSet implements Serializable {
	private ArrayList<Blank> blanks;
	String savedir;
	String dataType;
	
	public DataSet(String savedir){
		blanks = new ArrayList<Blank>();
		this.savedir = savedir;
	}
	
	public synchronized void addData(Blank newData) {
		blanks.add(newData);
	}

	public ArrayList<Blank> getBlanks() {
		return blanks;
	}

	/*
	public abstract void loadData(String filename);

	public abstract void saveData(String filename);
	*/
}
