package com.playgileplayground.jira.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserProjectHistoryManager;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.web.ContextProvider;

import com.playgileplayground.jira.api.ProjectMonitor;
import com.playgileplayground.jira.jiraissues.JiraQueryResult;
import com.playgileplayground.jira.jiraissues.ProjectPreparationIssue;
import com.playgileplayground.jira.persistence.*;
import com.playgileplayground.jira.jiraissues.JiraInterface;

import java.util.*;


@Scanned
public class ProjectPreparationImpl implements com.playgileplayground.jira.api.ProjectPreparation, ContextProvider {
    @ComponentImport
    private final UserProjectHistoryManager userProjectHistoryManager;
    @ComponentImport
    ProjectManager projectManager;
    @ComponentImport
    SearchService searchService;
    @ComponentImport
    DateTimeFormatterFactory dateTimeFormatter;

    

    public ProjectPreparationImpl(UserProjectHistoryManager userProjectHistoryManager,
                              ProjectManager projectManager,
                              DateTimeFormatterFactory dateTimeFormatter,
                              SearchService searchService){
        this.userProjectHistoryManager = userProjectHistoryManager;
        this.projectManager = projectManager;
        this.searchService = searchService;
        this.dateTimeFormatter = dateTimeFormatter;
    }
    @Override
    public Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper) {
        return null;
    }

    @Override
    public void init(Map<String, String> map) throws PluginParseException {
    }

    @Override
    public Map getContextMap(Map<String, Object> map) {
        StatusText.getInstance().reset();
        Map<String, Object> contextMap = new HashMap<>();
        String messageToDisplay = "";
        boolean bAllisOk;
        ArrayList<RoadmapFeatureDescriptor> roadmapFeatureDescriptors = new ArrayList<>();
        JiraQueryResult jqr;

        JiraAuthenticationContext jac = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser applicationUser = jac.getLoggedInUser();
        if (applicationUser == null)
        {
            bAllisOk = false;
            messageToDisplay = "User Authentication Failure";
            return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
        }

        String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        contextMap.put(BASEURL, baseUrl);
        contextMap.put(CURRENTUSER, applicationUser.getKey());

        JiraInterface jiraInterface = new JiraInterface(applicationUser,  searchService);

        //get the current project
        Project currentProject = projectManager.getProjectByCurrentKey((String) map.get("projectKey"));
        //start the real work
        if(null != currentProject) {
            contextMap.put(PROJECT, currentProject);
            ProjectPreparationMisc projectPreparationMisc = new ProjectPreparationMisc(jiraInterface);
            //get list of roadmap features
            List<Issue> roadmapFeatures = jiraInterface.getRoadmapFeaturesInPreparationPhase(currentProject, ProjectMonitor.ROADMAPFEATUREKEY);

            if (roadmapFeatures != null && roadmapFeatures.size() > 0)
            {
                for (Issue roadmapFeature : roadmapFeatures)
                {
                    RoadmapFeatureDescriptor roadmapFeatureDescriptor = new RoadmapFeatureDescriptor();
                    //set name
                    roadmapFeatureDescriptor.Name = roadmapFeature.getSummary();
                    roadmapFeatureDescriptor.Key = roadmapFeature.getKey();

                    jqr = jiraInterface.getBusinessApprovalDateForIssue(roadmapFeature);
                    if (jqr.Code == JiraQueryResult.STATUS_CODE_SUCCESS)
                    {
                        roadmapFeatureDescriptor.BusinessApprovalDate = (Date)jqr.Result;
                        StatusText.getInstance().add (true, "Business approval date for " +
                            roadmapFeature.getKey() + " is " + roadmapFeatureDescriptor.BusinessApprovalDate.toString());
                    }
                    else //error
                    {
                        if (jqr.Code == JiraQueryResult.STATUS_CODE_DATE_PARSE_ERROR)
                        {
                            String specifiedDate = (String)jqr.Result;
                            StatusText.getInstance().add ( true, "Business approval date for " + roadmapFeature.getKey() + " is invalid " + specifiedDate);
                        }
                        else //not found
                        {
                            StatusText.getInstance().add ( true, "Business approval date for " + roadmapFeature.getKey() + " not defined");
                        }
                        //continue - no need to analyze the feature as the business approval date is not available
                        continue;
                    }

                    //let's fill all the fields
                    //get the list of our product related issues
                    List<Issue> productRelatedIssues = jiraInterface.getAllProductRelatedIssues(roadmapFeatureDescriptor);
                    //let's add them to the roadmap feature descriptor
                    if (productRelatedIssues != null && productRelatedIssues.size() > 0) {
                        StatusText.getInstance().add ( true, "Product issues number " + productRelatedIssues.size() + " for feature " + roadmapFeatureDescriptor.Key);
                        for (Issue productIssue : productRelatedIssues) {
                            ProjectPreparationIssue ourIssue = projectPreparationMisc.identifyProductPreparationIssue(productIssue, roadmapFeatureDescriptor);
                            if (ourIssue != null) {
                                ourIssue.businessApprovalDate = roadmapFeatureDescriptor.BusinessApprovalDate;
                                //add to RF
                                roadmapFeatureDescriptor.PreparationIssues.add(ourIssue);
                                StatusText.getInstance().add ( true, "Issue " + ourIssue.issueSummary + " " + ourIssue.issueKey + " added with type " + ourIssue.issueTypeName);
                            }
                            else //not our issue or something
                            {
                                StatusText.getInstance().add ( true, "Not our or invalid issue " + productIssue.getKey());
                            }
                        }
                        //add to the list of our features only if our issues are detected
                        roadmapFeatureDescriptors.add(roadmapFeatureDescriptor);
                    }
                    else
                    {
                        StatusText.getInstance().add ( true, "No product issues found for " + roadmapFeature.getKey());
                    }
                }

                if (roadmapFeatureDescriptors.size() > 0) {
                    //create presentation data
                    ProjectPreparationPresentation presentation = new ProjectPreparationPresentation();
                    presentation.createPresentationData(roadmapFeatureDescriptors);
                    bAllisOk = true;
                    String presentationString = presentation.dataForBrowser().toString();
                    contextMap.put(MONTHSROWS, presentationString);
                }
                else {
                    messageToDisplay = "No roadmap feature with a correct product preparation structure was detected";
                    bAllisOk = false;
                }
            }
            else //no suitable roadmap features found
            {
                bAllisOk = false;
                messageToDisplay = "No suitable Roadmap Features detected";
            }
        }
        else
        {
            messageToDisplay = "Failed to retrieve the current project";
            bAllisOk = false;
        }

        return ReturnContextMapToVelocityTemplate(contextMap, bAllisOk, messageToDisplay);
    }
    @Override
    public UserProjectHistoryManager getUserProjectHistoryManager() {
        return this.userProjectHistoryManager;
    }
    private Map<String, Object> ReturnContextMapToVelocityTemplate(Map<String, Object> contextMap, boolean bAllisOk, String messageToDisplay)
    {
        contextMap.put(ALLISOK, bAllisOk);
        contextMap.put(MESSAGETODISPLAY, messageToDisplay);
        contextMap.put(STATUSTEXT, StatusText.getInstance());
        return contextMap;
    }
}
