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
package edu.kit.scc.webreg.entity.identity;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import edu.kit.scc.webreg.entity.AbstractBaseEntity;
import edu.kit.scc.webreg.entity.UserEntity;

@Entity(name = "IdentityEntity")
@Table(name = "idty")
public class IdentityEntity extends AbstractBaseEntity {

	private static final long serialVersionUID = 1L;

	@Column(name="twofa_user_id", length=512, unique = true)
	private String twoFaUserId;

	@Column(name="twofa_user_name", length=512)
	private String twoFaUserName;

	@OneToMany(targetEntity=UserEntity.class, mappedBy = "identity")
	private Set<UserEntity> users;

	@OneToMany(targetEntity=IdentityUserPreferenceEntity.class, mappedBy = "identity")
	private Set<IdentityUserPreferenceEntity> userPrefs;

	@ManyToOne(targetEntity = UserEntity.class)
    @JoinColumn(name = "pref_user_id")
	private UserEntity prefUser;

	@Column(name = "uid_number")
	private Integer uidNumber;

	public Set<UserEntity> getUsers() {
		return users;
	}

	public void setUsers(Set<UserEntity> users) {
		this.users = users;
	}

	public String getTwoFaUserId() {
		return twoFaUserId;
	}

	public void setTwoFaUserId(String twoFaUserId) {
		this.twoFaUserId = twoFaUserId;
	}

	public String getTwoFaUserName() {
		return twoFaUserName;
	}

	public void setTwoFaUserName(String twoFaUserName) {
		this.twoFaUserName = twoFaUserName;
	}

	public Set<IdentityUserPreferenceEntity> getUserPrefs() {
		return userPrefs;
	}

	public void setUserPrefs(Set<IdentityUserPreferenceEntity> userPrefs) {
		this.userPrefs = userPrefs;
	}

	public UserEntity getPrefUser() {
		return prefUser;
	}

	public void setPrefUser(UserEntity prefUser) {
		this.prefUser = prefUser;
	}

	public Integer getUidNumber() {
		return uidNumber;
	}

	public void setUidNumber(Integer uidNumber) {
		this.uidNumber = uidNumber;
	}
}