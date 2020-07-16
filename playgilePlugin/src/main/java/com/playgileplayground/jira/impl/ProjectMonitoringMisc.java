package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.jiraissues.SprintState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by Ext_EdG on 7/16/2020.
 */
public class ProjectMonitoringMisc {
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
    public void WriteToStatus(StringBuilder statusText, boolean debug, String text)
    {
        if (debug) statusText.append(text + "<br>");
    }
}
