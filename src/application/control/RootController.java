package application.control;

import javafx.application.Platform;
import javafx.concurrent.Task;
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
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class RootController implements Initializable {

    @FXML
    AnchorPane mainTextPane, editPane;
    @FXML
    ScrollPane scrollPane;
    @FXML
    Button textButton, imageButton, imageBlankButton, textBlankButton, saveButton, loadButton;
    @FXML
    Button loadTextButton, loadImageButton;
    @FXML
    Group imageViewGroup, imageGroup, textGroup;
    @FXML
    ImageView imageView;
    @FXML
    TextArea textArea;
    @FXML
    Label statLabel;

    Stage currentStage;

    ImageCropper imageCropper;
    Tesseract instance = new Tesseract();

    DataSet dataset;

    DataSaver dataSaver;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        instance.setLanguage("kor+eng+osd");

        imageCropper = new ImageCropper(imageViewGroup);

        dataSaver = new DataSaver(System.getProperty("user.home"));

        imageGroup.setVisible(false);
        textGroup.setVisible(false);

        //imageGroup

        imageButton.setOnAction(e -> {
            imageGroup.setVisible(true);
            textGroup.setVisible(false);
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

        imageBlankButton.setOnAction(e -> {
            Pair imgRect = imageCropper.crop();
            BufferedImage img = (BufferedImage) imgRect.getKey();
            Rectangle rect = (Rectangle) imgRect.getValue();
            try {
                String result = instance.doOCR(img);
                System.out.println("OCR result:" + result);
                dataset.addData(new ImageBlank(rect, result));
                statLabel.setText("Blank generation success!");
                imageViewGroup.getChildren().remove(rect);
            } catch (TesseractException ex) {
                System.out.println(ex.getMessage());
            }
        });

        //textGroup
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
                String str = "";
                String line = "";
                BufferedReader br = new BufferedReader(new FileReader(file));
                while ((line = br.readLine()) != null) {
                    str += line;
                    str += "\n";
                }
                textArea.setText(str);
                br.close();
            } catch (Exception exc) {

            }
        });

        textBlankButton.setOnAction(e -> {
            statLabel.setText("Generating blanks...");
            Task task = new Task<Void>() {

                @Override
                protected Void call() throws Exception {
                    String str = textArea.getText();
                    dataset = new TextData("./save", str.replaceAll("\\/*", "").replaceAll("\\*/", ""));
                    int l = -1;
                    int cnt = 0;
                    for (int i = 0; i < str.length() - 1; i++) {
                        if (str.charAt(i) == '/' && str.charAt(i + 1) == '*') {
                            if (l != -1) {
                                Platform.runLater(() -> {
                                    statLabel.setText("Text format error: aborted.");
                                });

                                return null;
                            }
                            l = i;
                        }
                        if (str.charAt(i) == '*' && str.charAt(i + 1) == '/') {
                            if (l == -1) {
                                Platform.runLater(() -> {
                                    statLabel.setText("Text format error: aborted.");
                                });
                                return null;
                            }
                            dataset.addData(new TextBlank(l - 2 - cnt * 4, i - 1 - cnt * 4 - 2, str.substring(l - 2 - cnt * 4, i - 1 - cnt * 4 - 2)));
                            cnt++;
                            l = -1;
                        }
                    }
                    Platform.runLater(() -> {
                        statLabel.setText("Blank generation success!");
                    });
                    return null;
                }
            };
            task.run();
        });

        //save&load
        saveButton.setOnAction(e -> {
            if (dataset != null) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setTitle("Save path...");
                File directory = directoryChooser.showDialog(currentStage);
                dataSaver.save(dataset, "DATA_" + LocalDateTime.now().toString() + ".blankdata");
                statLabel.setText("Save complete.");
            } else {
                statLabel.setText("No data to save. aborted.");
            }
        });

        loadButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Data File", "*.blankdata"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.setTitle("Load data...");
            File file = fileChooser.showOpenDialog(currentStage);
            imageView.setImage(new Image(file.toURI().toString()));
            dataset = dataSaver.load(file.toURI().toString());
        });

    }

    public void setupStage(Stage currentStage) {
        this.currentStage = currentStage;
    }

    class ImageCropper {
        Group imageGroup;

        class DragContext {
            public double x, y;
        }

        DragContext dragContext = new DragContext();
        Rectangle rect;

        ImageCropper(Group imageGroup) {
            this.imageGroup = imageGroup;
            rect = new Rectangle(0, 0, 0, 0);
            rect.setStroke(Color.BLUE);
            rect.setFill(Color.LIGHTGRAY.deriveColor(0, 0, 1, 0.5));

            imageGroup.setOnMousePressed(e -> {
                imageGroup.getChildren().remove(rect);
                rect.setX(e.getX());
                rect.setY(e.getY());
                rect.setWidth(0);
                rect.setHeight(0);
                dragContext.x = e.getX();
                dragContext.y = e.getY();
                imageGroup.getChildren().add(rect);
            });

            imageGroup.setOnMouseDragged(e -> {
                rect.setX(Math.min(e.getX(), dragContext.x));
                rect.setY(Math.min(e.getY(), dragContext.y));
                rect.setWidth(Math.abs(e.getX() - dragContext.x));
                rect.setHeight(Math.abs(e.getY() - dragContext.y));
            });

            imageGroup.setOnMouseReleased(e -> {
                imageBlankButton.setDisable(false);
            });
        }

        Pair<BufferedImage, Rectangle> crop() {
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.TRANSPARENT);
            Bounds bounds = rect.getBoundsInParent();
            parameters.setViewport(new Rectangle2D(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight()));

            WritableImage wi = new WritableImage((int) bounds.getWidth(), (int) bounds.getHeight());
            imageView.snapshot(parameters, wi);

            BufferedImage bufImageARGB = SwingFXUtils.fromFXImage(wi, null);
            //BufferedImage bufImageRGB = new BufferedImage(bufImageARGB.getWidth(), bufImageARGB.getHeight(), BufferedImage.OPAQUE);
            BufferedImage bufImageGray = new BufferedImage(bufImageARGB.getWidth(), bufImageARGB.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
            System.out.println("Width: " + bufImageARGB.getWidth());
            System.out.println("Height: " + bufImageARGB.getHeight());


            //Graphics2D graphics = bufImageRGB.createGraphics();
            Graphics2D graphics = bufImageGray.createGraphics();
            graphics.drawImage(bufImageARGB, 0, 0, null);

//			return new Pair(bufImageRGB, rect);
            return new Pair(bufImageGray, rect);

        }
    }

    class DataSaver {

        String savepath;

        DataSaver(String savepath) {
            this.savepath = savepath;
        }

        void save(DataSet set, String filename) {
            try {
                File file = new File(savepath + filename);
                file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
                ObjectOutputStream oos = new ObjectOutputStream(fos);

                oos.writeObject(set);
                oos.flush();
                oos.close();
                fos.close();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        DataSet load(String filepathname) {
            DataSet set;
            try {
                File file = new File(filepathname);
                FileInputStream fos = new FileInputStream(file.getAbsolutePath());
                ObjectInputStream oos = new ObjectInputStream(fos);

                set = (DataSet) oos.readObject();
                oos.close();
                fos.close();
                return set;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return null;
            }
        }
    }
}
