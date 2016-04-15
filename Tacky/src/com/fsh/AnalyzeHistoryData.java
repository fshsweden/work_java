package com.fsh;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ev112.codeblack.common.utilities.Flag;
import com.ib.client.Contract;
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
 *	Should change this to use MyTrade instead! 
 * 
 */
public class AnalyzeHistoryData {

	private MarketAPI marketAPI;
	private Logger logger = LogManager.getLogger(getClass());

	Boolean hasArg = true;
	Boolean noArg = false;
	Boolean req = true;
	Boolean notreq = false;

	private String twsHost = "";
	private String twsPort = "";
	private String twsClientId = "";
	private String twsAccount = ""; // default
	
	private LinkedBlockingQueue<MyInstrument> instrumentQueue = new LinkedBlockingQueue<MyInstrument>();

	private int duration_i = 5;
	private String duration_unit_s = "Y";
	private String barsize_s = "1D";
	
	public enum PositionState {NEUTRAL, LONG, SHORT } ;
	
	private List<Contract> contracts = new ArrayList<Contract>();
	private List<Symbol> symbols = new ArrayList<Symbol>();
	
	private void fillContractList() {

		/* Replace Contract reference with "GenericContract" */
		Contract c = new Contract();
		contracts.add(makeContract("STK", "DFE","SMART","USD","",""));
		contracts.add(makeContract("STK", "ISRA","SMART","USD","",""));
		contracts.add(makeContract("STK", "EWS","SMART","USD","",""));
		contracts.add(makeContract("STK", "EWO","SMART","USD","",""));
		contracts.add(makeContract("STK", "EIRL","SMART","USD","",""));
		
//		contracts.add(makeContract("STK", "MIDD","","","",""));
		contracts.add(makeContract("STK", "VTI","SMART","USD","",""));
		contracts.add(makeContract("STK", "SPY","SMART","USD","",""));
		contracts.add(makeContract("STK", "RSP","SMART","USD","",""));
		contracts.add(makeContract("STK", "SPD","SMART","USD","",""));
		
		contracts.add(makeContract("STK", "FXE","SMART","USD","",""));
	}
	
	private Contract makeContract(String secType, String symbol, String exchange, String currency, String expiry, String tradingClass) {
		Contract c = new Contract();
		c.secType(secType);
		c.symbol(symbol);
		c.exchange(exchange);
		c.currency(currency);
		c.lastTradeDateOrContractMonth(expiry);
		c.tradingClass(tradingClass);
		return c;
	}
	
	
	
	

	/*
	 * 
	 * 
	 * 
	 */
	public AnalyzeHistoryData(String args[]) {

		Options options = new Options();
		options.addOption("help", false, "Show help");

		options.addOption("host", true, "Hostname or IP");
		options.addOption("port", true, "Port number");
		options.addOption("account", true, "Account name");
		options.addOption("client", true, "Client # (1-100 unique)");

		// EDecoder.enableDebugging(true);
		
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
						logger.debug("[INFO]:" + it.name() + " "	+ s + " " + info.get(s));
					}
					
					/* connection errors */
					if (info.get("CODE").equals("507")) {
						logger.error("Invalid/Duplicate Client ID");
						System.exit(1);
					}
					
					/* reqHistory errors - should be directed */
					if (info.get("CODE").equals("162")) {
						logger.error("No market data permission");
					}
					
					if (info.get("CODE").equals("354")) {
						logger.error("Requested data not subscribed");
					}
				}
			});

			if (rc.get("STATUS").equals("OK")) {
				
				fillContractList();
				new Thread(loadAndExecuteThread).start();
				
				for (Contract c : contracts) {
					
					final Flag f = new Flag(false);
					doAnalyze(c, duration_i, duration_unit_s, new IsReady() {
						@Override
						public void isReady() {
							f.set(true);
						}
					});
					
					f.waitUntil(true);
				}
				
				/*
				 * Add a poison-pill to instrument queue
				 */
				Contract poison = new Contract();
				poison.secType("None");
				MyIBInstrument m = new MyIBInstrument(poison);
				instrumentQueue.add(m);
			}

		} catch (UnrecognizedOptionException ex) {
			logger.info(ex.getLocalizedMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("AnalyzeHistoryData", options);
			System.exit(5);
		} catch (ParseException e) {
			logger.info("CLI Option Parsing error!");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("AnalyzeHistoryData", options);
			System.exit(4);
		}

	}
	
	/*
	 * 
	 * 
	 */
	private Runnable loadAndExecuteThread = new Runnable() {
		@Override
		public void run() {

			MyInstrument m;
			try {
				while ((m = instrumentQueue.take()) != null) {

					MyIBInstrument myib = (MyIBInstrument) m;
					if (myib.getContract().secType() == SecType.None) {
						// This signals "exit"
						
						System.out.println("\n------------------------- SUMMARY -------------------------");
						for (Symbol ss: symbols) {
							System.out.println(ss.statistics());
						}
						
						System.exit(1);
					} 
					else {
						// logger.info("Symbol " + getId(m) + " loaded OK, getting Price History ");
						
						final MyInstrument instr = m;
						
						final Flag flag = new Flag(false);
						
						Symbol sym = new Symbol(instr.getSymbol());
						symbols.add(sym);
						
						marketAPI.loadPriceHistory(instr, duration_i, duration_unit_s, barsize_s, new HistoryPriceHandler() {
							@Override
							public void bars(List<GenericBar> bars) {
								// logger.info("---- Evaluating " + getId(instr) + " ---------------------------------------------");
								sym.calcSignalsAndAct(instr, bars);
								// logger.info("Price History for " + getId(instr) + " analyzed!");
								// logger.info("-----------------------------------------------------------------------------------\n");
								flag.set(true);
							}
						});

						flag.waitUntil(true);
						
						
					}
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	
	private void doAnalyze(Contract c, Integer duration_i, String duration_unit_s, IsReady ready) {
		
		/*
		 * Producer
		 */
		GenericSecType st = GenericSecType.valueOf(c.secType().name());

		// logger.info("Loading Symbols for " + c.symbol() + " " + c.exchange() + " " + c.currency() + " " + c.lastTradeDateOrContractMonth());
		
		marketAPI.loadSymbols(st, c.symbol(), c.exchange(), c.currency(), c.lastTradeDateOrContractMonth(), new SymbolEventHandler() {
			@Override
			public void symbolLoaded(MyInstrument i) {
				// logger.info("Adding " + getId(i) + " to Instrument Queue");
				instrumentQueue.add(i);
			}

			@Override
			public void noMoreSymbols() {
				ready.isReady();
			}
		});
	}
	
	private String getId(Contract c) {
		return c.secType().name() + "/" + c.symbol() + "/" + c.exchange() + "/" + c.currency() + "/" + c.tradingClass();
	}
	
	private String getId(MyInstrument i) {
		return i.getSymbol() + "/" + i.getExchange() + "/" + i.getCurrency() + "/" + i.getMultiplier() + "/" + i.getStr("LONGNAME");
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
