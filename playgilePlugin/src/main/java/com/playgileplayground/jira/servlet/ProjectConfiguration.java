package com.playgileplayground.jira.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.impl.GroupByTypes;
import com.playgileplayground.jira.impl.ProjectConfigurationModel;
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;
import com.playgileplayground.jira.impl.StatusText;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.playgileplayground.jira.impl.ViewTypes.EPIC;
import static com.playgileplayground.jira.impl.ViewTypes.ROADMAP_FEATURE;

//TODO Add logs to statusText and present it.
//TODO check if isAdmin is correct on Get
@Scanned
public class ProjectConfiguration extends HttpServlet {
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    @ComponentImport
    ActiveObjects ao;
    @ComponentImport
    PermissionManager permissionManager;

    private static final ArrayList<String> parametersNames = new ArrayList<>();
    private final ManageActiveObjects mao;


    public ProjectConfiguration(ActiveObjects ao, TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService, PermissionManager permissionManager) {
        StatusText.getInstance().reset();
        this.ao = ao;
        this.templateRenderer = templateRenderer;
        this.projectManager = projectManager;
        this.searchService = searchService;
        this.permissionManager = permissionManager;

        mao = new ManageActiveObjects(ao);
        parametersNames.addAll(new ProjectConfigurationModel().getAllFields());
    }

    @Override
    @Transactional
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        processGetRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        processPostRequest(req, resp);
    }

    private void processPostRequest(HttpServletRequest req, HttpServletResponse resp) {
        StatusText.getInstance().reset();
        boolean bSendLog = req.getParameter("sendLog") != null;
        Project currentProject = checkMainUserParameters(req);
        ProjectConfigurationResponse ourResponse;
        try {
            if (currentProject != null) {

                ManageActiveObjectsEntityKey entityKey = new ManageActiveObjectsEntityKey(currentProject.getKey(), ProjectMonitor.PROJECTCONFIGURATIONKEYNAME);
                resp.setContentType("text/html;charset=utf-8");

                ao.executeInTransaction((TransactionCallback<Void>) () -> {
                    ProjectConfigurationResponse ourInnerResponse;
                    StatusText.getInstance().add(true, "Setting configuration");
                    ManageActiveObjectsResult configurationResponse = checkInitialConfiguration(entityKey);
                    ProjectConfigurationModel config = (ProjectConfigurationModel) configurationResponse.Result;

                    if (configurationResponse.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS
                        || configurationResponse.Code == ManageActiveObjectsResult.STATUS_CODE_ENTRY_ALREADY_EXISTS) {

                        String parameterValue;

                        StringBuilder collectedStatus = new StringBuilder();
                        for (String parameterName : parametersNames) {
                            parameterValue = Optional.ofNullable(req.getParameter(parameterName)).orElse("");
                            if (!parameterValue.isEmpty()) {
                                try {
                                    collectedStatus.append(" Setting ").append(parameterName).append(" to ").append(parameterValue);
                                    switch (parameterName) {
                                        case "viewType":

                                            /* set the value */
                                            switch (parameterValue) {
                                                case ROADMAP_FEATURE:
                                                    config.setViewType(ROADMAP_FEATURE);
                                                    configurationResponse = mao.SetProjectConfiguration(mao, entityKey, config);
                                                    break;
                                                case EPIC:
                                                    config.setViewType(EPIC);
                                                    configurationResponse = mao.SetProjectConfiguration(mao, entityKey, config);
                                                    break;
                                            }

                                            break;

                                        case "groupBy":
                                            config.setGroupBy(parameterValue);
                                            configurationResponse = mao.SetProjectConfiguration(mao, entityKey, config);
                                            break;

                                        case "isProjectEpics":
                                            boolean isProjectEpics = Boolean.parseBoolean(parameterValue);
                                            if (config.getViewType().equals(ROADMAP_FEATURE)) {
                                                config.setProjectEpics(isProjectEpics);
                                                configurationResponse = mao.SetProjectConfiguration(mao, entityKey, config);
                                            }
                                            break;

                                        case "isProjectTickets":
                                            boolean isProjectTickets = Boolean.parseBoolean(parameterValue);
                                            config.setProjectTickets(isProjectTickets);
                                            configurationResponse = mao.SetProjectConfiguration(mao, entityKey, config);

                                            break;
                                    }
                                    //check save return code
                                    if (configurationResponse.Code != ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                                        //exit with error
                                        ourInnerResponse = generateSimpleErrorResponse(collectedStatus + " " + "Failed to set parameter " + parameterName + " " + configurationResponse.Message, bSendLog);
                                        servletMisc.serializeToJsonAndSend(ourInnerResponse, resp);
                                        return null;
                                    }
                                } catch (Exception e) {
                                    //exit with error
                                    ourInnerResponse = generateSimpleErrorResponse("Parsing exception " + ProjectMonitoringMisc.getExceptionTrace(e), bSendLog);
                                    servletMisc.serializeToJsonAndSend(ourInnerResponse, resp);
                                    return null;
                                }
                            }
                        }
                    } else {
                        ourInnerResponse = generateSimpleErrorResponse("Failed to find project " + currentProject.getKey(), bSendLog);
                        servletMisc.serializeToJsonAndSend(ourInnerResponse, resp);
                        return null;
                    }

                    configurationResponse = mao.GetProjectConfiguration(entityKey);
                    ourInnerResponse = generateResponse(configurationResponse);
                    servletMisc.serializeToJsonAndSend(ourInnerResponse, resp);

                    return null;
                });
            } else {
                ourResponse = generateSimpleErrorResponse("Operation forbidden due to some reason.", bSendLog);
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
            }

        } catch (Exception e) {
            ourResponse = generateSimpleErrorResponse("Route exception " + ProjectMonitoringMisc.getExceptionTrace(e), bSendLog);
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
        }
    }


    private void processGetRequest(HttpServletRequest req, HttpServletResponse resp) {
        boolean bSendLog = req.getParameter("sendLog") != null;
        Project currentProject = checkMainUserParameters(req);
        ProjectConfigurationResponse ourResponse;

        try {
            if (currentProject != null) {

                ManageActiveObjectsEntityKey entityKey = new ManageActiveObjectsEntityKey(currentProject.getKey(), ProjectMonitor.PROJECTCONFIGURATIONKEYNAME);
                ManageActiveObjectsResult configurationResponse = checkInitialConfiguration(entityKey);

                resp.setContentType("text/html;charset=utf-8");
                ourResponse = generateResponse(configurationResponse);
            } else {
                //error
                ourResponse = generateSimpleErrorResponse("Failed to retrieve current project", bSendLog);
            }
        } catch (Exception e) {
            ourResponse = generateSimpleErrorResponse("Route exception " + ProjectMonitoringMisc.getExceptionTrace(e), bSendLog);
        }
        servletMisc.serializeToJsonAndSend(ourResponse, resp);
    }

    private Project checkMainUserParameters(HttpServletRequest req) {
        //first check user
        JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser applicationUser = jac.getLoggedInUser();
        if (applicationUser == null) {
            StatusText.getInstance().add(true, "ourResponse.statusMessage");
            return null;
        }

        String projectKey = Optional.ofNullable(req.getParameter("projectKey")).orElse("");
        if (projectKey.isEmpty()) {
            StatusText.getInstance().add(true, "Project key is missing");
            return null;
        }

        Project currentProject = projectManager.getProjectByCurrentKey(projectKey);
        if (currentProject == null) {
            StatusText.getInstance().add(true, "Failed to find project by key " + projectKey);
            return null;
        }

        //does user has rights to change project configuration?
        boolean hasPermissions = false;
        hasPermissions = permissionManager.hasPermission(
            ProjectPermissions.ADMINISTER_PROJECTS,
            currentProject,
            applicationUser
        );
        //temporary workaround
        if (!hasPermissions && applicationUser.getEmailAddress().equals("andreyp@playtika.com")) {
            hasPermissions = true;
        }
        if (!hasPermissions) {
            StatusText.getInstance().add(true, "The current user has no rights to change the project " + projectKey);
            return null;
        }
        return currentProject;
    }

    private ManageActiveObjectsResult checkInitialConfiguration(ManageActiveObjectsEntityKey entityKey) {
        ProjectConfigurationModel initConfig;
        ManageActiveObjectsResult configurationResponse = mao.GetProjectConfiguration(entityKey);

        if (configurationResponse.Result == null) {
            initConfig = new ProjectConfigurationModel();
            configurationResponse = mao.SetProjectConfiguration(mao, entityKey, initConfig);
        }
        return configurationResponse;
    }

    private ProjectConfigurationResponse generateSimpleErrorResponse(String errorMessage, boolean bSendLog) {
        ProjectConfigurationResponse ourResponse = new ProjectConfigurationResponse();
        ourResponse.statusMessage = errorMessage;
        if (bSendLog) ourResponse.logInfo = StatusText.getInstance().toString();
        return ourResponse;
    }

    private ProjectConfigurationResponse generateResponse(ManageActiveObjectsResult configurationResponse) {
        ProjectConfigurationResponse ourResponse = new ProjectConfigurationResponse();
        ourResponse.statusMessage = "";
        ourResponse.logInfo = "Current project configuration";
        ourResponse.isAdmin = true;
        if (configurationResponse != null) {
            ourResponse.viewType = ((ProjectConfigurationModel) configurationResponse.Result).getViewType();
            ourResponse.isProjectEpics = ((ProjectConfigurationModel) configurationResponse.Result).isProjectEpics();
            ourResponse.isProjectTickets = ((ProjectConfigurationModel) configurationResponse.Result).isProjectTickets();
            ourResponse.groupBy = ((ProjectConfigurationModel) configurationResponse.Result).getGroupBy();
        }
        return ourResponse;
    }
}


class ProjectConfigurationResponse {
    public String statusMessage = "";
    public String logInfo = "";
    public boolean isAdmin = false;
    public boolean isProjectEpics = false;
    public boolean isProjectTickets = false;
    public String viewType = ROADMAP_FEATURE;
    public String groupBy = "";
    public List<String> groups = GroupByTypes.getViewTypes();
}

