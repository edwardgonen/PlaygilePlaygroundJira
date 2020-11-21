package com.playgileplayground.jira.jiraissues;


import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.playgileplayground.jira.impl.OldProjectMonitorImpl;
import com.playgileplayground.jira.impl.ProjectMonitorImpl;
import com.playgileplayground.jira.impl.RoadmapFeatureDescriptor;
import com.playgileplayground.jira.impl.StatusText;
import org.ofbiz.core.entity.GenericEntityException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
    String jiraVersion;
    public JiraInterface(ProjectMonitorImpl mainClass, ApplicationUser applicationUser, SearchService searchService)
    {
        this.mainClass = mainClass;
        this.applicationUser = applicationUser;
        this.searchService = searchService;
        this.jiraVersion = ComponentAccessor.getComponent(BuildUtilsInfo.class).getVersion();
    }
    public JiraInterface(OldProjectMonitorImpl mainClass, ApplicationUser applicationUser, SearchService searchService)
    {
//for compatibility. remove later
    }
    public JiraInterface(ApplicationUser applicationUser, SearchService searchService)
    {
        this.applicationUser = applicationUser;
        this.searchService = searchService;
        this.jiraVersion = ComponentAccessor.getComponent(BuildUtilsInfo.class).getVersion();
    }
    public List<Issue> getAllIssues(Project currentProject) {
        //mainClass.WriteToStatus(false, "In JiraInterface Getting all issues");
        IssueManager issueManager = ComponentAccessor.getIssueManager();
        Collection<Long> allIssueIds = null;
        try {
            allIssueIds = issueManager.getIssueIdsForProject(currentProject.getId());
        } catch (GenericEntityException e) {
            //mainClass.WriteToStatus(false, "Failed to get all issues " + e.toString());
            //System.out.println("Failed to get issue ids " + e.toString());
        }
        List<Issue>	allIssues = issueManager.getIssueObjects(allIssueIds);
        return allIssues;
    }
    public List<Issue> getAllProductRelatedIssues(RoadmapFeatureDescriptor roadmapFeature)
    {
        //get all our issues for Feature
        if (roadmapFeature == null) return null;
        Query query;
        //issuefunction in linkedIssuesOf('id=PKP-85')

        String searchString;
        if (!jiraVersion.startsWith("7.")) //higher than 7
            searchString = "issueFunction in linkedIssuesof(\"id=" + roadmapFeature.Key + "\")";
        else searchString = "issue in linkedIssues(\"" + roadmapFeature.Key + "\")";
        JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
        try {
            query = jqlQueryParser.parseQuery(searchString);
        } catch (JqlParseException e) {
            return null;
        }

        PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(applicationUser, query, pagerFilter);
        } catch (SearchException e) {
            //mainClass.WriteToStatus(true, "In JiraInterface exception " + e.toString());
        }
        List<Issue> issues = new ArrayList<>();

        if (searchResults != null)
        {
            issues = this.AccessVersionIndependentListOfIssues(searchResults);
            if (issues != null && issues.size() > 0)
            {

            }
            else
            {
                issues = null;
            }
        }
        else
        {
            issues = null;
        }

        return issues;
    }

    public List<Issue> getAllRoadmapFeatures(ApplicationUser applicationUser, Project currentProject, String featureKey)
    {
        JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
        Query query = jqlClauseBuilder.project(currentProject.getKey()).and().issueType(featureKey).buildQuery();
        PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(applicationUser, query, pagerFilter);
        } catch (SearchException e) {
            //mainClass.WriteToStatus(true, "In JiraInterface exception " + e.toString());
        }
        if (searchResults == null)
        {
            return null;
        }
        else {
            return this.AccessVersionIndependentListOfIssues(searchResults);
        }
    }
    public List<Issue> getIssuesByFixVersion(ApplicationUser applicationUser, Project currentProject, String fixVersion) {
        //if the version is not defined return null. no query
        if (fixVersion == null) return null;

        Query query;
        String searchString = "project = \"" + currentProject.getKey() + "\" AND fixVersion = " + fixVersion;
        JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
        try {
            query = jqlQueryParser.parseQuery(searchString);
        } catch (JqlParseException e) {
            return null;
        }

        PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(applicationUser, query, pagerFilter);
        } catch (SearchException e) {
            //mainClass.WriteToStatus(true, "In JiraInterface exception " + e.toString());
        }
        if (searchResults == null)
        {
            return null;
        }
        else {
            return this.AccessVersionIndependentListOfIssues(searchResults);
        }
    }

    public List<Issue> getIssuesForRoadmapFeature( ApplicationUser applicationUser, Project currentProject, Issue roadmapFeature)
    {
        //get all linked epics for Feature
        if (roadmapFeature == null) return null;

        Query query;

        String searchString;
        if (!jiraVersion.startsWith("7.") && !currentProject.getKey().contains("BKM")) //higher than 7
            searchString = "issueFunction in linkedIssuesOf(\"issueKey=" + roadmapFeature.getKey() + "\",\"Is Parent task of:\")";
        else searchString = "issue in linkedIssues(\""  + roadmapFeature.getKey() +  "\")";
        //as asked by Dima Gil - don't count not needed epic links
        // issueFunction in linkedIssuesOf("issuekey = BINGOBLITZ-119252","Is Parent task of:")
        //NOTE - the above query does not work for Board Kings - the Is Parent taks of: does not return any results

        JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
        try {
            query = jqlQueryParser.parseQuery(searchString);
        } catch (JqlParseException e) {
            return null;
        }

        PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(applicationUser, query, pagerFilter);
        } catch (SearchException e) {
            //mainClass.WriteToStatus(true, "In JiraInterface exception " + e.toString());
        }
        List<Issue> issues = new ArrayList<>();
        List<Issue> issueLinks;
        if (searchResults != null)
        {
            issueLinks = this.AccessVersionIndependentListOfIssues(searchResults);
            if (issueLinks != null && issueLinks.size() > 0)
            {
                for (Issue issueLink : issueLinks) {

                    //TEST
                    StatusText.getInstance().add(true, "Issue link id " + issueLink.getKey());

                    //get all issues with epics from all issueLinks
                    List<Issue> nextEpicIssues = getIssuesByEpic(applicationUser, currentProject, issueLink);
                    if (nextEpicIssues != null && nextEpicIssues.size() > 0)
                    {
                        //TEST
                        StatusText.getInstance().add(true, "for issue link id " + issueLink.getId() + " got next epic issues " + nextEpicIssues.size());

                        //add to initial array
                        issues.addAll(nextEpicIssues);
                    }
                }
            }
            else
            {
                issues = null;
            }
        }
        else
        {
            issues = null;
        }

        return issues;
    }


    public List<Issue> getRoadmapFeaturesNotCancelledAndNotGoLiveAndNotOnHold(ApplicationUser applicationUser, Project currentProject, String featureKey)
    {
        Query query;
        //query project = "Bingo Blitz 2.0" and issuetype = "Roadmap Feature" and (status !=  Cancelled or status != Go-Live)
        String searchString = "project = \"" + currentProject.getName() + "\" and issuetype = \"" + featureKey + "\" and status != Cancelled and status != Go-Live and status != \"On Hold\"";

        JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
        try {
            query = jqlQueryParser.parseQuery(searchString);
        } catch (JqlParseException e) {
            return null;
        }

        PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(applicationUser, query, pagerFilter);
        } catch (SearchException e) {
            //mainClass.WriteToStatus(true, "In JiraInterface exception " + e.toString());
        }
        if (searchResults == null)
        {
            return null;
        }
        else {
            return this.AccessVersionIndependentListOfIssues(searchResults);
        }
    }

    public List<Issue> getRoadmapFeaturesInPreparationPhase(ApplicationUser applicationUser, Project currentProject, String featureKey)
    {
        Query query;
        //project="PK Features" AND issuetype="Roadmap Feature" and issueLinkType="Is Parent task of:" AND Status!=Done AND Status!=Resolved AND Status!=Closed

        String searchString;
        if (!jiraVersion.startsWith("7.")) //higher than 7
            searchString = "project = \"" + currentProject.getName() + "\" and issuetype = \"" + featureKey + "\" and issueLinkType=\"Is Parent task of:\" AND Status!=Done AND Status!=Resolved AND Status!=Closed";
        else searchString = "project = \"" + currentProject.getName() + "\" and issuetype = \"" + featureKey + "\" and Status!=Done AND Status!=Resolved AND Status!=Closed";

        JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
        try {
            query = jqlQueryParser.parseQuery(searchString);
        } catch (JqlParseException e) {
            return null;
        }

        PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(applicationUser, query, pagerFilter);
        } catch (SearchException e) {
            //mainClass.WriteToStatus(true, "In JiraInterface exception " + e.toString());
        }
        if (searchResults == null)
        {
            return null;
        }
        else {
            return this.AccessVersionIndependentListOfIssues(searchResults);
        }
    }

    public List<Issue> getIssuesByEpic(ApplicationUser applicationUser, Project currentProject, Issue epic) {
        //if the version is not defined return null. no query
        if (epic == null) return null;

        //project="BKM" and ("Epic Link" = BKM-2)

        Query query;
        //String searchString = "project = \"" + currentProject.getKey() + "\" AND (\"Epic Link\"=" + epic.getKey() + ")";
        //TEST
        String searchString = "(\"Epic Link\"=" + epic.getKey() + ")";
        JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
        try {
            query = jqlQueryParser.parseQuery(searchString);
        } catch (JqlParseException e) {
            return null;
        }

        PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(applicationUser, query, pagerFilter);
        } catch (SearchException e) {
            //mainClass.WriteToStatus(true, "In JiraInterface exception " + e.toString());
        }
        if (searchResults == null)
        {
            return null;
        }
        else {
            return this.AccessVersionIndependentListOfIssues(searchResults);
        }
    }
    public List<Issue> getEpics(ApplicationUser applicationUser, Project currentProject) {
        JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
        Query query = jqlClauseBuilder.project(currentProject.getKey()).and().issueType("Epic").buildQuery();
        PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();
        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(applicationUser, query, pagerFilter);
        } catch (SearchException e) {
            //mainClass.WriteToStatus(true, "In JiraInterface exception " + e.toString());
        }
        if (searchResults == null)
        {
            return null;
        }
        else {
            return this.AccessVersionIndependentListOfIssues(searchResults);
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
    public JiraQueryResult getBusinessApprovalDateForIssue(Issue issue, DateTimeFormatter formatter)
    {
        JiraQueryResult result = new JiraQueryResult();
        String[] values = getSpecificCustomFields(issue, "Business Approval");
        if (values != null && values.length > 0)
        {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            try {
                result.Result = format.parse(values[0]);
            } catch (ParseException e)
            {
                result.Code = JiraQueryResult.STATUS_CODE_DATE_PARSE_ERROR;
                result.Message = "Invalid Date format";
                result.Result = values[0];
            }
        }
        else
        {
            result.Code = JiraQueryResult.STATUS_CODE_NO_SUCH_FIELD;
            result.Message = "Business Approval Date is missing";
            result.Result = null;
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


    public ArrayList<String> getAllVersionsForProject(Project currentProject)
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
    public String getAllCustomFieldsForIssue(Issue issue)
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

    private List<Issue> AccessVersionIndependentListOfIssues(SearchResults searchResults)
    {
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
