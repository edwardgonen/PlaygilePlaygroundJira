package com.playgileplayground.jira.projectprogress;

import com.playgileplayground.jira.impl.DateTimeUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Ext_EdG on 7/9/2020.
 */
public class ProjectProgress
{
    public int _sprintLength;
    private double _teamVelocity;
    private double _projectVelocity;
    private ProgressData _progressData;
    private ProgressData _idealData;

    public ProjectProgressResult Initiate(double teamVelocity, double projectVelocity, int sprintLength, ArrayList<DateAndValues> remainingEstimations)
    {
        ProjectProgressResult result = new ProjectProgressResult();

        if (teamVelocity == 0 || projectVelocity == 0 || sprintLength == 0)
        {
            result.Code = ProjectProgressResult.STATUS_CODE_BAD_INPUT;
            return result;
        }


        _sprintLength = sprintLength;
        _projectVelocity = projectVelocity;
        _teamVelocity = teamVelocity;
        _idealData = new ProgressData();
        _idealData.SetData(remainingEstimations);

        _progressData = new ProgressData();
        _progressData.SetData(remainingEstimations);



        //adjust data to time line in dates

        double dailyVelocity = _projectVelocity / (double)_sprintLength;
        double dailyIdealVelocity = _teamVelocity / (double)_sprintLength;
        Date lastDateInProjectData =
            _progressData.GetEstimationDatesList().get(_progressData.GetEstimationDatesList().size() - 1);

        //let's do the prediction very simply
        //just add the last points
        //1. Calculate how many ideal days till the end
        double lastEstimation = _progressData.GetEstimationValuesList().get(_progressData.GetEstimationValuesList().size() -1 );
        int idealDaysToFinishProject = (int)(lastEstimation / dailyIdealVelocity);
        int predictedDaysToFinishProject = (int)(lastEstimation / dailyVelocity);
        Date idealProjectEnd = DateTimeUtils.AddDays(lastDateInProjectData, idealDaysToFinishProject);
        _idealData.AddDataPair(new DateAndValues(idealProjectEnd, 0));
        Date predictedProjectEnd = DateTimeUtils.AddDays(lastDateInProjectData, predictedDaysToFinishProject);
        _progressData.AddDataPair(new DateAndValues(predictedProjectEnd, 0));

        //we need to align both ideal and progressdata so they will contain the same points
        int differenceBetweenPredictedAndRealDates = DateTimeUtils.Days(predictedProjectEnd, idealProjectEnd);

        //1. are the both dates the same? if yes - nothing to do
        if (differenceBetweenPredictedAndRealDates != 0)
        {
            if (differenceBetweenPredictedAndRealDates > 0) //predicted is later than ideal
            {
                //calculate estimation at the ideal date using predicted velocity
                double predictedValueAtIdealDate = differenceBetweenPredictedAndRealDates * dailyVelocity;
                //add from ideal to predicted at index before the last
                _progressData.AddDataPair(_progressData.Length() - 1, new DateAndValues(idealProjectEnd, predictedValueAtIdealDate));
                //add predicted to ideal as the last element
                _idealData.AddDataPair(new DateAndValues(predictedProjectEnd, 0));
            }
            else //ideal is later than predicted
            {
                //calculate estimation at the predicted date using ideal velocity
                double predictedValueAtPredictedDate = (-differenceBetweenPredictedAndRealDates) * dailyIdealVelocity;
                //add from predicted to ideal at index before the last
                _idealData.AddDataPair(_idealData.Length() - 1, new DateAndValues(predictedProjectEnd, predictedValueAtPredictedDate));
                //add ideal to predicted as the last element
                _progressData.AddDataPair(new DateAndValues(idealProjectEnd, 0));
            }
        }

        result.idealData = _idealData;
        result.progressData = _progressData;
        result.idealProjectEnd = idealProjectEnd;
        result.predictedProjectEnd = predictedProjectEnd;

        return result;
    }

    private boolean AddDataPairToList(boolean continueAddingPoints, Date date, double estimation, ProgressData data)
    {
        boolean result = continueAddingPoints;
        if (continueAddingPoints)
        {
            DateAndValues tmpPair = new DateAndValues(date, Math.max(estimation, 0));
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
    public ProjectProgressResult OldInitiate(double teamVelocity, double projectVelocity, int sprintLength, ArrayList<DateAndValues> remainingEstimations)
    {
        ProjectProgressResult result = new ProjectProgressResult();

        if (teamVelocity == 0 || projectVelocity == 0 || sprintLength == 0)
        {
            result.Code = ProjectProgressResult.STATUS_CODE_BAD_INPUT;
            return result;
        }

        _sprintLength = sprintLength;
        _projectVelocity = projectVelocity;
        _teamVelocity = teamVelocity;
        _idealData = new ProgressData();
        _idealData.SetData(remainingEstimations);

        _progressData = new ProgressData();
        _progressData.SetData(remainingEstimations);



        //adjust data to time line in dates


        Date startProjectDate = _progressData.GetEstimationDatesList().get(0);
        double dailyVelocity = _projectVelocity / (double) _sprintLength;
        double dailyIdealVelocity = _teamVelocity / (double) _sprintLength;
        Date lastDateInProjectData =
            _progressData.GetEstimationDatesList().get(_progressData.GetEstimationDatesList().size() - 1);
        DateAndValues tmpPair;
        int daysLeftSinceLastUpdateTillEndOfSprint = _sprintLength - 1 - DateTimeUtils.AbsDays(lastDateInProjectData, startProjectDate) % _sprintLength;
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
            tmpPair = new DateAndValues(closestSprintEnd, endSprintExpectation);
            _progressData.AddDataPair(tmpPair);

            tmpPair = new DateAndValues(closestSprintEnd, endSprintExpectatonIdeal);
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
            pointDate = DateTimeUtils.AddDays(pointDate, _sprintLength);
            endSprintExpectation = CalculateIdealEstimationByDate(lastSprintEnd, pointDate, lastFullSprintEndValue, dailyVelocity);
            continueAddingProgressPoints = AddDataPairToList(continueAddingProgressPoints, pointDate, endSprintExpectation, _progressData);
            endSprintExpectatonIdeal = CalculateIdealEstimationByDate(lastSprintEnd, pointDate, lastFullSprintEndValue, dailyIdealVelocity);
            continueAddingIdealPoints = AddDataPairToList(continueAddingIdealPoints, pointDate, endSprintExpectatonIdeal, _idealData);
        }

        Date idealProjectEnd = _idealData.GetElementAtIndex(0).Date;
        //end of the project for each set is the first date where the estimation is 0
        for (int i = _idealData.GetEstimationValuesList().size() - 1; i > 0; i--)
        {
            if (_idealData.GetElementAtIndex(i).Estimation <= 0 && _idealData.GetElementAtIndex(i - 1).Estimation > 0)
            {
                idealProjectEnd = _idealData.GetElementAtIndex(i).Date;
                break;
            }
        }

        Date predictedProjectEnd = _progressData.GetElementAtIndex(0).Date;
        //end of the project for each set is the first date where the estimation is 0
        for (int i = _progressData.GetEstimationValuesList().size() - 1; i > 0; i--)
        {
            if (_progressData.GetElementAtIndex(i).Estimation <= 0 && _progressData.GetElementAtIndex(i - 1).Estimation > 0)
            {
                predictedProjectEnd = _progressData.GetElementAtIndex(i).Date;
                break;
            }
        }
        result.idealData = _idealData;
        result.progressData = _progressData;
        result.idealProjectEnd = idealProjectEnd;
        result.predictedProjectEnd = predictedProjectEnd;

        return result;
    }
}
