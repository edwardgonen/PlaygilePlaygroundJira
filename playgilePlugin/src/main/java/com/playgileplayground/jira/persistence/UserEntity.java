package com.playgileplayground.jira.persistence;

import net.java.ao.Entity;

import java.util.Date;

/**
 * Created by Ext_EdG on 7/16/2020.
 */
public interface UserEntity extends Entity{
    String getUserId();
    void setUserId(String userId);
    String getLastProjectId();
    void setLastProjectID(String projectId);
    String getLastRoadmapFeature();
    void setLastRoadmapFeature(String roadmapFeature);
}
