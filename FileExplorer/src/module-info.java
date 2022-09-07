module FileExplorer {
	requires javafx.controls;
	requires javafx.graphics;
	requires javafx.base;
	requires java.logging;
	
	opens application to javafx.graphics, javafx.fxml;
}
