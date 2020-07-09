package com.playgileplayground.jira.impl;


import java.util.Date;

/**
 * Created by Ext_EdG on 7/9/2020.
 */
public class DataPair {
    public Date Date;
    public double RemainingEstimation;

    public DataPair(Date date, double estimation) {
        Date = date;
        RemainingEstimation = estimation;
    }
}
