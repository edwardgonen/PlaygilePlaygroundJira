package com.playgileplayground.jira.projectprogress;

import com.playgileplayground.jira.impl.DateTimeUtils;
import com.playgileplayground.jira.impl.StatusText;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Ext_EdG on 7/9/2020.
 */
public class ProjectProgress
{
    public int SprintLength;
    private double _teamVelocity;
    private double _projectVelocity;
    private ProgressData _progressData;
    private ProgressData _idealData;

    public ProjectProgressResult Initiate(double teamVelocity, double projectVelocity, int sprintLength, ArrayList<DataPair> remainingEstimations)
    {
        ProjectProgressResult result = new ProjectProgressResult();

        if (teamVelocity == 0 || projectVelocity == 0 || sprintLength == 0)
        {
            result.Code = ProjectProgressResult.STATUS_CODE_BAD_INPUT;
            return result;
        }

        SprintLength = sprintLength;
        _projectVelocity = projectVelocity;
        _teamVelocity = teamVelocity;
        _idealData = new ProgressData();
        _idealData.SetData(remainingEstimations);

        _progressData = new ProgressData();
        _progressData.SetData(remainingEstimations);



        //adjust data to time line in dates


        Date startProjectDate = _progressData.GetEstimationDatesList().get(0);
        double dailyVelocity = _projectVelocity / (double)SprintLength;
        double dailyIdealVelocity = _teamVelocity / (double)SprintLength;
        Date lastDateInProjectData =
            _progressData.GetEstimationDatesList().get(_progressData.GetEstimationDatesList().size() - 1);
        DataPair tmpPair;
        int daysLeftSinceLastUpdateTillEndOfSprint = SprintLength - 1 - DateTimeUtils.AbsDays(lastDateInProjectData, startProjectDate) % SprintLength;
        Date closestSprintEnd =
            DateTimeUtils.AddDays(lastDateInProjectData, daysLeftSinceLastUpdateTillEndOfSprint);

        double endSprintExpectation = 0;
        double endSprintExpectatonIdeal = 0;

        if (daysLeftSinceLastUpdateTillEndOfSprint > 0)
        {
            //add predicted closest sprint end
            double remainingWorkInRecentSprint = dailyVelocity * ((double)(daysLeftSinceLastUpdateTillEndOfSprint));
            double remainingWorkIRecentSprintIdeal = dailyIdealVelocity * ((double)(daysLeftSinceLastUpdateTillEndOfSprint));
            endSprintExpectation = Math.max(_progressData.GetEstimationValuesList().get(_progressData.GetEstimationValuesList().size() -1 ) - remainingWorkInRecentSprint, 0);
            endSprintExpectatonIdeal = Math.max(_idealData.GetEstimationValuesList().get(_idealData.GetEstimationValuesList().size() -1 ) - remainingWorkIRecentSprintIdeal, 0);
            tmpPair = new DataPair(closestSprintEnd, endSprintExpectation);
            _progressData.AddDataPair(tmpPair);

            tmpPair = new DataPair(closestSprintEnd, endSprintExpectatonIdeal);
            _idealData.AddDataPair(tmpPair);
        }
        else //we are the last day of sprint - so the sprint ends with the same value as we have the last
        {
            endSprintExpectation = Math.max(_progressData.GetEstimationValuesList().get(_progressData.GetEstimationValuesList().size() -1 ), 0);
            endSprintExpectatonIdeal = Math.max(_idealData.GetEstimationValuesList().get(_idealData.GetEstimationValuesList().size() -1 ), 0);
        }


        //calculate prediction
        //first let's set the predicted point to the end of recent sprint
        Date lastSprintEnd = closestSprintEnd;

        double lastFullSprintEndValue =
            _progressData.GetEstimationValuesList().get(_progressData.GetEstimationValuesList().size() - 1);
        Date pointDate = lastSprintEnd;
        boolean continueAddingIdealPoints = true;
        boolean continueAddingProgressPoints = true;
        while (endSprintExpectation > 0 || endSprintExpectatonIdeal > 0)
        {
            pointDate = DateTimeUtils.AddDays(pointDate, SprintLength);
            endSprintExpectation = CalculateIdealEstimationByDate(lastSprintEnd, pointDate, lastFullSprintEndValue, dailyVelocity);
            continueAddingProgressPoints = AddDataPairToList(continueAddingProgressPoints, pointDate, endSprintExpectation, _progressData);
            endSprintExpectatonIdeal = CalculateIdealEstimationByDate(lastSprintEnd, pointDate, lastFullSprintEndValue, dailyIdealVelocity);
            continueAddingIdealPoints = AddDataPairToList(continueAddingIdealPoints, pointDate, endSprintExpectatonIdeal, _idealData);
        }

        Date idealProjectEnd = _idealData.GetElementAtIndex(0).Date;
        //end of the project for each set is the first date where the estimation is 0
        for (int i = _idealData.GetEstimationValuesList().size() - 1; i > 0; i--)
        {
            if (_idealData.GetElementAtIndex(i).RemainingEstimation <= 0 && _idealData.GetElementAtIndex(i - 1).RemainingEstimation > 0)
            {
                idealProjectEnd = _idealData.GetElementAtIndex(i).Date;
                break;
            }
        }

        Date predictedProjectEnd = _progressData.GetElementAtIndex(0).Date;
        //end of the project for each set is the first date where the estimation is 0
        for (int i = _progressData.GetEstimationValuesList().size() - 1; i > 0; i--)
        {
            if (_progressData.GetElementAtIndex(i).RemainingEstimation <= 0 && _progressData.GetElementAtIndex(i - 1).RemainingEstimation > 0)
            {
                predictedProjectEnd = _progressData.GetElementAtIndex(i).Date;
                break;
            }
        }
        result.idealData = _idealData;
        result.progressData = _progressData;
        result.idealProjectEnd = idealProjectEnd;
        result.predictedProjectEnd = predictedProjectEnd;

        //logic of color
        //TODO move color logic to client
        int differenceInDays = DateTimeUtils.Days(predictedProjectEnd, idealProjectEnd);
        if (differenceInDays <= 7) result.progressDataColor = new Color(0,153,0);//dark green
        else
        if (differenceInDays > 7 && differenceInDays < 30) result.progressDataColor = new Color(204,204,0);//dark yellow;
        else result.progressDataColor = Color.RED;

        return result;
    }

    private boolean AddDataPairToList(boolean continueAddingPoints, Date date, double estimation, ProgressData data)
    {
        boolean result = continueAddingPoints;
        if (continueAddingPoints)
        {
            DataPair tmpPair = new DataPair(date, Math.max(estimation, 0));
            if (estimation <= 0) result = false;
            data.AddDataPair(tmpPair);
        }
        return result;
    }
    private Double CalculateIdealEstimationByDate(Date projectStartDate, Date currentDate, double initialProjectEstimation, double dailyVelocity)
    {
        long distanceDays = DateTimeUtils.AbsDays(currentDate, projectStartDate);
        return Math.max(initialProjectEstimation - distanceDays * dailyVelocity, 0);
    }

    public static String convertColorToHexadeimal(Color color)
    {
        String hex = Integer.toHexString(color.getRGB() & 0xffffff);
        if(hex.length() < 6)
        {
            if(hex.length()==5)
                hex = "0" + hex;
            if(hex.length()==4)
                hex = "00" + hex;
            if(hex.length()==3)
                hex = "000" + hex;
        }
        hex = "#" + hex;
        return hex;
    }
}
