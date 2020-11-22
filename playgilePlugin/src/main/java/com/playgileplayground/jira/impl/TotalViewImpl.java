
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
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;
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

        double projectVelocity = 0;
        String messageToDisplay = "";
        boolean bAllisOk;
        ManageActiveObjectsResult maor;
        ArrayList<RoadmapFeatureDescriptor> roadmapFeatureDescriptors = new ArrayList<>();
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
                    }




                }//loop by features

                //prepare for web
                prepareDataForWeb(contextMap);

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
                double statusScore = getStatusScore(rfd);
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
    private double getStatusScore(RoadmapFeatureAnalysis rfd)
    {
        double result = 0;

        double VELOCITY_DIFFERENCT_PART = 0.1;
        double COMPLETION_DATE_DIFFERENCE_PART = 0.50;
        double ESTIMATED_STORIES_DIFFERENCE_PART = 0.40;
        //total must be 0 - 1

        /*
        Estimated percentage 0 - 1
        0 - <=0.25     0.05
        >0.25 - <= 0.5 0.15
        > 0.5 <= 0.8   0.50
        > 0.8 <= 0.9   0.80
        >0.9           1.00


        (real date - predicted date) / sprint length
        <= 1,      1.0
        > 1- <= 2, 0.5
        > 2-..     0.1

        predicted velocity / real velocity percentage (0  - 1)
        0 - <=0.25     0.10
        >0.25 - <= 0.5 0.20
        > 0.5 <= 0.8   0.50
        > 0.8 <= 0.9   0.80
        > 0.9          1

        */
        //veloctiy difference impact
        double velocityDifference = rfd.plannedRoadmapFeatureVelocity / rfd.predictedVelocity;
        double veloctiyDifferenceImpact = 1.0;
        if (velocityDifference <= 0.25) veloctiyDifferenceImpact = 0.10;
        else if (velocityDifference <= 0.5) veloctiyDifferenceImpact = 0.20;
        else if (velocityDifference <= 0.8) veloctiyDifferenceImpact = 0.50;
        else if (velocityDifference <= 0.9) veloctiyDifferenceImpact = 0.80;

        //completion date difference impact
        int completionDateDifference = DateTimeUtils.Days(rfd.projectProgressResult.predictedProjectEnd, rfd.projectProgressResult.idealProjectEnd) / (int)rfd.sprintLengthRoadmapFeature;
        double completionDateDifferenceImpact = 1.0;
        if (completionDateDifference > 2) completionDateDifferenceImpact = 0.1;
        else if (completionDateDifference > 1) completionDateDifferenceImpact = 0.5;

        //estimations impact
        double totalIssues = rfd.analyzedStories.EstimatedStoriesNumber + rfd.analyzedStories.NotEstimatedStoriesNumber +
            rfd.analyzedStories.VeryLargeStoriesNumber + rfd.analyzedStories.LargeStoriesNumber;
        double estimationRatio = rfd.analyzedStories.EstimatedStoriesNumber / totalIssues;
        double estimationRatioImpact = 1.0;
        if (estimationRatio <= 0.25) estimationRatioImpact = 0.05;
        else if (estimationRatio <= 0.5) estimationRatioImpact = 0.15;
        else if (estimationRatio <= 0.8) estimationRatioImpact = 0.50;
        else if (estimationRatio <= 0.9) estimationRatioImpact = 0.80;


        result = VELOCITY_DIFFERENCT_PART * veloctiyDifferenceImpact +
            COMPLETION_DATE_DIFFERENCE_PART * completionDateDifferenceImpact +
            ESTIMATED_STORIES_DIFFERENCE_PART * estimationRatioImpact;


        return result;
    }

}
