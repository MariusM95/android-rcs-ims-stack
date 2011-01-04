/*******************************************************************************
 * Software Name : RCS IMS Stack
 * Version : 2.0
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
package com.orangelabs.rcs.service.api.server.messaging;

import java.util.Date;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.orangelabs.rcs.core.content.FileContent;
import com.orangelabs.rcs.core.content.PhotoContent;
import com.orangelabs.rcs.core.content.VideoContent;
import com.orangelabs.rcs.core.ims.service.im.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.InstantMessageError;
import com.orangelabs.rcs.core.ims.service.im.InstantMessageSession;
import com.orangelabs.rcs.core.ims.service.im.InstantMessageSessionListener;
import com.orangelabs.rcs.provider.messaging.RichMessaging;
import com.orangelabs.rcs.provider.messaging.RichMessagingData;
import com.orangelabs.rcs.service.api.client.messaging.IChatEventListener;
import com.orangelabs.rcs.service.api.client.messaging.IChatSession;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Chat session
 * 
 * @author jexa7410
 */
public class ChatSession extends IChatSession.Stub implements InstantMessageSessionListener {
	
	/**
	 * Core session
	 */
	private InstantMessageSession session;

	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IChatEventListener> listeners = new RemoteCallbackList<IChatEventListener>();

    /**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param session Session
	 */
	public ChatSession(InstantMessageSession session) {
		this.session = session;
		session.addListener(this);
	}

	/**
	 * Get session ID
	 * 
	 * @return Session ID
	 */
	public String getSessionID() {
		return session.getSessionID();
	}
	
	/**
	 * Get remote contact
	 * 
	 * @return Contact
	 */
	public String getRemoteContact() {
		return session.getRemoteContact();
	}
	
	/**
	 * Get subject
	 * 
	 * @return Subject
	 */
	public String getSubject() {
		return session.getSubject();
	}

	/**
	 * Accept the session invitation
	 */
	public void acceptSession() {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}
				
		// Accept invitation
		session.acceptSession();

	}
	
	/**
	 * Reject the session invitation
	 */
	public void rejectSession() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Update rich messaging history
		RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, session.getSessionID(), null, session.getRemoteContact(), getSubject(), RichMessagingData.EVENT_OUTGOING, "text/plain", null, getSubject().length(), null, RichMessagingData.STATUS_TERMINATED);
		
        // Reject invitation
		session.rejectSession();

	}

	/**
	 * Cancel the session
	 */
	public void cancelSession() {
		if (logger.isActivated()) {
			logger.info("Abort session");
		}
		
		// Update rich messaging history
		if (!RichMessaging.getInstance().isSessionTerminated(session.getSessionID())){
			RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, session.getSessionID(), null, session.getRemoteContact(), getSubject(), RichMessagingData.EVENT_OUTGOING, "text/plain", null, getSubject().length(), null, RichMessagingData.STATUS_TERMINATED);
		}
		
		// Abort the session
		session.abortSession();
	}
	
	/**
	 * Send a text message
	 * 
	 * @param text Text message
	 */
	public void sendMessage(String text) {
		// Update rich messaging history
    	RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, getSessionID(), null, getRemoteContact(), text, RichMessagingData.EVENT_OUTGOING, "text/plain", null, text.length(), null, RichMessagingData.STATUS_SENT);

    	// Send message
		session.sendMessage(text);
	}

	/**
	 * Set the is composing status
	 * 
	 * @param status Status
	 */
	public void setIsComposingStatus(boolean status) throws RemoteException {
		session.setIsComposingStatus(status);
	}

	/**
	 * Add session listener
	 * 
	 * @param listener Listener
	 */
	public void addSessionListener(IChatEventListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an event listener");
		}

		listeners.register(listener);
	}
	
	/**
	 * Remove session listener
	 * 
	 * @param listener Listener
	 */
	public void removeSessionListener(IChatEventListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an event listener");
		}

		listeners.unregister(listener);
	}
	
	/**
	 * Session is started
	 */
    public void handleSessionStarted() {
		if (logger.isActivated()) {
			logger.info("Session started");
		}

  		// Notify event listeners
		final int N = listeners.beginBroadcast();
        for (int i=0; i < N; i++) {
            try {
            	listeners.getBroadcastItem(i).handleSessionStarted();
            } catch (RemoteException e) {
            	if (logger.isActivated()) {
            		logger.error("Can't notify listener", e);
            	}
            }
        }
        listeners.finishBroadcast();		
    }

    /**
     * Session has been aborted
     */
    public void handleSessionAborted() {
		if (logger.isActivated()) {
			logger.info("Session aborted");
		}

		// Update rich messaging history
		if (!RichMessaging.getInstance().isSessionTerminated(session.getSessionID())){
			RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, session.getSessionID(), null, session.getRemoteContact(), getSubject(), RichMessagingData.EVENT_INCOMING, "text/plain", null, getSubject().length(), null, RichMessagingData.STATUS_TERMINATED);
		}
		
  		// Notify event listeners
		final int N = listeners.beginBroadcast();
        for (int i=0; i < N; i++) {
            try {
            	listeners.getBroadcastItem(i).handleSessionAborted();
            } catch (RemoteException e) {
            	if (logger.isActivated()) {
            		logger.error("Can't notify listener", e);
            	}
            }
        }
        listeners.finishBroadcast();		
    }

    /**
     * Session has been terminated
     */
    public void handleSessionTerminated() {
		if (logger.isActivated()) {
			logger.info("Session terminated");
		}

		// Update rich messaging history
		RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, session.getSessionID(), null, session.getRemoteContact(), getSubject(), RichMessagingData.EVENT_INCOMING, "text/plain", null, getSubject().length(), null, RichMessagingData.STATUS_TERMINATED);
		
  		// Notify event listeners
		final int N = listeners.beginBroadcast();
        for (int i=0; i < N; i++) {
            try {
            	listeners.getBroadcastItem(i).handleSessionTerminated();
            } catch (RemoteException e) {
            	if (logger.isActivated()) {
            		logger.error("Can't notify listener", e);
            	}
            }
        }
        listeners.finishBroadcast();		
    }
    
    /**
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote() {
		if (logger.isActivated()) {
			logger.info("Session terminated by remote");
		}

		// Update rich messaging history
		RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, session.getSessionID(), null, session.getRemoteContact(), getSubject(), RichMessagingData.EVENT_INCOMING, "text/plain", null, getSubject().length(), null, RichMessagingData.STATUS_TERMINATED);
		
  		// Notify event listeners
		final int N = listeners.beginBroadcast();
        for (int i=0; i < N; i++) {
            try {
            	listeners.getBroadcastItem(i).handleSessionTerminatedByRemote();
            } catch (RemoteException e) {
            	if (logger.isActivated()) {
            		logger.error("Can't notify listener", e);
            	}
            }
        }
        listeners.finishBroadcast();		
    }

	/**
	 * New text message received
	 * 
	 * @param text Text message
	 */
    public void handleReceiveMessage(InstantMessage message) {
		if (logger.isActivated()) {
			logger.info("New IM received");
		}

		// Update rich messaging history
		RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, getSessionID(), null, getRemoteContact(), message.getTextMessage(), RichMessagingData.EVENT_INCOMING, "text/plain", message.getRemote(), message.getTextMessage().length(), message.getDate(), RichMessagingData.STATUS_RECEIVED);
		
  		// Notify event listeners
		final int N = listeners.beginBroadcast();
        for (int i=0; i < N; i++) {
            try {
            	listeners.getBroadcastItem(i).handleReceiveMessage(message);
            } catch (RemoteException e) {
            	if (logger.isActivated()) {
            		logger.error("Can't notify listener", e);
            	}
            }
        }
        listeners.finishBroadcast();		
    }
    
    /**
     * New picture received
     * 
     * @param contact Remote contact
     * @param date Date of reception 
     * @param content Picture content 
     */
    public void handleReceivePicture(String contact, Date date, PhotoContent content) {
		// Update rich messaging history
    	RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, getSessionID(), null, getRemoteContact(), content.getUrl(), RichMessagingData.EVENT_INCOMING, content.getEncoding(), content.getName(), content.getSize(), date, RichMessagingData.STATUS_RECEIVED);
    }

    /**
     * New video received
     * 
     * @param contact Remote contact
     * @param date Date of reception
     * @param content Video content
     */
    public void handleReceiveVideo(String contact, Date date, VideoContent content) {
		// Update rich messaging history
    	RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, getSessionID(), null, getRemoteContact(), content.getUrl(), RichMessagingData.EVENT_INCOMING, content.getEncoding(), content.getName(), content.getSize(), date, RichMessagingData.STATUS_RECEIVED);
    }
    
    /**
     * New file received
     * 
     * @param contact Remote contact
     * @param date Date of reception
     * @param content File content
     */
    public void handleReceiveFile(String contact, Date date, FileContent content) {
		// Update rich messaging history
	    RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, getSessionID(), null, getRemoteContact(), content.getUrl(), RichMessagingData.EVENT_INCOMING, content.getEncoding(), content.getName(), content.getSize(), date, RichMessagingData.STATUS_RECEIVED);
    }

    /**
	 * Message has been transfered
	 */
    public void handleMessageTransfered() {
    	// TODO
    }
    
    /**
     * IM session error
     * 
     * @param error Error
     */
    public void handleImError(InstantMessageError error) {
		if (logger.isActivated()) {
			logger.info("IM error received");
		}
		
		// Update rich messaging history
    	switch(error.getErrorCode()){
    	case InstantMessageError.SESSION_INITIATION_DECLINED:
    	case InstantMessageError.SESSION_INITIATION_CANCELLED:
    		RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, getSessionID(), null, getRemoteContact(), "", RichMessagingData.EVENT_INCOMING, "text/plain", "", 0, null, RichMessagingData.STATUS_TERMINATED);
    		break;
    	default:
        	RichMessaging.getInstance().addMessage(RichMessagingData.TYPE_CHAT, getSessionID(), null, getRemoteContact(), "", RichMessagingData.EVENT_INCOMING, "text/plain", "", 0, null, RichMessagingData.STATUS_FAILED);
    		break;
    	}
    	
  		// Notify event listeners
		final int N = listeners.beginBroadcast();
        for (int i=0; i < N; i++) {
            try {
            	listeners.getBroadcastItem(i).handleImError(error.getErrorCode());
            } catch (RemoteException e) {
            	if (logger.isActivated()) {
            		logger.error("Can't notify listener", e);
            	}
            }
        }
        listeners.finishBroadcast();	
    }

    /**
	 * Handle "contact is composing" event
	 * 
	 * @param contact Contact
	 * @param status Status
	 */
	public void handleContactIsComposing(String contact, boolean status) {
		if (logger.isActivated()) {
			logger.info(contact + " is composing status set to " + status);
		}

  		// Notify event listeners
		final int N = listeners.beginBroadcast();
        for (int i=0; i < N; i++) {
            try {
            	listeners.getBroadcastItem(i).handleIsComposingEvent(contact, status);
            } catch (RemoteException e) {
            	if (logger.isActivated()) {
            		logger.error("Can't notify listener", e);
            	}
            }
        }
        listeners.finishBroadcast();
	}
}
