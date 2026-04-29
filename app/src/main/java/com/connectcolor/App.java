package com.connectcolor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import com.connectcolor.View.BoardPanel;
import com.connectcolor.Model.Board;
import com.connectcolor.Controllers.PrimaryController;
import com.connectcolor.Model.Cell;
import java.io.IOException;
import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;
import com.connectcolor.Util.LevelSeed;
import com.connectcolor.Util.ProgressStore;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private Stage stage;
    private BorderPane root;
    private Label levelLabel;
    private ComboBox<Difficulty> difficultyBox;
    private ProgressStore progressStore;
    private Difficulty currentDifficulty;
    private int currentLevel;
    private boolean updatingDifficulty;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        this.progressStore = new ProgressStore();
        this.currentDifficulty = progressStore.getSelectedDifficulty();
        this.currentLevel = progressStore.getLevel(currentDifficulty);

        root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setTop(createTopBar());

        scene = new Scene(root);
        scene.getStylesheets().add(
        App.class.getResource("/com/app/styles.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.setTitle("Connect Color");

        loadLevel(currentDifficulty);
        stage.show();
    }

    private HBox createTopBar() {
        levelLabel = new Label();
        levelLabel.getStyleClass().add("level-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        difficultyBox = new ComboBox<>();
        difficultyBox.getItems().setAll(Difficulty.values());
        difficultyBox.setValue(currentDifficulty);
        difficultyBox.setFocusTraversable(false);
        difficultyBox.setOnAction(e -> {
            if (updatingDifficulty) {
                return;
            }
            Difficulty selected = difficultyBox.getValue();
            if (selected == null || selected == currentDifficulty) {
                return;
            }

            currentDifficulty = selected;
            currentLevel = progressStore.getLevel(currentDifficulty);
            progressStore.setSelectedDifficulty(currentDifficulty);
            progressStore.save();
            loadLevel(currentDifficulty);
        });

        HBox topBar = new HBox(12, levelLabel, spacer, difficultyBox);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10, 14, 10, 14));
        return topBar;
    }

    private void loadLevel(Difficulty difficulty) {
        GameSettings settings = GameSettings.forDifficulty(difficulty);
        long seed = LevelSeed.forLevel(difficulty, currentLevel);

        Board board = new Board(settings, seed);
        BoardPanel boardPanel = new BoardPanel(settings);
        boardPanel.setAlignment(Pos.CENTER);

        PrimaryController controller = new PrimaryController(board, boardPanel, this::advanceLevel);
        board.addListener(controller);
        boardPanel.addListener(controller);

        repaintFixedCells(board, boardPanel);

        root.setCenter(boardPanel);
        root.setPrefSize(settings.getSceneWidth(), settings.getSceneHeight() + 48);
        updateTopBar();

        if (stage != null) {
            stage.sizeToScene();
        }
    }

    private void repaintFixedCells(Board board, BoardPanel boardPanel) {
        for (Cell cell : board.getFixedCells()) {
            boardPanel.setColor(
                cell.getRow(),
                cell.getCol(),
                cell.getSolutionState(),
                cell.isFixed(),
                null,
                null
            );
        }
    }

    private void advanceLevel() {
        currentLevel = progressStore.incrementLevel(currentDifficulty);
        progressStore.setSelectedDifficulty(currentDifficulty);
        progressStore.save();
        loadLevel(currentDifficulty);
    }

    private void updateTopBar() {
        levelLabel.setText("Level " + currentLevel);
        updatingDifficulty = true;
        difficultyBox.setValue(currentDifficulty);
        updatingDifficulty = false;
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}
