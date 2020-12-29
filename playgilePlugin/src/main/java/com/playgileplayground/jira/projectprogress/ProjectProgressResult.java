package com.playgileplayground.jira.projectprogress;

import java.util.Date;

/**
 * Created by Ext_EdG on 7/10/2020.
 */
public class ProjectProgressResult {
    public int Code;
    public String Message;
    public Date idealProjectEnd;
    public Date predictedProjectEnd;
    public ProgressData progressData;
    public ProgressData idealData;


    public ProjectProgressResult()
    {
        this.Code = STATUS_CODE_SUCCESS;
        this.Message = "Success";
    }

    /////////// codes
    final static int STATUS_CODE_BASE = 0;
    public static final int STATUS_CODE_SUCCESS = STATUS_CODE_BASE;
    public static final int STATUS_CODE_BAD_INPUT = STATUS_CODE_BASE + 1;
}
