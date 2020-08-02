package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.lang.Pair;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.jiraissues.SprintState;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.projectprogress.ProjectProgress;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Ext_EdG on 7/16/2020.
 */
public class ProjectMonitoringMisc {

    String TASK = "Task";
    String STORY = "Story";
    String BUG = "Bug";

    private JiraInterface jiraInterface;
    private ApplicationUser applicationUser;
    private Project currentProject;
    public ProjectMonitoringMisc(JiraInterface jiraInterface, ApplicationUser applicationUser, Project currentProject)
    {
        this.jiraInterface = jiraInterface;
        this.currentProject = currentProject;
        this.applicationUser = applicationUser;
    }
    public ArrayList<String> getAllRoadmapFeatureNames(List<Issue> features)
    {
        ArrayList<String> roadmapFeaturesNames = new ArrayList<>();
        if (features != null && features.size() > 0)
        {
            //convert to string list
            for (Issue feature : features)
            {
                roadmapFeaturesNames.add(feature.getSummary());
            }
            Collections.sort(roadmapFeaturesNames);//sort alphabetically for better user experience
        }
        return roadmapFeaturesNames;
    }
    public void addIssueSprintsToList(Issue issue, ArrayList<PlaygileSprint> playgileSprints)
    {
        //get all sprints
        Collection<PlaygileSprint> sprintsForIssue = jiraInterface.getAllSprintsForIssue(issue);
        if (sprintsForIssue != null && sprintsForIssue.size() > 0) {
            //WriteToStatus(false, "Sprints for " + issue.getId() + " " + sprintsForIssue.size());
            for (PlaygileSprint playgileSprint : sprintsForIssue)
            {
                //do we have such sprint already
                PlaygileSprint foundSprint = null;
                for (PlaygileSprint tmpSprint : playgileSprints)
                {
                    if (tmpSprint.getId() == playgileSprint.getId())
                    {
                        foundSprint = tmpSprint;
                        break;
                    }
                }

                if (playgileSprint.getState() != SprintState.FUTURE && (playgileSprint.getState() != SprintState.UNDEFINED)) {
                    //add if needed
                    if (foundSprint == null) {
                        foundSprint = playgileSprint;
                        //WriteToStatus(false,"Adding sprint for " + issue.getId() + " " + playgileSprint.getName());
                        playgileSprints.add(foundSprint);
                    }

                    //update velocity only for closed sprints and completed features
                    if (playgileSprint.getState() == SprintState.CLOSED)
                    {
                        if (isIssueCompleted(issue))
                        {
                            //if estimation is missing for velocity set it to 0
                            double tmpVelocity = jiraInterface.getStoryPointsForIssue(issue);
                            if (tmpVelocity <= 0) tmpVelocity = 0;
                            foundSprint.sprintVelocity += tmpVelocity;
                            foundSprint.updateIssuesTimeDistribution(issue); //update distribution statistics
                        }
                    }
                }
            }
        }
        else
        {
            //WriteToStatus(false, "No sprints for " + issue.getId());
        }
    }
    public Issue SearchSelectedIssue(List<Issue> roadmapFeatures, String selectedRoadmapFeature)
    {
        //let's find our selected in the list
        //find issue in the list of all roadmap features
        Issue selectedRoadmapFeatureIssue = null;
        for (Issue tmpFeature : roadmapFeatures)
        {
            if (selectedRoadmapFeature.equals(tmpFeature.getSummary()))
            {
                selectedRoadmapFeatureIssue = tmpFeature;
                break;
            }
        }
        return selectedRoadmapFeatureIssue;
    }

    public void getNotCompletedIssuesAndAndSprints(Collection<Issue> issues,
                                                   ArrayList<Issue> foundIssues,
                                                   ArrayList<PlaygileSprint> playgileSprints,
                                                   StringBuilder statusText)
    {
        for (Issue issue : issues)
        {
            boolean bOurIssueType = isIssueOneOfOurs(issue);
            addIssueSprintsToList(issue, playgileSprints);
            double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);
            WriteToStatus(statusText, false, "Story points " + issue.getId() + " " + storyPointValue);
            if (!isIssueCompleted(issue)
                        /*&& storyPointValue >= 0*/ //we don't mind not set story points. I'll set them to 21
                && (bOurIssueType)
                )
            {
                WriteToStatus(statusText, true, "Issue for calculation " +
                    storyPointValue + " " +
                    issue.getKey());
                foundIssues.add(issue);
            }
            else
            {
                WriteToStatus(statusText, true, "Issue is not ours - Completed " +
                    issue.getKey());
                //go to the next issue
                continue;
            }
        }
    }
    public double getInitialEstimation(Collection<Issue> issues, Date startDate, StringBuilder statusText)
    {
        double result = 0;
        WriteToStatus(statusText, true, "Calculate initial estimation for project start date " + startDate);
        for (Issue issue : issues)
        {
            boolean bOurIssueType = isIssueOneOfOurs(issue);
            double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);

            storyPointValue = adjustStoryPointsIfNotEstimated(storyPointValue, isIssueBug(issue));

            WriteToStatus(statusText, true, "Story points for initial estimation in issue " + issue.getId() + " " + storyPointValue + " created " + issue.getCreated());
            if (bOurIssueType && CompareDatesOnly(issue.getCreated(), startDate) <= 0) //our issue and created before project started
            {
                WriteToStatus(statusText, true, "Issue for initial estimation calculation " +
                    storyPointValue + " " +
                    issue.getKey());
                result += storyPointValue;
            }
            else
            {
                WriteToStatus(statusText, true, "Issue for initial estimation is not ours " +
                    storyPointValue + " " +
                    issue.getKey());
                //go to the next issue
                continue;
            }
        }
        return result;
    }

    public AnalyzedStories getStoriesAnalyzed(Collection<Issue> issues) {
        AnalyzedStories result = new AnalyzedStories();
        if (issues == null) return result;
        for (Issue issue : issues)
        {
            boolean bOurIssueType = isIssueOneOfOurs(issue) && !isIssueCompleted(issue);
            if (bOurIssueType) //our issue and created before project started
            {
                double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);
                if (storyPointValue > 0 && storyPointValue < 13) result.EstimatedStoriesNumber++;
                if (storyPointValue == 13) result.LargeStoriesNumber++;
                if (storyPointValue > 13) result.VeryLargeStoriesNumber++;
                if (storyPointValue <= 0) result.NotEstimatedStoriesNumber++;
            }
        }
        return result;
    }

    public ArrayList<Double> getAverageForRealSprintVelocities(Collection<PlaygileSprint> allRealSprints, Date startDate, StringBuilder statusText)
    {
        ArrayList<Double> result = new ArrayList<>();
        if (allRealSprints.size() <= 0) {
            return result;
        }

        double countForAverage = 0;
        double  sumForAverage = 0;


        for (PlaygileSprint realSprint : allRealSprints)
        {
            countForAverage++;
            sumForAverage += realSprint.sprintVelocity;
            result.add(sumForAverage / countForAverage);
        }
        return result;
    }

    public ArrayList<Double> getLinearRegressionForRealSprintVelocities(Collection<PlaygileSprint> allRealSprints, Date startDate, StringBuilder statusText)
    {
        ArrayList<Double> result = new ArrayList<>();
        if (allRealSprints.size() <= 0) {
            return result;
        }

        double[] x = new double[allRealSprints.size()];
        double[] y = new double[allRealSprints.size()];
        int index = 0;
        for (PlaygileSprint realSprint : allRealSprints)
        {
            x[index] = ProjectProgress.AbsDays(realSprint.getEndDate(), startDate);
            y[index] = realSprint.sprintVelocity;
            index++;
        }
        //y = k*x + b
        LinearRegression lr = new LinearRegression();
        lr.getRegressionSlopeAndIntercept(x, y);
        double slope = lr.slope; double intercept = lr.intercept;
        for (PlaygileSprint realSprint : allRealSprints)
        {
            result.add(ProjectProgress.AbsDays(realSprint.getEndDate(), startDate) * slope + intercept);
        }

        return result;
    }
    public Collection<PlaygileSprint> getAllRealSprintsVelocities(Collection<PlaygileSprint> playgileSprints, Date startDate, double teamVelocity, int sprintLength, StringBuilder statusText)
    {
        ArrayList<PlaygileSprint> result = new ArrayList<>();

        if (playgileSprints == null) return result;


        //here is an interesting situation. Two or more teams working on the same feature. They might have sprints
        //that are not starting-ending the same day - ie. overlapping. For example the sprints might be shifted
        //---start-----------end---
        //------start-----------end;
        //in this situation I select the start of the project and go by increments of sprint length periods
        //calculating the total velocity during sprint length

        //the algorithm to account for overlaps is following:
        //1. Take the project start (earliest sprint start date
        //2. Do a loop with period of sprint length by (14 days) till the calculated date is beyond all sprint ends
        //3. For each sprint that overlap or match the start and end dates sum up the velocities

        //add first record as team velocity
        PlaygileSprint sprintToAdd = new PlaygileSprint();
        sprintToAdd.setEndDate(startDate);
        sprintToAdd.sprintVelocity = teamVelocity;
        sprintToAdd.setState(SprintState.CLOSED);
        result.add(sprintToAdd);

        Date correctSprintStart = startDate;
        Date correctSprintEnd = ProjectProgress.AddDays(correctSprintStart, sprintLength - 1);

        boolean bLoopNotFinished;
        do
        {
            WriteToStatus(statusText, true, "*** in the loop - correct sprint " + correctSprintStart + " " + correctSprintEnd);
            bLoopNotFinished = false;
            double correctSprintVelocity = 0;
            for (PlaygileSprint playgileSprint : playgileSprints)
            {
                if (playgileSprint.getState() == SprintState.CLOSED) {

                    WriteToStatus(statusText, true, "Checking sprint " + playgileSprint.getName() + " " + playgileSprint.getStartDate() + " " + playgileSprint.getEndDate());
                    //sprint fits within correct dates
                    boolean bSprintFound =
                        // |-----| correct sprint
                        // |-----|
                        (CompareDatesOnly(playgileSprint.getStartDate(), correctSprintStart) <= 0 && CompareDatesOnly(playgileSprint.getEndDate(), correctSprintEnd) >= 0)
                        ||
                        (CompareDatesOnly(playgileSprint.getStartDate(), correctSprintStart) >= 0 && CompareDatesOnly(playgileSprint.getEndDate(), correctSprintEnd) <= 0)
                        ||
                        // |-----| correct sprint
                        //    |-----|
                        (CompareDatesOnly(playgileSprint.getEndDate(), correctSprintStart) > 0 && CompareDatesOnly(playgileSprint.getEndDate(), correctSprintEnd) < 0)
                        ||
                        //     |-----| correct sprint
                        // |-----|
                        (CompareDatesOnly(playgileSprint.getStartDate(), correctSprintStart) > 0 && CompareDatesOnly(playgileSprint.getStartDate(), correctSprintEnd) < 0)
                        ;

                    if (bSprintFound) { //overlapping sprint found
                        WriteToStatus(statusText, true, "sprint " + playgileSprint.getName() + " is overlapping ");
                        correctSprintVelocity += playgileSprint.sprintVelocity;
                        bLoopNotFinished |= bSprintFound;
                    }
                }
            }
            if (bLoopNotFinished) {
                sprintToAdd = new PlaygileSprint();
                sprintToAdd.setEndDate(correctSprintEnd);
                sprintToAdd.sprintVelocity = correctSprintVelocity;
                sprintToAdd.setState(SprintState.CLOSED);
                result.add(sprintToAdd);

                //next sprint
                correctSprintStart = ProjectProgress.AddDays(correctSprintStart, sprintLength);
                correctSprintEnd = ProjectProgress.AddDays(correctSprintStart, sprintLength - 1);
            }
        } while (bLoopNotFinished);

        return result;
    }

    public double getAverageProjectRealVelocity(Collection<PlaygileSprint> playgileSprints, double teamVelocity, StringBuilder statusText)
    {
        double result;
        if (teamVelocity == 0) teamVelocity = 1.0; //just to be fair
        if (playgileSprints == null) return teamVelocity;
        int numOfAvailableSprints = 0;
        double tmpSum = 0;

        WriteToStatus(statusText, true, " Avg velocity calculation for num of sprints " + playgileSprints.size() + " initial sum " + tmpSum);
        for (PlaygileSprint playgileSprint : playgileSprints)
        {
            if (playgileSprint.getState() == SprintState.CLOSED)
            {
                numOfAvailableSprints++;
                tmpSum += playgileSprint.sprintVelocity;
                WriteToStatus(statusText, true, " Adding sprint velocity " + playgileSprint.sprintVelocity + " sum is " + tmpSum + " num of values " + numOfAvailableSprints);
            }
        }
        WriteToStatus(statusText, true, " Total sum is " + tmpSum + " and num is " + numOfAvailableSprints);
        result = (int)Math.round(tmpSum / numOfAvailableSprints);
        return result;
    }

    public String ConvertDateToOurFormat(Date dateToConvert)
    {
        SimpleDateFormat outputDateFormat = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT);
        return outputDateFormat.format(dateToConvert);
    }

    public void WriteToStatus(StringBuilder statusText, boolean debug, String text)
    {
        if (debug) statusText.append(text + "<br>");
    }

    public int CompareDatesOnly(Date firstDate, Date secondDate)
    {
        return (getZeroTimeDate(firstDate).compareTo(getZeroTimeDate(secondDate)));
    }
    private static Date getZeroTimeDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        date = calendar.getTime();
        return date;
    }

    public boolean isIssueOneOfOurs(Issue issue)
    {
        IssueType issueType = issue.getIssueType();
        return (issueType.getName().equals(STORY) ||
            issueType.getName().equals(TASK) ||
            issueType.getName().equals(BUG));
    }

    public boolean isIssueBug(Issue issue)
    {
        IssueType issueType = issue.getIssueType();
        return issueType.getName().equals(BUG);
    }
    public double adjustStoryPointsIfNotEstimated(double storyPointValue, boolean isIssueBug)
    {
        //Julia asked for not estimated bug to be 13
        double result = storyPointValue;
        if (storyPointValue <= 0)
        {
            if (isIssueBug) result = ProjectMonitor.MAX_BUG_ESTIMATION; //for not estimated
            else result = ProjectMonitor.MAX_STORY_ESTIMATION; //for not estimated
        }
        return result;
    }
    public boolean isIssueCompleted(Issue issue)
    {
        Status issueStatus = issue.getStatus();
        StatusCategory statusCategory = issueStatus.getStatusCategory();
        return (statusCategory.getKey() == StatusCategory.COMPLETE);
    }

}
