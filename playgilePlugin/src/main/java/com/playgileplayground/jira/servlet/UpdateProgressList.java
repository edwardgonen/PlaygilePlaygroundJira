package com.playgileplayground.jira.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.base.Strings;
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;
import com.playgileplayground.jira.impl.StatusText;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;
import com.playgileplayground.jira.projectprogress.DateAndValues;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

@Scanned
public class UpdateProgressList extends HttpServlet {
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    @ComponentImport
    ActiveObjects ao;

    public UpdateProgressList(ActiveObjects ao, TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService) {
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        processRequest(req, resp);
    }

    private void processRequest(HttpServletRequest req, HttpServletResponse resp) {
        StatusText.getInstance().reset();
        GetUpdateProgressListResponse ourResponse = new GetUpdateProgressListResponse();
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
            String featureName = Optional.ofNullable(req.getParameter("feature")).orElse("");
            String updatedEstimationEntryNumberString = Optional.ofNullable(req.getParameter("number")).orElse("-1");
            String updateValueString = Optional.ofNullable(req.getParameter("value")).orElse("0");
            if (projectKey.isEmpty() || featureName.isEmpty()) {
                ourResponse.statusMessage = "Project key and/or feature is missing " + projectKey + " " + featureName;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            double updateValue;
            try {
                updateValue = Double.parseDouble(updateValueString);
            } catch (Exception e) {
                ourResponse.statusMessage = "Wrong value " + updateValueString;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }
            String[] updatedEstimationEntryNumber;
            try {
                updatedEstimationEntryNumber = updatedEstimationEntryNumberString.split(",");
            } catch (Exception e) {
                ourResponse.statusMessage = "Wrong entry number " + updatedEstimationEntryNumberString;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }
            boolean bSendLog = req.getParameter("sendLog") != null;

            ManageActiveObjects mao = new ManageActiveObjects(this.ao);
            ManageActiveObjectsResult maor = new ManageActiveObjectsResult();
            for (String updateNumber : updatedEstimationEntryNumber) {
                String currentStatusMessage = ourResponse.statusMessage;
                try {
                    int updateNumberInt = Integer.parseInt(updateNumber);
                    maor = mao.UpdateHistoricalRecord(new ManageActiveObjectsEntityKey(projectKey, featureName),
                        updateNumberInt, updateValue);
                    String message = "Number " + updateNumber + " successfully updated; ";
                    currentStatusMessage = Strings.isNullOrEmpty(currentStatusMessage) ? message : currentStatusMessage + message;
                } catch (Exception e) {
                    String message = "Provided Number is not Integer! Number value is : " + updateNumber + "; ";
                    currentStatusMessage = Strings.isNullOrEmpty(currentStatusMessage) ? message : currentStatusMessage + message;
                }
                if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                    ourResponse.statusMessage = currentStatusMessage;
                    ourResponse.progressList = (ArrayList<DateAndValues>) maor.Result;
                } else {
                    ourResponse.statusMessage = maor.Message + "; " + currentStatusMessage;
                }
            }

            if (bSendLog) {
                ourResponse.logInfo = StatusText.getInstance().toString();
            }
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
        } catch (Exception e) {
            ourResponse.statusMessage = "Route exception " + ProjectMonitoringMisc.getExceptionTrace(e);
            servletMisc.serializeToJsonAndSend(ourResponse, resp);
        }
    }
}

class GetUpdateProgressListResponse {
    public String statusMessage = "";
    public String logInfo = "";
    public ArrayList<DateAndValues> progressList;
}

