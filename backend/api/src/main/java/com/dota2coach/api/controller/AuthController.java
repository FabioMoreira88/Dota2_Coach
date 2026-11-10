package com.dota2coach.api.controller;

import com.dota2coach.api.config.AppProperties;
import com.dota2coach.api.security.SteamLoginException;
import com.dota2coach.api.security.SteamOpenIdService;
import com.dota2coach.api.security.SteamPrincipal;
import com.dota2coach.core.model.SteamId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Início e volta do login pela Steam. O logout é tratado pelo Spring Security. */
@RestController
@RequestMapping("/api/v1/auth/steam")
public class AuthController {

    static final String STATE_ATTRIBUTE = "steamLoginState";
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final SteamOpenIdService steamOpenIdService;
    private final SecurityContextRepository securityContextRepository;
    private final AppProperties appProperties;

    public AuthController(
            SteamOpenIdService steamOpenIdService,
            SecurityContextRepository securityContextRepository,
            AppProperties appProperties) {
        this.steamOpenIdService = steamOpenIdService;
        this.securityContextRepository = securityContextRepository;
        this.appProperties = appProperties;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login(HttpServletRequest request) {
        String state = steamOpenIdService.newState();
        request.getSession(true).setAttribute(STATE_ATTRIBUTE, state);
        return redirect(steamOpenIdService.loginUrl(state));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam Map<String, String> params,
            @RequestParam(name = "state", required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        String expectedState = session == null ? null : (String) session.getAttribute(STATE_ATTRIBUTE);
        if (session != null) {
            session.removeAttribute(STATE_ATTRIBUTE);
        }

        Map<String, String> openIdParams = new HashMap<>();
        params.forEach((key, value) -> {
            if (key.startsWith("openid.")) {
                openIdParams.put(key, value);
            }
        });

        SteamId steamId;
        try {
            steamId = steamOpenIdService.verify(openIdParams, state, expectedState);
        } catch (SteamLoginException e) {
            log.warn("Login Steam recusado: {}", e.getMessage());
            return redirect(appProperties.frontendUrl() + "/?login=erro");
        }

        request.changeSessionId();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                new SteamPrincipal(steamId.steamId64()), null,
                List.of(new SimpleGrantedAuthority("ROLE_PLAYER")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return redirect(appProperties.frontendUrl() + "/?login=ok");
    }

    private static ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}
