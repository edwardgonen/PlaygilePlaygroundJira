
/**
 * Created by Ext_EdG on 11/22/2020.
 */
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
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.projectprogress.ProjectProgress;

import java.util.*;


@Scanned
public class TotalViewImpl implements com.playgileplayground.jira.api.TotalView, ContextProvider {
    @ComponentImport
    private final UserProjectHistoryManager userProjectHistoryManager;
    @ComponentImport
    private final ActiveObjects ao;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;


    public TotalViewImpl(UserProjectHistoryManager userProjectHistoryManager,
                         ProjectManager projectManager,
                         ActiveObjects ao,
                         SearchService searchService) {
        this.userProjectHistoryManager = userProjectHistoryManager;
        this.ao = ao;
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
        String messageToDisplay = "";
        boolean bAllisOk;
        activeRoadmapFeatures = new ArrayList<>();
        inactiveRoadmapFeatures = new ArrayList<>();

        StatusText.getInstance().reset();

        JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
        String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        contextMap.put(BASEURL, baseUrl);
        ApplicationUser applicationUser = jac.getLoggedInUser();
        contextMap.put(CURRENTUSER, applicationUser.getKey());

        JiraInterface jiraInterface = new JiraInterface(applicationUser, searchService);

        ManageActiveObjects mao = new ManageActiveObjects(this.ao);

        //get the current project
        Project currentProject = projectManager.getProjectByCurrentKey((String) map.get("projectKey"));
        ProjectMonitoringMisc projectMonitoringMisc = new ProjectMonitoringMisc(jiraInterface, applicationUser, currentProject, mao);

        try {
            //start the real work
            // do we have the current project?
            if (currentProject == null) //no current project found - exit
            {
                //no current project
                StatusText.getInstance().add( false, "No current project found");
                bAllisOk = false;
                messageToDisplay = "Cannot identify current project. Please try to reload this page";
                return projectMonitoringMisc.returnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            } else
            {
                contextMap.put(PROJECT, currentProject);
                /*
                //get list of roadmap features
                List<Issue> roadmapFeatures = jiraInterface.getRoadmapFeaturesNotCancelledAndNotGoLiveAndNotOnHold(applicationUser, currentProject, ProjectMonitor.ROADMAPFEATUREKEY);
                if (roadmapFeatures != null && roadmapFeatures.size() > 0)
                {
                    StatusText.getInstance().add( false, "Found roadmap features total " + roadmapFeatures.size());
                }
                else
                {
                    StatusText.getInstance().add( false, "No roadmap feature found for project");
                    bAllisOk = false;
                    messageToDisplay = "Failed to retrieve a list of Roadmap Features for the project. Please create the Roadmap Features";
                    return projectMonitoringMisc.returnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                }

                for (Issue roadmapFeature : roadmapFeatures) {

                    /////////////////////// we are ready to analyze the roadmap feature /////////////////////////////////////////////
                    RoadmapFeatureAnalysis roadmapFeatureAnalysis = new RoadmapFeatureAnalysis(
                        roadmapFeature,
                        jiraInterface,
                        applicationUser,
                        currentProject,
                        projectMonitoringMisc,
                        mao);
                    if (roadmapFeatureAnalysis.analyzeRoadmapFeature()) {
                        if (roadmapFeatureAnalysis.isRoadmapFeatureStarted()) {
                            //Roadmap feature is active
                            StatusText.getInstance().add(true, "Found active Roadmap feature " + roadmapFeatureAnalysis.getRoadmapFeatureKeyAndSummary());
                            activeRoadmapFeatures.add(roadmapFeatureAnalysis);



                        } else //not active
                        {
                            //add to the list of inactive features
                            inactiveRoadmapFeatures.add(roadmapFeatureAnalysis);
                        }
                    }
                    else
                    {
                        messageToDisplay = "Failed to analyze roadmap feature " + roadmapFeature.getKey() + " " + roadmapFeature.getSummary();
                        StatusText.getInstance().add(true, messageToDisplay);
                    }




                }//loop by features

                //prepare for web
                prepareDataForWeb(contextMap);
*/
                StatusText.getInstance().add(false, "Exiting successfully");
                bAllisOk = true;
                return projectMonitoringMisc.returnContextMapToVelocityTemplate(contextMap, bAllisOk, "");
            }
        } catch (Exception e) {
            String trace = projectMonitoringMisc.getExceptionTrace(e);
            StatusText.getInstance().add(true, "Main route exception " + trace);
            bAllisOk = false;
            messageToDisplay = "General code failure in Total View. Please check the log";
            return projectMonitoringMisc.returnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
        }
    }

    public void prepareDataForWeb(Map<String, Object> contextMap) {
        Collections.sort(activeRoadmapFeatures);
        //convert to strings for the web
        StringBuilder featuresRows = new StringBuilder();
        for (RoadmapFeatureAnalysis rfd : activeRoadmapFeatures) {
                double statusScore = rfd.qualityScore;
                featuresRows.append(
                    //name
                    rfd.roadmapFeature.getSummary() + ManageActiveObjects.PAIR_SEPARATOR +
                        //status
                        statusScore * 100.0 + ManageActiveObjects.PAIR_SEPARATOR +
                        //start date
                        DateTimeUtils.ConvertDateToOurFormat(rfd.startDateRoadmapFeature) + ManageActiveObjects.PAIR_SEPARATOR +
                        //predicted velocity
                        rfd.plannedRoadmapFeatureVelocity + ManageActiveObjects.PAIR_SEPARATOR +
                        //real project velocity
                        rfd.predictedVelocity + ManageActiveObjects.PAIR_SEPARATOR +
                        //predicted end date
                        DateTimeUtils.ConvertDateToOurFormat(rfd.projectProgressResult.idealProjectEnd) + ManageActiveObjects.PAIR_SEPARATOR +
                        //real end date
                        DateTimeUtils.ConvertDateToOurFormat(rfd.projectProgressResult.predictedProjectEnd) + ManageActiveObjects.PAIR_SEPARATOR +

                        rfd.analyzedStories.NotEstimatedStoriesNumber + ManageActiveObjects.PAIR_SEPARATOR +
                        rfd.analyzedStories.LargeStoriesNumber + ManageActiveObjects.PAIR_SEPARATOR +
                        rfd.analyzedStories.VeryLargeStoriesNumber + ManageActiveObjects.PAIR_SEPARATOR +
                        rfd.analyzedStories.EstimatedStoriesNumber + ManageActiveObjects.PAIR_SEPARATOR +
                        ProjectProgress.convertColorToHexadeimal(rfd.projectProgressResult.progressDataColor) +
                        "BOBRUISK"
                );
        }
        contextMap.put(FEATURESROWS, featuresRows.toString());
    }

}
