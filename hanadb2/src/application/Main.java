package application;

import java.io.IOException;
import java.sql.Connection;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

public class Main extends Application {
	private Stage primaryStage;
	private BorderPane rootLayout;

	public Connection conn = null;

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("SAP HANA Database Monitoring 500");

		// connect();
		initRootLayout();
		showMainOverview();
	}

	public void initRootLayout() {
		try {
			// Load root layout from fxml file.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(Main.class.getResource("RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();

			/*
			 * Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
			 * double width = screenSize.getWidth() / 2; double height =
			 * screenSize.getHeight() - 100;
			 */

			Scene scene = new Scene(rootLayout);
			primaryStage.setScene(scene);

			RootLayoutController controller = loader.getController();
			controller.setMainApp(this);

			primaryStage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void showMainOverview() {
		try {
			// Load main overview.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(Main.class.getResource("MainOverview.fxml"));
			AnchorPane mainOverview = (AnchorPane) loader.load();

			rootLayout.setCenter(mainOverview);

			MainOverviewController controller = loader.getController();
			controller.setMainApp(this);
			this.conn = controller.connection;
			// this.timeline = controller.timeline;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Stage getPrimaryStage() {
		return primaryStage;
	}

	public static void main(String[] args) {
		launch(args);
	}
}
