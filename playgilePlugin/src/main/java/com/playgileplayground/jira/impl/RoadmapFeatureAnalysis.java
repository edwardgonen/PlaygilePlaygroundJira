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
import com.playgileplayground.jira.projectprogress.DataPair;
import com.playgileplayground.jira.projectprogress.ProgressData;
import com.playgileplayground.jira.projectprogress.ProjectProgress;
import com.playgileplayground.jira.projectprogress.ProjectProgressResult;

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
    public ArrayList<PlaygileSprint> playgileSprints = new ArrayList<>();
    Collection<PlaygileSprint> artificialTimeWindowsForVelocityCalculation;
    public double[] overallIssuesDistributionInSprint = new double[ProjectMonitor.DISTRIBUTION_SIZE];
    public List<PlaygileIssue> allPlaygileIssues = new ArrayList<>();
    public List<PlaygileIssue> futurePlaygileIssues = new ArrayList<>();
    public double remainingTotalEstimations;
    public double predictedVelocity;
    public ArrayList<Double> interpolatedVelocityPoints;
    public ProjectProgressResult projectProgressResult;
    public AnalyzedStories analyzedStories = new AnalyzedStories();



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
        StatusText.getInstance().add(true, "Start analysis for " + roadmapFeature.getKey() + " " + roadmapFeature.getSummary());

        List<Issue> issues = jiraInterface.getIssuesForRoadmapFeature(applicationUser, currentProject, roadmapFeature);
        if (null != issues && issues.size() > 0) {
            for (Issue issue : issues) {
                PlaygileIssue playgileIssue = new PlaygileIssue(issue, projectMonitoringMisc, jiraInterface);

                //======================================================
                //for performance and cache instantiate our PlaygileIssue
                //======================================================
                playgileIssue.instantiatePlaygileIssue(defaultNotEstimatedIssueValue);

                //======================================================
                //            get list of sprint for issue
                //======================================================
                //get sprints for issue
                projectMonitoringMisc.addIssueSprintsToList(playgileIssue.jiraIssue, playgileSprints);

                allPlaygileIssues.add(playgileIssue);

                //if issue is not completed yet, add to the list of futureIssues
                if (!playgileIssue.bIssueCompleted && playgileIssue.bOurIssueType) {
                    futurePlaygileIssues.add(playgileIssue);
                    //adjust to default if needed
                    double estimationForIssue = playgileIssue.getAdjustedEstimationValue();
                    remainingTotalEstimations += estimationForIssue;

                    if (playgileIssue.storyPoints > 0 && playgileIssue.storyPoints < 13) analyzedStories.EstimatedStoriesNumber++;
                    if (playgileIssue.storyPoints == 13) analyzedStories.LargeStoriesNumber++;
                    if (playgileIssue.storyPoints > 13) analyzedStories.VeryLargeStoriesNumber++;
                    if (playgileIssue.storyPoints <= 0) analyzedStories.NotEstimatedStoriesNumber++;


                }
            }

            //now we have everything in the cache - list of instantiated issues
            //let's process
            //sort sprints
            Collections.sort(playgileSprints); //sort by dates
            //========================================================
            // calculate average issues closure distribution across all closed sprints
            //========================================================
            for (PlaygileSprint notFutureSprint : playgileSprints)
            {
                double[] sprintIssuesDistribution = notFutureSprint.getIssuesTimeDistribution();
                //update main counters
                for (int i = 0; i < overallIssuesDistributionInSprint.length; i++) {
                    overallIssuesDistributionInSprint[i] += sprintIssuesDistribution[i];
                }
            }
            //now average it across all sprints
            if (playgileSprints.size() > 0) {
                for (int i = 0; i < overallIssuesDistributionInSprint.length; i++) {
                    overallIssuesDistributionInSprint[i] /= (double) playgileSprints.size();
                }
            }


            //the first sprint startDate would be the project start date.
            //BUT!!! there may be no sprints at all
            PlaygileSprint oldestSprint = null;
            if (playgileSprints != null && playgileSprints.size() > 0)  oldestSprint = playgileSprints.iterator().next(); //first - the oldest

            //we have the sprints, so let's read and set configuration parameters
            //we couldn't read those parameters up to now since we didn't have the sprint list
            //which is mandatory to find the fallback values in case sprint length and RF start are nod configured
            getConfiguredParameters(oldestSprint);


            //add current estimation to the list of estimations
            //tmpDate = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT).parse("6/23/2020");
            Date timeStamp = DateTimeUtils.getCurrentDate();
            StatusText.getInstance().add(false, "Current time to add to list " + timeStamp);
            ManageActiveObjectsResult maor = mao.AddRemainingEstimationsRecord(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()), timeStamp, remainingTotalEstimations);


            //get real velocities
            //fill real sprint velocity

            //Collection<PlaygileSprint> allRealSprints = projectMonitoringMisc.getAllRealSprintsVelocities(playgileSprints, startDateRoadmapFeature, plannedRoadmapFeatureVelocity, (int)sprintLengthRoadmapFeature, logText);
            artificialTimeWindowsForVelocityCalculation = projectMonitoringMisc.getAllRealSprintsVelocitiesForConstantTimeWindows(allPlaygileIssues, startDateRoadmapFeature, plannedRoadmapFeatureVelocity, (int) sprintLengthRoadmapFeature);

            //linear regression
            interpolatedVelocityPoints = projectMonitoringMisc.getLinearRegressionForRealSprintVelocities(artificialTimeWindowsForVelocityCalculation, startDateRoadmapFeature);
            //averaging
            //interpolatedVelocityPoints = projectMonitoringMisc.getAverageForRealSprintVelocities(allRealSprints, startDate, logText);

            //if previous call wat not able to calculate interpolations, the output will be empty
            predictedVelocity = 0;
            if (interpolatedVelocityPoints.size() > 0) {
                predictedVelocity = (int) Math.round(interpolatedVelocityPoints.get(interpolatedVelocityPoints.size() - 1));
            }
            if (predictedVelocity <= 0) {
                predictedVelocity = plannedRoadmapFeatureVelocity;
                StatusText.getInstance().add(true, "Project velocity is 0, setting to team velocity " + plannedRoadmapFeatureVelocity);
            }

            //now do predictions
            ArrayList<DataPair> historicalEstimationPairs = getHistoricalEstimations();
            if (historicalEstimationPairs != null)
            {
                ProjectProgress projectProgress = new ProjectProgress();
                projectProgressResult = projectProgress.Initiate(plannedRoadmapFeatureVelocity,
                    predictedVelocity,
                    (int)sprintLengthRoadmapFeature,
                    historicalEstimationPairs);
            }
            else
            {
                StatusText.getInstance().add(true, "Failed to get historical estimations");
            }

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

    private ArrayList<DataPair> getHistoricalEstimations()
    {
        ArrayList<DataPair> result = null;
        ManageActiveObjectsResult maor = mao.GetProgressDataList(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            result = (ArrayList<DataPair>)maor.Result;
        }
        return result;
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
        ManageActiveObjectsResult maor = mao.GetPlannedRoadmapVelocity(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            plannedRoadmapFeatureVelocity = (double)maor.Result;
        }
        if (plannedRoadmapFeatureVelocity <= 0) plannedRoadmapFeatureVelocity = ProjectMonitor.defaultInitialRoadmapFeatureVelocity;

        getDefaultValueForNonEstimatedIssue();

        startDateRoadmapFeature = null;
        maor = mao.GetProjectStartDate(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            startDateRoadmapFeature = (Date)maor.Result;
            StatusText.getInstance().add(true, "Start feature date from DB is " + startDateRoadmapFeature);
        }
        if (startDateRoadmapFeature == null) {
            //try to find from sprints or first issue created
            if (oldestSprint != null) {
                startDateRoadmapFeature = oldestSprint.getStartDate();
                StatusText.getInstance().add(true, "Start feature date from oldest sprint is " + startDateRoadmapFeature);
            }
            else //set today as latest fall back
            {
                startDateRoadmapFeature = DateTimeUtils.getCurrentDate();
                StatusText.getInstance().add(true, "Start feature date is set for today as fallback " + startDateRoadmapFeature);
            }
        }
        StatusText.getInstance().add(true, "Detected start date is " + startDateRoadmapFeature);

        //sprint length
        sprintLengthRoadmapFeature = 0;
        maor = mao.GetSprintLength(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            sprintLengthRoadmapFeature = (double)maor.Result;
        }
        if (sprintLengthRoadmapFeature <= 0) {
            //try to find from sprints
            if (oldestSprint != null) sprintLengthRoadmapFeature = DateTimeUtils.AbsDays(oldestSprint.getStartDate(), oldestSprint.getEndDate());
        }
        StatusText.getInstance().add(true, "Detected sprint length is " + sprintLengthRoadmapFeature);

        //round to one week if needed
        if (sprintLengthRoadmapFeature <= 0) sprintLengthRoadmapFeature = ProjectMonitor.defaultSprintLength;
        else if (sprintLengthRoadmapFeature < 7) sprintLengthRoadmapFeature = 7;

        /*
        else {
            int remainder = ((int) sprintLengthRoadmapFeature % 7);
            sprintLengthRoadmapFeature = ((int) sprintLengthRoadmapFeature / 7) * 7;
            if (remainder > 0) sprintLengthRoadmapFeature += 7;
        }
        */
        StatusText.getInstance().add(true, "Detected sprint length is rounded to " + sprintLengthRoadmapFeature);

    }

    public void prepareDateForWeb(Map<String, Object> contextMap)
    {
        contextMap.put(ProjectMonitor.TEAMVELOCITY, plannedRoadmapFeatureVelocity);
        //================================================================================
        StringBuilder issuesDistributionString = new StringBuilder();
        if (playgileSprints.size() > 0) {
            for (int i = 0; i < overallIssuesDistributionInSprint.length; i++) {
                double roundTo2Digits = 100.0 * Math.round(overallIssuesDistributionInSprint[i] * 100.0) / 100.0;
                issuesDistributionString.append(roundTo2Digits + ManageActiveObjects.PAIR_SEPARATOR);
            }
        }
        contextMap.put(ProjectMonitor.ISSUESDISTRIBUTION, issuesDistributionString.toString());
        //========================================================================================
        //convert to strings
        StringBuilder resultRows = new StringBuilder();
        int index = 0;
        for (PlaygileSprint sprintToConvert : artificialTimeWindowsForVelocityCalculation)
        {
            resultRows.append(
                DateTimeUtils.ConvertDateToOurFormat(sprintToConvert.getEndDate()) + ManageActiveObjects.PAIR_SEPARATOR +
                    sprintToConvert.sprintVelocity  + ManageActiveObjects.PAIR_SEPARATOR +
                    interpolatedVelocityPoints.get(index++) + ManageActiveObjects.LINE_SEPARATOR
            );
        }
        contextMap.put(ProjectMonitor.REALVELOCITIES, resultRows);
        //============================================================================================
        contextMap.put(ProjectMonitor.AVERAGEREALVELOCITY, (int)Math.round(predictedVelocity));
        //==============================================================================================
        //what is the longest array?
        StringBuilder chartRows = new StringBuilder();
        ProgressData longestList;
        ProgressData shortestList;
        boolean predictedIsLongest;
        if (projectProgressResult.progressData.Length() >= projectProgressResult.idealData.Length()) {
            longestList = projectProgressResult.progressData;
            shortestList = projectProgressResult.idealData;
            predictedIsLongest = true;
        }
        else
        {
            longestList = projectProgressResult.idealData;
            shortestList = projectProgressResult.progressData;
            predictedIsLongest = false;
        }
        DataPair tmpPredictedDataPair, tmpIdealDataPair;
        for (int i = 0; i < longestList.Length(); i++)
        {
            if (predictedIsLongest) {
                tmpPredictedDataPair = longestList.GetElementAtIndex(i);
                tmpIdealDataPair = shortestList.GetElementAtIndex(i);
            }
            else
            {
                tmpPredictedDataPair = shortestList.GetElementAtIndex(i);
                tmpIdealDataPair = longestList.GetElementAtIndex(i);
            }
            if (i >= shortestList.Length()) //no more elements in shortest
            {
                if (predictedIsLongest) {
                    chartRows.append(
                        DateTimeUtils.ConvertDateToOurFormat(tmpPredictedDataPair.Date) + ManageActiveObjects.PAIR_SEPARATOR +
                            "" + ManageActiveObjects.PAIR_SEPARATOR +
                            tmpPredictedDataPair.RemainingEstimation + ManageActiveObjects.LINE_SEPARATOR
                    );
                }
                else
                {
                    chartRows.append(
                        DateTimeUtils.ConvertDateToOurFormat(tmpIdealDataPair.Date) + ManageActiveObjects.PAIR_SEPARATOR +
                            tmpIdealDataPair.RemainingEstimation + ManageActiveObjects.PAIR_SEPARATOR +
                            "" + ManageActiveObjects.LINE_SEPARATOR
                    );
                }
            }
            else //both records available
            {
                chartRows.append(
                    DateTimeUtils.ConvertDateToOurFormat(tmpPredictedDataPair.Date) + ManageActiveObjects.PAIR_SEPARATOR +
                        tmpIdealDataPair.RemainingEstimation + ManageActiveObjects.PAIR_SEPARATOR +
                        tmpPredictedDataPair.RemainingEstimation + ManageActiveObjects.LINE_SEPARATOR
                );
            }
        }
        contextMap.put(ProjectMonitor.CHARTROWS, chartRows.toString());
        contextMap.put(ProjectMonitor.IDEALENDOFPROJECT, DateTimeUtils.ConvertDateToOurFormat(projectProgressResult.idealProjectEnd));
        //make the logic of color
        contextMap.put(ProjectMonitor.PREDICTIONCOLOR, ProjectProgress.convertColorToHexadeimal(projectProgressResult.progressDataColor));
        contextMap.put(ProjectMonitor.PREDICTEDENDOFPROJECT, DateTimeUtils.ConvertDateToOurFormat(projectProgressResult.predictedProjectEnd));

        //=========================================================================================================================
        contextMap.put(ProjectMonitor.NOTESTIMATEDSTORIES, analyzedStories.NotEstimatedStoriesNumber);
        contextMap.put(ProjectMonitor.LARGESTORIES, analyzedStories.LargeStoriesNumber);
        contextMap.put(ProjectMonitor.VERYLARGESTORIES, analyzedStories.VeryLargeStoriesNumber);
        contextMap.put(ProjectMonitor.ESTIMATEDSTORIES, analyzedStories.EstimatedStoriesNumber);
    }

}
