/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
package com.orangelabs.rcs.core.ims.service.capability;

import java.util.List;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.orangelabs.rcs.core.ims.service.SessionAuthenticationAgent;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.service.api.client.capability.Capabilities;
import com.orangelabs.rcs.service.api.client.contacts.ContactInfo;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Options request task
 * 
 * @author jexa7410
 */
public class OptionsRequestTask implements Runnable {
    /**
     * IMS module
     */
    private ImsModule imsModule;
    
    /**
     * Remote contact
     */
    private String contact;
    
    /**
     * Feature tags
     */
    private String[] featureTags;
    
    /**
     * Dialog path
     */
    private SipDialogPath dialogPath = null;
    
    /**
	 * Authentication agent
	 */
	private SessionAuthenticationAgent authenticationAgent;

	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
	 * Constructor
	 * 
     * @param parent IMS module
   	 * @param contact Remote contact
   	 * @param featureTags Feature tags
	 */
	public OptionsRequestTask(ImsModule parent, String contact, String[] featureTags) {
        this.imsModule = parent;
        this.contact = contact;
        this.featureTags = featureTags;
		this.authenticationAgent = new SessionAuthenticationAgent(imsModule);
	}
	
	/**
	 * Background processing
	 */
	public void run() {
    	sendOptions();
	}
	
	/**
	 * Send an OPTIONS request
	 */
	private void sendOptions() {
    	if (logger.isActivated()) {
    		logger.info("Send an OPTIONS message to " + contact);                
    	}

        try {
            if (!imsModule.getCurrentNetworkInterface().isRegistered()) {
                if (logger.isActivated()) {
                    logger.debug("IMS not registered, do nothing");
                }
                return;
            }

            // Create a dialog path
        	String contactUri = PhoneUtils.formatNumberToSipUri(contact);
        	dialogPath = new SipDialogPath(
        			imsModule.getSipManager().getSipStack(),
        			imsModule.getSipManager().getSipStack().generateCallId(),
					1,
					contactUri,
					ImsModule.IMS_USER_PROFILE.getPublicUri(),
					contactUri,
					imsModule.getSipManager().getSipStack().getServiceRoutePath());        	
        	
            // Create OPTIONS request
        	if (logger.isActivated()) {
        		logger.debug("Send first OPTIONS");
        	}
	        SipRequest options = SipMessageFactory.createOptions(dialogPath, featureTags);
	        
	        // Send OPTIONS request
	    	sendOptions(options);
        } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("OPTIONS request has failed", e);
        	}
        	handleError(new CapabilityError(CapabilityError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }        
    }
    
	/**
	 * Send OPTIONS message
	 * 
	 * @param options SIP OPTIONS
	 * @throws Exception
	 */
	private void sendOptions(SipRequest options) throws Exception {
        if (logger.isActivated()) {
        	logger.info("Send OPTIONS");
        }

        // Send OPTIONS request
        SipTransactionContext ctx = imsModule.getSipManager().sendSipMessageAndWait(options);

        // Analyze the received response 
        if (ctx.isSipResponse()) {
        	// A response has been received
            if (ctx.getStatusCode() == 200) {
            	// 200 OK
    			handle200OK(ctx);
            } else
            if (ctx.getStatusCode() == 407) {
            	// 407 Proxy Authentication Required
            	handle407Authentication(ctx);
            } else
            if ((ctx.getStatusCode() == 480) || (ctx.getStatusCode() == 408)) {
            	// User not registered
            	handleUserNotRegistered(ctx);
            } else
            if (ctx.getStatusCode() == 404) {
            	// User not found
            	handleUserNotFound(ctx);
            } else {
            	// Other error response
    			handleError(new CapabilityError(CapabilityError.OPTIONS_FAILED,
    					ctx.getStatusCode() + " " + ctx.getReasonPhrase()));
            }
        } else {
    		if (logger.isActivated()) {
        		logger.debug("No response received for OPTIONS");
        	}

    		// No response received: timeout
            handleError(new CapabilityError(CapabilityError.OPTIONS_FAILED,
                    ctx.getStatusCode() + " " + ctx.getReasonPhrase()));
        }
	}       
    
	/**
	 * Handle user not registered 
	 * 
	 * @param ctx SIP transaction context
	 */
	private void handleUserNotRegistered(SipTransactionContext ctx) {
        // 408 or 480 response received
        if (logger.isActivated()) {
            logger.info("User " + contact + " is not registered");
        }

        ContactInfo info = ContactsManager.getInstance().getContactInfo(contact);
        if (info.getRcsStatus() == ContactInfo.NO_INFO) {
        	// If we do not have already some info on this contact
        	// We update the database with empty capabilities
        	Capabilities capabilities = new Capabilities();
        	ContactsManager.getInstance().setContactCapabilities(contact, capabilities, ContactInfo.NO_INFO, ContactInfo.REGISTRATION_STATUS_OFFLINE);
    	} else {
    		// We have some info on this contact
    		// We update the database with its previous infos and set the registration state to offline
    		ContactsManager.getInstance().setContactCapabilities(contact, info.getCapabilities(), info.getRcsStatus(), ContactInfo.REGISTRATION_STATUS_OFFLINE);
    		
        	// Notify listener
        	imsModule.getCore().getListener().handleCapabilitiesNotification(contact, info.getCapabilities());
    	}
	}
	
	/**
	 * Handle user not found 
	 * 
	 * @param ctx SIP transaction context
	 */
	private void handleUserNotFound(SipTransactionContext ctx) {
        // 404 response received
        if (logger.isActivated()) {
            logger.info("User " + contact + " is not found");
        }

        // The contact is not RCS
        Capabilities capabilities = new Capabilities();
        ContactsManager.getInstance().setContactCapabilities(contact, capabilities, ContactInfo.NOT_RCS, ContactInfo.REGISTRATION_STATUS_UNKNOWN);
        
    	// Notify listener
    	imsModule.getCore().getListener().handleCapabilitiesNotification(contact, capabilities);
	}

	/**
	 * Handle 200 0K response 
	 * 
	 * @param ctx SIP transaction context
	 */
	private void handle200OK(SipTransactionContext ctx) {
        // 200 OK response received
        if (logger.isActivated()) {
            logger.info("200 OK response received for " + contact);
        }
    	
    	// Read capabilities
        SipResponse resp = ctx.getSipResponse();
    	Capabilities capabilities = CapabilityUtils.extractCapabilities(resp);

    	// Update the database capabilities
    	if (capabilities.isImSessionSupported()) {
    		// The contact is RCS capable
   			ContactsManager.getInstance().setContactCapabilities(contact, capabilities, ContactInfo.RCS_CAPABLE, ContactInfo.REGISTRATION_STATUS_ONLINE);
    	} else {
    		// The contact is not RCS
    		ContactsManager.getInstance().setContactCapabilities(contact, capabilities, ContactInfo.NOT_RCS, ContactInfo.REGISTRATION_STATUS_UNKNOWN);
    	}

    	// Notify listener
    	imsModule.getCore().getListener().handleCapabilitiesNotification(contact, capabilities);
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

        // Create a second OPTIONS request with the right token
        if (logger.isActivated()) {
        	logger.info("Send second OPTIONS");
        }
        SipRequest options = SipMessageFactory.createOptions(dialogPath, featureTags);
        
        // Set the Authorization header
        authenticationAgent.setProxyAuthorizationHeader(options);
        
        // Send OPTIONS request
    	sendOptions(options);
	}		
	
	/**
	 * Handle error response 
	 * 
	 * @param error Error
	 */
	private void handleError(CapabilityError error) {
        // Error
    	if (logger.isActivated()) {
    		logger.info("Options has failed for contact " + contact + ": " + error.getErrorCode() + ", reason=" + error.getMessage());
    	}
    	
    	// We update the database capabilities timestamp
    	ContactsManager.getInstance().setContactCapabilitiesTimestamp(contact, System.currentTimeMillis());
	}	
}
