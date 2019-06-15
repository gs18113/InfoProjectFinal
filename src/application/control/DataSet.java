package application.control;

import java.io.Serializable;
import java.util.ArrayList;

public abstract class DataSet implements Serializable {
	ArrayList<Blank> data;
	String savedir;
	String dataType;
	
	public DataSet(String savedir){
		data = new ArrayList<Blank>();
		this.savedir = savedir;
	}
	
	public void addData(Blank newData) {
		data.add(newData);
	}
	
	/*
	public abstract void loadData(String filename);

	public abstract void saveData(String filename);
	*/
}
