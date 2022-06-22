package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileIssue;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.jiraissues.SprintState;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;


public class ProjectMonitoringMisc {

    String TASK = "Task";
    String STORY = "Story";
    String TECH_STORY = "Tech Story";
    String RESEARCH = "Research";
    String TECH_DEBT = "Technical Debt";
    String BUG = "Bug";

    private JiraInterface jiraInterface;

    public ProjectMonitoringMisc(JiraInterface jiraInterface) {
        this.jiraInterface = jiraInterface;
    }

    public void addIssueSprintsToList(Issue issue, ArrayList<PlaygileSprint> playgileSprints) {
        //get all sprints
        Collection<PlaygileSprint> sprintsForIssue = jiraInterface.getAllSprintsForIssue(issue);
        if (sprintsForIssue == null || sprintsForIssue.size() <= 0) {
            StatusText.getInstance().add(false, "No sprints for " + issue.getId());
        } else {
            //StatusText.getInstance().add(false, "Sprints for " + issue.getId() + " " + sprintsForIssue.size());
            for (PlaygileSprint playgileSprint : sprintsForIssue) {
                //do we have such sprint already
                PlaygileSprint foundSprint = null;
                for (PlaygileSprint tmpSprint : playgileSprints) {
                    if (tmpSprint.getId() == playgileSprint.getId()) {
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
                    if (playgileSprint.getState() == SprintState.CLOSED) {
                        if (isIssueCompleted(issue)) {
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
    }


    public ArrayList<Double> getLinearRegressionForRealSprintVelocities(Collection<PlaygileSprint> allRealSprints, Date startDate) {
        ArrayList<Double> result = new ArrayList<>();
        if (allRealSprints.size() <= 0) {
            return result;
        }

        double[] x = new double[allRealSprints.size()];
        double[] y = new double[allRealSprints.size()];
        int index = 0;
        for (PlaygileSprint realSprint : allRealSprints) {
            x[index] = DateTimeUtils.AbsDays(realSprint.getEndDate(), startDate);
            y[index] = realSprint.sprintVelocity;
            index++;
        }
        //y = k*x + b
        LinearRegression lr = new LinearRegression();
        lr.getRegressionSlopeAndIntercept(x, y);
        double slope = lr.slope;
        double intercept = lr.intercept;
        result.addAll(allRealSprints.stream().map(realSprint -> DateTimeUtils.AbsDays(realSprint.getEndDate(), startDate) * slope + intercept).collect(Collectors.toList()));

        return result;
    }

    public Collection<PlaygileSprint> getAllRealSprintsVelocitiesForConstantTimeWindows(Collection<PlaygileIssue> playgileIssues, Date startDate, double plannedRoadmapFeatureVelocity, int sprintLength) {
        //in this algorithm I calculate each constant sprint velocity based on time frame
        //for example - if the first sprint starts on January 1st, I calculate constant frames, Jan, 1st + sprintLength - 1, and so on
        //So I don't rely on real sprints, just on the issues completed within each such period
        ArrayList<PlaygileSprint> result = new ArrayList<>();

        if (playgileIssues == null) return result;
        if (sprintLength < 2) return result; //otherwise algorithm below will not end

        Date constantSprintStart = startDate;
        Date constantSprintEnd = DateTimeUtils.AddDays(constantSprintStart, sprintLength - 1);

        StatusText.getInstance().add(true, "#### starting to identify issues by artificial sprints");

        while (DateTimeUtils.CompareZeroBasedDatesOnly(constantSprintEnd, DateTimeUtils.getCurrentDate()) < 0) {
            StatusText.getInstance().add(true, "*** in the loop - artificial sprint " + constantSprintStart + " " + constantSprintEnd);
            //find all issues closed withing this period
            PlaygileSprint sprintToAdd = new PlaygileSprint();
            double constantSprintProjectVelocity = 0;
            for (PlaygileIssue issue : playgileIssues) {
                //logText.append("EEEDDD " + issue.getKey() + " " + issue.getIssueType().getName() + " bool is " + bIssueIsOurs + " issue completed " + issue.getStatus().getStatusCategory().getKey());
                if (issue.bOurIssueType &&
                    issue.bIssueCompleted &&
                    issue.resolutionDate != null &&
                    DateTimeUtils.CompareZeroBasedDatesOnly(issue.resolutionDate, constantSprintStart) >= 0 &&
                    DateTimeUtils.CompareZeroBasedDatesOnly(issue.resolutionDate, constantSprintEnd) <= 0
                ) {
                    //issue closed within our constant sprint
                    double storyPointValue = issue.storyPoints; //we can possibly in the future use adjusted issue.getAdjustedEstimationValue()
                    StatusText.getInstance().add(true, "Issue to count " + issue.issueKey + " with " + storyPointValue + " points and resolution date " + issue.resolutionDate);
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

        StatusText.getInstance().add(true, "#### ended to identify issues by artificial sprints, found " + result.size());

        //if number of calculated artificial sprints is 2 or less - we use the team velocity
        if (result.size() < 3) {
            for (PlaygileSprint tmpSprint : result) {
                tmpSprint.sprintVelocity = plannedRoadmapFeatureVelocity;
            }
        }

        return result;
    }

    public boolean isIssueOneOfOurs(Issue issue, Issue roadmapFeature) {
        IssueType issueType = issue.getIssueType();
        boolean result = false;
        result = (issueType != null &&
            (issueType.getName().equalsIgnoreCase(STORY) || issueType.getName().equalsIgnoreCase(TECH_STORY) ||
                issueType.getName().equalsIgnoreCase(RESEARCH) || issueType.getName().equalsIgnoreCase(TECH_DEBT) ||
                issueType.getName().equalsIgnoreCase(TASK)));
        if (!result && Objects.requireNonNull(roadmapFeature.getIssueType()).getName().equalsIgnoreCase(ViewTypes.EPIC) && issueType != null) {
            result = issueType.getName().equalsIgnoreCase(BUG);//don't count bugs in the issues
        }
        return result;
    }

    public boolean isIssueCompleted(Issue issue) {
        Status issueStatus = issue.getStatus();
        StatusCategory statusCategory = issueStatus.getStatusCategory();
        return (Objects.equals(issueStatus.getName(), "Closed") || Objects.equals(issueStatus.getName(), "Done"));
    }

    public boolean isIssueOpen(Issue issue) {
        return (getIssueStatus(issue).equalsIgnoreCase("open") || getIssueStatus(issue).equalsIgnoreCase("To Do"));
    }

    public boolean isIssueReadyForEstimation(Issue issue) {
        return (getIssueStatus(issue).equalsIgnoreCase("READY FOR ESTIMATION"));
    }

    public boolean isIssueReadyForDevelopment(Issue issue) {
        return (getIssueStatus(issue).equalsIgnoreCase("READY FOR DEV") || getIssueStatus(issue).equalsIgnoreCase("Ready For Development"));
    }

    public String getIssueStatus(Issue issue) {
        return issue.getStatus().getName();
    }

    public static String getExceptionTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public static Map<String, Object> returnContextMapToVelocityTemplate(Map<String, Object> contextMap, boolean bAllisOk, String messageToDisplay) {
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

    public static <T> T as(Class<T> clazz, Object o) {
        if (clazz.isInstance(o)) {
            return clazz.cast(o);
        }
        return null;
    }
}
