package ru.logs;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LogViewerApp extends Application {
    // Константа для пункта меню, отключающего фильтрацию по заказу
    private static final String ALL = "Все заказы";

    // Сервис для загрузки логов и элементы управления
    private final LogService service = new LogService();
    private final TextArea logArea = new TextArea();
    private final TableView<LogEntry> table = new TableView<>();
    private final Button fetchBtn = new Button("Загрузить");
    private final ComboBox<String> orderBox = new ComboBox<>();
    private final ToggleButton viewBtn = new ToggleButton();

    // Кэш всех загруженных записей для мгновенной фильтрации без повторных запросов
    private List<LogEntry> entries = List.of();

    @Override
    public void start(Stage stage) {
        // Настройка выпадающего списка заказов (разрешаем ручной ввод и фильтрацию на лету)
        orderBox.setEditable(true);
        orderBox.setValue(ALL);
        orderBox.getEditor().textProperty().addListener((o, old, val) -> render());

        // Привязка загрузки логов к кнопке
        fetchBtn.setOnAction(e -> load());

        // Настройка области для сырого текста логов
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: monospace;");

        // Создание столбцов таблицы с процентным соотношением ширины
        addCol("Время", LogEntry::timestamp, 0.17);
        addCol("Заказ", LogEntry::orderId, 0.09);
        addCol("Попытка", LogEntry::attemptId, 0.08);
        addCol("Код", LogEntry::code, 0.22);
        addCol("Описание", e -> CodeDictionary.describe(e.code()), 0.44);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Растягиваем текстовое поле и таблицу на всю доступную высоту окна
        VBox.setVgrow(logArea, Priority.ALWAYS);
        VBox.setVgrow(table, Priority.ALWAYS);

        // Компоновка окна: верхняя панель управления + основная рабочая область (по умолчанию сырой вид)
        var root = new VBox(8, new HBox(8, fetchBtn, orderBox, viewBtn), logArea);
        root.setPadding(new Insets(10));

        // Автоматическая смена текста кнопки и подмена содержимого (текст <-> таблица) при нажатии
        viewBtn.textProperty().bind(Bindings.when(viewBtn.selectedProperty()).then("Сырой вид").otherwise("Таблица"));
        viewBtn.setOnAction(e -> root.getChildren().set(1, viewBtn.isSelected() ? table : logArea));

        // Инициализация и показ окна
        stage.setTitle("Логи");
        stage.setScene(new Scene(root, 950, 480));
        stage.show();
    }


     // Создаёт столбец таблицы, привязывает его к полю модели и задаёт пропорциональную ширину.
    private void addCol(String title, Function<LogEntry, String> fn, double ratio) {
        var col = new TableColumn<LogEntry, String>(title);
        col.setCellValueFactory(d -> new SimpleStringProperty(fn.apply(d.getValue())));
        col.prefWidthProperty().bind(table.widthProperty().multiply(ratio));
        table.getColumns().add(col);
    }


     // Асинхронно загружает логи с сервера и обновляет список доступных заказов.
    private void load() {
        fetchBtn.setDisable(true);
        logArea.setText("Загрузка...");

        service.fetchLogs().whenComplete((data, ex) -> Platform.runLater(() -> {
            fetchBtn.setDisable(false);
            if (ex != null) {
                logArea.setText("Ошибка: " + ex.getMessage());
                return;
            }
            entries = data;

            // Заполняем выпадающий список уникальными номерами заказов
            orderBox.getItems().setAll(ALL);
            data.stream().map(LogEntry::orderId).distinct().sorted().forEach(orderBox.getItems()::add);
            orderBox.setValue(ALL);

            render();
        }));
    }


    // Фильтрует записи по выбранному заказу, сортирует по времени и обновляет оба представления.
    private void render() {
        String filter = orderBox.getEditor().getText().trim().toLowerCase();
        boolean isAll = filter.isEmpty() || filter.equals(ALL.toLowerCase());

        // Фильтрация по заказу и хронологическая сортировка
        var filtered = entries.stream()
                .filter(e -> isAll || e.orderId().toLowerCase().contains(filter))
                .sorted(Comparator.comparing(LogEntry::timestamp))
                .toList();

        // Обновление данных в таблице
        table.getItems().setAll(filtered);

        // Обновление данных в сыром текстовом виде
        String text = filtered.stream().map(LogEntry::rawLine).collect(Collectors.joining("\n"));
        logArea.setText(text.isEmpty() && !entries.isEmpty() ? "Ничего не найдено" : text);
    }
}
