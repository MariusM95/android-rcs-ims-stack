package com.orangelabs.rcs.service.api.client.richcall;

import com.orangelabs.rcs.service.api.client.richcall.IVideoSharingSession;
import com.orangelabs.rcs.service.api.client.richcall.IImageSharingSession;
import com.orangelabs.rcs.service.api.client.IMediaPlayer;

/**
 * Rich call API
 */
interface IRichCallApi {

	// Request content sharing capabilities
	void requestContentSharingCapabilities(in String contact);

	// Get the remote phone number involved in the current call
	String getRemotePhoneNumber();

	// Initiate a live video sharing session
	IVideoSharingSession initiateLiveVideoSharing(in String contact, in IMediaPlayer player);

	// Initiate a pre-recorded video sharing session
	IVideoSharingSession initiateVideoSharing(in String contact, in String file, in IMediaPlayer player);

	// Get a video sharing session from its session ID
	IVideoSharingSession getVideoSharingSession(in String id);

	// Initiate an image sharing session
	IImageSharingSession initiateImageSharing(in String contact, in String file);

	// Get an image sharing session from its session ID
	IImageSharingSession getImageSharingSession(in String id);
}

