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
package edu.kit.scc.webreg.bean.project;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ComponentSystemEvent;
import javax.inject.Inject;

import edu.kit.scc.webreg.entity.project.ProjectEntity;
import edu.kit.scc.webreg.service.project.ProjectService;
import edu.kit.scc.webreg.session.SessionManager;
import edu.kit.scc.webreg.util.ViewIds;

@ManagedBean
@ViewScoped
public class ProjectAdminAddProjectBean implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private SessionManager session;
	
	@Inject
	private ProjectService service;
	
	private ProjectEntity entity;

	public void preRenderView(ComponentSystemEvent ev) {
		if (entity == null)
			entity = service.createNew();
		
	}
	
	public String save() {
		entity = service.save(entity, session.getIdentityId());
		
		return ViewIds.PROJECT_INDEX + "&faces-redirect=true";
	}

	public String cancel() {
		return ViewIds.PROJECT_INDEX + "&faces-redirect=true";
	}

	public ProjectEntity getEntity() {
		return entity;
	}

	public void setEntity(ProjectEntity entity) {
		this.entity = entity;
	}
}