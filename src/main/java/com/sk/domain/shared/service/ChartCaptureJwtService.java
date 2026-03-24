package com.sk.domain.shared.service;

import com.sk.common.properties.ChartCaptureProperties;
import com.sk.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import java.time.Instant;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChartCaptureJwtService {

    private static final String PURPOSE_VALUE = "chart-capture";

    private final JwtUtil jwtUtil;
    private final ChartCaptureProperties properties;

    /**
     * 차트 캡처용 단기 JWT 발급
     * claims: newsletterId, issueNo, purpose=chart-capture
     */
    public String issueToken(Integer newsletterId, Integer issueNo) {
        return Jwts.builder()
            .header().type("JWT").and()
            .subject(PURPOSE_VALUE)
            .claim("newsletterId", newsletterId)
            .claim("issueNo", issueNo)
            .claim("purpose", PURPOSE_VALUE)
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plusSeconds(properties.getJwtTtlSeconds())))
            .signWith(jwtUtil.getPrivateKey(), Jwts.SIG.RS256)
            .compact();
    }

    /**
     * JWT 서명/만료/purpose 검증 후 Claims 반환
     * @throws IllegalArgumentException 서명 오류, 만료, purpose 불일치
     */
    public Claims validateAndExtract(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(jwtUtil.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

            if (!PURPOSE_VALUE.equals(claims.get("purpose"))) {
                throw new IllegalArgumentException("Invalid chart-capture token purpose");
            }
            return claims;
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("Chart-capture token expired", e);
        } catch (MalformedJwtException e) {
            throw new IllegalArgumentException("Malformed chart-capture token", e);
        }
    }

}
