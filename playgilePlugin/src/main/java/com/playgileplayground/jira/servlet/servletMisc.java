package com.playgileplayground.jira.servlet;

import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Ext_EdG on 7/16/2020.
 */
public class servletMisc {
    static void responseToWeb(String response, ManageActiveObjectsResult maor, HttpServletResponse resp)
    {
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
        {
            try {
                resp.getWriter().write(response);
            } catch (IOException e) {
            }
        } else

        {
            try {
                resp.getWriter().write("Failure " + maor.Message);
            } catch (IOException e) {
            }
        }
    }
    static void responseListToWeb(ManageActiveObjectsResult maor, HttpServletResponse resp)
    {
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS)
        {
            StringBuilder output = new StringBuilder();
            ArrayList<String> allEntities = (ArrayList<String>) maor.Result;
            for (String entity : allEntities)
            {
                output.append(entity + "<br>");
            }
            try {
                resp.getWriter().write(output.toString());
            } catch (IOException e) {
            }
        }
        else
        {
            try {
                resp.getWriter().write("Failure " + maor.Message);
            } catch (IOException e) {
            }
        }
    }
}
