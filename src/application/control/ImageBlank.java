package application.control;


import javafx.scene.shape.Rectangle;

public class ImageBlank extends Blank {

	private Rectangle rect;

	public Rectangle getRect() {
		return rect;
	}

	ImageBlank(Rectangle rect, String str) {
		super(str);
		this.rect = rect;
	}

}
