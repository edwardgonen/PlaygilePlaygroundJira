package com.playgileplayground.jira.projectprogress;

import com.playgileplayground.jira.impl.DateTimeUtils;
import java.util.ArrayList;
import java.util.Date;

public class ProjectProgress
{

    public ProjectProgressResult Initiate(double teamVelocity, double projectVelocity, int sprintLength, ArrayList<DateAndValues> remainingEstimations)
    {
        ProjectProgressResult result = new ProjectProgressResult();

        if (teamVelocity == 0 || projectVelocity == 0 || sprintLength == 0)
        {
            result.Code = ProjectProgressResult.STATUS_CODE_BAD_INPUT;
            return result;
        }


        ProgressData _idealData = new ProgressData();
        _idealData.SetData(remainingEstimations);

        ProgressData _progressData = new ProgressData();
        _progressData.SetData(remainingEstimations);



        //adjust data to time line in dates

        double dailyVelocity = projectVelocity / (double) sprintLength;
        double dailyIdealVelocity = teamVelocity / (double) sprintLength;
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
}
