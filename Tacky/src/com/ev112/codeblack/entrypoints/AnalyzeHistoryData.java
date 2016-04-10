package com.ev112.codeblack.entrypoints;

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

import com.ev112.codeblack.common.strategy.strategies.tech.ClassicMACD;
import com.ev112.codeblack.common.utilities.DateTools;
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
 * BUG: THis app doesnt use CONFIG, it is hardcoded against alphatrading.dnsalias.com !!!
 * 
 * 
 * 
 * 
 * 
 * 
 */
public class AnalyzeHistoryData {

	private MarketAPI marketAPI;
	
	Boolean hasArg = true;
	Boolean noArg = false;
	Boolean req = true;
	Boolean notreq = false;
	
	private String symbol;
	private SecType secType 		= SecType.STK;
	private String twsHost		= "";
	private String twsPort		= "";
	private String twsClientId	= "";
	private String twsAccount	= "";  // default
	private String currency		= "";
	private String exchange		= "";
	private String expiry		= "";
	private String duration 		= "";
	
	private String env = "";
	private Configuration alpha_config;
	
	private LinkedBlockingQueue<MyInstrument> q = new LinkedBlockingQueue<MyInstrument>();

	private int duration_i = 2;
	private String duration_unit_s = "Y";
	
	/*
	 * 
	 * 
	 * 
	 */
	public AnalyzeHistoryData(String args[]) {

		Options options = new Options();		options.addOption("help", false, "Show help");
		
		options.addOption("host",		true, "Hostname or IP");
		options.addOption("port",		true, "Port number");
		options.addOption("account",		true, "Account name");
		options.addOption("client",		true, "Client # (1-100 unique)");
		
		options.addOption("sectype",		true, "Security type");
		options.addOption("symbol", 		true, "Symbol or Underlying (for FUT)");
		options.addOption("expiry", 		true, "Expiry, only used for sectype FUT");
		options.addOption("exchange", 	true, "Exchange abbrev");
		options.addOption("currency", 	true, "Currency");
		options.addOption("duration", 	true, "Duration");
		options.addOption("unit", 		true, "Unit");

		
		CommandLineParser parser = new BasicParser();
		try {
			CommandLine cmd = parser.parse( options, args);
			
			if (cmd.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "SaveHistoryData", options );
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
			
			if (cmd.hasOption("unit")) {
				duration_unit_s = cmd.getOptionValue("unit");
			}
			
			
			

			if (symbol == null || symbol.equals("")) {
				System.out.println("Must give a symbol!");
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "SaveHistoryData", options );
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
						System.out.println("[INFO]:" + it.name() + " " + s + " " + info.get(s));
					}
				}
			});
			
			if (rc.get("STATUS").equals("OK")) {

				/*
				 * Consumer Thread
				 * 
				 * 
				 * 
				 */
				
				Runnable r = new Runnable() {
					@Override
					public void run() {
						
						MyInstrument m;
						try {
							while ((m = q.take()) != null) {
								
								MyIBInstrument myib = (MyIBInstrument)m;
								if (myib.getContract().secType() == SecType.None) {
									// This signals "exit"
									System.exit(1);
								}
								else {
									System.out.println("Symbol " + m.getSymbol() + " loaded OK, getting Price History ");
									AtomicBoolean flag = new AtomicBoolean(false);
									
									final MyInstrument instr = m;
									
									marketAPI.loadPriceHistory(instr, duration_i, duration_unit_s, new HistoryPriceHandler() {
										@Override
										public void bars(List<GenericBar> bars) {
											analyzePriceHistory(instr, bars);
											System.out.println("Price History for " + instr.getSymbol() + " analyzed!");
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
				 * 
				 * Producer
				 * 
				 */
				GenericSecType st = GenericSecType.valueOf(secType.name());
				
				System.out.println("Loading Symbols for " + symbol + " " + exchange + " " + currency + " " + expiry);
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
			
			
			
		} 
		catch (UnrecognizedOptionException ex) {
			System.out.println(ex.getLocalizedMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "SaveHistoryData", options );
			System.exit(5);
		}
		catch (ParseException e) {
			System.out.println("CLI Option Parsing error!");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "SaveHistoryData", options );
			System.exit(4);
		}

		
		
	}

	/*
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	public void analyzePriceHistory(MyInstrument instr, List<GenericBar> bars) {
		
		ClassicMACD macd = new ClassicMACD(26, 12, 9);
		
		String endDate = DateTools.getCurDateTimeLongAsIBStr();

		Double highest = -9999999.0;
		Double lowest = 9999999.0;
		
		SummaryStatistics stats = new SummaryStatistics();
		
		for (GenericBar b : bars) {
			String symbol = instr.getSymbol();
			String date = DateTools.DateFromTimestamp(b.getTime() * 1000);
			
			System.out.println(symbol + " " + date + " time: " + " open:" + b.getOpen() + " close:" + b.getClose() + " low:" + b.getLow() + " high:" + b.getHigh());
			
			macd.addPrice(b.getClose());
			
			// System.out.println("Current MACD:" + macd.getMacd() + " " + macd.getSignal());
			
			if (macd.getMacd() > highest)
				highest = macd.getMacd();
			if (macd.getMacd() < lowest)
				lowest = macd.getMacd();
			
			stats.addValue(macd.getMacd());
		}
		
		System.out.println("Lowest MACD:" + lowest);
		System.out.println("Highest MACD:" + highest);
		
		System.out.println("MACD StdDev:" + stats.getStandardDeviation());
		// check standard deviation from 0
		

		// Second run:
		
		
		Boolean armed = false;
		Double oldDiff = 0d;
		
		macd = new ClassicMACD(26, 12, 9);
		for (GenericBar b : bars) {
			String symbol = instr.getSymbol();
			String date = DateTools.DateFromTimestamp(b.getTime() * 1000);
			
			// System.out.println(symbol + " " + date + /* " time: " + b.formattedTime() + */ " open:" + b.getOpen() + " close:" + b.getClose() + " low:" + b.getLow() + " high:" + b.getHigh());
			macd.addPrice(b.getClose());
			
			Double diff = macd.getMacd() - macd.getSignal();
			Double absdiff = Math.abs(diff);
			
			// System.out.println("Current MACD:" + macd.getMacd() + " " + macd.getSignal() + " " + diff);
			System.out.println(DateTools.DateFromTimestamp(b.getTime() * 1000) + " " + b.getClose());
			
			if (!armed && !in_pos && absdiff > stats.getStandardDeviation() * 0.5) {
				/* arm indicator! */
				armed = true;
				System.out.println("....ARMING....");
			}
			else if (armed || in_pos) {
				if (isCross(oldDiff, diff)) {
					
					if (oldDiff > diff) {
						System.out.println("We have a cross DOWN!");
						takeOrExitPos("SHORT", b.getClose());
					}
					else {
						System.out.println("We have a cross UP!");
						takeOrExitPos("LONG", b.getClose());
					}
					armed = false;
				}
			}
			
			oldDiff = diff;
		}
		
		System.out.println("Last price is " + bars.get(bars.size()-1).getClose());
	}
	
	
	private Boolean in_pos = false;
	private Double entry_price = 0d;
	
	private void takeOrExitPos(String str, Double price) {
		if (str.equals("LONG")) {
			if (in_pos) {
				// exit SHORT pos
				Double profit = entry_price - price;
				System.out.println("Exit short pos with Result:" + profit);
				in_pos = false;
			}
			else {
				entry_price = price;
				in_pos = true;
				System.out.println("Taking long pos at " + entry_price);
			}
		}
		else { // SHORT
			if (in_pos) {
				// exit LONG pos
				Double profit = price - entry_price;
				System.out.println("Exit long pos at Result:" + profit);
				in_pos = false;
			}
			else {
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
		new AnalyzeHistoryData(args);
	}

	private Option createOption(String opt, boolean hasArg, String description, boolean required) throws IllegalArgumentException {
		Option option = new Option(opt, hasArg, description);
		option.setRequired(required);
		return option;
	}
}
