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
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.persistence.*;
import com.playgileplayground.jira.projectprogress.DataPair;
import com.playgileplayground.jira.projectprogress.ProgressData;
import com.playgileplayground.jira.projectprogress.ProjectProgress;
import com.playgileplayground.jira.projectprogress.ProjectProgressResult;

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

    List<Issue> issues;
    public StringBuilder statusText = new StringBuilder();

    public ProjectMonitorImpl(UserProjectHistoryManager userProjectHistoryManager,
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
        double teamVelocity = 0;
        double projectVelocity = 0;
        String selectedRoadmapFeature;
        List<Issue> roadmapFeatures;
        String messageToDisplay = "";
        boolean bAllisOk;
        Date startDate;
        double sprintLength;
        double[] overallIssuesDistributionInSprint = new double[ProjectMonitor.DISTRIBUTION_SIZE];
        ManageActiveObjectsResult maor;
        Issue selectedRoadmapFeatureIssue;
        ManageActiveObjects mao = new ManageActiveObjects(this.ao);
        JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
        String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        contextMap.put(BASEURL, baseUrl);
        ApplicationUser applicationUser = jac.getLoggedInUser();
        contextMap.put(CURRENTUSER, applicationUser.getKey());


        Project currentProject = projectManager.getProjectByCurrentKey((String) map.get("projectKey"));
        contextMap.put(MAINJAVACLASS, this);
        JiraInterface jiraInterface = new JiraInterface(this, applicationUser,  searchService);
        ProjectMonitoringMisc projectMonitoringMisc = new ProjectMonitoringMisc(jiraInterface, applicationUser, currentProject);

        //start the real work
        if(null != currentProject) {
            projectMonitoringMisc.WriteToStatus(statusText, false,"Got current project " + currentProject.getName() + " key " + currentProject.getKey());
            contextMap.put(PROJECT, currentProject);

            //first set the list
            //do we have any roadmap defined?
            //roadmapFeatures = jiraInterface.getAllRoadmapFeatures(applicationUser, currentProject, ROADMAPFEATUREKEY);
            roadmapFeatures = jiraInterface.getRoadmapFeaturesNotCancelledAndNotGoLive(applicationUser, currentProject, ROADMAPFEATUREKEY);
            ArrayList<String> roadmapFeaturesNames = projectMonitoringMisc.getAllRoadmapFeatureNames(roadmapFeatures);
            if (roadmapFeatures.size() > 0)
            {
                projectMonitoringMisc.WriteToStatus(statusText, false, "Found roadmap features total " + roadmapFeaturesNames.size());
                contextMap.put(ROADMAPFEATURESLIST, roadmapFeaturesNames);
            }
            else
            {
                projectMonitoringMisc.WriteToStatus(statusText, false, "No roadmap feature found for project");
                bAllisOk = false;
                messageToDisplay = "Failed to retrieve a list of Roadmap Features for the project. Please create the  Roadmap Features";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }

            //try to read from AO for this user
            maor = mao.GetUserLastLocations(applicationUser.getKey());
            if (maor.Code != ManageActiveObjectsResult.STATUS_CODE_SUCCESS) //not found
            {
                //give a message and ask to recalculate
                projectMonitoringMisc.WriteToStatus(statusText, false, "Failed to retrieve any project issues - no selected roadmap feature");
                bAllisOk = false;
                messageToDisplay = "Please select a Roadmap Feature from the list and press Recalculate (also check if the Team's velocity is not 0)";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }

            //user found
            projectMonitoringMisc.WriteToStatus(statusText, false, "First AO that matches project is found");
            //get selected roadmap feature and velocity and project?

            UserLastLocations userLastLocations = (UserLastLocations)maor.Result;
            selectedRoadmapFeature = userLastLocations.lastRoadmapFeature;
            contextMap.put(SELECTEDROADMAPFEATURE, selectedRoadmapFeature);
            if (selectedRoadmapFeature.isEmpty())
            {
                projectMonitoringMisc.WriteToStatus(statusText, false, "Roadmap feature is not selected");
                bAllisOk = false;
                messageToDisplay = "Please select the Roadmap Feature and press Recalculate";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }
            teamVelocity = userLastLocations.lastTeamVelocity;
            contextMap.put(TEAMVELOCITY, teamVelocity);
            if (teamVelocity <= 0)
            {
                projectMonitoringMisc.WriteToStatus(statusText, false, "Team velocity is not specified");
                bAllisOk = false;
                messageToDisplay = "Please specify team's velocity and press Recalculate";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }


            selectedRoadmapFeatureIssue = projectMonitoringMisc.SearchSelectedIssue(roadmapFeatures, selectedRoadmapFeature);
            if (selectedRoadmapFeatureIssue == null) //not found
            {
                projectMonitoringMisc.WriteToStatus(statusText, false, "our selected feature is not in the list " + selectedRoadmapFeature);
                bAllisOk = false;
                messageToDisplay = "Please select a Roadmap Feature and press Recalculate";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }

            this.issues = jiraInterface.getIssuesForRoadmapFeature(statusText, applicationUser, currentProject, selectedRoadmapFeatureIssue);
            if (null != issues && issues.size() > 0)
            {
                //if no roadmap features selected yet - give a message to select an recalculate
                //also check if no velocity stored yet - also give a message
                // if one of above is correct display a message to the user
                projectMonitoringMisc.WriteToStatus(statusText, false, "We have roadmap feature and team's velocity " + selectedRoadmapFeature + " " + teamVelocity);
                //find all User stories Task and bugs of the issues and only those that are not completed

                ArrayList<Issue> foundIssues = new ArrayList<>();
                ArrayList<PlaygileSprint> playgileSprints = new ArrayList<>();

                //get list of sprints out of user stories.
                projectMonitoringMisc.getNotCompletedIssuesAndAndSprints(issues, foundIssues, playgileSprints, statusText);
                //sort sprints
                Collections.sort(playgileSprints); //sort by dates
                //here we have in playgileSprints all closed and active sprints
                //so we can get our issues distribution statistics

                for (PlaygileSprint notFutureSprint : playgileSprints)
                {
                    double[] sprintIssuesDistribution = notFutureSprint.getIssuesTimeDistribution();
                    //update main counters
                    for (int i = 0; i < overallIssuesDistributionInSprint.length; i++) {
                        overallIssuesDistributionInSprint[i] += sprintIssuesDistribution[i];
                    }
                }
                //lets' get average
                StringBuilder issuesDistributionString = new StringBuilder();
                double sumOfClosed = 0;
                if (playgileSprints.size() > 0) {
                    for (int i = 0; i < overallIssuesDistributionInSprint.length; i++) {
                        overallIssuesDistributionInSprint[i] /= (double) playgileSprints.size();
                        double roundTo2Digits = 100.0 * Math.round(overallIssuesDistributionInSprint[i] * 100.0) / 100.0;
                        sumOfClosed += roundTo2Digits;
                        issuesDistributionString.append(roundTo2Digits + ManageActiveObjects.PAIR_SEPARATOR);
                    }
                }
                contextMap.put(ISSUESDISTRIBUTION, issuesDistributionString.toString());

                //did we find any matching issues?
                if (foundIssues.size() > 0)
                {
                    //the project is ok. Let's see if it has started yet
                    //get the start flag from AO
                    maor = mao.GetProjectStartedFlag(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature));
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
                                startDate = sprint.getStartDate();
                                //also get the sprint length
                                sprintLength = ProjectProgress.AbsDays(sprint.getStartDate(), sprint.getEndDate()) + 1;
                                projectMonitoringMisc.WriteToStatus(statusText, false,"Detected start date " + startDate);
                                projectMonitoringMisc.WriteToStatus(statusText, false,"Detected sprint length " + sprintLength);

                                //set to AO entity - project started, start date and sprint length
                                projectStarted = true;
                                maor = mao.SetProjectStartedFlag(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), projectStarted);
                                if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                                {
                                    projectMonitoringMisc.WriteToStatus(statusText, false,"Project start flag is set to true. Setting start date");
                                    maor = mao.SetProjectStartDate(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), startDate);
                                    if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                                    {
                                        projectMonitoringMisc.WriteToStatus(statusText, false,"Project start date is set to " + startDate);
                                    }
                                    else
                                    {
                                        projectMonitoringMisc.WriteToStatus(statusText, false, "Failed to set project start date " + maor.Message);
                                        bAllisOk = false;
                                        messageToDisplay = "General failure - AO problem - failed to set project start date. Report to Ed";
                                        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                                    }
                                    maor = mao.SetSprintLength(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), sprintLength);
                                    if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                                    {
                                        projectMonitoringMisc.WriteToStatus(statusText, false,"Project sprint length is set " + sprintLength);
                                    }
                                    else
                                    {
                                        projectMonitoringMisc.WriteToStatus(statusText, false, "Failed to set project sprint length " + maor.Message);
                                        bAllisOk = false;
                                        messageToDisplay = "General failure - AO problem - failed to set project sprint length. Report to Ed";
                                        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                                    }
                                }
                                else
                                {
                                    projectMonitoringMisc.WriteToStatus(statusText, false, "Failed to set project start flag " + maor.Message);
                                    bAllisOk = false;
                                    messageToDisplay = "General failure - AO problem - failed to set project start flag. Report to Ed";
                                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                                }
                            }
                            else //no active or closed sprints at all - not started yet
                            {
                                projectMonitoringMisc.WriteToStatus(statusText, false, "No valid sprints found for any issues");
                                bAllisOk = false;
                                messageToDisplay = "The project has not started yet. Please start a sprint";
                                maor = mao.SetProjectStartedFlag(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), false);
                                if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                                {
                                    projectMonitoringMisc.WriteToStatus(statusText, false,"Set false project start flag");
                                }
                                else
                                {
                                    projectMonitoringMisc.WriteToStatus(statusText, false,"Failed to set project flag to false " + maor.Message);
                                }
                                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                            }
                        }
                        if (projectStarted)
                        {
                            projectMonitoringMisc.WriteToStatus(statusText, false,"Project start flag is true");

                            maor = mao.GetProjectStartDate(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature));
                            if (maor.Code != ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                            {
                                projectMonitoringMisc.WriteToStatus(statusText, false, "Failed to get project start date " + maor.Message);
                                bAllisOk = false;
                                messageToDisplay = "General failure - AO problem - failed to get project start date. Report to Ed";
                                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                            }
                            startDate = (Date)maor.Result;
                            //get the sprint length
                            sprintLength = getSprintLength(mao, currentProject, selectedRoadmapFeature);
                            //now let's calculate the remaining story points
                            double currentEstimation = 0;
                            for (Issue issue : foundIssues)
                            {
                                if (!projectMonitoringMisc.isIssueCompleted(issue))
                                {
                                    projectMonitoringMisc.WriteToStatus(statusText, false,"Issue status is Complete");
                                    double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);
                                    storyPointValue = projectMonitoringMisc.adjustStoryPointsIfNotEstimated(storyPointValue,
                                        projectMonitoringMisc.isIssueBug(issue));
                                    currentEstimation += storyPointValue;
                                    projectMonitoringMisc.WriteToStatus(statusText, true, "Adding story points for issue " +
                                        storyPointValue + " " +
                                        issue.getKey());
                                }
                                else
                                {
                                    projectMonitoringMisc.WriteToStatus(statusText, false,"Issue status Complete");
                                }
                            }
                            projectMonitoringMisc.WriteToStatus(statusText, false,"Calculated estimation " + currentEstimation);
                            //after calculation
                            //1. set initial estimation if previousProjectStartFlag is false
                            //get the initial estimations
                            double initialEstimation = currentEstimation;
                            //let's try to get initial estimation in the right way. Comment it out if not working
                            initialEstimation = projectMonitoringMisc.getInitialEstimation(issues, startDate, statusText);
                            projectMonitoringMisc.WriteToStatus(statusText, false,"Setting initial estimation " + initialEstimation);
                            maor = mao.SetProjectInitialEstimation(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), startDate, initialEstimation);
                            if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                                projectMonitoringMisc.WriteToStatus(statusText, false, "initial estimation set for project");
                            }
                            else
                            {
                                projectMonitoringMisc.WriteToStatus(statusText, false, "Failed to set initial project estimation " + maor.Message);
                                bAllisOk = false;
                                messageToDisplay = "General failure - AO problem - failed to set project initial estimation. Report to Ed";
                                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                            }

                            //2. add current estimation to the list of estimations
                            //tmpDate = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT).parse("6/23/2020");
                            Date timeStamp = Calendar.getInstance().getTime();
                            projectMonitoringMisc.WriteToStatus(statusText, false,"Current time to add to list " + timeStamp);
                            maor = mao.AddRemainingEstimationsRecord(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), timeStamp, currentEstimation);
                            if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                                projectMonitoringMisc.WriteToStatus(statusText, false, "Last estimation added to the list " + timeStamp + " " + currentEstimation);
                            }
                            else {
                                projectMonitoringMisc.WriteToStatus(statusText, false, "Failed to add Last estimation " + maor.Message);
                                bAllisOk = false;
                                messageToDisplay = "General failure - Failed to add last estimation to the list. Report to Ed";
                                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                            }

                            //get real velocities
                            //fill real sprint velocity

                            Collection<PlaygileSprint> allRealSprints = projectMonitoringMisc.getAllRealSprintsVelocities(playgileSprints, startDate, teamVelocity, (int)sprintLength, statusText);
                            //linear regression
                            ArrayList<Double> predictedVelocities;
                            predictedVelocities = projectMonitoringMisc.getLinearRegressionForRealSprintVelocities(allRealSprints, startDate, statusText);

                            //averaging
                            //predictedVelocities = projectMonitoringMisc.getAverageForRealSprintVelocities(allRealSprints, startDate, statusText);

                            //convert to strings
                            StringBuilder resultRows = new StringBuilder();
                            int index = 0;
                            for (PlaygileSprint sprintToConvert : allRealSprints)
                            {
                                resultRows.append(
                                    projectMonitoringMisc.ConvertDateToOurFormat(sprintToConvert.getEndDate()) + ManageActiveObjects.PAIR_SEPARATOR +
                                        sprintToConvert.sprintVelocity  + ManageActiveObjects.PAIR_SEPARATOR +
                                        predictedVelocities.get(index++) + ManageActiveObjects.LINE_SEPARATOR
                                );
                            }

                            contextMap.put(REALVELOCITIES, resultRows);
                            //and the average is
                            //projectVelocity = projectMonitoringMisc.getAverageProjectRealVelocity(allRealSprints, teamVelocity, statusText);
                            projectVelocity = (int)Math.round(predictedVelocities.get(predictedVelocities.size() - 1));
                            if (projectVelocity <= 0) {
                                projectVelocity = teamVelocity;
                                projectMonitoringMisc.WriteToStatus(statusText, true,"Project velocity is 0, setting to team velocity " + teamVelocity);
                            }
                            contextMap.put(AVERAGEREALVELOCITY, projectVelocity);

                        }
                    }
                    else //not retrieved start flag
                    {
                        projectMonitoringMisc.WriteToStatus(statusText, false, "Failed to read project start flag " + maor.Message);
                        bAllisOk = false;
                        messageToDisplay = "General failure - AO problem. Report to Ed";
                        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                    }
                }
                else
                {
                    projectMonitoringMisc.WriteToStatus(statusText, false, "Didn't find any issue not COMPLETED");
                    bAllisOk = false;
                    messageToDisplay = "The backlog does not contain any issue not completed";
                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                }
            }
            else
            {
                projectMonitoringMisc.WriteToStatus(statusText, true, "Failed to retrieve any project issues for " + selectedRoadmapFeature);
                bAllisOk = false;
                messageToDisplay = "Failed to retrieve any project's issues for Roadmap Feature" +
                    ". Please make sure the Roadmap Feature has the right structure (epics, linked epics etc.)";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);

            }

            maor = mao.GetProgressDataList(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature));
            if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                ProjectProgress projectProgress = new ProjectProgress();

                /*//////////////////////////////////
                maor.Result = new ArrayList<DataPair>();
                DataPair item = new DataPair(new Date("7/1/2020"), 500);
                ((ArrayList<DataPair>)maor.Result).add(item);
                item = new DataPair(new Date("7/15/2020"), 400);
                ((ArrayList<DataPair>)maor.Result).add(item);
                ////////////////////////////////*/
                sprintLength = getSprintLength(mao, currentProject, selectedRoadmapFeature);
                ProjectProgressResult ppr = projectProgress.Initiate(teamVelocity, projectVelocity, (int)sprintLength, (ArrayList<DataPair>) maor.Result);
                //what is the longest array?
                StringBuilder chartRows = new StringBuilder();
                ProgressData longestList;
                ProgressData shortestList;
                boolean predictedIsLongest;
                if (ppr.progressData.Length() >= ppr.idealData.Length()) {
                    longestList = ppr.progressData;
                    shortestList = ppr.idealData;
                    predictedIsLongest = true;
                }
                else
                {
                    longestList = ppr.idealData;
                    shortestList = ppr.progressData;
                    predictedIsLongest = false;
                }
                DataPair tmpPredictedDataPair, tmpIdealDataPair;
                for (int i = 0; i < longestList.Length(); i++)
                {
                    if (predictedIsLongest) {
                        tmpPredictedDataPair = longestList.GetElementAtIndex(i);
                        tmpIdealDataPair = shortestList.GetElementAtIndex(i);
                    }
                    else
                    {
                        tmpPredictedDataPair = shortestList.GetElementAtIndex(i);
                        tmpIdealDataPair = longestList.GetElementAtIndex(i);
                    }
                    if (i >= shortestList.Length()) //no more elements in shortest
                    {
                        if (predictedIsLongest) {
                            chartRows.append(
                                projectMonitoringMisc.ConvertDateToOurFormat(tmpPredictedDataPair.Date) + ManageActiveObjects.PAIR_SEPARATOR +
                                    "" + ManageActiveObjects.PAIR_SEPARATOR +
                                    tmpPredictedDataPair.RemainingEstimation + ManageActiveObjects.LINE_SEPARATOR
                            );
                        }
                        else
                        {
                            chartRows.append(
                                projectMonitoringMisc.ConvertDateToOurFormat(tmpIdealDataPair.Date) + ManageActiveObjects.PAIR_SEPARATOR +
                                    tmpIdealDataPair.RemainingEstimation + ManageActiveObjects.PAIR_SEPARATOR +
                                    "" + ManageActiveObjects.LINE_SEPARATOR
                            );
                        }
                    }
                    else //both records available
                    {
                        chartRows.append(
                            projectMonitoringMisc.ConvertDateToOurFormat(tmpPredictedDataPair.Date) + ManageActiveObjects.PAIR_SEPARATOR +
                                tmpIdealDataPair.RemainingEstimation + ManageActiveObjects.PAIR_SEPARATOR +
                                tmpPredictedDataPair.RemainingEstimation + ManageActiveObjects.LINE_SEPARATOR
                        );
                    }
                }
                contextMap.put(CHARTROWS, chartRows.toString());
                contextMap.put(IDEALENDOFPROJECT, projectMonitoringMisc.ConvertDateToOurFormat(ppr.idealProjectEnd));
                //make the logic of color
                contextMap.put(PREDICTIONCOLOR, ProjectProgress.convertColorToHexadeimal(ppr.progressDataColor));
                contextMap.put(PREDICTEDENDOFPROJECT, projectMonitoringMisc.ConvertDateToOurFormat(ppr.predictedProjectEnd));
            }
            else
            {
                projectMonitoringMisc.WriteToStatus(statusText, false, "Failed to retrieve project list of progress data");
                bAllisOk = false;
                messageToDisplay = "General failure - no progress data - AO problem. Report to Ed";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }


            /*

            ProjectProgress pp = new ProjectProgress();
            ArrayList<DataPair> testData = new ArrayList<>();
            DataPair item = new DataPair(new Date("7/1/2020"), 500);
            testData.add(item);
            item = new DataPair(new Date("7/15/2020"), 480);
            testData.add(item);

            pp.Initiate(teamVelocity, 14, testData);
            //test Active objects

            String chartRows =
                "6/1/2020" + ManageActiveObjects.PAIR_SEPARATOR +
                    "500.0" + ManageActiveObjects.PAIR_SEPARATOR +
                    "500.0" + ManageActiveObjects.LINE_SEPARATOR +

                    "6/7/2020" + ManageActiveObjects.PAIR_SEPARATOR +
                    "450.0" + ManageActiveObjects.PAIR_SEPARATOR +
                    "480.0" + ManageActiveObjects.LINE_SEPARATOR +

                    "7/14/2020" + ManageActiveObjects.PAIR_SEPARATOR +
                    "400.0" + ManageActiveObjects.PAIR_SEPARATOR +
                    "450.0" + ManageActiveObjects.LINE_SEPARATOR +

                    "11/11/2020" + ManageActiveObjects.PAIR_SEPARATOR +
                    "0.0" + ManageActiveObjects.PAIR_SEPARATOR +
                    "40.0" + ManageActiveObjects.LINE_SEPARATOR +

                    "12/1/2020" + ManageActiveObjects.PAIR_SEPARATOR +
                    "" + ManageActiveObjects.PAIR_SEPARATOR +
                    "0.0" + ManageActiveObjects.LINE_SEPARATOR;

*/

            //analyze stories for roadmap feature
            AnalyzedStories analysis = projectMonitoringMisc.getStoriesAnalyzed(issues);
            //fill the stories analyze
            contextMap.put(NOTESTIMATEDSTORIES, analysis.NotEstimatedStoriesNumber);
            contextMap.put(LARGESTORIES, analysis.LargeStoriesNumber);
            contextMap.put(VERYLARGESTORIES, analysis.VeryLargeStoriesNumber);
            contextMap.put(ESTIMATEDSTORIES, analysis.EstimatedStoriesNumber);



            contextMap.put(AORESULT, maor.Message);

        }
        else
        {
            //no current project
            projectMonitoringMisc.WriteToStatus(statusText, false, "No current project found");
            bAllisOk = false;
            messageToDisplay = "Cannot identify current project. Please try to reload this page";
            return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);

        }

        projectMonitoringMisc.WriteToStatus(statusText, false, "Exiting successfully");
        bAllisOk = true;
        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
    }
    @Override
    public UserProjectHistoryManager getUserProjectHistoryManager() {
        return this.userProjectHistoryManager;
    }


    private double getSprintLength(ManageActiveObjects mao, Project currentProject, String selectedRoadmapFeature)
    {
        double sprintLength;
        ManageActiveObjectsResult maor = mao.GetSprintLength(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature));
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
            sprintLength = (double)maor.Result;
            //round to one week
            sprintLength = ((int)sprintLength / 7) * 7;
        }
        else
        {
            sprintLength = 14;
        }
        return sprintLength;
    }
    private Map<String, Object> ReturnContextMapToVelocityTemplate(Map<String, Object> contextMap, boolean bAllisOk, String messageToDisplay)
    {
        contextMap.put(ALLISOK, bAllisOk);
        contextMap.put(MESSAGETODISPLAY, messageToDisplay);
        contextMap.put(STATUSTEXT, statusText.toString());
        return contextMap;
    }
}
