/*
 * *****************************************************************************
 * Copyright (c) 2014 Michael Simon.
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3.0 which accompanies
 * this distribution, and is available at http://www.gnu.org/licenses/gpl.html
 *
 * Contributors: Michael Simon - initial
 * *****************************************************************************
 */
package edu.kit.scc.webreg.dao;

import java.util.Date;
import java.util.List;

import edu.kit.scc.webreg.entity.RegistryEntity;
import edu.kit.scc.webreg.entity.RegistryStatus;
import edu.kit.scc.webreg.entity.ServiceEntity;
import edu.kit.scc.webreg.entity.UserEntity;
import edu.kit.scc.webreg.entity.identity.IdentityEntity;

public interface RegistryDao extends BaseDao<RegistryEntity> {

	List<RegistryEntity> findByService(ServiceEntity service);

	List<RegistryEntity> findByServiceAndStatus(ServiceEntity service, RegistryStatus status);

	List<RegistryEntity> findByServiceAndAttribute(String key, String value, ServiceEntity service);

	List<RegistryEntity> findByIdentityAndStatus(IdentityEntity identity, RegistryStatus... status);

	RegistryEntity findByServiceAndUserAndStatus(ServiceEntity service, UserEntity user, RegistryStatus status);

	List<RegistryEntity> findByUser(UserEntity user);

	List<UserEntity> findUserListByServiceAndStatus(ServiceEntity service, RegistryStatus status);

	List<RegistryEntity> findRegistriesForDepro(String serviceShortName);

	List<RegistryEntity> findByIdentityAndNotStatusAndNotHidden(IdentityEntity identity, RegistryStatus... status);

	List<RegistryEntity> findByServiceAndNotStatus(ServiceEntity service, RegistryStatus... status);

	List<RegistryEntity> findAllByRegValueAndStatus(ServiceEntity service, String key, String value, RegistryStatus status);

	List<RegistryEntity> findByServiceAndIdentityAndNotStatus(ServiceEntity service, IdentityEntity identity, RegistryStatus... status);

	List<RegistryEntity> findByUserAndStatus(UserEntity user, RegistryStatus... status);

	RegistryEntity findByServiceAndIdentityAndStatus(ServiceEntity service, IdentityEntity identity, RegistryStatus status);

	List<RegistryEntity> findAllExternalBySsn(String serviceShortName);

	RegistryEntity findRegistryForDepro(String serviceShortName, String key, String value);

	List<RegistryEntity> findByServiceAndStatusAndIDPGood(String serviceShortName, RegistryStatus status, Date date, int limit);

}
