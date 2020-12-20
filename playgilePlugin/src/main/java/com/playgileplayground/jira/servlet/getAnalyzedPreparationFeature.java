/**
 * Created by on 11/12/2020.
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
import com.playgileplayground.jira.impl.FeatureScore;
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;
import com.playgileplayground.jira.impl.RoadmapFeatureAnalysis;
import com.playgileplayground.jira.impl.StatusText;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.PlaygileSprint;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.projectprogress.DateAndValues;
import com.playgileplayground.jira.projectprogress.ProjectProgressResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

@Scanned
public class getAnalyzedPreparationFeature extends HttpServlet {
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    ActiveObjects ao;

    public getAnalyzedPreparationFeature(ActiveObjects ao,TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService)
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
        GetAnalyzedFeatureResponse ourResponse = new GetAnalyzedFeatureResponse();
        try {
            //first check user
            JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
            ApplicationUser applicationUser = jac.getLoggedInUser();
            if (applicationUser == null)
            {
                ourResponse.statusMessage = "User authentication failure";
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            String projectKey = Optional.ofNullable(req.getParameter("projectKey")).orElse("");
            String roadmapFeatureName = Optional.ofNullable(req.getParameter("roadmapFeature")).orElse("");
            if (projectKey.isEmpty() || roadmapFeatureName.isEmpty()) {
                ourResponse.statusMessage = "Project key and/or roadmap feature is missing " + projectKey + " " + roadmapFeatureName;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            boolean bSendLog = req.getParameter("sendLog") != null;

            //prepare to walk through the features
            ManageActiveObjects mao = new ManageActiveObjects(this.ao);
            JiraInterface jiraInterface = new JiraInterface(applicationUser, searchService);

            Project currentProject = projectManager.getProjectByCurrentKey(projectKey);
            if (currentProject == null) {
                ourResponse.statusMessage = "Failed to find current project " + projectKey;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            ProjectMonitoringMisc projectMonitoringMisc = new ProjectMonitoringMisc(jiraInterface);

            StatusText.getInstance().add(true, "Getting roadmap feature issues for " + roadmapFeatureName);
            Issue selectedRoadmapFeatureIssue = jiraInterface.getIssueByKey(currentProject.getKey(), roadmapFeatureName);
            if (selectedRoadmapFeatureIssue == null) //not found
            {
                ourResponse.statusMessage = "Failed to find the selected feature in Jira " + roadmapFeatureName;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }
            resp.setContentType("text/html;charset=utf-8");

        }
        catch (Exception e)
        {
            ourResponse.statusMessage = "Route exception " + ProjectMonitoringMisc.getExceptionTrace(e);
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
        }
    }
}

class GetAnalyzedPreparationFeatureResponse
{

    public String statusMessage = "";
    public String logInfo = "";
    public String summary;
    public String key;
    public String teamName = "";
    public Date startDateRoadmapFeature;
    public Date targetDate;


    public FeatureScore qualityScore;


    public void fillTheFields(RoadmapFeatureAnalysis roadmapFeatureAnalysis)
    {

    }
}
