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
package com.orangelabs.rcs.samples.utils;

import com.orangelabs.rcs.samples.R;
import com.orangelabs.rcs.samples.R.string;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;
import android.provider.Contacts.Phones;

/**
 * Utility functions
 * 
 * @author jexa7410
 */
public class Utils {
	/**
	 * Create a contact selector based on the native address book
	 * 
	 * @param activity Activity
	 * @return List adapter
	 */
	public static ContactListAdapter createContactListAdapter(Activity activity) {
	    String[] PROJECTION = new String[] {
	    		Phones._ID,
	    		Phones.NUMBER,
	    		Phones.NAME
		    };
        ContentResolver content = activity.getContentResolver();
		Cursor cursor = content.query(Phones.CONTENT_URI, PROJECTION, Phones.NUMBER + "!='null'", null, null);
        activity.startManagingCursor(cursor);
		return new ContactListAdapter(activity, cursor);
	}
	
	/**
	 * Show an error info and exit from the current activity
	 * 
	 * @param activity Activity
	 * @param msg Message to be displayed
	 * @return Dialog to be displayed
	 */
    public static AlertDialog showError(final Activity activity, String msg) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    	builder.setMessage(msg);
    	builder.setTitle(R.string.title_error);
    	builder.setCancelable(false);
    	builder.setPositiveButton(activity.getString(R.string.label_ok), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int id) {
    			activity.finish();
    		}
    	});
    	AlertDialog alert = builder.create();
    	alert.show();
    	return alert;
    }

	/**
	 * Show an info and exit from the current activity
	 * 
	 * @param activity Activity
	 * @param msg Message to be displayed
	 * @return Dialog to be displayed
	 */
    public static AlertDialog showInfo(final Activity activity, String msg) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    	builder.setMessage(msg);
    	builder.setTitle(R.string.title_info);
    	builder.setCancelable(false);
    	builder.setPositiveButton(activity.getString(R.string.label_ok), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int id) {
    			activity.finish();
    		}
    	});
    	AlertDialog alert = builder.create();
    	alert.show();
    	return alert;
    }

	/**
	 * Show an info and exit from the current activity
	 * 
	 * @param activity Activity
	 * @param title Title of the dialog
	 * @param msg Message to be displayed
	 * @return Dialog to be displayed
	 */
    public static AlertDialog showInfo(final Activity activity, String title, String msg) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    	builder.setMessage(msg);
    	builder.setTitle(title);
    	builder.setCancelable(false);
    	builder.setPositiveButton(activity.getString(R.string.label_ok), new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int id) {
    			activity.finish();
    		}
    	});
    	AlertDialog alert = builder.create();
    	alert.show();
    	return alert;
    }
}
