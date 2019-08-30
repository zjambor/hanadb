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
	protected ObservableList<ObservableList> data_m;
	protected ObservableList<ObservableList> data_disk;
	protected ObservableList<ObservableList> data_srv;
	protected ObservableList<ObservableList> data_comp;
	private Thread t1;
	private Thread t2;

	@FXML
	private Button exit;
	@FXML
	private Button start;
	@FXML
	private Button disconnect;
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

	// public final ObservableList<String> dataList =
	// FXCollections.observableArrayList();
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
		String SQL = "SELECT TO_VARCHAR(round(SUM(COL),2,ROUND_HALF_UP),'9999.99') AS \"Column Tables MB\", TO_VARCHAR(round(SUM(ROWSS),2,ROUND_HALF_UP),'9999.99') AS \"Row Tables MB\"\r\n"
				+ "FROM (SELECT round (sum(MEMORY_SIZE_IN_TOTAL)/1024/1024,2) AS COL,\r\n"
				+ "0 AS ROWSS FROM M_CS_TABLES union SELECT 0 AS COL,\r\n"
				+ "round (sum(USED_FIXED_PART_SIZE + USED_VARIABLE_PART_SIZE)/1024/1024,2) AS ROWSS FROM M_RS_TABLES);";

		return SQL;
	}

	protected String SqlText_serv() {
		String SQL = "select HOST, SERVICE_NAME, TO_VARCHAR(round(TOTAL_MEMORY_USED_SIZE/(1024*1024), 2),'9999.99') as \"Used Memory MB\"\r\n"
				+ "from M_SERVICE_MEMORY;";
		return SQL;
	}

	protected String SqlText_disk() {
		String SQL = "SELECT A.DATABASE_NAME, A.HOST, A.SERVICE_NAME, B.DATA AS DATA_GB, C.LOG AS LOG_GB FROM \"SYS\".\"M_VOLUMES_\" AS A INNER JOIN (SELECT VOLUME_ID, DATABASE_NAME, TO_VARCHAR(ROUND(DATA_SIZE/1024/1024/1024, 2),'9999.99') AS DATA FROM \"SYS\".\"M_VOLUME_SIZES_\" WHERE LOG_SIZE =-1) AS B ON A.VOLUME_ID = B.VOLUME_ID AND A.DATABASE_NAME = B.DATABASE_NAME INNER JOIN\r\n"
				+ "(SELECT VOLUME_ID, DATABASE_NAME, TO_VARCHAR(ROUND(LOG_SIZE/1024/1024/1024, 2),'9999.99') AS LOG FROM \"SYS\".\"M_VOLUME_SIZES_\" WHERE DATA_SIZE =-1) AS C ON B.VOLUME_ID = C.VOLUME_ID AND B.DATABASE_NAME = C.DATABASE_NAME;";

		return SQL;
	}

	protected String SqlText_comp() {
		String SQL = "select * from (select \"Host\",\"Database\",\"Component\",\"Used Memory Size MB\" from (select \"Host\",\r\n"
				+ "\"Database\", \"Component\", TO_VARCHAR(round(sum(\"Used Memory Exclusive\")/(1024*1024),2),'9999.99') as \"Used Memory Size MB\"\r\n"
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

	@SuppressWarnings("unchecked")
	private void buildHeader(TableView tv, String sqltext) throws SQLException {
		ResultSet rs = connection.prepareStatement(sqltext).executeQuery();
		for (int i = 0; i < rs.getMetaData().getColumnCount(); i++) {
			final int j = i;

			TableColumn col = new TableColumn(rs.getMetaData().getColumnName(i + 1));
			col.setCellValueFactory(new Callback<CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
				public ObservableValue<String> call(CellDataFeatures<ObservableList, String> param) {
					Object val = param.getValue().get(j);
					if (val != null)
						return new SimpleStringProperty(val.toString());
					else
						return new SimpleStringProperty("");
				}
			});
			Platform.runLater(() -> {
				tv.getColumns().addAll(col);
			});
		}
	}

	protected void buildData(TableView tv, String sqltext, ObservableList<ObservableList> data, int k) {

		if (connection == null) {
			this.connection = mainApp.conn;
		}
		try {
			ResultSet rs = connection.prepareStatement(sqltext).executeQuery();

			data = null;
			data = FXCollections.observableArrayList();

			while (rs.next()) {
				ObservableList<String> row = FXCollections.observableArrayList();
				for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
					row.add(rs.getString(i));
				}

				data.addAll(row);
			}
			switch (k) {
			case 0:
				data_m = data;
				break;
			case 1:
				data_srv = data;
				break;
			case 2:
				data_disk = data;
				break;
			case 3:
				data_comp = data;
				break;
			default:
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error on Building Data");
		}
	}

	@SuppressWarnings("unchecked")
	private void refreshViews() {
		Platform.runLater(() -> {
			tableView.setItems(data_m);
			tableView.setColumnResizePolicy((param) -> true);
			customResize(tableView);
		});

		Platform.runLater(() -> {
			tableView_services.setItems(data_srv);
			tableView_services.setColumnResizePolicy((param) -> true);
			customResize(tableView_services);
		});

		Platform.runLater(() -> {
			tableView_disk.setItems(data_disk);
			tableView_disk.setColumnResizePolicy((param) -> true);
			customResize(tableView_disk);
		});

		Platform.runLater(() -> {
			tableView_components.setItems(data_comp);
			tableView_components.setColumnResizePolicy((param) -> true);
			customResize(tableView_components);
		});

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

		buildHeader(tableView, SqlText_mem());
		buildHeader(tableView_services, SqlText_serv());
		buildHeader(tableView_disk, SqlText_disk());
		buildHeader(tableView_components, SqlText_comp());

		buildData(tableView, SqlText_mem(), data_m, 0);
		buildData(tableView_services, SqlText_serv(), data_srv, 1);
		buildData(tableView_disk, SqlText_disk(), data_disk, 2);
		buildData(tableView_components, SqlText_comp(), data_comp, 3);
//		refreshViews();

		finished = false;
		t1 = new Thread(() -> {
			while (!finished) {
				try {
					message = "Querying data...";
					Platform.runLater(() -> {
						ctrlmsg.appendText(message + "\n");
					});
					Thread.sleep(10000);
					buildData(tableView, SqlText_mem(), data_m, 0);
					buildData(tableView_services, SqlText_serv(), data_srv, 1);
					buildData(tableView_disk, SqlText_disk(), data_disk, 2);
					buildData(tableView_components, SqlText_comp(), data_comp, 3);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		t1.start();

		t2 = new Thread(() -> {
			while (!finished) {
				refreshViews();
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		t2.start();
	}

	@FXML
	private void handleDisconnect() throws SQLException, InterruptedException {
		message = "Exiting...";
		ctrlmsg.appendText(message + "\n");
		finished = true;
		Thread.sleep(500);
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (connection != null) {
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
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (connection != null) {
			connection.close();
			connection = null;
		}
		System.exit(0);
	}
}
