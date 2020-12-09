package com.playgileplayground.jira.projectprogress;

import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;

public class ProgressData {

    private ArrayList<DateAndValues> _progressData = new ArrayList<>();

    public int Length() {
        return _progressData.size();
    }

    public ArrayList<Double> GetEstimationValuesList() {
        return _progressData.stream().map(dataPair -> dataPair.Estimation).collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Date> GetEstimationDatesList() {
        return _progressData.stream().map(dataPair -> dataPair.Date).collect(Collectors.toCollection(ArrayList::new));
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
    public void AddDataPair(int index, DateAndValues dataPair) {
        _progressData.add(index, dataPair);
    }

    public void SetData(ArrayList<DateAndValues> progressData) {
        this._progressData.addAll(progressData.stream().collect(Collectors.toList()));
    }
}
