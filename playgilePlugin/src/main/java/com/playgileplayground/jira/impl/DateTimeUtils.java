package com.playgileplayground.jira.impl;

import com.playgileplayground.jira.persistence.ManageActiveObjects;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ext_EdG on 11/17/2020.
 */
public class DateTimeUtils {
    public static String ConvertDateToOurFormat(Date dateToConvert)
    {
        SimpleDateFormat outputDateFormat = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT);
        return outputDateFormat.format(dateToConvert);
    }

    public static boolean CheckIfDateIsInsideDateSegmentInclusive(Date dateInQuestion, Date startSegment, Date endSegment)
    {
        Calendar calendar = Calendar.getInstance();
        Date firstAdjustedToZeroDate = getZeroTimeDate(startSegment);

        calendar.setTime(endSegment);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date secondAdjustedToEODDate = calendar.getTime();

        return (dateInQuestion.after(firstAdjustedToZeroDate) || dateInQuestion.equals(firstAdjustedToZeroDate))&&
            (dateInQuestion.before(secondAdjustedToEODDate) || dateInQuestion.equals(secondAdjustedToEODDate));
    }

    public static int CompareZeroBasedDatesOnly(Date firstDate, Date secondDate)
    {
        return (getZeroTimeDate(firstDate).compareTo(getZeroTimeDate(secondDate)));
    }

    public static Date getZeroTimeDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        date = calendar.getTime();
        return date;
    }


    public static Date getCurrentDate()
    {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0); // same for minutes and seconds
        return today.getTime();
    }

    public static Date AddDays(Date date, int addDays)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, addDays);
        return c.getTime();
    }

    public static Date AddMonths(Date date, int addMonths)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MONTH, addMonths);
        return c.getTime();
    }

    public static int AbsDays(Date secondDate, Date firstDate)
    {
        long diffInMillies = Math.abs(secondDate.getTime() - firstDate.getTime());
        return (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    public static boolean CompareDatesByMonthYear(Date firstDate, Date secondDate)
    {
        Calendar cFirst = Calendar.getInstance();
        cFirst.setTime(firstDate);
        Calendar cSecond = Calendar.getInstance();
        cSecond.setTime(secondDate);
        return (cFirst.get(Calendar.MONTH) == cSecond.get(Calendar.MONTH)) && (cFirst.get(Calendar.YEAR) == cSecond.get(Calendar.YEAR));
    }

    public static int Days(Date secondDate, Date firstDate)
    {
        int sign = 1;
        long diffInMillies = secondDate.getTime() - firstDate.getTime();
        if (diffInMillies >= 0) sign = 1;
        else sign = -1;
        return sign * (int)TimeUnit.DAYS.convert(Math.abs(diffInMillies), TimeUnit.MILLISECONDS);
    }
}
