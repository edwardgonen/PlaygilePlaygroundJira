package com.playgileplayground.jira.jiraissues;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.user.ApplicationUser;
import org.ofbiz.core.entity.GenericEntityException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Ext_EdG on 7/6/2020.
 */
public class JiraInterface {
    public List<Issue> getAllIssues(ApplicationUser applicationUser, Project currentProject) {
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
    public String[] getAllSprintsForIssue(Issue issue)
    {
        return getSpecificCustomFields(issue, "Sprint");
    }
    public String[] getStoryPointsForIssue(Issue issue)
    {
        return getSpecificCustomFields(issue, "Story Points");
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
