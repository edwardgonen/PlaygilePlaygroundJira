package com.playgileplayground.jira.persistence;

/**
 * Created by Ext_EdG on 7/16/2020.
 */
public class ManageActiveObjectsEntityKey {
    public String projectKey;
    public String roadmapFeature;
    public ManageActiveObjectsEntityKey()
    {
        projectKey = "";
        roadmapFeature = "";
    }
    public ManageActiveObjectsEntityKey(String projectKey, String roadmapFeature)
    {
        this.projectKey = projectKey;
        this.roadmapFeature = roadmapFeature;
    }
}
