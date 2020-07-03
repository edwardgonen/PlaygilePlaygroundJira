package com.playgileplayground.jira.persistence;

import net.java.ao.Entity;

import java.util.Date;

/**
 * Created by Ext_EdG on 7/2/2020.
 */
public interface PrjStatEntity extends Entity {
    public String getProjectKey();
    public void setProjectKey(String projectKey);
    public double getRemainingStoriesEstimation(Date date);
    public void setRemainingStoriesEstimation(Date date, double remainingEstimation);
}
