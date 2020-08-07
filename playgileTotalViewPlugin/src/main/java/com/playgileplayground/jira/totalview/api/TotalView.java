package com.playgileplayground.jira.totalview.api;

import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserProjectHistoryManager;

import java.util.Map;

public interface TotalView {
    String PROJECT = "project";
    String ISSUE = "issue";
    String AORESULT = "aoresult";
    String CHARTROWS = "chartrows";
    String STATUSTEXT = "statustext";
    String STORYPOINTS = "storypoints";
    String SPRINTINFO = "sprintinfo";
    String MAINJAVACLASS = "mainjavaclass";
    String ALLISOK = "allisok";
    String MESSAGETODISPLAY = "messagetodisplay";
    String PROJECTVERSIONS = "projectversions";
    String SELECTEDPROJECTVERSION = "selectedprojectversion";
    String TEAMVELOCITY = "teamvelocity";
    String BASEURL = "baseurl";
    String IDEALENDOFPROJECT = "idealendofproject";
    String PREDICTEDENDOFPROJECT = "predictedendofproject";
    String PREDICTIONCOLOR = "predictioncolor";
    String SELECTEDROADMAPFEATURE = "selectedroadmapfeature";
    String ROADMAPFEATURESLIST = "roadmapfeatureslist";
    String CURRENTUSER = "currentuser";

    //Stories related
    String NOTESTIMATEDSTORIES = "notestimatedstories";
    String LARGESTORIES  = "largestories";
    String VERYLARGESTORIES  = "verylargestories";
    String ESTIMATEDSTORIES  = "estimatedstories";

    //real velocity
    String REALVELOCITIES = "realvelocities";
    String AVERAGEREALVELOCITY = "averagerealvelocity";


    ///////////////////// our pre-defined keys /////////////////
    String ROADMAPFEATUREKEY = "Roadmap Feature";

    String ISSUESDISTRIBUTION = "issuedistribution";

    Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper);
    UserProjectHistoryManager getUserProjectHistoryManager();
}
