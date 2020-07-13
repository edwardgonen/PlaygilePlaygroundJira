package com.playgileplayground.jira.jiraissues;

/*
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
*/

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.web.bean.Page;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.query.Query;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;

import com.playgileplayground.jira.impl.ProjectMonitorImpl;
import org.ofbiz.core.entity.GenericEntityException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Ext_EdG on 7/6/2020.
 */
public class JiraInterface {
    ProjectMonitorImpl mainClass;
    ApplicationUser applicationUser;
    SearchService searchService;
    public JiraInterface(ProjectMonitorImpl mainClass, ApplicationUser applicationUser, SearchService searchService)
    {
        this.mainClass = mainClass;
        this.applicationUser = applicationUser;
        this.searchService = searchService;
    }
    public List<Issue> getAllIssues(Project currentProject) {
        mainClass.WriteToStatus(false, "In JiraInterface Getting all issues");
        IssueManager issueManager = ComponentAccessor.getIssueManager();
        Collection<Long> allIssueIds = null;
        try {
            allIssueIds = issueManager.getIssueIdsForProject(currentProject.getId());
        } catch (GenericEntityException e) {
            mainClass.WriteToStatus(false, "Failed to get all issues " + e.toString());
            System.out.println("Failed to get issue ids " + e.toString());
        }
        List<Issue>	allIssues = issueManager.getIssueObjects(allIssueIds);
        return allIssues;
    }

    public List<Issue> getIssues(ApplicationUser applicationUser, Project currentProject) {
        JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
        Query query = jqlClauseBuilder.project(currentProject.getKey()).buildQuery();
        PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(applicationUser, query, pagerFilter);
        } catch (SearchException e) {
            mainClass.WriteToStatus(true, "In JiraInterface exception " + e.toString());
        }
        if (searchResults == null)
        {
            return null;
        }
        else {
            //ugly situation - jira replaced API from getIssues to getResult :(
            Method newGetMethod = null;
            List<Issue> result;
            try {
                newGetMethod = SearchResults.class.getMethod("getIssues");
            } catch (NoSuchMethodException e) {
                try {
                    newGetMethod = SearchResults.class.getMethod("getResults");
                } catch (NoSuchMethodException e1) {
                }
            }
            if (newGetMethod != null) {
                try {
                    result = (List<Issue>)newGetMethod.invoke(searchResults);
                } catch (IllegalAccessException e) {
                    result = null;
                } catch (InvocationTargetException e) {
                    result = null;
                }
            }
            else
            {
                result = null;
            }
            return result;
        }
    }

    public Collection<PlaygileSprint> getAllSprintsForIssue(Issue issue)
    {
        Collection<PlaygileSprint> result = new ArrayList<>();
        String[] allSprintsAsStrings = getSpecificCustomFields(issue, "Sprint");
        if (allSprintsAsStrings != null && allSprintsAsStrings.length > 0)
        {
            for (String item : allSprintsAsStrings) {
                PlaygileSprint playgileSprint = new PlaygileSprint();
                playgileSprint = playgileSprint.parse(item);
                if (playgileSprint != null) result.add(playgileSprint);
            }
        }

        return result;
    }
    public double getStoryPointsForIssue(Issue issue)
    {
        double result = -1;
        String[] values = getSpecificCustomFields(issue, "Story Points");
        if (values != null && values.length > 0)
        {
            try {
                result = Double.parseDouble(values[0]);
            }
            catch (Exception ex)
            {
                result = -3;
            }
        }
        else
        {
            if (values == null) result = -1; //no story points at all - null returned
            else result = -2; //no story points values
        }
        return result;
    }


    public Collection<String> getAllVersionsForProject(Project currentProject)
    {
        ArrayList<String> result = new ArrayList<>();
        VersionManager vm = ComponentAccessor.getVersionManager();
        Collection<Project> projects = new ArrayList<>();
        projects.add(currentProject);
        Collection<Version> versions = vm.getAllVersionsForProjects(projects, false);
        for (Version version : versions)
        {
            result.add(version.getName());
        }
        return result;
    }

    private String[] getSpecificCustomFields(Issue issue, String key)
    {
        String[] result = null;
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        Collection<CustomField> fields = customFieldManager.getCustomFieldObjectsByName(key);
        if (fields != null && fields.size() > 0)
        {
            result = new String[fields.size()];
            int i = 0;
            for (CustomField field : fields)
            {
                try {
                    result[i] = field.getValue(issue).toString();
                }
                catch (Exception e)
                {
                    result[i] = "";
                }
                finally {
                    if (result[i] == null)
                    {
                        System.out.println("$$$ NULL");
                        result[i] = "";
                    }
                }
                i++;
            }
        }
        return result;
    }
    private String getAllCustomFieldsForIssue(Issue issue)
    {
        StringBuilder result = new StringBuilder("Custom objects: ");
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();

        List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue);
        for (CustomField tmpField : customFields)
        {
            result.append("\n");
            result.append(tmpField.getFieldName() + "=");
            result.append(tmpField.getValue(issue));
        }
        return result.toString();
    }


}
