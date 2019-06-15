package application.control;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ResourceBundle;

public class RootController implements Initializable {

	@FXML AnchorPane mainTextPane, editPane;
	@FXML ScrollPane scrollPane;
	@FXML Button textButton, imageButton, blankButton;
	@FXML Button loadTextButton, loadImageButton;
	@FXML Group imageViewGroup, imageGroup, textGroup;
	@FXML ImageView imageView;
	@FXML TextArea textArea;

	Stage currentStage;

	ImageCropper imageCropper;
	Tesseract instance = new Tesseract();

	DataSet dataset;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub

		instance.setLanguage("kor+eng");
		
		imageCropper = new ImageCropper(imageViewGroup);

		imageGroup.setVisible(false);
		textGroup.setVisible(false);

		imageButton.setOnAction(e -> {
			imageGroup.setVisible(true);
			textGroup.setVisible(false);
		});

		textButton.setOnAction(e -> {
			imageGroup.setVisible(false);
			textGroup.setVisible(true);
		});

		loadTextButton.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.getExtensionFilters().addAll(
					new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt"),
					new FileChooser.ExtensionFilter("All Text", "*.*")
					);
			fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
			fileChooser.setTitle("Load text...");
			File file = fileChooser.showOpenDialog(currentStage);
			
			try {
				String str = new String();
				String line = new String();
				BufferedReader br = new BufferedReader(new FileReader(file));
				while((line = br.readLine()) != null) {
					str+=line;
					str+="\n";
				}
				textArea.setText(str);
				br.close();
			} catch (Exception exc) {
				
			}
		});


		loadImageButton.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.getExtensionFilters().addAll(
					new FileChooser.ExtensionFilter("All Images", "*.*"),
					new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.JPG", "*.jpg", "*.JPEG", "*.jpeg"),
					new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG", "*.png")
					);
			fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
			fileChooser.setTitle("Load image...");
			File file = fileChooser.showOpenDialog(currentStage);
			imageView.setImage(new Image(file.toURI().toString()));
			dataset = new ImageData("./save", imageView.getImage());
		});

		//imageGroup
		blankButton.setOnAction(e -> {
			BufferedImage img = imageCropper.crop();
			try{
				String result = instance.doOCR(img);
				System.out.println(result);

			} catch(TesseractException ex) {
				System.out.println(ex.getMessage());
			}
		});


		//textGroup

	}

	public void setupStage(Stage currentStage){
		this.currentStage = currentStage;
	}

	class ImageCropper{
		Group imageGroup;
		class DragContext{
			public double x, y;
		}
		DragContext dragContext = new DragContext();
		Rectangle rect;

		public ImageCropper(Group imageGroup) {
			this.imageGroup = imageGroup;
			rect = new Rectangle(0, 0, 0, 0);
			rect.setStroke(Color.BLUE);
			rect.setFill(Color.LIGHTGRAY.deriveColor(0,  0,  1,  0.5));

			imageGroup.setOnMousePressed(e -> {
				imageGroup.getChildren().remove(rect);
				rect.setX(e.getX());
				rect.setY(e.getY());
				dragContext.x = e.getX();
				dragContext.y = e.getY();
				imageGroup.getChildren().add(rect);
			});

			imageGroup.setOnMouseDragged(e -> {
				rect.setX(Math.min(e.getX(), dragContext.x));
				rect.setY(Math.min(e.getY(), dragContext.y));
				rect.setWidth(Math.abs(e.getX()-dragContext.x));
				rect.setHeight(Math.abs(e.getY()-dragContext.y));
			});

			imageGroup.setOnMouseReleased(e -> {
				blankButton.setDisable(false);
			});
		}

		BufferedImage crop() {
			SnapshotParameters parameters = new SnapshotParameters();
			parameters.setFill(Color.TRANSPARENT);
			Bounds bounds = rect.getBoundsInParent();
			parameters.setViewport(new Rectangle2D(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight()));

			WritableImage wi = new WritableImage((int)bounds.getWidth(), (int)bounds.getHeight());
			imageView.snapshot(parameters, wi);

			BufferedImage bufImageARGB = SwingFXUtils.fromFXImage(wi, null);
			BufferedImage bufImageRGB = new BufferedImage(bufImageARGB.getWidth(), bufImageARGB.getHeight(), BufferedImage.OPAQUE);

			Graphics2D graphics = bufImageRGB.createGraphics();
			graphics.drawImage(bufImageARGB, 0, 0, null);

			return bufImageRGB;
		}
	}



}

