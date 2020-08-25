package com.playgileplayground.jira.projectprogress;


import java.util.Comparator;
import java.util.Date;

/**
 * Created by Ext_EdG on 7/9/2020.
 */
public class DataPair implements Comparator<DataPair>, Comparable<DataPair>{
    public Date Date;
    public double RemainingEstimation;

    public DataPair(Date date, double estimation) {
        Date = date;
        RemainingEstimation = estimation;
    }
    @Override
    public int compareTo(DataPair o) {
        return Date.compareTo(o.Date);
    }
    @Override
    public int compare(DataPair o1, DataPair o2) {
        return o1.Date.compareTo(o2.Date);
    }

}
