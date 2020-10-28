package com.playgileplayground.jira.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
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

import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.persistence.*;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.projectprogress.DataPair;
import com.playgileplayground.jira.projectprogress.ProjectProgress;
import com.playgileplayground.jira.projectprogress.ProjectProgressResult;

import java.util.*;


@Scanned
public class ProjectPreparationImpl implements com.playgileplayground.jira.api.TotalView, ContextProvider {
    @ComponentImport
    private final UserProjectHistoryManager userProjectHistoryManager;
    @ComponentImport
    private final ActiveObjects ao;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;


    public StringBuilder statusText = new StringBuilder();

    public ProjectPreparationImpl(UserProjectHistoryManager userProjectHistoryManager,
                              ProjectManager projectManager,
                              ActiveObjects ao,
                              SearchService searchService){
        this.userProjectHistoryManager = userProjectHistoryManager;
        this.ao = ao;
        this.projectManager = projectManager;
        this.searchService = searchService;
    }
    @Override
    public Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        return null;
    }

    @Override
    public void init(Map<String, String> map) throws PluginParseException {
    }

    @Override
    public Map getContextMap(Map<String, Object> map) {
        Map<String, Object> contextMap = new HashMap<>();
        statusText = new StringBuilder();
        String messageToDisplay = "";
        boolean bAllisOk;
        ManageActiveObjectsResult maor;
        ArrayList<RoadmapFeatureDescriptor> roadmapFeatureDescriptors = new ArrayList<>();


        JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
        String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        contextMap.put(BASEURL, baseUrl);
        ApplicationUser applicationUser = jac.getLoggedInUser();
        contextMap.put(CURRENTUSER, applicationUser.getKey());
        contextMap.put(MAINJAVACLASS, this);

        JiraInterface jiraInterface = new JiraInterface(applicationUser,  searchService);

        ManageActiveObjects mao = new ManageActiveObjects(this.ao);

        //get the current project
        Project currentProject = projectManager.getProjectByCurrentKey((String) map.get("projectKey"));
        //start the real work
        if(null != currentProject) {
            contextMap.put(PROJECT, currentProject);
            ProjectMonitoringMisc projectMonitoringMisc = new ProjectMonitoringMisc(jiraInterface, applicationUser, currentProject);
            //get list of roadmap features
            List<Issue> roadmapFeatures = jiraInterface.getRoadmapFeaturesNotCancelledAndNotGoLiveAndNotOnHold(applicationUser, currentProject, ProjectMonitor.ROADMAPFEATUREKEY);

            if (roadmapFeatures != null && roadmapFeatures.size() > 0)
            {
                for (Issue roadmapFeature : roadmapFeatures)
                {
                    RoadmapFeatureDescriptor roadmapFeatureDescriptor = new RoadmapFeatureDescriptor();
                    //set name
                    roadmapFeatureDescriptor.Name = roadmapFeature.getSummary();
                    //get team velocity
                    maor = mao.GetTeamVelocity(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeatureDescriptor.Name));
                    if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                        roadmapFeatureDescriptor.TeamVelocity = (double)maor.Result;
                        if (roadmapFeatureDescriptor.TeamVelocity <= 0) roadmapFeatureDescriptor.TeamVelocity = DEFAULT_TEAM_VELOCITY;
                    }
                    else
                    {
                        projectMonitoringMisc.WriteToStatus(statusText, false, "Velocity is not set");
                        //team velocity is not set. So use 50. just for fun
                        roadmapFeatureDescriptor.TeamVelocity = DEFAULT_TEAM_VELOCITY;
                    }

                    roadmapFeatureDescriptors.add(roadmapFeatureDescriptor);
                }


                bAllisOk = true;
            }
            else //no suitable roadmap features found
            {
                bAllisOk = false;
                messageToDisplay = "No suitable Roadmap Features detected";
            }
        }
        else
        {
            messageToDisplay = "Failed to retrieve the current project";
            bAllisOk = false;
        }

        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
    }
    @Override
    public UserProjectHistoryManager getUserProjectHistoryManager() {
        return this.userProjectHistoryManager;
    }
    private Map<String, Object> ReturnContextMapToVelocityTemplate(Map<String, Object> contextMap, boolean bAllisOk, String messageToDisplay)
    {
        contextMap.put(ALLISOK, bAllisOk);
        contextMap.put(MESSAGETODISPLAY, messageToDisplay);
        contextMap.put(STATUSTEXT, statusText.toString());
        return contextMap;
    }
}
