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

import java.io.ByteArrayInputStream;

import org.xml.sax.InputSource;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.presence.watcherinfo.Watcher;
import com.orangelabs.rcs.core.ims.service.presence.watcherinfo.WatcherInfoDocument;
import com.orangelabs.rcs.core.ims.service.presence.watcherinfo.WatcherInfoParser;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Subscribe manager for presence watcher info event
 * 
 * @author jexa7410
 */
public class WatcherInfoSubscribeManager extends SubscribeManager {
	/**
     * The log4j logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param presentity Presentity
     * @param defaultExpirePeriod Default expiration period in seconds
     */
    public WatcherInfoSubscribeManager(ImsModule parent, String presentity, int defaultExpirePeriod) {
    	super(parent, presentity, defaultExpirePeriod);
    }
    	
	/**
     * Create a SUBSCRIBE request
     * 
	 * @param dialog SIP dialog path
	 * @param expirePeriod Expiration period
	 * @param accessInfo Access info
	 * @return SIP request
	 * @throws SipException
     */
    public SipRequest createSubscribe(SipDialogPath dialog, int expirePeriod, String accessInfo) throws SipException {
    	// Create SUBSCRIBE message
    	SipRequest subscribe = SipMessageFactory.createSubscribe(dialog, expirePeriod, accessInfo);

    	// Set the Event header
    	subscribe.addHeader("Event: presence.winfo");

    	// Set the Accept header
    	subscribe.addHeader("Accept: application/watcherinfo+xml");

    	return subscribe;
    }

    /**
     * Receive a notification
     * 
     * @param notify Received notify
     */
    public void receiveNotification(SipRequest notify) {
    	// Check notification
    	if (!isNotifyForThisSubscriber(notify)) {
    		return;
    	}    	

    	if (logger.isActivated()) {
			logger.debug("New watcher-info notification received");
		}    	
    	
	    // Parse XML part
	    String content = notify.getContent();
		if (content != null) {
	    	try {
				InputSource input = new InputSource(
						new ByteArrayInputStream(notify.getContent().getBytes()));
				WatcherInfoParser parser = new WatcherInfoParser(input);
				WatcherInfoDocument watcherinfo = parser.getWatcherInfo();
				if (watcherinfo != null) {
					for (int i=0; i < watcherinfo.getWatcherList().size(); i++) {
						Watcher w = (Watcher)watcherinfo.getWatcherList().elementAt(i);
						String contact = w.getUri();
						String status = w.getStatus();
						String event = w.getEvent();
						
						if ((contact != null) && (status != null) && (event != null)) {
							if (status.equalsIgnoreCase("pending")) {
								// It's an invitation or a new status
								getImsModule().getCore().getListener().handlePresenceSharingInvitation(contact);
							}
							
							// Notify listener
							getImsModule().getCore().getListener().handlePresenceSharingNotification(contact, status, event);
						}
					}
				}
	    	} catch(Exception e) {
	    		if (logger.isActivated()) {
	    			logger.error("Can't parse watcher-info notification", e);
	    		}
	    	}
	    }
		
		// Check subscription state
		String state = notify.getHeader("Subscription-State");
		if ((state != null) && (state.indexOf("terminated") != -1)) {
			if (logger.isActivated()) {
				logger.info("Watcher-info subscription has been terminated by server");
			}
			terminatedByServer();
		}
    }
}
