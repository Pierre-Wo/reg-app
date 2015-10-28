package edu.kit.scc.webreg.service.impl;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.beanutils.PropertyUtils;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.ws.soap.common.SOAPException;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.security.SecurityException;
import org.slf4j.Logger;

import edu.kit.scc.webreg.audit.Auditor;
import edu.kit.scc.webreg.audit.UserUpdateAuditor;
import edu.kit.scc.webreg.bootstrap.ApplicationConfig;
import edu.kit.scc.webreg.dao.AuditDetailDao;
import edu.kit.scc.webreg.dao.AuditEntryDao;
import edu.kit.scc.webreg.dao.SamlIdpMetadataDao;
import edu.kit.scc.webreg.dao.SamlSpConfigurationDao;
import edu.kit.scc.webreg.dao.UserDao;
import edu.kit.scc.webreg.dao.as.ASUserAttrDao;
import edu.kit.scc.webreg.entity.AuditStatus;
import edu.kit.scc.webreg.entity.EventType;
import edu.kit.scc.webreg.entity.GroupEntity;
import edu.kit.scc.webreg.entity.SamlIdpMetadataEntity;
import edu.kit.scc.webreg.entity.SamlSpConfigurationEntity;
import edu.kit.scc.webreg.entity.ServiceEntity;
import edu.kit.scc.webreg.entity.UserEntity;
import edu.kit.scc.webreg.entity.UserStatus;
import edu.kit.scc.webreg.entity.as.ASUserAttrEntity;
import edu.kit.scc.webreg.entity.as.AttributeSourceServiceEntity;
import edu.kit.scc.webreg.event.EventSubmitter;
import edu.kit.scc.webreg.event.UserEvent;
import edu.kit.scc.webreg.exc.EventSubmitException;
import edu.kit.scc.webreg.exc.MetadataException;
import edu.kit.scc.webreg.exc.NoAssertionException;
import edu.kit.scc.webreg.exc.SamlAuthenticationException;
import edu.kit.scc.webreg.exc.UserUpdateException;
import edu.kit.scc.webreg.service.SerialService;
import edu.kit.scc.webreg.service.ServiceService;
import edu.kit.scc.webreg.service.UserServiceHook;
import edu.kit.scc.webreg.service.reg.AttributeSourceQueryService;
import edu.kit.scc.webreg.service.saml.AttributeQueryHelper;
import edu.kit.scc.webreg.service.saml.Saml2AssertionService;
import edu.kit.scc.webreg.service.saml.SamlHelper;

@ApplicationScoped
public class UserUpdater implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private Logger logger;
	
	@Inject
	private AuditEntryDao auditDao;

	@Inject
	private AuditDetailDao auditDetailDao;

	@Inject
	private Saml2AssertionService saml2AssertionService;

	@Inject
	private AttributeQueryHelper attrQueryHelper;
	
	@Inject
	private UserDao userDao;

	@Inject
	private ServiceService serviceService;
	
	@Inject
	private HomeOrgGroupUpdater homeOrgGroupUpdater;

	@Inject
	private SamlHelper samlHelper;
		
	@Inject 
	private SamlIdpMetadataDao idpDao;
	
	@Inject 
	private SamlSpConfigurationDao spDao;

	@Inject
	private SerialService serialService;
	
	@Inject
	private HookManager hookManager;
	
	@Inject
	private ASUserAttrDao asUserAttrDao;
	
	@Inject
	private AttributeSourceQueryService attributeSourceQueryService;
	
	@Inject
	private AttributeMapHelper attrHelper;

	@Inject
	private EventSubmitter eventSubmitter;

	@Inject
	private ApplicationConfig appConfig;
	
	public UserEntity updateUser(UserEntity user, Map<String, List<Object>> attributeMap, String executor)
			throws UserUpdateException {
		return updateUser(user, attributeMap, executor, null);
	}
	
	public UserEntity updateUser(UserEntity user, Map<String, List<Object>> attributeMap, String executor, 
			ServiceEntity service)
			throws UserUpdateException {
		logger.debug("Updating user {}", user.getEppn());

		user = userDao.merge(user);
		
		boolean changed = false;
		
		UserUpdateAuditor auditor = new UserUpdateAuditor(auditDao, auditDetailDao, appConfig);
		auditor.startAuditTrail(executor);
		auditor.setName(getClass().getName() + "-UserUpdate-Audit");
		auditor.setDetail("Update user " + user.getEppn());
	
		/**
		 * put no_assertion_count in generic store if assertion is missing. Else
		 * reset no assertion count and put last valid assertion date in
		 */
		if (attributeMap == null) {
			if (! user.getGenericStore().containsKey("no_assertion_count")) {
				user.getGenericStore().put("no_assertion_count", "1");
			}
			else {
				user.getGenericStore().put("no_assertion_count", 
						"" + (Long.parseLong(user.getGenericStore().get("no_assertion_count")) + 1L));
			}
			
			logger.info("No attribute for user {}, skipping updateFromAttribute", user.getEppn());
			
			user.getAttributeStore().clear();

			if (UserStatus.ACTIVE.equals(user.getUserStatus())) {
				user.setUserStatus(UserStatus.ON_HOLD);
				user.setLastStatusChange(new Date());
			}
		}
		else {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			user.getGenericStore().put("no_assertion_count", "0");
			user.getGenericStore().put("last_valid_assertion", df.format(new Date()));
		
			changed |= updateUserFromAttribute(user, attributeMap, auditor);
			
			if (UserStatus.ON_HOLD.equals(user.getUserStatus())) {
				user.setUserStatus(UserStatus.ACTIVE);
				user.setLastStatusChange(new Date());
				
				/*
				 * fire a user changed event to be sure, when the user is activated
				 */
				changed = true;
			}

			/*
			 * if service is set, update only attribute sources spcific for this 
			 * service. Else update all (login via web or generic attribute query)
			 */
			if (service != null) {
				service = serviceService.findByIdWithAttrs(service.getId(), "attributeSourceService");
				
				for (AttributeSourceServiceEntity asse : service.getAttributeSourceService()) {
					changed |= attributeSourceQueryService.updateUserAttributes(user, asse.getAttributeSource(), executor);
				}
			}
			else {
				List<ASUserAttrEntity> asUserAttrList = asUserAttrDao.findForUser(user);
				for (ASUserAttrEntity asUserAttr : asUserAttrList) {
					changed |= attributeSourceQueryService.updateUserAttributes(user, asUserAttr.getAttributeSource(), executor);
				}
			}
			
			Set<GroupEntity> changedGroups = homeOrgGroupUpdater.updateGroupsForUser(user, attributeMap, auditor);

			if (changedGroups.size() > 0) {
				changed = true;
			}
			
			Map<String, String> attributeStore = user.getAttributeStore();
			attributeStore.clear();
			for (Entry<String, List<Object>> entry : attributeMap.entrySet()) {
				attributeStore.put(entry.getKey(), attrHelper.attributeListToString(entry.getValue()));
			}
		}
		
		user.setLastUpdate(new Date());
		user.setLastFailedUpdate(null);

		if (changed) {
			fireUserChangeEvent(user, auditor.getActualExecutor());
		}
		
		auditor.setUser(user);
		auditor.finishAuditTrail();
		
		return user;
	}
	
	public UserEntity updateUser(UserEntity user, Assertion assertion, String executor, ServiceEntity service)
			throws UserUpdateException {
		Map<String, List<Object>> attributeMap = saml2AssertionService.extractAttributes(assertion);

		return updateUser(user, attributeMap, executor, service);
	}

	public UserEntity updateUserFromIdp(UserEntity user) 
			throws UserUpdateException {
		return updateUserFromIdp(user, null);
	}
	
	public UserEntity updateUserFromIdp(UserEntity user, ServiceEntity service) 
			throws UserUpdateException {

		SamlSpConfigurationEntity spEntity = spDao.findByEntityId(user.getPersistentSpId());
		SamlIdpMetadataEntity idpEntity = idpDao.findByEntityId(user.getIdp().getEntityId());
		
		EntityDescriptor idpEntityDescriptor = samlHelper.unmarshal(
				idpEntity.getEntityDescriptor(), EntityDescriptor.class);
		
		Response samlResponse;
		try {
			/*
			 * If something goes wrong here, communication with the idp probably failed
			 */
			
			samlResponse = attrQueryHelper.query(user, idpEntity, idpEntityDescriptor, spEntity);
		} catch (SOAPException e) {
			/*
			 * This exception is thrown if the certificate chain is incomplete e.g.
			 */
			updateFail(user, e);
			throw new UserUpdateException(e);
		} catch (MetadataException e) {
			/*
			 * is thrown if AttributeQuery location is missing in metadata, or something is wrong
			 * with the sp certificate
			 */
			updateFail(user, e);
			throw new UserUpdateException(e);
		} catch (SecurityException e) {
			updateFail(user, e);
			throw new UserUpdateException(e);
		}

		try {
			/*
			 * Don't check Assertion Signature, because we are contacting the IDP directly
			 */
			Assertion assertion;
			try {
				assertion = saml2AssertionService.processSamlResponse(samlResponse, idpEntity, 
						idpEntityDescriptor, spEntity, false);
			} catch (NoAssertionException e) {
				if (user.getIdp() != null)
					logger.warn("No assertion delivered for user {} from idp {}", user.getEppn(), user.getIdp().getEntityId());
				else
					logger.warn("No assertion delivered for user {} from idp {}", user.getEppn());
				assertion = null;
			}

			return updateUser(user, assertion, "attribute-query", service);
		} catch (DecryptionException e) {
			updateFail(user, e);
			throw new UserUpdateException(e);
		} catch (IOException e) {
			updateFail(user, e);
			throw new UserUpdateException(e);
		} catch (SamlAuthenticationException e) {
			/*
			 * Thrown if i.e. the AttributeQuery profile is not configured correctly
			 */
			updateFail(user, e);
			throw new UserUpdateException(e);
		}
	}
	
	protected void updateFail(UserEntity user, Exception e) {
		user.setLastFailedUpdate(new Date());
		user.setGroups(null);
		user = userDao.persist(user);
	}

	protected void fireUserChangeEvent(UserEntity user, String executor) {
		
		UserEvent userEvent = new UserEvent(user);
		
		try {
			eventSubmitter.submit(userEvent, EventType.USER_UPDATE, executor);
		} catch (EventSubmitException e) {
			logger.warn("Could not submit event", e);
		}
	}

	public boolean updateUserFromAttribute(UserEntity user, Map<String, List<Object>> attributeMap, Auditor auditor) 
				throws UserUpdateException {
		return updateUserFromAttribute(user, attributeMap, false, auditor);
	}

	public boolean updateUserFromAttribute(UserEntity user, Map<String, List<Object>> attributeMap, boolean withoutUidNumber, Auditor auditor) 
				throws UserUpdateException {

		boolean changed = false;
		
		UserServiceHook completeOverrideHook = null;
		Set<UserServiceHook> activeHooks = new HashSet<UserServiceHook>();
		
		for (UserServiceHook hook : hookManager.getUserHooks()) {
			if (hook.isResponsible(user, attributeMap)) {
				
				hook.preUpdateUserFromAttribute(user, attributeMap, auditor);
				activeHooks.add(hook);
				
				if (hook.isCompleteOverride()) {
					completeOverrideHook = hook;
				}
			}
		}
		
		if (completeOverrideHook == null) {
			changed |= compareAndChangeProperty(user, "email", attributeMap.get("urn:oid:0.9.2342.19200300.100.1.3"), auditor);
			changed |= compareAndChangeProperty(user, "eppn", attributeMap.get("urn:oid:1.3.6.1.4.1.5923.1.1.1.6"), auditor);
			changed |= compareAndChangeProperty(user, "givenName", attributeMap.get("urn:oid:2.5.4.42"), auditor);
			changed |= compareAndChangeProperty(user, "surName", attributeMap.get("urn:oid:2.5.4.4"), auditor);

			List<String> emailList = attrHelper.attributeListToStringList(attributeMap, "urn:oid:0.9.2342.19200300.100.1.3");
			if (emailList != null && emailList.size() > 1) {
				
				if (user.getEmailAddresses() == null) {
					user.setEmailAddresses(new HashSet<String>());
				}
				
				for (int i=1; i<emailList.size(); i++) {
					user.getEmailAddresses().add(emailList.get(i));
				}
			}
			
			if ((! withoutUidNumber) && (user.getUidNumber() == null)) {
				user.setUidNumber(serialService.next("uid-number-serial").intValue());
				logger.info("Setting UID Number {} for user {}", user.getUidNumber(), user.getEppn());
				auditor.logAction(user.getEppn(), "SET FIELD", "uidNumber", "" + user.getUidNumber(), AuditStatus.SUCCESS);
				changed = true;
			}
		}
		else {
			logger.info("Overriding standard User Update Mechanism! Activator: {}", completeOverrideHook.getClass().getName());
		}
		
		for (UserServiceHook hook : activeHooks) {
			hook.postUpdateUserFromAttribute(user, attributeMap, auditor);
		}

		return changed;
	}

	
	private boolean compareAndChangeProperty(UserEntity user, String property, List<Object> objectValue, Auditor auditor) {
		String s = null;
		String action = null;
		
		// In case of a List (multiple SAML Values), take the first value
		String value = attrHelper.getSingleStringFirst(objectValue);
		
		try {
			Object actualValue = PropertyUtils.getProperty(user, property);

			if (actualValue != null && actualValue.equals(value)) {
				// Value didn't change, do nothing
				return false;
			}
			
			if (actualValue == null) {
				s = "null";
				action = "SET FIELD";
			}
			else {
				s = actualValue.toString();
				action = "UPDATE FIELD";
			}
			
			s = s + " -> " + value;
			if (s.length() > 1017) s = s.substring(0, 1017) + "...";
			
			PropertyUtils.setProperty(user, property, value);
			
			auditor.logAction(user.getEppn(), action, property, s, AuditStatus.SUCCESS);
		} catch (IllegalAccessException e) {
			logger.warn("This probably shouldn't happen: ", e);
			auditor.logAction(user.getEppn(), action, property, s, AuditStatus.FAIL);
		} catch (InvocationTargetException e) {
			logger.warn("This probably shouldn't happen: ", e);
			auditor.logAction(user.getEppn(), action, property, s, AuditStatus.FAIL);
		} catch (NoSuchMethodException e) {
			logger.warn("This probably shouldn't happen: ", e);
			auditor.logAction(user.getEppn(), action, property, s, AuditStatus.FAIL);
		}
		
		return true;
	}
}