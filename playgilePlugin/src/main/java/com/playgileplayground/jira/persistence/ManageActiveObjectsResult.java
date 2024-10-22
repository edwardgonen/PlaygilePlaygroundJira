package com.playgileplayground.jira.persistence;

/**
 * Created by Ext_EdG on 7/3/2020.
 */
public class ManageActiveObjectsResult {
    public int Code;
    public String Message;
    public Object Result;
    public ManageActiveObjectsResult()
    {
        this.Code = STATUS_CODE_SUCCESS;
        this.Result = null;
        this.Message = "Success";
    }

    /////////// codes
    final static int STATUS_CODE_BASE = 0;
    public static final int STATUS_CODE_SUCCESS = STATUS_CODE_BASE;
    public static final int STATUS_CODE_ENTRY_ALREADY_EXISTS = STATUS_CODE_BASE + 1;
    public static final int STATUS_CODE_NO_SUCH_ENTRY = STATUS_CODE_BASE + 2;
    public static final int STATUS_CODE_EXCEPTION = STATUS_CODE_BASE + 3;
    public static final int STATUS_CODE_PROJECT_NOT_FOUND = STATUS_CODE_BASE + 4;
    public static final int STATUS_CODE_REMAINING_ESTIMATIONS_FOR_DATE_NOT_FOUND = STATUS_CODE_BASE + 5;
    public static final int STATUS_CODE_DATA_FORMAT_PARSING_ERROR = STATUS_CODE_BASE + 6;
    public static final int STATUS_CODE_NO_ESTIMATIONS_YET = STATUS_CODE_BASE + 7;
    public static final int STATUS_CODE_WRONG_PARAMETER_VALUE = STATUS_CODE_BASE + 8;

}
