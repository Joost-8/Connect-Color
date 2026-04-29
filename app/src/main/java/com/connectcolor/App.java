package com.connectcolor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.animation.PauseTransition;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.connectcolor.Util.Difficulty;
import com.connectcolor.Util.GameSettings;
import com.connectcolor.Util.ProgressStore;
import com.connectcolor.Model.PathProgressStore;
import com.connectcolor.Model.PuzzlePreloader;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private Stage stage;
    private BorderPane root;
    private Label levelLabel;
    private ProgressStore progressStore;
    private PuzzlePreloader puzzlePreloader;
    private PathProgressStore pathProgressStore;
    private final Map<String, Board> sessionBoards = new HashMap<>();
    private Board activeBoard;
    private Difficulty currentDifficulty;
    private int currentLevel;
    private int loadRequestId;

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        this.progressStore = new ProgressStore();
        this.puzzlePreloader = new PuzzlePreloader();
        this.pathProgressStore = new PathProgressStore();
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

        queueInitialPreloads();
        showMainMenu();
        stage.show();
    }

    @Override
    public void stop() {
        saveSessionBoards();
        if (puzzlePreloader != null) {
            puzzlePreloader.shutdown();
        }
    }

    private void showMainMenu() {
        loadRequestId++;
        saveSessionBoards();
        if (activeBoard != null) {
            activeBoard.clearListeners();
            activeBoard = null;
        }
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

        sizeStageForInitialScene();
    }

    private void startDifficulty(Difficulty difficulty) {
        currentDifficulty = difficulty;
        currentLevel = progressStore.getLevel(currentDifficulty);
        progressStore.setSelectedDifficulty(currentDifficulty);
        progressStore.save();
        puzzlePreloader.preloadAround(currentDifficulty, currentLevel, 10);
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
        int level = currentLevel;
        int requestId = ++loadRequestId;
        Board sessionBoard = sessionBoards.get(boardKey(difficulty, level));
        if (sessionBoard != null) {
            showLoadedBoard(difficulty, level, settings, sessionBoard);
            return;
        }

        var boardFuture = puzzlePreloader.loadBoard(difficulty, level);
        if (!boardFuture.isDone()) {
            showLoading(difficulty, level);
        }

        boardFuture.whenComplete((board, error) -> Platform.runLater(() -> {
            if (requestId != loadRequestId) {
                return;
            }
            if (error != null) {
                showLoadError(error);
                return;
            }
            pathProgressStore.load(difficulty, level).ifPresent(board::restorePlayerPaths);
            sessionBoards.put(boardKey(difficulty, level), board);
            showLoadedBoard(difficulty, level, settings, board);
        }));
    }

    private void showLoadedBoard(Difficulty difficulty, int level, GameSettings settings, Board board) {
        currentDifficulty = difficulty;
        currentLevel = level;
        activeBoard = board;
        BoardPanel boardPanel = new BoardPanel(settings);
        boardPanel.setAlignment(Pos.CENTER);

        board.clearListeners();
        PrimaryController controller = new PrimaryController(board, boardPanel, this::advanceLevel);
        board.addListener(controller);
        boardPanel.addListener(controller);

        repaintBoard(board, boardPanel, settings);

        root.setTop(createGameTopBar());
        root.setCenter(boardPanel);
        root.setPrefSize(settings.getSceneWidth(), settings.getSceneHeight() + 48);
        updateTopBar();

        sizeStageForInitialScene();
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

        sizeStageForInitialScene();
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

    private void sizeStageForInitialScene() {
        if (stage != null && !stage.isShowing() && !stage.isFullScreen()) {
            stage.sizeToScene();
        }
    }

    private void repaintBoard(Board board, BoardPanel boardPanel, GameSettings settings) {
        for (int row = 0; row < settings.getRows(); row++) {
            for (int col = 0; col < settings.getCols(); col++) {
                Cell cell = board.getCell(row, col);
                var dirs = board.getPrevNextDirs(row, col);
                boardPanel.setColor(
                    row,
                    col,
                    cell.isFixed() ? cell.getSolutionState() : cell.getPlayerState(),
                    cell.isFixed(),
                    dirs[0],
                    dirs[1]
                );
            }
        }
    }

    private void advanceLevel() {
        currentLevel = progressStore.incrementLevel(currentDifficulty);
        progressStore.setSelectedDifficulty(currentDifficulty);
        progressStore.save();
        puzzlePreloader.preloadAround(currentDifficulty, currentLevel, 10);

        PauseTransition pause = new PauseTransition(Duration.millis(450));
        pause.setOnFinished(e -> loadLevel(currentDifficulty));
        pause.play();
    }

    private void updateTopBar() {
        levelLabel.setText(currentDifficulty + " - Level " + currentLevel);
    }

    private void queueInitialPreloads() {
        Set<Difficulty> queued = new HashSet<>();
        queuePreload(currentDifficulty, queued);
        queuePreload(Difficulty.HARD, queued);
        for (Difficulty difficulty : Difficulty.values()) {
            queuePreload(difficulty, queued);
        }
    }

    private void queuePreload(Difficulty difficulty, Set<Difficulty> queued) {
        if (queued.add(difficulty)) {
            puzzlePreloader.preloadAround(difficulty, progressStore.getLevel(difficulty), 10);
        }
    }

    private String boardKey(Difficulty difficulty, int level) {
        return difficulty.getKey() + ":" + Math.max(1, level);
    }

    private void saveSessionBoards() {
        for (Map.Entry<String, Board> entry : sessionBoards.entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length != 2) {
                continue;
            }

            try {
                Difficulty difficulty = Difficulty.fromKey(parts[0]);
                int level = Integer.parseInt(parts[1]);
                pathProgressStore.save(difficulty, level, entry.getValue());
            } catch (NumberFormatException e) {
                // Ignore malformed in-memory keys; they are only created by this class.
            }
        }
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
