package com.playgileplayground.jira.api;

import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserProjectHistoryManager;

import java.util.Map;

public interface ProjectPreparation {
    String PROJECT = "project";
    String STATUSTEXT = "statustext";
    String ALLISOK = "allisok";
    String MESSAGETODISPLAY = "messagetodisplay";
    String BASEURL = "baseurl";
    String CURRENTUSER = "currentuser";


    Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper);
    UserProjectHistoryManager getUserProjectHistoryManager();
}
