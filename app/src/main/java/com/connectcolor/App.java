package com.connectcolor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
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
    private ProgressStore progressStore;
    private Difficulty currentDifficulty;
    private int currentLevel;
    private int loadRequestId;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        this.progressStore = new ProgressStore();
        this.currentDifficulty = progressStore.getSelectedDifficulty();
        this.currentLevel = progressStore.getLevel(currentDifficulty);

        root = new BorderPane();
        root.getStyleClass().add("app-root");

        scene = new Scene(root);
        scene.getStylesheets().add(
        App.class.getResource("/com/app/styles.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.setTitle("Connect Color");

        showMainMenu();
        stage.show();
    }

    private void showMainMenu() {
        loadRequestId++;
        root.setTop(null);

        Label title = new Label("Connect Color");
        title.getStyleClass().add("menu-title");

        VBox difficultyList = new VBox(12);
        difficultyList.setAlignment(Pos.CENTER);

        for (Difficulty difficulty : Difficulty.values()) {
            Button button = new Button(difficulty + " - Level " + progressStore.getLevel(difficulty));
            button.getStyleClass().add("menu-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setFocusTraversable(false);
            button.setOnAction(e -> startDifficulty(difficulty));
            difficultyList.getChildren().add(button);
        }

        VBox menu = new VBox(28, title, difficultyList);
        menu.getStyleClass().add("main-menu");
        menu.setAlignment(Pos.CENTER);
        menu.setPadding(new Insets(32));

        root.setCenter(menu);
        root.setPrefSize(520, 620);

        if (stage != null) {
            stage.sizeToScene();
        }
    }

    private void startDifficulty(Difficulty difficulty) {
        currentDifficulty = difficulty;
        currentLevel = progressStore.getLevel(currentDifficulty);
        progressStore.setSelectedDifficulty(currentDifficulty);
        progressStore.save();
        loadLevel(currentDifficulty);
    }

    private HBox createGameTopBar() {
        Button backButton = new Button("Back");
        backButton.getStyleClass().add("back-button");
        backButton.setFocusTraversable(false);
        backButton.setOnAction(e -> showMainMenu());

        levelLabel = new Label();
        levelLabel.getStyleClass().add("level-label");

        HBox topBar = new HBox(14, backButton, levelLabel);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10, 14, 10, 14));
        return topBar;
    }

    private void loadLevel(Difficulty difficulty) {
        GameSettings settings = GameSettings.forDifficulty(difficulty);
        long seed = LevelSeed.forLevel(difficulty, currentLevel);
        int level = currentLevel;
        int requestId = ++loadRequestId;

        showLoading(difficulty, level);

        Task<Board> loadTask = new Task<>() {
            @Override
            protected Board call() {
                return new Board(settings, seed);
            }
        };

        loadTask.setOnSucceeded(e -> {
            if (requestId != loadRequestId) {
                return;
            }
            showLoadedBoard(difficulty, level, settings, loadTask.getValue());
        });

        loadTask.setOnFailed(e -> {
            if (requestId != loadRequestId) {
                return;
            }
            showLoadError(loadTask.getException());
        });

        Thread thread = new Thread(loadTask, "connect-color-level-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void showLoadedBoard(Difficulty difficulty, int level, GameSettings settings, Board board) {
        currentDifficulty = difficulty;
        currentLevel = level;
        BoardPanel boardPanel = new BoardPanel(settings);
        boardPanel.setAlignment(Pos.CENTER);

        PrimaryController controller = new PrimaryController(board, boardPanel, this::advanceLevel);
        board.addListener(controller);
        boardPanel.addListener(controller);

        repaintFixedCells(board, boardPanel);

        root.setTop(createGameTopBar());
        root.setCenter(boardPanel);
        root.setPrefSize(settings.getSceneWidth(), settings.getSceneHeight() + 48);
        updateTopBar();

        if (stage != null) {
            stage.sizeToScene();
        }
    }

    private void showLoading(Difficulty difficulty, int level) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.getStyleClass().add("loading-spinner");

        Label loadingLabel = new Label("Loading " + difficulty + " - Level " + level);
        loadingLabel.getStyleClass().add("loading-label");

        VBox loading = new VBox(14, spinner, loadingLabel);
        loading.getStyleClass().add("loading-view");
        loading.setAlignment(Pos.CENTER);
        loading.setPadding(new Insets(32));

        root.setTop(null);
        root.setCenter(loading);
        root.setPrefSize(520, 620);

        if (stage != null) {
            stage.sizeToScene();
        }
    }

    private void showLoadError(Throwable error) {
        Label message = new Label("Could not load puzzle");
        message.getStyleClass().add("loading-label");

        Button backButton = new Button("Back");
        backButton.getStyleClass().add("back-button");
        backButton.setFocusTraversable(false);
        backButton.setOnAction(e -> showMainMenu());

        VBox errorView = new VBox(16, message, backButton);
        errorView.getStyleClass().add("loading-view");
        errorView.setAlignment(Pos.CENTER);
        errorView.setPadding(new Insets(32));

        root.setTop(null);
        root.setCenter(errorView);
        root.setPrefSize(520, 620);

        if (error != null) {
            error.printStackTrace();
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

        PauseTransition pause = new PauseTransition(Duration.millis(450));
        pause.setOnFinished(e -> loadLevel(currentDifficulty));
        pause.play();
    }

    private void updateTopBar() {
        levelLabel.setText(currentDifficulty + " - Level " + currentLevel);
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
