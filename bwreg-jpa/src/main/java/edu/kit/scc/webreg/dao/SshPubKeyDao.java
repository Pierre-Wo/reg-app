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
package edu.kit.scc.webreg.dao;

import java.util.List;

import edu.kit.scc.webreg.entity.SshPubKeyEntity;
import edu.kit.scc.webreg.entity.SshPubKeyStatus;

public interface SshPubKeyDao extends BaseDao<SshPubKeyEntity, Long> {

	List<SshPubKeyEntity> findByUser(Long userId);

	List<SshPubKeyEntity> findByUserAndStatus(Long userId, SshPubKeyStatus keyStatus);

	List<SshPubKeyEntity> findByUserAndKey(Long userId, String encodedKey);

	List<SshPubKeyEntity> findByUserAndStatusWithRegs(Long userId, SshPubKeyStatus keyStatus);

	List<SshPubKeyEntity> findByKey(String encodedKey);

}
