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
package com.orangelabs.rcs.samples.im;

import android.app.Activity;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.orangelabs.rcs.samples.R;
import com.orangelabs.rcs.samples.utils.Utils;
import com.orangelabs.rcs.service.api.client.messaging.MessagingApi;

/**
 * Send IM in short mode
 * 
 * @author jexa7410
 */
public class SendShortIm extends Activity {

	/**
	 * Rich messaging API
	 */
    public static MessagingApi messagingApi;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.messaging_send_im);
        
        // Set UI title
        setTitle(R.string.menu_short_im);
        
        // Set the contact selector
        Spinner spinner = (Spinner)findViewById(R.id.contact);
        spinner.setAdapter(Utils.createContactListAdapter(this));
        
        // Set button callback
        Button btn = (Button)findViewById(R.id.send);
        btn.setOnClickListener(btnSendListener);
        
        // Instanciate messaging API
		messagingApi = new MessagingApi(getApplicationContext());
		messagingApi.connectApi();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
        // Disconnect messaging API
    	messagingApi.disconnectApi();
    }

    /**
     * Send button callback
     */
    private OnClickListener btnSendListener = new OnClickListener() {
        public void onClick(View v) {
        	try {
        		// Get the remote contact
                Spinner spinner = (Spinner)findViewById(R.id.contact);
                CursorWrapper cursor = (CursorWrapper)spinner.getSelectedItem();
                String remote = cursor.getString(1);
                
                // Get the message to be sent
                EditText msgEdit = (EditText)findViewById(R.id.message);
                
                // Send the message
                messagingApi.sendShortIM(remote, msgEdit.getText().toString());
        		Toast.makeText(SendShortIm.this, R.string.label_send_msg_ok, Toast.LENGTH_SHORT).show();
        	} catch(Exception e) {
		    	Utils.showError(SendShortIm.this, getString(R.string.label_send_msg_ko));
        	}
        }
    };
}
