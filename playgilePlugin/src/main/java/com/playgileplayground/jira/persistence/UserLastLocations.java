package com.playgileplayground.jira.persistence;

/**
 * Created by Ext_EdG on 7/16/2020.
 */
public class UserLastLocations {
    public String lastProjectId;
    public String lastRoadmapFeature;
    public UserLastLocations(String lastProjectId, String lastRoadmapFeature)
    {
        this.lastProjectId = lastProjectId;
        this.lastRoadmapFeature = lastRoadmapFeature;
    }
}
