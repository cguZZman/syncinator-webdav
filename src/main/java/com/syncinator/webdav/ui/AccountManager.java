package com.syncinator.webdav.ui;

import com.syncinator.webdav.model.Account;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class AccountManager extends Application {
	private final TableView<Account> table = new TableView<Account>();
	

	@Override
	@SuppressWarnings("unchecked")
	public void start(Stage primaryStage) throws Exception {
		BorderPane border = new BorderPane();
		TableColumn<Account, String> name = new TableColumn<Account, String>("Provider");
		name.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
		TableColumn<Account, String> owner = new TableColumn<Account, String>("Owner");
		owner.prefWidthProperty().bind(table.widthProperty().multiply(0.4));
		TableColumn<Account, String> driveId = new TableColumn<Account, String>("Drive Id");
		driveId.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
		table.getColumns().addAll(name, owner, driveId);
		table.setPlaceholder(new Label());
		VBox optionBox = new VBox(10);
		optionBox.setPrefWidth(100);
		optionBox.setPadding(new Insets(25, 0, 0, 10));
		Button button = new Button("Add", new ImageView(new Image(getClass().getResourceAsStream("/images/add.gif"))));
		button.setAlignment(Pos.CENTER_LEFT);
		button.prefWidthProperty().bind(optionBox.prefWidthProperty());
		button.setOnAction(e -> {
			AccountWizard wizard = new AccountWizard();
			wizard.initOwner(primaryStage);
			wizard.show();
		});
		optionBox.getChildren().add(button);
		
		button = new Button("Details", new ImageView(new Image(getClass().getResourceAsStream("/images/gear.png"))));
		button.setAlignment(Pos.CENTER_LEFT);
		button.prefWidthProperty().bind(optionBox.prefWidthProperty());
		button.setDisable(true);
		optionBox.getChildren().add(button);
		button = new Button("Remove", new ImageView(new Image(getClass().getResourceAsStream("/images/delete.gif"))));
		button.setAlignment(Pos.CENTER_LEFT);
		button.prefWidthProperty().bind(optionBox.prefWidthProperty());
		button.setDisable(true);
		optionBox.getChildren().add(button);
		Label title = new Label("Cloud Accounts");
		title.setFont(Font.font(18));
		title.setPrefHeight(32);
		title.setAlignment(Pos.TOP_LEFT);
		border.setTop(title);
		border.setCenter(table);
		border.setRight(optionBox);
		border.setPadding(new Insets(5, 10, 10, 10));
		Scene scene = new Scene(border);
		primaryStage.setTitle("Syncinator WebDAV");
		primaryStage.setScene(scene);
		primaryStage.setWidth(700);
		primaryStage.setHeight(400);
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/network-drive-16.png")));
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/network-drive-24.png")));
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/network-drive-32.png")));
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/network-drive-48.png")));
        primaryStage.show();
        border.requestFocus();
	}

	public static void main(String[] args) {
		launch(args);
	}
}
