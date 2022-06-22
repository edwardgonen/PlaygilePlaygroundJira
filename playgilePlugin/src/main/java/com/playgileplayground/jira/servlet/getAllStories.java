package com.playgileplayground.jira.servlet;

    import com.atlassian.activeobjects.external.ActiveObjects;
    import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;
import com.playgileplayground.jira.impl.StatusText;
import com.playgileplayground.jira.jiraissues.JiraInterface;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Scanned
public class GetAllStories extends HttpServlet {
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    @ComponentImport
    ActiveObjects ao;

    public GetAllStories(ActiveObjects ao, TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService)
    {
        this.ao = ao;
        this.templateRenderer = templateRenderer;
        this.projectManager = projectManager;
        this.searchService = searchService;
    }
    @Override
    @Transactional
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req,  resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StatusText.getInstance().reset();
        GetAllStoriesResponse ourResponse = new GetAllStoriesResponse();
        try {
            //first check user
            JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
            ApplicationUser applicationUser = jac.getLoggedInUser();
            if (applicationUser == null)
            {
                servletMisc.simpleResponseToWeb("User authentication failure", resp);
                return;
            }

            String projectKey = Optional.ofNullable(req.getParameter("projectKey")).orElse("");
            if (projectKey.isEmpty()) {
                servletMisc.simpleResponseToWeb("Project key is missing", resp);
                return;
            }

            String issueTypeKey = Optional.ofNullable(req.getParameter("issueType")).orElse("story");

            JiraInterface jiraInterface = new JiraInterface(ao, applicationUser, searchService);

            Project currentProject = projectManager.getProjectByCurrentKey(projectKey);
            if (currentProject == null) {
                servletMisc.simpleResponseToWeb("Failed to find project by key " + projectKey, resp);
                return;
            }

            List<Issue> stories = jiraInterface.getAllStories(currentProject, issueTypeKey);
            if (stories == null) {
                servletMisc.simpleResponseToWeb("Failed to find any issue for " + projectKey, resp);
                return;
            }

            //prepare list of short feature descriptors
            if (stories.size() > 0)
            {
                //convert to our list
                for (Issue story  : stories)
                {
                    StoryDescriptor sd = new StoryDescriptor();
                    sd.storyKey = story.getKey();
                    sd.storySummary = story.getSummary();
                    sd.storyEstimation = jiraInterface.getStoryPointsForIssue(story);
                    ourResponse.storiesList.add(sd);
                }
                servletMisc.responseSimpleListToWeb(ourResponse.getStoriesList(), resp);
            }
            else
            {
                servletMisc.simpleResponseToWeb("No stories found for " + projectKey, resp);
            }
        }
        catch (Exception e)
        {
            servletMisc.simpleResponseToWeb("Route exception " + ProjectMonitoringMisc.getExceptionTrace(e), resp);
        }

    }
}

class GetAllStoriesResponse
{
    final String DELIMITER = "|";
    public ArrayList<StoryDescriptor> storiesList = new ArrayList<>();
    public ArrayList<String> getStoriesList()
    {
        ArrayList<String> result = new ArrayList<>();
        for (StoryDescriptor story : storiesList)
        {
            String line = story.storyEstimation + DELIMITER + story.storySummary + DELIMITER + story.storyKey;
            result.add(line);
        }
        return result;
    }
}
class StoryDescriptor
{
    public String storyKey;
    public String storySummary;
    public double storyEstimation;
}
