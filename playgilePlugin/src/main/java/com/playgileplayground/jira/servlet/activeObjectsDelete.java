package com.playgileplayground.jira.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
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
    protected void doGet (HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        ao.executeInTransaction((TransactionCallback<Void>) () -> {
            String projectKey = Optional.ofNullable(req.getParameter("projectKey")).orElse("");
            if (projectKey.isEmpty())
            {
                try {
                    resp.getWriter().write("No project key provided");
                } catch (IOException e) {
                }
                return null;
            }
            String roadmapFeature = Optional.ofNullable(req.getParameter("roadmapFeature")).orElse("");
            ManageActiveObjects mao = new ManageActiveObjects(ao);
            ManageActiveObjectsEntityKey key = new ManageActiveObjectsEntityKey(projectKey, roadmapFeature);
            ManageActiveObjectsResult maor = mao.DeleteProjectEntity(key); //will not create if exists
            servletMisc.responseToWeb("Deleted",maor, resp);
            return null;
        });
    }
}
