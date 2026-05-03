package com.fyp.controller;

import com.fyp.model.Notification;
import com.fyp.service.NotificationService;
import com.fyp.util.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationPanelController implements Initializable {

    @FXML private VBox notifContainer;

    private final NotificationService notifService = new NotificationService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadNotifications();
    }

    private void loadNotifications() {
        Task<List<Notification>> t = new Task<>() {
            @Override protected List<Notification> call() throws Exception {
                return notifService.getNotificationsForUser(SessionManager.getCurrentUser().getUserId(),
                    SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() -> renderNotifications(t.getValue())));
        new Thread(t).start();
    }

    private void renderNotifications(List<Notification> notifs) {
        notifContainer.getChildren().clear();
        if (notifs.isEmpty()) {
            Label empty = new Label("🎉  You're all caught up! No new notifications.");
            empty.setStyle("-fx-text-fill:#64748B; -fx-font-size:14; -fx-padding:24;");
            notifContainer.getChildren().add(empty);
            return;
        }
        for (Notification n : notifs) {
            HBox row = new HBox(12);
            row.setPadding(new Insets(14, 20, 14, 20));
            row.setStyle("-fx-border-width:0 0 1 0; -fx-border-color:#1E293B;" +
                (n.isRead() ? "" : "-fx-background-color:rgba(99,102,241,0.05);"));

            VBox info = new VBox(4);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label msg = new Label(n.getMessage());
            msg.setStyle("-fx-text-fill:" + (n.isRead() ? "#94A3B8" : "#F8FAFC") +
                "; -fx-font-size:14;" + (n.isRead() ? "" : "-fx-font-weight:bold;"));
            msg.setWrapText(true);

            Label time = new Label(n.getCreatedAt() != null ? n.getCreatedAt().toString().substring(0, 16) : "");
            time.setStyle("-fx-text-fill:#475569; -fx-font-size:11;");
            info.getChildren().addAll(msg, time);

            if (!n.isRead()) {
                Button markRead = new Button("✓");
                markRead.setStyle("-fx-background-color:transparent; -fx-text-fill:#818CF8; -fx-cursor:hand; -fx-font-size:16;");
                markRead.setOnAction(e -> markRead(n));
                row.getChildren().addAll(info, markRead);
            } else {
                row.getChildren().add(info);
            }
            notifContainer.getChildren().add(row);
        }
    }

    private void markRead(Notification n) {
        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return notifService.markRead(n.getNotificationId(), SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> loadNotifications());
        new Thread(t).start();
    }

    @FXML
    private void handleMarkAllRead() {
        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return notifService.markAllRead(SessionManager.getCurrentUser().getUserId(),
                    SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> loadNotifications());
        new Thread(t).start();
    }
}
