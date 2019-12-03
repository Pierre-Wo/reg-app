package edu.kit.scc.webreg.service.oidc;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.kit.scc.webreg.service.saml.exc.OidcAuthenticationException;
import net.minidev.json.JSONObject;

public interface OidcOpLogin {

	void registerAuthRequest(String realm, String responseType, String redirectUri, String scope, String state,
			String nonce, String clientId, HttpServletRequest request, HttpServletResponse response)
					 throws IOException, OidcAuthenticationException ;

	JSONObject serveToken(String realm, String grantType, String code, String redirectUri, HttpServletRequest request,
			HttpServletResponse response) throws OidcAuthenticationException;

	JSONObject serveUserInfo(String realm, String tokeType, String tokenId, HttpServletRequest request,
			HttpServletResponse response) throws OidcAuthenticationException;

}