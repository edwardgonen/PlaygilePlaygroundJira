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
import com.playgileplayground.jira.impl.StatusText;
import com.playgileplayground.jira.jiraissues.JiraInterface;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Scanned
public class getPreparationFeatures extends HttpServlet {
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    ActiveObjects ao;

    public getPreparationFeatures(ActiveObjects ao,TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService)
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
        GetPreparationFeaturesResponse ourResponse = new GetPreparationFeaturesResponse();
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
            if (projectKey.isEmpty()) {
                ourResponse.statusMessage = "Project key is missing";
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            //prepare to walk through the features

            JiraInterface jiraInterface = new JiraInterface(applicationUser, searchService);

            Project currentProject = projectManager.getProjectByCurrentKey(projectKey);
            if (currentProject == null) {
                ourResponse.statusMessage = "Failed to find project by key " + projectKey;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            List<Issue> roadmapFeatures = jiraInterface.getRoadmapFeaturesInPreparationPhase(applicationUser, currentProject, ProjectMonitor.ROADMAPFEATUREKEY);
            if (roadmapFeatures == null) {
                ourResponse.statusMessage = "Failed to find any feature for " + projectKey;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            //prepare list of short feature descriptors
            if (roadmapFeatures.size() > 0)
            {
                //convert to string list
                for (Issue feature : roadmapFeatures)
                {
                    PreparationFeatureShortDescriptor afsd = new PreparationFeatureShortDescriptor();
                    afsd.featureKey = feature.getKey();
                    afsd.setFeatureSummary(feature.getSummary());
                    ourResponse.featuresList.add(afsd);
                }
                Collections.sort(ourResponse.featuresList);//sort alphabetically for better user experience
            }
            else
            {
                ourResponse.statusMessage = "No features in preparation status found for " + projectKey;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }
            resp.setContentType("text/html;charset=utf-8");
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
        }
        catch (Exception e)
        {
            ourResponse.statusMessage = "Route exception " + ProjectMonitoringMisc.getExceptionTrace(e);
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
        }

    }
}

class GetPreparationFeaturesResponse
{
    public String statusMessage = "";
    public ArrayList<PreparationFeatureShortDescriptor> featuresList = new ArrayList<>();
}
class PreparationFeatureShortDescriptor implements Comparator<PreparationFeatureShortDescriptor>, Comparable<PreparationFeatureShortDescriptor>
{
    public String featureKey;
    private String featureSummary;

    public void setFeatureSummary(String featureSummary)
    {
        this.featureSummary = featureSummary;
    }
    @Override
    public int compareTo(PreparationFeatureShortDescriptor o) {
        return featureSummary.compareTo(o.featureSummary);
    }

    @Override
    public int compare(PreparationFeatureShortDescriptor o1, PreparationFeatureShortDescriptor o2) {
        return o1.featureSummary.compareTo(o2.featureSummary);
    }
}
