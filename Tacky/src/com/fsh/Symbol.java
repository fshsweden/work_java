package com.fsh;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mytrade.common_if.GenericBar;
import mytrade.common_if.MyInstrument;

public class Symbol {

	private Logger logger = LogManager.getLogger(getClass());

	private String symbol;
	private List<Position> trades = new ArrayList<Position>();
	private Position currentPosition = null;

	private Double sumPnl = 0d;
	private Double sumPct = 0.0;

	private Double sumPnlLong = 0d;
	private Double sumPnlShort = 0d;

	private Integer countShort = 0;
	private Integer countLong = 0;

	Double startValue = null;
	Double lastValue = null;
	
	public Symbol(String name) {
		symbol = name;
	}

	public String statistics() {
		return String.format("%-6s %1.2f %1.2f%%  Price range: %1.2f - %1.2f B&H: %1.2f%%", symbol, sumPnl, sumPct, startValue, lastValue, ((lastValue - startValue) / startValue * 100.0));
	}
	
	public void recordPosition(Position i) {
		trades.add(i);
	}

	/*
	 * 
	 * 
	 * 
	 */
	private void takeOrExitPosMethod1(PositionType pType, String date, Double price) {

		if (pType == PositionType.LONG) {

			// Take new LONG Position
			if (currentPosition == null) {
				currentPosition = new Position(symbol);
				currentPosition.enterPosition(PositionType.LONG, date, 1, price);

				// logger.info(date + " ENTER LONG " + currentPosition.getEntryPrice());
			} 
			else {

				currentPosition.closePosition(date, price);

				// We had a SHORT pos - exit!
				Double profit = currentPosition.getEntryPrice() - price;
				Double profitpct = profit / currentPosition.getEntryPrice() * 100.0;
				// logger.info(date + " EXIT SHORT " + price + " PROFIT " + Money(profit) + " (" + Percent(profitpct) + " " + Money(profit) + "/" + currentPosition.getEntryPrice() + " )");

				sumPct += profitpct;
				sumPnl += profit;
				countShort++;
				sumPnlShort += profit;
				
				// logger.info("  " + ProfitPct(profitpct) + " ACCUMULATED PNL=" + Percent(sumPct));

				currentPosition = null;
			}
		} else
			if (pType == PositionType.SHORT) {

				// Take new SHORT Position
				if (currentPosition == null) {

					currentPosition = new Position(symbol);
					currentPosition.enterPosition(PositionType.SHORT, date, -1, price);

					// logger.info(date + " ENTER SHORT " + currentPosition.getEntryPrice());
				}
				// We had a LONG pos - exit!
				else {

					currentPosition.closePosition(date, price);

					Double profit = price - currentPosition.getEntryPrice();
					Double profitpct = profit / currentPosition.getEntryPrice() * 100.0;
					// logger.info(date + " EXIT LONG " + price + Profit(profit) + " (" + Percent(profitpct) + " " + Money(profit) + "/" + currentPosition.getEntryPrice() + " )");

					sumPct += profitpct;
					sumPnl += profit;
					countLong++;

					sumPnlLong += profit;

					// logger.info("  " + ProfitPct(profitpct) + " ACCUMULATED PNL=" + Percent(sumPct));

					currentPosition = null;
				}
			}
	}

	private String Money(final Double d) {
		return String.format("%1.2f", d);
	}

	private String Profit(final Double d) {
		if (d < 0)
			return String.format("LOSS %s", Money(d));
		else
			return String.format("PROFIT %s", Money(d));
	}
	
	private String ProfitPct(final Double d) {
		if (d < 0)
			return String.format("LOSS %s", Percent(d));
		else
			return String.format("PROFIT %s", Percent(d));
	}
	
	private String Percent(final Double d) {
		return String.format("%1.2f%%", d);
	}

	private String Macd(final Double d) {
		return String.format("%1.4f", d);
	}

	private Boolean isCrossUp(final String date, final Double startValue, final Double oldDiff, final Double newDiff) {
		if (oldDiff == 0.0 || newDiff == 0.0)
			return false;

		if (oldDiff < 0.0 && newDiff >= 0.0) {
			logger.debug(date + " CROSS UP, MACD DIFF CHANGED FROM " + Macd(oldDiff) + " " + Macd(newDiff) + " " + (Percent(oldDiff / startValue * 100.0)));
			return true;
		}
		return false;
	}

	private Boolean isCrossDown(final String date, final Double startValue, final Double oldDiff, final Double newDiff) {
		if (oldDiff == 0.0 || newDiff == 0.0)
			return false;

		if (oldDiff >= 0.0 && newDiff < 0.0) {
			logger.debug(date + " CROSS DOWN, MACD DIFF CHANGED FROM " + Macd(oldDiff) + " " + Macd(newDiff) + " " + (Percent(oldDiff / startValue * 100.0)));
			return true;
		}
		return false;
	}

	public class MACDInfo {
		Double high;
		Double low;
		Double stddev;
	}

	MACDInfo getMACDInfo(MyInstrument instr, List<GenericBar> bars) {

		ClassicMACD macdClassic = new ClassicMACD(26, 12, 9);
		MACDInfo mi = new MACDInfo();

		SummaryStatistics stats = new SummaryStatistics();

		for (GenericBar b : bars) {
			macdClassic.addPrice(b.getClose());
			if (macdClassic.getMacd() > mi.high)
				mi.high = macdClassic.getMacd();
			if (macdClassic.getMacd() < mi.low)
				mi.low = macdClassic.getMacd();
			stats.addValue(macdClassic.getMacd());
		}

		// Some stats
//		logger.info("Lowest MACD:" + mi.low);
//		logger.info("Highest MACD:" + mi.high);
//		logger.info("MACD StdDev:" + stats.getStandardDeviation());

		return mi;
	}

	/*
	 * 
	 * 
	 * 
	 */
	public void calcSignalsAndAct(MyInstrument instr, List<GenericBar> bars) {

		ClassicMACD macd = new ClassicMACD(26, 12, 9);
		EMA longEMA = new EMA(55);
		EMA shortEMA = new EMA(21);
		EMA ema5 = new EMA(5);

		Double oldDiff = 0d;

		
		Predicate<EMA> p; 
		
		for (GenericBar b : bars) {

			if (startValue == null) {
				startValue = b.getClose();
			}

			lastValue = b.getClose();
			
			macd.addPrice(b.getClose());
			longEMA.addPrice(b.getClose());
			shortEMA.addPrice(b.getClose());
			ema5.addPrice(b.getClose());

			String date = DateTools.DateFromTimestamp(b.getTime() * 1000);
			// System.out.println("DATE:" + date);

			Double diff = macd.getDiff();
			Double absdiff = Math.abs(diff);

			// logger.info(date + " " + b.getClose() + " " + macd.getMacd());

			/*
			for (Indicator i : indicators) {
				switch (i.getAction()) {
					case LONG: 		// Go LONG
					break;
					case SHORT:		// Go SHORT
					break;
					case UP:			// Direction is UP but dont go LONG
					break;
					case DN:			// Direction is UP but dont go LONG
					break;
					case UNKNOWN:	// No action
					break;
				}
			}
			*/

			if (currentPosition == null && macd.getMacd() >= -0.25 && macd.getMacd() <= 0.25) {
				/* SKIP */
			} 
			else {
				Boolean crossUp		= isCrossUp(date, startValue, oldDiff, diff);
				Boolean crossDown	= isCrossDown(date, startValue, oldDiff, diff);
				
				Boolean uptrend = longEMA.getEMA() > shortEMA.getEMA();
				Boolean dntrend = shortEMA.getEMA()  < longEMA.getEMA();
				
				Boolean trendTest = false;

				if (crossUp || crossDown) {
					if (crossDown) {
						if (trendTest && currentPosition == null && uptrend ) {
							logger.warn("Skipping NEW position since longEMA < shortEMA)");
						} else {
							takeOrExitPosMethod1(PositionType.SHORT, date, b.getClose());
						}
					} else {

						if (trendTest && currentPosition == null && dntrend ) {
							logger.warn("Skipping position since longEMA > shortEMA");
						} else {
							takeOrExitPosMethod1(PositionType.LONG, date, b.getClose());
						}
					}
				}
			}

			oldDiff = diff;
		}

		// Dont print if we have no positions!
//		if (countLong > 0 || countShort > 0) {
//			// logger.info("Last price is " + bars.get(bars.size() - 1).getClose());
//			logger.info("sumPnl=" + Money(sumPnl) + " (" + Percent(sumPct) + ") NO FEES!");
//			logger.info("sumPnl Long =" + Money(sumPnlLong) + " " + countLong + " Positions");
//			logger.info("sumPnl Short=" + Money(sumPnlShort) + " " + countShort + " Positions");
//		}
	}
}
