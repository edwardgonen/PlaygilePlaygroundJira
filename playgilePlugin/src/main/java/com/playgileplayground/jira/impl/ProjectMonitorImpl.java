package com.playgileplayground.jira.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;
import com.playgileplayground.jira.restapi.RestAPI;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericEntityException;

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
        Project currentProject = userProjectHistoryManager.getCurrentProject(BROWSE, applicationUser);


        if(null != currentProject) {
            contextMap.put(PROJECT, currentProject);
            this.issues = this.getAllIssues(applicationUser, currentProject);


            if (null != this.issues)
            {
                if (issues.size() > 0) contextMap.put(ISSUE, issues.get(0));
                if (issues.size() > 0)
                    contextMap.put(STORYPOINTS, getAllCustomFieldsForIssue(issues.get(0)));
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
        return contextMap;
    }
    @Override
    public UserProjectHistoryManager getUserProjectHistoryManager() {
        return this.userProjectHistoryManager;
    }

    private String getAllCustomFieldsForIssue(Issue issue)
    {
        StringBuilder result = new StringBuilder("Custom objects: ");
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();

        /*
        List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue);
        for (CustomField tmpField : customFields)
        {
            result.append("\n");
            result.append(tmpField.getFieldName() + "=");
            result.append(tmpField.getValue(issue));
        }
        */
        //check the story points
        Collection <CustomField> storyPoints = customFieldManager.getCustomFieldObjectsByName("Story Points");
        if (storyPoints != null)
        {
            result.append(storyPoints.iterator().next().getValue(issue));
        }
        else result.append("### no value for story points ###");
        return result.toString();
    }
    private List<Issue> getAllIssues(ApplicationUser applicationUser, Project currentProject) {
        //CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        //Collection<CustomField> customFields = customFieldManager.getCustomFieldObjectsByName("Sprint");

        IssueManager issueManager = ComponentAccessor.getIssueManager();


        Collection<Long> allIssueIds = null;
        try {
            allIssueIds = issueManager.getIssueIdsForProject(currentProject.getId());
        } catch (GenericEntityException e) {
            System.out.println("Failed to get issue ids " + e.toString());
        }
        List<Issue>	allIssues = issueManager.getIssueObjects(allIssueIds);
        return allIssues;

    }

    private void getAllSprints()
    {

    }
}
