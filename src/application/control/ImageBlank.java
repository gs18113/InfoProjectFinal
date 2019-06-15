package application.control;


import javafx.scene.shape.Rectangle;

public class ImageBlank extends Blank {

	Rectangle rect;

	ImageBlank(Rectangle rect, String str) {
		super(str);
		this.rect = rect;
	}

}
