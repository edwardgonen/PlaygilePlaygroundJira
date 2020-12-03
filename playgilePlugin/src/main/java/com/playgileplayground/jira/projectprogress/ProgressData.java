package com.playgileplayground.jira.projectprogress;

import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Created by Ext_EdG on 7/9/2020.
 */
public class ProgressData {

    private ArrayList<DateAndValues> _progressData = new ArrayList<>();

    public int Length() {
        return _progressData.size();
    }

    public ArrayList<Double> GetEstimationValuesList() {
        ArrayList<Double> result = _progressData.stream().map(dataPair -> dataPair.Estimation).collect(Collectors.toCollection(ArrayList::new));
        return result;
    }

    public ArrayList<Date> GetEstimationDatesList() {
        ArrayList<Date> result = _progressData.stream().map(dataPair -> dataPair.Date).collect(Collectors.toCollection(ArrayList::new));
        return result;
    }

    public DateAndValues GetElementAtIndex(int index) {
        if (_progressData.size() > 0 && index < _progressData.size()) {
            return _progressData.get(index);
        }
        return null;
    }

    public void AddDataPair(DateAndValues dataPair) {
        _progressData.add(dataPair);
    }

    public void SetData(ArrayList<DateAndValues> progressData) {
        for (DateAndValues dataPair : progressData) {
            this._progressData.add(dataPair);
        }
    }
}
