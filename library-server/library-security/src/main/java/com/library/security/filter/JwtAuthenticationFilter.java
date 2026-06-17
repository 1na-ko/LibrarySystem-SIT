package com.library.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.exception.ErrorCode;
import com.library.common.result.Result;
import com.library.core.enums.RoleEnum;
import com.library.core.enums.UserStatusEnum;
import com.library.security.context.LoginUser;
import com.library.security.jwt.JwtUtils;
import com.library.security.token.TokenService;
import com.library.security.util.SecurityResponseUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器.
 * <p>
 * 从 {@code Authorization: Bearer <token>} 提取 Access Token 并验签：
 * <ul>
 *   <li>无 Token → 不设置 SecurityContext，放行（由授权层 EntryPoint 兜底 401）</li>
 *   <li>验签成功且 type=access → 构造 {@link LoginUser} 设入 SecurityContext</li>
 *   <li>过期 → 直接返回 401 {@link ErrorCode#TOKEN_EXPIRED}</li>
 *   <li>无效/类型不符 → 直接返回 401 {@link ErrorCode#TOKEN_INVALID}</li>
 * </ul>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtUtils.parse(token);
            if (!jwtUtils.isAccess(claims)) {
                writeError(response, ErrorCode.TOKEN_INVALID);
                return;
            }
            // 校验 AT 是否在用户 logout 时间戳之前签发——若是则视为已撤销
            // （无状态 AT 设计下的撤销机制，详见 TokenServiceImpl LOGOUT_KEY_PREFIX）
            long userId = Long.parseLong(claims.getSubject());
            long iatEpochSeconds = claims.getIssuedAt().toInstant().getEpochSecond();
            if (tokenService.isAccessTokenLoggedOut(userId, iatEpochSeconds)) {
                log.debug("Access Token 已被登出撤销: userId={}, iat={}", userId, iatEpochSeconds);
                writeError(response, ErrorCode.TOKEN_INVALID);
                return;
            }
            LoginUser principal = buildPrincipal(claims);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            log.debug("Access Token 已过期: {}", e.getMessage());
            writeError(response, ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException e) {
            log.debug("Access Token 无效: {}", e.getMessage());
            writeError(response, ErrorCode.TOKEN_INVALID);
        } catch (IllegalArgumentException e) {
            // claims 中 role/userId 等字段为非法值（RoleEnum.valueOf / Long.parseLong 失败）
            log.debug("Access Token claims 非法: {}", e.getMessage());
            writeError(response, ErrorCode.TOKEN_INVALID);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            String tokenPart = header.substring(BEARER_PREFIX.length()).trim();
            return tokenPart.isEmpty() ? null : tokenPart;
        }
        return null;
    }

    private LoginUser buildPrincipal(Claims claims) {
        long userId = Long.parseLong(claims.getSubject());
        String username = claims.get(JwtUtils.CLAIM_USERNAME, String.class);
        RoleEnum role = RoleEnum.valueOf(claims.get(JwtUtils.CLAIM_ROLE, String.class));
        // JWT 无状态：status 默认 ACTIVE；账户冻结/禁用在 login/refresh 时校验
        return new LoginUser(userId, username, role, UserStatusEnum.ACTIVE,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        SecurityResponseUtil.writeJsonError(response, HttpStatus.UNAUTHORIZED,
                Result.error(errorCode), objectMapper);
    }
}
