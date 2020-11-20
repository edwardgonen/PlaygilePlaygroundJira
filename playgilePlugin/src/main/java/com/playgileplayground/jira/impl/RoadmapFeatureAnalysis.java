package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileIssue;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import java.util.*;

/**
 * Created by Ext_EdG on 11/19/2020.
 */
public class RoadmapFeatureAnalysis {
    Issue roadmapFeature;
    JiraInterface jiraInterface;
    ApplicationUser applicationUser;
    Project currentProject;
    ProjectMonitoringMisc projectMonitoringMisc;
    ManageActiveObjects mao;

    boolean bRoadmapFeatureAnalyzed = false;



    /////////// publics
    public StringBuilder logText;
    public String messageToDisplay;
    public boolean bProcessed = false;
    public ArrayList<PlaygileSprint> playgileSprints = new ArrayList<>();
    public double[] overallIssuesDistributionInSprint = new double[ProjectMonitor.DISTRIBUTION_SIZE];
    public List<PlaygileIssue> allPlaygileIssues = new ArrayList<>();
    public List<PlaygileIssue> futurePlaygileIssues = new ArrayList<>();



    public double plannedRoadmapFeatureVelocity = 0;
    public double defaultNotEstimatedIssueValue;
    public Date startDateRoadmapFeature = null;
    public double sprintLengthRoadmapFeature = 0;

    public RoadmapFeatureAnalysis(
        Issue roadmapFeature,
        JiraInterface jiraInterface,
        ApplicationUser applicationUser,
        Project currentProject,
        ProjectMonitoringMisc projectMonitoringMisc,
        ManageActiveObjects mao
    )
    {
        bRoadmapFeatureAnalyzed = false;
        this.roadmapFeature = roadmapFeature;
        this.jiraInterface = jiraInterface;
        this.applicationUser = applicationUser;
        this.currentProject = currentProject;
        this.projectMonitoringMisc = projectMonitoringMisc;
        this.mao = mao;

    }

    public boolean analyzeRoadmapFeature()
    {
        boolean result = false;


        //get list of issues and convert them to PlaygileIssues


        List<Issue> issues = jiraInterface.getIssuesForRoadmapFeature(applicationUser, currentProject, roadmapFeature);
        if (null != issues && issues.size() > 0) {

            for (Issue issue : issues) {
                PlaygileIssue playgileIssue = new PlaygileIssue(issue, projectMonitoringMisc, jiraInterface);
                playgileIssue.instantiatePlaygileIssue();
                //get sprints for issue
                projectMonitoringMisc.addIssueSprintsToList(playgileIssue.jiraIssue, playgileSprints);

                allPlaygileIssues.add(playgileIssue);
                //add to the list of futureIssues
                if (!playgileIssue.bIssueCompleted && playgileIssue.bOurIssueType)
                {
                    futurePlaygileIssues.add(playgileIssue);
                }
            }

            //now we have everything in the cache - list of instantiated issues
            //let's process
            //sort sprints
            Collections.sort(playgileSprints); //sort by dates
            //the first sprint startDate would be the project start date
            PlaygileSprint oldestSprint = playgileSprints.iterator().next(); //first - the oldest

            //we have the sprints, so let's read and set configuration parameters
            //we couldn't read those parameters up to now since we didn't have the sprint list
            //which is mandatory to find the fallback values in case sprint length and RF start are nod configured
            getConfiguredParameters(oldestSprint);



            result = true;
        }
        else
        {
            logText.append("Failed to retrieve any project issues for " + roadmapFeature.getSummary());
            messageToDisplay = "Failed to retrieve any project's issues for Roadmap Feature" +
                ". Please make sure the Roadmap Feature has the right structure (epics, linked epics etc.)";
            result = false;
        }

        bRoadmapFeatureAnalyzed = result; //set to true if analyzed ok
        return result;
    }

    public boolean isRoadmapFeatureStarted()
    {
        //active only if current date is after or equal to start date
        boolean result = false;
        if (bRoadmapFeatureAnalyzed)
        {
            result = DateTimeUtils.CompareZeroBasedDatesOnly(DateTimeUtils.getCurrentDate(), startDateRoadmapFeature) >= 0;
        }
        return result;
    }
    public String getRoadmapFeatureKeyAndSummary()
    {
        return roadmapFeature.getKey() + " " + roadmapFeature.getSummary();
    }

    private void getConfiguredParameters(PlaygileSprint oldestSprint)
    {
        //we read them from DB (Active objects) and provide fallback if not found
        plannedRoadmapFeatureVelocity = 0;
        ManageActiveObjectsResult maor = mao.GetTeamVelocity(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            plannedRoadmapFeatureVelocity = (double)maor.Result;
        }
        if (plannedRoadmapFeatureVelocity <= 0) plannedRoadmapFeatureVelocity = ProjectMonitor.defaultInitialRoadmapFeatureVelocity;

        defaultNotEstimatedIssueValue = 0;
        maor = mao.GetDefaultNotEstimatedIssueValue(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            defaultNotEstimatedIssueValue = (double)maor.Result;
        }
        if (defaultNotEstimatedIssueValue <= 0) defaultNotEstimatedIssueValue = ProjectMonitor.defaultNotEstimatedIssueValueHardCoded;

        startDateRoadmapFeature = null;
        maor = mao.GetProjectStartDate(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            startDateRoadmapFeature = (Date)maor.Result;
        }
        if (startDateRoadmapFeature == null) {
            //try to find from sprints or first issue created
            if (oldestSprint != null) startDateRoadmapFeature = oldestSprint.getStartDate();
        }

        //sprint length
        sprintLengthRoadmapFeature = 0;
        maor = mao.GetSprintLength(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            sprintLengthRoadmapFeature = (double)maor.Result;
        }
        if (sprintLengthRoadmapFeature <= 0) {
            //try to find from sprints
            if (oldestSprint != null) sprintLengthRoadmapFeature = DateTimeUtils.AbsDays(oldestSprint.getStartDate(), oldestSprint.getEndDate()) + 1;
        }
        //round
        //round to one week if needed
        if (sprintLengthRoadmapFeature <= 0) sprintLengthRoadmapFeature = ProjectMonitor.defaultSprintLength;
        else {
            if (sprintLengthRoadmapFeature < 7) sprintLengthRoadmapFeature = 7.0;
            else
                sprintLengthRoadmapFeature = ((int) sprintLengthRoadmapFeature / 7) * 7;
        }

    }

}
