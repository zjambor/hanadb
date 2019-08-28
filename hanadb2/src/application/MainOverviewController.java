package application;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import javafx.util.Callback;

@SuppressWarnings("rawtypes")
public class MainOverviewController {
	private Main mainApp;
	public Connection connection;
	protected ObservableList<ObservableList> data;
	private Thread t1;

	@FXML
	private Button exit;
	@FXML
	private Button start;
	@FXML
	public TextArea ctrlmsg;
	@FXML
	protected TableView tableView;
	@FXML
	protected TableView tableView_services;
	@FXML
	protected TableView tableView_disk;
	@FXML
	protected TableView tableView_components;
	@FXML
	private ProgressBar pb_memory;
	@FXML
	private ProgressBar pb_disk;

	@FXML
	private ProgressBar usedTemppb;
	@FXML
	private ProgressBar usedTempInd;
	@FXML
	private Label redo_entriesLabel;
	@FXML
	private Label redo_writesLabel;
	@FXML
	private Label redo_blocks_writtenLabel;

	public final ObservableList<String> dataList = FXCollections.observableArrayList();
	public static volatile String message = "";
	public static boolean finished = false;

	private double[] redo_entriesPerSec = new double[2];
	private double[] redo_writesPerSec = new double[2];
	private double[] redo_blocks_writtenPerSec = new double[2];
	private Double usedTempPercent;
	private final Integer duration = 5;

	public void setMainApp(Main mainApp) {
		this.mainApp = mainApp;
		mainApp.conn = this.connection;
	}

	public Connection connect() throws ClassNotFoundException {
		if (connection == null) {
			String connectionString = "jdbc:sap://jamborz-hxe.westeurope.cloudapp.azure.com:39013";
			String user = "SYSTEM";
			String password = "szaolikK9595";
			try {
				connection = DriverManager.getConnection(connectionString, user, password);
			} catch (SQLException e) {
				System.err.println("Connection Failed. User/Passwd Error? Message: " + e.getMessage());
			}
		}
		return connection;
	}

	protected String SqlText_mem() {
		String SQL = "SELECT SUM(COL) AS \"Column Tables MB\", SUM(ROWSS) AS \"Row Tables MB\"\r\n"
				+ "FROM (SELECT round (sum(MEMORY_SIZE_IN_TOTAL)/1024/1024,2) AS COL,\r\n"
				+ "0 AS ROWSS FROM M_CS_TABLES union SELECT 0 AS COL,\r\n"
				+ "round (sum(USED_FIXED_PART_SIZE + USED_VARIABLE_PART_SIZE)/1024/1024,2) AS ROWSS FROM M_RS_TABLES);";

		return SQL;
	}

	protected String SqlText_serv() {
		String SQL = "select HOST, SERVICE_NAME, round(TOTAL_MEMORY_USED_SIZE/(1024*1024), 2) as \"Used Memory MB\"\r\n"
				+ "from M_SERVICE_MEMORY;";
		return SQL;
	}

	protected String SqlText_disk() {
		String SQL = "SELECT A.DATABASE_NAME, A.HOST, A.SERVICE_NAME, B.DATA AS DATA_GB, C.LOG AS LOG_GB FROM \"SYS\".\"M_VOLUMES_\" AS A INNER JOIN (SELECT VOLUME_ID, DATABASE_NAME, ROUND(DATA_SIZE/1024/1024/1024, 3) AS DATA FROM \"SYS\".\"M_VOLUME_SIZES_\" WHERE LOG_SIZE =-1) AS B ON A.VOLUME_ID = B.VOLUME_ID AND A.DATABASE_NAME = B.DATABASE_NAME INNER JOIN\r\n"
				+ "(SELECT VOLUME_ID, DATABASE_NAME, ROUND(LOG_SIZE/1024/1024/1024, 3) AS LOG FROM \"SYS\".\"M_VOLUME_SIZES_\" WHERE DATA_SIZE =-1) AS C ON B.VOLUME_ID = C.VOLUME_ID AND B.DATABASE_NAME = C.DATABASE_NAME;";

		return SQL;
	}

	protected String SqlText_comp() {
		String SQL = "select * from (select \"Host\",\"Database\",\"Component\",\"Used Memory Size MB\" from (select \"Host\",\r\n"
				+ "\"Database\", \"Component\", round (sum(\"Used Memory Exclusive\")/(1024*1024),2) as \"Used Memory Size MB\"\r\n"
				+ "from ( select t1.host \"Host\", t1.database_name \"Database\", component \"Component\", exclusive_size_in_use\r\n"
				+ "\"Used Memory Exclusive\" from sys_databases.m_heap_memory t1 join sys_databases.m_service_memory t2 on\r\n"
				+ "t1.host=t2.host and t1.port=t2.port and category != '/' and t1.component != 'Row Store Tables' where\r\n"
				+ "t1.database_name != '' and t1.database_name != 'SYSTEMDB' union ( select t1.host \"Host\",\r\n"
				+ "t1.database_name \"Database\", 'Row Store Tables' as \"Component\", allocated_size \"Used Memory Exclusive\"\r\n"
				+ "from sys_databases.m_rs_memory t1 join sys_databases.m_service_memory t2 on t1.host = t2.host and\r\n"
				+ "t1.port = t2.port and t1.database_name = t2.database_name where t1.database_name != '' and\r\n"
				+ "t1.database_name != 'SYSTEMDB' )) group by \"Host\", \"Database\",\"Component\")) order by \"Host\",\r\n"
				+ "\"Database\",\"Used Memory Size MB\" desc;";

		return SQL;
	}

	private void setprogress() throws SQLException {
		String query = "select sum(redo_entries) as redo_entries,\r\n" + "sum(redo_writes) as redo_writes,\r\n"
				+ "sum(redo_blocks_written) as redo_blocks_written\r\n" + "from (\r\n"
				+ "select value as redo_entries,\r\n" + "0 as redo_writes,\r\n" + "0 as redo_blocks_written\r\n"
				+ "from v$sysstat where name = 'redo entries'\r\n" + "union\r\n" + "select 0 as redo_entries,\r\n"
				+ "value as redo_writes,\r\n" + "0 as redo_blocks_written\r\n"
				+ "from v$sysstat where name = 'redo writes'\r\n" + "union\r\n" + "select 0 as redo_entries,\r\n"
				+ "0 as redo_writes,\r\n" + "value as redo_blocks_written\r\n"
				+ "from v$sysstat where name = 'redo blocks written')";

		Statement stmt = connection.prepareStatement(query);
		ResultSet r = stmt.executeQuery(query);

		while (r.next()) {
			redo_entriesPerSec[0] = r.getDouble("redo_entries");
			redo_writesPerSec[0] = r.getDouble("redo_writes");
			redo_blocks_writtenPerSec[0] = r.getDouble("redo_blocks_written");
		}
		stmt.close();

		double redo_entriesDelta = redo_entriesPerSec[0] - redo_entriesPerSec[1];
		double redo_writesDelta = redo_writesPerSec[0] - redo_writesPerSec[1];
		double redo_blocks_writtenDelta = redo_blocks_writtenPerSec[0] - redo_blocks_writtenPerSec[1];

		redo_entriesLabel.setText(String.valueOf(redo_entriesDelta / duration) + "/s");
		redo_writesLabel.setText(String.valueOf(redo_writesDelta / duration) + "/s");
		redo_blocks_writtenLabel.setText(String.valueOf(redo_blocks_writtenDelta / duration) + "/s");

		usedTemppb.setProgress(usedTempPercent / 100);
		usedTempInd.setProgress(usedTempPercent / 100);
	}

	@SuppressWarnings({ "unchecked" })
	protected void buildData(TableView tv, String sqltext) {
		data = FXCollections.observableArrayList();
		if (connection == null) {
			this.connection = mainApp.conn;
		}
		try {
			String SQL = sqltext;

			ResultSet rs = connection.prepareStatement(SQL).executeQuery();

			for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
				// We are using non property style for making dynamic table
				final int j = i;

				TableColumn col = new TableColumn(rs.getMetaData().getColumnName(i + 1));
				col.setCellValueFactory(
						new Callback<CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
							public ObservableValue<String> call(CellDataFeatures<ObservableList, String> param) {
								Object val = param.getValue().get(j);
								if (val != null)
									return new SimpleStringProperty(val.toString());
								else
									return new SimpleStringProperty("");
							}
						});
				Platform.runLater(() -> tv.getColumns().addAll(col));
			}

			while (rs.next()) {
				// Iterate Row
				ObservableList<String> row = FXCollections.observableArrayList();
				for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
					// Iterate Column
					row.add(rs.getString(i));
				}

				data.addAll(row);
			}

			// FINALLY ADDED TO TableView
			Platform.runLater(() -> {
				tv.setItems(data);
				tv.setColumnResizePolicy((param) -> true);
				customResize(tv);
			});

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error on Building Data");
		}
	}

	protected void customResize(TableView<?> view) {
		AtomicLong width = new AtomicLong();
		view.getColumns().forEach(col -> {
			width.addAndGet((long) col.getWidth());
		});
		double tableWidth = view.getWidth();

		if (tableWidth > width.get()) {
			view.getColumns().forEach(col -> {
				col.setPrefWidth(col.getWidth() + ((tableWidth - width.get()) / view.getColumns().size()));
			});
		}
	}

	@FXML
	private void handleConnect(ActionEvent t) throws SQLException, InterruptedException, IOException {
		message = "Connecting...";
		ctrlmsg.appendText(message + "\n");

		t1 = new Thread(() -> {
			try {
				connect();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		});
		t1.start();
		t1.join();

		message = "Connected to SAP HANA database.";
		ctrlmsg.appendText(message + "\n");
		Thread.sleep(500);

		finished = false;
		t1 = new Thread(() -> {
			while (!finished) {
				try {
					message = "Querying data...";
					Platform.runLater(() -> {
						ctrlmsg.appendText(message + "\n");
					});

					buildData(tableView, SqlText_mem());
					buildData(tableView_services, SqlText_serv());
					buildData(tableView_disk, SqlText_disk());
					buildData(tableView_components, SqlText_comp());
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		t1.start();
	}

	@FXML
	private void handleDisconnect() throws SQLException, InterruptedException {
		finished = true;
		message = "Exiting...";
		ctrlmsg.appendText(message + "\n");
		Thread.sleep(500);
		try {
			t1.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (connection != null) {
			// timeline.stop();
			// timeline = null;
			// ((ValidConnection) connection).setInvalid();
			connection.close();
			connection = null;
		}
	}

	@FXML
	private void handleDisconnectAndExit() throws SQLException, InterruptedException {
		message = "Exiting...";
		ctrlmsg.appendText(message + "\n");
		finished = true;
		Thread.sleep(500);
		try {
			t1.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (connection != null) {
			// timeline.stop();
			// timeline = null;
			// ((ValidConnection) connection).setInvalid();
			connection.close();
			connection = null;
			// mainApp.conn.close();
			// mainApp.conn = null;
		}
		System.exit(0);
	}
}
