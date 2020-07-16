package com.playgileplayground.jira.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.ContextProvider;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.jiraissues.SprintState;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;
import com.playgileplayground.jira.persistence.PrjStatEntity;
import com.playgileplayground.jira.projectprogress.DataPair;
import com.playgileplayground.jira.projectprogress.ProgressData;
import com.playgileplayground.jira.projectprogress.ProjectProgress;
import com.playgileplayground.jira.projectprogress.ProjectProgressResult;
import org.apache.commons.collections.ArrayStack;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.atlassian.jira.security.Permissions.BROWSE;

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
    StringBuilder statusText = new StringBuilder();

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

        double teamVelocity = 0;
        String selectedRoadmapFeature = "";
        List<Issue> roadmapFeatures;
        String messageToDisplay = "";
        boolean bAllisOk = false;
        Date startDate = null;
        double sprintLength;
        ManageActiveObjectsResult maor;
        ManageActiveObjects mao = new ManageActiveObjects(this.ao);
        JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
        String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        contextMap.put(BASEURL, baseUrl);
        ApplicationUser applicationUser = jac.getLoggedInUser();
        contextMap.put(CURRENTUSER, applicationUser.getKey());


        Project currentProject = projectManager.getProjectByCurrentKey((String) map.get("projectKey"));
        WriteToStatus(false,"After getting current project " + (currentProject != null));
        contextMap.put(MAINJAVACLASS, this);
        JiraInterface jiraInterface = new JiraInterface(this, applicationUser,  searchService);
        ProjectMonitoringMisc projectMonitoringMisc = new ProjectMonitoringMisc(jiraInterface, applicationUser, currentProject);

        //start the real work
        if(null != currentProject) {
            WriteToStatus(false,"Got current project " + currentProject.getName() + " key " + currentProject.getKey());
            contextMap.put(PROJECT, currentProject);

            //first set the list
            //do we have any roadmap defined?
            roadmapFeatures = jiraInterface.getAllRoadmapFeatures(applicationUser, currentProject, ROADMAPFEATUREKEY);
            ArrayList<String> roadmapFeaturesNames = projectMonitoringMisc.getAllRoadmapFeatureNames(roadmapFeatures);
            if (roadmapFeatures.size() > 0)
            {
                WriteToStatus(false, "Found roadmap features total " + roadmapFeaturesNames.size());
                contextMap.put(ROADMAPFEATURESLIST, roadmapFeaturesNames);
            }
            else
            {
                WriteToStatus(false, "No roadmap feature found for project");
                bAllisOk = false;
                messageToDisplay = "Failed to retrieve a list of Roadmap Features for the project. Please create the  Roadmap Features";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }

            //get first entity from the AO
            maor = mao.GetFirstProjectEntity(currentProject.getKey());
            if (maor.Code != ManageActiveObjectsResult.STATUS_CODE_SUCCESS) //not found
            {
                //give a message and ask to recalculate
                WriteToStatus(false, "Failed to retrieve any project issues - no selected roadmap feature");
                bAllisOk = false;
                messageToDisplay = "Please select a Roadmap Feature from the list and press Recalculate (also check if the Team's velocity is not 0)";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }

            //found
            WriteToStatus(false, "First AO that matches project is found");
            //get selected roadmap feature and velocity
            PrjStatEntity foundEnity = (PrjStatEntity) maor.Result;
            selectedRoadmapFeature = foundEnity.getRoadmapFeature();
            contextMap.put(SELECTEDROADMAPFEATURE, selectedRoadmapFeature);
            teamVelocity = foundEnity.getProjectTeamVelocity();
            contextMap.put(TEAMVELOCITY, teamVelocity);

            //find issue in the list of all roadmap features
            Issue selectedRoadmapFeatureIssue = null;
            for (Issue tmpFeature : roadmapFeatures)
            {
                if (selectedRoadmapFeature.equals(tmpFeature.getSummary()))
                {
                    selectedRoadmapFeatureIssue = tmpFeature;
                    break;
                }
            }

            this.issues = jiraInterface.getIssuesForRoadmapFeature(applicationUser, currentProject, selectedRoadmapFeatureIssue);
            if (null != this.issues)
            {
                WriteToStatus(false, "Got issues " + this.issues.size());
                if (issues.size() <= 0)
                {
                    WriteToStatus(false, "No issues found for current project");
                    bAllisOk = false;
                    messageToDisplay = "There is none user story/issue defined for this project";
                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                }



                //if no roadmap features selected yet - give a message to select an recalculate
                //also check if no velocity stored yet - also give a message
                // if one of above is correct display a message to the user
                if (teamVelocity <= 0)
                {
                    WriteToStatus(false, "Team velocity is not specified");
                    bAllisOk = false;
                    messageToDisplay = "Please specify team's velocity and press Recalculate";
                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                }
                if (selectedRoadmapFeature.isEmpty())
                {
                    WriteToStatus(false, "Roadmap feature is not selected");
                    bAllisOk = false;
                    messageToDisplay = "Please select the Roadmap Feature and press Recalculate";
                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                }

                WriteToStatus(false, "We have roadmap feature and team's velocity " + selectedRoadmapFeature + " " + teamVelocity);
                //find all User stories Task and bugs of the issues and only those that are not completed
                ArrayList<Issue> foundIssues = new ArrayList<>();
                ArrayList<PlaygileSprint> playgileSprints = new ArrayList<>();
                for (Issue issue : issues)
                {
                    Status issueStatus = issue.getStatus();
                    StatusCategory statusCategory = issueStatus.getStatusCategory();
                    IssueType issueType = issue.getIssueType();

                    boolean bOurIssueType =
                        issueType.getName().equals(STORY) ||
                        issueType.getName().equals(TASK) ||
                        issueType.getName().equals(BUG);

                    if (statusCategory == null)
                    {
                        WriteToStatus(false, "Failed to retrieve issue status for issue " + issue.getId());
                        //go to next issue
                        continue;
                    }
                    double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);
                    WriteToStatus(false, "Story points " + issue.getId() + " " + storyPointValue);
                    if (statusCategory.getKey() != StatusCategory.COMPLETE
                        /*&& storyPointValue >= 0*/ //we don't mind not set story points. I'll set them to 21
                        && (bOurIssueType)
                        )
                    {
                        foundIssues.add(issue);

                        Collection<PlaygileSprint> sprintsForIssue = jiraInterface.getAllSprintsForIssue(issue);
                        if (sprintsForIssue != null && sprintsForIssue.size() > 0) {
                            WriteToStatus(false, "Sprints for " + issue.getId() + " " + sprintsForIssue.size());
                            for (PlaygileSprint playgileSprint : sprintsForIssue)
                            {
                                if (playgileSprint.getState() != SprintState.FUTURE && (playgileSprint.getState() != SprintState.UNDEFINED))
                                {
                                    WriteToStatus(false,"Adding sprint for " + issue.getId() + " " + playgileSprint.getName());
                                    playgileSprints.add(playgileSprint);
                                }
                            }
                        }
                        else
                        {
                            WriteToStatus(false, "No sprints for " + issue.getId());
                        }
                    }
                    else
                    {
                        WriteToStatus(false, "Issue status is COMPLETE " +
                            statusCategory.getKey() + " " +
                            storyPointValue + " " +
                            issue.getId());
                        //go to the next issue
                        continue;
                    }
                }

                //get list of sprints out of user stories.
                //did we find any matching issues?
                if (foundIssues.size() > 0)
                {
                    //the project is ok. Let's see if it has started yet
                    //get the start flag from AO
                    maor = mao.GetProjectStartedFlag(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature));
                    if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                    {
                        boolean previousProjectStartedFlag = (boolean)maor.Result;
                        boolean projectStarted = (boolean)maor.Result;
                        if (!projectStarted) //not started
                        {
                            WriteToStatus(false,"Project start flag is false");
                            //let's find out if the project has started
                            //we should have a list of sprints
                            if (playgileSprints.size() > 0) {
                                WriteToStatus(false,"Valid sprints " + playgileSprints.size());
                                Collections.sort(playgileSprints); //sort by dates
                                //the first sprint startDate would be the project start date
                                PlaygileSprint sprint = playgileSprints.iterator().next(); //first
                                startDate = sprint.getStartDate();
                                //also get the sprint length
                                sprintLength = ProjectProgress.Days(sprint.getStartDate(), sprint.getEndDate()) + 1;
                                WriteToStatus(false,"Detected start date " + startDate);
                                WriteToStatus(false,"Detected sprint length " + sprintLength);
                                //set to AO entity
                                maor = mao.SetProjectStartedFlag(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), true);
                                if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                                {
                                    WriteToStatus(false,"Project start flag is set to true. Setting start date");
                                    maor = mao.SetProjectStartDate(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), startDate);
                                    if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                                    {
                                        projectStarted = true;
                                        WriteToStatus(false,"Project start date is set to " + startDate);
                                    }
                                    else
                                    {
                                        WriteToStatus(false, "Failed to set project start date " + maor.Message);
                                        bAllisOk = false;
                                        messageToDisplay = "General failure - AO problem - failed to set project start date. Report to Ed";
                                        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                                    }
                                    maor = mao.SetSprintLength(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), sprintLength);
                                    if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                                    {
                                        projectStarted = true;
                                        WriteToStatus(false,"Project sprint length is set " + sprintLength);
                                    }
                                    else
                                    {
                                        WriteToStatus(false, "Failed to set project sprint length " + maor.Message);
                                        bAllisOk = false;
                                        messageToDisplay = "General failure - AO problem - failed to set project sprint length. Report to Ed";
                                        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                                    }
                                }
                                else
                                {
                                    WriteToStatus(false, "Failed to set project start flag " + maor.Message);
                                    bAllisOk = false;
                                    messageToDisplay = "General failure - AO problem - failed to set project start flag. Report to Ed";
                                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                                }
                            }
                            else //no active or closed sprints at all - not started yet
                            {
                                WriteToStatus(false, "No valid sprints found for any issues");
                                bAllisOk = false;
                                messageToDisplay = "The project has not started yet. Please start a sprint";
                                maor = mao.SetProjectStartedFlag(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), false);
                                if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                                {
                                    WriteToStatus(false,"Set false project start flag");
                                }
                                else
                                {
                                    WriteToStatus(false,"Failed to set project flag to false " + maor.Message);
                                }
                                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                            }
                        }
                        if (projectStarted)
                        {
                            WriteToStatus(false,"Project start flag is true");
                            //now let's calculate the remaining story points
                            double currentEstimation = 0;
                            for (Issue issue : foundIssues)
                            {
                                com.atlassian.jira.issue.status.Status issueStatus = issue.getStatus();
                                if (issueStatus != null)
                                {
                                    StatusCategory statusCategory = issueStatus.getStatusCategory();
                                    WriteToStatus(false,"Issue status " + statusCategory);
                                    if (statusCategory.getKey() == StatusCategory.IN_PROGRESS || statusCategory.getKey() == StatusCategory.TO_DO)
                                    {
                                        WriteToStatus(false,"Issue status is one of ours " + statusCategory);
                                        double storyPointValue = jiraInterface.getStoryPointsForIssue(issue);
                                        //not estimated? set to max
                                        if (storyPointValue <= 0) storyPointValue = MAX_STORY_ESTIMATION;
                                        currentEstimation += storyPointValue;
                                    }
                                }
                                else
                                {
                                    WriteToStatus(false,"Failed to get status for issue " + issue.getKey());
                                }
                            }
                            WriteToStatus(false,"Calculated estimation " + currentEstimation);
                            //after calculation
                            //1. set initial estimation if previousProjectStartFlag is false
                            if (!previousProjectStartedFlag) //first time so store the initial estimations
                            {
                                WriteToStatus(false,"Setting initial estimation " + currentEstimation);
                                maor = mao.SetProjectInitialEstimation(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), startDate, currentEstimation);
                                if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                                    WriteToStatus(false, "initial estimation set for project");
                                }
                                else
                                {
                                    WriteToStatus(false, "Failed to set initial project estimation " + maor.Message);
                                    bAllisOk = false;
                                    messageToDisplay = "General failure - AO problem - failed to set project initial estimation. Report to Ed";
                                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                                }

                            }

                            //2. add current estimation to the list of estimations
                            //tmpDate = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT).parse("6/23/2020");
                            Date timeStamp = Calendar.getInstance().getTime();
                            WriteToStatus(false,"Current time to add to list " + timeStamp);
                            maor = mao.AddRemainingEstimationsRecord(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature), timeStamp, currentEstimation);
                            if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                                WriteToStatus(false, "Last estimation added to the list " + timeStamp + " " + currentEstimation);
                            }
                            else {
                                WriteToStatus(false, "Failed to add Last estimation " + maor.Message);
                                bAllisOk = false;
                                messageToDisplay = "General failure - Failed to add last estimation to the list. Report to Ed";
                                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                            }
                        }
                    }
                    else //not retrieved start flag
                    {
                        WriteToStatus(false, "Failed to read project start flag " + maor.Message);
                        bAllisOk = false;
                        messageToDisplay = "General failure - AO problem. Report to Ed";
                        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                    }
                }
                else
                {
                    WriteToStatus(false, "Didn't find any issue not COMPLETED");
                    bAllisOk = false;
                    messageToDisplay = "The backlog does not contain any issue not completed";
                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                }
            }
            else
            {
                WriteToStatus(false, "Failed to retrieve any project issues for " + selectedRoadmapFeature);
                bAllisOk = false;
                messageToDisplay = "Failed to retrieve any project's issues for Roadmap Feature" +
                    ". Please make sure the Roadmap Feature has the right structure (epics, linked epics etc.)";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);

            }

            maor = mao.GetSprintLength(new ManageActiveObjectsEntityKey(currentProject.getKey(), selectedRoadmapFeature));
            if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                WriteToStatus(false,"Got sprint length " + maor.Result);
                sprintLength = (double)maor.Result;
            }
            else
            {
                WriteToStatus(false,"Failed to retrieve sprint length, using 14");
                sprintLength = 14;
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
                ProjectProgressResult ppr = projectProgress.Initiate(teamVelocity, (int)sprintLength, (ArrayList<DataPair>) maor.Result);
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
                                ConvertDateToOurFormat(tmpPredictedDataPair.Date) + ManageActiveObjects.PAIR_SEPARATOR +
                                    "" + ManageActiveObjects.PAIR_SEPARATOR +
                                    tmpPredictedDataPair.RemainingEstimation + ManageActiveObjects.LINE_SEPARATOR
                            );
                        }
                        else
                        {
                            chartRows.append(
                                ConvertDateToOurFormat(tmpIdealDataPair.Date) + ManageActiveObjects.PAIR_SEPARATOR +
                                    tmpIdealDataPair.RemainingEstimation + ManageActiveObjects.PAIR_SEPARATOR +
                                    "" + ManageActiveObjects.LINE_SEPARATOR
                            );
                        }
                    }
                    else //both records available
                    {
                        chartRows.append(
                            ConvertDateToOurFormat(tmpPredictedDataPair.Date) + ManageActiveObjects.PAIR_SEPARATOR +
                                tmpIdealDataPair.RemainingEstimation + ManageActiveObjects.PAIR_SEPARATOR +
                                tmpPredictedDataPair.RemainingEstimation + ManageActiveObjects.LINE_SEPARATOR
                        );
                    }
                }
                contextMap.put(CHARTROWS, chartRows.toString());
                contextMap.put(IDEALENDOFPROJECT, ConvertDateToOurFormat(ppr.idealProjectEnd));
                //make the logic of color
                contextMap.put(PREDICTIONCOLOR, ProjectProgress.convertColorToHexadeimal(ppr.progressDataColor));
                contextMap.put(PREDICTEDENDOFPROJECT, ConvertDateToOurFormat(ppr.predictedProjectEnd));
            }
            else
            {
                WriteToStatus(false, "Failed to retrieve project list of progress data");
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

            contextMap.put(AORESULT, maor.Message);

        }
        else
        {
            //no current project
            WriteToStatus(false, "No current project found");
            bAllisOk = false;
            messageToDisplay = "Cannot identify current project. Please try to reload this page";
            return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);

        }

        WriteToStatus(false, "Exiting successfully");
        bAllisOk = true;
        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
    }
    @Override
    public UserProjectHistoryManager getUserProjectHistoryManager() {
        return this.userProjectHistoryManager;
    }

    private String ConvertDateToOurFormat(Date dateToConvert)
    {
        SimpleDateFormat outputDateFormat = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT);
        return outputDateFormat.format(dateToConvert);
    }
    private Map<String, Object> ReturnContextMapToVelocityTemplate(Map<String, Object> contextMap, boolean bAllisOk, String messageToDisplay)
    {
        contextMap.put(ALLISOK, bAllisOk);
        contextMap.put(MESSAGETODISPLAY, messageToDisplay);
        contextMap.put(STATUSTEXT, statusText.toString());
        return contextMap;
    }

    public void WriteToStatus(boolean debug,String text)
    {
        if (debug) statusText.append(text + "<br>");
    }

}
