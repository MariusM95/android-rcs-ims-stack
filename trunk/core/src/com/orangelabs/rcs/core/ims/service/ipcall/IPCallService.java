package com.orangelabs.rcs.core.ims.service.ipcall;

import java.util.Enumeration;
import java.util.Vector;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.LiveAudioContent;
import com.orangelabs.rcs.core.content.LiveVideoContent;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.capability.CapabilityUtils;
import com.orangelabs.rcs.provider.eab.ContactsManager;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.capability.Capabilities;
import com.orangelabs.rcs.service.api.client.contacts.ContactInfo;
import com.orangelabs.rcs.service.api.client.media.IAudioPlayer;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IP call service has in charge to monitor the IP call in order to stop the
 * current audio and video communication when the call terminates , to process 
 * capability request from remote and to request remote capabilities
 *
 * @author opob7414
 */
public class IPCallService extends ImsService {
    /**
     * IP VOICE CALL features tags 
     */
    public final static String[] FEATURE_TAGS_IP_VOICE_CALL = { FeatureTags.FEATURE_3GPP_IP_VOICE_CALL, FeatureTags.FEATURE_RCSE_IP_VOICE_CALL};

    /**
     * IP AUDIO+VIDEO CALL features tags 
     */
    public final static String[] FEATURE_TAGS_IP_AUDIOVIDEO_CALL = { FeatureTags.FEATURE_3GPP_IP_VOICE_CALL, FeatureTags.FEATURE_RCSE_IP_VOICE_CALL , FeatureTags.FEATURE_RCSE_IP_VIDEO_CALL};
    
    /**
     * IP VIDEO CALL features tags 
     */
    public final static String[] FEATURE_TAGS_IP_VIDEO_CALL = { FeatureTags.FEATURE_RCSE_IP_VIDEO_CALL};
    
	/**
	 * Max sessions
	 */
	private int maxSessions;

	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param parent IMS module
     * @throws CoreException
     */
    public IPCallService(ImsModule parent) throws CoreException {
		super(parent, true);
		
		this.maxSessions = RcsSettings.getInstance().getMaxIPCallSessions();
    }

	/**
	 * Start the IMS service
	 */
	@Override
	public void start() {
		if (isServiceStarted()) {
			// Already started
			return;
		}
		setServiceStarted(true);
	}

	/**
	 * Stop the IMS service
	 */
	@Override
	public void stop() {
		if (!isServiceStarted()) {
			// Already stopped
			return;
		}
		setServiceStarted(false);
	}

	/**
     * Check the IMS service
     */
	@Override
	public void check() {
	
	}
	
    /**
     * Returns the IP call sessions
     *
     * @return session
     */
    public Vector<IPCallStreamingSession> getIPCallSessions() {
        Vector<IPCallStreamingSession> result = new Vector<IPCallStreamingSession>();
        Enumeration<ImsServiceSession> list = getSessions();
        while (list.hasMoreElements()) {
            ImsServiceSession session = list.nextElement();
            result.add((IPCallStreamingSession) session);
        }
        return result;
    }
    
    /**
     * Initiate an IPCall session
     *
     * @param contact Remote contact
     * @param player Media player
     * @param player Audio player
     * @return Call session
     * @throws CoreException
     */
    public IPCallStreamingSession initiateIPCallSession(String contact, IMediaPlayer videoPlayer, IAudioPlayer audioPlayer) throws CoreException {
		if (logger.isActivated()) {
			logger.info("Initiate an IP call session");
		}
		
		// Test number of sessions
		if ((maxSessions != 0) && (getIPCallSessions().size() >= maxSessions)) {
			if (logger.isActivated()) {
                logger.debug("The max number of IP call sessions is achieved: cancel the initiation");
			}
			throw new CoreException("Max sessions achieved");
		}

        
        // Create a new session
        if (logger.isActivated()) {
            logger.debug("AudioContent = "+ContentManager.createGenericLiveAudioContent());
            logger.debug("VideoContent = "+ContentManager.createGenericLiveVideoContent());
            logger.debug("AudioPlayer = "+audioPlayer);
            logger.debug("VideoPlayer = "+videoPlayer);
        }
        
        LiveVideoContent liveVideoContent = (videoPlayer==null) ? null:ContentManager.createGenericLiveVideoContent();
        LiveAudioContent liveAudioContent = (audioPlayer==null) ? null:ContentManager.createGenericLiveAudioContent();
		OriginatingIPCallStreamingSession session = new OriginatingIPCallStreamingSession(
				this,
				videoPlayer,
				audioPlayer,
				liveVideoContent,
				liveAudioContent,
				PhoneUtils.formatNumberToSipUri(contact));
		
		// Start the session
		session.startSession();
		return session;
	}

    /**
     * Receive a IP call invitation
     *
     * @param invite Initial invite
     */
	public void receiveIPCallInvitation(SipRequest invite, boolean audio, boolean video) {
        // Reject if there is already a call in progress
        Vector<IPCallStreamingSession> currentSessions = getIPCallSessions();
        if (currentSessions.size() >= 1) {
        	// Max session
        	if (logger.isActivated()) {
                logger.debug("The max number of IP call sessions is achieved: reject the invitation");
            }
            sendErrorResponse(invite, 486);
            return;
        } 

		// Create a new session    
        IPCallStreamingSession session = new TerminatingIPCallStreamingSession(this, invite);
        
		// Start the session
		session.startSession();
	}
	
    /**
     * Receive a capability request (options procedure)
     *
     * @param options Received options message
     */
    public void receiveCapabilityRequest(SipRequest options) { 
    	// Not used, see ImsDispatcher (OPTIONS request) and CapabilityUtils for OPTIONS request management
    	String contact = SipUtils.getAssertedIdentity(options);

    	if (logger.isActivated()) {
			logger.debug("OPTIONS request received from " + contact);
		}

	    try {
	    	// Create 200 OK response
	    	String ipAddress = getImsModule().getCurrentNetworkInterface().getNetworkAccess().getIpAddress();
			boolean ipcall = getImsModule().getIPCallService().isCallConnectedWith(contact);
	        SipResponse resp = SipMessageFactory.create200OkOptionsResponse(options,
	        		getImsModule().getSipManager().getSipStack().getLocalContact(),
	        		CapabilityUtils.getSupportedFeatureTags(false, ipcall),
	        		CapabilityUtils.buildSdp(ipAddress, ipcall));

	        // Send 200 OK response
	        getImsModule().getSipManager().sendSipResponse(resp);
	    } catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Can't send 200 OK for OPTIONS", e);
        	}
	    }

		// Extract capabilities from the request
    	Capabilities capabilities = CapabilityUtils.extractCapabilities(options);
    	if (capabilities.isImSessionSupported()) {
    		// The contact is RCS capable
   			ContactsManager.getInstance().setContactCapabilities(contact, capabilities, ContactInfo.RCS_CAPABLE, ContactInfo.REGISTRATION_STATUS_ONLINE);
    	} else {
    		// The contact is not RCS
    		ContactsManager.getInstance().setContactCapabilities(contact, capabilities, ContactInfo.NOT_RCS, ContactInfo.REGISTRATION_STATUS_UNKNOWN);
    	}

    	// Notify listener
    	getImsModule().getCore().getListener().handleCapabilitiesNotification(contact, capabilities);
    }
	
	/**
	 * Abort all pending sessions
	 */
	public void abortAllSessions() {
		if (logger.isActivated()) {
			logger.debug("Abort all pending sessions");
		}
		for (Enumeration<ImsServiceSession> e = getSessions(); e.hasMoreElements() ;) {
			ImsServiceSession session = (ImsServiceSession)e.nextElement();
			if (logger.isActivated()) {
				logger.debug("Abort pending session " + session.getSessionID());
			}
			session.abortSession(ImsServiceSession.TERMINATION_BY_SYSTEM);
		}
    }
	
	/**
     * Is call connected
     * 
     * @return Boolean
     */
	public boolean isCallConnected() {
		Vector<IPCallStreamingSession> sessions = getIPCallSessions(); 
		return (sessions.size() > 0);
	}

	/**
     * Is call connected with a given contact
     * 
     * @param contact Contact
     * @return Boolean
     */
	public boolean isCallConnectedWith(String contact) {
		boolean connected = false;
		Vector<IPCallStreamingSession> sessions = getIPCallSessions(); 
		for(int i=0; i < sessions.size(); i++) {
			IPCallStreamingSession session = sessions.get(i);
			if (PhoneUtils.compareNumbers(session.getRemoteContact(), contact)) {
				connected = true;
				break;
			}
		}
		return connected;
	}	
}
