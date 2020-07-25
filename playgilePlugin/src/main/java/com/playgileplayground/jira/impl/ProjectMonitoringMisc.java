package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.jiraissues.SprintState;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;
import org.joda.time.DateTime;

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
    public void addIssueSprintsToList(Issue issue,ArrayList<PlaygileSprint> playgileSprints)
    {
        //get all sprints
        Collection<PlaygileSprint> sprintsForIssue = jiraInterface.getAllSprintsForIssue(issue);
        if (sprintsForIssue != null && sprintsForIssue.size() > 0) {
            //WriteToStatus(false, "Sprints for " + issue.getId() + " " + sprintsForIssue.size());
            for (PlaygileSprint playgileSprint : sprintsForIssue)
            {
                //do we have such sprint already
                boolean bSprintAlreadyAdded = false;
                for (PlaygileSprint tmpSprint : playgileSprints)
                {
                    if (tmpSprint.getId() == playgileSprint.getId())
                    {
                        bSprintAlreadyAdded = true;
                        break;
                    }
                }
                if (!bSprintAlreadyAdded && playgileSprint.getState() != SprintState.FUTURE && (playgileSprint.getState() != SprintState.UNDEFINED))
                {
                    //WriteToStatus(false,"Adding sprint for " + issue.getId() + " " + playgileSprint.getName());
                    playgileSprints.add(playgileSprint);
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
            Status issueStatus = issue.getStatus();
            StatusCategory statusCategory = issueStatus.getStatusCategory();
            IssueType issueType = issue.getIssueType();

            boolean bOurIssueType =
                issueType.getName().equals(STORY) ||
                    issueType.getName().equals(TASK) ||
                    issueType.getName().equals(BUG);

            if (statusCategory == null)
            {
                WriteToStatus(statusText, false, "Failed to retrieve issue status for issue " + issue.getId());
                //go to next issue
                continue;
            }

            addIssueSprintsToList(issue, playgileSprints);
            double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);
            WriteToStatus(statusText, false, "Story points " + issue.getId() + " " + storyPointValue);
            if (statusCategory.getKey() != StatusCategory.COMPLETE
                        /*&& storyPointValue >= 0*/ //we don't mind not set story points. I'll set them to 21
                && (bOurIssueType)
                )
            {
                WriteToStatus(statusText, true, "Issue for calculation " +
                    statusCategory.getKey() + " " +
                    storyPointValue + " " +
                    issue.getKey());
                foundIssues.add(issue);
            }
            else
            {
                WriteToStatus(statusText, true, "Issue is not ours " +
                    statusCategory.getKey() + " " +
                    storyPointValue + " " +
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
            IssueType issueType = issue.getIssueType();

            boolean bOurIssueType =
                issueType.getName().equals(STORY) ||
                    issueType.getName().equals(TASK) ||
                    issueType.getName().equals(BUG);
            double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);
            if (storyPointValue <= 0) storyPointValue = ProjectMonitor.MAX_STORY_ESTIMATION; //for not estimated
            WriteToStatus(statusText, true, "Story points for initial estimation in issue " + issue.getId() + " " + storyPointValue + " created " + issue.getCreated());
            if (bOurIssueType && issue.getCreated().compareTo(startDate) <= 0) //our issue and created before project started
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

    public void WriteToStatus(StringBuilder statusText, boolean debug, String text)
    {
        if (debug) statusText.append(text + "<br>");
    }
}
