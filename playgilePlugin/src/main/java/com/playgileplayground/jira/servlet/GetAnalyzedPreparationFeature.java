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
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;
import com.playgileplayground.jira.impl.ProjectPreparationMisc;
import com.playgileplayground.jira.impl.RoadmapFeaturePreparationAnalysis;
import com.playgileplayground.jira.impl.StatusText;
import com.playgileplayground.jira.jiraissues.JiraInterface;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Scanned
public class GetAnalyzedPreparationFeature extends HttpServlet {
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    ActiveObjects ao;

    public GetAnalyzedPreparationFeature(ActiveObjects ao, TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService)
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
        AnalyzedPreparationFeatureResponse ourResponse = new AnalyzedPreparationFeatureResponse();
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
            String featureName = Optional.ofNullable(req.getParameter("feature")).orElse("");
            if (projectKey.isEmpty() || featureName.isEmpty()) {
                ourResponse.statusMessage = "Project key and/or roadmap feature is missing " + projectKey + " " + featureName;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            boolean bSendLog = req.getParameter("sendLog") != null;

            Project currentProject = projectManager.getProjectByCurrentKey(projectKey);
            if (currentProject == null) {
                ourResponse.statusMessage = "Failed to find current project " + projectKey;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }
            JiraInterface jiraInterface = new JiraInterface(ao, applicationUser, searchService);
            ProjectPreparationMisc projectPreparationMisc = new ProjectPreparationMisc(jiraInterface);
            StatusText.getInstance().add(true, "Getting roadmap feature issues for " + featureName);
            Issue selectedRoadmapFeatureIssue = jiraInterface.getIssueByKey(currentProject.getKey(), featureName);
            if (selectedRoadmapFeatureIssue == null) //not found
            {
                ourResponse.statusMessage = "Failed to find the selected feature in Jira " + featureName;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }
            resp.setContentType("text/html;charset=utf-8");
            //get the feature, analyze it and convert it to our response
            RoadmapFeaturePreparationAnalysis roadmapFeaturePreparationAnalysis = new RoadmapFeaturePreparationAnalysis(
                selectedRoadmapFeatureIssue,
                jiraInterface,
                projectPreparationMisc);
            if (roadmapFeaturePreparationAnalysis.analyzePreparationFeature()) { //we take all successfully analyzed features - started or non-started
                ourResponse.roadmapFeaturePreparationAnalysis = roadmapFeaturePreparationAnalysis;
                if (bSendLog)
                {
                    ourResponse.logInfo = StatusText.getInstance().toString();
                }
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
            } else //failed to analyze feature
            {
                ourResponse.statusMessage = "Failed to analyze feature " + featureName;
                if (bSendLog)
                {
                    ourResponse.logInfo = StatusText.getInstance().toString();
                }
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
            }
        }
        catch (Exception e)
        {
            ourResponse.statusMessage = "Route exception " + ProjectMonitoringMisc.getExceptionTrace(e);
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
        }
    }
}

class AnalyzedPreparationFeatureResponse
{
    public String statusMessage = "";
    public String logInfo = "";
    public RoadmapFeaturePreparationAnalysis roadmapFeaturePreparationAnalysis;
}

