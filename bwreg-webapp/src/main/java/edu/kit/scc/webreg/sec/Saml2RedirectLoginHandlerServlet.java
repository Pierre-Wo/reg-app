/*******************************************************************************
 * Copyright (c) 2014 Michael Simon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Michael Simon - initial
 ******************************************************************************/
package edu.kit.scc.webreg.sec;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.messaging.encoder.MessageEncodingException;
import org.slf4j.Logger;

import edu.kit.scc.webreg.service.saml.SamlSpRedirectService;
import edu.kit.scc.webreg.session.SessionManager;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

@Named
@WebServlet(urlPatterns = {"/Shibboleth.sso/Login", "/saml/sp/login"})
public class Saml2RedirectLoginHandlerServlet implements Servlet {

	@Inject
	private Logger logger;

	@Inject
	private SessionManager session;

	@Inject
	private SamlSpRedirectService samlSpRedirectService;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		
	}

	@Override
	public void service(ServletRequest servletRequest, ServletResponse servletResponse)
			throws ServletException, IOException {

		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		if (session == null || session.getIdpId() == null || session.getSpId() == null) {
			logger.debug("Client session from {} not established. Sending client back to welcome page",
					request.getRemoteAddr());
			response.sendRedirect("/welcome/index.xhtml");
			return;
		}
		
		try {
			samlSpRedirectService.redirectClient(session.getIdpId(), session.getSpId(), request, response);

		} catch (MessageEncodingException e) {
            throw new ServletException("Error encoding outgoing message", e);
        } catch (ComponentInitializationException e) {
            throw new ServletException("Error encoding outgoing message", e);
		}
		
	}
	
	@Override
	public ServletConfig getServletConfig() {
		return null;
	}

	@Override
	public String getServletInfo() {
		return null;
	}

	@Override
	public void destroy() {
	}	
}
