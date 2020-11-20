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
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileIssue;
import com.playgileplayground.jira.persistence.*;

import java.util.*;


@Scanned
public class ProjectMonitorImpl implements com.playgileplayground.jira.api.ProjectMonitor, ContextProvider {
    @ComponentImport
    private final UserProjectHistoryManager userProjectHistoryManager;
    @ComponentImport
    private final ActiveObjects ao;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;

    public StringBuilder statusText = new StringBuilder();

    public ProjectMonitorImpl(UserProjectHistoryManager userProjectHistoryManager,
                              ProjectManager projectManager,
                              ActiveObjects ao,
                              SearchService searchService) {
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
    public UserProjectHistoryManager getUserProjectHistoryManager() {
        return this.userProjectHistoryManager;
    }

    /////////////////////// new code ////////////////////////////
    @Override
    public Map getContextMap(Map<String, Object> map) {
        Map<String, Object> contextMap = new HashMap<>();
        String messageToDisplay;
        boolean bAllisOk;
        List<Issue> roadmapFeatures;

        //////////////////// values taken from configuration //////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////

        double initialRoadmapFeatureVelocity = 0;
        String selectedRoadmapFeature;
        ManageActiveObjectsResult maor;
        Issue selectedRoadmapFeatureIssue;
        ManageActiveObjects mao = new ManageActiveObjects(this.ao);
        JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
        String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        contextMap.put(BASEURL, baseUrl);
        ApplicationUser applicationUser = jac.getLoggedInUser();
        contextMap.put(CURRENTUSER, applicationUser.getKey());

        JiraInterface jiraInterface = new JiraInterface(applicationUser,  searchService);
        Project currentProject = projectManager.getProjectByCurrentKey((String) map.get("projectKey"));
        ProjectMonitoringMisc projectMonitoringMisc = new ProjectMonitoringMisc(jiraInterface, applicationUser, currentProject, mao);

        try { //main try/catch

            // do we have the current project?
            if (currentProject == null) //no current project found - exit
            {
                //no current project
                StatusText.getInstance().add(statusText, false, "No current project found");
                bAllisOk = false;
                messageToDisplay = "Cannot identify current project. Please try to reload this page";
                return returnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }

            //let's get all roadmap features and for each do analysis
            //first set the list
            //do we have any roadmap defined?
            //roadmapFeatures = jiraInterface.getAllRoadmapFeatures(applicationUser, currentProject, ROADMAPFEATUREKEY);
            roadmapFeatures = jiraInterface.getRoadmapFeaturesNotCancelledAndNotGoLiveAndNotOnHold(applicationUser, currentProject, ROADMAPFEATUREKEY);
            ArrayList<String> roadmapFeaturesNames = projectMonitoringMisc.getAllRoadmapFeatureNames(roadmapFeatures);
            if (roadmapFeatures.size() > 0)
            {
                StatusText.getInstance().add(statusText, false, "Found roadmap features total " + roadmapFeaturesNames.size());
                contextMap.put(ROADMAPFEATURESLIST, roadmapFeaturesNames);
            }
            else
            {
                StatusText.getInstance().add(statusText, false, "No roadmap feature found for project");
                bAllisOk = false;
                messageToDisplay = "Failed to retrieve a list of Roadmap Features for the project. Please create the Roadmap Features";
                return returnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }
            //we got all roadmap features

            //try to read from AO for this user
            maor = mao.GetUserLastLocations(applicationUser.getKey());
            if (maor.Code != ManageActiveObjectsResult.STATUS_CODE_SUCCESS) //not found
            {
                //give a message and ask to recalculate
                StatusText.getInstance().add(statusText, false, "Failed to retrieve any project issues - no selected roadmap feature");
                bAllisOk = false;
                messageToDisplay = "Please select a Roadmap Feature from the list and press Recalculate (also check if the Team's velocity is not 0)";
                return returnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }

            //user found
            UserLastLocations userLastLocations = (UserLastLocations)maor.Result;
            selectedRoadmapFeature = userLastLocations.lastRoadmapFeature;
            contextMap.put(SELECTEDROADMAPFEATURE, selectedRoadmapFeature);
            selectedRoadmapFeatureIssue = projectMonitoringMisc.SearchSelectedIssue(roadmapFeatures, selectedRoadmapFeature);
            if (selectedRoadmapFeatureIssue == null) //not found
            {
                StatusText.getInstance().add(statusText, false, "our selected feature is not in the list " + selectedRoadmapFeature);
                bAllisOk = false;
                messageToDisplay = "Please select a Roadmap Feature";
                return returnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }
            if (selectedRoadmapFeature.isEmpty())
            {
                StatusText.getInstance().add(statusText, false, "Roadmap feature is not selected");
                bAllisOk = false;
                messageToDisplay = "Please select the Roadmap Feature";
                return returnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }
            StatusText.getInstance().add(statusText, false, "First AO that matches project is found");

            /////////////////////// we are ready to analyze the roadmap feature /////////////////////////////////////////////
            RoadmapFeatureAnalysis roadmapFeatureAnalysis = new RoadmapFeatureAnalysis(
                    selectedRoadmapFeatureIssue,
                    jiraInterface,
                    applicationUser,
                    currentProject,
                    projectMonitoringMisc,
                    mao);
            roadmapFeatureAnalysis.analyzeRoadmapFeature();

            if (roadmapFeatureAnalysis.isRoadmapFeatureStarted()) {
                //Roadmap feature is active
                StatusText.getInstance().add(statusText, true, "Active Roadmap feature " + roadmapFeatureAnalysis.getRoadmapFeatureKeyAndSummary());





                StatusText.getInstance().add(statusText, false, "Exiting successfully");
                bAllisOk = true;
                return returnContextMapToVelocityTemplate(contextMap, bAllisOk, "");
            }
            else //not active
            {
                StatusText.getInstance().add(statusText, false, "Roadmap feature has not started yet");
                bAllisOk = false;
                messageToDisplay = "Selected Roadmap feature has not started yet";
                return returnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }
        }
        catch (Exception e)
        {
            StatusText.getInstance().add(statusText, true, "Main route exception " + e);
            bAllisOk = false;
            messageToDisplay = "General code failure. Please check the log";
            return returnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
        }
    }

    //////////////////////////////////////////////////////////////

    private Map<String, Object> returnContextMapToVelocityTemplate(Map<String, Object> contextMap, boolean bAllisOk, String messageToDisplay)
    {
        contextMap.put(ALLISOK, bAllisOk);
        contextMap.put(MESSAGETODISPLAY, messageToDisplay);
        contextMap.put(STATUSTEXT, statusText.toString());
        return contextMap;
    }
}
