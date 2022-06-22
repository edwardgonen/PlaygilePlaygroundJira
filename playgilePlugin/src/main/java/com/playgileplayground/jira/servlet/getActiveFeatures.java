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
import com.playgileplayground.jira.impl.ProjectConfigurationModel;
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;
import com.playgileplayground.jira.impl.StatusText;
import com.playgileplayground.jira.impl.ViewTypes;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Scanned
public class GetActiveFeatures extends HttpServlet {
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    @ComponentImport
    ActiveObjects ao;

    public GetActiveFeatures(ActiveObjects ao, TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService) {
        this.ao = ao;
        this.templateRenderer = templateRenderer;
        this.projectManager = projectManager;
        this.searchService = searchService;
    }

    @Override
    @Transactional
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StatusText.getInstance().reset();
        GetActiveFeaturesResponse ourResponse = new GetActiveFeaturesResponse();
        try {
            //first check user
            JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
            ApplicationUser applicationUser = jac.getLoggedInUser();
            if (applicationUser == null) {
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

            JiraInterface jiraInterface = new JiraInterface(ao, applicationUser, searchService);

            Project currentProject = projectManager.getProjectByCurrentKey(projectKey);
            if (currentProject == null) {
                ourResponse.statusMessage = "Failed to find project by key " + projectKey;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            //We need to check project configuration - do we want epics or roadmap features
            ManageActiveObjects mao = new ManageActiveObjects(ao);
            ManageActiveObjectsResult maor = mao.GetProjectConfiguration(new ManageActiveObjectsEntityKey(projectKey, ProjectMonitor.PROJECTCONFIGURATIONKEYNAME));
            String viewType = ViewTypes.ROADMAP_FEATURE;
            ProjectConfigurationModel config;
            if (maor.Result != null) //we got something from the database
            {
                config = (ProjectConfigurationModel) maor.Result;
                viewType = config.getViewType();
            } else {
                config = new ProjectConfigurationModel();
                ManageActiveObjectsEntityKey entityKey = new ManageActiveObjectsEntityKey(projectKey, ProjectMonitor.PROJECTCONFIGURATIONKEYNAME);
                mao.SetProjectConfiguration(mao, entityKey, config);
                StatusText.getInstance().add(true, "Configuration was created for " + projectKey + " and View Type is " + viewType +
                    ". Probably it is first run for this project. ");
            }

            List<Issue> featuresList = jiraInterface.getActiveRoadmapFeaturesOrEpics(currentProject, viewType);
            if (featuresList == null) {
                ourResponse.statusMessage = "Failed to find any feature for " + projectKey + ". View Type is " + viewType;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            //prepare list of short feature descriptors
            if (featuresList.size() > 0) {
                //convert to string list
                for (Issue feature : featuresList) {
                    ActiveFeatureShortDescriptor afsd = new ActiveFeatureShortDescriptor();
                    afsd.featureKey = feature.getKey();
                    afsd.setFeatureSummary(feature.getSummary());
                    ourResponse.featuresList.add(afsd);
                }
                Collections.sort(ourResponse.featuresList);//sort alphabetically for better user experience
            } else {
                ourResponse.statusMessage = "No active features found for " + projectKey + ". View type is " + viewType;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }
            resp.setContentType("text/html;charset=utf-8");
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
        } catch (Exception e) {
            ourResponse.statusMessage = "Route exception " + ProjectMonitoringMisc.getExceptionTrace(e);
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
        }

    }
}

class GetActiveFeaturesResponse {
    public String statusMessage = "";
    public ArrayList<ActiveFeatureShortDescriptor> featuresList = new ArrayList<>();
}

class ActiveFeatureShortDescriptor implements Comparator<ActiveFeatureShortDescriptor>, Comparable<ActiveFeatureShortDescriptor> {
    public String featureKey;
    private String featureSummary;

    public void setFeatureSummary(String featureSummary) {
        this.featureSummary = featureSummary;
    }

    @Override
    public int compareTo(ActiveFeatureShortDescriptor o) {
        return featureSummary.compareTo(o.featureSummary);
    }

    @Override
    public int compare(ActiveFeatureShortDescriptor o1, ActiveFeatureShortDescriptor o2) {
        return o1.featureSummary.compareTo(o2.featureSummary);
    }
}
