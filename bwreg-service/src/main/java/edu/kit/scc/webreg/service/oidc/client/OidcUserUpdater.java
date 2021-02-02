package edu.kit.scc.webreg.service.oidc.client;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;

import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import edu.kit.scc.webreg.audit.Auditor;
import edu.kit.scc.webreg.audit.RegistryAuditor;
import edu.kit.scc.webreg.audit.UserUpdateAuditor;
import edu.kit.scc.webreg.bootstrap.ApplicationConfig;
import edu.kit.scc.webreg.dao.RegistryDao;
import edu.kit.scc.webreg.dao.as.ASUserAttrDao;
import edu.kit.scc.webreg.dao.audit.AuditDetailDao;
import edu.kit.scc.webreg.dao.audit.AuditEntryDao;
import edu.kit.scc.webreg.dao.oidc.OidcUserDao;
import edu.kit.scc.webreg.entity.EventType;
import edu.kit.scc.webreg.entity.RegistryEntity;
import edu.kit.scc.webreg.entity.RegistryStatus;
import edu.kit.scc.webreg.entity.ServiceEntity;
import edu.kit.scc.webreg.entity.UserEntity;
import edu.kit.scc.webreg.entity.UserStatus;
import edu.kit.scc.webreg.entity.as.ASUserAttrEntity;
import edu.kit.scc.webreg.entity.as.AttributeSourceServiceEntity;
import edu.kit.scc.webreg.entity.audit.AuditStatus;
import edu.kit.scc.webreg.entity.oidc.OidcUserEntity;
import edu.kit.scc.webreg.event.EventSubmitter;
import edu.kit.scc.webreg.event.UserEvent;
import edu.kit.scc.webreg.exc.EventSubmitException;
import edu.kit.scc.webreg.exc.RegisterException;
import edu.kit.scc.webreg.exc.UserUpdateException;
import edu.kit.scc.webreg.service.SerialService;
import edu.kit.scc.webreg.service.ServiceService;
import edu.kit.scc.webreg.service.UserServiceHook;
import edu.kit.scc.webreg.service.impl.AttributeMapHelper;
import edu.kit.scc.webreg.service.impl.HookManager;
import edu.kit.scc.webreg.service.reg.AttributeSourceQueryService;
import edu.kit.scc.webreg.service.reg.impl.Registrator;

@ApplicationScoped
public class OidcUserUpdater implements Serializable {

	private static final long serialVersionUID = 1L;

	@Inject
	private Logger logger;
	
	@Inject
	private AuditEntryDao auditDao;

	@Inject
	private AuditDetailDao auditDetailDao;

	@Inject
	private OidcUserDao userDao;

	@Inject
	private ServiceService serviceService;
	
	@Inject
	private RegistryDao registryDao;
	
	@Inject
	private SerialService serialService;
	
	@Inject
	private HookManager hookManager;
	
	@Inject
	private ASUserAttrDao asUserAttrDao;
	
	@Inject
	private AttributeSourceQueryService attributeSourceQueryService;
	
	@Inject
	private EventSubmitter eventSubmitter;

	@Inject
	private ApplicationConfig appConfig;

	@Inject
	private Registrator registrator;
	
	@Inject
	private AttributeMapHelper attrHelper;

	@Inject
	private OidcTokenHelper oidcTokenHelper;
	
	public OidcUserEntity updateUserFromOP(OidcUserEntity user, String executor) {
		user = userDao.merge(user);
		
		/**
		 * TODO Implement refresh here
		 */
		
		
		user.setScheduledUpdate(getNextScheduledUpdate());
		
		return user;
	}
	
	public OidcUserEntity updateUser(OidcUserEntity user, Map<String, List<Object>> attributeMap, String executor)
			throws UserUpdateException {
		return updateUser(user, attributeMap, executor, null);
	}
	
	public OidcUserEntity updateUser(OidcUserEntity user, Map<String, List<Object>> attributeMap, String executor, 
			ServiceEntity service)
			throws UserUpdateException {
		MDC.put("userId", "" + user.getId());
		logger.debug("Updating user {}", user.getEppn());

		user = userDao.merge(user);
		
		boolean changed = false;
		
		UserUpdateAuditor auditor = new UserUpdateAuditor(auditDao, auditDetailDao, appConfig);
		auditor.startAuditTrail(executor);
		auditor.setName(getClass().getName() + "-UserUpdate-Audit");
		auditor.setDetail("Update OIDC user " + user.getSubjectId());
	
		// List to store parent services, that are not registered. Need to be registered
		// later, when attribute map is populated
		List<ServiceEntity> delayedRegisterList = new ArrayList<ServiceEntity>();
		
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
				changeUserStatus(user, UserStatus.ON_HOLD, auditor);

				/*
				 * Also flag all registries for user ON_HOLD
				 */
				List<RegistryEntity> registryList = registryDao.findByUserAndStatus(user, 
						RegistryStatus.ACTIVE, RegistryStatus.LOST_ACCESS, RegistryStatus.INVALID);
				for (RegistryEntity registry : registryList) {
					changeRegistryStatus(registry, RegistryStatus.ON_HOLD, auditor);
				}
			}
		}
		else {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			user.getGenericStore().put("no_assertion_count", "0");
			user.getGenericStore().put("last_valid_assertion", df.format(new Date()));
		
			changed |= updateUserFromAttribute(user, attributeMap, auditor);
			
			if (UserStatus.ON_HOLD.equals(user.getUserStatus())) {
				changeUserStatus(user, UserStatus.ACTIVE, auditor);
				
				/*
				 * Also reenable all registries for user to LOST_ACCESS. 
				 * They are rechecked then
				 */
				List<RegistryEntity> registryList = registryDao.findByUserAndStatus(user, 
						RegistryStatus.ON_HOLD);
				for (RegistryEntity registry : registryList) {
					changeRegistryStatus(registry, RegistryStatus.LOST_ACCESS, auditor);
					
					/*
					 * check if parent registry is missing
					 */
					if (registry.getService().getParentService() != null) {
						List<RegistryEntity> parentRegistryList = registryDao.findByServiceAndIdentityAndNotStatus(
								registry.getService().getParentService(), user.getIdentity(), 
								RegistryStatus.DELETED, RegistryStatus.DEPROVISIONED);
						if (parentRegistryList.size() == 0) {
							delayedRegisterList.add(registry.getService().getParentService());
						}
					}
				}
				
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
			
			Map<String, String> attributeStore = user.getAttributeStore();
			attributeStore.clear();
			for (Entry<String, List<Object>> entry : attributeMap.entrySet()) {
				attributeStore.put(entry.getKey(), attrHelper.attributeListToString(entry.getValue()));
			}
		}

		for (ServiceEntity delayedService : delayedRegisterList) {
			try {
				registrator.registerUser(user, delayedService, 
						"user-" + user.getId(), false);
			} catch (RegisterException e) {
				logger.warn("Parent registrytion didn't work out like it should", e);
			}
		}
		
		user.setLastUpdate(new Date());
		user.setLastFailedUpdate(null);
		user.setScheduledUpdate(getNextScheduledUpdate());
		
		if (changed) {
			fireUserChangeEvent(user, auditor.getActualExecutor(), auditor);
		}
		
		auditor.setUser(user);
		auditor.finishAuditTrail();
		auditor.commitAuditTrail();
		
		return user;
	}
	
	public OidcUserEntity updateUser(OidcUserEntity user, IDTokenClaimsSet claims, UserInfo userInfo, String executor, ServiceEntity service)
			throws UserUpdateException {

		Map<String, List<Object>> attributeMap = oidcTokenHelper.convertToAttributeMap(claims, userInfo);

		if (service != null)
			return updateUser(user, attributeMap, executor, service);
		else
			return updateUser(user, attributeMap, executor);
	}

	public OidcUserEntity updateUser(OidcUserEntity user, IDTokenClaimsSet claims, UserInfo userInfo, String executor)
			throws UserUpdateException {
		
		return updateUser(user, claims, userInfo, executor, null);
	}
	
	protected void fireUserChangeEvent(UserEntity user, String executor, Auditor auditor) {
		
		UserEvent userEvent = new UserEvent(user, auditor.getAudit());
		
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
			IDTokenClaimsSet claims = oidcTokenHelper.claimsFromMap(attributeMap);
			if (claims == null) { 
				throw new UserUpdateException("ID claims are missing in session");
			}

			UserInfo userInfo = oidcTokenHelper.userInfoFromMap(attributeMap);
			if (userInfo == null) { 
				throw new UserUpdateException("User info is missing in session");
			}

			changed |= compareAndChangeProperty(user, "email", userInfo.getEmailAddress(), auditor);
			changed |= compareAndChangeProperty(user, "eppn", userInfo.getStringClaim("eduPersonPrincipalName"), auditor);
			changed |= compareAndChangeProperty(user, "givenName", userInfo.getGivenName(), auditor);
			changed |= compareAndChangeProperty(user, "surName", userInfo.getFamilyName(), auditor);

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

	
	private boolean compareAndChangeProperty(UserEntity user, String property, String value, Auditor auditor) {
		String s = null;
		String action = null;
		
		try {
			Object actualValue = PropertyUtils.getProperty(user, property);

			if (actualValue != null && actualValue.equals(value)) {
				// Value didn't change, do nothing
				return false;
			}
			
			if (actualValue == null && value == null) {
				// Value stayed null
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
	
	protected void changeUserStatus(UserEntity user, UserStatus toStatus, Auditor auditor) {
		UserStatus fromStatus = user.getUserStatus();
		user.setUserStatus(toStatus);
		user.setLastStatusChange(new Date());
		
		logger.debug("{}: change user status from {} to {}", user.getEppn(), fromStatus, toStatus);
		auditor.logAction(user.getEppn(), "CHANGE STATUS", fromStatus + " -> " + toStatus, 
				"Change status " + fromStatus + " -> " + toStatus, AuditStatus.SUCCESS);
	}
	
	protected void changeRegistryStatus(RegistryEntity registry, RegistryStatus toStatus, Auditor parentAuditor) {
		RegistryStatus fromStatus = registry.getRegistryStatus();
		registry.setRegistryStatus(toStatus);
		registry.setLastStatusChange(new Date());

		logger.debug("{} {} {}: change registry status from {} to {}", new Object[] { 
				registry.getUser().getEppn(), registry.getService().getShortName(), registry.getId(), fromStatus, toStatus });
		RegistryAuditor registryAuditor = new RegistryAuditor(auditDao, auditDetailDao, appConfig);
		registryAuditor.setParent(parentAuditor);
		registryAuditor.startAuditTrail(parentAuditor.getActualExecutor());
		registryAuditor.setName(getClass().getName() + "-UserUpdate-Registry-Audit");
		registryAuditor.setDetail("Update registry " + registry.getId() + " for user " + registry.getUser().getEppn());
		registryAuditor.setRegistry(registry);
		registryAuditor.logAction(registry.getUser().getEppn(), "CHANGE STATUS", "registry-" + registry.getId(), 
				"Change status " + fromStatus + " -> " + toStatus, AuditStatus.SUCCESS);
		registryAuditor.finishAuditTrail();
	}
	
	private Date getNextScheduledUpdate() {
		Long futureMillis = 30L * 24L * 60L * 60L * 1000L;
		if (appConfig.getConfigOptions().containsKey("update_schedule_future")) {
			futureMillis = Long.decode(appConfig.getConfigValue("update_schedule_future"));
		}
		Integer futureMillisRandom = 6 * 60 * 60 * 1000;
		if (appConfig.getConfigOptions().containsKey("update_schedule_future_random")) {
			futureMillisRandom = Integer.decode(appConfig.getConfigValue("update_schedule_future_random"));
		}
		Random r = new Random();
		return new Date(System.currentTimeMillis() + futureMillis + r.nextInt(futureMillisRandom));
	}	
}
