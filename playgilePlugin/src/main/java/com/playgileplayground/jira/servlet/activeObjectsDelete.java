package com.playgileplayground.jira.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

@Scanned
@Transactional
public class activeObjectsDelete extends HttpServlet{
    ActiveObjects ao;
    public activeObjectsDelete(ActiveObjects ao)
    {
        this.ao = ao;
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String projectKey = Optional.ofNullable(req.getParameter("projectKey")).orElse("");
        if (projectKey.isEmpty())
        {
            resp.getWriter().write("No project key provided");
            return;
        }
        ManageActiveObjects mao = new ManageActiveObjects(this.ao);
        ManageActiveObjectsResult maor = mao.DeleteProjectEntity(projectKey); //will not create if exists
        if (maor.Code == ManageActiveObjectsResult.STATUS_CODE_SUCCESS) {
                resp.getWriter().write("Deleted");
        }
        else
        {
            resp.getWriter().write("Failure " + maor.Message);
        }
    }
}
