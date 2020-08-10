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
import com.playgileplayground.jira.api.TotalView;
import com.playgileplayground.jira.persistence.*;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.persistence.*;
import com.playgileplayground.jira.projectprogress.DataPair;
import com.playgileplayground.jira.projectprogress.ProgressData;
import com.playgileplayground.jira.projectprogress.ProjectProgress;
import com.playgileplayground.jira.projectprogress.ProjectProgressResult;

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


    public StringBuilder statusText = new StringBuilder();

    public TotalViewImpl(UserProjectHistoryManager userProjectHistoryManager,
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
        double projectVelocity = 0;
        String messageToDisplay = "";
        boolean bAllisOk;
        ManageActiveObjectsResult maor;
        ArrayList<RoadmapFeatureDescriptor> roadmapFeatureDescriptors = new ArrayList<>();

        Issue selectedRoadmapFeatureIssue;

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
            List<Issue> roadmapFeatures = jiraInterface.getRoadmapFeaturesNotCancelledAndNotGoLive(applicationUser, currentProject, ProjectMonitor.ROADMAPFEATUREKEY);

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

                    //do we have any issues in the Roadmap feature?
                    List<Issue> issues = jiraInterface.getIssuesForRoadmapFeature(statusText, applicationUser, currentProject, roadmapFeature);
                    if (null != issues && issues.size() > 0)
                    {
                        //do all our stuff
                        ArrayList<Issue> foundIssues = new ArrayList<>();
                        ArrayList<PlaygileSprint> playgileSprints = new ArrayList<>();

                        //get list of sprints out of user stories.
                        projectMonitoringMisc.getNotCompletedIssuesAndAndSprints(issues, foundIssues, playgileSprints, statusText);
                        //sort sprints
                        Collections.sort(playgileSprints); //sort by dates
                        //did we find any matching issues? - i.e. not completed
                        if (foundIssues.size() > 0 && playgileSprints.size() > 0) {
                            //the project is ok. Let's see if it has started yet
                            //get the start flag from AO
                            maor = mao.GetProjectStartedFlag(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
                            if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                            {
                                boolean projectStarted = (boolean)maor.Result;
                                if (!projectStarted) //not started
                                {
                                    projectMonitoringMisc.WriteToStatus(statusText, false,"Project start flag is false");
                                    //let's find out if the project has started
                                    //we should have a list of sprints
                                    if (playgileSprints.size() > 0) {
                                        projectMonitoringMisc.WriteToStatus(statusText, false,"Valid sprints " + playgileSprints.size());
                                        //the first sprint startDate would be the project start date
                                        PlaygileSprint sprint = playgileSprints.iterator().next(); //first
                                        roadmapFeatureDescriptor.StartDate = sprint.getStartDate();
                                        //also get the sprint length
                                        roadmapFeatureDescriptor.SprintLength = ProjectProgress.AbsDays(sprint.getStartDate(), sprint.getEndDate()) + 1;
                                        //set to AO entity - project started, start date and sprint length
                                        roadmapFeatureDescriptor.Status = TotalViewMisc.FeatureStatus.STARTED;
                                        maor = mao.SetProjectStartedFlag(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()), true);
                                        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                                        {
                                            projectMonitoringMisc.WriteToStatus(statusText, false,"Project start flag is set to true. Setting start date");
                                            maor = mao.SetProjectStartDate(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()), roadmapFeatureDescriptor.StartDate);
                                            maor = mao.SetSprintLength(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()), roadmapFeatureDescriptor.SprintLength);
                                        }
                                    }
                                    else //no active or closed sprints at all - not started yet
                                    {
                                        roadmapFeatureDescriptor.Status = TotalViewMisc.FeatureStatus.NOT_STARTED;
                                    }
                                }
                                else //project started - just set the flag to descriptor
                                {
                                    roadmapFeatureDescriptor.Status = TotalViewMisc.FeatureStatus.STARTED;
                                }

                                //now calculate all the stuff
                                if (roadmapFeatureDescriptor.Status == TotalViewMisc.FeatureStatus.STARTED)
                                {
                                    maor = mao.GetProjectStartDate(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
                                    roadmapFeatureDescriptor.StartDate = (Date)maor.Result;
                                    //get the sprint length
                                    roadmapFeatureDescriptor.SprintLength = projectMonitoringMisc.getSprintLength(mao, currentProject, roadmapFeature.getSummary());
                                    //now let's calculate the remaining story points
                                    double currentEstimation = projectMonitoringMisc.getCurrentEstimations(foundIssues, statusText);
                                    //after calculation
                                    //1. set initial estimation if previousProjectStartFlag is false
                                    //get the initial estimations
                                    //let's try to get initial estimation in the right way. Comment it out if not working
                                    double initialEstimation = projectMonitoringMisc.getInitialEstimation(issues, roadmapFeatureDescriptor.StartDate, statusText);
                                    maor = mao.SetProjectInitialEstimation(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()), roadmapFeatureDescriptor.StartDate, initialEstimation);

                                    //2. add current estimation to the list of estimations
                                    //tmpDate = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT).parse("6/23/2020");
                                    Date timeStamp = Calendar.getInstance().getTime();
                                    projectMonitoringMisc.WriteToStatus(statusText, false,"Current time to add to list " + timeStamp);
                                    maor = mao.AddRemainingEstimationsRecord(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()), timeStamp, currentEstimation);

                                    //get real velocities
                                    //fill real sprint velocity

                                    Collection<PlaygileSprint> allRealSprints = projectMonitoringMisc.getAllRealSprintsVelocities(playgileSprints,
                                        roadmapFeatureDescriptor.StartDate,
                                        roadmapFeatureDescriptor.TeamVelocity,
                                        (int)roadmapFeatureDescriptor.SprintLength, statusText);
                                    //linear regression
                                    ArrayList<Double> predictedVelocities;
                                    predictedVelocities = projectMonitoringMisc.getLinearRegressionForRealSprintVelocities(allRealSprints, roadmapFeatureDescriptor.StartDate, statusText);

                                    //averaging
                                    //predictedVelocities = projectMonitoringMisc.getAverageForRealSprintVelocities(allRealSprints, roadmapFeatureDescriptor.StartDate, statusText);

                                    //and the average is
                                    //projectVelocity = projectMonitoringMisc.getAverageProjectRealVelocity(allRealSprints, teamVelocity, statusText);
                                    projectVelocity = (int)Math.round(predictedVelocities.get(predictedVelocities.size() - 1));
                                    if (projectVelocity <= 0) {
                                        projectVelocity = roadmapFeatureDescriptor.TeamVelocity;
                                    }
                                    roadmapFeatureDescriptor.ProjectVelocity = projectVelocity;
                                }
                            }

                            /// continue calculations //////

                            //estimated stories
                            AnalyzedStories analysis = projectMonitoringMisc.getStoriesAnalyzed(issues);
                            roadmapFeatureDescriptor.EstimatedStories = analysis;


                            maor = mao.GetProgressDataList(new ManageActiveObjectsEntityKey(currentProject.getKey(), roadmapFeature.getSummary()));
                            if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                                ProjectProgress projectProgress = new ProjectProgress();
                                ProjectProgressResult ppr = projectProgress.Initiate(roadmapFeatureDescriptor.TeamVelocity, roadmapFeatureDescriptor.ProjectVelocity,
                                    (int)roadmapFeatureDescriptor.SprintLength, (ArrayList<DataPair>) maor.Result);

                                roadmapFeatureDescriptor.IdealEndOfProjet = ppr.idealProjectEnd;
                                roadmapFeatureDescriptor.PredictedEndOfProjet = ppr.predictedProjectEnd;
                                roadmapFeatureDescriptor.ProgressDataColor = ppr.progressDataColor;
                            }
                        }
                        else
                        {
                            roadmapFeatureDescriptor.Status = TotalViewMisc.FeatureStatus.NO_OPEN_ISSUES;
                        }
                    }
                    else
                    {
                        //not our structure. Just go to the next one
                        continue;
                    }
                    roadmapFeatureDescriptors.add(roadmapFeatureDescriptor);
                }

                //convert to strings for the web
                StringBuilder featuresRows = new StringBuilder();
                for (RoadmapFeatureDescriptor rfd : roadmapFeatureDescriptors) {
                    if (rfd.Status == TotalViewMisc.FeatureStatus.STARTED)
                    {
                        double statusScore = getStatusScore(rfd);
                        featuresRows.append(
                            //name
                            rfd.Name + ManageActiveObjects.PAIR_SEPARATOR +
                                //status
                                statusScore * 100.0 + ManageActiveObjects.PAIR_SEPARATOR +
                                //start date
                                projectMonitoringMisc.ConvertDateToOurFormat(rfd.StartDate) + ManageActiveObjects.PAIR_SEPARATOR +
                                //predicted velocity
                                rfd.TeamVelocity + ManageActiveObjects.PAIR_SEPARATOR +
                                //real project velocity
                                rfd.ProjectVelocity + ManageActiveObjects.PAIR_SEPARATOR +
                                //predicted end date
                                projectMonitoringMisc.ConvertDateToOurFormat(rfd.IdealEndOfProjet) + ManageActiveObjects.PAIR_SEPARATOR +
                                //real end date
                                projectMonitoringMisc.ConvertDateToOurFormat(rfd.PredictedEndOfProjet) + ManageActiveObjects.PAIR_SEPARATOR +

                                rfd.EstimatedStories.NotEstimatedStoriesNumber + ManageActiveObjects.PAIR_SEPARATOR +
                                rfd.EstimatedStories.LargeStoriesNumber + ManageActiveObjects.PAIR_SEPARATOR +
                                rfd.EstimatedStories.VeryLargeStoriesNumber + ManageActiveObjects.PAIR_SEPARATOR +
                                rfd.EstimatedStories.EstimatedStoriesNumber + ManageActiveObjects.PAIR_SEPARATOR +
                                ProjectProgress.convertColorToHexadeimal(rfd.ProgressDataColor) +
                                "BOBRUISK"
                        );
                    }
                    else
                    {
                        projectMonitoringMisc.WriteToStatus(statusText, true, rfd.Name +  " Feature not started or no open issues");
                    }
                }

                contextMap.put(FEATURESROWS, featuresRows.toString());

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
    private double getStatusScore(RoadmapFeatureDescriptor rfd)
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
        double velocityDifference = rfd.TeamVelocity / rfd.ProjectVelocity;
        double veloctiyDifferenceImpact = 1.0;
        if (velocityDifference <= 0.25) veloctiyDifferenceImpact = 0.10;
        else if (velocityDifference <= 0.5) veloctiyDifferenceImpact = 0.20;
             else if (velocityDifference <= 0.8) veloctiyDifferenceImpact = 0.50;
                  else if (velocityDifference <= 0.9) veloctiyDifferenceImpact = 0.80;

        //completion date difference impact
        int completionDateDifference = ProjectProgress.Days(rfd.PredictedEndOfProjet, rfd.IdealEndOfProjet) / 14;
        double completionDateDifferenceImpact = 1.0;
        if (completionDateDifference > 2) completionDateDifferenceImpact = 0.1;
        else if (completionDateDifference > 1) completionDateDifferenceImpact = 0.5;

        //estimations impact
        double totalIssues = rfd.EstimatedStories.EstimatedStoriesNumber + rfd.EstimatedStories.NotEstimatedStoriesNumber +
            rfd.EstimatedStories.VeryLargeStoriesNumber + rfd.EstimatedStories.LargeStoriesNumber;
        double estimationRatio = rfd.EstimatedStories.EstimatedStoriesNumber / totalIssues;
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
