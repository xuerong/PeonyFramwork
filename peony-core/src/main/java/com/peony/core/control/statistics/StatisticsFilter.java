package com.peony.core.control.statistics;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Created by a on 2016/9/29.
 */
public class StatisticsFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)request;
        String path = httpServletRequest.getServletPath();
        if(path.endsWith(".js")){
            request.getRequestDispatcher(path).forward(request,response);
        }else{
            String oper = httpServletRequest.getParameter("oper");
            if(oper == null) {
                request.getRequestDispatcher("/statistics.jsp").forward(request, response);
            }else{
                request.getRequestDispatcher(path).forward(request, response);
            }
        }
    }

    @Override
    public void destroy() {

    }
}
