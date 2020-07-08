package com.playgileplayground.jira.persistence;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Console;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Optional;

/**
 * Created by Ext_EdG on 7/2/2020.
 */
@Scanned
@Named
public final class ManageActiveObjects{
    public static final String LINE_SEPARATOR = ",";
    public static final String PAIR_SEPARATOR = "\\|";
    public static final String DATE_FORMAT = "MM/dd/yyyy";
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

                PrjStatEntity prjStatEntity = FindEntityByProjectKey(projectKey, projectStatusEntities);
                if(prjStatEntity != null) {//Check whether optional has element you are looking for
                    result.Code = ManageActiveObjectsResult.STATUS_CODE_ENTRY_ALREADY_EXISTS;
                    result.Message = "Entry already exists -- " + projectKey + " " + projectStatusEntities.length;
                    return result;
                }
                else //not found - create it
                {
                    PrjStatEntity prjCreatedStatEntity = ao.create(PrjStatEntity.class);
                    prjCreatedStatEntity.setProjectKey(projectKey);
                    prjCreatedStatEntity.save();
                    result.Message = "Created";
                }
            }
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
        PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
        if (projectStatusEntities.length <= 0) {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_NO_SUCH_ENTRY;
            result.Message = "Entry not found -- " + projectKey;
            return result;
        }
        //see if it already there
        PrjStatEntity prjStatEntity = FindEntityByProjectKey(projectKey, projectStatusEntities);
        if(prjStatEntity != null) {//Check whether optional has element you are looking for
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
    public ManageActiveObjectsResult GetProjectReleaseVersion(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();

        PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
        if (projectStatusEntities.length <= 0) {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_NO_SUCH_ENTRY;
            result.Message = "Entry not found -- " + projectKey;
            return result;
        }

        PrjStatEntity prjStatEntity = FindEntityByProjectKey(projectKey, projectStatusEntities);
        if(prjStatEntity != null) {//Check whether optional has element you are looking for
            String releaseVersion = prjStatEntity.getProjectVersionLabel();
            result.Result = releaseVersion;
            result.Message = "Version: " + releaseVersion;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
    public ManageActiveObjectsResult GetTeamVelocity(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();

        PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
        if (projectStatusEntities.length <= 0) {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_NO_SUCH_ENTRY;
            result.Message = "Entry not found -- " + projectKey;
            return result;
        }

        PrjStatEntity prjStatEntity = FindEntityByProjectKey(projectKey, projectStatusEntities);
        if(prjStatEntity != null) {//Check whether optional has element you are looking for
            double teamVelocity = prjStatEntity.getProjectTeamVelocity();
            result.Result = teamVelocity;
            result.Message = "Velocity " + teamVelocity;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }

    public ManageActiveObjectsResult AddVelocityAndReleaseVersion(String projectKey, String releaseVersion, double teamVelocity)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();

        PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
        if (projectStatusEntities.length <= 0) {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_NO_SUCH_ENTRY;
            result.Message = "Entry not found -- " + projectKey;
            return result;
        }

        PrjStatEntity prjStatEntity = FindEntityByProjectKey(projectKey, projectStatusEntities);
        if(prjStatEntity != null) {//Check whether optional has element you are looking for
            prjStatEntity.setProjectTeamVelocity(teamVelocity);
            prjStatEntity.setProjectVersionLabel(releaseVersion);
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
    public ManageActiveObjectsResult AddRemainingEstimationsRecord(String projectKey, Date date, double remainingEstimations)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();

        PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
        if (projectStatusEntities.length <= 0) {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_NO_SUCH_ENTRY;
            result.Message = "Entry not found -- " + projectKey;
            return result;
        }

        PrjStatEntity prjStatEntity = FindEntityByProjectKey(projectKey, projectStatusEntities);
        if(prjStatEntity != null) {//Check whether optional has element you are looking for
            //read the existing data
            Hashtable<Date, Double> existingDate = GetDateRemainingEstimationsList(prjStatEntity);
            existingDate.put(date, remainingEstimations);
            SetDateRemainingEstimationsList(existingDate, prjStatEntity);
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
        PrjStatEntity prjStatEntity = FindEntityByProjectKey(projectKey, projectStatusEntities);
        if (prjStatEntity != null) {//Check whether optional has element you are looking for
            //read the data
            Hashtable<Date, Double> existingData = GetDateRemainingEstimationsList(prjStatEntity);
            if (existingData.containsKey(date))
            {
                result.Result = existingData.get(date);
                result.Message = "Retrieved data " + result.Result.toString();

            }
            else {
                result.Result = null;
                result.Message = "No remaining estimation for date found";
                result.Code = ManageActiveObjectsResult.STATUS_CODE_REMAINING_ESTIMATIONS_FOR_DATE_NOT_FOUND;
            }
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
        PrjStatEntity prjStatEntity = FindEntityByProjectKey(projectKey, projectStatusEntities);
        if(prjStatEntity != null) {//Check whether optional has element you are looking for
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

    private PrjStatEntity FindEntityByProjectKey(String projectKey, PrjStatEntity[] projectStatusEntities)
    {
        Optional<PrjStatEntity> optional = Arrays.stream(projectStatusEntities).filter(x -> projectKey.equals(x.getProjectKey())).findFirst();
        if(optional.isPresent()) {//Check whether optional has element you are looking for
            return optional.get();
        }
        else return null;
    }
    private Hashtable<Date, Double> GetDateRemainingEstimationsList(PrjStatEntity entity)
    {
        Hashtable<Date, Double> list = new Hashtable<>();
        String allEstimationsListAsString = entity.getRemainingStoriesEstimations();

        if (allEstimationsListAsString == null) return list; //first time, so the list does not exists
        //parse it
        String[] entries = allEstimationsListAsString.split(LINE_SEPARATOR);
        if (entries.length > 0)
        {
            for (String entry : entries)
            {
                String[] pairParts = entry.split(PAIR_SEPARATOR);
                if (pairParts.length > 0)
                {
                    try {
                        Date tmpDate = new SimpleDateFormat(DATE_FORMAT).parse(pairParts[0]);
                        Double tmpRemainingEstimation = Double.parseDouble(pairParts[1]);
                        list.put(tmpDate, tmpRemainingEstimation);
                    }
                    catch (ParseException e) {
                        //ignore
                    }
                }
            }
        }
        return list;
    }

    private void SetDateRemainingEstimationsList(Hashtable<Date, Double> list, PrjStatEntity entity)
    {
        StringBuilder outputString = new StringBuilder();
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        list.forEach((key, value) -> outputString.append(formatter.format(key) + PAIR_SEPARATOR + value + LINE_SEPARATOR));
        entity.setRemainingStoriesEstimations(outputString.toString());
    }
}

