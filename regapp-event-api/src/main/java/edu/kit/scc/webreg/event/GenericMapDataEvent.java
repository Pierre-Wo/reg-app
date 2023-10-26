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
package edu.kit.scc.webreg.event;

import java.util.HashMap;

import edu.kit.scc.webreg.entity.audit.AuditEntryEntity;

public class GenericMapDataEvent extends AbstractEvent<HashMap<String, String>> {

	private static final long serialVersionUID = 1L;

	public GenericMapDataEvent(HashMap<String, String> map) {
		super(map);
	}
	
	public GenericMapDataEvent(HashMap<String, String> map, AuditEntryEntity audit) {
		super(map, audit);
	}
}
