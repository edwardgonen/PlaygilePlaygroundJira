package com.playgileplayground.jira.impl;

import java.util.Date;

/**
 * Created by Ext_EdG on 11/7/2020.
 */
public class ProductPreparationIssue
{
    public String issueTypeKey;
    public String issueTypeName;
    private Date startDate;
    private Date dueDate;
    public String assigneeName;


    public Date getStartDate()
    {
        Date result = null;
        //if start date is not available return the creation date
        return result;
    }

    public Date getDueDate()
    {
        Date result = null;
        //if due date is not available return the business approval date
        return result;
    }
}
