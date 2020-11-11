package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.status.category.StatusCategory;

import java.util.Date;

/**
 * Created by Ext_EdG on 11/7/2020.
 */
public class ProjectPreparationIssue
{
    public String issueName;
    public String issueKey;
    public String issueTypeKey;
    public String issueTypeName;
    public Date createdDate;
    public Date dueDate;
    public Date businessApprovalDate;
    public String assigneeName;
    public IssueState issueState;

    public enum IssueState
    {
        UNDEFINED,
        ACTIVE,
        CLOSED,
        FUTURE
    }

    public Date getStartDate()
    {
        Date result = null;
        //if start date is not available return the creation date
        result = createdDate;
        return result;
    }

    public Date getDueDate()
    {
        Date result = dueDate;
        //if due date is not available return the business approval date
        if (result == null) result = businessApprovalDate;
        return result;
    }
}
