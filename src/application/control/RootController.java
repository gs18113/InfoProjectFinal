package application.control;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class RootController implements Initializable {

    @FXML
    AnchorPane root, mainTextPane, setupPane;
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
    TextArea textArea, previewArea;
    @FXML
    Label statLabel;
    @FXML
    CheckBox previewCheckBox;
    @FXML
    HBox mainHBox;
    @FXML
    VBox mainVBox;

    private Stage currentStage;

    private ImageCropper imageCropper;
    private Tesseract instance = new Tesseract();

    private DataSet dataset;

    private DataSaver dataSaver;

    private SetupUIController setupUIController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        instance.setLanguage("kor+eng+osd");

        imageCropper = new ImageCropper(imageViewGroup);

        dataSaver = new DataSaver(System.getProperty("user.home"));


        setupPane.setPrefWidth(mainHBox.getPrefWidth() - mainVBox.getPrefWidth());
        setupPane.setPrefHeight(mainHBox.getPrefHeight());
        setupUIController = new SetupUIController(setupPane);

        imageGroup.setVisible(false);
        textGroup.setVisible(false);

        previewArea.setEditable(false);

        //imageGroup

        imageButton.setOnAction(e -> {
            if(dataset != null){
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Save data?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                alert.showAndWait();
                if (alert.getResult() == ButtonType.YES) {
                    saveAction();
                }
                if(alert.getResult() == ButtonType.NO);
                else return;
            }
            imageGroup.setVisible(true);
            textGroup.setVisible(false);
            previewCheckBox.setSelected(false);
            previewService.cancel();
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
            dataset = new ImageData("./save", file.toURI().toString());
        });

        imageBlankButton.setOnAction(e -> {
            imageBlankButton.setDisable(true);
            Pair imgRect = imageCropper.crop();
            BufferedImage img = (BufferedImage) imgRect.getKey();
            Bounds bounds = (Bounds) imgRect.getValue();
            try {
                String result = instance.doOCR(img);
                System.out.println("OCR result:" + result);
                dataset.addData(new ImageBlank(bounds, result));
                statLabel.setText("Blank generation success!");
                setupUIController.update();
            } catch (TesseractException ex) {
                System.out.println(ex.getMessage());
            }
        });

        //textGroup
        textButton.setOnAction(e -> {
            if(dataset != null){
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Save data?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                alert.showAndWait();
                if (alert.getResult() == ButtonType.YES) {
                    saveAction();
                }
                if(alert.getResult() == ButtonType.NO);
                else return;
            }
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
            updateText();
        });

        previewCheckBox.setSelected(false);

        previewCheckBox.setOnAction(e -> {
            if (previewCheckBox.isSelected()) {
                previewService.restart();
                textBlankButton.setDisable(true);
            } else {
                previewService.cancel();
                textBlankButton.setDisable(false);
            }
        });

        //save&load
        saveButton.setOnAction(e -> {
            if (dataset != null) {
                saveAction();
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
            dataSaver.load(file.getAbsolutePath());
        });

    }

    void saveAction(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Current save path: " + dataSaver.getSavepath() + "\n Select new path?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            directoryChooser.setTitle("Save path...");
            File directory = directoryChooser.showDialog(currentStage);
            dataSaver.setSavepath(directory.getAbsolutePath());
        }
        if (alert.getResult() == ButtonType.CANCEL) {
            return;
        }
        dataSaver.save(dataset, "DATA_" + LocalDateTime.now().toString() + ".blankdata");
        statLabel.setText("Save complete.");
    }

    private synchronized void updateText() {
        Task task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String str = textArea.getText();
                dataset = new TextData("./save", str, str.replace("/*", "").replace("*/", ""));
                int l = -1;
                int cnt = 0;
                for (int i = 0; i < str.length() - 1; i++) {
                    if (str.charAt(i) == '/' && str.charAt(i + 1) == '*') {
                        if (l != -1) {
                            Platform.runLater(() -> {
                                statLabel.setText("Text format error: aborted.");
                            });
                            dataset = null;
                            return null;
                        }
                        l = i;
                    }
                    if (str.charAt(i) == '*' && str.charAt(i + 1) == '/') {
                        if (l == -1) {
                            Platform.runLater(() -> {
                                statLabel.setText("Text format error: aborted.");
                            });
                            dataset = null;
                            return null;
                        }
                        dataset.addData(new TextBlank(l - cnt * 4, i - cnt * 4 - 2, textArea.getText().substring(l + 2, i)));
                        cnt++;
                        l = -1;
                    }
                }
                if (l != -1) {
                    Platform.runLater(() -> {
                        statLabel.setText("Text format error: aborted.");
                    });
                    dataset = null;
                    return null;
                }
                Platform.runLater(() -> {
                    updatePreview();
                    setupUIController.update();
                    statLabel.setText("Blank generation success!");
                });
                return null;
            }
        };
        task.run();
    }

    private void updatePreview() {
        Task task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {
                StringBuffer str = new StringBuffer(((TextData) dataset).getText());
                int c = 0;
                for (int i = 0; i < dataset.getBlanks().size(); i++) {
                    TextBlank t = (TextBlank) dataset.getBlanks().get(i);
                    str.replace(t.l + c, t.r + c, "  [" + (i + 1) + "]  ");
                    c += ("  [" + (i + 1) + "]  ").length() - (t.r - t.l);
                }
                Platform.runLater(() -> {
                    previewArea.setText(str.toString());
                });
                return null;
            }
        };
        task.run();
    }

    class SetupUIController {
        ScrollPane scroll = new ScrollPane();
        VBox vBox = new VBox();

        SetupUIController(AnchorPane setupPane) {
            scroll.setStyle("-fx-background-color:transparent;");
            scroll.setPrefHeight(setupPane.getPrefHeight() - 60);
            scroll.setPrefWidth(setupPane.getPrefWidth() - 60);
            setupPane.getChildren().add(scroll);
            scroll.setContent(vBox);
            scroll.setLayoutX(30);
            scroll.setLayoutY(30);
            vBox.setSpacing(5);
        }

        void update() {
            vBox.getChildren().clear();
            for (int i = 0; i < dataset.getBlanks().size(); i++) {
                vBox.getChildren().add(new DataPane(i + 1, dataset.getBlanks().get(i).getStr()));
            }
        }
    }

    class DataPane extends HBox {
        // TODO
        Label blankNumLabel = new Label();
        TextField blankTextField = new TextField();
        String str;

        public DataPane(int num, String s) {
            super();
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(5);
            this.blankNumLabel.setText("[" + num + "]");
            this.blankTextField.setText(s);
            this.str = s;
            this.getChildren().addAll(this.blankNumLabel, this.blankTextField);
            this.blankNumLabel.setLayoutY(5);
            this.blankTextField.setLayoutY(20);
            this.blankTextField.textProperty().addListener((observable, oldValue, newValue) -> str = newValue);
        }
    }

    private Service<String> previewService = new Service<String>() {
        @Override
        protected Task<String> createTask() {
            Task<String> task = new Task<String>() {
                @Override
                protected String call() throws Exception {
                    while (true) {
                        if (isCancelled()) break;
                        updateText();
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            if (isCancelled()) break;
                        }
                    }
                    return null;
                }
            };
            return task;
        }
    };

    public void setupStage(Stage currentStage) {
        this.currentStage = currentStage;
    }

    class ImageCropper {
        Group layer;

        class DragContext {
            public double x, y;
        }

        DragContext dragContext = new DragContext();
        Rectangle rect;

        ImageCropper(Group layer) {
            this.layer = layer;
            rect = new Rectangle(0, 0, 0, 0);
            rect.setStroke(Color.BLUE);
            rect.setFill(Color.LIGHTGRAY.deriveColor(0, 0, 1, 0.5));

            layer.setOnMousePressed(e -> {
                this.layer.getChildren().remove(rect);
                rect = new Rectangle(e.getX(), e.getY(), 0, 0);
                rect.setStroke(Color.BLUE);
                rect.setFill(Color.LIGHTGRAY.deriveColor(0, 0, 1, 0.5));
                dragContext.x = e.getX();
                dragContext.y = e.getY();
                this.layer.getChildren().add(rect);
            });

            layer.setOnMouseDragged(e -> {
                rect.setX(Math.min(e.getX(), dragContext.x));
                rect.setY(Math.min(e.getY(), dragContext.y));
                rect.setWidth(Math.abs(e.getX() - dragContext.x));
                rect.setHeight(Math.abs(e.getY() - dragContext.y));
            });

            layer.setOnMouseReleased(e -> {
                imageBlankButton.setDisable(false);
            });
        }

        Pair<BufferedImage, Bounds> crop() {
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setFill(Color.TRANSPARENT);
            Bounds bounds = rect.getBoundsInParent();
            parameters.setViewport(new Rectangle2D( bounds.getMinX(), bounds.getMinY(), (int) bounds.getWidth(), (int) bounds.getHeight()));

            WritableImage wi = new WritableImage((int) bounds.getWidth(), (int) bounds.getHeight());
            imageView.snapshot(parameters, wi);


            BufferedImage bufImageARGB = SwingFXUtils.fromFXImage(wi, null);
            //BufferedImage bufImageRGB = new BufferedImage(bufImageARGB.getWidth(), bufImageARGB.getHeight(), BufferedImage.OPAQUE);
            BufferedImage bufImageGray = new BufferedImage(bufImageARGB.getWidth(), bufImageARGB.getHeight(), BufferedImage.TYPE_BYTE_GRAY);


            //Graphics2D graphics = bufImageRGB.createGraphics();
            Graphics2D graphics = bufImageGray.createGraphics();
            graphics.drawImage(bufImageARGB, 0, 0, null);

            /*
            try {
                File file = new File("./saveddata/cap.jpg");
                file.createNewFile();

                ImageIO.write(bufImageGray, "jpg", file);

                System.out.println( "Image saved to " + file.getAbsolutePath());

            } catch (IOException e) {
                e.printStackTrace();
            }
            */

            rect = new Rectangle(0, 0, 0, 0);
            imageGroup.getChildren().add(rect);

            return new Pair(bufImageGray, bounds);

        }
    }

    class DataSaver {

        String savepath;

        DataSaver(String savepath) {
            this.savepath = savepath;
        }

        public String getSavepath() {
            return savepath;
        }

        public void setSavepath(String savepath) {
            this.savepath = savepath;
        }

        void save(DataSet set, String filename) {
            try {
                File file;
                if (savepath.charAt(savepath.length() - 1) != '/') file = new File(savepath + "/" + filename);
                else file = new File(savepath + filename);
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

        void load(String filepathname) {
            DataSet set;
            try {
                File file = new File(filepathname);
                FileInputStream fos = new FileInputStream(file.getAbsolutePath());
                ObjectInputStream oos = new ObjectInputStream(fos);

                dataset = (DataSet) oos.readObject();
                oos.close();
                fos.close();

                if (dataset.dataType.equals("Text")) {
                    imageGroup.setVisible(false);
                    textGroup.setVisible(true);
                    textArea.setText(((TextData) dataset).getFullText());
                    updateText();
                    setupUIController.update();
                } else {
                    imageGroup.setVisible(true);
                    textGroup.setVisible(false);
                    imageView.setImage(((ImageData) dataset).getFullImage());
                    for (int i = 0; i < dataset.getBlanks().size(); i++) {
                        imageViewGroup.getChildren().add(((ImageBlank) dataset.getBlanks().get(i)).getRect());
                    }
                    setupUIController.update();
                }
                return;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return;
            }
        }
    }
}
