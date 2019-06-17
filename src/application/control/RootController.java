package application.control;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
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
import java.lang.reflect.Array;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class RootController implements Initializable {

    @FXML
    AnchorPane root, mainTextPane, setupPane;
    @FXML
    ScrollPane scrollPane;
    @FXML
    Button textButton, imageButton, imageBlankButton, textBlankButton, saveButton, loadButton;
    @FXML
    Button loadTextButton, loadImageButton, startButton;
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
            imageCropper.reset();
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
                ImageBlank newBlank = new ImageBlank(bounds, result);
                dataset.addData(newBlank);
                imageCropper.getRects().get(imageCropper.getRects().size()-1).setData(newBlank);
                statLabel.setText("Blank generation success!");
                statLabel.setTextFill(Color.BLUE);
                setupUIController.update();
            } catch (TesseractException ex) {
                System.out.println(ex.getMessage());
                ImageBlank newBlank = new ImageBlank(bounds, "");
                dataset.addData(newBlank);
                imageCropper.getRects().get(imageCropper.getRects().size()-1).setData(newBlank);
                statLabel.setText("OCR ERROR: Set string to default.");
                statLabel.setTextFill(Color.RED);
                setupUIController.update();
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
            statLabel.setTextFill(Color.BLACK);
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
                statLabel.setTextFill(Color.RED);
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

        startButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/test.fxml"));
                Parent parent = (Parent) loader.load();
                ((TestController) loader.getController()).setDataset(dataset);
                Scene newScene = new Scene(parent);
                Stage newStage = new Stage();
                newStage.setScene(newScene);
                newStage.setTitle("Test!");
                newStage.show();
            } catch(Exception ee){
                //System.out.println(ee.getMessage());
                ee.printStackTrace();
            }
        });

        MouseContext context = new MouseContext();
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(e -> {
            for(int i=0;i<imageCropper.getRects().size();i++){
                ImageCropper.RectContext rectContext = imageCropper.getRects().get(i);
                Bounds bounds = rectContext.getRectangle().getBoundsInParent();
                if(bounds.getMinX()<context.x&&context.x<bounds.getMaxX()&&bounds.getMinY()<context.y&&context.y<bounds.getMaxY()){
                    imageCropper.getRects().remove(rectContext);
                    imageViewGroup.getChildren().remove(rectContext.getRectangle());
                    imageViewGroup.getChildren().remove(rectContext.getText());
                    dataset.getBlanks().remove(rectContext.getData());
                    imageCropper.updateImageIndex();
                    break;
                }
            }
            setupUIController.update();
        });
        contextMenu.getItems().add(deleteMenuItem);

        imageViewGroup.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            context.x = e.getX();
            context.y = e.getY();
            if(e.isSecondaryButtonDown()){
                contextMenu.show(imageViewGroup, e.getScreenX(), e.getScreenY());

            }
        });
    }

    private void saveAction(){
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
        statLabel.setTextFill(Color.GREEN);
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
                                statLabel.setTextFill(Color.RED);
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
                                statLabel.setTextFill(Color.RED);
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
                        statLabel.setTextFill(Color.RED);
                    });
                    dataset = null;
                    return null;
                }
                Platform.runLater(() -> {
                    updatePreview();
                    setupUIController.update();
                    statLabel.setText("Blank generation success!");
                    statLabel.setTextFill(Color.BLUE);
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
                vBox.getChildren().add(new DataPane(i + 1, dataset.getBlanks().get(i)));
            }
        }
    }

    class DataPane extends HBox {
        Label blankNumLabel = new Label();
        TextField blankTextField = new TextField();
        Blank parent;

        public DataPane(int num, Blank parent) {
            super();
            this.parent = parent;
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(5);
            this.blankNumLabel.setText("[" + num + "]");
            this.blankTextField.setText(parent.getStr());
            this.getChildren().addAll(this.blankNumLabel, this.blankTextField);
            this.blankNumLabel.setLayoutY(5);
            this.blankTextField.setLayoutY(20);
            this.blankTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                parent.setStr(newValue);
            });
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

    class MouseContext {
        public double x, y;
    }

    class ImageCropper {
        Group layer;
        ArrayList<RectContext> rects = new ArrayList<>();
        private int imageCount=1;

        public ArrayList<RectContext> getRects() {
            return rects;
        }

        class RectContext{
            Rectangle rectangle;
            Text text;
            Blank data;

            public RectContext(Rectangle rectangle, Text text, Blank data) {
                this.rectangle = rectangle;
                this.text = text;
                this.data = data;
            }

            public Rectangle getRectangle() {
                return rectangle;
            }

            public void setRectangle(Rectangle rectangle) {
                this.rectangle = rectangle;
            }

            public Text getText() {
                return text;
            }

            public void setText(Text text) {
                this.text = text;
            }

            public Blank getData() {
                return data;
            }

            public void setData(Blank data) {
                this.data = data;
            }
        }

        public void setImageCount(int imageCount) {
            this.imageCount = imageCount;
        }

        public void updateImageIndex(){
            for(int i=0;i<rects.size();i++){
                rects.get(i).getText().setText("["+(i+1)+"]");
            }
            imageCount = rects.size()+1;
        }

        MouseContext dragContext = new MouseContext();
        Rectangle rect;

        ImageCropper(Group layer) {
            this.layer = layer;
            rect = new Rectangle(0, 0, 0, 0);
            rect.setStroke(Color.BLUE);
            rect.setFill(Color.LIGHTGRAY.deriveColor(0, 0, 1, 0.5));

            layer.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
                if(e.isSecondaryButtonDown()) return;
                if(rect!=null) this.layer.getChildren().remove(rect);
                rect = new Rectangle(e.getX(), e.getY(), 0, 0);
                rect.setStroke(Color.BLUE);
                rect.setFill(Color.LIGHTGRAY.deriveColor(0, 0, 1, 0.5));
                dragContext.x = e.getX();
                dragContext.y = e.getY();
                this.layer.getChildren().add(rect);
            });

            layer.setOnMouseDragged(e -> {
                if(e.isSecondaryButtonDown()) return;
                rect.setX(Math.min(e.getX(), dragContext.x));
                rect.setY(Math.min(e.getY(), dragContext.y));
                rect.setWidth(Math.abs(e.getX() - dragContext.x));
                rect.setHeight(Math.abs(e.getY() - dragContext.y));
            });

            layer.setOnMouseReleased(e -> {
                if(e.isSecondaryButtonDown()) return;
                imageBlankButton.setDisable(false);
            });
        }

        void reset(){
            imageCount = 1;
            rects.clear();
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

            Text text = new Text(bounds.getMinX()+bounds.getWidth()/2 - 2, bounds.getMinY()+bounds.getHeight()/2 - 2, "[" + (imageCount++) + "]");
            text.setFill(Color.GREEN);
            text.setFont(new Font(13));
            imageViewGroup.getChildren().add(text);
            rects.add(new RectContext(rect, text, null));

            rect = null;

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
                    updatePreview();
                    setupUIController.update();
                } else {
                    imageGroup.setVisible(true);
                    textGroup.setVisible(false);
                    imageView.setImage(((ImageData) dataset).getFullImage());
                    imageCropper.reset();
                    for (int i = 0; i < dataset.getBlanks().size(); i++) {
                        Rectangle loadedRect = ((ImageBlank) dataset.getBlanks().get(i)).getRect();
                        imageViewGroup.getChildren().add(loadedRect);
                        Bounds bounds = loadedRect.getBoundsInParent();
                        Text loadedText = new Text(bounds.getMinX()+bounds.getWidth()/2 - 2, bounds.getMinY()+bounds.getHeight()/2 - 2, "[" + (i+1) + "]");
                        imageViewGroup.getChildren().add(loadedText);
                        imageCropper.getRects().add(imageCropper.new RectContext(loadedRect, loadedText, dataset.getBlanks().get(i)));
                    }
                    imageCropper.setImageCount(dataset.getBlanks().size()+1);
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
