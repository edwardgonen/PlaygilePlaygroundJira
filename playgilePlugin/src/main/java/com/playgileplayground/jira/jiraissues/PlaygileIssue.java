package com.playgileplayground.jira.jiraissues;

import com.atlassian.jira.issue.Issue;
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;

/**
 * Created by Ext_EdG on 11/19/2020.
 */
//The purpose of this class is purely performance. Instead of doing every time a request to Jira I do that once at beginning and cache
public class PlaygileIssue {

    ProjectMonitoringMisc projectMonitoringMisc;
    JiraInterface jiraInterface;

    /////// publics
    public String issueKey;
    public String issueSummary;
    public boolean bIssueCompleted;
    public boolean bOurIssueType;
    public Issue jiraIssue;
    public double storyPoints;

    public PlaygileIssue(Issue jiraIssue, ProjectMonitoringMisc projectMonitoringMisc, JiraInterface jiraInterface)
    {
        this.jiraIssue = jiraIssue;
        this.projectMonitoringMisc = projectMonitoringMisc;
        this.jiraInterface = jiraInterface;
    }

    public boolean instantiatePlaygileIssue()
    {
        boolean result = false;
        try {
            issueKey = jiraIssue.getKey();
            issueSummary = jiraIssue.getSummary();
            bIssueCompleted = projectMonitoringMisc.isIssueCompleted(jiraIssue);
            bOurIssueType = projectMonitoringMisc.isIssueOneOfOurs(jiraIssue);
            storyPoints = jiraInterface.getStoryPointsForIssue(jiraIssue);
            bIssueCompleted = projectMonitoringMisc.isIssueCompleted(jiraIssue);
            result = true;
        }
        catch (Exception e)
        {
        }

        return result;
    }

}
