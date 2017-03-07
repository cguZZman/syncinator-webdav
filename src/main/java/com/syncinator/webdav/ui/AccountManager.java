package com.syncinator.webdav.ui;

import java.io.File;
import java.util.Optional;

import com.onedrive.api.OneDrive;
import com.syncinator.webdav.SyncinatorCookieManager;
import com.syncinator.webdav.SyncinatorWebdavApplication;
import com.syncinator.webdav.cloud.onedrive.IdSerializatorAccessTokenListener;
import com.syncinator.webdav.cloud.onedrive.OneDriveConnectionRepository;
import com.syncinator.webdav.cloud.onedrive.SyncinatorAccessToken;
import com.syncinator.webdav.model.Account;
import com.syncinator.webdav.model.Provider;

import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class AccountManager extends Application {

	public static final String WEBENGINE_BASE_DIR = SyncinatorWebdavApplication.APP_BASE_DIR +  File.separator + "webengine";
	private TableView<Account> table;
	
	@Override
	@SuppressWarnings("unchecked")
	public void start(Stage primaryStage) throws Exception {
		BorderPane layout = new BorderPane();
		layout.setPadding(new Insets(5, 10, 10, 10));
		
		Label title = new Label("Accounts");
		title.setFont(Font.font(18));
		title.setPrefHeight(32);
		title.setAlignment(Pos.TOP_LEFT);
		layout.setTop(title);
		
		table = new TableView<Account>();
		TableColumn<Account, Provider> name = new TableColumn<Account, Provider>("Provider");
		name.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
		name.setCellValueFactory(new PropertyValueFactory<>("provider"));
		name.setCellFactory((TableColumn<Account, Provider> param) -> { return new ProviderCell(); });
		TableColumn<Account, String> owner = new TableColumn<Account, String>("Owner");
		owner.prefWidthProperty().bind(table.widthProperty().multiply(0.39));
		owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
		TableColumn<Account, String> driveId = new TableColumn<Account, String>("Drive Id");
		driveId.prefWidthProperty().bind(table.widthProperty().multiply(0.3));
		driveId.setCellValueFactory(new PropertyValueFactory<>("driveId"));
		table.getColumns().addAll(name, owner, driveId);
		table.setPlaceholder(new Label());
		table.setItems(FXCollections.observableArrayList());
		loadAccounts();
		layout.setCenter(table);
		
		VBox optionBox = new VBox(10);
		optionBox.setPrefWidth(110);
		optionBox.setPadding(new Insets(25, 0, 0, 10));
		Button addButton = new Button("Add", new ImageView(new Image(getClass().getResourceAsStream("/images/add.gif"))));
		addButton.setAlignment(Pos.CENTER_LEFT);
		addButton.prefWidthProperty().bind(optionBox.prefWidthProperty());
		addButton.setOnAction(e -> {
			SyncinatorCookieManager.getCookieManager().getCookieStore().removeAll();
			AccountWizard wizard = new AccountWizard();
			wizard.initOwner(primaryStage);
			wizard.setOnAccountAdded(ev -> {
				wizard.close();
				addAccount(wizard.getSelectedProvider(), wizard.getDriveId());
			});
			wizard.show();
		});
		optionBox.getChildren().add(addButton);
		Button detailsButton = new Button("Details", new ImageView(new Image(getClass().getResourceAsStream("/images/gear.png"))));
		detailsButton.setAlignment(Pos.CENTER_LEFT);
		detailsButton.prefWidthProperty().bind(optionBox.prefWidthProperty());
		detailsButton.setDisable(true);
		optionBox.getChildren().add(detailsButton);
		Button removeButton = new Button("Remove", new ImageView(new Image(getClass().getResourceAsStream("/images/delete.gif"))));
		removeButton.setAlignment(Pos.CENTER_LEFT);
		removeButton.prefWidthProperty().bind(optionBox.prefWidthProperty());
		removeButton.setDisable(true);
		removeButton.setOnAction(e -> {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Syncinator");
			alert.setHeaderText(null);
			alert.setContentText("Are you sure you want to remove the account?");
			alert.initOwner(primaryStage);
			Optional<ButtonType> result  = alert.showAndWait();
			if (result.get() == ButtonType.OK){
				Account account = table.getSelectionModel().getSelectedItem();
				table.getItems().remove(account);
				removeAccount(account);
			}
		});
		optionBox.getChildren().add(removeButton);
		layout.setRight(optionBox);
		
		table.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Account> observable, Account oldValue, Account newValue) -> {
			boolean selected = newValue != null;
			detailsButton.setDisable(!selected);
			removeButton.setDisable(!selected);
		});
		
		Scene scene = new Scene(layout);
		scene.getStylesheets().add("css/styles.css");
		primaryStage.setTitle("Syncinator WebDAV");
		primaryStage.setScene(scene);
		primaryStage.setWidth(700);
		primaryStage.setHeight(400);
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/network-drive-16.png")));
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/network-drive-24.png")));
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/network-drive-32.png")));
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/network-drive-48.png")));
        primaryStage.show();
        layout.requestFocus();
	}
	
	public void loadAccounts(){
		table.getItems().clear();
		
		File dir = new File(new IdSerializatorAccessTokenListener(null).getApplicationFolder());
		if (dir.exists() && dir.isDirectory()){
			for (File file : dir.listFiles()){
				if (file.getName().endsWith(IdSerializatorAccessTokenListener.ACCESS_TOKEN_EXTENSION)){
					String id = file.getName().substring(0, file.getName().length() - IdSerializatorAccessTokenListener.ACCESS_TOKEN_EXTENSION.length());
					OneDriveConnectionRepository.addConnectionFromDisk(id);
					addAccount(Provider.ONEDRIVE, id);
				}
			}
		}
	}
	public void removeAccount(Account account){
		if (account.getProvider().equals(Provider.ONEDRIVE)){
			OneDriveConnectionRepository.removeConnection(account.getDriveId());
			File file = new File(new IdSerializatorAccessTokenListener(null).getApplicationFolder(), account.getDriveId() + IdSerializatorAccessTokenListener.ACCESS_TOKEN_EXTENSION);
			if (file.exists()) {
				file.delete();
			}
		}
	}
	public void addAccount(Provider provider, String driveId){
		if (provider.equals(Provider.ONEDRIVE)){
			OneDrive onedrive = OneDriveConnectionRepository.getConnection(driveId);
			SyncinatorAccessToken accessToken = (SyncinatorAccessToken) onedrive.getAccessTokenListener().onAccessTokenRequired(onedrive);
			table.getItems().add(new Account(provider, accessToken.getOwner(), driveId));	
		}
	}
	
	static class ProviderCell extends TableCell<Account, Provider> {
		@Override
		protected void updateItem(Provider item, boolean empty) {
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
	
	public static void main(String[] args) {
		launch(args);
	}
}
