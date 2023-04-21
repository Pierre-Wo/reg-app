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
package edu.kit.scc.webreg.dao.project;

import java.util.List;
import java.util.Set;

import edu.kit.scc.webreg.dao.BaseDao;
import edu.kit.scc.webreg.entity.ServiceEntity;
import edu.kit.scc.webreg.entity.identity.IdentityEntity;
import edu.kit.scc.webreg.entity.project.ProjectAdminType;
import edu.kit.scc.webreg.entity.project.ProjectEntity;
import edu.kit.scc.webreg.entity.project.ProjectIdentityAdminEntity;
import edu.kit.scc.webreg.entity.project.ProjectMembershipEntity;
import edu.kit.scc.webreg.entity.project.ProjectMembershipType;
import edu.kit.scc.webreg.entity.project.ProjectServiceEntity;
import edu.kit.scc.webreg.entity.project.ProjectServiceStatusType;
import edu.kit.scc.webreg.entity.project.ProjectServiceType;

public interface BaseProjectDao<T extends ProjectEntity> extends BaseDao<T> {

	List<ProjectServiceEntity> findAllByService(ServiceEntity service);

	ProjectServiceEntity addServiceToProject(ProjectEntity project, ServiceEntity service, ProjectServiceType type,
			ProjectServiceStatusType status);

	ProjectIdentityAdminEntity addAdminToProject(ProjectEntity project, IdentityEntity identity, ProjectAdminType type);

	void removeAdminFromProject(ProjectIdentityAdminEntity pia);

	ProjectMembershipEntity addMemberToProject(ProjectEntity project, IdentityEntity identity, ProjectMembershipType type);

	void deleteMembership(ProjectMembershipEntity entity);

	void deleteProjectService(ProjectServiceEntity entity);

	List<ProjectIdentityAdminEntity> findAdminByIdentity(IdentityEntity identity);

	List<ProjectIdentityAdminEntity> findAdminsForProject(ProjectEntity project);

	Set<ProjectServiceEntity> findServicesForProject(ProjectEntity project, Boolean withParents, ProjectServiceStatusType status);

	ProjectMembershipEntity findByIdentityAndProject(IdentityEntity identity, ProjectEntity project);

	List<ProjectMembershipEntity> findByIdentity(IdentityEntity identity);

	List<IdentityEntity> findIdentitiesForProject(ProjectEntity project);

	List<ProjectMembershipEntity> findMembersForProject(ProjectEntity project, boolean withChildren);

	Set<ProjectServiceEntity> findServicesForProject(ProjectEntity project, Boolean withParents);

	ProjectServiceEntity findByServiceAndProject(ServiceEntity service, ProjectEntity project);

	ProjectServiceEntity mergeProjectService(ProjectServiceEntity entity);

	void removeMemberFromProject(ProjectMembershipEntity pme);

}
