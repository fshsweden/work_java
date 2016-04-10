package com.ev112.codeblack.common.utilities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.Days;


public class DateTools {
	public static String getNowAsStr() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date today = Calendar.getInstance().getTime();        
		String reportDate = df.format(today);
		return reportDate;
	}
	public static String getCurDateAsStr() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date today = Calendar.getInstance().getTime();        
		String reportDate = df.format(today);
		return reportDate;
	}
	public static String getCurTimeAsStr() {
		DateFormat df = new SimpleDateFormat("HH:mm:ss");
		Date today = Calendar.getInstance().getTime();        
		String reportDate = df.format(today);
		return reportDate;
	}
	
	public static String getCurDateTimeLongAsStr() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date today = Calendar.getInstance().getTime();        
		String reportDate = df.format(today);
		return reportDate;
	}

	public static String getCurDateTimeLong2AsStr() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		Date today = Calendar.getInstance().getTime();        
		String reportDate = df.format(today);
		return reportDate;
	}
	
	// IB special format
	public static String getCurDateTimeLongAsIBStr() {
		DateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		Date today = Calendar.getInstance().getTime();        
		String reportDate = df.format(today);
		return reportDate;
	}
	
	public static String getTodayAsStr() {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Date today = Calendar.getInstance().getTime();        
		String reportDate = df.format(today);
		return reportDate;
	}
	public static String getTodayAsStrNoDashes() {
		DateFormat df = new SimpleDateFormat("yyyyMMdd");
		Date today = Calendar.getInstance().getTime();        
		String reportDate = df.format(today);
		return reportDate;
	}

	public static String getDateAsStr(Date d) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		String reportDate = df.format(d);
		return reportDate;
	}
	
	public static Long getCurrentTs() {
		return System.currentTimeMillis(); 
	}
	
	public static Long getCurrentTime() {
		return new Date().getTime();
	}
	
	public static String getDateTimeAsStr(Date d) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
		String reportDate = df.format(d);
		return reportDate;
	}
	
	public static String getDateAsStr(Long ts) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		String reportDate = df.format(new Date(ts));
		return reportDate;
	}
	
	public static String getTimeAsStr(Long ts) {
		return getTimeAsStr(ts, true); // default is WITH ms
	}
	
	public static String getTimeAsStr(Long ts, Boolean with_ms) {
		DateFormat df;
		if (with_ms)
			df = new SimpleDateFormat("HH:mm:ss.SSS");
		else
			df = new SimpleDateFormat("HH:mm:ss");
		String reportDate = df.format(new Date(ts));
		return reportDate;
	}
	
	public static String getDateTimeAsStr(Long ts) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		String reportDate = df.format(new Date(ts));
		return reportDate;
	}
	
	public static int getHourFromTimestamp(long ts) {
		Calendar calendar = Calendar.getInstance();
		// calendar.setTimeZone(TimeZone.getDefault());
		calendar.setTimeInMillis(ts);

//		int year = calendar.get(Calendar.YEAR);
//		int day = calendar.get(Calendar.DATE);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
//		int minute = calendar.get(Calendar.MINUTE);
		
		return hour;
	}

	public static int getMinuteFromTimestamp(long ts) {
		Calendar calendar = Calendar.getInstance();
		// calendar.setTimeZone(TimeZone.getDefault());
		calendar.setTimeInMillis(ts);

//		int year = calendar.get(Calendar.YEAR);
//		int day = calendar.get(Calendar.DATE);
//		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		
		return minute;
	}
	
	/*
	 * argument could be System.currentTimeMillis() !
	 */
	public static String DateFromTimestamp(long l) {
		Date d = new Date(l);
		return new SimpleDateFormat("yyyy-MM-dd").format(d);
	}
	
	public static String TimeFromTimestamp(long l) {
		Date d = new Date(l);
		return new SimpleDateFormat("HH:mm:ss").format(d);
	}
	
	public static String TimeWithMsFromTimestamp(long l) {
		Date d = new Date(l);
		return new SimpleDateFormat("HH:mm:ss.SSS").format(d);
	}
	
	public static String DateTimeFromTimestamp(long l) {
		Date d = new Date(l);
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
	}
	
	public static String DateTimeStringFromDate(Date d) {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
	}

	public static String DateStringFromDate(Date d) {
		return new SimpleDateFormat("yyyy-MM-dd").format(d);
	}
	
	public static String TimeStringFromDate(Date d) {
		return new SimpleDateFormat("HH:mm:ss").format(d);
	}
	
	public static Long TimeFromString(String str) {
		if (str == null)
			return null;
//		System.out.println("Trying to parse:" + str);
		DateFormat format = new SimpleDateFormat("HH:mm:ss");
		Date date;
		try {
			date = format.parse(str);
			return date.getTime();
		} catch (ParseException e) {
			return new Long(0); // a default formaqt that doesnt destroy anything
		}
	}
	
	public static Date DateFromString(String str) {
		if (str == null)
			return null;
//		System.out.println("Trying to parse:" + str);
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date;
		try {
			date = format.parse(str);
			return date;
		} catch (ParseException e) {
			return null;
		}
	}

	public static Date DateFromDateString(String str) {
		if (str == null)
			return null;
//		System.out.println("Trying to parse:" + str);
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date date;
		try {
			date = format.parse(str);
			return date;
		} catch (ParseException e) {
			return null;
		}
	}
	
	public static String YearFromDateString(String str) {
		if (str == null) {
			return null;
		}
		try {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			format.parse(str);
		} catch (ParseException pe) {
			return null;
		}
		return str.substring(0, 4);
	}

	public static String MonthFromDateString(String str) {
		if (str == null) {
			return null;
		}
		try {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			format.parse(str);
		} catch (ParseException pe) {
			return null;
		}
		return str.substring(5, 7);
	}

	public static String DayFromDateString(String str) {
		if (str == null) {
			return null;
		}
		try {
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			format.parse(str);
		} catch (ParseException pe) {
			return null;
		}
		return str.substring(8, 10);
	}

	public static Date DateFromIBDateTimeString(String str) {
		if (str == null)
			return null;
//		System.out.println("Trying to parse:" + str);
		DateFormat format = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		Date date;
		try {
			date = format.parse(str);
			return date;
		} catch (ParseException e) {
			return null;
		}
	}
	
	public static Date DateFromIBDateString(String str) {
		if (str == null)
			return null;
//		System.out.println("Trying to parse:" + str);
		DateFormat format = new SimpleDateFormat("yyyyMMdd");
		Date date;
		try {
			date = format.parse(str);
			return date;
		} catch (ParseException e) {
			return null;
		}
	}
	
	/*	--------------------------------------------------------------------------
	 * 
	 * 
	 * 	--------------------------------------------------------------------------
	 */
	public static int  getNumWeekDaysBetweenDates(Date start, Date end){
	    //Ignore argument check

	    Calendar c1 = Calendar.getInstance();
	    c1.setTime(start);
	    int w1 = c1.get(Calendar.DAY_OF_WEEK);
	    c1.add(Calendar.DAY_OF_WEEK, -w1);

	    Calendar c2 = Calendar.getInstance();
	    c2.setTime(end);
	    int w2 = c2.get(Calendar.DAY_OF_WEEK);
	    c2.add(Calendar.DAY_OF_WEEK, -w2);

	    //end Saturday to start Saturday 
	    long days = (c2.getTimeInMillis()-c1.getTimeInMillis())/(1000*60*60*24);
	    long daysWithoutWeekendDays = days-(days*2/7);

	    // Adjust w1 or w2 to 0 since we only want a count of *weekdays*
	    // to add onto our daysWithoutWeekendDays
	    if (w1 == Calendar.SUNDAY) {
	        w1 = Calendar.MONDAY;
	    }

	    if (w2 == Calendar.SUNDAY) {
	        w2 = Calendar.MONDAY;
	    }

	    return (int)daysWithoutWeekendDays-w1+w2;
	}

	/*
	 * 
	 */
	public static Pair<Date, Date> cvtMonthToDatePair(String shortMonth, int year) {

		SimpleDateFormat format = new SimpleDateFormat("MMM yyyy", Locale.ENGLISH);

		// the parsed date will be the first day of the given month and year
		Date startDate;
		try {
			startDate = format.parse(shortMonth + " " + year);
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(startDate);
			// set calendar to the last day of this given month
			calendar.set(Calendar.DATE, calendar.getActualMaximum(Calendar.DATE));
			// and get a Date object
			Date endDate = calendar.getTime();

			// do whatever you need to do with your dates, return them in a Pair or print out
			return new Pair<Date, Date>(startDate, endDate);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	/*
	 * Convert "JAN" 15  to  "2015-01-01" <--> "2015-01-31"
	 */
	public static Pair<String, String> cvtMonthToDateStrPair(String shortMonth, int year) {

		Pair<Date, Date> pd = cvtMonthToDatePair(shortMonth, year);

		String s1 = DateTools.DateStringFromDate(pd.getFirst());
		// System.out.println("Debug:" + s1);
		String s2 = DateTools.DateStringFromDate(pd.getSecond());
		// System.out.println("Debug:" + s2);

		return new Pair<String, String>(s1, s2);
	}
	
	
	/*	--------------------------------------------------------------------------
	 * 
	 * 
	 * 	--------------------------------------------------------------------------
	 */
	public static int getNumDaysBetweenDates(Date date1, Date date2){
		
		DateTime d1 = new DateTime(date1);
		DateTime d2 = new DateTime(date2);
		int days = Days.daysBetween(d1, d2).getDays();
		return days;
	}
	
	
	public static int getNumDaysBetweenDates(Calendar day1, Calendar day2){
	    Calendar dayOne = (Calendar) day1.clone(),
	            dayTwo = (Calendar) day2.clone();

	    if (dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR)) {
	        return Math.abs(dayOne.get(Calendar.DAY_OF_YEAR) - dayTwo.get(Calendar.DAY_OF_YEAR));
	    } else {
	        if (dayTwo.get(Calendar.YEAR) > dayOne.get(Calendar.YEAR)) {
	            //swap them
	            Calendar temp = dayOne;
	            dayOne = dayTwo;
	            dayTwo = temp;
	        }
	        int extraDays = 0;

	        while (dayOne.get(Calendar.YEAR) > dayTwo.get(Calendar.YEAR)) {
	            dayOne.add(Calendar.YEAR, -1);
	            // getActualMaximum() important for leap years
	            extraDays += dayOne.getActualMaximum(Calendar.DAY_OF_YEAR);
	        }

	        return extraDays - dayTwo.get(Calendar.DAY_OF_YEAR) + dayOne.get(Calendar.DAY_OF_YEAR);
	    }
	}	
	
	public static List<Date> getWorkdaysBetweenDates(String fromDateStr, String toDateStr) {
		Date fromDate = DateTools.convertStrToDate(fromDateStr);
		Date toDate   = DateTools.convertStrToDate(toDateStr);
		
		List<Date> dates = new ArrayList<Date>();
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(fromDate);
		while (cal.getTime().before(toDate)) {
		    cal.add(Calendar.DATE, 1);
		    if (cal.get(Calendar.DAY_OF_WEEK) == 7 || cal.get(Calendar.DAY_OF_WEEK) == 1) {
		    	
		    }
		    else {
		    		dates.add(cal.getTime());
		    }
		}
		
		return dates;
	}
	
	// expects format YYYY-MM-DD
	public static Date convertStrToDate(String dateStr) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date date;
		try {
			date = format.parse(dateStr);
			return date;
		} catch (ParseException e) {
			return null;
		}
		
	}
	
	public static void main(String[] args) {
		System.out.println(DateTools.getNumWeekDaysBetweenDates(
			DateTools.DateFromDateString("2015-05-04"), 
			DateTools.DateFromDateString("2015-05-11")));
	}
}

