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

import edu.kit.scc.webreg.entity.GroupEntity;
import edu.kit.scc.webreg.entity.RegistryStatus;
import edu.kit.scc.webreg.entity.ServiceEntity;
import edu.kit.scc.webreg.entity.UserEntity;
import edu.kit.scc.webreg.entity.identity.IdentityEntity;

public interface UserDao extends BaseDao<UserEntity> {

	List<UserEntity> findByEppn(String eppn);

	UserEntity findByIdWithAll(Long id);

	UserEntity findByIdWithStore(Long id);

	List<UserEntity> findByGroup(GroupEntity group);

	UserEntity findByUidNumber(Integer uidNumber);

	List<UserEntity> findByIdentity(IdentityEntity identity);

	List<UserEntity> findByAttribute(String key, String value);

	List<UserEntity> findByGeneric(String key, String value);

	List<UserEntity> findByGroupAndService(GroupEntity group, ServiceEntity service, RegistryStatus registryStatus);

}
