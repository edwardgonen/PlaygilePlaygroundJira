package com.playgileplayground.jira.jiraissues;

import com.atlassian.jira.issue.Issue;
import com.playgileplayground.jira.impl.ProjectPreparationMisc;
import com.playgileplayground.jira.impl.StatusText;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Ext_EdG on 11/7/2020.
 */
public class ProjectPreparationIssue
{
    public String issueName;
    public String issueKey;
    public String issueTypeName;
    public Date createdDate;
    public Date dueDate;
    public Date businessApprovalDate;
    public String assigneeName;
    public IssueState issueState;
    public ArrayList<ProjectPreparationTask> preparationTasks;

    private ProjectPreparationMisc projectPreparationMisc;
    private Issue jiraIssue;
    private JiraInterface jiraInterface;


    public enum IssueState
    {
        UNDEFINED,
        ACTIVE,
        CLOSED,
        FUTURE
    }
    public ProjectPreparationIssue(Issue jiraIssue,
                                   ProjectPreparationMisc projectPreparationMisc, JiraInterface jiraInterface)
    {
        this.jiraIssue = jiraIssue;
        this.jiraInterface = jiraInterface;
        this.issueKey = jiraIssue.getKey();
        this.issueName = jiraIssue.getSummary();
        this.preparationTasks = new ArrayList<>();
        this.projectPreparationMisc = projectPreparationMisc;
    }

    public boolean instantiateProjectPreparationIssue() {
        boolean result = false;
        //get business approval date
        businessApprovalDate = projectPreparationMisc.getBusinessApprovalDate(jiraIssue);
        if (businessApprovalDate == null)
        {
            StatusText.getInstance().add(true, "No or invalid business approval date for " + issueName);
            return result;
        }

        //get list of tasks for preparation feature
        List<Issue> tasks = jiraInterface.getTasksForPreparationFeature(jiraIssue);
        if (tasks != null && tasks.size() > 0)
        {
            for (Issue task : tasks)
            {
                ProjectPreparationTask projectPreparationTask = new ProjectPreparationTask();
                projectPreparationTask.assignee = projectPreparationMisc.getAssignee(task);
                projectPreparationTask.dueDate = task.getDueDate();
                projectPreparationTask.issueKey = task.getKey();
                projectPreparationTask.issueSummary = task.getSummary();
                if (projectPreparationMisc.isIssueCompleted(task))
                {
                    projectPreparationTask.state = IssueState.CLOSED;
                }
                else
                {
                    projectPreparationTask.state = IssueState.ACTIVE;
                }
                preparationTasks.add(projectPreparationTask);
            }
            result = true;
        }
        else
        {
            StatusText.getInstance().add(true, "No preparation tasks found for " + issueName);
            return result;
        }

        return result;
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
