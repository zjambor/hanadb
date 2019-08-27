package application;

import java.sql.Connection;
import java.sql.SQLException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

public class MainOverviewController {
	private Main mainApp;
	public Connection connection;

	@FXML
	private Button exit;
	@FXML
	private Button start;
	@FXML
	public TextArea ctrlmsg;
	@FXML
	private final TableView<String> tableView = new TableView<>();

	public final ObservableList<String> dataList = FXCollections.observableArrayList();

	public void setMainApp(Main mainApp) {
		this.mainApp = mainApp;
		mainApp.conn = this.connection;
	}

	public Connection connect() {
		try {
			if (connection == null) {
				Class.forName("oracle.jdbc.driver.OracleDriver");

				/*
				 * PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
				 * pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
				 * 
				 * if (c_dbNames.getValue() == "CENAICST1") {
				 * pds.setURL("jdbc:oracle:thin:system/szaolikK9595@cenaicst1:1521:TESTDB3"); }
				 * else if (c_dbNames.getValue() == "LKSZDBT") {
				 * pds.setURL("jdbc:oracle:thin:@//cenlksz2dbt-scan:1521/LKSZDB");
				 * pds.setUser("monitoring"); pds.setPassword("UgO6raspO7"); } else if
				 * (c_dbNames.getValue() == "LKSZDB") {
				 * pds.setURL("jdbc:oracle:thin:@//cenlksz2db-scan:1521/LKSZDB");
				 * pds.setUser("monitoring"); pds.setPassword("UgO6raspO7"); } // else if
				 * (c_dbNames.getValue() == "LKSZDBDEV") { //
				 * pds.setURL("jdbc:oracle:thin:@CENLKSZDBD1:1521:LKSZDBDEV"); //
				 * pds.setUser("monitoring"); // pds.setPassword("UgO6raspO7"); // }
				 * 
				 * connection = pds.getConnection();
				 */
			}

			return connection;
		} catch (ClassNotFoundException e) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setHeaderText("Connection error");
			// alert.setContentText(c_dbNames.getValue()); // e.getMessage());
			alert.showAndWait();
			return connection;
		}
	}

	@FXML
	private void handleConnect(ActionEvent t) throws SQLException, InterruptedException {

	}

	@FXML
	private void handleDisconnect() throws SQLException {
		if (connection != null) {
			// timeline.stop();
			// timeline = null;
			// ((ValidConnection) connection).setInvalid();
			connection.close();
			connection = null;
		}
	}

	@FXML
	private void handleDisconnectAndExit() throws SQLException {
		if (connection != null) {
			// timeline.stop();
			// timeline = null;
			// ((ValidConnection) connection).setInvalid();
			connection.close();
			connection = null;
			mainApp.conn.close();
			mainApp.conn = null;
		}
		System.exit(0);
	}
}
