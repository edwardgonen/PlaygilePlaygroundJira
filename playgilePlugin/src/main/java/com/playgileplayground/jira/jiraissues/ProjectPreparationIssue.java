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

    final double ALLOWED_TARDINESS_PERCENTAGE = 10.0;
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
            return result;
        }

        Date today = DateTimeUtils.getCurrentDate();
        if (today.after(businessApprovalDate))
        {
            tardiness = TARDINESS_TOO_LATE; //too late...
        }
        else //still have time
        {
            //how much is till end?
            double daysTillDueDate = DateTimeUtils.Days(businessApprovalDate, today);
            double totalDaysForTask = DateTimeUtils.Days(businessApprovalDate, createdDate);
            if ((daysTillDueDate / totalDaysForTask) * 100.0 <= ALLOWED_TARDINESS_PERCENTAGE) //less than 10% - becoming late
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
                        double totalDaysForTask = DateTimeUtils.Days(projectPreparationTask.dueDate, projectPreparationTask.createDate);
                        if ((daysTillDueDate / totalDaysForTask) * 100.0 <= ALLOWED_TARDINESS_PERCENTAGE) //less than 10% - becoming late
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
            return result;
        }

        return result;
    }
    public Date getStartDate()
    {
        Date result;
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
