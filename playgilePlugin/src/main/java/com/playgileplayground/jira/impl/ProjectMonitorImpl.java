package com.playgileplayground.jira.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.issue.Issue;
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
import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.atlassian.jira.security.Permissions.BROWSE;

@Scanned
public class ProjectMonitorImpl extends AbstractJiraContextProvider implements com.playgileplayground.jira.api.ProjectMonitor {
    private static final Logger log = Logger.getLogger(ProjectMonitorImpl.class);
    @ComponentImport
    private final UserProjectHistoryManager userProjectHistoryManager;
    @ComponentImport
    private final ActiveObjects ao;

    List<Issue> issues;

    public ProjectMonitorImpl(UserProjectHistoryManager userProjectHistoryManager, ActiveObjects ao){
        this.userProjectHistoryManager = userProjectHistoryManager;
        this.ao = ao;
    }

    @Override
    public Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        Map<String, Object> contextMap = new HashMap<>();
        String statusText = "";

        String messageToDisplay = "";
        boolean bAllisOk = false;


        Project currentProject = userProjectHistoryManager.getCurrentProject(BROWSE, applicationUser);
        contextMap.put(MAINJAVACLASS, this);
        JiraInterface jiraInterface = new JiraInterface();
        if(null != currentProject) {
            contextMap.put(PROJECT, currentProject);
            this.issues = jiraInterface.getAllIssues(applicationUser, currentProject);

            if (null != this.issues)
            {
                if (issues.size() > 0) {
                    contextMap.put(ISSUE, issues.get(0));

                    double storyPointValue = jiraInterface.getStoryPointsForIssue(issues.get(0));
                    if (storyPointValue <= 0) storyPointValue = 21;
                    contextMap.put(STORYPOINTS, storyPointValue);

/*
                    Collection<PlaygileSprint> sprintsForIssue = jiraInterface.getAllSprintsForIssue(issues.get(0));
                    System.out.println(" **** " + sprintsForIssue.iterator().next().toString());
//                    if (sprintsForIssue != null && sprintsForIssue.size() > 0) {
//                        contextMap.put(SPRINTINFO, sprintsForIssue.iterator().next());
//                   }
*/
                    contextMap.put(SPRINTINFO, "kuku");
                }
                else
                {
                    bAllisOk = false;
                    messageToDisplay = "There is none user story/issue defined for this project";
                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                }


                //do we have any versions defined?
                Collection<String> versions = jiraInterface.getAllVersionsForProject(currentProject);
                if (versions == null)
                {
                    bAllisOk = false;
                    messageToDisplay = "Failed to retrieve a list of versions for the project. Please add release versions";
                    return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                }
                else
                {
                    if (versions.size() > 0)
                    {
                        contextMap.put(PROJECTVERSIONS, versions);

                        //get selected version and velocity from AO

                        //if no version selected yet - give a message to select an recalculate
                        //also check if no velocity stored yet - also give a message
                        // if one of above is correct display a message to the user

                        //now let's get all issues
                        //from the issues find all those which have the selected version
                        //find all User stories of the issues and only those that are not complteted
                        //get list of sprints out of user stories.

                        //At least one should be either:
                        // ACTIVE
                        // CLOSED
                        // - this is a flag that project started
                        //calculate remaining sum of story points
                    }
                    else
                    {
                        messageToDisplay = "The list of versions for the project is empty. Please add release versions";
                        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
                    }
                }
            }
            else
            {
                bAllisOk = false;
                messageToDisplay = "Failed to retrieve project's issues";
                return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
            }
            //log.debug("EdGonen issue " + issues.get(0).getSummary());

            ManageActiveObjects mao = new ManageActiveObjects(this.ao);
            ManageActiveObjectsResult maor = mao.CreateProjectEntity(currentProject.getKey()); //will not create if exists
            if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS || maor.Code == ManageActiveObjectsResult.STATUS_CODE_ENTRY_ALREADY_EXISTS) {

                //maor = mao.GetProjectKey(currentProject.getKey());
               // maor = mao.DeleteProjectEntity(currentProject.getKey());


                Date tmpDate;

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
            }
            contextMap.put(AORESULT, maor.Message);
            contextMap.put(STATUSTEXT, statusText);

        }
        else
        {
            //no current project
            bAllisOk = false;
            messageToDisplay = "Cannot identify current project. Please try to reload this page";
            return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);

        }
        bAllisOk = true;
        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
    }
    @Override
    public UserProjectHistoryManager getUserProjectHistoryManager() {
        return this.userProjectHistoryManager;
    }
    public String testCallFromVelocity(String str)
    {
        System.out.println("%%% " + str);
        return str + " @@@";
    }

    private Map<String, Object> ReturnContextMapToVelocityTemplate(Map<String, Object> contextMap, boolean bAllisOk, String messageToDisplay)
    {
        contextMap.put(ALLISOK, bAllisOk);
        contextMap.put(MESSAGETODISPLAY, messageToDisplay);
        return contextMap;
    }
}
