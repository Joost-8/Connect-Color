package com.connectcolor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import com.connectcolor.View.BoardPanel;
import com.connectcolor.Model.Board;
import com.connectcolor.Controllers.PrimaryController;
import com.connectcolor.Model.Cell;
import javafx.scene.input.KeyCode;
import java.io.IOException;
import com.connectcolor.Util.Dir;
import com.connectcolor.Util.GameSettings;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {

        GameSettings settings = GameSettings.expert();
        Board board = new Board(settings);
        BoardPanel boardPanel = new BoardPanel(settings);
        boardPanel.setAlignment(Pos.CENTER);


        scene = new Scene(boardPanel, settings.getSceneWidth(), settings.getSceneHeight());
        scene.getStylesheets().add(
        App.class.getResource("/com/app/styles.css").toExternalForm()
        );

        PrimaryController controller = new PrimaryController(board, boardPanel);

        board.addListener(controller);
        boardPanel.addListener(controller);

        // repaint fixed cells manually (since notifications were before listener was added)
        Platform.runLater(() -> {
            for(Cell cell : board.getFixedCells()) {
            boardPanel.setColor(cell.getRow(), cell.getCol(), cell.getSolutionState(), cell.isFixed(), null,  null);
            }
        });

        stage.setScene(scene);
        stage.setTitle("Connect Color");
        stage.show();
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
