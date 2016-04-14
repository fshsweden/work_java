package com.fsh;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.security.auth.login.Configuration;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.ib.client.Contract;
import com.ib.client.EDecoder;
import com.ib.client.Types.SecType;

import mytrade.common_if.GenericBar;
import mytrade.common_if.GenericSecType;
import mytrade.common_if.HistoryPriceHandler;
import mytrade.common_if.InformationEventHandler;
import mytrade.common_if.InformationType;
import mytrade.common_if.KeyValues;
import mytrade.common_if.MarketAPI;
import mytrade.common_if.MyInstrument;
import mytrade.common_if.RetCodes;
import mytrade.common_if.SymbolEventHandler;
import mytrade.common_if.TwsConfig;
import mytrade.common_if.TwsConfig.Cfg;
import mytrade.ib.IBMarketAPI;
import mytrade.ib.MyIBInstrument;

/*
 * 
 * SKRIV EN APP SOM SPARAR DATA I SÅ HÖG DETALJ SOM MÖJLIGT!
 * 
 * DAGSDATA
 * TIMDATA
 * MINUTDATA
 * etc...
 * 
 * Lägg gärna till en kontroll på felaktigt data, evt flera källor osv osv!
 * 
 * Lägg i nån "enkel" databas, inte MYSQL!
 * 
 * MySQLi?
 * SQLite?
 * 
 * 
 * 
 */
public class ImportIBHistoryData {

	private MarketAPI marketAPI;

	Boolean hasArg = true;
	Boolean noArg = false;
	Boolean req = true;
	Boolean notreq = false;

	private String symbol;
	private SecType secType = SecType.STK;
	private String twsHost = "";
	private String twsPort = "";
	private String twsClientId = "";
	private String twsAccount = ""; // default
	private String currency = "";
	private String exchange = "";
	private String expiry = "";
	private String duration = "";

	private String env = "";
	private Configuration alpha_config;

	private LinkedBlockingQueue<MyInstrument> q = new LinkedBlockingQueue<MyInstrument>();

	private int duration_i = 2;
	private String duration_unit_s = "Y";
	private String barsize_s = "1D";

	/*
	 * 
	 * 
	 * 
	 */
	public ImportIBHistoryData(String args[]) {

		Options options = new Options();
		options.addOption("help", false, "Show help");

		options.addOption("host", true, "Hostname or IP");
		options.addOption("port", true, "Port number");
		options.addOption("account", true, "Account name");
		options.addOption("client", true, "Client # (1-100 unique)");

		options.addOption("sectype", true, "Security type");
		options.addOption("symbol", true, "Symbol or Underlying (for FUT)");
		options.addOption("expiry", true, "Expiry, only used for sectype FUT");
		options.addOption("exchange", true, "Exchange abbrev");
		options.addOption("currency", true, "Currency");
		options.addOption("duration", true, "Duration (Depends on duration unit, not all combinations are allowed)");
		options.addOption("durationunit", true, "Duration Unit (Y,M,W,D,H,M,S)");
		options.addOption("barsize", true, "Barsize (See IB docs. Can be similar to 1D, 4H, 30S, 1W)");

		Connection c = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:ohlc.db");
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		System.out.println("Opened database successfully");

		final Connection conn = c;
		setupDatabase(conn);

		CommandLineParser parser = new BasicParser();
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("SaveHistoryData", options);
				System.exit(1);
			}

			if (cmd.hasOption("host")) {
				twsHost = cmd.getOptionValue("host");
			}

			if (cmd.hasOption("port")) {
				twsPort = cmd.getOptionValue("port");
			}

			if (cmd.hasOption("client")) {
				twsClientId = cmd.getOptionValue("client");
			}

			if (cmd.hasOption("account")) {
				twsAccount = cmd.getOptionValue("account");
			}

			if (cmd.hasOption("symbol")) {
				symbol = cmd.getOptionValue("symbol");
			}

			if (cmd.hasOption("sectype")) {
				secType = SecType.valueOf(cmd.getOptionValue("sectype"));
			}

			if (cmd.hasOption("expiry")) {
				expiry = cmd.getOptionValue("expiry");
			}

			if (cmd.hasOption("exchange")) {
				exchange = cmd.getOptionValue("exchange");
			}

			if (cmd.hasOption("currency")) {
				currency = cmd.getOptionValue("currency");
			}

			if (cmd.hasOption("duration")) {
				duration_i = Integer.parseInt(cmd.getOptionValue("duration"));
			}

			if (cmd.hasOption("durationunit")) {
				duration_unit_s = cmd.getOptionValue("durationunit");
			}

			if (cmd.hasOption("barsize")) {
				barsize_s = cmd.getOptionValue("barsize");
			}
			
			if (symbol == null || symbol.equals("")) {
				System.out.println("Must give a symbol!");
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("SaveHistoryData", options);
				System.exit(1);
			}

			/*
			 * 
			 * 
			 * 
			 */
			EDecoder.enableDebugging(true);

			marketAPI = new IBMarketAPI();
			TwsConfig kv = new TwsConfig();
			kv.put(Cfg.HOST.name(), twsHost);
			kv.put(Cfg.PORT.name(), twsPort);
			kv.put(Cfg.CLIENTID.name(), twsClientId);
			// kv.put(Cfg.Account.name(), twsaccount);
			marketAPI.configure(kv);

			RetCodes rc = marketAPI.connect(null, new InformationEventHandler() {
				@Override
				public void information(InformationType it, KeyValues info) {
					for (String s : info.keySet()) {
						System.out.println("[INFO]:" + it.name() + " "
								+ s + " " + info.get(s));
					}
				}
			});

			if (rc.get("STATUS").equals("OK")) {

				/*
				 * Consumer Thread
				 */

				Runnable r = new Runnable() {
					@Override
					public void run() {

						MyInstrument m;
						try {
							while ((m = q.take()) != null) {

								MyIBInstrument myib = (MyIBInstrument) m;
								if (myib.getContract().secType() == SecType.None) {
									// This signals "exit"
									System.exit(1);
								} else {
									System.out.println("Symbol " + m.getSymbol()
											+ " loaded OK, getting Price History ");
									AtomicBoolean flag = new AtomicBoolean(false);

									final MyInstrument instr = m;

									marketAPI.loadPriceHistory(instr, duration_i, duration_unit_s, barsize_s, new HistoryPriceHandler() {
										@Override
										public void bars(List<GenericBar> bars) {

											savePriceHistory(conn, instr, bars);

											System.out.println("Price History for "
													+ instr.getSymbol()
													+ " analyzed!");
											flag.set(true);
										}
									});

									while (!flag.get()) {
										Thread.sleep(1000);
									}
								}
							}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};

				new Thread(r).start();

				/*
				 * Producer
				 */
				GenericSecType st = GenericSecType.valueOf(secType.name());

				System.out.println("Loading Symbols for " + symbol + " "
						+ exchange + " " + currency + " " + expiry);
				marketAPI.loadSymbols(st, symbol, exchange, currency, expiry, new SymbolEventHandler() {
					@Override
					public void symbolLoaded(MyInstrument i) {
						q.add(i);
					}

					@Override
					public void noMoreSymbols() {
						System.out.println("No more symbols!");

						// soooooooo ugly....

						Contract c = new Contract();
						c.secType(SecType.None);
						MyInstrument end = new MyIBInstrument(c);
						q.add(end);
					}
				});
			}

		} catch (UnrecognizedOptionException ex) {
			System.out.println(ex.getLocalizedMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("SaveHistoryData", options);
			System.exit(5);
		} catch (ParseException e) {
			System.out.println("CLI Option Parsing error!");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("SaveHistoryData", options);
			System.exit(4);
		}

	}

	public void savePriceHistory(Connection conn, MyInstrument instr, List<GenericBar> bars) {
		for (GenericBar b : bars) {
			String symbol = instr.getSymbol();

			String date = DateTools.DateFromTimestamp(b.getTime() * 1000);
			System.out.println(symbol + " " + date + " " + b.getOpen() + " " + b.getHigh() + " " + b.getLow() + " " + b.getClose() + " " + b.getVolume());
			replaceMarketPrice(conn, symbol, "1D", date, b.getOpen(), b.getHigh(), b.getLow(), b.getClose(), b.getVolume());
		}
	}

	private Boolean in_pos = false;
	private Double entry_price = 0d;
	private String entry_date = "";
	private Boolean is_long = false;

	private void takeOrExitPos(String date, String str, Double price) {
		entry_date = date;
		if (str.equals("LONG")) {
			if (in_pos) {
				// exit SHORT pos
				Double profit = entry_price - price;
				System.out.println("Exit short pos with Result:" + profit);
				in_pos = false;
				is_long = true;
			} else {
				entry_price = price;
				in_pos = true;
				System.out.println("Taking long pos at " + entry_price);
				is_long = false;
			}
		} else { // SHORT
			if (in_pos) {
				// exit LONG pos
				Double profit = price - entry_price;
				System.out.println("Exit long pos at Result:" + profit);
				in_pos = false;
			} else {
				System.out.println("Taking short pos at " + entry_price);
				entry_price = price;
				in_pos = true;
			}
		}
	}

	private Boolean isCross(final Double a, final Double b) {
		return a * b < 0.0;
	}

	public static void main(String[] args) {
		new ImportIBHistoryData(args);
	}

	private Option createOption(String opt, boolean hasArg, String description, boolean required) throws IllegalArgumentException {
		Option option = new Option(opt, hasArg, description);
		option.setRequired(required);
		return option;
	}

	private void setupDatabase(Connection c) {

		// if (!checkDBExists(c, "IB")) {
		// createDatabase(c, "IB");
		// }

		// useDatabase(c, "IB");

		if (!tableExists(c, "OHLC")) {
			List<ColumnDef> prices = new ArrayList<ColumnDef>();
			prices.add(new StringColumn("SYMBOL", 40));
			prices.add(new StringColumn("PERIOD", 3));
			prices.add(new StringColumn("DT", 10));
			prices.add(new DoubleColumn("OPEN"));
			prices.add(new DoubleColumn("CLOSE"));
			prices.add(new DoubleColumn("HIGH"));
			prices.add(new DoubleColumn("LOW"));
			prices.add(new IntegerColumn("VOLUME"));

			// System.out.println("Definition: \n" + createTableStatement("OHLC", prices));

			String str = createTableStatement("OHLC", prices);
			try {
				PreparedStatement stmt = c.prepareStatement(str);
				stmt.execute();
			} catch (SQLException e) {
				System.out.println(str);
				e.printStackTrace();
			}
		}
	}

	public boolean checkDBExists(Connection conn, String dbName) {

		try {
			ResultSet resultSet = conn.getMetaData().getCatalogs();
			while (resultSet.next()) {
				String databaseName = resultSet.getString(1);
				if (databaseName.equals(dbName)) {
					return true;
				}
			}
			resultSet.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public Boolean createDatabase(Connection conn, String dbName) {
		try {
			PreparedStatement stmt = conn.prepareStatement(createDbStatement("IB"));
			ResultSet rs = stmt.executeQuery();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public Boolean useDatabase(Connection conn, String dbName) {
		try {
			PreparedStatement stmt = conn.prepareStatement("USE " + dbName);
			ResultSet rs = stmt.executeQuery();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public Boolean replaceMarketPrice(Connection conn, String symbol, String period, String date, Double open, Double high, Double low, Double close, Long volume) {
		try {
			PreparedStatement stmt = conn.prepareStatement("REPLACE INTO OHLC(symbol,period,dt,open,high,low,close,volume) values (?,?,?,?,?,?,?,?) ");
			stmt.setString(1, symbol);
			stmt.setString(2, period);
			stmt.setString(3, date);
			stmt.setDouble(4, open);
			stmt.setDouble(5, high);
			stmt.setDouble(6, low);
			stmt.setDouble(7, close);
			stmt.setLong(8, volume);

			int rs = stmt.executeUpdate();
			return rs > 0;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public Boolean tableExists(Connection con, String tableName) {

		DatabaseMetaData meta;
		try {
			meta = con.getMetaData();
			ResultSet res = meta.getTables(null, null, tableName, new String[] { "TABLE" });
			while (res.next()) {
				System.out.println("   " + res.getString("TABLE_CAT") + ", "
						+ res.getString("TABLE_SCHEM") + ", "
						+ res.getString("TABLE_NAME") + ", "
						+ res.getString("TABLE_TYPE") + ", "
						+ res.getString("REMARKS"));
				return true;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public abstract class ColumnDef {
		protected String columnName;
		protected Integer length;
		protected String type;

		protected ColumnDef(final String type, final String name, Integer length) {
			this.type = type;
			this.columnName = name;
			this.length = length;
		}

		public abstract String definition();
	}

	private String createTableStatement(String name, List<ColumnDef> cols) {
		String def = "CREATE TABLE " + name + "\n(";
		for (ColumnDef c : cols) {
			def += c.definition();
			if (c == cols.get(cols.size() - 1)) {
				def += "\n";
			} else {
				def += ",\n";
			}
		}
		def += ")\n";
		return def;
	}

	private String createDbStatement(String name) {
		String def = "CREATE DATABASE " + name;
		return def;
	}

	public class StringColumn extends ColumnDef {
		public StringColumn(String name, Integer length) {
			super("VARCHAR", name, length);
		}

		@Override
		public String definition() {
			return columnName + " " + type + "(" + length + ")";
		}
	}

	public class IntegerColumn extends ColumnDef {
		public IntegerColumn(String name) {
			super("INT", name, null);
		}

		@Override
		public String definition() {
			return columnName + " " + type;
		}
	}

	public class DoubleColumn extends ColumnDef {
		public DoubleColumn(String name) {
			super("FLOAT", name, null);
		}

		@Override
		public String definition() {
			return columnName + " " + type;
		}
	}

}
