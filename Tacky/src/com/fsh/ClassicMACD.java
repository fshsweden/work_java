package com.fsh;

public class ClassicMACD {
	
	private EMA short_ema,long_ema;
	private Double macd = null;
	private Double macdPct = null;
	private EMA signal;
	private Double signalPct = null;
	private Double last_price = new Double(0.0);
	private Double first_price = null;
	
	public ClassicMACD(Integer long_period, Integer short_period, final Integer signal_ema) {
		long_ema = new EMA(long_period);
		short_ema = new EMA(short_period);
		signal = new EMA(signal_ema);
	}
	
	public Double addPrice(Double price) {
		
		if (first_price == null) {
			// System.out.println("  SETTING FIRST VALUE TO " + price);
			first_price = price; // for % return calculation
		}
		last_price = price;
		short_ema.addPrice(price);
		long_ema.addPrice(price);
		
		// System.out.println("  ADDING " + price);
		// System.out.println("  EMA(26)=" + long_ema.getEMA() + " EMA(12)=" + short_ema.getEMA());
		macd = short_ema.getEMA() - long_ema.getEMA();
		macdPct = (macd / first_price) * 100.0;
		// System.out.println("  MACD=" + macd + " PCT:" + macdPct);
		
		signal.addPrice(macd);
		signalPct = (signal.getEMA() / first_price) * 100.0;
		// System.out.println("  EMA(9) on MACD=" + signal.getEMA() + " PCT:" + signalPct);
		
		// System.out.println("DIFF = " + String.format("%1.4f", (macd - signal.getEMA())));
		return signal.getEMA();
	}
	
	public Double getMacd() {
		return macd;
	}
	
	public Double getMacdPct() {
		return (macd / first_price) * 100.0 ;
	}
	
	public Double getDiff() {
		return macd - signal.getEMA();
	}
	
	public Double getSignal() {
		return signal.getEMA();
	}
	
	public Double getSignalPct() {
		return signalPct;
	}
	
	public Double getShortEMA() {
		return short_ema.getEMA();
	}
	public Double getLongEMA() {
		return long_ema.getEMA();
	}
	
}
