/*******************************************************************************
 * Software Name : RCS IMS Stack
 * Version : 2.0.0
 * 
 * Copyright � 2010 France Telecom S.A.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.orangelabs.rcs.core.ims.service.presence;

import java.util.Vector;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipManager;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.SessionAuthenticationAgent;
import com.orangelabs.rcs.platform.registry.RegistryFactory;
import com.orangelabs.rcs.utils.PeriodicRefresher;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Publish manager for sending current user presence status
 *
 * @author JM. Auffret
 */
public class PublishManager extends PeriodicRefresher {

    /**
     * IMS module
     */
    private ImsModule imsModule;
    
	/**
     * Dialog path
     */
    private SipDialogPath dialogPath = null;

    /**
     * Expire period
     */
    private int expirePeriod;

	/**
     * Entity tag
     */
    private String entityTag = null;

    /**
     * Published flag
     */
    private boolean published = false;
    
	/**
	 * Authentication agent
	 */
	private SessionAuthenticationAgent authenticationAgent = new SessionAuthenticationAgent();

	/**
     * The log4j logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param defaultExpirePeriod Default expiration period in seconds
     * @param geoloc Geoloc option
     */
    public PublishManager(ImsModule parent, int defaultExpirePeriod, boolean geoloc) {
        this.imsModule = parent;
        this.expirePeriod = defaultExpirePeriod;

		// Restore the last SIP-ETag from the registry
        readEntityTag();
    	
        if (logger.isActivated()) {
        	logger.info("Publish manager started");
        }
    }

    /**
     * Is published
     * 
     * @return Return True if the terminal has published, else return False
     */
    public boolean isPublished() {
        return published;
    }

    /**
     * Terminate manager
     */
    public void terminate() {
    	if (logger.isActivated()) {
    		logger.info("Terminate the publish manager");
    	}
    	
    	// Do not unpublish for RCS, just stop timer
    	if (published) {
	    	// Stop timer
	    	stopTimer();
	    	published = false;
    	}
    	
        if (logger.isActivated()) {
        	logger.info("Publish manager is terminated");
        }
    }

    /**
     * Publish refresh processing
     */
    public void periodicProcessing() {
        // Make a publish
    	if (logger.isActivated()) {
    		logger.info("Execute re-publish");
    	}

    	try {
	        // Create a new dialog path for each publish
	        dialogPath = createDialogPath();
	        
	        // Send a refresh PUBLISH with no SDP and expire period 
	        SipRequest publish = SipMessageFactory.createPublish(createDialogPath(),
	        		expirePeriod,
	        		entityTag,
	        		imsModule.getCurrentNetworkInterface().getAccessInfo(),
	        		null);
	        sendPublish(publish);
        } catch (Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Publish has failed", e);
        	}
        	handleError(new PresenceError(PresenceError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }
    
    /**
     * Publish presence status
     * 
     * @param info Presence info
     * @return Boolean
     */
    public synchronized boolean publish(String info) {
        try {
	        // Create a new dialog path for each publish
	        dialogPath = createDialogPath();

			// Set the local SDP part in the dialog path
	    	dialogPath.setLocalSdp(info);
	    	
	    	// Send a PUBLISH 
            SipRequest publish = SipMessageFactory.createPublish(dialogPath,
            		expirePeriod,
            		entityTag,
            		imsModule.getCurrentNetworkInterface().getAccessInfo(),
            		info);
	        sendPublish(publish);
	
	    	// If publish is successful
	    	if (published) {
	        	if (logger.isActivated()) {
	        		logger.debug("Publish successful");
	        	}
	
	        	// Restart the periodic refresh
	            startTimer(expirePeriod, 0.8);
	    	} else {
	        	if (logger.isActivated()) {
	        		logger.debug("Publish has failed");
	        	}	    		
	    	}
        } catch (Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Publish has failed", e);
        	}
        	handleError(new PresenceError(PresenceError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
        return published;
    }

    /**
     * Unpublish
     */
    public synchronized void unPublish() {
    	if (!published) {
			// Already unpublished
			return;
    	}    	

    	try {
	        // Stop periodic publish
	        stopTimer();

	        // Create a new dialog path for each publish
	        dialogPath = createDialogPath();
	        
	        // Send a refresh PUBLISH with no SDP and expire period 
	        SipRequest publish = SipMessageFactory.createPublish(dialogPath,
	        		0,
	        		entityTag,
	        		imsModule.getCurrentNetworkInterface().getAccessInfo(),
	        		null);
	    	 sendPublish(publish);
	    	 
	        // Force publish flag to false
	        published = false;

	        // Notify listener				
	        imsModule.getCore().getListener().handlePublishPresenceTerminated();
        } catch (Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Publish has failed", e);
        	}
        	handleError(new PresenceError(PresenceError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }
    
	/**
	 * Send PUBLISH message
	 * 
	 * @param publish SIP PUBLISH
	 * @throws Exception
	 */
	private void sendPublish(SipRequest publish) throws Exception {
        // Send a PUBLISH
        if (logger.isActivated()) {
        	logger.info("Send PUBLISH, expire=" + publish.getExpires());
        }

        if (published) {
	        // Set the Authorization header
            authenticationAgent.setProxyAuthorizationHeader(publish);
        }
        
        // Send message
        SipTransactionContext ctx = imsModule.getSipManager().sendSipMessageAndWait(publish);

        // Wait response
        if (logger.isActivated()) {
        	logger.info("Wait response");
        }
        ctx.waitResponse(SipManager.TIMEOUT);
        
        // Analyze the received response 
        if (ctx.isSipResponse()) {
        	// A response has been received
            if (ctx.getStatusCode() == 200) {
            	// 200 OK
        		if (publish.getExpires() != 0) {
        			handle200OK(ctx);
        		} else {
        			handle200OkUnpublish(ctx);
        		}
            } else
            if (ctx.getStatusCode() == 407) {
            	// 407 Proxy Authentication Required
            	handle407Authentication(ctx);
            } else
            if (ctx.getStatusCode() == 412) {
            	// 412 Error
            	handle412ConditionalRequestFailed(ctx);
            } else
            if (ctx.getStatusCode() == 423) {
            	// 423 Interval Too Brief
            	handle423IntervalTooBrief(ctx);
            } else {
            	// Other error response
    			handleError(new PresenceError(PresenceError.PUBLISH_FAILED, ctx.getReasonPhrase()));    					
            }
        } else {
    		if (logger.isActivated()) {
        		logger.debug("No response received for PUBLISH");
        	}

    		// No response received: timeout
        	handleError(new PresenceError(PresenceError.PUBLISH_FAILED));
        }
	}    

	/**
	 * Handle 200 0K response 
	 * 
	 * @param ctx SIP transaction context
	 */
	private void handle200OK(SipTransactionContext ctx) {
        // 200 OK response received
    	if (logger.isActivated()) {
    		logger.info("200 OK response received");
    	}
    	published = true;

    	SipResponse resp = ctx.getSipResponse();

    	// Set the Proxy-Authorization header
    	authenticationAgent.readProxyAuthenticateHeader(resp);

        // Retrieve the expire value in the response
        retrieveExpirePeriod(resp);
        
    	// Retrieve the entity tag in the response
    	saveEntityTag(resp.getHeader("SIP-ETag"));
    	
    	// Notify listener				
        imsModule.getCore().getListener().handlePublishPresenceSuccessful();                
	}	
	
	/**
	 * Handle 200 0K response of UNPUBLISH
	 * 
	 * @param ctx SIP transaction context
	 */
	private void handle200OkUnpublish(SipTransactionContext ctx) {
        // 20x response received
        if (logger.isActivated()) {
            logger.info("20x response received");
        }

    	SipResponse resp = ctx.getSipResponse();
    	
    	// Retrieve the entity tag in the response
    	saveEntityTag(resp.getHeader("SIP-ETag"));
    	
    	// Notify listener				
        imsModule.getCore().getListener().handlePublishPresenceTerminated();                
	}
	
	/**
	 * Handle 407 response 
	 * 
	 * @param ctx SIP transaction context
	 * @throws Exception
	 */
	private void handle407Authentication(SipTransactionContext ctx) throws Exception {
        // 407 response received
    	if (logger.isActivated()) {
    		logger.info("407 response received");
    	}

    	SipResponse resp = ctx.getSipResponse();

    	// Set the Proxy-Authorization header
    	authenticationAgent.readProxyAuthenticateHeader(resp);

        // Increment the Cseq number of the dialog path
        dialogPath.incrementCseq();

        // Send a second PUBLISH with the right token
        if (logger.isActivated()) {
        	logger.info("Send second PUBLISH");
        }
    	SipRequest publish = SipMessageFactory.createPublish(dialogPath,
    			ctx.getMessageSent().getExpires(),
        		entityTag,
        		imsModule.getCurrentNetworkInterface().getAccessInfo(),
        		dialogPath.getLocalSdp());
    	
        // Set the Authorization header
        authenticationAgent.setProxyAuthorizationHeader(publish);
    	
        // Send message
    	sendPublish(publish);
	}	

	/**
	 * Handle 412 response 
	 * 
	 * @param ctx SIP transaction context
	 */
	private void handle412ConditionalRequestFailed(SipTransactionContext ctx) throws Exception {
		// 412 response received
    	if (logger.isActivated()) {
    		logger.info("412 conditional response received");
    	}

        // Increment the Cseq number of the dialog path
        dialogPath.incrementCseq();

        // Reset Sip-Etag
        saveEntityTag(null);

        // Retry to PUBLISH again without ETag 
        SipRequest publish = SipMessageFactory.createPublish(dialogPath,
        		expirePeriod,
        		entityTag,
        		imsModule.getCurrentNetworkInterface().getAccessInfo(),
        		dialogPath.getLocalSdp());

        // Send message
        sendPublish(publish);        
	}	
	
	/**
	 * Handle 423 response 
	 * 
	 * @param ctx SIP transaction context
	 */
	private void handle423IntervalTooBrief(SipTransactionContext ctx) throws Exception {
		// 423 response received
    	if (logger.isActivated()) {
    		logger.info("423 interval too brief response received");
    	}

    	SipResponse resp = ctx.getSipResponse();

    	// Increment the Cseq number of the dialog path
        dialogPath.incrementCseq();

        // Extract the Min-Expire value
        int minExpire = SipUtils.extractMinExpiresPeriod(resp);
        if (minExpire == -1) {
            if (logger.isActivated()) {
            	logger.error("Can't read the Min-Expires value");
            }
        	handleError(new PresenceError(PresenceError.PUBLISH_FAILED, "No Min-Epires value found"));
        	return;
        }
        
        // Set the default expire value
    	expirePeriod = minExpire;
    	
        // Send a new PUBLISH with the right expire period
        SipRequest publish = SipMessageFactory.createPublish(dialogPath,
        		expirePeriod,
        		entityTag,
        		imsModule.getCurrentNetworkInterface().getAccessInfo(),
        		dialogPath.getLocalSdp());

        // Send message
        sendPublish(publish);        
	}	
	
	/**
	 * Handle error response 
	 * 
	 * @param error Error
	 */
	private void handleError(PresenceError error) {
        // Error
    	if (logger.isActivated()) {
    		logger.info("Publish has failed: " + error.getErrorCode() + ", reason=" + error.getMessage());
    	}
        published = false;
        
        // Publish has failed, stop the periodic publish
		stopTimer();
        
        // Error
        if (logger.isActivated()) {
        	logger.info("Publish has failed");
        }
        
        // Notify listener				
        imsModule.getCore().getListener().handlePublishPresenceFailed(error);
	}

    /**
     * Retrieve the expire period in the contact header or in the expire header
     * 
     * @param response SIP response
     */
    private void retrieveExpirePeriod(SipResponse response) {
    	int expires = response.getExpires(dialogPath.getSipStack().getLocalIpAddress());
    	if (expires != -1) {
    		// Set expire period
            expirePeriod = expires;            
        }
    }
	
	/**
	 * Save the SIP entity tag
	 * 
	 * @param etag New tag
	 */
	private void saveEntityTag(String etag) {
		entityTag = etag;
		if (entityTag != null) {
			RegistryFactory.getFactory().writeString("SipEntityTag", entityTag);
			long etagExpiration = System.currentTimeMillis() + (expirePeriod * 1000);
	    	RegistryFactory.getFactory().writeLong("SipETagExpiration", etagExpiration);
	        if (logger.isActivated()) {
	        	logger.debug("New entity tag: " + entityTag + ", expire at=" + etagExpiration);
	        }
		} else {
			RegistryFactory.getFactory().removeParameter("SipEntityTag");
	    	RegistryFactory.getFactory().removeParameter("SipETagExpiration");
	        if (logger.isActivated()) {
	        	logger.debug("Entity tag has been reset");
	        }
		}
	}
	
	/**
	 * Read the SIP entity tag
	 */
	private void readEntityTag() {
		entityTag = RegistryFactory.getFactory().readString("SipEntityTag", null);
    	long etagExpiration = RegistryFactory.getFactory().readLong("SipETagExpiration", -1);
        if (logger.isActivated()) {
        	logger.debug("New entity tag: " + entityTag + ", expire at=" + etagExpiration);
        }
	}
	
	/**
	 * Create a new dialog path
	 * 
	 * @return Dialog path
	 */
	private SipDialogPath createDialogPath() {
        // Set Call-Id
    	String callId = imsModule.getSipManager().generateCallId();

    	// Set target
    	String target = ImsModule.IMS_USER_PROFILE.getPublicUri();

        // Set local party
    	String localParty = ImsModule.IMS_USER_PROFILE.getPublicUri();

        // Set remote party
    	String remoteParty = ImsModule.IMS_USER_PROFILE.getPublicUri();

    	// Set the route path
    	Vector<String> route = imsModule.getSipManager().getSipStack().getDefaultRoutePath();

    	// Create a dialog path
    	SipDialogPath dialogPath = new SipDialogPath(
        		imsModule.getSipManager().getSipStack(),
        		callId,
        		1,
        		target,
        		localParty,
        		remoteParty,
        		route);
    	return dialogPath;
	}
}
