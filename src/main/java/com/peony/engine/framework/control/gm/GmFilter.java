package com.peony.engine.framework.control.gm;

import com.peony.engine.framework.data.DataService;
import com.peony.engine.framework.server.Server;
import com.peony.engine.framework.tool.helper.BeanHelper;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * GM工具请求的过滤器，用于进行登录校验和登录处理。
 *
 * @author zhengyuzhen
 * @see GmService
 * @see Gm
 * @see GmAdmin
 * @see GmServlet
 * @see GmSegment
 * @since 1.0
 */
public class GmFilter implements Filter {

    private DataService dataService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        dataService = BeanHelper.getServiceBean(DataService.class);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)request;
        String path = httpServletRequest.getServletPath();

        if(path.endsWith(".js")){
            request.getRequestDispatcher(path).forward(request,response);
        }else{

            String url = ((HttpServletRequest) request).getRequestURL().toString();
            if(url.endsWith("submitlogin")){
                String account = request.getParameter("account");
                String password = request.getParameter("password");

                if(StringUtils.isEmpty(account) || StringUtils.isEmpty(password)){
                    request.getRequestDispatcher("/login.jsp?warn=params error").forward(request, response);
                    return;
                }
                GmAdmin gmAdmin = dataService.selectObjectBySql(GmAdmin.class,"select * from gmadmin where account=? limit 1",account);
                if(gmAdmin == null){
                    request.getRequestDispatcher("/login.jsp?warn=account not exist!").forward(request, response);
                    return;
                }

                if(!password.equals(gmAdmin.getPassword())){
                    request.getRequestDispatcher("/login.jsp?warn=password error!").forward(request, response);
                    return;
                }

                HttpSession session = ((HttpServletRequest) request).getSession();
                session.setAttribute("account",account);
                session.setAttribute("password",password);

                request.getRequestDispatcher("/gm.jsp").forward(request, response);
                return;
            }

            if(!Server.getEngineConfigure().getBoolean("server.is.test", false)) {
                HttpSession session = ((HttpServletRequest) request).getSession();
                String account = (String)session.getAttribute("account");
                String password = (String)session.getAttribute("password");

                if(StringUtils.isEmpty(account) || StringUtils.isEmpty(password)){
                    request.getRequestDispatcher("/login.jsp").forward(request, response);
                    return;
                }
            }


            String oper = httpServletRequest.getParameter("oper");
            if(oper == null) {
                request.getRequestDispatcher("/gm.jsp").forward(request, response);
            }else{
                request.getRequestDispatcher(path).forward(request, response);
            }
        }
    }

    @Override
    public void destroy() {

    }
}
