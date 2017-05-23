package com.syncinator.webdav.ui;



import java.io.File;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.onedrive.api.OneDrive;
import com.syncinator.webdav.cloud.onedrive.OneDriveConnectionRepository;
import com.syncinator.webdav.model.Provider;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class AccountWizard extends Stage {
	private String step = "select-provider";
	private Label title;
	private ListView<Provider> providerView;
	private VBox centerPane;
	private WebView loginView;
	private ProgressBar progressBar; 
	private Button nextButton;
	private Button backButton;
	private String currentLocation;
	public static final EventType<WindowEvent> ADD_ACCOUNT = new EventType<WindowEvent>(Event.ANY, "ADD_ACCOUNT");
	
	private Provider selectedProvider;
	private String driveId;
	
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
		nextButton.setDisable(true);
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
		setWidth(550);
		setHeight(660);
		initModality(Modality.WINDOW_MODAL);
	}
	
	private void nextStep(){
		BorderPane layout = (BorderPane)getScene().getRoot();
		if (step.equals("select-provider")){
			Provider provider = getProviderView().getSelectionModel().getSelectedItem();
			if (provider != null){
				this.selectedProvider = provider;
				step = "privider-login";
				backButton.setDisable(false);
				nextButton.setDisable(true);
				layout.setCenter(getCenterPane());
				loadLogin();
				title.setText("Login into the account:");
			}
		} else if (step.equals("privider-login")){
			if (currentLocation.contains("error=access_denied")){
				close();
				Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle("Syncinator");
				alert.setHeaderText(null);
				alert.setContentText("No account added. Permission denied by user.");
				alert.initOwner(getOwner());
				alert.showAndWait();
			} else if (currentLocation.contains("code=")){
				UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(currentLocation).build();
				driveId = OneDriveConnectionRepository.addConnectionWithAuthCode(uriComponents.getQueryParams().getFirst("code"));
				fireEvent(new WindowEvent(this, ADD_ACCOUNT));
			}
		}
	}
	private void previousStep(){
		BorderPane layout = (BorderPane)getScene().getRoot();
		if (step.equals("privider-login")){
			step = "select-provider";
			backButton.setDisable(true);
			nextButton.setDisable(false);
			layout.setCenter(getProviderView());
			title.setText("Select your provider:");
		}
	}
	
	private ListView<Provider> getProviderView(){
		if (providerView == null){
			providerView = new ListView<Provider>();
			ObservableList<Provider> items = FXCollections.observableArrayList(Provider.ONEDRIVE, Provider.GDRIVE);
			providerView.setItems(items);
			providerView.setCellFactory((ListView<Provider> param) -> { return new ProviderCell(); });
			providerView.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Provider> observable, Provider oldValue, Provider newValue) -> {
				nextButton.setDisable(newValue == null);
			});
		}
		return providerView;
	}
	
	private WebView getLoginView(){
		if (loginView == null){
			loginView = new WebView();
			loginView.getEngine().setUserDataDirectory(new File(AccountManager.WEBENGINE_BASE_DIR));
			loginView.getEngine().locationProperty().addListener((o, old, location) -> {
				currentLocation = location;
				System.out.println(location);
				if (location.startsWith(OneDrive.MOBILE_REDIRECT_URI)){
					nextStep();
				}
			});
			loginView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
				@Override
				public void changed(ObservableValue ov, State oldState, State newState) {
					getProgressBar().setVisible(newState != State.SUCCEEDED && newState != State.FAILED);
				}
			});
			
			loginView.getEngine().getLoadWorker().exceptionProperty().addListener((o‌​bs, oldExc, newExc) -> {
				if (newExc != null) {
					newExc.printStackTrace();
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Syncinator");
					alert.setHeaderText(null);
					alert.setContentText("Error loading sign-in page: " + newExc.getMessage());
					alert.initOwner(getOwner());
					alert.showAndWait();
				}
			});
		}
		return loginView;
	}
	
	private VBox getCenterPane(){
		if (centerPane == null){
			centerPane = new VBox();
			centerPane.getChildren().addAll(getProgressBar(), getLoginView());
			
		}
		return centerPane;
	}
	
	private ProgressBar getProgressBar(){
		if (progressBar == null){
			progressBar = new ProgressBar();
			progressBar.progressProperty().bind(getLoginView().getEngine().getLoadWorker().progressProperty());
			progressBar.setVisible(false);
			progressBar.prefWidthProperty().bind(getLoginView().prefWidthProperty());
		}
		return progressBar;
	}
	
	private void loadLogin(){
		if (selectedProvider.equals(Provider.ONEDRIVE)) {
			getLoginView().getEngine().load("https://login.live.com/oauth20_authorize.srf?client_id=0000000048145120&scope=offline_access+onedrive.readwrite&response_type=code&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf");
		} else {
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Syncinator");
			alert.setHeaderText(null);
			alert.setContentText("Provider not implemented.");
			alert.initOwner(getOwner());
			alert.showAndWait();
			previousStep();
		}
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
	
	public Provider getSelectedProvider() {
		return selectedProvider;
	}

	public String getDriveId() {
		return driveId;
	}

	private ObjectProperty<EventHandler<WindowEvent>> onAccountAdded;

	public final void setOnAccountAdded(EventHandler<WindowEvent> value) {
		onAccountAddedProperty().set(value);
	}

	public final EventHandler<WindowEvent> getOnAccountAdded() {
		return (onAccountAdded != null) ? onAccountAdded.get() : null;
	}

	public final ObjectProperty<EventHandler<WindowEvent>> onAccountAddedProperty() {
		if (onAccountAdded == null) {
			onAccountAdded = new ObjectPropertyBase<EventHandler<WindowEvent>>() {
				@Override
				protected void invalidated() {
					setEventHandler(ADD_ACCOUNT, get());
				}

				@Override
				public Object getBean() {
					return this;
				}

				@Override
				public String getName() {
					return "onAccountAdded";
				}
			};
		}
		return onAccountAdded;
	}
}
