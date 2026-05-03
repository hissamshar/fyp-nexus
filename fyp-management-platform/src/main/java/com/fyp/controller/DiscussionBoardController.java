package com.fyp.controller;

import com.fyp.model.*;
import com.fyp.service.DiscussionService;
import com.fyp.util.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DiscussionBoardController implements Initializable {

    @FXML private ListView<DiscussionThread> threadList;
    @FXML private Label threadTitleLabel;
    @FXML private VBox postsContainer;
    @FXML private TextArea replyField;

    private final DiscussionService discussionService = new DiscussionService();
    private DiscussionThread selectedThread;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        threadList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DiscussionThread t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    // Fix Issue 11 — always clear style/text/graphic on empty cells
                    setText(null);
                    setGraphic(null);
                    setStyle(null);
                } else {
                    setText(t.getTitle());
                    setStyle("-fx-text-fill:#F8FAFC; -fx-padding:10; -fx-font-size:13; -fx-background-color:transparent;");
                }
            }
        });
        threadList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) loadPosts(n);
        });
        loadThreads();
    }

    private void loadThreads() {
        Task<List<DiscussionThread>> t = new Task<>() {
            @Override protected List<DiscussionThread> call() throws Exception {
                return discussionService.getThreadsForCurrentProject(SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() -> {
            List<DiscussionThread> threads = t.getValue();
            threadList.setItems(FXCollections.observableArrayList(threads));
            if (threads.isEmpty()) {
                threadTitleLabel.setText("No threads yet — create one with the + button");
            }
        }));
        t.setOnFailed(e -> Platform.runLater(() ->
            threadTitleLabel.setText("Failed to load threads.")));
        new Thread(t).start();
    }

    private void loadPosts(DiscussionThread thread) {
        selectedThread = thread;
        threadTitleLabel.setText(thread.getTitle());
        Task<List<Post>> t = new Task<>() {
            @Override protected List<Post> call() throws Exception {
                return discussionService.getPostsForThread(thread.getThreadId(), SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() -> renderPosts(t.getValue())));
        t.setOnFailed(e -> Platform.runLater(() -> postsContainer.getChildren().clear()));
        new Thread(t).start();
    }

    private void renderPosts(List<Post> posts) {
        postsContainer.getChildren().clear();
        User me = SessionManager.getCurrentUser();
        if (posts.isEmpty()) {
            Label empty = new Label("No posts yet. Be the first to reply!");
            empty.setStyle("-fx-text-fill:#64748B; -fx-font-size:13; -fx-padding:12;");
            postsContainer.getChildren().add(empty);
            return;
        }
        for (Post p : posts) {
            boolean isMe = me != null && me.getUserId().equals(p.getAuthorId());
            VBox bubble = new VBox(4);
            bubble.setPadding(new Insets(10, 14, 10, 14));
            bubble.setMaxWidth(480);
            bubble.setStyle("-fx-background-color:" + (isMe ? "rgba(99,102,241,0.2)" : "#1E293B") +
                ";-fx-background-radius:10;");

            Label author = new Label(isMe ? "You" : "Member");
            author.setStyle("-fx-text-fill:#818CF8; -fx-font-size:11; -fx-font-weight:bold;");
            Label content = new Label(p.getContent());
            content.setStyle("-fx-text-fill:#F8FAFC; -fx-font-size:13;");
            content.setWrapText(true);

            bubble.getChildren().addAll(author, content);

            HBox row = new HBox(bubble);
            row.setPadding(new Insets(4, 8, 4, 8));
            if (isMe) row.setStyle("-fx-alignment:center-right;");
            postsContainer.getChildren().add(row);
        }
    }

    @FXML
    private void handleSendReply() {
        if (selectedThread == null || replyField.getText().trim().isEmpty()) return;
        String content = replyField.getText().trim();
        Task<Post> t = new Task<>() {
            @Override protected Post call() throws Exception {
                return discussionService.addPostUI(selectedThread.getThreadId(), content, SessionManager.getJwtToken());
            }
        };
        t.setOnSucceeded(e -> Platform.runLater(() -> {
            replyField.clear();
            loadPosts(selectedThread);
        }));
        t.setOnFailed(e -> Platform.runLater(() -> {
            // Show feedback in a non-intrusive way
            replyField.setStyle("-fx-border-color:#F87171;");
        }));
        new Thread(t).start();
    }

    @FXML
    private void handleNewThread() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("New Thread");
        dlg.setHeaderText("Create a new discussion thread");
        dlg.setContentText("Thread title:");
        dlg.getEditor().setStyle("-fx-background-color:#1E293B;-fx-text-fill:#F8FAFC;");
        dlg.showAndWait().ifPresent(title -> {
            if (title.trim().isEmpty()) return;
            Task<DiscussionThread> t = new Task<>() {
                @Override protected DiscussionThread call() throws Exception {
                    return discussionService.createThread(title.trim(), SessionManager.getJwtToken());
                }
            };
            t.setOnSucceeded(e -> Platform.runLater(() -> {
                if (t.getValue() != null) {
                    loadThreads();
                } else {
                    threadTitleLabel.setText("Failed to create thread — no active project found.");
                }
            }));
            new Thread(t).start();
        });
    }
}
