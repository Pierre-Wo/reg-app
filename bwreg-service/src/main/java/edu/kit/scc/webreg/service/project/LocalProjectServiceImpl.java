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
package edu.kit.scc.webreg.service.project;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import edu.kit.scc.webreg.dao.BaseDao;
import edu.kit.scc.webreg.dao.identity.IdentityDao;
import edu.kit.scc.webreg.dao.project.LocalProjectDao;
import edu.kit.scc.webreg.entity.ServiceEntity;
import edu.kit.scc.webreg.entity.identity.IdentityEntity;
import edu.kit.scc.webreg.entity.project.LocalProjectEntity;
import edu.kit.scc.webreg.service.impl.BaseServiceImpl;

@Stateless
public class LocalProjectServiceImpl extends BaseServiceImpl<LocalProjectEntity, Long> implements LocalProjectService {

	private static final long serialVersionUID = 1L;

	@Inject
	private IdentityDao identityDao;
	
	@Inject
	private LocalProjectDao dao;
	
	@Inject
	private LocalProjectCreater creater;
	
	@Override
	public List<LocalProjectEntity> findByService(ServiceEntity service) {
		return dao.findByService(service);
	}

	@Override 
	public LocalProjectEntity save(LocalProjectEntity project, Long identityId) {
		IdentityEntity identity = identityDao.findById(identityId);
		
		return (LocalProjectEntity) creater.create(project, identity);
	}
		
	@Override
	protected BaseDao<LocalProjectEntity, Long> getDao() {
		return dao;
	}
}
