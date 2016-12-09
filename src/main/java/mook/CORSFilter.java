package mook;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("*")
public class CORSFilter implements Filter {

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        ((HttpServletResponse)response).addHeader("Access-Control-Allow-Origin", "*");
        ((HttpServletResponse)response).addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE");
        ((HttpServletResponse)response).addHeader("Access-Control-Allow-Headers", "Content-Type,X-requested-with,auth");

        if (!((HttpServletRequest)request).getMethod().equalsIgnoreCase("OPTIONS")) {
            chain.doFilter(request, response);
        }
    }
    
    public void init(FilterConfig filterConfig) {}

    public void destroy() {}
}