/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.ppaas.rest.endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.exception.PersistenceManagerException;
import org.apache.stratos.manager.lookup.LookupDataHolder;
import org.apache.stratos.manager.persistence.PersistenceManager;
import org.apache.stratos.manager.persistence.RegistryBasedPersistenceManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;

import java.util.Collection;
import java.util.Set;

public class DataInsertionAndRetrievalManager {

    private static final Log log = LogFactory.getLog(DataInsertionAndRetrievalManager.class);

    // TODO: use a global object
    private static PersistenceManager persistenceManager = new RegistryBasedPersistenceManager();

    public void cacheAndPersistSubcription(CartridgeSubscription cartridgeSubscription)
            throws PersistenceManagerException {

        // get the write lock
        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            // store in LookupDataHolder
            LookupDataHolder.getInstance().putSubscription(cartridgeSubscription);

            try {
                // store in Persistence Manager
                persistenceManager.persistCartridgeSubscription(cartridgeSubscription);

            }
            catch (PersistenceManagerException e) {
                String errorMsg = "Error in persisting CartridgeSubscription in Persistence Manager";
                log.error(errorMsg, e);
                // remove from the in memory model since persisting failed
                LookupDataHolder.getInstance().removeSubscription(cartridgeSubscription.getSubscriber().getTenantId(),
                        cartridgeSubscription.getType(),
                        cartridgeSubscription.getAlias(), cartridgeSubscription.getClusterDomain(),
                        cartridgeSubscription.getRepository() != null ? cartridgeSubscription.getRepository().getUrl() :
                                null);

                throw e;
            }

        }
        finally {
            // release the write lock
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    public void cacheAndUpdateSubscription(CartridgeSubscription cartridgeSubscription)
            throws PersistenceManagerException {

        // get the write lock
        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            // store in LookupDataHolder
            LookupDataHolder.getInstance().putSubscription(cartridgeSubscription);

            try {
                // store in Persistence Manager
                persistenceManager.persistCartridgeSubscription(cartridgeSubscription);

            }
            catch (PersistenceManagerException e) {
                String errorMsg = "Error in updating cartridge subscription in persistence manager";
                log.error(errorMsg, e);
                throw e;
            }

        }
        finally {
            // release the write lock
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    public void removeSubscription(int tenantId, String subscriptionAlias) throws PersistenceManagerException {

        CartridgeSubscription cartridgeSubscription = getCartridgeSubscription(tenantId, subscriptionAlias);

        if (cartridgeSubscription == null) {
            if (log.isDebugEnabled()) {
                log.debug("No CartridgeSubscription found for tenant " + tenantId + ", subscription alias " +
                        subscriptionAlias);
            }
            return;
        }

        String cartridgeType = cartridgeSubscription.getType();
        String clusterId = cartridgeSubscription.getClusterDomain();

        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            // remove from persistence manager
            try {
                persistenceManager.removeCartridgeSubscription(tenantId, cartridgeType, subscriptionAlias);

            }
            catch (PersistenceManagerException e) {
                String errorMsg = "Error in removing CartridgeSubscription from Persistence Manager";
                log.error(errorMsg, e);
                throw e;
            }

            // remove from cache
            LookupDataHolder.getInstance().removeSubscription(tenantId, cartridgeType, subscriptionAlias, clusterId,
                    cartridgeSubscription.getRepository() != null ? cartridgeSubscription.getRepository().getUrl() :
                            null);

        }
        finally {
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    public void cachePersistedSubscriptions() throws PersistenceManagerException {

        Collection<CartridgeSubscription> cartridgeSubscriptions;

        // get the write lock
        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            try {
                cartridgeSubscriptions = persistenceManager.getCartridgeSubscriptions();

            }
            catch (PersistenceManagerException e) {
                String errorMsg = "Error in retrieving CartridgeSubscriptions from Persistence Manager";
                log.error(errorMsg, e);
                throw e;
            }

            if (cartridgeSubscriptions == null || cartridgeSubscriptions.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("No CartridgeSubscriptions found to add to the cache");
                }
                return;
            }
            cacheSubscriptions(cartridgeSubscriptions);

        }
        finally {
            // release the write lock
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    public void cachePersistedSubscriptions(int tenantId) throws PersistenceManagerException {

        Collection<CartridgeSubscription> cartridgeSubscriptions;

        // get the write lock
        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            try {
                cartridgeSubscriptions = persistenceManager.getCartridgeSubscriptions(tenantId);

            }
            catch (PersistenceManagerException e) {
                String errorMsg = "Error in retrieving CartridgeSubscriptions from Persistence Manager";
                log.error(errorMsg, e);
                throw e;
            }

            if (cartridgeSubscriptions == null || cartridgeSubscriptions.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("No CartridgeSubscriptions found to add to the cache");
                }
                return;
            }
            cacheSubscriptions(cartridgeSubscriptions);

        }
        finally {
            // release the write lock
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    public void cacheSubscriptionsWithoutPersisting(Collection<CartridgeSubscription> cartridgeSubscriptions) {

        // get the write lock
        LookupDataHolder.getInstance().acquireWriteLock();

        try {
            cacheSubscriptions(cartridgeSubscriptions);

        }
        finally {
            // release the write lock
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    public void removeSubscriptionFromCache(int tenantId, String subscriptionAlias) {

        LookupDataHolder.getInstance().acquireWriteLock();

        CartridgeSubscription cartridgeSubscription = getCartridgeSubscription(tenantId, subscriptionAlias);
        if (cartridgeSubscription == null) {
            if (log.isDebugEnabled()) {
                log.debug("No CartridgeSubscription found for tenant " + tenantId + ", subscription alias " +
                        subscriptionAlias);
            }
            return;
        }

        String cartridgeType = cartridgeSubscription.getType();
        String clusterId = cartridgeSubscription.getClusterDomain();

        try {
            // remove from cache
            LookupDataHolder.getInstance().removeSubscription(tenantId, cartridgeType, subscriptionAlias, clusterId,
                    cartridgeSubscription.getRepository() != null ? cartridgeSubscription.getRepository().getUrl() :
                            null);

        }
        finally {
            LookupDataHolder.getInstance().releaseWriteLock();
        }
    }

    private void cacheSubscriptions(Collection<CartridgeSubscription> cartridgeSubscriptions) {

        // cache all
        for (CartridgeSubscription cartridgeSubscription : cartridgeSubscriptions) {
            LookupDataHolder.getInstance().putSubscription(cartridgeSubscription);
            if (log.isDebugEnabled()) {
                log.debug("Updated the in memory cache with the CartridgeSubscription: " +
                        cartridgeSubscription.toString());
            }
        }
    }

    public void persistService(Service service) throws PersistenceManagerException {

        persistenceManager.persistService(service);
    }

    public Collection<Service> getServices() throws PersistenceManagerException {

        return persistenceManager.getServices();
    }

    public Service getService(String cartridgeType) throws PersistenceManagerException {

        return persistenceManager.getService(cartridgeType);
    }

    public void removeService(String cartridgeType) throws PersistenceManagerException {

        persistenceManager.removeService(cartridgeType);
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions(String cartridgeType) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptions(cartridgeType);

        }
        finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public CartridgeSubscription getCartridgeSubscription(int tenantId, String subscriptionAlias) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptionForAlias(tenantId, subscriptionAlias);

        }
        finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public Set<CartridgeSubscription> getCartridgeSubscriptionForCluster(String clusterId) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscription(clusterId);

        }
        finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public Set<CartridgeSubscription> getCartridgeSubscriptionForRepository(String repoUrl) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptionsForRepoUrl(repoUrl);

        }
        finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions(int tenantId) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptions(tenantId);

        }
        finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public Collection<CartridgeSubscription> getCartridgeSubscriptions(int tenantId, String cartridgeType) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptionForType(tenantId, cartridgeType);

        }
        finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    //Don't use this method unless absolutely necessary, use getCartridgeSubscription (int tenantId, String subscriptionAlias)
    public CartridgeSubscription getCartridgeSubscriptionForAlias(String subscriptionAlias) {

        // acquire read lock
        LookupDataHolder.getInstance().acquireReadLock();

        try {
            return LookupDataHolder.getInstance().getSubscriptionForAlias(subscriptionAlias);

        }
        finally {
            // release read lock
            LookupDataHolder.getInstance().releaseReadLock();
        }
    }

    public static PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public static void setPersistenceManager(PersistenceManager persistenceManager) {
        DataInsertionAndRetrievalManager.persistenceManager = persistenceManager;
    }
}
