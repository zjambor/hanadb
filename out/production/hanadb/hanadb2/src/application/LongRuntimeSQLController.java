package application;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

import application.Main;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;

@SuppressWarnings("rawtypes")
public class LongRuntimeSQLController {
	private ObservableList<ObservableList> data;
	@FXML
	private TableView tableview;
	@FXML
	private TextArea sqlText;
	@FXML
	private Button sqlPlanButton;
	@FXML
	private Button sqlTextButton;
	@FXML
	private Button refreshButton;

	public Connection connection;
	private Main mainApp;

	public static void main(String[] args) {
		// launch(args);
	}

	/**
	 * The constructor. The constructor is called before the initialize() method.
	 */
	public LongRuntimeSQLController() {

	}

	/**
	 * Initializes the controller class. This method is automatically called after
	 * the fxml file has been loaded.
	 */
	@FXML
	private void initialize() {

	}

	public void setMainApp(Main mainApp) {
		this.mainApp = mainApp;
		this.connection = mainApp.conn;
	}

	@SuppressWarnings({ "unchecked" })
	public void buildData() {
		data = FXCollections.observableArrayList();
		if (connection == null) {
			this.connection = mainApp.conn;
		}
		try {
			String SQL = "SELECT nvl(ses.username,'ORACLE PROC')||' ('||ses.sid||')' USERNAME,\r\n"
					+ "       SERIAL#,MACHINE,\r\n"
					+ "       ltrim(to_char(floor(SES.LAST_CALL_ET/3600), '09')) || ':'\r\n"
					+ "       || ltrim(to_char(floor(mod(SES.LAST_CALL_ET, 3600)/60), '09')) || ':'\r\n"
					+ "       || ltrim(to_char(mod(SES.LAST_CALL_ET, 60), '09')) RUNT,\r\n"
					+ "       SES.STATUS,to_char(SQL.LAST_ACTIVE_TIME,'YYYY.MM.DD HH24:MI') LAST_ACTIVE_TIME,\r\n"
					+ "       SQL.INST_ID,SQL.SQL_ID,SQL.child_number,\r\n"
					+ "       SQL.PARSE_CALLS,SQL.EXECUTIONS,SQL.OPTIMIZER_COST,\r\n"
					+ "       SQL.BUFFER_GETS,SQL.PHYSICAL_READ_BYTES/1024 PHYSICAL_READ_KB,\r\n"
					+ "       SQL.PHYSICAL_WRITE_BYTES/1024 PHYSICAL_WRITE_KB,SQL.ELAPSED_TIME,\r\n"
					+ "       SQL.SQL_FULLTEXT\r\n" + "FROM GV$SESSION SES,   \r\n" + "       GV$SQL SQL \r\n"
					+ "where SES.USERNAME is not null\r\n" + "   and SES.SQL_ADDRESS    = SQL.ADDRESS \r\n"
					+ "   and SES.SQL_HASH_VALUE = SQL.HASH_VALUE \r\n"
					+ "   and Ses.AUDSID <> userenv('SESSIONID') \r\n" + "order by runt desc";

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
								// return new SimpleStringProperty(param.getValue().get(j).toString());
							}
						});
				if (i == rs.getMetaData().getColumnCount() - 1)
					col.setVisible(false);

				tableview.getColumns().addAll(col);
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
			tableview.setItems(data);

			tableview.setColumnResizePolicy((param) -> true);
			Platform.runLater(() -> customResize(tableview));

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error on Building Data");
		}
	}

	public void customResize(TableView<?> view) {

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

	@SuppressWarnings("unchecked")
	@FXML
	private void showSqlText() {
		if (!tableview.getSelectionModel().getSelectedItems().isEmpty()) {
			ObservableList<String> selectedItems = (ObservableList<String>) tableview.getSelectionModel()
					.getSelectedItems().get(0);

			String columnData = selectedItems.get(16);
			sqlText.setText(columnData);
		}
	}

	@SuppressWarnings("unchecked")
	@FXML
	private void showSqlPlan() throws SQLException {
		if (!tableview.getSelectionModel().getSelectedItems().isEmpty()) {
			ObservableList<String> selectedItems = (ObservableList<String>) tableview.getSelectionModel()
					.getSelectedItems().get(0);
			// String first_Column = selectedItems.toString().split(",")[16].substring(1);

			var planText = "";

			var sql_id = selectedItems.get(7);
			var query = "select plan_table_output from table(dbms_xplan.display_cursor('" + sql_id + "'))";

			var r = connection.prepareStatement(query).executeQuery();

			while (r.next()) {
				planText += r.getString("plan_table_output");
				planText += "\n";
			}

			sqlText.setFont(Font.font("Courier New", FontWeight.NORMAL, 12));
			sqlText.setText(planText);
		}
	}

	@FXML
	private void refresh() {
		buildData();
	}
}
