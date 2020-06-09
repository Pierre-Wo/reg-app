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
package edu.kit.scc.webreg.service.ssh;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import edu.kit.scc.webreg.dao.BaseDao;
import edu.kit.scc.webreg.dao.SshPubKeyDao;
import edu.kit.scc.webreg.entity.SshPubKeyEntity;
import edu.kit.scc.webreg.entity.SshPubKeyStatus;
import edu.kit.scc.webreg.service.impl.BaseServiceImpl;

@Stateless
public class SshPubKeyServiceImpl extends BaseServiceImpl<SshPubKeyEntity, Long> implements SshPubKeyService {

	private static final long serialVersionUID = 1L;

	@Inject
	private SshPubKeyDao dao;

	@Override
	public List<SshPubKeyEntity> findByUser(Long userId) {
		return dao.findByUser(userId);
	}
	
	@Override
	public List<SshPubKeyEntity> findByUserAndStatus(Long userId, SshPubKeyStatus keyStatus) {
		return dao.findByUserAndStatus(userId, keyStatus);
	}
	
	@Override
	protected BaseDao<SshPubKeyEntity, Long> getDao() {
		return dao;
	}
}
