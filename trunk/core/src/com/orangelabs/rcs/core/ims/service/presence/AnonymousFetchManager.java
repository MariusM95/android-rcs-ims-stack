package com.orangelabs.rcs.core.ims.service.presence;

import java.io.ByteArrayInputStream;

import org.xml.sax.InputSource;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.orangelabs.rcs.core.ims.service.presence.pidf.PidfParser;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Anonymous fetch manager
 * 
 * @author jexa7410
 */
public class AnonymousFetchManager {
	   /**
     * IMS module
     */
    private ImsModule imsModule;
    
	/**
     * The log4j logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     */
    public AnonymousFetchManager(ImsModule parent) {
        this.imsModule = parent;
    }

    /**
     * Terminate manager
     */
    public void terminate() {
    	if (logger.isActivated()) {
    		logger.info("Terminate the anonymous fetch manager");
    	}
    }
    
	/**
	 * Request capabilities for a given contact (i.e anonymous fetch)
	 * 
	 * @param contact Contact
	 */
	public void requestCapabilities(String contact) {
		AnonymousFetchCapabilities request = new AnonymousFetchCapabilities(imsModule, contact);
		request.start();
	}	
	
	/**
     * Receive a notification
     * 
     * @param notify Received notify
     */
    public void receiveNotification(SipRequest notify) {
    	if (logger.isActivated()) {
			logger.debug("Anonymous fetch notification received");
		}
    	
		// Parse XML part
	    String content = notify.getContent();
		if (content != null) {
			try {
    			// Parse PIDF part
				InputSource pidfInput = new InputSource(new ByteArrayInputStream(content.getBytes()));
				PidfParser pidfParser = new PidfParser(pidfInput);
				PidfDocument presenceInfo = pidfParser.getPresence();
				
				// Notify listener
		    	imsModule.getCore().getListener().handleAnonymousFetchNotification(
		    			presenceInfo.getEntity(), presenceInfo);
	    	} catch(Exception e) {
	    		if (logger.isActivated()) {
	    			logger.error("Can't parse PIDF notification", e);
	    		}
	    	}
		}
    }	
}
