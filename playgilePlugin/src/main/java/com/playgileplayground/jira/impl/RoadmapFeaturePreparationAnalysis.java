/**
 * Created by on 12/15/2020.
 */
package com.playgileplayground.jira.impl;

    import com.atlassian.jira.issue.Issue;
    import com.atlassian.jira.project.Project;
    import com.atlassian.jira.user.ApplicationUser;
    import com.playgileplayground.jira.jiraissues.*;
    import org.codehaus.jackson.map.ser.StdSerializers;

    import java.util.*;

public class RoadmapFeaturePreparationAnalysis implements Comparator<RoadmapFeaturePreparationAnalysis>, Comparable<RoadmapFeaturePreparationAnalysis> {
    Issue roadmapFeature;
    JiraInterface jiraInterface;
    ProjectPreparationMisc projectPreparationMisc;


    boolean bRoadmapFeatureAnalyzed = false;



    /////////// publics
    public String issueKey;
    public String issueSummary;
    public Date businessApprovalDate;
    public ArrayList<ProjectPreparationTask> tasksList;


    public RoadmapFeaturePreparationAnalysis(
        Issue roadmapFeature,
        JiraInterface jiraInterface,
        ProjectPreparationMisc projectPreparationMisc
    )
    {
        bRoadmapFeatureAnalyzed = false;
        this.roadmapFeature = roadmapFeature;
        this.jiraInterface = jiraInterface;
        this.projectPreparationMisc = projectPreparationMisc;
    }

    @Override
    public int compareTo(RoadmapFeaturePreparationAnalysis o) {
        return issueSummary.compareTo(o.issueSummary);
    }

    @Override
    public int compare(RoadmapFeaturePreparationAnalysis o1, RoadmapFeaturePreparationAnalysis o2) {
        return o1.issueSummary.compareTo(o2.issueSummary);
    }


    public boolean analyzePreparationFeature()
    {
        boolean result = false;
        StatusText.getInstance().add(true, "Start preparation analysis for " + roadmapFeature.getKey() + " " + roadmapFeature.getSummary());

        ProjectPreparationIssue projectPreparationIssue = new ProjectPreparationIssue(roadmapFeature, projectPreparationMisc, jiraInterface);
        if (projectPreparationIssue.instantiateProjectPreparationIssue())
        {
            this.issueKey = projectPreparationIssue.issueKey;
            this.issueSummary = projectPreparationIssue.issueName;
            this.businessApprovalDate = projectPreparationIssue.businessApprovalDate;
            this.tasksList = projectPreparationIssue.preparationTasks;
            result = true;
        }
        else
        {
            //error
        }

        bRoadmapFeatureAnalyzed = result; //set to true if analyzed ok
        return result;
    }
}
