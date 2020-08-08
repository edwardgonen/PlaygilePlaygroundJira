package com.playgileplayground.jira.api;

import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserProjectHistoryManager;

import java.util.Map;

public interface TotalView {
    String PROJECT = "project";
    String STATUSTEXT = "statustext";
    String MAINJAVACLASS = "mainjavaclass";
    String ALLISOK = "allisok";
    String MESSAGETODISPLAY = "messagetodisplay";
    String BASEURL = "baseurl";
    String CURRENTUSER = "currentuser";
    String FEATURESROWS = "featuresrows";

    double DEFAULT_TEAM_VELOCITY = 50.0;



    Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper);
    UserProjectHistoryManager getUserProjectHistoryManager();
}
