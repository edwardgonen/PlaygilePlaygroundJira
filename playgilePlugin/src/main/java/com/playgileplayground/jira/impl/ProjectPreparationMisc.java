package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.JiraQueryResult;
import com.playgileplayground.jira.jiraissues.ProjectPreparationIssue;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Created by on 8/8/2020.
 */
public class ProjectPreparationMisc {
    private JiraInterface jiraInterface;
    public enum IssueTardiness {
        OK,
        LATE,
        TOO_LATE
    }
    public ProjectPreparationMisc(JiraInterface jiraInterface)
    {
        this.jiraInterface = jiraInterface;
    }

    public Date getBusinessApprovalDate(Issue roadmapFeature)
    {
        Date result = null;

        JiraQueryResult jqr;
        //get business approval date
        jqr = jiraInterface.getBusinessApprovalDateForIssue(roadmapFeature);
        if (jqr.Code == JiraQueryResult.STATUS_CODE_SUCCESS)
        {
            result = (Date)jqr.Result;
        }
        return result;
    }
    public boolean isIssueCompleted(Issue issue)
    {
        Status issueStatus = issue.getStatus();
        StatusCategory statusCategory = issueStatus.getStatusCategory();
        return (Objects.equals(statusCategory.getKey(), StatusCategory.COMPLETE));
    }
    public String getAssignee(Issue issue)
    {
        String result = "No assignee";
        ApplicationUser assignee = issue.getAssignee();
        if (assignee != null) {
            result = assignee.getDisplayName();
        }
        return result;
    }
    public ProjectPreparationIssue identifyProductPreparationIssue(Issue issue, RoadmapFeatureDescriptor roadmapFeatureDescriptor)
    {
        ProjectPreparationIssue result = null;
        return result;
    }
}


