package application.control;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class TestController implements Initializable {
    @FXML
    AnchorPane root, mainTextPane, setupPane;
    @FXML
    ScrollPane scrollPane;
    @FXML
    Button readyButton;
    @FXML
    Group imageViewGroup, imageGroup, textGroup;
    @FXML
    ImageView imageView;
    @FXML
    TextArea previewArea;
    @FXML
    Label statLabel;
    @FXML
    HBox mainHBox;
    @FXML
    VBox mainVBox;

    DataSet dataset;

    private SetupUIController setupUIController;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        setupPane.setPrefWidth(mainHBox.getPrefWidth() - mainVBox.getPrefWidth());
        setupPane.setPrefHeight(mainHBox.getPrefHeight());
        previewArea.setEditable(false);

        readyButton.setOnAction(e -> {
            setupUIController.update();
        });
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
            for (int i = 0; i < dataset.getBlanks().size(); i++) {
                vBox.getChildren().add(new DataPane(i + 1, ""));
            }
        }

        void update() {
            int rightCnt = 0;
            for (int i = 0; i < dataset.getBlanks().size(); i++) {
                boolean right = dataset.getBlanks().get(i).getStr().replaceAll("\\s+","").equalsIgnoreCase(((DataPane) vBox.getChildren().get(i)).getStr().replaceAll("\\s+",""));
                if (right) rightCnt++;
                else dataset.getBlanks().get(i).increaseWrong();
                ((DataPane) vBox.getChildren().get(i)).setImage(right);
            }
            statLabel.setText("Correct: " + rightCnt + " Wrong: " + (dataset.getBlanks().size() - rightCnt));
        }
    }

    class DataPane extends HBox {
        Label blankNumLabel = new Label();
        TextField blankTextField = new TextField();
        ImageView rightImage = new ImageView();
        String str;

        public DataPane(int num, String s) {
            super();
            setAlignment(Pos.CENTER_LEFT);
            setSpacing(5);
            this.blankNumLabel.setText("[" + num + "]");
            this.blankTextField.setText(s);
            this.str = s;
            this.rightImage.setImage(new Image(getClass().getResource("../images/thinkingface.png").toString()));
            rightImage.setPreserveRatio(true);
            rightImage.setFitWidth(40);
            this.getChildren().addAll(this.blankNumLabel, this.blankTextField, this.rightImage);
            this.blankNumLabel.setLayoutY(5);
            this.blankTextField.setLayoutY(20);
            this.blankTextField.textProperty().addListener((observable, oldValue, newValue) -> str = newValue);
            this.rightImage.setOnMousePressed(e -> {
                if(e.isSecondaryButtonDown()){

                }
            });
        }

        public String getStr() {
            return str;
        }

        public void setImage(boolean isRight) {
            this.rightImage.setImage(isRight ? new Image(getClass().getResource("../images/check.png").toString()) : new Image(getClass().getResource("../images/cross.png").toString()));
        }
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

    void setDataset(DataSet dataset) {
        this.dataset = dataset;
        if (dataset.dataType.equals("Text")) {
            textGroup.setVisible(true);
            imageGroup.setVisible(false);
            updatePreview();
        } else {
            imageGroup.setVisible(true);
            textGroup.setVisible(false);
            imageView.setImage(((ImageData) dataset).getFullImage());
            for (int i = 0; i < dataset.getBlanks().size(); i++) {
                ImageBlank blank = (ImageBlank) dataset.getBlanks().get(i);
                imageViewGroup.getChildren().add(blank.getWhiteRect());
                Text text = new Text(blank.getCenter().getKey() - 2, blank.getCenter().getValue() - 2, "[" + (i + 1) + "]");
                text.setFill(Color.RED);
                text.setFont(new Font(13));
                imageViewGroup.getChildren().add(text);
            }
        }
        setupUIController = new SetupUIController(setupPane);
    }
}
