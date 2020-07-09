package com.playgileplayground.jira.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Ext_EdG on 7/9/2020.
 */
public class ProjectProgress
{
    public int SprintLength;
    private double _teamVelocity;
    private ProgressData _progressData;
    private ProgressData _idealData;

    public void Initiate(double teamVelocity, int sprintLength, ArrayList<DataPair> remainingEstimations)
    {
        SprintLength = sprintLength;
        _teamVelocity = teamVelocity;
        _idealData = new ProgressData();

        _progressData = new ProgressData();
        _progressData.SetData(remainingEstimations);

        //adjust data to time line in dates

        boolean projectInitialEstimationSpecified = false;
        double initialEstimationSpecified = 500;
        boolean projectStartSpecified = false;
        Date specifiedProjectDate;

        Date startProjectDate = _progressData.GetEstimationDatesList().First();

        double initialProjectEstimation = _progressData.GetEstimationValuesList().First();
        double dailyVelocity = _teamVelocity / (double)SprintLength;
        Date lastDateInProjectData = _progressData.GetEstimationDatesList().Last();
        DataPair tmpPair;
        int daysLeftSinceLastUpdateTillEndOfSprint = SprintLength - 1 - (lastDateInProjectData - startProjectDate).Days % SprintLength;
        Date closestSprintEnd = lastDateInProjectData.AddDays(daysLeftSinceLastUpdateTillEndOfSprint);
        if (daysLeftSinceLastUpdateTillEndOfSprint > 0)
        {
            //add predicted closest sprint end
            double remainingWorkInRecentSprint = dailyVelocity * ((double)(daysLeftSinceLastUpdateTillEndOfSprint));
            double estimationHowWeFinishCurrentSprint = _progressData.GetEstimationValuesList().Last() - remainingWorkInRecentSprint;
            if (estimationHowWeFinishCurrentSprint > 0)
            {
                tmpPair = new DataPair(closestSprintEnd, estimationHowWeFinishCurrentSprint);
                _progressData.AddDataPair(tmpPair);
            }
        }
        //to this point the predicted array contains real data from the past.
        //The dates of those estimations may not be on the sprint end
        //so we need to add those points to the ideal line to make both lines parallel in sense of dates
        //it is easy since the ideal is linear y = initial estimation -(DailyVelocity) * NumberOfDaysSinceProjectStarts
        tmpPair = new DataPair(startProjectDate, initialProjectEstimation);
        _idealData.AddDataPair(tmpPair);
        for (int i = 1; i < _progressData.Length(); i++)
        {
            //calculate this point distance from the project start
            double tmpEstimation = CalculateIdealEstimationByDate(LocalDateTime.from(startProjectDate.toInstant()).plusDays(-1),
                _progressData.GetElementAtIndex(i).Date, initialProjectEstimation, dailyVelocity);
            tmpPair = new DataPair(_progressData.GetElementAtIndex(i).Date, tmpEstimation);
            _idealData.AddDataPair(tmpPair);
        }


        //calculate prediction
        //first let's set the predicted point to the end of recent sprint
        Date lastSprintEnd = closestSprintEnd;
        double endSprintExpectation;
        double idealEstimation;
        double lastFullSprintEndValue = _progressData.GetEstimationValuesList().Last();
        Date pointDate = lastSprintEnd;
        boolean continueAddingIdealPoints = true;
        boolean continueAddingProgressPoints = true;
        do
        {
            pointDate = pointDate.AddDays(SprintLength);
            endSprintExpectation = CalculateIdealEstimationByDate(lastSprintEnd, pointDate, lastFullSprintEndValue, dailyVelocity);
            if (continueAddingProgressPoints)
            {
                tmpPair = new DataPair(pointDate, Math.Max(endSprintExpectation, 0));
                _progressData.AddDataPair(tmpPair);
                if (endSprintExpectation <= 0) continueAddingProgressPoints = false;
            }
            idealEstimation = CalculateIdealEstimationByDate(startProjectDate.AddDays(-1), pointDate, initialProjectEstimation, dailyVelocity);
            if (continueAddingIdealPoints)
            {
                tmpPair = new DataPair(pointDate, Math.Max(idealEstimation, 0));
                _idealData.AddDataPair(tmpPair);
                if (idealEstimation <= 0) continueAddingIdealPoints = false;
            }
        } while (endSprintExpectation > 0 || idealEstimation > 0);

        Date idealProjectEnd = null;
        //end of the project for each set is the first date where the estimation is 0
        for (int i = 0; i < _idealData.GetEstimationValuesList().Count(); i++)
        {
            if (_idealData.GetElementAtIndex(i).RemainingEstimation <= 0)
            {
                idealProjectEnd = _idealData.GetElementAtIndex(i).Date;
                break;
            }
        }

        Date predictedProjectEnd = default;
        //end of the project for each set is the first date where the estimation is 0
        for (int i = 0; i < _progressData.GetEstimationValuesList().Count(); i++)
        {
            if (_progressData.GetElementAtIndex(i).RemainingEstimation <= 0)
            {
                predictedProjectEnd = _progressData.GetElementAtIndex(i).Date;
                break;
            }
        }
    }

    private Double CalculateIdealEstimationByDate(Date projectStartDate, Date currentDate, double initialProjectEstimation, double dailyVelocity)
    {
        int distanceDays = (currentDate - projectStartDate).Days;
        return (initialProjectEstimation - distanceDays * dailyVelocity);
    }
}
