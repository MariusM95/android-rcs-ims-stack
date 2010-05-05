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
package com.orangelabs.rcs.core.ims.service.im;

/**
 * Instant message error
 * 
 * @author jexa7410
 */
public class InstantMessageError extends Error {
	static final long serialVersionUID = 1L;
	
	/**
	 * Unexpected exception occurs in the module (e.g. internal exception)
	 */
	public final static int UNEXPECTED_EXCEPTION = 0x01;
	
	/**
	 * IM pager mode has failed (e.g. 404 not found)
	 */
	public final static int IM_PAGER_FAILED = 0x02;
	
	/**
	 * IM session initiation has failed (e.g. 408 timeout)
	 */
	public final static int SESSION_INITIATION_FAILED = 0x03;

	/**
	 * IM session has been terminated by the remote (e.g. incoming BYE)
	 */
	public final static int SESSION_TERMINATED_BY_REMOTE = 0x04;

	/**
	 * Message transfer has failed (e.g. MSRP failure)
	 */
	public final static int MSG_TRANSFER_FAILED = 0x05;

	/**
	 * Error code
	 */
	private int code;
	
	/**
	 * Constructor
	 * 
	 * @param code Error code
	 */
	public InstantMessageError(int code) {
		super();
		
		this.code = code;
	}
	
	/**
	 * Constructor
	 * 
	 * @param code Error code
	 * @param msg Detail message 
	 */
	public InstantMessageError(int code, String msg) {
		super(msg);
		
		this.code = code;
	}

	/**
	 * Returns the error code
	 * 
	 * @return Error code
	 */
	public int getErrorCode() {
		return code; 
	}
}
