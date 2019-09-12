package application;

import java.sql.Connection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class InsertDataController {
	@FXML
	private Button insert;

	@FXML
	private Label counter;
	@FXML
	private Label records;
	public Connection connection;
	private Main mainApp;

	private Thread t1;
	public static boolean finished = false;

	public void setMainApp(Main mainApp) {
		this.mainApp = mainApp;
		this.connection = mainApp.conn;
	}

	@FXML
	private void handleInsert(ActionEvent t) throws InterruptedException {
		if (connection == null) {
			this.connection = mainApp.conn;
		}

		t1 = new Thread(() -> {
			insert_data();
		});
		t1.start();
		t1.join();
	}

	private void insert_data() {
		// insert data
	}
}
