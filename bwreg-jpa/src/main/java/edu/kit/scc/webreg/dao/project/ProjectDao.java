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
package edu.kit.scc.webreg.dao.project;

import java.util.List;

import edu.kit.scc.webreg.dao.BaseDao;
import edu.kit.scc.webreg.entity.ServiceEntity;
import edu.kit.scc.webreg.entity.identity.IdentityEntity;
import edu.kit.scc.webreg.entity.project.ProjectAdminType;
import edu.kit.scc.webreg.entity.project.ProjectEntity;
import edu.kit.scc.webreg.entity.project.ProjectIdentityAdminEntity;
import edu.kit.scc.webreg.entity.project.ProjectMembershipEntity;
import edu.kit.scc.webreg.entity.project.ProjectServiceEntity;
import edu.kit.scc.webreg.entity.project.ProjectServiceType;

public interface ProjectDao extends BaseDao<ProjectEntity, Long> {

	List<ProjectEntity> findByService(ServiceEntity service);

	ProjectServiceEntity addServiceToProject(ProjectEntity project, ServiceEntity service, ProjectServiceType type);

	ProjectIdentityAdminEntity addAdminToProject(ProjectEntity project, IdentityEntity identity, ProjectAdminType type);

	List<ProjectIdentityAdminEntity> findAdminByIdentity(IdentityEntity identity);

	List<ProjectMembershipEntity> findMembersForProject(ProjectEntity project);
	
	List<ProjectIdentityAdminEntity> findAdminsForProject(ProjectEntity project);
}
