package com.playgileplayground.jira.jiraissues;

/**
 * Created by Ext_EdG on 11/6/2020.
 */
public class JiraQueryResult {
    public int Code;
    public String Message;
    public Object Result;
    public JiraQueryResult()
    {
        this.Code = STATUS_CODE_SUCCESS;
        this.Result = null;
        this.Message = "Success";
    }

    /////////// codes
    final static int STATUS_CODE_BASE = 0;
    public static final int STATUS_CODE_SUCCESS = STATUS_CODE_BASE;
    public static final int STATUS_CODE_DATE_PARSE_ERROR = STATUS_CODE_BASE + 1;
    public static final int STATUS_CODE_DATE_IS_EMPTY = STATUS_CODE_BASE + 2;
    public static final int STATUS_CODE_NO_SUCH_FIELD = STATUS_CODE_BASE + 3;
}
