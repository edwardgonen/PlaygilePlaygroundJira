package com.playgileplayground.jira.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.issue.status.category.StatusCategory;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import java.util.*;

import static com.atlassian.jira.security.Permissions.BROWSE;

@Scanned
public class ProjectMonitorImpl extends AbstractJiraContextProvider implements com.playgileplayground.jira.api.ProjectMonitor {
    @ComponentImport
    private final UserProjectHistoryManager userProjectHistoryManager;
    @ComponentImport
    private final ActiveObjects ao;

    List<Issue> issues;
    StringBuilder statusText = new StringBuilder();

    public ProjectMonitorImpl(UserProjectHistoryManager userProjectHistoryManager, ActiveObjects ao){
        this.userProjectHistoryManager = userProjectHistoryManager;
        this.ao = ao;
    }

    @Override
    public Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        Map<String, Object> contextMap = new HashMap<>();

        double teamVelocity = 0;
        String selectedVersion = "";
        String messageToDisplay = "";
        boolean bAllisOk = false;
        ManageActiveObjectsResult maor;
        ManageActiveObjects mao = new ManageActiveObjects(this.ao);

        Project currentProject = userProjectHistoryManager.getCurrentProject(BROWSE, applicationUser);
        WriteToStatus("After getting current project " + (currentProject != null));

        contextMap.put(MAINJAVACLASS, this);
        JiraInterface jiraInterface = new JiraInterface(this);
        if(null != currentProject) {
            WriteToStatus("Got current project " + currentProject.getName() + " key " + currentProject.getKey());
            contextMap.put(PROJECT, currentProject);
            this.issues = jiraInterface.getAllIssues(applicationUser, currentProject);
            if (null != this.issues)
            {
                WriteToStatus( "Got issues " + this.issues.size());
                if (issues.size() > 0) {
                    //get velocity and release version
                    maor = mao.CreateProjectEntity(currentProject.getKey()); //will not create if exists
                    WriteToStatus( "Tried to create new entry in AO");
                    if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS || maor.Code == ManageActiveObjectsResult.STATUS_CODE_ENTRY_ALREADY_EXISTS) {
                        WriteToStatus( "Created or existed in AO");
                        maor = mao.GetTeamVelocity(currentProject.getKey());
                        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                            WriteToStatus( "Team velocity found " + maor.Result);
                            teamVelocity = (double)maor.Result;
                            contextMap.put(TEAMVELOCITY, maor.Result);
                        }
                        else
                        {
                            WriteToStatus( "Team velocity not found " + maor.Code);
                            contextMap.put(TEAMVELOCITY, maor.Message);
                        }
                        maor = mao.GetProjectReleaseVersion(currentProject.getKey());
                        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                            selectedVersion = (String)maor.Result;
                            WriteToStatus( "Release version found " + maor.Result);
                            contextMap.put(SELECTEDPROJECTVERSION, maor.Result);
                        }
                        else
                        {
                            WriteToStatus( "Release version not found " + maor.Code);
                            contextMap.put(SELECTEDPROJECTVERSION, maor.Message);
                        }
                    }
                    else
                    {
                        //no current project
                        WriteToStatus( "Current project not found. Setting AllIsOk to false");
                        bAllisOk = false;
                        messageToDisplay = "Cannot identify current project. Please try to reload this page";
                        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                    }


                    contextMap.put(ISSUE, issues.get(0));

                    double storyPointValue = jiraInterface.getStoryPointsForIssue(issues.get(0));
                    if (storyPointValue <= 0) storyPointValue = 21;
                    contextMap.put(STORYPOINTS, storyPointValue);

                    Collection<PlaygileSprint> sprintsForIssue = jiraInterface.getAllSprintsForIssue(issues.get(0));
                    if (sprintsForIssue != null && sprintsForIssue.size() > 0) {
                        contextMap.put(SPRINTINFO, sprintsForIssue.iterator().next().toString());
                   }
                }
                else
                {
                    WriteToStatus( "No issues found for current project");
                    bAllisOk = false;
                    messageToDisplay = "There is none user story/issue defined for this project";
                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                }


                //do we have any versions defined?
                Collection<String> versions = jiraInterface.getAllVersionsForProject(currentProject);
                if (versions == null)
                {
                    WriteToStatus( "No versions found for project");
                    bAllisOk = false;
                    messageToDisplay = "Failed to retrieve a list of versions for the project. Please add release versions";
                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                }
                else
                {
                    if (versions.size() > 0)
                    {
                        WriteToStatus( "Found versions total " + versions.size());
                        contextMap.put(PROJECTVERSIONS, versions);

                        //if no version selected yet - give a message to select an recalculate
                        //also check if no velocity stored yet - also give a message
                        // if one of above is correct display a message to the user
                        if (teamVelocity <= 0)
                        {
                            WriteToStatus( "Team velocity is not specified");
                            bAllisOk = false;
                            messageToDisplay = "Please specify team's velocity and press Recalculate";
                            return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                        }
                        if (selectedVersion.isEmpty())
                        {
                            WriteToStatus( "Team velocity is not specified");
                            bAllisOk = false;
                            messageToDisplay = "Please select the Release Version and press Recalculate";
                            return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                        }

                        WriteToStatus( "We have version and team's velocity " + selectedVersion + " " + teamVelocity);
                        //from the issues find all those which have the selected version
                        //find all User stories of the issues and only those that are not completed
                        ArrayList<Issue> foundIssues = new ArrayList<>();
                        for (Issue issue : issues)
                        {
                            Status issueStatus = issue.getStatus();
                            StatusCategory statusCategory = issueStatus.getStatusCategory();
                            if (statusCategory == null)
                            {
                                WriteToStatus( "Failed to retrieve issue status for issue " + issue.getId());
                                //go to next issue
                                continue;
                            }
                            Collection<Version> fixedVersions = issue.getFixVersions();
                            if (statusCategory == null)
                            {
                                WriteToStatus( "Failed to retrieve versions for issue " + issue.getId());
                                //go to next issue
                                continue;
                            }
                            boolean versionFound = false;
                            for (Version version : fixedVersions)
                            {
                                if (version.getName().equals(selectedVersion))
                                {
                                    versionFound = true;
                                    break;
                                }
                            }

                            if (statusCategory.getKey() != StatusCategory.COMPLETE && versionFound)
                            {
                                foundIssues.add(issue);
                            }
                            else
                            {
                                WriteToStatus( "Issue version does not match selected or status is COMPLETE " + issue.getId());
                                //go to the next issue
                                continue;
                            }
                        }

                        //get list of sprints out of user stories.
                        //did we find any matching issues?
                        if (foundIssues.size() > 0)
                        {
                            //At least one should be either:
                            // ACTIVE
                            // CLOSED
                            // - this is a flag that project started
                            //calculate remaining sum of story points
                        }
                        else
                        {
                            WriteToStatus( "Didn't find any issue with selected version and not COMPLETED");
                            bAllisOk = false;
                            messageToDisplay = "The backlog does not contain any issue matching version " +
                                selectedVersion + " or not completed";
                            return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                        }
                    }
                    else
                    {
                        WriteToStatus( "No versions found for project");
                        messageToDisplay = "The list of versions for the project is empty. Please add release versions";
                        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                    }
                }
            }
            else
            {
                WriteToStatus( "Failed to retrieve project settings");
                bAllisOk = false;
                messageToDisplay = "Failed to retrieve project's issues";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }
            //log.debug("EdGonen issue " + issues.get(0).getSummary());

            //maor = mao.GetProjectKey(currentProject.getKey());
           // maor = mao.DeleteProjectEntity(currentProject.getKey());


            Date tmpDate;
/*
            try {

                //tmpDate = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT).parse("6/23/2020");
                //maor = mao.AddRemainingEstimationsRecord(currentProject.getKey(), tmpDate, 400);

                tmpDate = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT).parse("7/2/2020");
                maor = mao.AddRemainingEstimationsRecord(currentProject.getKey(), tmpDate, 350);

                //tmpDate = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT).parse("7/14/2020");
                //maor = mao.AddRemainingEstimationsRecord(currentProject.getKey(), tmpDate, 300);

                tmpDate = new SimpleDateFormat(ManageActiveObjects.DATE_FORMAT).parse("7/2/2020");
                maor = mao.GetRemainingEstimationsForDate(currentProject.getKey(), tmpDate);

            }
            catch (ParseException e) {
                maor.Code = ManageActiveObjectsResult.STATUS_CODE_DATA_FORMAT_PARSING_ERROR;
                maor.Message = "Failed to parse date";
            }
*/
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

            contextMap.put(CHARTROWS, chartRows);
            contextMap.put(AORESULT, maor.Message);

        }
        else
        {
            //no current project
            WriteToStatus( "No current project found");
            bAllisOk = false;
            messageToDisplay = "Cannot identify current project. Please try to reload this page";
            return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);

        }

        WriteToStatus( "Exiting successfully");
        bAllisOk = true;
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

    public void WriteToStatus(String text)
    {
        statusText.append(text + "***");
    }
}
