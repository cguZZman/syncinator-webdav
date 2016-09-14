package com.syncinator.webdav.ui;

import com.syncinator.webdav.model.Provider;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AccountWizard extends Stage {
	private String step = "provider";
	private Label title;
	private ListView<Provider> providerView;
	private WebView loginView;
	private Button nextButton;
	private Button backButton;
	
	public AccountWizard(){
		setTitle("Add an account...");
		BorderPane layout = new BorderPane();
		layout.setPadding(new Insets(5, 10, 10, 10));
		
		title = new Label("Select your provider:");
		title.setPrefHeight(32);
		title.setAlignment(Pos.TOP_LEFT);
		layout.setTop(title);
		
		HBox navigationBar = new HBox(2);
		navigationBar.setPrefHeight(50);
		navigationBar.setAlignment(Pos.CENTER_RIGHT);
		backButton = new Button("< Back");
		backButton.setDisable(true);
		backButton.setPrefWidth(80);
		backButton.setOnAction(e -> {previousStep();});
		navigationBar.getChildren().add(backButton);
		nextButton = new Button("Next >");
		nextButton.setPrefWidth(80);
		nextButton.setOnAction(e -> {nextStep();});
		navigationBar.getChildren().add(nextButton);
		Region spacer = new Region();
		spacer.setPrefWidth(10);
		navigationBar.getChildren().add(spacer);
		Button button = new Button("Cancel");
		button.setPrefWidth(80);
		button.setOnAction(e -> {close();});
		navigationBar.getChildren().add(button);
		layout.setBottom(navigationBar);
		
		layout.setCenter(getProviderView());
		Scene scene = new Scene(layout);
		scene.getStylesheets().add("css/styles.css");
		scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {if (e.getCode().equals(KeyCode.ESCAPE)) close();});
		setScene(scene);
		setWidth(450);
		setHeight(500);
		initModality(Modality.WINDOW_MODAL);
	}
	
	private void nextStep(){
		BorderPane layout = (BorderPane)getScene().getRoot();
		if (step.equals("provider")){
			step = "login";
			backButton.setDisable(false);
			layout.setCenter(getLoginView());
			loadLogin();
			title.setText("Login into the account:");
		}
	}
	private void previousStep(){
		BorderPane layout = (BorderPane)getScene().getRoot();
		if (step.equals("login")){
			step = "provider";
			backButton.setDisable(true);
			layout.setCenter(getProviderView());
			title.setText("Select your provider:");
		}
	}
	
	private ListView<Provider> getProviderView(){
		if (providerView == null){
			providerView = new ListView<Provider>();
			ObservableList<Provider> items = FXCollections.observableArrayList(
				new Provider("onedrive", "OneDrive", getClass().getResource("/images/provider/onedrive.png").toString()),
				new Provider("gdrive", "Google Drive", getClass().getResource("/images/provider/google-drive.png").toString())
			);
			providerView.setItems(items);
			providerView.setCellFactory((ListView<Provider> param) -> { return new ProviderCell(); });
		}
		return providerView;
	}
	
	private WebView getLoginView(){
		if (loginView == null){
			loginView = new WebView();
			loginView.getEngine().locationProperty().addListener((o, old, location) -> {
				System.out.println(location);
			});
		}
		return loginView;
	}
	
	private void loadLogin(){
		getLoginView().getEngine().load("https://login.live.com/oauth20_authorize.srf?client_id=0000000048145120&scope=wl.offline_access+wl.skydrive&response_type=code&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf&display=touch&state=1111");
	}
	
	static class ProviderCell extends ListCell<Provider> {
        @Override
        public void updateItem(Provider item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
            	HBox box = new HBox(10);
            	box.setAlignment(Pos.CENTER_LEFT);
            	ImageView icon = new ImageView(item.getIconUrl());
            	box.getChildren().add(icon);
            	Label label = new Label(item.getName());
            	box.getChildren().add(label);
                setGraphic(box);
            }
        }
    }
}
