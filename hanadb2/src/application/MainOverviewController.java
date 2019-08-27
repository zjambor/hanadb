package application;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import javafx.util.Callback;

@SuppressWarnings("rawtypes")
public class MainOverviewController {
	private Main mainApp;
	public Connection connection;
	protected ObservableList<ObservableList> data;
	public boolean finished;
	private Thread t1;

	@FXML
	private Button exit;
	@FXML
	private Button start;
	@FXML
	public TextArea ctrlmsg;
	@FXML
	protected TableView tableView;

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

	protected String SqlText() {
		String SQL = "select b.tablespace_name \"Tablespace\", round(a.bytes_alloc / 1024 / 1024, 0) as \"Allocated (Mb)\",\r\n"
				+ "round(nvl(b.bytes_free, 0) / 1024 / 1024, 0) \"Free (Mb)\", round((a.bytes_alloc - nvl(b.bytes_free, 0)) / 1024 / 1024, 0) \"Used (Mb)\",\r\n"
				+ "round((nvl(b.bytes_free, 0) / a.bytes_alloc) * 100,2) \"% Free\",\r\n"
				+ "100 - round((nvl(b.bytes_free, 0) / a.bytes_alloc) * 100,2) \"% Used\",\r\n"
				+ "round(maxbytes/1048576,0) \"Max (Mb)\" from (select f.tablespace_name, sum(f.bytes) bytes_alloc,\r\n"
				+ "sum(decode(f.autoextensible, 'YES',f.maxbytes,'NO', f.bytes)) maxbytes\r\n"
				+ "from dba_data_files f group by tablespace_name\r\n"
				+ ") a, (select f.tablespace_name,sum(f.bytes) bytes_free from dba_free_space f\r\n"
				+ "group by tablespace_name) b where a.tablespace_name = b.tablespace_name (+)\r\n"
				+ "union SELECT TABLESPACE_NAME \"Tablespace\", FILESIZE as \"Allocated (Mb)\",\r\n"
				+ "FILESIZE-USED as \"Free (Mb)\", USED \"Used (Mb)\",trunc(NVL(FILESIZE-USED,0.0)/FILESIZE * 1000) / 10 AS \"% Free\",\r\n"
				+ "trunc(NVL(USED,0.0)/FILESIZE * 1000) / 10 AS \"% Used\",TOTAL \"Max (Mb)\"\r\n"
				+ "FROM (SELECT TABLESPACE_NAME,SUM(FILESIZE) FILESIZE,SUM(TOTAL) TOTAL,SUM(used) USED FROM \r\n"
				+ "(SELECT TABLESPACE_NAME, 0 FILESIZE,round(sum(TOTAL1),0) TOTAL,0 used FROM\r\n"
				+ "(select a.TABLESPACE_NAME,A.AUTOEXTENSIBLE,round(a.bytes/1024/1024,2) FILESIZE,\r\n"
				+ "decode (round(a.MAXBYTES/1024/1024,2), 0,round(a.bytes/1024/1024,2), round(a.MAXBYTES/1024/1024,2)) TOTAL1\r\n"
				+ "FROM dba_temp_files A) GROUP BY TABLESPACE_NAME,0\r\n"
				+ "union select TABLESPACE_NAME,round(TABLESPACE_SIZE/1024/1024,0) FILESIZE,0 TOTAL,round((TABLESPACE_SIZE-FREE_SPACE)/1024/1024,0) used\r\n"
				+ "from dba_temp_free_space) GROUP BY TABLESPACE_NAME)";

		return SQL;
	}

	@SuppressWarnings({ "unchecked" })
	protected void buildData() {
		data = FXCollections.observableArrayList();
		if (connection == null) {
			this.connection = mainApp.conn;
		}
		try {
			String SQL = SqlText();

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
				Platform.runLater(() -> tableView.getColumns().addAll(col));
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
				tableView.setItems(data);
				tableView.setColumnResizePolicy((param) -> true);
				customResize(tableView);
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
		finished = true;
		t1 = new Thread(() -> {
			while (!finished) {
				try {
					Thread.sleep(5000);
					buildData();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		t1.start();
	}

	@FXML
	private void handleDisconnect() throws SQLException {
		finished = false;
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
	private void handleDisconnectAndExit() throws SQLException {
		finished = false;
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
			mainApp.conn.close();
			mainApp.conn = null;
		}
		System.exit(0);
	}
}
