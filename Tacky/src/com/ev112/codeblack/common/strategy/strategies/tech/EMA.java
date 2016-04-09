package com.ev112.codeblack.common.strategy.strategies.tech;

import java.util.ArrayList;
import java.util.List;

public class EMA {
	private int periods;
	private double ema = 0d;
	private int count = 0;
	private final double k;
	private final double x;

	private List<Double> values = new ArrayList<Double>();

	public EMA(Integer periods) {
		this.periods = periods;
		
		k = 2.0d / (periods + 1.0d);
		x = 1.0d - k;
		
		// System.out.println("k = " + k + " and x = " + x);
	}

	private double CalculateEMA(double todaysPrice, double EMAYesterday) {
		return (todaysPrice * k) + (EMAYesterday * x);
	}

	private double yesterdayEMA = 0d;

	public double addPrice(Double value) {

		/* if this is the first value, come up with a start value! */
		if (count++ == 0) {
			// We just set EMA = CLOSE since we have no other data!
			ema = value;
			yesterdayEMA = ema;
			return ema;
		}

		yesterdayEMA = ema = CalculateEMA(value, yesterdayEMA);
		
		return ema;
	}

	public double getEMA() {
		return ema;
	}

	public Boolean isValid() {
		return count >= periods;
	}

	public void test() {
		/*
		 	FACIT:
		 	164,9200
			164,9200
			164,9573
			164,9724
			165,0027
			165,0370
			165,0414
			164,9572
			164,8269
			164,6913
			164,6112
			164,5790
		 */
		System.out.println("" + addPrice(164.92));
		System.out.println("" + addPrice(164.92));
		System.out.println("" + addPrice(165.2));
		System.out.println("" + addPrice(165.07));
		System.out.println("" + addPrice(165.2));
		System.out.println("" + addPrice(165.26));
		System.out.println("" + addPrice(165.07));
		System.out.println("" + addPrice(164.41));
		System.out.println("" + addPrice(163.98));
		System.out.println("" + addPrice(163.81));
		System.out.println("" + addPrice(164.09));
		System.out.println("" + addPrice(164.37));
		System.out.println("" + addPrice(164.44));
		System.out.println("" + addPrice(164.4));
		System.out.println("" + addPrice(164.34));
		System.out.println("" + addPrice(164.35));
		System.out.println("" + addPrice(164.35));
		System.out.println("" + addPrice(164.29));
		System.out.println("" + addPrice(164.44));
		System.out.println("" + addPrice(164.4));
	}

	public static void main(String[] args) {
		new EMA(14).test();
	}
}
