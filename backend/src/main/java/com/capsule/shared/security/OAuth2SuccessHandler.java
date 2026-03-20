package com.capsule.shared.security;

import com.capsule.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserService userService;

    public OAuth2SuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        var oauthUser = (OAuth2User) authentication.getPrincipal();
        var token = (OAuth2AuthenticationToken) authentication;
        var provider = token.getAuthorizedClientRegistrationId();
        var subject = oauthUser.getAttribute("sub") != null
                ? oauthUser.<String>getAttribute("sub")
                : oauthUser.<String>getAttribute("id");
        var email = oauthUser.<String>getAttribute("email");

        var user = userService.upsertOAuthUser(provider, subject, email);
        var tokens = userService.issueTokens(user);

        var redirectUrl = String.format("/oauth2/success?accessToken=%s&refreshToken=%s",
                tokens.accessToken(), tokens.refreshToken());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
