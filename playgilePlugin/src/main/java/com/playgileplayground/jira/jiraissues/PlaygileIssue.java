package com.playgileplayground.jira.jiraissues;

import com.atlassian.jira.issue.Issue;
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;

/**
 * Created by Ext_EdG on 11/19/2020.
 */
//The purpose of this class is purely performance. Instead of doing every time a request to Jira I do that once at beginning and cache
public class PlaygileIssue {
    Issue jiraIssue;
    ProjectMonitoringMisc projectMonitoringMisc;

    /////// publics
    public String issueKey;
    public String issueSummary;
    public boolean bIssueCompleted;

    public PlaygileIssue(Issue jiraIssue, ProjectMonitoringMisc projectMonitoringMisc)
    {
        this.jiraIssue = jiraIssue;
        this.projectMonitoringMisc = projectMonitoringMisc;
    }

    public boolean instantiatePlaygileIssue()
    {
        boolean result = false;
        issueKey = jiraIssue.getKey();
        issueSummary = jiraIssue.getSummary();
        bIssueCompleted = projectMonitoringMisc.isIssueCompleted(jiraIssue);



        return result;
    }

}
