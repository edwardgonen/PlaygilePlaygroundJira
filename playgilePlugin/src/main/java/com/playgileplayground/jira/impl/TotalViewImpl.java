package com.playgileplayground.jira.impl;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.ContextProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Scanned
public class TotalViewImpl implements com.playgileplayground.jira.api.TotalView, ContextProvider {
    @ComponentImport
    private final UserProjectHistoryManager userProjectHistoryManager;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;


    public TotalViewImpl(UserProjectHistoryManager userProjectHistoryManager,
                         ProjectManager projectManager,
                         SearchService searchService) {
        this.userProjectHistoryManager = userProjectHistoryManager;
        this.projectManager = projectManager;
        this.searchService = searchService;
    }

    ArrayList<RoadmapFeatureAnalysis> activeRoadmapFeatures;
    ArrayList<RoadmapFeatureAnalysis> inactiveRoadmapFeatures;

    @Override
    public Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        return null;
    }

    @Override
    public void init(Map<String, String> map) throws PluginParseException {
    }
    @Override
    public UserProjectHistoryManager getUserProjectHistoryManager() {
        return this.userProjectHistoryManager;
    }

    @Override
    public Map getContextMap(Map<String, Object> map) {
        Map<String, Object> contextMap = new HashMap<>();
        String messageToDisplay;
        activeRoadmapFeatures = new ArrayList<>();
        inactiveRoadmapFeatures = new ArrayList<>();

        StatusText.getInstance().reset();

        JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser applicationUser = jac.getLoggedInUser();
        if (applicationUser == null)
        {
            StatusText.getInstance().add( false, "User Authentication Failure");
            messageToDisplay = "User Authentication Failure";
            return ProjectMonitoringMisc.returnContextMapToVelocityTemplate(contextMap, false, messageToDisplay);
        }
        String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        contextMap.put(BASEURL, baseUrl);
        contextMap.put(CURRENTUSER, applicationUser.getKey());


        //get the current project
        Project currentProject = projectManager.getProjectByCurrentKey((String) map.get("projectKey"));
        try {
            //start the real work
            // do we have the current project?
            if (currentProject == null) //no current project found - exit
            {
                //no current project
                StatusText.getInstance().add( false, "No current project found");
                messageToDisplay = "Cannot identify current project. Please try to reload this page";
                return ProjectMonitoringMisc.returnContextMapToVelocityTemplate(contextMap, false, messageToDisplay);
            } else
            {
                contextMap.put(PROJECT, currentProject);
                StatusText.getInstance().add(false, "Exiting successfully");
                return ProjectMonitoringMisc.returnContextMapToVelocityTemplate(contextMap, true, "");
            }
        } catch (Exception e) {
            String trace = ProjectMonitoringMisc.getExceptionTrace(e);
            StatusText.getInstance().add(true, "Main route exception " + trace);
            messageToDisplay = "General code failure in Total View. Please check the log";
            return ProjectMonitoringMisc.returnContextMapToVelocityTemplate(contextMap, false, messageToDisplay);
        }
    }
}
