/**
 * Created by on 12/15/2020.
 */
package com.playgileplayground.jira.impl;

import com.atlassian.jira.issue.Issue;
import com.playgileplayground.jira.jiraissues.JiraInterface;
import com.playgileplayground.jira.jiraissues.ProjectPreparationIssue;

import java.util.Comparator;

public class RoadmapFeaturePreparationAnalysis implements Comparator<RoadmapFeaturePreparationAnalysis>, Comparable<RoadmapFeaturePreparationAnalysis> {
    Issue roadmapFeature;
    JiraInterface jiraInterface;
    ProjectPreparationMisc projectPreparationMisc;


    boolean bRoadmapFeatureAnalyzed = false;



    /////////// publics
    public ProjectPreparationIssue projectPreparationIssue;

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
        return DateTimeUtils.Days(projectPreparationIssue.businessApprovalDate, o.projectPreparationIssue.businessApprovalDate);
    }

    @Override
    public int compare(RoadmapFeaturePreparationAnalysis o1, RoadmapFeaturePreparationAnalysis o2) {
        return DateTimeUtils.Days(o1.projectPreparationIssue.businessApprovalDate, o2.projectPreparationIssue.businessApprovalDate);
    }


    public boolean analyzePreparationFeature()
    {
        boolean result = false;
        StatusText.getInstance().add(true, "Start preparation analysis for " + roadmapFeature.getKey() + " " + roadmapFeature.getSummary());

        projectPreparationIssue = new ProjectPreparationIssue(roadmapFeature, projectPreparationMisc, jiraInterface);
        if (projectPreparationIssue.instantiateProjectPreparationIssue())
        {
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
