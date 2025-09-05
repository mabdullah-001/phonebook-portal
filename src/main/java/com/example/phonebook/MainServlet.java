package com.example.phonebook;

import com.vaadin.flow.server.VaadinServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.annotation.WebInitParam;

@WebServlet(urlPatterns = "/*", name = "MainServlet", asyncSupported = true,
        initParams = {
                @WebInitParam(name = "ui", value = "com.example.phonebook.MainView")
        })
public class MainServlet extends VaadinServlet {


}
