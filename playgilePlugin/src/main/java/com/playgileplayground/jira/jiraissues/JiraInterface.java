package com.playgileplayground.jira.jiraissues;


import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.impl.GroupByTypes;
import com.playgileplayground.jira.impl.ProjectConfigurationModel;
import com.playgileplayground.jira.impl.RoadmapFeatureDescriptor;
import com.playgileplayground.jira.impl.StatusText;
import com.playgileplayground.jira.impl.ViewTypes;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class JiraInterface {
    ApplicationUser applicationUser;
    SearchService searchService;
    String jiraVersion;
    ActiveObjects ao;

    public JiraInterface(ActiveObjects ao, ApplicationUser applicationUser, SearchService searchService) {
        this.ao = ao;
        this.applicationUser = applicationUser;
        this.searchService = searchService;
        this.jiraVersion = ComponentAccessor.getComponent(BuildUtilsInfo.class).getVersion();
    }

    public Issue getIssueByKey(String projectKey, String issueKey) {
        Issue result;

        Query query;
        String searchString = "project = \"" + projectKey + "\" AND issueKey = " + issueKey;
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
        if (searchResults == null) {
            result = null;
        } else {
            List<Issue> tmpList = this.AccessVersionIndependentListOfIssues(searchResults);
            if (tmpList != null && tmpList.size() > 0) {
                result = tmpList.get(0); //first
            } else {
                result = null;
            }
        }

        return result;
    }

    public List<Issue> getAllProductRelatedIssues(RoadmapFeatureDescriptor roadmapFeature) {
        //get all our issues for Feature
        if (roadmapFeature == null) return null;
        Query query;
        //issuefunction in linkedIssuesOf('id=PKP-85')

        String searchString = "issue in linkedIssues(\"" + roadmapFeature.Key + "\")";
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
        List<Issue> issues;

        if (searchResults != null) {
            issues = this.AccessVersionIndependentListOfIssues(searchResults);
            if (issues == null || issues.size() <= 0) {
                issues = null;
            }
        } else {
            issues = null;
        }

        return issues;
    }

    public List<Issue> getIssuesForRoadmapFeatureOrEpics(Project currentProject, Issue roadmapFeature) {
        //get all linked epics for Feature
        ManageActiveObjects mao = new ManageActiveObjects(ao);
        ManageActiveObjectsResult maor = mao.GetProjectConfiguration(new ManageActiveObjectsEntityKey(currentProject.getKey(),
            ProjectMonitor.PROJECTCONFIGURATIONKEYNAME));
        ProjectConfigurationModel configuration = (ProjectConfigurationModel) maor.Result;
        if (roadmapFeature == null) return null;

        String searchString;

        /*
        if (viewType.equals(ProjectMonitor.ROADMAPFEATUREKEY)) {
            if (!jiraVersion.startsWith("7.")) //higher than 7
                searchString = "project = " + currentProject.getKey() + " AND issueFunction in linkedIssuesOf(\"issueKey=" + roadmapFeature.getKey() + "\",\"Is Parent task of:\")";
            else searchString = "project = " + currentProject.getKey() + " AND issue in linkedIssues(\"" + roadmapFeature.getKey() + "\")";
            //as asked by Dima Gil - don't count not needed epic links
            // issueFunction in linkedIssuesOf("issuekey = BINGOBLITZ-119252","Is Parent task of:")
        } else {
                searchString = "project = " + currentProject.getKey() + " AND issuetype = " + viewType;
        }
        */

        if (configuration.getViewType().equals(ViewTypes.ROADMAP_FEATURE)) {
            searchString = "issueFunction in linkedIssuesOf(\"issueKey=" + roadmapFeature.getKey() + "\", \"Is Parent task of:\") and issuetype = Epic";
            //as asked by Dima Gil - don't count not needed epic links
            // issueFunction in linkedIssuesOf("issuekey = BINGOBLITZ-119252","Is Parent task of:")
        } else {
            searchString = "issueFunction in issuesInEpics(\"issuekey=" + roadmapFeature.getKey() + "\")";
        }

        if (configuration.isProjectEpics()) {
            searchString = "project = " + currentProject.getKey() + " and " + searchString;
        }

        SearchResults searchResults = getSearchResult(searchString);
        List<Issue> issues = new ArrayList<>();

        if (configuration.getViewType().equals(ViewTypes.ROADMAP_FEATURE)) {
            List<Issue> issueLinks;
            if (searchResults != null) {
                StatusText.getInstance().add(true, "Accessing list of issues for " + roadmapFeature.getKey());
                issueLinks = this.AccessVersionIndependentListOfIssues(searchResults);
                if (issueLinks != null && issueLinks.size() > 0) {
                    StatusText.getInstance().add(true, "Found " + issueLinks.size() + " issue links for feature " + roadmapFeature.getKey());
                    for (Issue issueLink : issueLinks) {

                        //TEST
                        StatusText.getInstance().add(true, "Issue link id " + issueLink.getKey());

                        //get all issues with epics from all issueLinks
                        List<Issue> nextEpicIssues = getIssuesByEpic(issueLink, currentProject, configuration.isProjectTickets());
                        if (nextEpicIssues != null && nextEpicIssues.size() > 0) {
                            //TEST
                            StatusText.getInstance().add(true, "for issue link id " + issueLink.getId() + " got next epic issues " + nextEpicIssues.size());

                            //add to initial array
                            issues.addAll(nextEpicIssues);
                        }
                    }
                } else {
                    StatusText.getInstance().add(true, "Returned no issue links for " + roadmapFeature.getKey());
                    issues = null;
                }
            } else {
                StatusText.getInstance().add(true, "Search result is null for " + roadmapFeature.getKey());
                issues = null;
            }
        } else {
            List<Issue> issuesList = this.AccessVersionIndependentListOfIssues(searchResults);

            //add to initial array
            issues.addAll(issuesList);
        }


        return issues;
    }


    public List<Issue> getActiveRoadmapFeaturesOrEpics(Project currentProject, String featureKey) {
        Query query;
        //query project = "Bingo Blitz 2.0" and issuetype = "Roadmap Feature" and (status !=  Cancelled or status != Go-Live)
        String searchString = "project = \"" + currentProject.getName() + "\" and issuetype = \"" + featureKey + "\" and status not in (Cancelled, Go-Live, \"On Hold\", Closed, Done)";

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
        if (searchResults == null) {
            return null;
        } else {
            return this.AccessVersionIndependentListOfIssues(searchResults);
        }
    }

    public ArrayList<Issue> getRoadmapFeaturesInPreparationPhase(Project currentProject, String featureKey) {
        Query query;
        //project="PK Features" AND issuetype="Roadmap Feature" and issueLinkType="Is Parent task of:" AND Status!=Done AND Status!=Resolved AND Status!=Closed

        String searchString;

        searchString = "project = \"" + currentProject.getName() + "\" and issuetype = \"" + featureKey + "\" AND Status=\"PREP FOR INTEGRATION\"";

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
            return null;
        else {
            return new ArrayList<>(this.AccessVersionIndependentListOfIssues(searchResults));
        }
    }

    public List<Issue> getAllStories(Project currentProject, String issueKey) {
        Query query;
        //project="BINGOBLITZ" AND issuetype="Story"

        String searchString;

        searchString = "project = \"" + currentProject.getName() + "\" and issuetype = \"" + issueKey + "\"";

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
            return null;
        else {
            return new ArrayList<>(this.AccessVersionIndependentListOfIssues(searchResults));
        }
    }

    public List<Issue> getIssuesByEpic(Issue epic, Project currentProject, boolean isProjectTickets) {
        if (epic == null) return null;

        String searchString = "issueFunction in issuesInEpics(\"issuekey=" + epic.getKey() + "\")";

        if (isProjectTickets) {
            searchString = "project = " + currentProject.getKey() + " and " + searchString;
        }

        SearchResults searchResults = getSearchResult(searchString);
        if (searchResults == null) {
            return null;
        } else {
            return this.AccessVersionIndependentListOfIssues(searchResults);
        }
    }

    public List<Issue> getInitiative(Issue issue) {
        if (issue == null) return null;

        String searchString = "issueFunction in linkedIssuesOf(\"issueKey=" + issue.getKey() + "\")";

        SearchResults searchResults = getSearchResult(searchString);
        List<Issue> allLinkedIssues;
        List<Issue> initiatives = new ArrayList<>();
        if (searchResults == null) {
            return null;
        } else {
            allLinkedIssues = this.AccessVersionIndependentListOfIssues(searchResults);
        }
        for (Issue issueFromList : allLinkedIssues) {
            if (Objects.requireNonNull(issueFromList.getIssueType()).getName().equalsIgnoreCase(GroupByTypes.INITIATIVE)) {
                initiatives.add(issueFromList);
            }
        }
        return initiatives;
    }


    public Collection<PlaygileSprint> getAllSprintsForIssue(Issue issue) {
        Collection<PlaygileSprint> result = new ArrayList<>();
        String[] allSprintsAsStrings = getSpecificCustomFields(issue, "Sprint");
        if (allSprintsAsStrings != null && allSprintsAsStrings.length > 0) {
            for (String item : allSprintsAsStrings) {
                PlaygileSprint playgileSprint = new PlaygileSprint();
                playgileSprint = playgileSprint.parse(item);
                if (playgileSprint != null) result.add(playgileSprint);
            }
        }

        return result;
    }

    public JiraQueryResult getBusinessApprovalDateForIssue(Issue issue) {
        JiraQueryResult result = new JiraQueryResult();
        String[] values = getSpecificCustomFields(issue, "Business Approval");
        if (values != null && values.length > 0) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            try {
                result.Result = format.parse(values[0]);
            } catch (ParseException e) {
                result.Code = JiraQueryResult.STATUS_CODE_DATE_PARSE_ERROR;
                result.Message = "Invalid Date format";
                result.Result = values[0];
            }
        } else {
            result.Code = JiraQueryResult.STATUS_CODE_NO_SUCH_FIELD;
            result.Message = "Business Approval Date is missing";
            result.Result = null;
        }
        return result;
    }

    public String getTeamNameForIssue(Issue issue) {
        String result = "";
        String[] values = getSpecificCustomFields(issue, "Team");
        if (values != null && values.length > 0) {
            result = values[0];
        }
        return result;
    }

    public double getStoryPointsForIssue(Issue issue) {
        double result;
        String[] values = getSpecificCustomFields(issue, "Story Points");
        if (values != null && values.length > 0) {
            try {
                result = Double.parseDouble(values[0]);
            } catch (Exception ex) {
                result = -3;
            }
        } else {
            if (values == null) result = -1; //no story points at all - null returned
            else result = -2; //no story points values
        }
        return result;
    }

    public String[] getSpecificCustomFields(Issue issue, String key) {
        String[] result = new String[]{""};
        CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();
        Collection<CustomField> fields = customFieldManager.getCustomFieldObjectsByName(key);
        if (fields != null && fields.size() > 0) {
            result = new String[fields.size()];
            int i = 0;
            for (CustomField field : fields) {
                try {
                    result[i] = field.getValue(issue).toString();
                } catch (Exception e) {
                    result[i] = "";
                } finally {
                    if (result[i] == null) {
                        System.out.println("$$$ NULL");
                        result[i] = "";
                    }
                }
                i++;
            }
        }
        return result;
    }

    public List<Issue> getTasksForPreparationFeature(Issue roadmapFeature) {
        //get all linked epics for Feature
        if (roadmapFeature == null) return null;

        Query query;

        String searchString = "issue in linkedIssues(\"" + roadmapFeature.getKey() + "\") and issuetype=Task";

        SearchResults searchResults = getSearchResult(searchString);
        List<Issue> tasks;
        if (searchResults != null) {
            StatusText.getInstance().add(true, "Accessing list of tasks for " + roadmapFeature.getKey());
            tasks = this.AccessVersionIndependentListOfIssues(searchResults);
            if (tasks != null && tasks.size() > 0) {
            } else {
                StatusText.getInstance().add(true, "Returned no tasks for " + roadmapFeature.getKey());
                tasks = null;
            }
        } else {
            StatusText.getInstance().add(true, "Search result is null for " + roadmapFeature.getKey());
            tasks = null;
        }

        return tasks;
    }

    private SearchResults getSearchResult(String searchString) {
        JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
        Query query;
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
            StatusText.getInstance().add(true, "Exception during execution JQL: " + searchString);
        }
        return searchResults;
    }

    private List<Issue> AccessVersionIndependentListOfIssues(SearchResults searchResults) {
        //ugly situation - jira replaced API from getIssues to getResult :(
        if (searchResults == null) {
            StatusText.getInstance().add(true, "In AccessVersionIndependentListOfIssues - Search results is null");
        }
        Method newGetMethod = null;
        List<Issue> result;
        try {
            newGetMethod = SearchResults.class.getMethod("getIssues");
        } catch (NoSuchMethodException e) {
            try {
                newGetMethod = SearchResults.class.getMethod("getResults");
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (newGetMethod != null) {
            StatusText.getInstance().add(true, "In AccessVersionIndependentListOfIssues - Before calling get results on search results");
            try {
                result = (List<Issue>) newGetMethod.invoke(searchResults);
            } catch (IllegalAccessException e) {
                StatusText.getInstance().add(true, "In AccessVersionIndependentListOfIssues - Access exception");
                result = null;
            } catch (InvocationTargetException e) {
                StatusText.getInstance().add(true, "In AccessVersionIndependentListOfIssues - Invocation exception");
                result = null;
            }
        } else {
            StatusText.getInstance().add(true, "Cannot call get results on search results");
            result = null;
        }
        return result;
    }

}
