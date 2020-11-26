/**
 * Created by Ext_EdG on 11/12/2020.
 */
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
    import com.playgileplayground.jira.api.ProjectMonitor;
    import com.playgileplayground.jira.impl.ProjectMonitoringMisc;
    import com.playgileplayground.jira.jiraissues.JiraInterface;
    import com.playgileplayground.jira.persistence.ManageActiveObjects;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    import javax.servlet.ServletException;
    import javax.servlet.http.HttpServlet;
    import javax.servlet.http.HttpServletRequest;
    import javax.servlet.http.HttpServletResponse;
    import java.io.IOException;
    import java.util.*;

@Scanned
public class getActiveFeatures extends HttpServlet {
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    ActiveObjects ao;

    public getActiveFeatures(ActiveObjects ao,TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService)
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
        Response ourResponse = new Response();
        String projectKey = Optional.ofNullable(req.getParameter("projectKey")).orElse("");
        if (projectKey.isEmpty())
        {
            ourResponse.statusMessage = "Project key is missing";
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
            return;
        }

        //prepare to walk through the features
        ManageActiveObjects mao = new ManageActiveObjects(this.ao);
        JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser applicationUser = jac.getLoggedInUser();
        JiraInterface jiraInterface = new JiraInterface(applicationUser, searchService);

        Project currentProject = projectManager.getProjectByCurrentKey(projectKey);
        if (currentProject == null)
        {
            ourResponse.statusMessage = "Failed to find project by key " + projectKey;
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
            return;
        }
        ProjectMonitoringMisc projectMonitoringMisc = new ProjectMonitoringMisc(jiraInterface, applicationUser, currentProject, mao);
        List<Issue> roadmapFeatures = jiraInterface.getRoadmapFeaturesNotCancelledAndNotGoLiveAndNotOnHold(applicationUser, currentProject, ProjectMonitor.ROADMAPFEATUREKEY);
        if (roadmapFeatures == null)
        {
            ourResponse.statusMessage = "Failed to find any feature for " + projectKey;
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
            return;
        }

        ArrayList<String> featureNames =  projectMonitoringMisc.getAllRoadmapFeatureNames(roadmapFeatures);
        Collections.sort(featureNames); //alphabetically
        ourResponse.featuresList = featureNames;
        resp.setContentType("text/html;charset=utf-8");
        servletMisc.serializeToJsonAndSend(ourResponse, resp);

    }
}

class Response
{
    public String statusMessage = "";
    public ArrayList<String> featuresList = new ArrayList<>();
}
