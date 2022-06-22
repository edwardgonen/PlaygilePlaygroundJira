package com.playgileplayground.jira.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsEntityKey;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Scanned
@Transactional
public class ActiveObjectsDelete extends HttpServlet{
    ActiveObjects ao;
    public ActiveObjectsDelete(ActiveObjects ao)
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
                } catch (IOException ignored) {
                }
                return null;
            }
            String roadmapFeature = Optional.ofNullable(req.getParameter("feature")).orElse("");
            ManageActiveObjects mao = new ManageActiveObjects(ao);
            ManageActiveObjectsEntityKey key = new ManageActiveObjectsEntityKey(projectKey, roadmapFeature);
            ManageActiveObjectsResult maor = mao.DeleteProjectEntity(key); //will not create if exists
            servletMisc.responseToWeb("Deleted",maor, resp);
            return null;
        });
    }
}
