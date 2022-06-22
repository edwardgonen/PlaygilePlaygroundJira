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

@Scanned
@Transactional
public class ActiveObjectsUserList extends HttpServlet{
    ActiveObjects ao;
    public ActiveObjectsUserList(ActiveObjects ao)
    {
        this.ao = ao;
    }

    @Override
    protected void doGet (HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException
    {
        ao.executeInTransaction((TransactionCallback<Void>) () -> {

            ManageActiveObjects mao = new ManageActiveObjects(ao);
            ManageActiveObjectsResult maor = mao.ListAllUsers();
            servletMisc.responseListToWeb(maor, resp);
            return null;
        });
    }
}
