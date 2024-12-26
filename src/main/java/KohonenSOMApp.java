import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class KohonenSOMApp extends Application {
    private static final int DEFAULT_GRID_SIZE = 50;
    private static final int RECT_SIZE = 10;
    private Rectangle[][] colorMap;
    private KohonenSOM som;
    private boolean isTraining = false;
    private Pane colorMapPane;
    private ProgressBar progressBar;
    private TextField radiusField;
    private TextField learningRateField;
    private TextField currentIterationField;
    private List<Color> inputColors = new ArrayList<>();
    private List<Color> preTrainingColors = new ArrayList<>();
    private List<Color> predefinedSamples = List.of(
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.ORANGE, Color.PURPLE, Color.PINK, Color.CYAN
    );

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Color Clustering using Kohonen SOM");

        BorderPane root = new BorderPane();
        colorMapPane = new Pane();
        GridPane controlPanel = new GridPane();
        root.setCenter(colorMapPane);
        root.setRight(controlPanel);

        setupControlPanel(controlPanel);

        Scene scene = new Scene(root, 1200, 600);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupControlPanel(GridPane controlPanel) {
        controlPanel.setVgap(20);
        controlPanel.setHgap(15);
        controlPanel.setPadding(new Insets(20));

        Label gridSizeLabel = new Label("Grid Size:");
        TextField gridSizeField = new TextField(String.valueOf(DEFAULT_GRID_SIZE));

        Label numInputsLabel = new Label("Number of Inputs:");
        TextField numInputsField = new TextField("3");

        Label inputValuesLabel = new Label("Selected Colors:");
        TextField inputValuesField = new TextField();
        inputValuesField.setEditable(false);

        Label learningRateLabel = new Label("Initial Learning Rate:");
        learningRateField = new TextField("0.1");

        Label radiusLabel = new Label("Initial Radius (in pixels):");
        radiusField = new TextField("150");

        Label stoppingCriteriaLabel = new Label("Stopping Criteria:");
        ComboBox<String> stoppingCriteriaComboBox = new ComboBox<>();
        stoppingCriteriaComboBox.getItems().addAll("Epochs", "Iterations");
        stoppingCriteriaComboBox.setValue("Iterations");

        Label stoppingValueLabel = new Label("Stopping Value:");
        TextField stoppingValueField = new TextField("5000");

        currentIterationField = new TextField();
        currentIterationField.setEditable(false);
        currentIterationField.setMinHeight(30);


        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(500);
        progressBar.setMinHeight(30);

        HBox progressBox = new HBox(10, currentIterationField, progressBar);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        progressBox.setSpacing(10);

        Button startButton = new Button("Start");
        Button stopButton = new Button("Stop");
        Button randomizeButton = new Button("Randomize");
        Button repeatButton = new Button("Repeat Learning");
        Button selectSampleButton = new Button("Select Sample");

        HBox buttonBox = new HBox(15, startButton, stopButton, randomizeButton, repeatButton, selectSampleButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));

        controlPanel.add(gridSizeLabel, 0, 0);
        controlPanel.add(gridSizeField, 1, 0);
        controlPanel.add(numInputsLabel, 0, 1);
        controlPanel.add(numInputsField, 1, 1);
        controlPanel.add(inputValuesLabel, 0, 2);
        controlPanel.add(inputValuesField, 1, 2);
        controlPanel.add(learningRateLabel, 0, 3);
        controlPanel.add(learningRateField, 1, 3);
        controlPanel.add(radiusLabel, 0, 4);
        controlPanel.add(radiusField, 1, 4);
        controlPanel.add(stoppingCriteriaLabel, 0, 5);
        controlPanel.add(stoppingCriteriaComboBox, 1, 5);
        controlPanel.add(stoppingValueLabel, 0, 6);
        controlPanel.add(stoppingValueField, 1, 6);
        controlPanel.add(currentIterationField, 0, 7, 2, 1);
        controlPanel.add(progressBar, 0, 8, 2, 1);
        controlPanel.add(buttonBox, 0, 9, 2, 1);

        startButton.setOnAction(e -> startTrainingHandler(gridSizeField, numInputsField, inputValuesField, stoppingCriteriaComboBox, stoppingValueField));
        stopButton.setOnAction(e -> stopTraining());
        randomizeButton.setOnAction(e -> randomizeGrid());
        repeatButton.setOnAction(e -> repeatTraining(gridSizeField, numInputsField, stoppingCriteriaComboBox, stoppingValueField));
        selectSampleButton.setOnAction(e -> openSampleSelectionWindow(numInputsField, inputValuesField));
    }

    private void repeatTraining(TextField gridSizeField, TextField numInputsField, ComboBox<String> stoppingCriteriaComboBox, TextField stoppingValueField) {
        if (som != null) {
            stopTraining();
            Platform.runLater(() -> {
                initializeColorMap(colorMapPane, Integer.parseInt(gridSizeField.getText()));
                startTraining(
                        Integer.parseInt(gridSizeField.getText()),
                        Integer.parseInt(numInputsField.getText()),
                        Double.parseDouble(learningRateField.getText()),
                        Integer.parseInt(radiusField.getText()),
                        Integer.parseInt(stoppingValueField.getText()),
                        stoppingCriteriaComboBox.getValue()
                );
            });
        }
    }

    private void initializeColorMap(Pane colorMapPane, int gridSize) {
        if (colorMap != null) {
            colorMapPane.getChildren().clear();
        }
        colorMap = new Rectangle[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                Rectangle rect = new Rectangle(RECT_SIZE, RECT_SIZE);
                rect.getStyleClass().add("rectangle");
                rect.setFill(Color.rgb((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256)));

                rect.setX(i * RECT_SIZE);
                rect.setY(j * RECT_SIZE);
                rect.setWidth(RECT_SIZE + 0.1);
                rect.setHeight(RECT_SIZE + 0.1);

                colorMapPane.getChildren().add(rect);
                colorMap[i][j] = rect;
            }
        }
        preTrainingColors.clear();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                preTrainingColors.add(((Color) colorMap[i][j].getFill()));
            }
        }
    }

    private void startTrainingHandler(TextField gridSizeField, TextField numInputsField, TextField inputValuesField, ComboBox<String> stoppingCriteriaComboBox, TextField stoppingValueField) {
        try {
            int gridSize = Integer.parseInt(gridSizeField.getText());
            int numInputs = Integer.parseInt(numInputsField.getText());

            if (inputColors.size() != numInputs) {
                showAlert(Alert.AlertType.ERROR, "Input Error", "Please select the correct number of colors.");
                return;
            }

            double learningRate = Double.parseDouble(learningRateField.getText());
            int radius = Integer.parseInt(radiusField.getText());
            int stoppingValue = Integer.parseInt(stoppingValueField.getText());
            String stoppingCriteria = stoppingCriteriaComboBox.getValue();

            if (gridSize <= 0 || numInputs <= 0 || learningRate <= 0 || radius <= 0 || stoppingValue <= 0) {
                showAlert(Alert.AlertType.ERROR, "Input Error", "All fields must be positive values.");
                return;
            }

            if (!isTraining) {
                initializeColorMap(colorMapPane, gridSize);
                startTraining(gridSize, numInputs, learningRate, radius, stoppingValue, stoppingCriteria);
            }
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Input Error", "Please enter valid numbers in all fields.");
        }
    }


    private void startTraining(int gridSize, int numInputs, double learningRate, int radius, int stoppingValue, String stoppingCriteria) {
        isTraining = true;
        if(stoppingCriteria.equalsIgnoreCase("Epoch")) {
            int maxepochs = stoppingValue * gridSize *gridSize;
            som = new KohonenSOM(gridSize, gridSize, learningRate, radius, maxepochs, inputColors);
        }

        else {
            som = new KohonenSOM(gridSize, gridSize, learningRate, radius, 100, inputColors);
        }
        preTrainingColors.clear();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                preTrainingColors.add(((Color) colorMap[i][j].getFill()));
            }
        }

        int maxIterations = stoppingCriteria.equals("Iterations") ? stoppingValue : stoppingValue * gridSize *gridSize *numInputs;

        progressBar.setProgress(0);

        Runnable progressUpdater = () -> {
            if (isTraining) {
                updateColorMap();
                currentIterationField.setText("Iteration: " + som.getCurrentIteration());
                progressBar.setProgress((double) som.getCurrentIteration() / stoppingValue);
            }
        };

        new Thread(() -> {
            som.train(maxIterations, (Void) -> {
                Platform.runLater(progressUpdater);
            });
            Platform.runLater(() -> {
                isTraining = false;
                updateColorMap();
                showAlert(Alert.AlertType.INFORMATION, "Training Complete", "The SOM has completed training.");
                showBeforeAfterComparison();
            });
        }).start();
    }


    private void updateColorMap() {
        for (int i = 0; i < colorMap.length; i++) {
            for (int j = 0; j < colorMap[i].length; j++) {
                colorMap[i][j].setFill(som.getColorAt(i, j));
            }
        }
    }

    private void stopTraining() {
        if (som != null) {
            som.stopTraining();
            isTraining = false;
        }
    }

    private void randomizeGrid() {
        for (Rectangle[] rectangles : colorMap) {
            for (Rectangle rect : rectangles) {
                rect.setFill(Color.rgb((int) (Math.random() * 256), (int) (Math.random() * 256), (int) (Math.random() * 256)));
            }
        }
    }

    private void openSampleSelectionWindow(TextField numInputsField, TextField inputValuesField) {
        int numInputs = Integer.parseInt(numInputsField.getText());
        Stage sampleSelectionStage = new Stage();
        sampleSelectionStage.setTitle("Select Sample Colors");

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Label instructions = new Label("Select " + numInputs + " colors:");
        HBox colorSelectionBox = new HBox(10);
        colorSelectionBox.setAlignment(Pos.CENTER);

        List<ColorPicker> colorPickers = new ArrayList<>();
        for (int i = 0; i < numInputs; i++) {
            ColorPicker colorPicker = new ColorPicker(predefinedSamples.get(i % predefinedSamples.size()));
            colorPickers.add(colorPicker);
            colorSelectionBox.getChildren().add(colorPicker);
        }

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> {
            inputColors.clear();
            for (ColorPicker colorPicker : colorPickers) {
                inputColors.add(colorPicker.getValue());
            }
            inputValuesField.setText(inputColors.toString());
            sampleSelectionStage.close();
        });

        layout.getChildren().addAll(instructions, colorSelectionBox, confirmButton);
        Scene scene = new Scene(layout, 400, 200);
        sampleSelectionStage.setScene(scene);
        sampleSelectionStage.show();
    }

    private void showBeforeAfterComparison() {
        Stage comparisonStage = new Stage();
        comparisonStage.setTitle("Grid Comparison");

        Pane beforePane = new Pane();
        Pane afterPane = new Pane();

        int gridSize = colorMap.length;

        Rectangle[][] beforeColorMap = new Rectangle[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                Rectangle rect = new Rectangle(RECT_SIZE, RECT_SIZE);
                rect.setFill(preTrainingColors.get(i * gridSize + j));
                rect.setX(i * RECT_SIZE);
                rect.setY(j * RECT_SIZE);
                beforePane.getChildren().add(rect);
                beforeColorMap[i][j] = rect;
            }
        }

        Rectangle[][] afterColorMap = new Rectangle[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                Rectangle rect = new Rectangle(RECT_SIZE, RECT_SIZE);
                rect.setFill(som.getColorAt(i, j));
                rect.setX(i * RECT_SIZE);
                rect.setY(j * RECT_SIZE);
                afterPane.getChildren().add(rect);
                afterColorMap[i][j] = rect;
            }
        }

        HBox layout = new HBox(20);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(
                new VBox(new Label("Before Training"), beforePane),
                new VBox(new Label("After Training"), afterPane)
        );

        Scene scene = new Scene(layout, 1200, 600);
        comparisonStage.setScene(scene);
        comparisonStage.show();
    }


    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
