package com.syncinator.webdav.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AccountWizard extends Stage {
	
	public AccountWizard(){
		BorderPane layout = new BorderPane();
		
		Label title = new Label("Select your cloud provider:");
		//title.setFont(Font.font(18));
		title.setPrefHeight(32);
		title.setAlignment(Pos.TOP_LEFT);
		layout.setTop(title);
		
		ListView<String> list = new ListView<>();
		ObservableList<String> items = FXCollections.observableArrayList ("OneDrive", "Google Drive", "Amazon Drive");
		list.setItems(items);
		layout.setCenter(list);
		
		HBox navigationBar = new HBox(2);
		navigationBar.setPrefHeight(50);
		navigationBar.setAlignment(Pos.CENTER_RIGHT);
		Button button = new Button("< Back");
		button.setDisable(true);
		button.setPrefWidth(80);
		navigationBar.getChildren().add(button);
		button = new Button("Next >");
		button.setPrefWidth(80);
		navigationBar.getChildren().add(button);
		Region spacer = new Region();
		spacer.setPrefWidth(10);
		navigationBar.getChildren().add(spacer);
		button = new Button("Cancel");
		button.setPrefWidth(80);
		button.setOnAction(e -> {close();});
		navigationBar.getChildren().add(button);
		layout.setBottom(navigationBar);
		
		layout.setPadding(new Insets(5, 10, 10, 10));
		Scene scene = new Scene(layout);
		setTitle("Add an account...");
		setScene(scene);
		setWidth(450);
		setHeight(500);
		initModality(Modality.WINDOW_MODAL);
	}
}
