package com.Fanbbs.common;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import java.util.Calendar;
import java.util.Map;

public class JWT {
    @Value("${token.secret:CHIKATA}")
    private static final String secret_key = "ABCDEFGHIJKLMN123456789114514";

    @Value("${token.issue:CHIKATA}")
    private static final String issue = "CHIKATA";

    @Value("${token.exp:7}")
    private static final Integer exp = 7;
    /**
     * 生成token  header.payload.singature
     */

    public static String getToken(Map<String, String> map) {

        Calendar instance = Calendar.getInstance();
        // 默认7天过期
        instance.add(Calendar.DATE, exp);

        //创建jwt builder
        JWTCreator.Builder builder = com.auth0.jwt.JWT.create();

        // payload
        map.forEach((k, v) -> {
            builder.withClaim(k, v);
        });
        builder.withClaim("iss",issue);
        String token = builder.withExpiresAt(instance.getTime())  //指定令牌过期时间
                .sign(Algorithm.HMAC256(secret_key));  // sign
        return token;
    }

    /**
     * 验证token  合法性
     */
    public static DecodedJWT verify(String token) {
        return com.auth0.jwt.JWT.require(Algorithm.HMAC256(secret_key)).build().verify(token);
    }

    /**
     * 获取token信息方法

    public static DecodedJWT getTokenInfo(String token){
        DecodedJWT verify = com.auth0.jwt.JWT.require(Algorithm.HMAC256(SING)).build().verify(token);
        return verify;
    }*/
}

