package com.fsh;
public class InvestmentManger {
	private Symbol symbol;
	public InvestmentManger(String symbolName) {
		this.symbol = new Symbol(symbolName);
	}
}
