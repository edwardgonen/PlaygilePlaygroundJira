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
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;
import com.playgileplayground.jira.impl.StatusText;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

@Scanned
public class pluginConfiguration extends HttpServlet {
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    ActiveObjects ao;

    private static final ArrayList<String> parametersNames = new ArrayList<>();


    public pluginConfiguration(ActiveObjects ao,TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService)
    {
        this.ao = ao;
        this.templateRenderer = templateRenderer;
        this.projectManager = projectManager;
        this.searchService = searchService;

        parametersNames.add("plannedRoadmapFeatureVelocity");
        parametersNames.add("defaultNotEstimatedIssueValue");
        parametersNames.add("startDateRoadmapFeature");
        parametersNames.add("sprintLengthRoadmapFeature");
        parametersNames.add("targetDate");
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
            String roadmapFeatureKey = Optional.ofNullable(req.getParameter("roadmapFeature")).orElse("");

            if (projectKey.isEmpty() || roadmapFeatureKey.isEmpty()) {
                ourResponse.statusMessage = "Failed to retrieve project and/or roadmap feature key";
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            //prepare to walk through the features
            ManageActiveObjects mao = new ManageActiveObjects(this.ao);
            JiraInterface jiraInterface = new JiraInterface(applicationUser, searchService);

            boolean bSendLog = req.getParameter("sendLog") != null;

            Project currentProject = projectManager.getProjectByCurrentKey(projectKey);
            if (currentProject == null) {
                ourResponse.statusMessage = "Failed to find project " + projectKey;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }
            Issue selectedRoadmapFeatureIssue = jiraInterface.getIssueByKey(currentProject.getKey(), roadmapFeatureKey);
            if (selectedRoadmapFeatureIssue == null) //not found
            {
                ourResponse.statusMessage = "Failed to find the selected feature in Jira " + roadmapFeatureKey;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            resp.setContentType("text/html;charset=utf-8");

            ao.executeInTransaction((TransactionCallback<Void>) () -> {

                //if at least one our parameter is specified - save it
                StatusText.getInstance().add(true, "Setting configuration");
                ManageActiveObjectsEntityKey key = new ManageActiveObjectsEntityKey(projectKey, selectedRoadmapFeatureIssue.getKey());
                ManageActiveObjectsResult maorLocal = mao.CreateProjectEntity(key); //will not create if exists
                if (maorLocal.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS || maorLocal.Code == ManageActiveObjectsResult.STATUS_CODE_ENTRY_ALREADY_EXISTS) {
                    //set team velocity if available
                    String parameterValue = "";
                    double value;
                    long timeStamp;
                    StringBuilder collectedStatus = new StringBuilder();
                    for (String parameterName : parametersNames) {
                        parameterValue = Optional.ofNullable(req.getParameter(parameterName)).orElse("");
                        if (!parameterValue.isEmpty()) {
                            try {
                                collectedStatus.append(" Setting ").append(parameterName).append(" to ").append(parameterValue);
                                switch (parameterName) {
                                    case "plannedRoadmapFeatureVelocity":
                                        value = Double.parseDouble(parameterValue);
                                        maorLocal = mao.SetTeamVelocity(key, value);
                                        break;
                                    case "defaultNotEstimatedIssueValue":
                                        value = Double.parseDouble(parameterValue);
                                        maorLocal = mao.SetDefaultNotEstimatedIssueValue(key, value);
                                        break;
                                    case "startDateRoadmapFeature":
                                        timeStamp = Long.parseLong(parameterValue);
                                        maorLocal = mao.SetProjectStartDate(key, new java.util.Date(timeStamp));
                                        break;
                                    case "sprintLengthRoadmapFeature":
                                        value = Double.parseDouble(parameterValue);
                                        maorLocal = mao.SetSprintLength(key, value);
                                        break;
                                    case "targetDate":
                                        timeStamp = Long.parseLong(parameterValue);
                                        maorLocal = mao.SetTargetDate(key, new java.util.Date(timeStamp));
                                        break;
                                    default:
                                        //unknown parameter
                                        maorLocal = new ManageActiveObjectsResult();
                                        maorLocal.Code = ManageActiveObjectsResult.STATUS_CODE_NO_SUCH_ENTRY;
                                        maorLocal.Message = "Invalid parameter " + parameterName;
                                        break;
                                }
                                //check save return code
                                if (maorLocal.Code != ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
                                {
                                    //exit with error
                                    ourResponse.statusMessage = collectedStatus.toString() + " " + "Failed to set parameter " + parameterName + " " + maorLocal.Message;
                                    if (bSendLog)
                                    {
                                        ourResponse.logInfo = StatusText.getInstance().toString();
                                    }
                                    servletMisc.serializeToJsonAndSend(ourResponse, resp);
                                    return null;
                                }
                                else
                                {
                                    if (bSendLog)
                                    {
                                        ourResponse.logInfo = StatusText.getInstance().toString();
                                    }
                                    collectedStatus.append(" Set");
                                }
                            } catch (Exception e) {
                                //exit with error
                                ourResponse.statusMessage = "Parsing exception " + ProjectMonitoringMisc.getExceptionTrace(e);
                                if (bSendLog)
                                {
                                    ourResponse.logInfo = StatusText.getInstance().toString();
                                }
                                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                                return null;
                            }
                        }
                    }
                } else {
                    ourResponse.statusMessage = "Failed to find project and feature " + projectKey + " " + roadmapFeatureKey;
                    if (bSendLog)
                    {
                        ourResponse.logInfo = StatusText.getInstance().toString();
                    }
                    servletMisc.serializeToJsonAndSend(ourResponse, resp);
                    return null;
                }

                //return answer to web
                ourResponse.statusMessage = "";
                if (bSendLog)
                {
                    ourResponse.logInfo = StatusText.getInstance().toString();
                }
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return null;
            });
        }
        catch (Exception e)
        {
            ourResponse.statusMessage = "Route exception " + ProjectMonitoringMisc.getExceptionTrace(e);
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
        }
    }

}
