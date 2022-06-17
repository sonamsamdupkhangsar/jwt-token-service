package me.sonam.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.*;

@Service
public class JwtService implements Jwt {

    private static final Logger LOG = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer}")
    private String issuer;

    static final String TOKEN_PREFIX = "Bearer";

    public JwtService() {
        LOG.info("started JwtService");
    }

    @Override
    public Mono<String> create(String subject, String audience, int calendarField, int calendarValue, String apiKey) {
        LOG.info("issuer: {}, secret: {}", issuer, secret);
        Calendar calendar = Calendar.getInstance();
        calendar.add(calendarField, calendarValue);
        Date expireDate = calendar.getTime();

        String jwt = Jwts.builder()
                .setSubject(subject)
                .setHeaderParam("apiKey", apiKey)
                .setIssuer(issuer)
                .setAudience(audience)
                .setExpiration(expireDate)
                .setId(UUID.randomUUID().toString())
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();

        LOG.info("returning jwt");
        return Mono.just(jwt);
    }

    @Override
    public Mono<Map<String, String>> validate(String jwt) {
        LOG.info("issuer: {}, secret: {}", issuer, secret);
        if (jwt != null) {
            LOG.debug("token is not null");

            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(jwt.replace(TOKEN_PREFIX, ""))
                    .getBody();

            Map<String, String> map = new HashMap<>();
            map.put("subject", claims.getSubject());
            map.put("audience", claims.getAudience());
            map.put("id", claims.getId());
            map.put("issuer", claims.getIssuer());

            Date expirationDate = claims.getExpiration();
            if (expirationDate == null) {
                LOG.info("no expiration date, jwt is valid");
                return Mono.just(map);
            }
            else {
                Calendar calendar = Calendar.getInstance();
                Date currentDate = calendar.getTime();

                if (currentDate.before(expirationDate)) {
                    LOG.debug("jwt is valid");
                    return Mono.just(map);
                } else {
                    LOG.debug("token has expired, ask user to renew");
                    return Mono.empty();
                }
            }

        }
        else {
            LOG.error("cannot authenticate a null jwt token");
            throw new RuntimeException("jwt is null");
        }

    }
}
