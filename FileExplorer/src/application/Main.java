package application;

import static java.util.logging.Level.SEVERE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

//This program will display all .txt files in a user selected directory and allow basic edit functionality
//Created through combining methods from a variety of sources with some edits to fit desired functionality

//I did think of and not implement some minor changes. Having one single class do everything is in this case feasible and manageable, 
//but likely not ideal. Not sure how it may function for JavaFx applications, but following more of a model-view-controller layout
//could have been preferred.

//Secondly, I noticed some sources I used utilized an FXML structure for creating layouts
//I got a slight impression this might be standard on larger scale applications, but I am familiar with XML and very
//comfortable with HTML, whereas I am entirely new to JavaFX, so I have opted to run everything here within the java
public class Main extends Application {
	private TabPane tabPane;
	private File activeDirectory;
	private TreeView<File> directoryTree;
	
	private Label errorLabel;

	public void initializeVariables() {
		String defaultStartingDirectory = "C:\\Users\\owent\\Documents";

		tabPane = new TabPane();
		tabPane.setOnKeyReleased(keyReleaseEvent());
		activeDirectory = new File(defaultStartingDirectory);
		directoryTree = new TreeView<File>();
		directoryTree.setOnMouseClicked(getDoubleClickEvent());
		errorLabel = new Label();
		errorLabel.setPadding(new Insets(5));
	}

	@Override
	public void start(Stage primaryStage) {
		initializeVariables();
		Button changeDirectory = new Button("Change Directory");
		changeDirectory.setText("Change Directory");
	    changeDirectory.setOnAction(
	        new EventHandler<ActionEvent>() {
	            @Override
	            public void handle(ActionEvent event) {
	                final Stage dialog = new Stage();
	                dialog.initModality(Modality.APPLICATION_MODAL);
	                dialog.initOwner(primaryStage);
	                VBox dialogVbox = new VBox(20);
	                Button openFileExplorer = new Button("Open File Explorer");
	        		openFileExplorer.setOnAction(new EventHandler<ActionEvent>() {
	        			@Override
	        			public void handle(ActionEvent openFileChooser) {
	        				DirectoryChooser directoryChooser = new DirectoryChooser();
	        				directoryChooser.setInitialDirectory(activeDirectory);
	        				File chosenFile = directoryChooser.showDialog(primaryStage);
	        				OpenDirectory(chosenFile);
	        				dialog.close();
	        			}
	        		});
	        		HBox enterDirectoryHbox = new HBox();
	        		Label enterDirectoryLabel = new Label("Enter a Directory");
	        		TextField enterDirectoryTextField = new TextField();
	        		//optional extension - prefill some text such as C://users/owent/
	        		enterDirectoryTextField.setPromptText("Enter a directory; C://");	
	        		Button enterDirectoryButton = new Button("Select Directory");
	        		enterDirectoryButton.setOnAction(new EventHandler<ActionEvent>() {
	        			@Override
	        			public void handle(ActionEvent selectDirectory) {
	        				OpenDirectory(new File(enterDirectoryTextField.getText()));
	        				dialog.close();
	        			}
	        		});
	        		enterDirectoryHbox.getChildren().addAll(enterDirectoryLabel, enterDirectoryTextField, enterDirectoryButton);
	                dialogVbox.getChildren().addAll(openFileExplorer, enterDirectoryHbox);
	                Scene dialogScene = new Scene(dialogVbox, 500, 200);
	                dialog.setScene(dialogScene);
	                dialog.show();
	            }
	         });

		OpenDirectory(activeDirectory);

		BorderPane root = new BorderPane();

		SplitPane mainView = new SplitPane();
		BorderPane headerView = new BorderPane();
		headerView.setLeft(changeDirectory);
		headerView.setRight(errorLabel);

		root.setTop(headerView);
		root.setCenter(mainView);
		BorderPane.setMargin(headerView, new Insets(5));

		mainView.getItems().addAll(directoryTree, tabPane);
		primaryStage.setScene(new Scene(root, 600, 400));
		primaryStage.setTitle("Folder View");
		primaryStage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

	// Returns a TreeItem representation of the given directory
	private TreeItem<File> getNodesForDirectory(File directory) {
		TreeItem<File> root = new TreeItem<File>(directory);
		if(!directory.isDirectory()) {
			errorLabel.setText("Provided root was not a directory!");
			return root;
		}
		if(directory.listFiles() == null) {
			System.out.println(directory.getPath()+" for some reason listed files as null");
			return root; //BAD! Fixes an error in selecting documents, but I'm not sure why it gives null when no other directory does
		}
		for (File f : directory.listFiles()) {
			if (f.isDirectory()) { // Then we call the function recursively
				root.getChildren().add(getNodesForDirectory(f));
			} else {
				if(f.getPath().trim().endsWith("txt")) {
					root.getChildren().add(new TreeItem<File>(f));
				}
			}
		}
		return root;
	}

	// On double click, assert clicked area is a tree node and open associated file
	private EventHandler<MouseEvent> getDoubleClickEvent() {
		return new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent click) {
				if (click.getClickCount() == 2) {
					TreeItem<File> item = directoryTree.getSelectionModel().getSelectedItem();
					if(item != null) {
						File file = item.getValue();
			            if(!file.isDirectory()) {
			            	System.out.println("Loading: "+file);
			            	Task<String> loadFileTask = fileLoaderTask(file);
			            	loadFileTask.run();
			            }
					}
				}
			}
		};
	}
	// ========End of getDoubleClickEvent=======

	private void OpenDirectory(File fileToOpen) {
		if (fileToOpen == null || !fileToOpen.isDirectory()) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setHeaderText("Could not open directory");
			alert.setContentText("The file is invalid.");

			alert.showAndWait();
		} else {  
			directoryTree.setRoot(getNodesForDirectory(fileToOpen));
			directoryTree.setCellFactory(tree -> {
	            TreeCell<File> cell = new TreeCell<File>() {
	                @Override
	                public void updateItem(File item, boolean empty) {
	                    super.updateItem(item, empty) ;
	                    if (empty || item == null) {
	                        setText(null);
	                        setGraphic(null);
	                    } else {
	                        setText(item.getName());
	                        if(item.isDirectory()) {
	                        	Image image = new Image(getClass().getResourceAsStream("icon.png"));
	                        	ImageView icon = new ImageView(image);
	                        	icon.setFitHeight(15);
	                        	icon.setFitWidth(17);
	                        	setGraphic(icon);
	                        }
	                    }
	                }
	            };
	            return cell;
	        });
			activeDirectory = fileToOpen;
		}
	}
	
	private Task<String> fileLoaderTask(File fileToLoad){
	    //Create a task to load the file asynchronously
	    Task<String> loadFileTask = new Task<>() {
	        @Override
	        protected String call() throws Exception {
	            BufferedReader reader = new BufferedReader(new FileReader(fileToLoad));

	            //Use Files.lines() to calculate total lines - used for progress
	            long lineCount;
	            try (Stream<String> stream = Files.lines(fileToLoad.toPath())) {
	                lineCount = stream.count();
	            }

	            //Load in all lines one by one into a StringBuilder separated by "\n" - compatible with TextArea
	            String line;
	            StringBuilder totalFile = new StringBuilder();
	            long linesLoaded = 0;
	            while((line = reader.readLine()) != null) {
	                totalFile.append(line);
	                totalFile.append("\n");
	                updateProgress(++linesLoaded, lineCount);
	            }
	            
	            reader.close();
	            return totalFile.toString();
	        }
	    };

	    //If successful, update the text area, display a success message and store the loaded file reference
	    loadFileTask.setOnSucceeded(workerStateEvent -> {
	        try {
	            Tab newTab = new Tab();
	            newTab.setText(fileToLoad.getName());
	            TextArea textArea = new TextArea();
	            textArea.setText(loadFileTask.get());
	            newTab.setContent(textArea);
	            tabPane.getTabs().add(newTab);
	        } catch (InterruptedException | ExecutionException e) {
	            Logger.getLogger(getClass().getName()).log(SEVERE, null, e);
	            errorLabel.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
	        }
	    });

	    //If unsuccessful, set text area with error message and status message to failed
	    loadFileTask.setOnFailed(workerStateEvent -> {
	        errorLabel.setText("Could not load file from:\n " + fileToLoad.getAbsolutePath());
	    });

	    return loadFileTask;
	}
	
	private EventHandler<KeyEvent> keyReleaseEvent() {
		return new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent release) {
				Tab openTab = tabPane.getSelectionModel().getSelectedItem();
				if(openTab == null) {
					errorLabel.setText("Active Tab not found, cannot update");
					return;
				}
				String currentText = openTab.getText();
				if(!currentText.endsWith("*")) {
					openTab.setText(currentText+"*");
				}
			}
		};
	}
}
