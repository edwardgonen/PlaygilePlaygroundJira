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
public class RoadmapFeatureAnalysis implements Comparator<RoadmapFeatureAnalysis>, Comparable<RoadmapFeatureAnalysis> {
    Issue roadmapFeature;
    JiraInterface jiraInterface;
    ApplicationUser applicationUser;
    Project currentProject;
    ProjectMonitoringMisc projectMonitoringMisc;
    ManageActiveObjects mao;

    boolean bRoadmapFeatureAnalyzed = false;



    /////////// publics
    public String featureSummary;
    public String featureKey;
    public String projectKey;
    public String messageToDisplay;
    public ArrayList<PlaygileSprint> playgileSprints = new ArrayList<>();
    public Collection<PlaygileSprint> artificialTimeWindowsForVelocityCalculation;
    public double[] overallIssuesDistributionInSprint = new double[ProjectMonitor.DISTRIBUTION_SIZE];
    public List<PlaygileIssue> allPlaygileIssues = new ArrayList<>();
    public List<PlaygileIssue> futurePlaygileIssues = new ArrayList<>();
    public double remainingTotalEstimations;
    public double predictedVelocity;
    public ArrayList<Double> interpolatedVelocityPoints;
    public ProjectProgressResult projectProgressResult;
    public AnalyzedStories analyzedStories = new AnalyzedStories();
    public int qualityScore;
    public int numberOfOpenIssues;


    public double plannedRoadmapFeatureVelocity = 0;
    public double defaultNotEstimatedIssueValue;
    public Date startDateRoadmapFeature = null;
    public double sprintLengthRoadmapFeature = 0;
    public Date targetDate = null;

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
        this.featureKey = roadmapFeature.getKey();
        this.featureSummary = roadmapFeature.getSummary();
        this.projectKey = currentProject.getKey();
        this.mao = mao;

    }

    @Override
    public int compareTo(RoadmapFeatureAnalysis o) {
        return featureSummary.compareTo(o.featureSummary);
    }

    @Override
    public int compare(RoadmapFeatureAnalysis o1, RoadmapFeatureAnalysis o2) {
        return o1.featureSummary.compareTo(o2.featureSummary);
    }


    public boolean analyzeRoadmapFeature()
    {
        boolean result = false;

        defaultNotEstimatedIssueValue = getDefaultValueForNonEstimatedIssue();
        //get list of issues and convert them to PlaygileIssues
        StatusText.getInstance().add(true, "Start analysis for " + featureKey + " " + featureSummary);

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

                if (playgileIssue.bIssueOpen) numberOfOpenIssues++;

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
            remainingTotalEstimations = projectMonitoringMisc.roundToDecimalNumbers(remainingTotalEstimations, 2);
            StatusText.getInstance().add(true, "Current time and estimations to add to list " + timeStamp + " " + remainingTotalEstimations);
            ManageActiveObjectsResult maor = mao.AddRemainingEstimationsRecord(new ManageActiveObjectsEntityKey(projectKey, featureSummary), timeStamp, remainingTotalEstimations);


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

                //get target date. I cannot do that before progress calculated, as in case it is not set in DB then I take it
                //as planned project end
                targetDate = getTargetDate();

                //calculate quality score
                qualityScore = getQualityScore();
                result = true;
            }
            else
            {
                StatusText.getInstance().add(true, "Failed to get historical estimations for " + featureSummary);
                result = false; //no estimations
            }

        }
        else
        {
            StatusText.getInstance().add(true, "Failed to retrieve any project issues for " + featureSummary);
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
        return featureKey + " " + featureSummary;
    }

    private ArrayList<DataPair> getHistoricalEstimations()
    {
        ArrayList<DataPair> result = null;
        ManageActiveObjectsResult maor = mao.GetProgressDataList(new ManageActiveObjectsEntityKey(projectKey, featureSummary));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            result = (ArrayList<DataPair>)maor.Result;
        }
        return result;
    }

    private Date getTargetDate()
    {
        targetDate = null;
        ManageActiveObjectsResult maor = mao.GetTargetDate(new ManageActiveObjectsEntityKey(projectKey, featureSummary));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            targetDate = (Date)maor.Result;
            StatusText.getInstance().add(true, "Target date from DB is " + targetDate);
        }
        if (targetDate == null) {
            //not configured - set to planned date
            if (projectProgressResult != null) {
                targetDate = projectProgressResult.idealProjectEnd;
                StatusText.getInstance().add(true, "Target date set from planned date  is " + targetDate);
            }
        }
        if (targetDate != null) targetDate = DateTimeUtils.getZeroTimeDate(targetDate);
        StatusText.getInstance().add(true, "Detected start date is " + targetDate);
        return targetDate;
    }

    private double getDefaultValueForNonEstimatedIssue()
    {
        defaultNotEstimatedIssueValue = 0;
        ManageActiveObjectsResult maor = mao.GetDefaultNotEstimatedIssueValue(new ManageActiveObjectsEntityKey(projectKey, featureSummary));
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
        ManageActiveObjectsResult maor = mao.GetPlannedRoadmapVelocity(new ManageActiveObjectsEntityKey(projectKey, featureSummary));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            plannedRoadmapFeatureVelocity = (double)maor.Result;
        }
        if (plannedRoadmapFeatureVelocity <= 0) plannedRoadmapFeatureVelocity = ProjectMonitor.defaultInitialRoadmapFeatureVelocity;

        getDefaultValueForNonEstimatedIssue();

        startDateRoadmapFeature = null;
        maor = mao.GetProjectStartDate(new ManageActiveObjectsEntityKey(projectKey, featureSummary));
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
        startDateRoadmapFeature = DateTimeUtils.getZeroTimeDate(startDateRoadmapFeature);
        StatusText.getInstance().add(true, "Detected start date is " + startDateRoadmapFeature);

        double oldestSprintLength = 0;
        if (oldestSprint != null) {
            oldestSprintLength = DateTimeUtils.AbsDays(oldestSprint.getStartDate(), oldestSprint.getEndDate()) + 1;
            StatusText.getInstance().add(true, "Detected oldest sprint is from " + oldestSprint.getStartDate() + " till " + oldestSprint.getEndDate() + " length is " + oldestSprintLength);
        }
        //sprint length
        sprintLengthRoadmapFeature = 0;
        maor = mao.GetSprintLength(new ManageActiveObjectsEntityKey(projectKey, featureSummary));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            sprintLengthRoadmapFeature = (double)maor.Result;
            StatusText.getInstance().add(true, "Detected sprint length from DB is " + sprintLengthRoadmapFeature);
        }
        if (sprintLengthRoadmapFeature <= 0) {
            //try to find from sprints
            if (oldestSprintLength > 0) {
                sprintLengthRoadmapFeature = oldestSprintLength;
                StatusText.getInstance().add(true, "Detected sprint length from the oldest sprint " + sprintLengthRoadmapFeature);
            }
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

    int getQualityScore()
    {
        int result;
        /*
        	1 Red, 2 - Yellow, 3 - Green

        //delay
        if (Expected end date <= Planned end date) DelayScore = 3;
        else
        {
           DelayScore = ((Expected date - Planned date) / Plannedlength)
           ---- up to 5%  green (3),  5-15% - yellow (2), > 15% - red (1)
        }
        */
        int delayScore;
        if (projectProgressResult.predictedProjectEnd.before(targetDate) || projectProgressResult.predictedProjectEnd.equals(targetDate))
        {
            delayScore = 3;
        }
        else
        {
            int differenceInDays = DateTimeUtils.AbsDays(projectProgressResult.predictedProjectEnd, targetDate);
            double delay = (double)differenceInDays / (double)(DateTimeUtils.AbsDays(targetDate, startDateRoadmapFeature));
            if (delay > 0 && delay <= 0.05) delayScore = 3;
            else if (delay > 0.05 && delay < 0.15) delayScore = 2;
            else delayScore = 1;
        }
        /*
        //estimation ration
        EstimationScore = (number of normal (>0 <13 SP) not closed stories) / (Total number of not closed stories)
           ----- 90-100% - green (3), 60-80% - yellow (2) 0-60% Red (1)
        */
        int estimationScore;
        double estimationRatio = analyzedStories.EstimatedStoriesNumber /
            (analyzedStories.EstimatedStoriesNumber + analyzedStories.LargeStoriesNumber +
                    analyzedStories.VeryLargeStoriesNumber + analyzedStories.NotEstimatedStoriesNumber);
        if (estimationRatio >= 0.9) estimationScore = 3;
        else if (estimationRatio >= 0.6 && estimationRatio < 0.9) estimationScore = 2;
        else estimationScore = 1;
        /*

        BacklogReadinessScore = Number of "Open" stories / (Total number of not closed stories)
                  0-10%  green (3), 10-30% yellow (2), 30-100% red (1)

        */
        int readinessScore = 3;
        double readinessRatio = (double)numberOfOpenIssues / (double)futurePlaygileIssues.size();
        if (readinessRatio >= 0.0 && readinessRatio < 0.1) readinessScore = 3;
        else if (readinessRatio >= 0.1 && readinessRatio < 0.3) readinessScore = 2;
        else readinessScore = 1;
        /*
        Here we have 3 scores each can be 1, 2 or 3. The minimum between them will be the final color
	    For example 1, 2, 2 -- 1 (red), 2, 3, 3 -- 2 (yellow) etc.
        */
        result = Math.min(delayScore, Math.min(estimationScore, readinessScore));

        return result;
    }
}
