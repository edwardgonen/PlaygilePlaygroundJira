package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileIssue;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.jiraissues.SprintState;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import java.io.PrintWriter;
import java.io.StringWriter;
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
    private ManageActiveObjects mao;
    public ProjectMonitoringMisc(JiraInterface jiraInterface, ApplicationUser applicationUser, Project currentProject, ManageActiveObjects mao)
    {
        this.jiraInterface = jiraInterface;
        this.currentProject = currentProject;
        this.applicationUser = applicationUser;
        this.mao = mao;
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
            //StatusText.getInstance().add(false, "Sprints for " + issue.getId() + " " + sprintsForIssue.size());
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
                        //StatusText.getInstance().add(false,"Adding sprint for " + issue.getId() + " " + playgileSprint.getName());
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
            //StatusText.getInstance().add(false, "No sprints for " + issue.getId());
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
                                                   ArrayList<PlaygileSprint> playgileSprints)
    {
        for (Issue issue : issues)
        {
            boolean bOurIssueType = isIssueOneOfOurs(issue);
            addIssueSprintsToList(issue, playgileSprints);
            double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);
            StatusText.getInstance().add(false, "Story points " + issue.getId() + " " + storyPointValue);
            if (!isIssueCompleted(issue)
                        /*&& storyPointValue >= 0*/ //we don't mind not set story points. I'll set them to 21
                && (bOurIssueType)
                )
            {
                StatusText.getInstance().add(true, "Issue for calculation " +
                    storyPointValue + " " +
                    issue.getKey());
                foundIssues.add(issue);
            }
            else
            {
                StatusText.getInstance().add( true, "Issue is not ours - Completed " +
                    issue.getKey());
                //go to the next issue
                continue;
            }
        }
    }

    public double getSprintLength(Project currentProject, String selectedRoadmapFeature)
    {
        double sprintLength;
        ManageActiveObjectsResult maor = mao.GetSprintLength(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            sprintLength = (double)maor.Result;
            //round to one week if needed
            if (sprintLength <= 0) sprintLength = 14;
            else {
                if (sprintLength > 7)
                    sprintLength = ((int) sprintLength / 7) * 7;
            }
        }
        else
        {
            sprintLength = 14;
        }
        return sprintLength;
    }

    public double getDefaultNotEstimatedIssueValue(Project currentProject, String selectedRoadmapFeature)
    {
        double defaultNotEstimatedIssueValue = 0;
        ManageActiveObjectsResult maor = mao.GetDefaultNotEstimatedIssueValue(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            defaultNotEstimatedIssueValue = (double)maor.Result;
        }
        return defaultNotEstimatedIssueValue;
    }

    public double getCurrentEstimations(List<Issue> foundIssues,  double defaultNotEstimatedIssueValue)
    {
        double result = 0;
        for (Issue issue : foundIssues)
        {
            if (!isIssueCompleted(issue))
            {
                StatusText.getInstance().add( false,"Issue status is Complete");
                double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);
                storyPointValue = adjustStoryPointsIfNotEstimated(storyPointValue, isIssueBug(issue), defaultNotEstimatedIssueValue);
                result += storyPointValue;
                StatusText.getInstance().add( true, "Adding story points for issue " +
                    storyPointValue + " " +
                    issue.getKey());
            }
            else
            {
                StatusText.getInstance().add( false,"Issue status Complete");
            }
        }
        return result;
    }

    public double getInitialEstimation(Collection<Issue> issues, Date startDate,  double defaultNotEstimatedIssueValue)
    {
        double result = 0;
        StatusText.getInstance().add( true, "Calculate initial estimation for project start date " + startDate);
        for (Issue issue : issues)
        {
            boolean bOurIssueType = isIssueOneOfOurs(issue);
            double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);
            storyPointValue = adjustStoryPointsIfNotEstimated(storyPointValue, isIssueBug(issue), defaultNotEstimatedIssueValue);

            StatusText.getInstance().add( true, "Story points for initial estimation in issue " + issue.getId() + " " + storyPointValue + " created " + issue.getCreated());
            if (bOurIssueType && DateTimeUtils.CompareZeroBasedDatesOnly(issue.getCreated(), startDate) <= 0) //our issue and created before project started
            {
                StatusText.getInstance().add( true, "Issue for initial estimation calculation " +
                    storyPointValue + " " +
                    issue.getKey());
                result += storyPointValue;
            }
            else
            {
                StatusText.getInstance().add( true, "Issue for initial estimation is not ours " +
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
            //count stories, tasks. don't count bugs
            boolean bOurIssueType = isIssueOneOfOurs(issue) && !isIssueCompleted(issue);
            if (bOurIssueType) //it is user story, task or bug and not completed and not a bug
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

    public ArrayList<Double> getLinearRegressionForRealSprintVelocities(Collection<PlaygileSprint> allRealSprints, Date startDate)
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
            x[index] = DateTimeUtils.AbsDays(realSprint.getEndDate(), startDate);
            y[index] = realSprint.sprintVelocity;
            index++;
        }
        //y = k*x + b
        LinearRegression lr = new LinearRegression();
        lr.getRegressionSlopeAndIntercept(x, y);
        double slope = lr.slope; double intercept = lr.intercept;
        for (PlaygileSprint realSprint : allRealSprints)
        {
            result.add(DateTimeUtils.AbsDays(realSprint.getEndDate(), startDate) * slope + intercept);
        }

        return result;
    }

    public Collection<PlaygileSprint> getAllRealSprintsVelocitiesForConstantSprints(Collection<Issue> issues, Date startDate, double teamVelocity, int sprintLength)
    {
        //in this algorithm I calculate each constant sprint velocity based on time frame
        //for example - if the first sprint starts on January 1st, I calculate constant frames, Jan, 1st + sprintLength - 1, and so on
        //So I don't rely on real sprints, just on the issues completed within each such period
        ArrayList<PlaygileSprint> result = new ArrayList<>();

        if (issues == null) return result;
        if (sprintLength < 2) return result; //otherwise algorithm below will not end

        Date constantSprintStart = startDate;
        Date constantSprintEnd = DateTimeUtils.AddDays(constantSprintStart, sprintLength - 1);

        StatusText.getInstance().add( true, "#### starting to identify issues by artificial sprints");

        while (DateTimeUtils.CompareZeroBasedDatesOnly(constantSprintEnd, DateTimeUtils.getCurrentDate()) < 0) {
            StatusText.getInstance().add( true, "*** in the loop - artificial sprint " + constantSprintStart + " " + constantSprintEnd);
            //find all issues closed withing this period
            PlaygileSprint sprintToAdd = new PlaygileSprint();
            double constantSprintProjectVelocity = 0;
            for (Issue issue : issues)
            {

                boolean bIssueIsOurs = isIssueOneOfOurs(issue);
                boolean bIssueCompleted = isIssueCompleted(issue);

                //logText.append("EEEDDD " + issue.getKey() + " " + issue.getIssueType().getName() + " bool is " + bIssueIsOurs + " issue completed " + issue.getStatus().getStatusCategory().getKey());
                Date resolutionDate = null;
                boolean bIssueResolutionWithinSprint = false;
                try {
                    if (bIssueCompleted && bIssueIsOurs)
                    {
                        resolutionDate = issue.getResolutionDate();
                        //bIssueResolutionWithinSprint = DateTimeUtils.CheckIfDateIsInsideDateSegmentInclusive(resolutionDate, constantSprintStart, constantSprintEnd);


                        bIssueResolutionWithinSprint =
                            DateTimeUtils.CompareZeroBasedDatesOnly(resolutionDate, constantSprintStart) >= 0 &&
                                DateTimeUtils.CompareZeroBasedDatesOnly(resolutionDate, constantSprintEnd) <= 0;

                    }
                    else // issue not completed - don't check resolution date
                    {
                        bIssueResolutionWithinSprint = false;
                    }
                }
                catch (Exception e)
                {
                    StatusText.getInstance().add( true, "EEEDDD EXCEPTION " + e);
                }

                if (
                    bIssueIsOurs &&
                        bIssueCompleted &&
                        bIssueResolutionWithinSprint
                    )
                {

                    //issue closed within our constant sprint
                    double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);
                    StatusText.getInstance().add( true, "Issue to count " + issue.getKey() + " with " + storyPointValue + " points and resolution date " + resolutionDate);
                    constantSprintProjectVelocity += storyPointValue;
                }
            }



            sprintToAdd.setEndDate(constantSprintEnd);
            sprintToAdd.sprintVelocity = constantSprintProjectVelocity;
            sprintToAdd.setState(SprintState.CLOSED);
            result.add(sprintToAdd);

            //move to next sprint
            constantSprintStart = DateTimeUtils.AddDays(constantSprintStart, sprintLength);
            constantSprintEnd = DateTimeUtils.AddDays(constantSprintStart, sprintLength - 1);
        }

        StatusText.getInstance().add( true, "#### ended to identify issues by artificial sprints, found " + result.size());

        //if number of calculated artificial sprints is 2 or less - we use the team velocity
        if (result.size() < 3)
        {
            for (PlaygileSprint tmpSprint : result)
            {
                tmpSprint.sprintVelocity = teamVelocity;
            }
        }

        return result;
    }

    public Collection<PlaygileSprint> getAllRealSprintsVelocitiesForConstantTimeWindows(Collection<PlaygileIssue> playgileIssues, Date startDate, double plannedRoadmapFeatureVelocity, int sprintLength)
    {
        //in this algorithm I calculate each constant sprint velocity based on time frame
        //for example - if the first sprint starts on January 1st, I calculate constant frames, Jan, 1st + sprintLength - 1, and so on
        //So I don't rely on real sprints, just on the issues completed within each such period
        ArrayList<PlaygileSprint> result = new ArrayList<>();

        if (playgileIssues == null) return result;
        if (sprintLength < 2) return result; //otherwise algorithm below will not end

        Date constantSprintStart = startDate;
        Date constantSprintEnd = DateTimeUtils.AddDays(constantSprintStart, sprintLength - 1);

        StatusText.getInstance().add( true, "#### starting to identify issues by artificial sprints");

        while (DateTimeUtils.CompareZeroBasedDatesOnly(constantSprintEnd, DateTimeUtils.getCurrentDate()) < 0) {
            StatusText.getInstance().add( true, "*** in the loop - artificial sprint " + constantSprintStart + " " + constantSprintEnd);
            //find all issues closed withing this period
            PlaygileSprint sprintToAdd = new PlaygileSprint();
            double constantSprintProjectVelocity = 0;
            for (PlaygileIssue issue : playgileIssues)
            {
                //logText.append("EEEDDD " + issue.getKey() + " " + issue.getIssueType().getName() + " bool is " + bIssueIsOurs + " issue completed " + issue.getStatus().getStatusCategory().getKey());
                if (issue.bOurIssueType &&
                    issue.bIssueCompleted &&
                    issue.resolutionDate != null &&
                    DateTimeUtils.CompareZeroBasedDatesOnly(issue.resolutionDate, constantSprintStart) >= 0 &&
                    DateTimeUtils.CompareZeroBasedDatesOnly(issue.resolutionDate, constantSprintEnd) <= 0
                    )
                {
                    //issue closed within our constant sprint
                    double storyPointValue = issue.getAdjustedEstimationValue();
                    StatusText.getInstance().add( true, "Issue to count " + issue.issueKey + " with " + storyPointValue + " points and resolution date " + issue.resolutionDate);
                    constantSprintProjectVelocity += storyPointValue;
                }
            }



            sprintToAdd.setEndDate(constantSprintEnd);
            sprintToAdd.sprintVelocity = constantSprintProjectVelocity;
            sprintToAdd.setState(SprintState.CLOSED);
            result.add(sprintToAdd);

            //move to next sprint
            constantSprintStart = DateTimeUtils.AddDays(constantSprintStart, sprintLength);
            constantSprintEnd = DateTimeUtils.AddDays(constantSprintStart, sprintLength - 1);
        }

        StatusText.getInstance().add( true, "#### ended to identify issues by artificial sprints, found " + result.size());

        //if number of calculated artificial sprints is 2 or less - we use the team velocity
        if (result.size() < 3)
        {
            for (PlaygileSprint tmpSprint : result)
            {
                tmpSprint.sprintVelocity = plannedRoadmapFeatureVelocity;
            }
        }

        return result;
    }
    public Collection<PlaygileSprint> getAllRealSprintsVelocities(Collection<PlaygileSprint> playgileSprints, Date startDate, double teamVelocity, int sprintLength, StringBuilder statusText)
    {
        ArrayList<PlaygileSprint> result = new ArrayList<>();

        if (playgileSprints == null) return result;
        if (sprintLength < 2) return result; //otherwise algorithm below will not end

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

        //add first record as team velocity if less than 3 sprints
        PlaygileSprint sprintToAdd = new PlaygileSprint();
        if (playgileSprints.size() < 3) //for mathematical purposed we count the team velocity if less than 3 sprints
        {
            StatusText.getInstance().add( true, "Getting real sprints velocity - less than 3 sprints. Using team velocity as first value");
            sprintToAdd.setEndDate(startDate);
            sprintToAdd.sprintVelocity = teamVelocity;
            sprintToAdd.setState(SprintState.CLOSED);
            result.add(sprintToAdd);
        }
        Date correctSprintStart = startDate;
        Date correctSprintEnd = DateTimeUtils.AddDays(correctSprintStart, sprintLength - 1);

        do
        {
            StatusText.getInstance().add( true, "*** in the loop - correct sprint " + correctSprintStart + " " + correctSprintEnd);
            double correctSprintVelocity = 0;
            boolean bFoundOverlappingSprints = false;
            for (PlaygileSprint playgileSprint : playgileSprints)
            {
                if (playgileSprint.getState() == SprintState.CLOSED) {

                    StatusText.getInstance().add( true, "Checking sprint " + playgileSprint.getName() + " " + playgileSprint.getStartDate() + " " + playgileSprint.getEndDate());
                    //sprint fits within correct dates
                    boolean bSprintFound =
                        // |-----| correct sprint
                        // |-----|
                        (DateTimeUtils.CompareZeroBasedDatesOnly(playgileSprint.getStartDate(), correctSprintStart) <= 0 && DateTimeUtils.CompareZeroBasedDatesOnly(playgileSprint.getEndDate(), correctSprintEnd) >= 0)
                        ||
                        (DateTimeUtils.CompareZeroBasedDatesOnly(playgileSprint.getStartDate(), correctSprintStart) >= 0 && DateTimeUtils.CompareZeroBasedDatesOnly(playgileSprint.getEndDate(), correctSprintEnd) <= 0)
                        ||
                        // |-----| correct sprint
                        //    |-----|
                        (DateTimeUtils.CompareZeroBasedDatesOnly(playgileSprint.getEndDate(), correctSprintStart) > 0 && DateTimeUtils.CompareZeroBasedDatesOnly(playgileSprint.getEndDate(), correctSprintEnd) < 0)
                        ||
                        //     |-----| correct sprint
                        // |-----|
                        (DateTimeUtils.CompareZeroBasedDatesOnly(playgileSprint.getStartDate(), correctSprintStart) > 0 && DateTimeUtils.CompareZeroBasedDatesOnly(playgileSprint.getStartDate(), correctSprintEnd) < 0)
                        ;
                    bFoundOverlappingSprints |= bSprintFound;
                    if (bSprintFound) { //overlapping sprint found
                        StatusText.getInstance().add( true, "sprint " + playgileSprint.getName() + " is overlapping ");
                        correctSprintVelocity += playgileSprint.sprintVelocity;
                    }
                }
            }

            if (bFoundOverlappingSprints)
            {
                sprintToAdd = new PlaygileSprint();
                sprintToAdd.setEndDate(correctSprintEnd);
                sprintToAdd.sprintVelocity = correctSprintVelocity;
                sprintToAdd.setState(SprintState.CLOSED);
                result.add(sprintToAdd);
            }

            //next sprint
            correctSprintStart = DateTimeUtils.AddDays(correctSprintStart, sprintLength);
            correctSprintEnd = DateTimeUtils.AddDays(correctSprintStart, sprintLength - 1);
        } while (DateTimeUtils.CompareZeroBasedDatesOnly(correctSprintEnd, DateTimeUtils.getCurrentDate()) < 0);

        return result;
    }

    public double getAverageProjectRealVelocity(Collection<PlaygileSprint> playgileSprints, double teamVelocity, StringBuilder statusText)
    {
        double result;
        if (teamVelocity == 0) teamVelocity = 1.0; //just to be fair
        if (playgileSprints == null) return teamVelocity;
        int numOfAvailableSprints = 0;
        double tmpSum = 0;

        StatusText.getInstance().add( true, " Avg velocity calculation for num of sprints " + playgileSprints.size() + " initial sum " + tmpSum);
        for (PlaygileSprint playgileSprint : playgileSprints)
        {
            if (playgileSprint.getState() == SprintState.CLOSED)
            {
                numOfAvailableSprints++;
                tmpSum += playgileSprint.sprintVelocity;
                StatusText.getInstance().add( true, " Adding sprint velocity " + playgileSprint.sprintVelocity + " sum is " + tmpSum + " num of values " + numOfAvailableSprints);
            }
        }
        StatusText.getInstance().add( true, " Total sum is " + tmpSum + " and num is " + numOfAvailableSprints);
        result = (int)Math.round(tmpSum / numOfAvailableSprints);
        return result;
    }



    public boolean isIssueOneOfOurs(Issue issue)
    {
        IssueType issueType = issue.getIssueType();
        return (
            issueType.getName().equals(STORY) ||
            //issueType.getName().equals(BUG) || //don't count bugs in the issues
            issueType.getName().equals(TASK));
    }

    public boolean isIssueBug(Issue issue)
    {
        IssueType issueType = issue.getIssueType();
        return issueType.getName().equals(BUG);
    }
    public double adjustStoryPointsIfNotEstimated(double storyPointValue, boolean isIssueBug, double defaultNotEstimatedIssueValue)
    {
        //Julia asked for not estimated bug to be 13
        double result = storyPointValue;
        if (storyPointValue <= 0)
        {
            if (defaultNotEstimatedIssueValue <= 0) {
                if (isIssueBug) result = ProjectMonitor.MAX_BUG_ESTIMATION; //for not estimated
                else result = ProjectMonitor.MAX_STORY_ESTIMATION; //for not estimated
            }
            else {
                result = defaultNotEstimatedIssueValue;
            }
        }
        return result;
    }
    public boolean isIssueCompleted(Issue issue)
    {
        Status issueStatus = issue.getStatus();
        StatusCategory statusCategory = issueStatus.getStatusCategory();
        return (statusCategory.getKey() == StatusCategory.COMPLETE);
    }

    public String getExceptionTrace(Exception e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    public Map<String, Object> returnContextMapToVelocityTemplate(Map<String, Object> contextMap, boolean bAllisOk, String messageToDisplay)
    {
        contextMap.put(ProjectMonitor.ALLISOK, bAllisOk);
        contextMap.put(ProjectMonitor.MESSAGETODISPLAY, messageToDisplay);
        contextMap.put(ProjectMonitor.STATUSTEXT, StatusText.getInstance());
        return contextMap;
    }

    public double roundToDecimalNumbers(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
