package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileIssue;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by Ext_EdG on 11/19/2020.
 */
public class RoadmapFeatureAnalysis {
    Issue roadmapFeature;
    JiraInterface jiraInterface;
    ApplicationUser applicationUser;
    Project currentProject;
    ProjectMonitoringMisc projectMonitoringMisc;
    double[] overallIssuesDistributionInSprint = new double[ProjectMonitor.DISTRIBUTION_SIZE];

    /////////// publics
    public StringBuilder logText;
    public String messageToDisplay;
    public boolean bProcessed = false;
    public ArrayList<PlaygileSprint> playgileSprints = new ArrayList<>();


    public RoadmapFeatureAnalysis(
        Issue roadmapFeature,
        JiraInterface jiraInterface,
        ApplicationUser applicationUser,
        Project currentProject,
        ProjectMonitoringMisc projectMonitoringMisc
    )
    {
        this.roadmapFeature = roadmapFeature;
        this.jiraInterface = jiraInterface;
        this.applicationUser = applicationUser;
        this.currentProject = currentProject;
        this.projectMonitoringMisc = projectMonitoringMisc;
    }

    public boolean analyzeRoadmapFeature()
    {
        boolean result = false;


        //get list of issues and convert them to PlaygileIssues

        List<PlaygileIssue> playgileIssues = new ArrayList<>();
        List<Issue> issues = jiraInterface.getIssuesForRoadmapFeature(logText, applicationUser, currentProject, roadmapFeature);
        if (null != issues && issues.size() > 0) {

            for (Issue issue : issues) {
                PlaygileIssue playgileIssue = new PlaygileIssue(issue, projectMonitoringMisc);
                playgileIssue.instantiatePlaygileIssue();
                //get sprints for issue
                projectMonitoringMisc.addIssueSprintsToList(issue, playgileSprints);

                playgileIssues.add(playgileIssue);
            }



            //now we have everything in the cache - list of instantiated issues
            //let's process
            //sort sprints
            Collections.sort(playgileSprints); //sort by dates

        }
        else
        {
            logText.append("Failed to retrieve any project issues for " + roadmapFeature.getSummary());
            result = false;
            messageToDisplay = "Failed to retrieve any project's issues for Roadmap Feature" +
                ". Please make sure the Roadmap Feature has the right structure (epics, linked epics etc.)";
            return result;
        }

        return result;
    }
}
