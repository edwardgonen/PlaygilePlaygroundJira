package com.playgileplayground.jira.persistence;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.playgileplayground.jira.projectprogress.DataPair;
import org.apache.commons.lang.time.DateUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    @Transactional
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
            else //no entity at all
            {
                //create
                PrjStatEntity prjCreatedStatEntity = ao.create(PrjStatEntity.class);
                prjCreatedStatEntity.setProjectKey(projectKey);
                prjCreatedStatEntity.save();
                result.Message = "Created";
            }
        }
        catch (Exception ex)
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_EXCEPTION;
            result.Message = ex.toString();
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult DeleteProjectEntity(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
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
    @Transactional
    public ManageActiveObjectsResult GetProjectReleaseVersion(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
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
    @Transactional
    public ManageActiveObjectsResult GetSprintLength(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
            result.Result = prjStatEntity.getSprintLength();
            result.Message = "Sprint length : " + result.Result;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult SetSprintLength(String projectKey, double sprintLength)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
            prjStatEntity.setSprintLength(sprintLength);
            prjStatEntity.save();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetProjectStartedFlag(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
            result.Result = prjStatEntity.getProjectStartedFlag();
            result.Message = "Started flag: " + result.Result;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult SetProjectStartedFlag(String projectKey, boolean startedFlag)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
            prjStatEntity.setProjectStartedFlag(startedFlag);
            prjStatEntity.save();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetProjectStartDate(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
            result.Result = prjStatEntity.getProjectStartDate();
            result.Message = "Started date: " + result.Result;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult SetProjectStartDate(String projectKey, Date startedDate)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
            prjStatEntity.setProjectStartDate(startedDate);
            prjStatEntity.save();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult SetProjectInitialEstimation(String projectKey, Date startDate, double initialEstimation)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
            prjStatEntity.setInitialEstimation(initialEstimation);
            prjStatEntity.save();
            //also add it as the first element in the list
            ArrayList<DataPair> tmpList = new ArrayList<>();
            tmpList.add(new DataPair(startDate, initialEstimation));
            SetDateRemainingEstimationsList(tmpList, prjStatEntity);
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetTeamVelocity(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
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
    @Transactional
    public ManageActiveObjectsResult AddVelocityAndReleaseVersion(String projectKey, String releaseVersion, double teamVelocity)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
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
    @Transactional
    public ManageActiveObjectsResult GetProgressDataList(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
            //read the existing data
            result.Result = GetDataRemainingEstimationsList(prjStatEntity);
            result.Message = "List returned";
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }

        return result;
    }
    @Transactional
    public ManageActiveObjectsResult AddRemainingEstimationsRecord(String projectKey, Date date, double remainingEstimations)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
            //read the existing data
            ArrayList<DataPair> existingData = GetDataRemainingEstimationsList(prjStatEntity);
            //do we have such date?
            for (DataPair dataPair : existingData) {
                if (DateUtils.isSameDay(dataPair.Date, date)) {
                    dataPair.RemainingEstimation = remainingEstimations;
                    SetDateRemainingEstimationsList(existingData, prjStatEntity);
                    prjStatEntity.save();
                    result.Message = "Data updated";
                    return result;
                }
            }
            existingData.add(new DataPair(date, remainingEstimations));
            SetDateRemainingEstimationsList(existingData, prjStatEntity);
            result.Message = "Data added";
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }

        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetRemainingEstimationsForDate(String projectKey, Date date)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
            //read the data
            ArrayList<DataPair> existingData = GetDataRemainingEstimationsList(prjStatEntity);
            for (DataPair dataPair : existingData)
                if (dataPair.Date == date) {
                    result.Result = dataPair;
                    result.Message = "Retrieved data " + result.Result.toString();
                    return result;
                }
            result.Result = null;
            result.Message = "No remaining estimation for date found";
            result.Code = ManageActiveObjectsResult.STATUS_CODE_REMAINING_ESTIMATIONS_FOR_DATE_NOT_FOUND;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + projectKey;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetProjectKey(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(projectKey);
        if(prjStatEntity != null) {
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
    @Transactional
    public ArrayList<DataPair> GetDataRemainingEstimationsList(PrjStatEntity entity)
    {
        ArrayList<DataPair> list = new ArrayList<>();
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
                        list.add(new DataPair(tmpDate, tmpRemainingEstimation));
                    }
                    catch (ParseException e) {
                        //ignore
                    }
                }
            }
        }
        return list;
    }
    @Transactional
    private void SetDateRemainingEstimationsList(ArrayList<DataPair> list, PrjStatEntity entity)
    {
        StringBuilder outputString = new StringBuilder();
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        list.forEach((dataPair) -> outputString.append(formatter.format(dataPair.Date) + PAIR_SEPARATOR + dataPair.RemainingEstimation + LINE_SEPARATOR));
        entity.setRemainingStoriesEstimations(outputString.toString());
        entity.save();
    }
    @Transactional
    private PrjStatEntity GetProjectEntity(String projectKey)
    {
        PrjStatEntity result = null;
        PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
        if (projectStatusEntities.length <= 0) {
            return result;
        }
        return FindEntityByProjectKey(projectKey, projectStatusEntities);
    }
}

