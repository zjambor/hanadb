package application;

import application.Main;

import java.awt.Dimension;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class RootLayoutController {
	private Main mainApp;

	Connection connection;
	// Timeline timeline;

	public void setMainApp(Main mainApp) {
		this.mainApp = mainApp;
		this.connection = mainApp.conn;
		// this.timeline = mainApp.timeline;
	}

	@FXML
	private void handleAbout() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("HANADBMon");
		alert.setHeaderText("About");
		alert.setContentText("SAP HANA DATABASE MONITORING APPLICATION\nAuthor: Jámbor Zoltán Péter");

		alert.showAndWait();
	}

	/**
	 * Closes the application.
	 *
	 * @throws SQLException
	 */
	@FXML
	private void handleExit() throws SQLException {
		// this.connection = mainApp.conn;
		if (connection != null) {
			// timeline.stop();
			// timeline = null;
			// ((ValidConnection) connection).setInvalid();
			connection.close();
			connection = null;
		}
		System.exit(0);
	}

	@FXML
	private void handleTopSQL() {
		try {
			this.connection = mainApp.conn;
			if (connection != null) {
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(Main.class.getResource("view/TopSQLOverview.fxml"));
				AnchorPane topsqlOverview = (AnchorPane) loader.load();

				Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
				double width = screenSize.getWidth() - 50;
				double height = screenSize.getHeight() - 100;

				Scene scene = new Scene(topsqlOverview, width, height);

				// TopSQLOverviewController controller = loader.getController();
				//
				// Stage stage = new Stage();
				// stage.setScene(scene);
				// controller.setMainApp(mainApp);
				// controller.buildData();

				// stage.setTitle("TOP SQL");
				// stage.show();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void handleLongRuntimeSQL() {
		try {
			this.connection = mainApp.conn;
			if (connection != null) {
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(Main.class.getResource("view/LongRuntimeSQL.fxml"));
				AnchorPane longruntimesqlOverview = (AnchorPane) loader.load();

				Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
				double width = screenSize.getWidth() - 50;
				double height = screenSize.getHeight() - 100;

				Scene scene = new Scene(longruntimesqlOverview, width, height);

				LongRuntimeSQLController controller = loader.getController();

				Stage stage = new Stage();
				stage.setScene(scene);
				controller.setMainApp(mainApp);
				controller.buildData();

				stage.setTitle("LONG RUNTIME SQL");
				stage.show();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void newTablespacesWindow() {
		try {
			connection = mainApp.conn;
			if (connection != null) {
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(Main.class.getResource("view/Tablespaces.fxml"));
				AnchorPane tablespaces = (AnchorPane) loader.load();
				Scene scene = new Scene(tablespaces);

				// TablespacesController controller = loader.getController();
				//
				// Stage stage = new Stage();
				// stage.setScene(scene);
				// controller.setMainApp(mainApp);
				// controller.buildData();
				//
				// stage.setTitle("TABLESPACES");
				// stage.show();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void newDatafilesWindow() {
		try {
			connection = mainApp.conn;
			if (connection != null) {
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(Main.class.getResource("view/DataFiles.fxml"));
				AnchorPane dataFiles = (AnchorPane) loader.load();
				Scene scene = new Scene(dataFiles);

				// DataFilesController controller = loader.getController();
				//
				// Stage stage = new Stage();
				// stage.setScene(scene);
				// controller.setMainApp(mainApp);
				// controller.buildData();
				//
				// stage.setTitle("DATA FILES");
				// stage.show();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void newAuditedEventsWindow() {
		try {
			connection = mainApp.conn;
			if (connection != null) {
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(Main.class.getResource("view/AuditedEvents.fxml"));
				AnchorPane auditedEvents = (AnchorPane) loader.load();
				Scene scene = new Scene(auditedEvents);

				// AuditedEventsController controller = loader.getController();
				//
				// Stage stage = new Stage();
				// stage.setScene(scene);
				// controller.setMainApp(mainApp);
				// controller.buildData();
				//
				// stage.setTitle("Audited Events");
				// stage.show();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void newUsersWindow() {
		try {
			connection = mainApp.conn;
			if (connection != null) {
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(Main.class.getResource("view/Users.fxml"));
				AnchorPane users = (AnchorPane) loader.load();
				Scene scene = new Scene(users);

				// UsersController controller = loader.getController();
				//
				// Stage stage = new Stage();
				// stage.setScene(scene);
				// controller.setMainApp(mainApp);
				// controller.buildData();
				//
				// stage.setTitle("USERS");
				// stage.show();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void newSessionsWindow() {
		try {
			connection = mainApp.conn;
			if (connection != null) {
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(Main.class.getResource("view/TopSessions.fxml"));
				AnchorPane sessions = (AnchorPane) loader.load();

				Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
				double width = screenSize.getWidth() - 50;
				double height = screenSize.getHeight() - 100;

				Scene scene = new Scene(sessions, width, height);

				// TopSessionsController controller = loader.getController();
				//
				// Stage stage = new Stage();
				// stage.setScene(scene);
				// controller.setMainApp(mainApp);
				// controller.buildData();
				//
				// stage.setTitle("TOP SESSIONS");
				// stage.show();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
