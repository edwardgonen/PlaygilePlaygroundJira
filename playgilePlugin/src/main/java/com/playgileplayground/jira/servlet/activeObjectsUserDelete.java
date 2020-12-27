package com.playgileplayground.jira.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.playgileplayground.jira.persistence.ManageActiveObjects;
import com.playgileplayground.jira.persistence.ManageActiveObjectsResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Scanned
@Transactional
public class activeObjectsUserDelete extends HttpServlet{
    ActiveObjects ao;
    public activeObjectsUserDelete(ActiveObjects ao)
    {
        this.ao = ao;
    }

    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException
    {
        ao.executeInTransaction((TransactionCallback<Void>) () -> {
            String user = Optional.ofNullable(req.getParameter("user")).orElse("");
            if (user.isEmpty())
            {
                try {
                    resp.getWriter().write("No user key provided");
                } catch (IOException ignored) {
                }
                return null;
            }
            ManageActiveObjects mao = new ManageActiveObjects(ao);
            ManageActiveObjectsResult maor = mao.DeleteUserEntity(user); //will not create if exists
            servletMisc.responseToWeb("Deleted", maor, resp);
            return null;
        });
    }
}
