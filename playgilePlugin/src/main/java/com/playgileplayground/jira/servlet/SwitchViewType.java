package com.playgileplayground.jira.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.base.Strings;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.impl.*;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Scanned
public class SwitchViewType extends HttpServlet {
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    @ComponentImport
    ActiveObjects ao;

    public ApplicationUser applicationUser;

    public SwitchViewType(ActiveObjects ao, TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService) {
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
        SwitchViewTypeResponse ourResponse = new SwitchViewTypeResponse();
        try {
            //first check user
            JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
            applicationUser = jac.getLoggedInUser();
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

            Project currentProject = projectManager.getProjectByCurrentKey(projectKey);
            if (currentProject == null) {
                ourResponse.statusMessage = "Failed to find project by key " + projectKey;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            ManageActiveObjects mao = new ManageActiveObjects(ao);
            ManageActiveObjectsEntityKey entityKey = new ManageActiveObjectsEntityKey(projectKey, ProjectMonitor.PROJECTCONFIGURATIONKEYNAME);
            ManageActiveObjectsResult configurationResponse;
            ManageActiveObjectsResult initialResult = mao.GetProjectConfiguration(entityKey);
            ProjectConfiguration initConfig;

            String viewType = ProjectMonitor.ROADMAPFEATUREKEY;
            if (initialResult.Result != null) //we got something from the database
            {
                ProjectConfiguration config = (ProjectConfiguration) initialResult.Result;
                viewType = config.getViewType();
                //flip the value
                switch (viewType) {
                    case ProjectMonitor.ROADMAPFEATUREKEY:
                        configurationResponse = mao.SetProjectConfiguration(mao, entityKey, new ProjectConfiguration(ProjectMonitor.EPIC));
                        break;
                    default:
                        configurationResponse = mao.SetProjectConfiguration(mao, entityKey, new ProjectConfiguration(ProjectMonitor.ROADMAPFEATUREKEY));
                }
            }
            else //failed to read from DB - probably first time
            {
                initConfig = new ProjectConfiguration(viewType);
                configurationResponse = mao.SetProjectConfiguration(mao, entityKey, initConfig);
            }


            ourResponse.statusMessage = "Switch View type Response: " + configurationResponse.Message;
            servletMisc.serializeToJsonAndSend(ourResponse, resp);

        } catch (Exception e) {
            ourResponse.statusMessage = "Route exception " + ProjectMonitoringMisc.getExceptionTrace(e);
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
        }

    }

}

class SwitchViewTypeResponse {
    public String statusMessage = "";
}

