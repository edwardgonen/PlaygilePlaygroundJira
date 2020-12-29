package com.playgileplayground.jira.jiraissues;

import com.atlassian.jira.issue.Issue;
import com.playgileplayground.jira.impl.DateTimeUtils;
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
    public String issueSummary;
    public String issueKey;
    public String issueTypeName;
    public double tardiness;
    public Date createdDate;
    public Date dueDate;
    public Date businessApprovalDate;
    public String assigneeName;
    public IssueState issueState;
    public ArrayList<ProjectPreparationTask> preparationTasks;

    private ProjectPreparationMisc projectPreparationMisc;
    private Issue jiraIssue;
    private JiraInterface jiraInterface;

    final double ALLOWED_TARDINESS_DAYS = 2.0;
    final double TARDINESS_OK = 1.0;
    final double TARDINESS_GETTING_LATE = 2.0;
    final double TARDINESS_TOO_LATE = 3.0;

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
        this.issueSummary = jiraIssue.getSummary();
        this.createdDate = jiraIssue.getCreated();
        this.preparationTasks = new ArrayList<>();
        this.projectPreparationMisc = projectPreparationMisc;
        this.tardiness = TARDINESS_OK; //ok
    }

    public boolean instantiateProjectPreparationIssue() {
        boolean result = false;
        //get business approval date
        businessApprovalDate = projectPreparationMisc.getBusinessApprovalDate(jiraIssue);
        if (businessApprovalDate == null)
        {
            StatusText.getInstance().add(true, "No or invalid business approval date for " + issueSummary);
            return false;
        }

        Date today = DateTimeUtils.getZeroTimeDate(DateTimeUtils.getCurrentDate());
        if (today.after(businessApprovalDate))
        {
            tardiness = TARDINESS_TOO_LATE; //too late...
        }
        else //still have time
        {
            //how much is till end?
            double daysTillBusinessApprovalDate = DateTimeUtils.Days(businessApprovalDate, today);
            if (daysTillBusinessApprovalDate <= ALLOWED_TARDINESS_DAYS) //less than 2 days  - becoming late
            {
                tardiness = TARDINESS_GETTING_LATE;
            }
            else
            {
                tardiness = TARDINESS_OK;
            }
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
                if (projectPreparationTask.dueDate == null) //not defined
                {
                    //set it to business approval date
                    projectPreparationTask.dueDate = businessApprovalDate;
                }
                projectPreparationTask.createDate = task.getCreated();
                projectPreparationTask.issueKey = task.getKey();
                projectPreparationTask.issueSummary = task.getSummary();
                if (projectPreparationMisc.isIssueCompleted(task))
                {
                    projectPreparationTask.state = IssueState.CLOSED;
                    projectPreparationTask.tardiness = TARDINESS_OK; //ok
                }
                else
                {
                    projectPreparationTask.state = IssueState.ACTIVE;
                    //calculate tardiness
                    //1. is it beyond due date?
                    if (today.after(projectPreparationTask.dueDate))
                    {
                        projectPreparationTask.tardiness = TARDINESS_TOO_LATE; //too late...
                    }
                    else //still have time
                    {
                        //how much is till end?
                        double daysTillDueDate = DateTimeUtils.Days(projectPreparationTask.dueDate, today);
                        if (daysTillDueDate <= ALLOWED_TARDINESS_DAYS) //less than 2 - becoming late
                        {
                            projectPreparationTask.tardiness = TARDINESS_GETTING_LATE;
                        }
                        else
                        {
                            projectPreparationTask.tardiness = TARDINESS_OK;
                        }
                    }
                }
                //if a specific task tardiness is worse than issue tardiness - set issue tardiness to the worst
                tardiness = Math.max(projectPreparationTask.tardiness,tardiness);
                preparationTasks.add(projectPreparationTask);
            }
            result = true;
        }
        else
        {
            StatusText.getInstance().add(true, "No preparation tasks found for " + issueSummary);
            return false;
        }

        return true;
    }
}
