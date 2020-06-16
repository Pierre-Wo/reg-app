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
package edu.kit.scc.webreg.bean.admin.ssh;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.event.ComponentSystemEvent;
import javax.inject.Inject;

import org.primefaces.model.LazyDataModel;

import edu.kit.scc.webreg.entity.SshPubKeyEntity;
import edu.kit.scc.webreg.model.GenericLazyDataModelImpl;
import edu.kit.scc.webreg.service.ssh.SshPubKeyService;

@ManagedBean
@ViewScoped
public class ListSshPubKeyBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private LazyDataModel<SshPubKeyEntity> list;
    
    @Inject
    private SshPubKeyService service;

	public void preRenderView(ComponentSystemEvent ev) {
		if (list == null) {
			list = new GenericLazyDataModelImpl<SshPubKeyEntity, SshPubKeyService, Long>(service);
		}
	}

    public LazyDataModel<SshPubKeyEntity> getEntityList() {
   		return list;
    }
}