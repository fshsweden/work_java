package com.fsh;

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
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ev112.codeblack.common.dataengine.DataEngineEventHandler.DATA_QUALITY;
import com.ev112.codeblack.common.feed.objects.FeedTrade;
import com.ev112.codeblack.common.strategy.strategies.tech.Candle;
import com.ev112.codeblack.common.strategy.strategies.tech.signals.MACD;
import com.ev112.codeblack.common.strategy.strategies.tech.signals.MACDSignal;
import com.ev112.codeblack.common.utilities.LogTools;
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
 * 
 */
public class AnalyzeHistoryData {

	private MarketAPI marketAPI;
	private Logger logger = LogManager.getLogger(getClass());

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

	private int duration_i = 1;
	private String duration_unit_s = "Y";
	private String barsize_s = "1D";
	
	public enum PositionState {NEUTRAL, LONG, SHORT } ;
	
	public class Investment {
		
		private String symbol;
		private String entry_date;
		private Double entry_price;
		private String exit_date;
		private Double exit_price;
		private Integer pos;
		
		public Investment(String symbol) {
			super();
			this.symbol = symbol;
		}
		
		public void enterPosition(String date, Integer new_pos, Double price) {
			entry_date = date;
			entry_price = price;
			pos = new_pos;
		}
		
		public void closePosition(String date, Double price) {
			exit_date = date;
			exit_price = price;
		}
		
		public Integer getPosition() {
			return pos;
		}
		
		public Double getPnL() {
			if (pos > 0)
				return exit_price - entry_price;
			if (pos < 0)
				return entry_price - exit_price;
			return 0.0;
		}
		
		public Double getPnlPct() {
			return getPnL() / entry_price;
		}
	}
	
	public class Symbol {
		private String symbol;
		private List<Investment> trades = new ArrayList<Investment>();
		private Investment currentInvestment = null;
		
		public Symbol(String name) {
			symbol = name;
		}
		
		public void addInvestment(Investment i) {
			trades.add(i);
		}
		
		public void takePosition(String date, Integer new_pos, Double price) {
			
			if (currentInvestment == null) {
				currentInvestment = new Investment(symbol);
				currentInvestment.enterPosition(date, new_pos, price);
			}
			else {
				if (currentInvestment.getPosition() < 0) {
					/* we are short */
					
					if (new_pos >= 0) {
						currentInvestment.closePosition(date, price);
						trades.add(currentInvestment);
					}
					
					if (new_pos > 0) {
						currentInvestment = new Investment(symbol);
						currentInvestment.enterPosition(date, new_pos, price);
					}
				}
				else if (currentInvestment.getPosition() > 0) {
					/* we are long */
				}
				else {
					/* we are neutral */
					
				}
			}
			
			
		}
	}
	
	public class InvestmentManger {
		private Symbol symbol;
		public InvestmentManger(String symbolName) {
			this.symbol = new Symbol(symbolName);
		}
	}
	

	/*
	 * 
	 * 
	 * 
	 */
	public AnalyzeHistoryData(String args[]) {

		LogTools.setLog4JRootLoggerDefaultLevel(Level.ALL);
		
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
		options.addOption("duration", true, "Duration");
		options.addOption("durationunit", true, "Duration Unit");

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

			if (symbol == null || symbol.equals("")) {
				logger.info("Must give a symbol!");
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
						logger.info("[INFO]:" + it.name() + " "
								+ s + " " + info.get(s));
					}
					
					if (info.get("CODE").equals("507")) {
						logger.info("Invalid/Duplicate Client ID");
						System.exit(1);
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
									logger.info("Symbol " + m.getSymbol() + " loaded OK, getting Price History ");
									AtomicBoolean flag = new AtomicBoolean(false);

									final MyInstrument instr = m;

									marketAPI.loadPriceHistory(instr, duration_i, duration_unit_s, barsize_s, new HistoryPriceHandler() {
										@Override
										public void bars(List<GenericBar> bars) {

											analyzePriceHistory(instr, bars);

											logger.info("Price History for "
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

				logger.info("Loading Symbols for " + symbol + " " + exchange + " " + currency + " " + expiry);
				marketAPI.loadSymbols(st, symbol, exchange, currency, expiry, new SymbolEventHandler() {
					@Override
					public void symbolLoaded(MyInstrument i) {
						q.add(i);
					}

					@Override
					public void noMoreSymbols() {
						logger.info("No more symbols!");

						// soooooooo ugly....

						Contract c = new Contract();
						c.secType(SecType.None);
						MyInstrument end = new MyIBInstrument(c);
						q.add(end);
					}
				});
			}

		} catch (UnrecognizedOptionException ex) {
			logger.info(ex.getLocalizedMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("SaveHistoryData", options);
			System.exit(5);
		} catch (ParseException e) {
			logger.info("CLI Option Parsing error!");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("SaveHistoryData", options);
			System.exit(4);
		}

	}

	
	
	
	
	
	/*
	 * 
	 * 
	 */
	public void analyzePriceHistory(MyInstrument instr, List<GenericBar> bars) {

		ClassicMACD macdClassic = new ClassicMACD(26, 12, 9);

		String endDate = DateTools.getCurDateTimeLongAsIBStr();

		Double highest = -9999999.0;
		Double lowest = 9999999.0;

		/*
		 * First scan to calc MACD standard deviation
		 * 
		 */
		SummaryStatistics stats = new SummaryStatistics();

		for (GenericBar b : bars) {
			macdClassic.addPrice(b.getClose());
			if (macdClassic.getMacd() > highest)
				highest = macdClassic.getMacd();
			if (macdClassic.getMacd() < lowest)
				lowest = macdClassic.getMacd();
			stats.addValue(macdClassic.getMacd());
		}

		// Some stats
		logger.info("Lowest MACD:" + lowest);
		logger.info("Highest MACD:" + highest);
		logger.info("MACD StdDev:" + stats.getStandardDeviation());

	
		
		MACD macd = new MACD(26,12);
		MACDSignal macdSignal = new MACDSignal(macd, 9);
		
		/*
		 * Second run:
		 * 
		 */

		int candleId = 1;
		Double oldDiff = 0d;
		String symbol = instr.getSymbol();
		int default_volume = 1;
		int flags = 0;
		String source = "";
		String buyer = "";
		String seller = "";
		DATA_QUALITY dataQuality = DATA_QUALITY.NORMAL;

		// macd = new ClassicMACD(26, 12, 9);
		for (GenericBar b : bars) {
			
			String date = DateTools.DateFromTimestamp(b.getTime() * 1000);

			//macd.addPrice(b.getClose());
			
			Candle c = new Candle(b.getTime() * 1000, 3600*24, candleId++);
			c.addTrade(new FeedTrade(symbol, b.getTime() * 1000, default_volume, b.getClose(), flags, source, buyer, seller, dataQuality, b.getClose()));
			macd.addCandle(c);

			//Double diff = macd.getMacd() - macd.getSignal();
			Double diff = macd.getValue() - macdSignal.getValue();
			Double absdiff = Math.abs(diff);

			// logger.info(date + " " + b.getClose() + " " + macd.getMacd());

			Boolean crossUp		= isCrossUp(date, oldDiff, diff);
			Boolean crossDown	= isCrossDown(date, oldDiff, diff);
			
			if (crossUp || crossDown) {
				if (crossDown) {
					takeOrExitPos2(date, "SHORT", b.getClose());
				} else {
					takeOrExitPos2(date, "LONG", b.getClose());
				}
			}
			
			oldDiff = diff;
		}


		logger.info("Last price is " + bars.get(bars.size() - 1).getClose());
		logger.info("sumPnl=" + Money(sumPnl) + " (" + Percent(sumPct) + ") NO FEES!");
	}

	private Boolean in_pos = false;
	private Double entry_price = 0d;
	private String entry_date = "";
	
	private Double sumPnl = 0d;
	private Double sumPct = 0.0;

	/*
	 * 
	 * 
	 * 
	 */
	private void takeOrExitPos(String date, String str, Double price) {
		entry_date = date;
		if (str.equals("LONG")) {
			if (in_pos) {
				// exit SHORT pos
				Double profit = entry_price - price;
				Double profitpct = profit / entry_price;
				logger.info(date + " EXIT SHORT " + entry_price + " PROFIT "+ Money(profit) + " (" + Percent(profitpct));
				
				sumPct += profitpct;
				
				sumPnl += profit;
				
				// in_pos = false;
				
				entry_price = price;
				in_pos = true;
				logger.info(date + " NEW LONG " + entry_price);
				
			} else {
				entry_price = price;
				in_pos = true;
				logger.info(date + "FIRST LONG " + entry_price);
			}
		} else { // SHORT
			if (in_pos) {
				// exit LONG pos
				Double profit = price - entry_price;
				Double profitpct = profit / entry_price;
				logger.info(date + " EXIT LONG " + entry_price + " PROFIT "+ Money(profit) + " (" + Percent(profitpct));
				
				sumPct += profitpct;

				sumPnl += profit;
				
				// in_pos = false;
				logger.info("Taking NEW short pos at " + entry_price);
				entry_price = price;
				in_pos = true;
				
			} else {
				// 
				logger.info("Taking FIRST short pos at " + entry_price);
				entry_price = price;
				in_pos = true;
			}
		}
	}

	
	
	/*
	 * 
	 * 
	 * 
	 */
	private void takeOrExitPos2(String date, String str, Double price) {
		entry_date = date;
		if (str.equals("LONG")) {
			if (in_pos) {
				// exit SHORT pos
				Double profit = entry_price - price;
				Double profitpct = profit / entry_price;
				logger.info(date + " EXIT SHORT " + price + " PROFIT "+ Money(profit) + " (" + Percent(profitpct) + ")");
				
				sumPct += profitpct;
				sumPnl += profit;
				
				// in_pos = false;
				
				in_pos = false;
			} 
			else {
				entry_price = price;
				in_pos = true;
				logger.info(date + "NEW LONG " + entry_price);
			}
		} else { // SHORT
			if (in_pos) {
				// exit LONG pos
				Double profit = price - entry_price;
				Double profitpct = profit / entry_price;
				logger.info(date + " EXIT LONG " + price + " PROFIT "+ Money(profit) + " (" + Percent(profitpct) + ")");
				
				sumPct += profitpct;
				sumPnl += profit;
				
				in_pos = false;
			} else {
				// 
				entry_price = price;
				in_pos = true;
				logger.info(date + " NEW SHORT " + entry_price);
			}
		}
	}
	
	
	private String Money(final Double d){
		return String.format("%1.2f", d);
	}
	
	private String Percent(final Double d){
		return String.format("%1.2f%%", d);
	}
	
	private String Macd(final Double d){
		return String.format("%1.4f", d);
	}
	
	private Boolean isCross(final String date, final Double a, final Double b) {
		if (a == 0.0 || b == 0.0)
			return false;
		Boolean r = a * b < 0.0;
		if (r) {
			logger.info(date + " CROSS!!! " + Macd(a) + " " + Macd(b));
		}
		return r;
	}

	private Boolean isCrossUp(final String date, final Double a, final Double b) {
		if (a == 0.0 || b == 0.0)
			return false;

		if (a < 0.0 && b >= 0.0) {
			logger.info(date + " CROSS UP, MACD CHANGED FROM " + Macd(a) + " " + Macd(b));
			return true;
		}
		return false;
	}

	private Boolean isCrossDown(final String date, final Double a, final Double b) {
		if (a == 0.0 || b == 0.0)
			return false;

		if (a >= 0.0 && b < 0.0) {
			logger.info(date + " CROSS DOWN, MACD CHANGED FROM " + Macd(a) + " " + Macd(b));
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		new AnalyzeHistoryData(args);
	}

	private Option createOption(String opt, boolean hasArg, String description, boolean required) throws IllegalArgumentException {
		Option option = new Option(opt, hasArg, description);
		option.setRequired(required);
		return option;
	}

}
