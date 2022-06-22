package com.playgileplayground.jira.projectprogress;

import java.util.Comparator;
import java.util.Date;

public class DateAndValues implements Comparator<DateAndValues>, Comparable<DateAndValues>{
    public Date Date;
    public double Estimation;
    public int TotalIssues;
    public int OpenIssues;
    public int ReadyForEstimationIssues;
    public int ReadyForDevelopmentIssues;

    //default constructor for deserialization purposes
    public DateAndValues() {}
    public DateAndValues(Date date, double estimation) {
        Date = date;
        Estimation = estimation;
    }
    @Override
    public int compareTo(DateAndValues o) {
        return Date.compareTo(o.Date);
    }
    @Override
    public int compare(DateAndValues o1, DateAndValues o2) {
        return o1.Date.compareTo(o2.Date);
    }

}
