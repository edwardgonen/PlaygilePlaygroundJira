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
    public ManageActiveObjectsResult CreateProjectEntity(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        //see if it already there
        try {
            PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
            if (projectStatusEntities.length > 0) {
                result.Code = ManageActiveObjectsResult.STATUS_CODE_ENTRY_ALREADY_EXISTS;
                result.Message = "Entry already exists -- " + projectKey + " " + projectStatusEntities.length;
                return result;
            }
            PrjStatEntity prjStatEntity = ao.create(PrjStatEntity.class);
            prjStatEntity.setProjectKey(projectKey);
            prjStatEntity.save();
            result.Message = "Created";
        }
        catch (Exception ex)
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_EXCEPTION;
            result.Message = ex.toString();
        }
        return result;
    }
    public ManageActiveObjectsResult DeleteProjectEntity(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        //see if it already there
        PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
        PrjStatEntity prjStatEntity = null;
        for (PrjStatEntity entity : projectStatusEntities)
        {
            if (entity.getProjectKey() == projectKey)
            {
                prjStatEntity = entity;
                break;
            }
        }
        if (prjStatEntity != null) {
            ao.delete(prjStatEntity);
            result.Message = "Deleted";
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }

        return result;
    }
    public ManageActiveObjectsResult AddRemainingEstimationsRecord(String projectKey, Date date, double remainingEstimations)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
        if (projectStatusEntities.length <= 0) {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_NO_SUCH_ENTRY;
            result.Message = "Entry not found -- " + projectKey;
            return result;
        }
        PrjStatEntity prjStatEntity = null;
        for (PrjStatEntity entity : projectStatusEntities)
        {
            if (entity.getProjectKey() == projectKey)
            {
                prjStatEntity = entity;
                break;
            }
        }
        if (prjStatEntity != null) {
            prjStatEntity.setRemainingStoriesEstimation(date, remainingEstimations);
            prjStatEntity.save();
            result.Message = "Data added";
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
    public ManageActiveObjectsResult GetRemainingEstimationsForDate(String projectKey, Date date)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
        if (projectStatusEntities.length <= 0) {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_NO_SUCH_ENTRY;
            result.Message = "Entry not found -- " + projectKey;
            return result;
        }
        //find our project key
        PrjStatEntity prjStatEntity = null;
        for (PrjStatEntity entity : projectStatusEntities)
        {
            if (entity.getProjectKey() == projectKey)
            {
                prjStatEntity = entity;
                break;
            }
        }
        if (prjStatEntity != null) {
            result.Result = prjStatEntity.getRemainingStoriesEstimation(date);
            result.Message = "Retrieved data " + result.Result.toString();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
    public ManageActiveObjectsResult GetProjectKey(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
        if (projectStatusEntities.length <= 0) {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_NO_SUCH_ENTRY;
            result.Message = "Table not found";
            return result;
        }
        //find our project key
        PrjStatEntity prjStatEntity = null;
        for (PrjStatEntity entity : projectStatusEntities)
        {
            if (entity.getProjectKey() == projectKey)
            {
                prjStatEntity = entity;
                break;
            }
        }
        if (prjStatEntity != null) {
            result.Result = prjStatEntity.getProjectKey();
            result.Message = "Retrieved data " + result.Result.toString();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
}

