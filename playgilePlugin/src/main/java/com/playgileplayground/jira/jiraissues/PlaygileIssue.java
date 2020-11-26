package com.playgileplayground.jira.jiraissues;

import com.atlassian.jira.issue.Issue;
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;
import com.playgileplayground.jira.impl.StatusText;

import java.util.Date;

/**
 * Created by Ext_EdG on 11/19/2020.
 */
//The purpose of this class is purely performance. Instead of doing every time a request to Jira I do that once at beginning and cache
public class PlaygileIssue {

    ProjectMonitoringMisc projectMonitoringMisc;
    JiraInterface jiraInterface;
    double defaultNotEstimatedIssueValue;

    /////// publics
    public String issueKey;
    public String issueSummary;
    public boolean bIssueCompleted;
    public boolean bOurIssueType;
    public Issue jiraIssue;
    public double storyPoints;
    public Date resolutionDate;

    public PlaygileIssue(Issue jiraIssue,
                         ProjectMonitoringMisc projectMonitoringMisc,
                         JiraInterface jiraInterface)
    {
        this.jiraIssue = jiraIssue;
        this.projectMonitoringMisc = projectMonitoringMisc;
        this.jiraInterface = jiraInterface;
    }

    public boolean instantiatePlaygileIssue(double defaultNotEstimatedIssueValue)
    {
        boolean result = false;
        try {
            issueKey = jiraIssue.getKey();
            issueSummary = jiraIssue.getSummary();
            bIssueCompleted = projectMonitoringMisc.isIssueCompleted(jiraIssue);
            bOurIssueType = projectMonitoringMisc.isIssueOneOfOurs(jiraIssue);
            storyPoints = jiraInterface.getStoryPointsForIssue(jiraIssue);
            bIssueCompleted = projectMonitoringMisc.isIssueCompleted(jiraIssue);
            resolutionDate = jiraIssue.getResolutionDate();
            this.defaultNotEstimatedIssueValue = defaultNotEstimatedIssueValue;
            result = true;
        }
        catch (Exception e)
        {
            StatusText.getInstance().add(true, "Error instantiating issue " + issueKey + " " + issueSummary + " Exception " + e);
        }

        return result;
    }
    public double getAdjustedEstimationValue()
    {
        double result = storyPoints;
        if (storyPoints <= 0) result = defaultNotEstimatedIssueValue;
        return  result;
    }

}