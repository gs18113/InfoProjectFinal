package application.control;


import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;


public class ImageBlank extends Blank {

	private double p, q, r, s;

	public Rectangle getRect() {
	    Rectangle rect = new Rectangle(p, q, r, s);
		rect.setStroke(Color.BLUE);
		rect.setFill(Color.LIGHTGRAY.deriveColor(0, 0, 1, 0.5));
		return rect;
	}

	ImageBlank(Bounds bounds, String str) {
		super(str);
		this.p = bounds.getMinX();
		this.q = bounds.getMinY();
		this.r = bounds.getWidth();
		this.s = bounds.getHeight();
	}

}
