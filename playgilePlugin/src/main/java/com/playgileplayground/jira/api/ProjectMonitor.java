
package com.playgileplayground.jira.api;

import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserProjectHistoryManager;

import java.util.Map;

public interface ProjectMonitor {
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

    Map getContextMap(ApplicationUser applicationUser, JiraHelper jiraHelper);
    UserProjectHistoryManager getUserProjectHistoryManager();
}
