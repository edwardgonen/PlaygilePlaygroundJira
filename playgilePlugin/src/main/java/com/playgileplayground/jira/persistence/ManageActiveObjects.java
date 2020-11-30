package com.playgileplayground.jira.persistence;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.playgileplayground.jira.impl.DateTimeUtils;
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
    public ManageActiveObjectsResult ListAllEntities()
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        ArrayList<String> allEntities = new ArrayList<>();
        try {
            PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
            if (projectStatusEntities.length > 0) {
                for (PrjStatEntity entity : projectStatusEntities)
                {
                    allEntities.add(entity.getProjectKey() + " " + entity.getRoadmapFeature() + " Estimations " + entity.getRemainingStoriesEstimations());
                }
                result.Result = allEntities;
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
    public ManageActiveObjectsResult CreateProjectEntity(ManageActiveObjectsEntityKey key)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        //see if it already there
        try {
            PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);
            if (projectStatusEntities.length > 0) {

                PrjStatEntity prjStatEntity = FindEntityByKey(key, projectStatusEntities);
                if(prjStatEntity != null) {//Check whether optional has element you are looking for
                    prjStatEntity.setProjectKey(key.projectKey);
                    prjStatEntity.setRoadmapFeature(key.roadmapFeature);
                    prjStatEntity.save();
                    result.Code = ManageActiveObjectsResult.STATUS_CODE_ENTRY_ALREADY_EXISTS;
                    result.Message = "Entry already exists -- " + key.projectKey + " " + key.roadmapFeature + " " + projectStatusEntities.length;
                    return result;
                }
                else //not found - create it
                {
                    prjStatEntity = ao.create(PrjStatEntity.class);
                    prjStatEntity.setProjectKey(key.projectKey);
                    prjStatEntity.setRoadmapFeature(key.roadmapFeature);
                    prjStatEntity.save();
                    result.Message = "Added";
                }
            }
            else //no entity at all
            {
                //create
                PrjStatEntity prjCreatedStatEntity = ao.create(PrjStatEntity.class);
                prjCreatedStatEntity.setProjectKey(key.projectKey);
                prjCreatedStatEntity.setRoadmapFeature(key.roadmapFeature);
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
    public ManageActiveObjectsResult DeleteProjectEntity(ManageActiveObjectsEntityKey key)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {//Check whether optional has element you are looking for
            ao.delete(prjStatEntity);
            result.Message = "Deleted";
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }

        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetSprintLength(ManageActiveObjectsEntityKey key)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            result.Result = prjStatEntity.getSprintLength();
            result.Message = "Sprint length : " + result.Result;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetDefaultNotEstimatedIssueValue(ManageActiveObjectsEntityKey key)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            result.Result = prjStatEntity.getDefNotEstimdIssueValue();
            result.Message = "Default not estimated value : " + result.Result;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }

    @Transactional
    public ManageActiveObjectsResult SetDefaultNotEstimatedIssueValue(ManageActiveObjectsEntityKey key, double defaultNotEstimatedIssueValue)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            prjStatEntity.setDefNotEstimdIssueValue(defaultNotEstimatedIssueValue);
            prjStatEntity.save();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }

    @Transactional
    public ManageActiveObjectsResult SetTargetDate(ManageActiveObjectsEntityKey key, Date startedDate)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            prjStatEntity.setTargetDate(startedDate);
            prjStatEntity.save();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetTargetDate(ManageActiveObjectsEntityKey key)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            result.Result = prjStatEntity.getTargetDate();
            result.Message = "Target date: " + result.Result;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }

    @Transactional
    public ManageActiveObjectsResult SetSprintLength(ManageActiveObjectsEntityKey key, double sprintLength)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            prjStatEntity.setSprintLength(sprintLength);
            prjStatEntity.save();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetProjectStartedFlag(ManageActiveObjectsEntityKey key)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            result.Result = prjStatEntity.getProjectStartedFlag();
            result.Message = "Started flag: " + result.Result;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult SetProjectStartedFlag(ManageActiveObjectsEntityKey key, boolean startedFlag)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            prjStatEntity.setProjectStartedFlag(startedFlag);
            prjStatEntity.save();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetProjectStartDate(ManageActiveObjectsEntityKey key)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            result.Result = prjStatEntity.getProjectStartDate();
            result.Message = "Started date: " + result.Result;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult SetProjectStartDate(ManageActiveObjectsEntityKey key, Date startedDate)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            prjStatEntity.setProjectStartDate(startedDate);
            prjStatEntity.save();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult SetProjectInitialEstimation(ManageActiveObjectsEntityKey key, Date startDate, double initialEstimation)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            prjStatEntity.setProjectStartDate(startDate);
            prjStatEntity.setInitialEstimation(initialEstimation);
            prjStatEntity.save();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetProjectInitialEstimation(ManageActiveObjectsEntityKey key)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            double initialEstimation = prjStatEntity.getInitialEstimation();
            result.Result = initialEstimation;
            result.Message = "Initial estimation " + initialEstimation;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult SetTeamVelocity(ManageActiveObjectsEntityKey key, double teamVelocity)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if (prjStatEntity != null) {//Check whether optional has element you are looking for
            prjStatEntity.setProjectTeamVelocity(teamVelocity);
            prjStatEntity.save();
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetPlannedRoadmapVelocity(ManageActiveObjectsEntityKey key)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {//Check whether optional has element you are looking for
            double teamVelocity = prjStatEntity.getProjectTeamVelocity();
            if (teamVelocity > 0) {
                result.Result = teamVelocity;
                result.Message = "Velocity " + teamVelocity;
            }
            else
            {
                result.Code = ManageActiveObjectsResult.STATUS_CODE_NO_SUCH_ENTRY;
                result.Message = "Velocity is not set " + key.projectKey + " " + key.roadmapFeature;
            }
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult AddRoadmapFeature(ManageActiveObjectsEntityKey key, double teamVelocity)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            prjStatEntity.setProjectTeamVelocity(teamVelocity);
            prjStatEntity.save();
            result.Message = "Data added";
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetProgressDataList(ManageActiveObjectsEntityKey key)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            //read the existing data
            ArrayList<DataPair> remainingEstimations = GetDataRemainingEstimationsList(prjStatEntity);
            result.Result = remainingEstimations;
            result.Message = "List returned";
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }

        return result;
    }
    @Transactional
    public ManageActiveObjectsResult AddRemainingEstimationsRecord(ManageActiveObjectsEntityKey key, Date date, double remainingEstimations)
    {
        //this method does not compress data. If we want to compress data please replace this by the method right below this method (..compressed)
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            //read the existing data
            ArrayList<DataPair> existingData = GetDataRemainingEstimationsList(prjStatEntity);
            //sort - just in case
            Collections.sort(existingData);
            //now compress - i.e. leave last 2 weeks on a daily basis, rest use only 2 weeks basis
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
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }

        return result;
    }
    @Transactional
    public ManageActiveObjectsResult AddRemainingEstimationsRecordCompressed(ManageActiveObjectsEntityKey key, Date date, double remainingEstimations)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        PrjStatEntity prjStatEntity = GetProjectEntity(key);
        if(prjStatEntity != null) {
            //read the existing data
            ArrayList<DataPair> existingData = GetDataRemainingEstimationsList(prjStatEntity);
            if (existingData.size() == 0)
            {
                result.Code = ManageActiveObjectsResult.STATUS_CODE_NO_ESTIMATIONS_YET;
                result.Message = "No estimation stored yet";
                return result;
            }
            //sort - just in case
            Collections.sort(existingData);
            //now compress - i.e. leave last 2 weeks on a daily basis, rest use only 2 weeks basis

            //let's get the first date and the last date
            ArrayList<DataPair> compressedList = new ArrayList<>();
            //how many full sprints we have?
            //we compress every 14 days
            int segmentSize = 14;

            DataPair firstRecord = existingData.get(0);
            DataPair lastRecord = existingData.get(existingData.size() - 1);
            int fullSprints = DateTimeUtils.Days(lastRecord.Date, firstRecord.Date) / segmentSize;
            int partialSprintDays = DateTimeUtils.Days(lastRecord.Date, firstRecord.Date) % segmentSize;
            //find the start of last segment as current date minus 2 weeks


            Date startOfLastSegment = DateTimeUtils.AddDays(Calendar.getInstance().getTime(), -segmentSize);
            if (startOfLastSegment.compareTo(firstRecord.Date) < 0)
            {
                startOfLastSegment = firstRecord.Date;
            }
            //how many compress segments we have?
            int totalCompressedSegments;
            if (partialSprintDays > 0) totalCompressedSegments = fullSprints + 1;
            else totalCompressedSegments = fullSprints;


            if (totalCompressedSegments > 0) {
                DataPair[] segmentsData = new DataPair[totalCompressedSegments];
                for (DataPair dataPair : existingData) {
                    if (dataPair.Date.compareTo(startOfLastSegment) < 0) //only compress if we are not yet beyond start of last segment
                    {
                        int segmentNumber = DateTimeUtils.Days(dataPair.Date, firstRecord.Date) / segmentSize;
                        segmentsData[segmentNumber] = dataPair;
                    } else //last segment
                    {
                        //first - copy all segments if not copied yet
                        if (compressedList.size() == 0) {
                            for (int i = 0; i < totalCompressedSegments; i++) {
                                if (segmentsData[i] != null) compressedList.add(segmentsData[i]);
                            }
                        }
                        compressedList.add(dataPair);//add all data for the last sprint
                    }
                }
            }

            //do we have such date?
            for (DataPair dataPair : compressedList) {
                if (DateUtils.isSameDay(dataPair.Date, date)) {
                    dataPair.RemainingEstimation = remainingEstimations;
                    SetDateRemainingEstimationsList(compressedList, prjStatEntity);
                    result.Message = "Data updated";
                    return result;
                }
            }
            compressedList.add(new DataPair(date, remainingEstimations));
            SetDateRemainingEstimationsList(compressedList, prjStatEntity);
            result.Message = "Data added";
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey + " " + key.roadmapFeature;
        }

        return result;
    }
    @Transactional
    public ManageActiveObjectsResult GetFirstProjectEntity(String projectKey)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        ManageActiveObjectsEntityKey key = new ManageActiveObjectsEntityKey();
        key.projectKey = projectKey;
        PrjStatEntity entity =  GetProjectEntity(key);
        if (entity != null)
        {
            result.Result = entity;
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "Project not found " + key.projectKey;
        }
        return result;
    }

    private PrjStatEntity FindEntityByKey(ManageActiveObjectsEntityKey key, PrjStatEntity[] projectStatusEntities)
    {
        //TODO make it strict search once old records are removed from all our Jira servers
        PrjStatEntity result = null;
        for (PrjStatEntity entity : projectStatusEntities)
        {
            if (!key.roadmapFeature.isEmpty()) {
                if (key.projectKey.equals(entity.getProjectKey()) && key.roadmapFeature.equals(entity.getRoadmapFeature())) {
                    result = entity;
                    break;
                }
            }
            else
            {
                if (key.projectKey.equals(entity.getProjectKey())) {
                    result = entity;
                    break;
                }
            }
        }
        return result;
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
    private PrjStatEntity GetProjectEntity(ManageActiveObjectsEntityKey key)
    {
        PrjStatEntity result = null;
        PrjStatEntity[] projectStatusEntities = ao.find(PrjStatEntity.class);

        if (projectStatusEntities.length <= 0) {
            return result;
        }
        return FindEntityByKey(key, projectStatusEntities);
    }


    ///////////////////////////////////////// Users //////////////////////////////////////
    @Transactional
    public ManageActiveObjectsResult CreateUserEntity(String userId)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        UserEntity userEntity;
        //see if it already there
        try {
            UserEntity[] userEntities = ao.find(UserEntity.class);
            if (userEntities.length > 0) {

                userEntity = FindUserEntityById(userId, userEntities);
                if(userEntity != null) {
                    result.Code = ManageActiveObjectsResult.STATUS_CODE_ENTRY_ALREADY_EXISTS;
                    result.Message = "Entry already exists -- " + userId + " " + userEntities.length;
                    return result;
                }
                else //not found - create it
                {
                    userEntity = ao.create(UserEntity.class);
                    userEntity.setUserId(userId);
                    userEntity.save();
                    result.Message = "Added";
                }
            }
            else //no entity at all
            {
                //create
                userEntity = ao.create(UserEntity.class);
                userEntity.setUserId(userId);
                userEntity.save();
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
    public ManageActiveObjectsResult ListAllUsers()
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        ArrayList<String> allEntities = new ArrayList<>();
        try {
            UserEntity[] userEntities = ao.find(UserEntity.class);
            if (userEntities.length > 0) {
                for (UserEntity entity : userEntities)
                {
                    allEntities.add(entity.getUserId() + " " + entity.getLastRoadmapFeature());
                }
                result.Result = allEntities;
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
    public ManageActiveObjectsResult GetUserLastLocations(String userId)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        UserEntity userEntity = GetUserEntity(userId);
        if(userEntity != null) {
            UserLastLocations usl = new UserLastLocations(userEntity.getLastProjectId(),userEntity.getLastRoadmapFeature());
            result.Result = usl;
            result.Message = "Found";
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "User not found " + userId;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult SetUserLastLocations(String userId, UserLastLocations usl)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        UserEntity userEntity = GetUserEntity(userId);
        if(userEntity != null) {
            userEntity.setLastProjectID(usl.lastProjectId);
            userEntity.setLastRoadmapFeature(usl.lastRoadmapFeature);
            userEntity.save();
            result.Message = "Found";
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "User not found " + userId;
        }
        return result;
    }
    @Transactional
    public ManageActiveObjectsResult DeleteUserEntity(String userId)
    {
        ManageActiveObjectsResult result = new ManageActiveObjectsResult();
        UserEntity userEntity = GetUserEntity(userId);
        if(userEntity != null) {//Check whether optional has element you are looking for
            ao.delete(userEntity);
            result.Message = "Deleted";
        }
        else
        {
            result.Code = ManageActiveObjectsResult.STATUS_CODE_PROJECT_NOT_FOUND;
            result.Message = "User not found " + userId;
        }

        return result;
    }

    private UserEntity FindUserEntityById(String userId, UserEntity[] userEntities)
    {
        UserEntity result = null;
        for (UserEntity entity : userEntities)
        {
            if (!userId.isEmpty()) {
                if (userId.equals(entity.getUserId())) {
                    result = entity;
                    break;
                }
            }
        }
        return result;
    }
    @Transactional
    private UserEntity GetUserEntity(String userId)
    {
        UserEntity result = null;
        UserEntity[] userEntities = ao.find(UserEntity.class);
        if (userEntities.length <= 0) {
            return result;
        }
        return FindUserEntityById(userId, userEntities);
    }
}

