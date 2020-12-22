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
import com.playgileplayground.jira.impl.*;
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
        PreparationFeaturesResponse ourResponse = new PreparationFeaturesResponse();
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

            List<Issue> roadmapFeatures = jiraInterface.getRoadmapFeaturesInPreparationPhase(currentProject, ProjectMonitor.ROADMAPFEATUREKEY);
            if (roadmapFeatures == null) {
                ourResponse.statusMessage = "Failed to find any feature for " + projectKey;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }

            //prepare list of short feature descriptors
            if (roadmapFeatures.size() > 0)
            {
                //prepare the response with features sorted by month/year
                //first - create our list of features
                ProjectPreparationMisc projectPreparationMisc = new ProjectPreparationMisc(jiraInterface);
                ArrayList<PreparationFeatureShortDescriptor> preparationFeatureShortDescriptors = new ArrayList<>();
                for (Issue feature : roadmapFeatures) {
                    PreparationFeatureShortDescriptor preparationFeatureShortDescriptor = new PreparationFeatureShortDescriptor();
                    preparationFeatureShortDescriptor.businessApprovalDate = projectPreparationMisc.getBusinessApprovalDate(feature);
                    preparationFeatureShortDescriptor.featureKey = feature.getKey();
                    preparationFeatureShortDescriptors.add(preparationFeatureShortDescriptor);
                }
                ourResponse.featuresListByMonthYear = createListOfFeaturesByMonthYear(preparationFeatureShortDescriptors);
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
    private ArrayList<PreparationFeaturesListByMonth> createListOfFeaturesByMonthYear(ArrayList<PreparationFeatureShortDescriptor> preparationFeatureShortDescriptors)
    {
        ArrayList<PreparationFeaturesListByMonth> result = new ArrayList<>();
        //1. Sort the input list by business approval date
        Collections.sort(preparationFeatureShortDescriptors);
        Date firstDate = preparationFeatureShortDescriptors.get(0).businessApprovalDate;
        Date lastDate = preparationFeatureShortDescriptors.get(preparationFeatureShortDescriptors.size() - 1).businessApprovalDate;

        //go through all months from first record to the last
        for (Date date = firstDate; date.before(lastDate) || date.equals(lastDate); date = DateTimeUtils.AddMonths(date, 1)) {
            PreparationFeaturesListByMonth preparationFeaturesListByMonth = new PreparationFeaturesListByMonth();
            //go through the list and build our array and find year/month
            for (PreparationFeatureShortDescriptor feature : preparationFeatureShortDescriptors)
            {
                if (DateTimeUtils.CompareDatesByMonthYear(feature.businessApprovalDate, date))
                {
                    preparationFeaturesListByMonth.monthYear = date;
                    preparationFeaturesListByMonth.featuresList.add(feature);
                }
            }
            if (preparationFeaturesListByMonth.featuresList.size() > 0) result.add(preparationFeaturesListByMonth);
        }

        return result;
    }

}

class PreparationFeaturesResponse
{
    public String statusMessage = "";
    public ArrayList<PreparationFeaturesListByMonth> featuresListByMonthYear = new ArrayList<>();
}
class PreparationFeaturesListByMonth
{
    public Date monthYear;
    public ArrayList<PreparationFeatureShortDescriptor> featuresList = new ArrayList<>();
}
class PreparationFeatureShortDescriptor implements Comparator<PreparationFeatureShortDescriptor>, Comparable<PreparationFeatureShortDescriptor>
{
    public String featureKey;
    public Date businessApprovalDate;

    @Override
    public int compareTo(PreparationFeatureShortDescriptor o) {
        return DateTimeUtils.Days(businessApprovalDate, o.businessApprovalDate);
    }

    @Override
    public int compare(PreparationFeatureShortDescriptor o1, PreparationFeatureShortDescriptor o2) {
        return DateTimeUtils.Days(o1.businessApprovalDate, o2.businessApprovalDate);
    }
}
