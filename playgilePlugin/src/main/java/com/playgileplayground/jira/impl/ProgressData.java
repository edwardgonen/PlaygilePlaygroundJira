package com.playgileplayground.jira.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Created by Ext_EdG on 7/9/2020.
 */
public class ProgressData {

    private ArrayList<DataPair> _progressData = new ArrayList<>();

    public int Length() {
        return _progressData.size();
    }

    public Collection<Double> GetEstimationValuesList() {
        ArrayList<Double> result = _progressData.stream().map(dataPair -> dataPair.RemainingEstimation).collect(Collectors.toCollection(ArrayList::new));
        return result;
    }

    public Collection<Date> GetEstimationDatesList() {
        ArrayList<Date> result = _progressData.stream().map(dataPair -> dataPair.Date).collect(Collectors.toCollection(ArrayList::new));
        return result;
    }

    public DataPair GetElementAtIndex(int index) {
        if (_progressData.size() > 0 && index < _progressData.size()) {
            return _progressData.get(index);
        }
        return null;
    }

    public void AddDataPair(DataPair dataPair) {
        _progressData.add(dataPair);
    }

    public void SetData(ArrayList<DataPair> progressData) {
        this._progressData = progressData;
    }
}
