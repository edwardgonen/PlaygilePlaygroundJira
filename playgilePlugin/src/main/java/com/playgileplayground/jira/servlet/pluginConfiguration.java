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
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.impl.ProjectMonitoringMisc;
import com.playgileplayground.jira.impl.RoadmapFeatureAnalysis;
import com.playgileplayground.jira.impl.StatusText;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Scanned
public class pluginConfiguration extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(activeObjectsAccess.class);
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    ActiveObjects ao;

    private static final String PLUGIN_CONFIGURATION_TEMPLATE = "/templates/plugin-configuration.vm";
    private static final String FEATURES_CONFIGURATIONS_KEY = "featuresconfigurationkeys";
    private static final String ALL_IS_OK_KEY = "allisok";

    public pluginConfiguration(ActiveObjects ao,TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService)
    {
        this.ao = ao;
        this.templateRenderer = templateRenderer;
        this.projectManager = projectManager;
        this.searchService = searchService;
    }
    @Override
    @Transactional
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        ManageActiveObjectsResult maor;
        boolean bAllIsOk = false;
        Map<String, Object> context = new HashMap<>();
        //String propertyName = Optional.ofNullable(req.getParameter("propertyName")).orElse("");
        //String propertyValue = Optional.ofNullable(req.getParameter("propertyValue")).orElse("");
        //String roadmapFeature = Optional.ofNullable(req.getParameter("roadmapFeature")).orElse("");

        String projectKey = Optional.ofNullable(req.getParameter("projectKey")).orElse("");
        if (projectKey == null || projectKey.isEmpty()) //no project key provided
        {
            context.put(FEATURES_CONFIGURATIONS_KEY, "Failed to retrieve project key");
            context.put(ALL_IS_OK_KEY, false);
            servletMisc.renderAndResponseToWeb(templateRenderer, PLUGIN_CONFIGURATION_TEMPLATE, context, resp);
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
            context.put(FEATURES_CONFIGURATIONS_KEY, "Failed to find project " + projectKey);
            context.put(ALL_IS_OK_KEY, false);
            servletMisc.renderAndResponseToWeb(templateRenderer, PLUGIN_CONFIGURATION_TEMPLATE, context, resp);
            return;
        }

        ProjectMonitoringMisc projectMonitoringMisc = new ProjectMonitoringMisc(jiraInterface, applicationUser, currentProject, mao);

        resp.setContentType("text/html;charset=utf-8");


        ao.executeInTransaction((TransactionCallback<Void>) () -> {

            ArrayList<RoadmapFeatureConfigurationParameters> featuresWithParameters = new ArrayList();
            ManageActiveObjectsResult maorLocal;
            String messageToDisplay;
            List<Issue> roadmapFeatures = jiraInterface.getRoadmapFeaturesNotCancelledAndNotGoLiveAndNotOnHold(applicationUser, currentProject, ProjectMonitor.ROADMAPFEATUREKEY);
            if (roadmapFeatures == null)
            {
                context.put(FEATURES_CONFIGURATIONS_KEY, "Failed to retrieve Roadmap features for project " + projectKey);
                context.put(ALL_IS_OK_KEY, false);
                servletMisc.renderAndResponseToWeb(templateRenderer, PLUGIN_CONFIGURATION_TEMPLATE, context, resp);
                return null;
            }


            for (Issue roadmapFeature : roadmapFeatures) {

                RoadmapFeatureAnalysis roadmapFeatureAnalysis = new RoadmapFeatureAnalysis(
                    roadmapFeature,
                    jiraInterface,
                    applicationUser,
                    currentProject,
                    projectMonitoringMisc,
                    mao);
                if (roadmapFeatureAnalysis.analyzeRoadmapFeature()) { //we take all successfully analyzed features - started or non-started
                    StatusText.getInstance().add(true, "Found active Roadmap feature " + roadmapFeatureAnalysis.getRoadmapFeatureKeyAndSummary());

                    RoadmapFeatureConfigurationParameters featureParameters = new RoadmapFeatureConfigurationParameters();
                    featureParameters.featureKey = roadmapFeatureAnalysis.featureKey;
                    featureParameters.featureSummary = roadmapFeatureAnalysis.featureSummary;
                    featureParameters.defaultNotEstimatedIssueValue = roadmapFeatureAnalysis.defaultNotEstimatedIssueValue;
                    featureParameters.plannedRoadmapFeatureVelocity = roadmapFeatureAnalysis.plannedRoadmapFeatureVelocity;
                    featureParameters.sprintLengthRoadmapFeature = roadmapFeatureAnalysis.sprintLengthRoadmapFeature;
                    featureParameters.startDateRoadmapFeature = roadmapFeatureAnalysis.startDateRoadmapFeature;
                    //add to the list
                    featuresWithParameters.add(featureParameters);
                }
                else //failed to analyze feature
                {
                }
            }
            //serialize
            ObjectMapper jsonMapper = new ObjectMapper();
            try {
                String json = jsonMapper.writeValueAsString(featuresWithParameters);
                context.put(FEATURES_CONFIGURATIONS_KEY, json);
            } catch (JsonProcessingException e) {
                context.put(FEATURES_CONFIGURATIONS_KEY, "Failed to serialize list of features " + e.toString());
                context.put(ALL_IS_OK_KEY, false);
                servletMisc.renderAndResponseToWeb(templateRenderer, PLUGIN_CONFIGURATION_TEMPLATE, context, resp);
                return null;
            }

            context.put(ALL_IS_OK_KEY, true);
            servletMisc.renderAndResponseToWeb(templateRenderer, PLUGIN_CONFIGURATION_TEMPLATE, context, resp);

            return null;
        });
    }
}
class RoadmapFeatureConfigurationParameters
{
    public String featureSummary;
    public String featureKey;
    public double plannedRoadmapFeatureVelocity = 0;
    public double defaultNotEstimatedIssueValue = 0;
    public Date startDateRoadmapFeature = null;
    public double sprintLengthRoadmapFeature = 0;
}
