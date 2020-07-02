package com.playgileplayground.jira.persistence;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.Query;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;

/**
 * Created by Ext_EdG on 7/2/2020.
 */
@Scanned
@Named
public final class ManageActiveObjects{
    private ActiveObjects ao;
    @Inject
    public ManageActiveObjects(@ComponentImport ActiveObjects ao)
    {
        this.ao = ao;
    }
    public void CreateProjectEntity(String projectKey)
    {
        ProjectStatusEntity projectStatusEntity = ao.create(ProjectStatusEntity.class);
        projectStatusEntity.setProjectKey(projectKey);
        projectStatusEntity.save();
    }
    public void AddRemainingEstimationsRecord(String projectKey, Date date, double remainingEstimations)
    {
        ProjectStatusEntity[] projectStatusEntities = ao.find(ProjectStatusEntity.class, Query.select().where("projectKey = ?", projectKey));
        ProjectStatusEntity projectStatusEntity = projectStatusEntities[0];
        projectStatusEntity.setRemainingStoriesEstimation(date, remainingEstimations);
        projectStatusEntity.save();
    }
    public double GetRemainingEstimationsForDate(String projectKey, Date date)
    {
        ProjectStatusEntity[] projectStatusEntities = ao.find(ProjectStatusEntity.class, Query.select().where("projectKey = ?", projectKey));
        ProjectStatusEntity projectStatusEntity = projectStatusEntities[0];
        return projectStatusEntity.getRemainingStoriesEstimation(date);
    }
}
