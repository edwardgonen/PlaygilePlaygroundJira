package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.jiraissues.JiraInterface;

import java.util.ArrayList;
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
}
