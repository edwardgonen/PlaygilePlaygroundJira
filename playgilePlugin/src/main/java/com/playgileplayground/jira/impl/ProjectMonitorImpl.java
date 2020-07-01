package com.playgileplayground.jira.impl;

import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
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
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.query.Query;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

import static com.atlassian.jira.security.Permissions.BROWSE;

@Scanned
public class ProjectMonitorImpl extends AbstractJiraContextProvider implements com.playgileplayground.jira.api.ProjectMonitor {
    private static final Logger log = Logger.getLogger(ProjectMonitorImpl.class);
    @ComponentImport
    private final UserProjectHistoryManager userProjectHistoryManager;

    List<Issue> issues;
    Issue issue;

    public ProjectMonitorImpl(UserProjectHistoryManager userProjectHistoryManager){
        this.userProjectHistoryManager = userProjectHistoryManager;
    }

    @Override
    public Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        Map<String, Object> contextMap = new HashMap<>();

        Project currentProject = userProjectHistoryManager.getCurrentProject(BROWSE, applicationUser);
        if(null != currentProject) {
            contextMap.put(PROJECT, currentProject);
            this.issues = this.getAllIssues(applicationUser, currentProject);
            if (null != this.issues)
            {
                contextMap.put(ISSUE, issues.get(0));
                contextMap.put("fields", getAllCustomFieldsForIssue(issues.get(0)));
            }
            //log.debug("EdGonen issue " + issues.get(0).getSummary());
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

        SearchService searchProvider = ComponentAccessor.getComponentOfType(SearchService.class);
        JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
        builder.where()
                .project(currentProject.getKey())
                .and()
                .sub()
                .issueType("Story").or().issueType("Bug")
                .endsub();
                /*.and()
                .customField(customField.getIdAsLong()).eq(sprint.trim()*/
        try {
            SearchResults results = searchProvider
                    .searchOverrideSecurity(applicationUser, builder.buildQuery(), PagerFilter.getUnlimitedFilter());
            return results.getIssues();
        }
        catch (com.atlassian.jira.issue.search.SearchException se)
        {
            return null;
        }
    }
}
