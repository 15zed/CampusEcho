package com.hgc.campusechogateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Slf4j
@Component
public class GlobalAuthFilter implements GlobalFilter, Ordered {
    /**
     * 优先级提到最高
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }

    private static final AntPathMatcher matcher = new AntPathMatcher();

    // 不需要处理的请求路径
    private static final String[] urls = {
        "/",
        "/css/**",
        "/js/**",
        "/user/forward",
        "/user/login",
        "/user/register",
        "/user/logout"
    };

    /**
     * 判断当前请求是否需要处理
     * @param urls 不需要处理的请求数组
     * @param requestURL 当前请求
     * @return true：不用处理  false：需要处理
     */
    public boolean check(String[] urls, String requestURL) {
        for (String url : urls) {
            if (matcher.match(url, requestURL)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 获取请求路径
        String path = request.getURI().getPath();

        // 检查是否需要处理
        if (check(urls, path)) {
            log.info("本次请求" + path + "不需要处理");
            return chain.filter(exchange);
        }

        // 判断登录状态，如果已经登录，直接放行
        return exchange.getSession().flatMap(webSession -> {
            if (webSession.getAttribute("user") != null) {
                log.info("用户已经登录");
                return chain.filter(exchange);//放行
            }

            // 如果未登录，返回到登录页面
            log.info("用户未登录");
            response.setStatusCode(HttpStatus.SEE_OTHER);
            response.getHeaders().set(HttpHeaders.LOCATION, "/user/forward");
            return response.setComplete();//拦截
        });
    }
}
