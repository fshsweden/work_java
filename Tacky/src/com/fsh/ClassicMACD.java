package com.fsh;

public class ClassicMACD {
	
	private EMA short_ema,long_ema;
	private Double macd = new Double(0.0);
	private EMA signal;
	private Double last_price = new Double(0.0);
	
	public ClassicMACD(Integer long_period, Integer short_period, final Integer signal_ema) {
		long_ema = new EMA(long_period);
		short_ema = new EMA(short_period);
		signal = new EMA(signal_ema);
	}
	
	public Double addPrice(Double price) {
		last_price = price;
		short_ema.addPrice(price);
		long_ema.addPrice(price);
		macd = short_ema.getEMA() - long_ema.getEMA();
		signal.addPrice(macd);
		return signal.getEMA();
	}
	
	public Double getMacd() {
		return macd;
	}
	
	public Double getSignal() {
		return signal.getEMA();
	}
	
	public Double getEMA1() {
		return short_ema.getEMA();
	}
	public Double getEMA2() {
		return long_ema.getEMA();
	}
	
	// Helpers!
	
	public Boolean isCrossOver() {
		return getMacd() <= getSignal(); // MACD falls below Signal !
	}
	
	public Double getDivergence()  {
		return getMacd() - getSignal();
	}
	
	public Boolean isUpwardMomentum() {
		return getMacd() > 0;
	}
	
	public Boolean isDownwardMomentum() {
		return getMacd() < 0;
	}
}
