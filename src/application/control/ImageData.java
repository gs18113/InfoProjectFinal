package application.control;

import javafx.scene.image.Image;

public class ImageData extends DataSet {

	Image fullImage;

	public ImageData(String savedir, Image fullImage) {
		super(savedir);
		this.fullImage = fullImage;
		this.dataType = "Image";
	}

	/*
	@Override
	public void loadData(String filename) {
		try {
			File file = new File(savedir+filename);
			FileInputStream fos = new FileInputStream(file.getAbsolutePath());
			ObjectInputStream oos = new ObjectInputStream(fos);
			
			fullImage = (Image)oos.readObject();
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
			
			oos.writeObject(fullImage);
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
