package application.control;

public class TextData extends DataSet {
	
	String fullText;
	
	public TextData(String savedir, String fullText){
		super(savedir);
		this.fullText = fullText;
		this.dataType = "Text";
	}
	
	/*
	@Override
	public void loadData(String filename) {
		try {
			File file = new File(savedir+filename);
			FileInputStream fos = new FileInputStream(file.getAbsolutePath());
			ObjectInputStream oos = new ObjectInputStream(fos);
			
			fullText = (String)oos.readObject();
			for(int i=0;i<data.size();i++) {
				data.add((PracticeData)oos.readObject());
			}
			oos.close();
			fos.close();
		} catch(Exception e){
			System.out.println(e.getMessage());
		}
		
	}

	@Override
	public void saveData(String filename) {
		
		try {
			File file = new File(savedir+filename);
			file.createNewFile();
			FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			
			oos.writeObject(fullText);
			for(int i=0;i<data.size();i++) {
				oos.writeObject(data.get(i));
			}
			oos.flush();
			oos.close();
			fos.close();
		} catch(Exception e){
			System.out.println(e.getMessage());
		}
		
	}
	*/
}
