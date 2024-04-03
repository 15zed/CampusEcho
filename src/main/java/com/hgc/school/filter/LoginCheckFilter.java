package com.hgc.school.filter;


import lombok.extern.slf4j.Slf4j;

import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 全局的用户校验
 */
@WebFilter("/*")
@Slf4j
public class LoginCheckFilter implements Filter {
    //请求路径比较器
    private static final AntPathMatcher matcher = new AntPathMatcher();


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestURI = request.getRequestURI();

        log.info("拦截到请求：" + requestURI);
        //不需要处理的请求路径
        String[] urls = {
                "/",
                "/css/**",
                "/js/**",
                "/user/forward",
                "/user/login",
                "/user/register",
                "/user/logout"
        };


        if (check(urls, requestURI)) {
            log.info("本次请求" + requestURI + "不需要处理");
            filterChain.doFilter(request, response);
            return;
        }
        //判断登录状态，如果已经登录，直接放行
        if (request.getSession().getAttribute("user") != null) {
            log.info("用户已经登录");
            filterChain.doFilter(request, response);
            return;
        }
        //如果未登录，返回到登陆页面
        log.info("用户未登录");
        response.sendRedirect("/user/forward");
    }


    public boolean check(String[] urls, String requestURL) {
        for (String url : urls) {
            boolean result = matcher.match(url, requestURL);
            if (result) {
                return true;
            }
        }
        return false;
    }
}
