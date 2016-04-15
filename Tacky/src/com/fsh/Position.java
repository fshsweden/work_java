package com.fsh;
public class Position {
	
	private String symbol;
	private String entry_date;
	private Double entry_price;
	private String exit_date;
	private Double exit_price;
	private Integer pos;
	private PositionType ptype;
	
	public Position(String symbol) {
		super();
		this.symbol = symbol;
	}
	
	public void enterPosition(PositionType pt, String date, Integer new_pos, Double price) {
		ptype = pt;
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
	
	public Double getEntryPrice() {
		return entry_price;
	}
	
	public Double getExitPrice() {
		return exit_price;
	}

	public String getEntryDate() {
		return entry_date;
	}
	
	public String getExitDate() {
		return exit_date;
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
