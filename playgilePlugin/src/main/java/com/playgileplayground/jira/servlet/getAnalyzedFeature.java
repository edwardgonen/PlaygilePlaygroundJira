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
public class getAnalyzedFeature extends HttpServlet {
    @ComponentImport
    TemplateRenderer templateRenderer;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    ActiveObjects ao;

    public getAnalyzedFeature(ActiveObjects ao,TemplateRenderer templateRenderer, ProjectManager projectManager, SearchService searchService)
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
            //Issue selectedRoadmapFeatureIssue = projectMonitoringMisc.SearchSelectedIssue(roadmapFeatures, roadmapFeatureName);

            Issue selectedRoadmapFeatureIssue = jiraInterface.getIssueByKey(currentProject.getKey(), roadmapFeatureName);
            if (selectedRoadmapFeatureIssue == null) //not found
            {
                ourResponse.statusMessage = "Failed to find the selected feature in Jira " + roadmapFeatureName;
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
                return;
            }
            resp.setContentType("text/html;charset=utf-8");

            //get the feature, analyze it and convert it to our response
            RoadmapFeatureAnalysis roadmapFeatureAnalysis = new RoadmapFeatureAnalysis(
                selectedRoadmapFeatureIssue,
                jiraInterface,
                applicationUser,
                currentProject,
                projectMonitoringMisc,
                mao);
            if (roadmapFeatureAnalysis.analyzeRoadmapFeature()) { //we take all successfully analyzed features - started or non-started
                ourResponse.fillTheFields(roadmapFeatureAnalysis);
                if (bSendLog)
                {
                    ourResponse.logInfo = StatusText.getInstance().toString();
                }
                servletMisc.serializeToJsonAndSend(ourResponse, resp);
            } else //failed to analyze feature
            {
                ourResponse.statusMessage = "Failed to analyze feature " + roadmapFeatureName;
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
class ProgressDataSet
{
    public Date date;
    public double predictedEstimations;
    public double idealEstimations;
}
class IssueCountsDataSet
{
    public Date date;
    public int openIssues;
    public int totalIssues;
    public int readyForDevelopmentIssues;
    public int readyForEstimationIssues;
}
class VelocitiesDataSet
{
    public Date date;
    public double realVelocity;
    public double interpolatedVelocity;
}
class GetAnalyzedFeatureResponse
{

    public String statusMessage = "";
    public String logInfo = "";
    public String summary;
    public String key;
    public String teamName = "";
    public Date startDateRoadmapFeature;
    public Date targetDate;
    public double plannedRoadmapFeatureVelocity;
    public double defaultNotEstimatedIssueValue;
    public double sprintLengthRoadmapFeature;
    public double predictedVelocity;
    public double[] overallIssuesDistributionInSprint;
    public Date idealProjectEnd;
    public Date predictedProjectEnd;
    public double notEstimatedStoriesNumber;
    public double largeStoriesNumber;
    public double veryLargeStoriesNumber;
    public double estimatedStoriesNumber;

    public FeatureScore qualityScore;
    public ArrayList<ProgressDataSet> progressDataSets;
    public ArrayList<VelocitiesDataSet> velocityDataSets;
    public ArrayList<IssueCountsDataSet> issueCountsDataSets;

    public void fillTheFields(RoadmapFeatureAnalysis roadmapFeatureAnalysis)
    {
        summary = roadmapFeatureAnalysis.featureSummary;
        key = roadmapFeatureAnalysis.featureKey;
        teamName = roadmapFeatureAnalysis.teamName;
        startDateRoadmapFeature = roadmapFeatureAnalysis.startDateRoadmapFeature;
        plannedRoadmapFeatureVelocity = roadmapFeatureAnalysis.plannedRoadmapFeatureVelocity;
        predictedVelocity = roadmapFeatureAnalysis.predictedVelocity;
        defaultNotEstimatedIssueValue = roadmapFeatureAnalysis.defaultNotEstimatedIssueValue;
        overallIssuesDistributionInSprint = roadmapFeatureAnalysis.overallIssuesDistributionInSprint;
        idealProjectEnd = roadmapFeatureAnalysis.projectProgressResult.idealProjectEnd;
        predictedProjectEnd = roadmapFeatureAnalysis.projectProgressResult.predictedProjectEnd;

        notEstimatedStoriesNumber = roadmapFeatureAnalysis.analyzedStories.NotEstimatedStoriesNumber;
        largeStoriesNumber = roadmapFeatureAnalysis.analyzedStories.LargeStoriesNumber;
        veryLargeStoriesNumber = roadmapFeatureAnalysis.analyzedStories.VeryLargeStoriesNumber;
        estimatedStoriesNumber = roadmapFeatureAnalysis.analyzedStories.EstimatedStoriesNumber;
        sprintLengthRoadmapFeature = roadmapFeatureAnalysis.sprintLengthRoadmapFeature;

        targetDate = roadmapFeatureAnalysis.targetDate;

        qualityScore = roadmapFeatureAnalysis.qualityScore;

        progressDataSets = getEstimationsSet(roadmapFeatureAnalysis.projectProgressResult);

        velocityDataSets = getRealInterpolatedVelocities(roadmapFeatureAnalysis.artificialTimeWindowsForVelocityCalculation,
            roadmapFeatureAnalysis.interpolatedVelocityPoints);

        issueCountsDataSets = getHistoricalIssuesCounts(roadmapFeatureAnalysis.historicalDateAndValues);
    }
    ArrayList<ProgressDataSet> getEstimationsSet(ProjectProgressResult projectProgressResult)
    {
        ArrayList<ProgressDataSet> result = new ArrayList<>();
        for (int i = 0; i < projectProgressResult.idealData.Length(); i++)
        {
            ProgressDataSet pds = new ProgressDataSet();
            DateAndValues tmpPredictedDataPair = projectProgressResult.progressData.GetElementAtIndex(i);
            DateAndValues tmpIdealDataPair = projectProgressResult.idealData.GetElementAtIndex(i);
            pds.date = tmpIdealDataPair.Date;
            pds.idealEstimations = tmpIdealDataPair.Estimation;
            pds.predictedEstimations = tmpPredictedDataPair.Estimation;
            result.add(pds);
        }
        return result;
    }
    ArrayList<VelocitiesDataSet> getRealInterpolatedVelocities(Collection<PlaygileSprint> artificialTimeWindowsForVelocityCalculation, ArrayList<Double> interpolatedVelocityPoints)
    {
        ArrayList<VelocitiesDataSet> result = new ArrayList<>();
        int index = 0;
        for (PlaygileSprint sprintToConvert : artificialTimeWindowsForVelocityCalculation)
        {
            VelocitiesDataSet vds = new VelocitiesDataSet();
            vds.date = sprintToConvert.getEndDate();
            vds.realVelocity = sprintToConvert.sprintVelocity;
            vds.interpolatedVelocity = interpolatedVelocityPoints.get(index++);
            result.add(vds);
        }
        return result;
    }
    ArrayList<IssueCountsDataSet> getHistoricalIssuesCounts(ArrayList<DateAndValues> historicalDateAndValues)
    {
        ArrayList<IssueCountsDataSet> result = new ArrayList<>();
        for (DateAndValues dateAndValues : historicalDateAndValues) {
            IssueCountsDataSet icds = new IssueCountsDataSet();
            icds.date = dateAndValues.Date;
            icds.openIssues = dateAndValues.OpenIssues;
            icds.readyForDevelopmentIssues = dateAndValues.ReadyForDevelopmentIssues;
            icds.readyForEstimationIssues = dateAndValues.ReadyForEstimationIssues;
            icds.totalIssues = dateAndValues.TotalIssues;
            result.add(icds);
        }
        return result;
    }
}
