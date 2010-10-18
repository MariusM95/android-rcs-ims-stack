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
package com.orangelabs.rcs.utils;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.orangelabs.rcs.core.ims.ImsModule;

/**
 * Phone utility functions
 * 
 * @author jexa7410
 */
public class PhoneUtils {
	/**
	 * Tel-URI format supported by the platform
	 */
	public static boolean TEL_URI_SUPPORTED = false;
	
	/**
	 * Country code
	 */
	public static String COUNTRY_CODE = "+33";

	/**
	 * Set the country code
	 * 
	 * @param context Context 
	 */
	public static void setCountryCode(Context context) {
		if (context == null) {
			return;
		}

		TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
		String cc = tm.getSimCountryIso();
		if (cc.startsWith("+")) {
			COUNTRY_CODE = cc; 
		} else {
			if (cc.equalsIgnoreCase("fr")) {
				COUNTRY_CODE = "+33"; 
			} else
			if (cc.equalsIgnoreCase("cn")) {
				COUNTRY_CODE = "+86"; 
			} else
			if (cc.equalsIgnoreCase("es")) {
				COUNTRY_CODE = "+34"; 
			}
			// TODO: how to generalize the mapping table
		}
	}

	/**
	 * Format a phone number to international format
	 * 
	 * @param number Phone number
	 * @return International number
	 */
	public static String formatNumberToInternational(String number) {
		if (number == null) {
			return null;
		}
		
		number = number.trim();

		String formattedNumber = "";
		for(int i=0; i < number.length(); i++) {
			char c = number.charAt(i);
			if (c != '-') {
				formattedNumber += c;
			}
		}

		// TODO: see RFC to format into international number
		if (formattedNumber.startsWith("0")) {
			formattedNumber = COUNTRY_CODE + formattedNumber.substring(1);
		} else
		if (!formattedNumber.startsWith("+")) {
			formattedNumber = COUNTRY_CODE + formattedNumber;
		}
		return formattedNumber;
	}
	
	/**
	 * Format a phone number to a SIP address (SIP-URI or Tel-URI)
	 * 
	 * @param number Phone number
	 * @return Tel-URI
	 */
	public static String formatNumberToSipAddress(String number) {
		if (number == null) {
			return null;
		}

		number = number.trim();
		
		if (number.startsWith("tel:")) {
			number = number.substring(4);
		} else		
		if (number.startsWith("sip:")) {
			number = number.substring(4, number.indexOf("@"));
		}
		
		if (TEL_URI_SUPPORTED) {
			return "tel:" + formatNumberToInternational(number);
		} else {
			return "sip:" + formatNumberToInternational(number) + "@" + ImsModule.IMS_USER_PROFILE.getHomeDomain() + ";user=phone";
		}
	}

	/**
	 * Extract a phone number from a SIP-URI or Tel-URI
	 * 
	 * @param uri SIP or Tel URI
	 * @return Number
	 */
	public static String extractNumberFromUri(String uri) {
		if (uri == null) {
			return null;
		}

		try {
			int index1 = uri.indexOf("tel:");
			if (index1 != -1) {
				uri = uri.substring(index1+4);
			}
			
			index1 = uri.indexOf("sip:");
			if (index1 != -1) {
				int index2 = uri.indexOf("@", index1);
				uri = uri.substring(index1+4, index2);
			}
			
			return formatNumberToInternational(uri);
		} catch(Exception e) {
			return null;
		}
	}
}
