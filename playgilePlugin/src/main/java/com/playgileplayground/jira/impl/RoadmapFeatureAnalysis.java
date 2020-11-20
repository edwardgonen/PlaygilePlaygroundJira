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
    public String messageToDisplay;
    public boolean bProcessed = false;
    public ArrayList<PlaygileSprint> playgileSprints = new ArrayList<>();
    Collection<PlaygileSprint> artificialTimeWindowsForVelocityCalculation;
    public double[] overallIssuesDistributionInSprint = new double[ProjectMonitor.DISTRIBUTION_SIZE];
    public List<PlaygileIssue> allPlaygileIssues = new ArrayList<>();
    public List<PlaygileIssue> futurePlaygileIssues = new ArrayList<>();
    public double remainingTotalEstimations;
    public double predictedVelocity;
    public ArrayList<Double> interpolatedVelocityPoints;



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

        defaultNotEstimatedIssueValue = getDefaultValueForNonEstimatedIssue();
        //get list of issues and convert them to PlaygileIssues


        List<Issue> issues = jiraInterface.getIssuesForRoadmapFeature(applicationUser, currentProject, roadmapFeature);
        if (null != issues && issues.size() > 0) {

            for (Issue issue : issues) {
                PlaygileIssue playgileIssue = new PlaygileIssue(issue, projectMonitoringMisc, jiraInterface);
                playgileIssue.instantiatePlaygileIssue(defaultNotEstimatedIssueValue);
                //get sprints for issue
                projectMonitoringMisc.addIssueSprintsToList(playgileIssue.jiraIssue, playgileSprints);

                allPlaygileIssues.add(playgileIssue);
                //add to the list of futureIssues
                if (!playgileIssue.bIssueCompleted && playgileIssue.bOurIssueType)
                {
                    futurePlaygileIssues.add(playgileIssue);
                    //adjust to default if needed
                    double estimationForIssue = playgileIssue.getAdjustedEstimationValue();
                    remainingTotalEstimations += estimationForIssue;
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


            //add current estimation to the list of estimations
            //tmpDate = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT).parse("6/23/2020");
            Date timeStamp = DateTimeUtils.getCurrentDate();
            StatusText.getInstance().add(false,"Current time to add to list " + timeStamp);
            ManageActiveObjectsResult maor = mao.AddRemainingEstimationsRecord(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()), timeStamp, remainingTotalEstimations);


            //get real velocities
            //fill real sprint velocity

            //Collection<PlaygileSprint> allRealSprints = projectMonitoringMisc.getAllRealSprintsVelocities(playgileSprints, startDateRoadmapFeature, plannedRoadmapFeatureVelocity, (int)sprintLengthRoadmapFeature, logText);
            artificialTimeWindowsForVelocityCalculation = projectMonitoringMisc.getAllRealSprintsVelocitiesForConstantTimeWindows(allPlaygileIssues, startDateRoadmapFeature, plannedRoadmapFeatureVelocity, (int)sprintLengthRoadmapFeature);

            //linear regression
            interpolatedVelocityPoints = projectMonitoringMisc.getLinearRegressionForRealSprintVelocities(artificialTimeWindowsForVelocityCalculation, startDateRoadmapFeature);

            //averaging
            //interpolatedVelocityPoints = projectMonitoringMisc.getAverageForRealSprintVelocities(allRealSprints, startDate, logText);

            predictedVelocity = (int)Math.round(interpolatedVelocityPoints.get(interpolatedVelocityPoints.size() - 1));
            if (predictedVelocity <= 0) {
                predictedVelocity = plannedRoadmapFeatureVelocity;
                StatusText.getInstance().add(true,"Project velocity is 0, setting to team velocity " + plannedRoadmapFeatureVelocity);
            }

            //now do predictions



            result = true;
        }
        else
        {
            StatusText.getInstance().add(true, "Failed to retrieve any project issues for " + roadmapFeature.getSummary());
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

    private double getDefaultValueForNonEstimatedIssue()
    {
        defaultNotEstimatedIssueValue = 0;
        ManageActiveObjectsResult maor = mao.GetDefaultNotEstimatedIssueValue(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            defaultNotEstimatedIssueValue = (double)maor.Result;
        }
        if (defaultNotEstimatedIssueValue <= 0) defaultNotEstimatedIssueValue = ProjectMonitor.defaultNotEstimatedIssueValueHardCoded;
        return defaultNotEstimatedIssueValue;
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

        getDefaultValueForNonEstimatedIssue();

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
