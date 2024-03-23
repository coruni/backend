package com.Fanbbs.common;

import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class JWTInterceptors implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Map<String,Object> map = new HashMap<>();
        // 获取请求头中令牌
        String token = request.getHeader("Authorization");

        try {
            // 验证令牌
            JWT.verify(token);
            return true;  // 放行请求
        } catch (SignatureVerificationException e) {
            map.put("msg","无效签名！");
            map.put("code",402);
        }catch (TokenExpiredException e){
            map.put("msg","token过期");
            map.put("code",401);
        }catch (AlgorithmMismatchException e){
            map.put("msg","算法不一致");
            map.put("code",403);
        }catch (Exception e){
            map.put("msg","token无效！");
            map.put("code",404);

        }
        map.put("state",false);  // 设置状态
        // 将map以json的形式响应到前台  map --> json  (jackson)
        String json = new ObjectMapper().writeValueAsString(map);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().println(json);
        return  false;
    }
}
