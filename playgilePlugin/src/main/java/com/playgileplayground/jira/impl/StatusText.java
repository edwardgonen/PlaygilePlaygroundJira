package com.playgileplayground.jira.impl;

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
        if (mustPrint) statusText.append(logEntry).append("<br>");
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
