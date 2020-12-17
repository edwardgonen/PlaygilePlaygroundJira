package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.user.ApplicationUser;
import com.playgileplayground.jira.jiraissues.JiraInterface;

/**
 * Created by on 8/8/2020.
 */
public class ProjectPreparationMisc {
    private JiraInterface jiraInterface;

    public ProjectPreparationMisc(JiraInterface jiraInterface)
    {
        this.jiraInterface = jiraInterface;
    }


    public ProjectPreparationIssue identifyProductPreparationIssue(Issue issue, RoadmapFeatureDescriptor roadmapFeatureDescriptor)
    {
        ProjectPreparationIssue result = new ProjectPreparationIssue();

        if (issue == null) return null; //wrong issue

        //1. get issue key
        String issueKey = issue.getKey();

        //Monetization(PKM), BI(BIT), BA(PKBA), Economics (PKEC)

        if (issueKey.contains("PKM")) {
            result.issueTypeKey = "PKM";
            result.issueTypeName = "Monetization";
        } else
        if (issueKey.contains("BIT")) {
            result.issueTypeKey = "BIT";
            result.issueTypeName = "Business Intelligence";
        } else
        if (issueKey.contains("PKBA")) {
            result.issueTypeKey = "PKBA";
            result.issueTypeName = "Business Analytics";
        } else
        if (issueKey.contains("PKEC")) {
            result.issueTypeKey = "PKEC";
            result.issueTypeName = "Economy";
        }
        else //unknown
        {
            return null;
        }

        //get assignee
        ApplicationUser assignee = issue.getAssignee();
        if (assignee != null) {
            result.assigneeName = issue.getAssignee().getName();
        } else {
            result.assigneeName = "Unassigned";
        }

        //get start date
        result.createdDate = issue.getCreated();
        //get due date
        result.dueDate = issue.getDueDate();
        if (result.dueDate == null) //not defined so I use the business approval date as due date
        {
            result.dueDate = roadmapFeatureDescriptor.BusinessApprovalDate;
        }
        //get status
        Status issueStatus = issue.getStatus();
        if (issueStatus != null) {
            String statusName = issueStatus.getName();
            switch (statusName)
            {
                case "Resolved":
                case "Closed":
                case "Done":
                    result.issueState = ProjectPreparationIssue.IssueState.CLOSED;
                    break;
                default: result.issueState = ProjectPreparationIssue.IssueState.ACTIVE;
            }

        }
        else result.issueState = ProjectPreparationIssue.IssueState.UNDEFINED;

        result.issueKey = issue.getKey();
        result.issueName = issue.getSummary();
        return result;
    }
}


