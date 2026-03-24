package com.sk.domain.shared.service;

import com.sk.common.properties.ChartCaptureProperties;
import com.sk.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
    private static final String NEWSLETTER_ID_CLAIM = "newsletterId";
    private static final String ISSUE_NO_CLAIM = "issueNo";
    private static final String PURPOSE_CLAIM = "purpose";

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
            .claim(NEWSLETTER_ID_CLAIM, newsletterId)
            .claim(ISSUE_NO_CLAIM, issueNo)
            .claim(PURPOSE_CLAIM, PURPOSE_VALUE)
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plusSeconds(properties.getJwtTtlSeconds())))
            .signWith(jwtUtil.getPrivateKey(), Jwts.SIG.RS256)
            .compact();
    }

    /**
     * JWT 서명/만료/purpose 검증만 수행한다.
     */
    public Claims validateAndExtract(String token) {
        return parseValidatedClaims(token);
    }

    /**
     * JWT 서명/만료/purpose 검증 후 차트 조회에 필요한 claim을 안전하게 추출한다.
     */
    public ChartCaptureTokenClaims validateAndExtractChartContext(String token) {
        Claims claims = parseValidatedClaims(token);
        Integer newsletterId = extractRequiredIntegerClaim(claims, NEWSLETTER_ID_CLAIM);
        Integer issueNo = extractRequiredIntegerClaim(claims, ISSUE_NO_CLAIM);
        return new ChartCaptureTokenClaims(newsletterId, issueNo);
    }

    private Claims parseValidatedClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(jwtUtil.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

            if (!PURPOSE_VALUE.equals(claims.get(PURPOSE_CLAIM))) {
                throw new IllegalArgumentException("Invalid chart-capture token purpose");
            }
            return claims;
        } catch (ExpiredJwtException e) {
            throw new IllegalArgumentException("Chart-capture token expired", e);
        } catch (MalformedJwtException e) {
            throw new IllegalArgumentException("Malformed chart-capture token", e);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid chart-capture token", e);
        }
    }

    private Integer extractRequiredIntegerClaim(Claims claims, String claimName) {
        Object claimValue = claims.get(claimName);

        if (claimValue == null) {
            throw new IllegalArgumentException("Missing chart-capture token claim: " + claimName);
        }

        try {
            if (claimValue instanceof Integer integerValue) {
                return integerValue;
            }
            if (claimValue instanceof Number numberValue) {
                return Math.toIntExact(numberValue.longValue());
            }
            if (claimValue instanceof String stringValue) {
                return Integer.parseInt(stringValue);
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid chart-capture token claim: " + claimName, e);
        }

        throw new IllegalArgumentException("Invalid chart-capture token claim: " + claimName);
    }

    public record ChartCaptureTokenClaims(Integer newsletterId, Integer issueNo) {
    }

}
