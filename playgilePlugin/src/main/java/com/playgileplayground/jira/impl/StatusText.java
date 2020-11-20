package com.playgileplayground.jira.impl;

/**
 * Created by Ext_EdG on 11/20/2020.
 */
public class StatusText {

    private static final StatusText instance = new StatusText();
    StringBuilder statusText = new StringBuilder();

    //private constructor to avoid client applications to use constructor
    private StatusText(){}

    public static StatusText getInstance(){
        return instance;
    }
    public void add(boolean mustPrint, String logEntry)
    {
        if (mustPrint) statusText.append(mustPrint + "<br>");
    }
    public void reset()
    {
        this.statusText = new StringBuilder();
    }
    @Override
    public String toString() {
        return statusText.toString();
    }
}
