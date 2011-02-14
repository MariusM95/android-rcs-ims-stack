package com.orangelabs.rcs.service.api.client.voip;

import com.orangelabs.rcs.service.api.client.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.IMediaRenderer;
import com.orangelabs.rcs.service.api.client.voip.IVoIpEventListener;

/**
 * VoIP session interface
 */
interface IVoIpSession {
	// Get session ID
	String getSessionID();

	// Get remote contact
	String getRemoteContact();
	
	// Accept the session invitation
	void acceptSession();

	// Reject the session invitation
	void rejectSession();

	// Cancel the session
	void cancelSession();

	// Set the media player
	void setMediaPlayer(in IMediaPlayer player);

	// Set the media renderer
	void setMediaRenderer(in IMediaRenderer renderer);

	// Add session listener
	void addSessionListener(in IVoIpEventListener listener);

	// Remove session listener
	void removeSessionListener(in IVoIpEventListener listener);
}
