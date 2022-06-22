package com.playgileplayground.jira.jiraissues;

import com.atlassian.jira.issue.Issue;
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;
import com.playgileplayground.jira.impl.StatusText;

import java.util.Date;

//The purpose of this class is purely performance. Instead of doing every time a request to Jira I do that once at beginning and cache
public class PlaygileIssue {

    ProjectMonitoringMisc projectMonitoringMisc;
    JiraInterface jiraInterface;
    double defaultNotEstimatedIssueValue;

    /////// publics
    public String issueKey;
    public String issueSummary;
    public String issueStatus;
    public boolean bIssueCompleted;
    public boolean bOurIssueType;
    public boolean bIssueOpen;
    public boolean bIssueReadyForEstimation;
    public boolean bIssueReadyForDevelopment;
    public Issue jiraIssue;
    public double storyPoints;
    public Date resolutionDate;

    public PlaygileIssue(Issue jiraIssue,
                         ProjectMonitoringMisc projectMonitoringMisc,
                         JiraInterface jiraInterface) {
        this.jiraIssue = jiraIssue;
        this.projectMonitoringMisc = projectMonitoringMisc;
        this.jiraInterface = jiraInterface;
    }

    public boolean instantiatePlaygileIssue(double defaultNotEstimatedIssueValue, Issue roadmapFeature) {
        boolean result = false;
        try {
            issueKey = jiraIssue.getKey();
            issueSummary = jiraIssue.getSummary();
            issueStatus = projectMonitoringMisc.getIssueStatus(jiraIssue);
            bIssueCompleted = projectMonitoringMisc.isIssueCompleted(jiraIssue);
            bOurIssueType = projectMonitoringMisc.isIssueOneOfOurs(jiraIssue, roadmapFeature);
            bIssueOpen = projectMonitoringMisc.isIssueOpen(jiraIssue);
            storyPoints = jiraInterface.getStoryPointsForIssue(jiraIssue);
            bIssueCompleted = projectMonitoringMisc.isIssueCompleted(jiraIssue);
            bIssueReadyForDevelopment = projectMonitoringMisc.isIssueReadyForDevelopment(jiraIssue);
            bIssueReadyForEstimation = projectMonitoringMisc.isIssueReadyForEstimation(jiraIssue);
            resolutionDate = jiraIssue.getResolutionDate();
            this.defaultNotEstimatedIssueValue = defaultNotEstimatedIssueValue;
            result = true;
        } catch (Exception e) {
            StatusText.getInstance().add(true, "Error instantiating issue " + issueKey + " " + issueSummary + " Exception " + projectMonitoringMisc.getExceptionTrace(e));
        }

        return result;
    }

    public double getAdjustedEstimationValue() {
        double result = storyPoints;
        if (storyPoints <= 0) {
            result = defaultNotEstimatedIssueValue;
        }
        return result;
    }

}
