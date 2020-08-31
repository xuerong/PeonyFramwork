package com.peony.common.tool.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 14-4-3.
 */
public class DateUtils {
    private static final Logger log = LoggerFactory.getLogger(DateUtils.class);

    public static final String TIME_FORMAT_TO_YYYY_MM_DD_HH_MM_SS_SSS = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 14-12-4 下午7:24
     * @param time
     * @return
     */
    public static String fromTimeToStr(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return sdf.format(calendar.getTime());
    }

    /**
     * 2014-12-04 19:24:29
     * @param time
     * @return
     */
    public static String fromTimeToStandardStr(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return sdf.format(calendar.getTime());
    }

    public static String fromTimeToFromatStr(long time, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return sdf.format(calendar.getTime());
    }

    /**
     * MM-DD-HH-mm，表示在MM月DD天HH时mm分
     * @param timeStr
     * @return
     */
    public static long parseTime(String timeStr) {
        String[] timeArr = StringUtils.split(timeStr, "-");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, Integer.parseInt(timeArr[0]) - 1);
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(timeArr[1]));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeArr[2]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(timeArr[3]));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * D-HH-MM，表示在每周第D天HH时MM分
     * @param timeStr
     * @return
     */
    public static long parseWeekTime(String timeStr) {
        String[] timeArr = StringUtils.split(timeStr, "-");
        Calendar calendar = Calendar.getInstance();
        int week = Integer.parseInt(timeArr[0]);
        int currWeek = calendar.get(Calendar.DAY_OF_WEEK);
        calendar.set(Calendar.DAY_OF_WEEK, week);
        if(currWeek > week) {
            calendar.add(Calendar.DAY_OF_WEEK_IN_MONTH, 1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeArr[1]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(timeArr[2]));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取某个时间戳的本周几： 周1 ~ 周日：1 ~ 7
     * @param millisTime
     * @param dayOfWeekNum
     * @return
     */
    public static long getThisWeekNumTime(Long millisTime, int dayOfWeekNum) {
        dayOfWeekNum = (dayOfWeekNum % 7) + 1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millisTime);
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeekNum);
//        calendar.set(Calendar.WEEK_OF_MONTH, dayOfWeekNum);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取某个时间戳的下一个周几： 周1 ~ 周日：1 ~ 7
     * @param millisTime
     * @param dayOfWeekNum
     * @return
     */
    public static long getNextWeekNumTime(Long millisTime, int dayOfWeekNum) {
        dayOfWeekNum = (dayOfWeekNum % 7) + 1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millisTime);
        int currWeek = calendar.get(Calendar.DAY_OF_WEEK);
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeekNum);
        if(currWeek >= dayOfWeekNum) {
            calendar.add(Calendar.WEEK_OF_MONTH, 1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取某个时间戳的上一个周几： 周1 ~ 周日：1 ~ 7
     * @param millisTime
     * @param dayOfWeekNum
     * @return
     */
    public static long getLastWeekNumTime(Long millisTime, int dayOfWeekNum) {
        dayOfWeekNum = (dayOfWeekNum % 7) + 1;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millisTime);
        int currWeek = calendar.get(Calendar.DAY_OF_WEEK);
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeekNum);
        if(currWeek <= dayOfWeekNum) {
            calendar.add(Calendar.WEEK_OF_MONTH, -1);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public static String unixTime2String(long time){
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(time);
    }

    public static long parseTimeInLiuWenStyle(String timeStr){
        return parseFullTimeStr(timeStr, "yyyy-MM-dd-HH-mm");
    }
    
    public static long parseFullTimeStr2(String fullTimeStr, String format) {
    	SimpleDateFormat sdf  =   new SimpleDateFormat(format);
        try {
            Date date = sdf.parse(fullTimeStr);
            return date.getTime();
        } catch (ParseException e) {
        	throw new IllegalArgumentException(e);
        }
    }

    public static long parseFullTimeStr(String fullTimeStr, String format) {
        SimpleDateFormat sdf  =   new SimpleDateFormat(format);
        try {
            Date date = sdf.parse(fullTimeStr);
            return date.getTime();
        } catch (ParseException e) {
            log.error("",e);
            return 0;
        }
    }

    public static long parseFullTimeStr(String fullTimeStr) {
        return parseFullTimeStr(fullTimeStr, "yyyy-MM-dd HH:mm:ss");
    }

    public static long parseTimeForCN(String timeStr){
        long cnTime = parseFullTimeStr(timeStr);
        return cnTime - 8*3600*1000;
    }

    public static long parseTimeForXG2(String timeStr){
        long cnTime = parseFullTimeStr(timeStr, "yyyyMMddHHmmss");
        return cnTime - 8*3600*1000;
    }

    public static long parseTimeForTstore(String timeStr){
        //20150924113656
        long krTime = parseFullTimeStr(timeStr, "yyyyMMddHHmmss");
        return krTime - 9*3600*1000;
    }
    
	public static String parseTimeStrForTstore(long time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		return sdf.format(calendar.getTime());
	}

	/**
	 * 获取下一天的零点
	 */
	public static long getNewDay() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		return  cal.getTimeInMillis();
	}
	
	public static void main(String[] args) {
        log.info(""+getMonthInterval(1464710400000L, 1462032000000L));

		long time = parseFullTimeStr("2017-01-31 10:22:33");
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, 12);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE, 1);
		
		long currentTime = System.currentTimeMillis();
		
		long t = getNextWeekNumTime(currentTime, 6);
		
		if(t - currentTime < 2*24*3600*1000){
			t += 7*24*3600*1000;
		}
        log.info(DateUtils.fromTimeToStandardStr(t));
		
	}
	
	/**
	 * 获取下一天的12点
	 */
	public static long getNewDay12() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.set(Calendar.HOUR_OF_DAY, 12);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DAY_OF_MONTH, 1);
		return  cal.getTimeInMillis();
	}
	
	/**
	 * 获取某时间下一天的0点
	 * @param time
	 * @return
	 */
	public static long getNewDay(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE,1);
		return  cal.getTimeInMillis();
	}
	
	/**
	 * 获取某时间下一天的12点
	 * @param time
	 * @return
	 */
	public static long getNewDay12(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, 12);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE,1);
		return  cal.getTimeInMillis();
	}

	/**
	 * 获取当天零点
	 */
	public static long getToday() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return  cal.getTimeInMillis();
	}
	
	/**
	 * 获取某时间所在那天几点的整点时间
	 * @param hour
	 * @return
	 */
	public static long getTodayByHour(long time, int hour) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return  cal.getTimeInMillis();
	}
	
	//获得本周一0点时间
	public static long getTimesWeeknight(){
		return getTimesWeeknight(System.currentTimeMillis());
	} 
	
	//获得本周日24点时间
	public static long getTimesWeeknight(long millis){
		return getTimesWeekmorning(millis) + (7 * 24 * 60 * 60 * 1000);
	} 
	
	//获得本周一0点时间
	public static long getTimesWeekmorning(){
		return getTimesWeekmorning(System.currentTimeMillis());
	} 
	
	//获得本周一0点时间
	public static long getTimesWeekmorning(long millis){
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(millis);
		cal.set(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0,0);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		return cal.getTimeInMillis();
	} 
	
	//获得本月第一天0点时间
	public static long getTimesMonthmorning(){
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0,0);
		cal.set(Calendar.DAY_OF_MONTH,cal.getActualMinimum(Calendar.DAY_OF_MONTH));
		return cal.getTimeInMillis();
	} 
	
	//获得本月最后一天24点时间
	public static long getTimesMonthnight(){
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0,0);
		cal.set(Calendar.DAY_OF_MONTH,cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		cal.set(Calendar.HOUR_OF_DAY, 24);
		return cal.getTimeInMillis();
	} 

	//获得本年最后一天24点时间
	public static long getTimesYearnight() {
		Calendar cal = Calendar.getInstance();
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 24, 0, 0);
		cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
		return cal.getTimeInMillis();
	}

    /*
    * 获取某天零点
    * */
    public static long getZeroClock(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return  cal.getTimeInMillis();
    }

    public static long getZeroClock(long dateInMillis){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateInMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return  cal.getTimeInMillis();
    }

	/**
	 * 周日 ~ 周六 : 1 ~ 7
	 */
	public static int getDayOfWeek() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		return cal.get(Calendar.DAY_OF_WEEK);
	}

    /**
     * 周日 ~ 周六 : 1 ~ 7
     */
    public static int getDayOfWeek(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return cal.get(Calendar.DAY_OF_WEEK);
    }
    
    /*
    * 计算两个时间戳间隔几天
    * */
    public static int getDayInterval(long time1, long time2){
		long zeroTime1 = getZeroClock(time1);
		long zeroTime2 = getZeroClock(time2);
        long interval = Math.abs(zeroTime2 - zeroTime1);
        int dayInterval = (int)(interval/86400000);
        return dayInterval;
    }

    /**
     * 取得当前日期所在周的第一天
     *
     * @param date
     * @return
     */
    public static Date getFirstDayOfWeek(Date date) {
        if(date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_WEEK,
                calendar.getFirstDayOfWeek()); // Sunday
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 获得指定某个日期那一周的第一天，中国是周一
     *
     * @return
     */
    public static Date getFirstDayOfWeekInChina(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return getFirstDayOfWeekInChina(calendar);
    }

    /**
     * 获得指定某个日期那一周的第一天，中国是周一
     *
     * @return
     */
    public static Date getFirstDayOfWeekInChina(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == 1) {
            calendar.add(Calendar.DATE, -6);
        } else {
            calendar.add(Calendar.DATE, (2 - dayOfWeek));
        }
        return calendar.getTime();
    }

    /**
     * 取得当前日期所在周的最后一天
     *
     * @param date
     * @return
     */
    public static Date getLastDayOfWeek(Date date) {
        if(date == null) {
            date = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        calendar.setTime(date);
        calendar.set(Calendar.DAY_OF_WEEK,
                calendar.getFirstDayOfWeek() + 6); // Saturday
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        return calendar.getTime();
    }

    public static int getHourOfDay(long timeStamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }


    /**
     * 是否是相同的日子（月 和 天 相同）
     *
     * @param time1
     * @param time2
     * @return
     */
    public static boolean isSameDay(long time1, long time2) {
        Calendar dt1 = Calendar.getInstance();
        Calendar dt2 = Calendar.getInstance();
        dt1.setTimeInMillis(time1);
        dt2.setTimeInMillis(time2);
        if (dt1.get(Calendar.MONTH) == dt2.get(Calendar.MONTH) && dt1.get(Calendar.DAY_OF_MONTH) == dt2.get(Calendar.DAY_OF_MONTH)) {
            return true;
        }
        return false;
    }

    /**
     * 判断两个时间戳是否为同一天(年月日都相同)
     * @param time1
     * @param time2
     * @return true if they represent the same day
     * */
    public static boolean isSameYMD(long time1, long time2) {
        return org.apache.commons.lang3.time.DateUtils.isSameDay(new Date(time1), new Date(time2));
//        return org.apache.commons.lang3.time.DateUtils.isSameDay(new Date(time1), new Date(time2));
    }

    /**
     * 判断两个时间戳是否为同一天(年月日都相同)
     * @param date1
     * @param date2
     * @return true if they represent the same day
     * */
    public static boolean isSameYMD(Date date1, Date date2) {
        return org.apache.commons.lang3.time.DateUtils.isSameDay(date1, date2);
//        return org.apache.commons.lang3.time.DateUtils.isSameDay(date1, date2);
    }

    /**
     * 判断两个时间戳是否为同一天(年月日都相同)
     * @param cal1
     * @param cal2
     * @return true if they represent the same day
     * */
    public static boolean isSameYMD(Calendar cal1, Calendar cal2) {
        return org.apache.commons.lang3.time.DateUtils.isSameDay(cal1, cal2);
//        return org.apache.commons.lang3.time.DateUtils.isSameDay(cal1, cal2);
    }

    public static int getDayOfMonth(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return cal.get(Calendar.DAY_OF_MONTH);
    }
    
    
    /**
     * Add By shilei
     * 功能 获取下个月的某天的时间间隔 单位 毫秒
     * @param targetDay
     * @return 
     */
    public static long getSuplusNextMonth(int targetDay){
    	Calendar today = Calendar.getInstance();
        long time1 = today.getTimeInMillis();
        int year = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH)+1;
        int day = today.get(Calendar.DAY_OF_MONTH);
        
        int yearNext = year;
        int monthNext = month + 1;
        // 需要获取明年的年份了
        if (month == 12) {
        	yearNext++;
        	monthNext = 1;
		}
        
        Calendar nextDay = Calendar.getInstance();
        // 因为在这里个函数里月份是从0开始的所以减1
        nextDay.set(yearNext, monthNext - 1, targetDay);
        long time2 = nextDay.getTimeInMillis();
        
        return time2-time1;
    }
    
    // 判断afterMonth个月后有多少天 afterMonth不要超过12
    public static int getDayNumForMonth(int afterMonth){
    	if (afterMonth >= 12) {
			return -1;
		}
    	
    	Calendar today = Calendar.getInstance();
        long time1 = today.getTimeInMillis();
        int year = today.get(Calendar.YEAR);
        int month = today.get(Calendar.MONTH)+1;
        
        int tmpMonth = month + afterMonth;
        int tmpYear = year;
        // 判断是否过年
        if (tmpMonth > 12) {
        	tmpMonth %= 12;
		}
        
        int []a={0,31,29,31,30,31,30,31,31,30,31,30,31};
        int []b={0,31,28,31,30,31,30,31,31,30,31,30,31};
        
        if((tmpYear % 4==0 && tmpYear % 100!=0) || tmpYear % 400 == 0){
        	return a[tmpMonth];
        }
        
        return b[tmpMonth];
    }

    public static String getCurDayStr() {
        SimpleDateFormat sdf  =   new SimpleDateFormat("yyyyMMdd");
        return sdf.format(new Date());
    }

    public static String getFirstDayInWeekStr() {
        SimpleDateFormat sdf  =   new SimpleDateFormat("yyyyMMdd");
        return sdf.format(getFirstDayOfWeek(new Date()));
    }

    public static String getFirstDayInWeekChinaStr() {
        SimpleDateFormat sdf  =   new SimpleDateFormat("yyyyMMdd");
        return sdf.format(getFirstDayOfWeekInChina(new Date()));
    }
    
    /**
     * 距离周几几点还有多久，定时器常用来计算参数delay
     * 如果目标时间点在前则为下周时间
     * 单位：毫秒
     * @param theWeekDay 第几天，周日~周六，1-7
     * @param theHour 几点
     * @return long
     */
	public static long timeToPointOfWeek(int theWeekDay, int theHour) {
		long now = System.currentTimeMillis();
		int weekDay = getDayOfWeek(now);
		long theTime = getTodayByHour(now, theHour);
		Calendar.getInstance();
		int intervalDay = (weekDay > theWeekDay || weekDay == theWeekDay && now > theTime ? 7 + theWeekDay : theWeekDay)
				- 1 - weekDay;
		long intervalTime = DateUtils.getNewDay(now) + theHour * 3600000L - now;
		return intervalDay * 86400000L + intervalTime;
	}
	
	public static int getMonthInterval(long timeMax,long timeMin){
		Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTimeInMillis(timeMax);
        c2.setTimeInMillis(timeMin);
        if(c1.getTimeInMillis() < c2.getTimeInMillis()) return 0;
        int year1 = c1.get(Calendar.YEAR);
        int year2 = c2.get(Calendar.YEAR);
        int month1 = c1.get(Calendar.MONTH);
        int month2 = c2.get(Calendar.MONTH);
        // 获取年的差值 假设 d1 = 2015-8-16  d2 = 2011-9-30
        int yearInterval = year1 - year2;
        // 如果 d1的 月-日 小于 d2的 月-日 那么 yearInterval-- 这样就得到了相差的年数
        if(month1 < month2) yearInterval --;
        // 获取月数差值
        int monthInterval =  (month1 + 12) - month2  ;
        monthInterval %= 12;
        return yearInterval * 12 + monthInterval;
	}
	
	/**
	 * 0点时间
	 * @param time
	 * @return
	 */
//	public static DateTime toZero(DateTime time){
//		return time.hourOfDay().setCopy(0).minuteOfHour().setCopy(0).secondOfMinute().setCopy(0).millisOfSecond().setCopy(0);
//	}
	

	/**
     * 毫秒转秒
     * @param mills
     * @return
     */
    public static int millsToSeconds(long mills) {
        return (int) TimeUnit.MILLISECONDS.toSeconds(mills);
    }
    public static boolean isToday(long time){
        if(time == 0){
            return false;
        }
        return org.apache.commons.lang3.time.DateUtils.isSameDay(new Timestamp(System.currentTimeMillis()),new Timestamp(time));
    }

    public static String getMonthTableName(String oriName) {
        long loginTime = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(loginTime);
        cal.get(Calendar.YEAR);
        cal.get(Calendar.MONTH);
        return new StringBuilder(oriName).append('_').append(cal.get(Calendar.YEAR)).append('_').append(cal.get(Calendar.MONTH)+1).toString();
    }


    //=------------

    /**
     * 获取今天的 hour 时 minute 分 0 秒 0 毫秒 所对应的 Date
     *
     * @param hour
     * @param minute
     * @return
     */
    public static Date getDate(int hour, int minute) {
        LocalTime localTime = LocalTime.of(hour, minute);
        return convertLDTToDate(localTime.atDate(LocalDate.now()));
    }

    //Date转换为LocalDateTime
    public static LocalDateTime convertDateToLDT(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    //LocalDateTime转换为Date
    public static Date convertLDTToDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }


    //获取指定日期的毫秒
    public static Long getMilliByTime(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    //获取指定日期的秒
    public static Long getSecondsByTime(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
    }

    //获取指定时间的指定格式
    public static String formatTime(LocalDateTime time, String pattern) {
        return time.format(DateTimeFormatter.ofPattern(pattern));
    }

    //获取当前时间的指定格式
    public static String formatNow(String pattern) {
        return formatTime(LocalDateTime.now(), pattern);
    }

    public static String format(long millis, String pattern) {
        return formatTime(LocalDateTime.ofInstant(new Date(millis).toInstant(), ZoneId.systemDefault()), pattern);
    }

    //日期加上一个数,根据field不同加不同值,field为ChronoUnit.*
    public static LocalDateTime plus(LocalDateTime time, long number, TemporalUnit field) {
        return time.plus(number, field);
    }

    //日期减去一个数,根据field不同减不同值,field参数为ChronoUnit.*
    public static LocalDateTime minu(LocalDateTime time, long number, TemporalUnit field) {
        return time.minus(number, field);
    }

    /**
     * 获取两个日期的差  field参数为ChronoUnit.*
     *
     * @param startTime
     * @param endTime
     * @param field     单位(年月日时分秒)
     * @return
     */
    public static long betweenTwoTime(LocalDateTime startTime, LocalDateTime endTime, ChronoUnit field) {
        Period period = Period.between(LocalDate.from(startTime), LocalDate.from(endTime));
        if (field == ChronoUnit.YEARS) return period.getYears();
        if (field == ChronoUnit.MONTHS) return period.getYears() * 12 + period.getMonths();
        return field.between(startTime, endTime);
    }

    //获取一天的开始时间，2017,7,22 00:00
    public static LocalDateTime getDayStart(LocalDateTime time) {
        return time.withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    //获取一天的结束时间，2017,7,22 23:59:59.999999999
    public static LocalDateTime getDayEnd(LocalDateTime time) {
        return time.withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999999999);
    }
}
