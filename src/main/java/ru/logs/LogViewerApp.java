package ru.logs;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LogViewerApp extends Application {
    private static final String LOG_URL = "https://curly-space-xylophone-q7q7w47j75wjhx66j-8000.app.github.dev/logs";
    private final HttpClient client = HttpClient.newHttpClient();
    private final TextArea logArea = new TextArea();
    private final Button fetchBtn = new Button("Загрузить логи");

    @Override
    public void start(Stage stage) {
        fetchBtn.setMaxWidth(Double.MAX_VALUE);
        fetchBtn.setOnAction(e -> loadLogs());
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 13px;");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        VBox root = new VBox(10, fetchBtn, logArea);
        root.setPadding(new Insets(12));
        stage.setTitle("Просмотр веб-логов");
        stage.setScene(new Scene(root, 650, 450));
        stage.show();
    }

    private void loadLogs() {
        fetchBtn.setDisable(true);
        logArea.setText("Загрузка логов...\n");
        var req = HttpRequest.newBuilder(URI.create(LOG_URL)).build();

        client.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .handle((res, ex) -> {
                    Platform.runLater(() -> {
                        logArea.setText(ex != null ? "Ошибка: " + ex.getMessage() :
                                res.statusCode() == 200 ? res.body() : "Код ошибки: " + res.statusCode());
                        fetchBtn.setDisable(false);
                    });
                    return null;
                });
    }
}
