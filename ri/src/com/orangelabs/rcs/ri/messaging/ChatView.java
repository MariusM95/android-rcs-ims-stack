/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright © 2010 France Telecom S.A.
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

package com.orangelabs.rcs.ri.messaging;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.SmileyParser;
import com.orangelabs.rcs.ri.utils.Smileys;
import com.orangelabs.rcs.ri.utils.Utils;
import com.orangelabs.rcs.service.api.client.ClientApiListener;
import com.orangelabs.rcs.service.api.client.ImsEventListener;
import com.orangelabs.rcs.service.api.client.contacts.ContactsApi;
import com.orangelabs.rcs.service.api.client.messaging.IChatEventListener;
import com.orangelabs.rcs.service.api.client.messaging.IChatSession;
import com.orangelabs.rcs.service.api.client.messaging.InstantMessage;
import com.orangelabs.rcs.service.api.client.messaging.MessagingApi;
import com.orangelabs.rcs.utils.PhoneUtils;

/**
 * Chat view
 */
public class ChatView extends ListActivity implements OnClickListener, OnKeyListener, ClientApiListener, ImsEventListener {
	/**
	 * Wizz message
	 */
	public final static String WIZZ_MSG = "((-))";
	
    /**
     * UI handler
     */
    private Handler handler = new Handler();
    
    /**
	 * Message composer
	 */
    private EditText mUserText;
    
    /**
     * Message history adapter
     */
    private ArrayAdapter<String> mAdapter;
    
    /**
     * Message history
     */
    private ArrayList<String> mStrings = new ArrayList<String>();
    
    /**
	 * Messaging API
	 */
    private MessagingApi messagingApi;
    
	/**
	 * Contacts API
	 */
    private ContactsApi contactsApi;    
    
    /**
     * Chat session 
     */
    private IChatSession chatSession = null;

    /**
     * Is chat group
     */
    private boolean chatGroup = false;
    
    /**
     * Session ID 
     */
    private String sessionId;
    
    /**
     * Utility class to manage IsComposing status
     */
	private IsComposingManager composingManager;
	
	/**
	 * Smileys
	 */
	private Smileys smileyResources;
	
	/**
	 * Flag indicating that the activity is put on background
	 */
	private boolean isInBackground = false;
	
	/**
	 * Message that were received while we were in background
	 */
	private List<InstantMessage> imReceivedInBackground = new ArrayList<InstantMessage>();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.messaging_chat_view);
        
        // Set the message list adapter
        mAdapter = new ArrayAdapter<String>(this,  android.R.layout.simple_list_item_1, mStrings) {
        	@Override
        	public View getView(int position, View convertView, android.view.ViewGroup parent) {
        		TextView v = (TextView)super.getView(position, convertView, parent);
        		v.setText(formatMessage(v.getText().toString()), BufferType.SPANNABLE);
        		return v;
        	};
        };
        setListAdapter(mAdapter);
        
        // Smiley resources
		smileyResources = new Smileys(this);

		// Instanciate the composing manager
		composingManager = new IsComposingManager();
        
        // Set message composer callbacks
        mUserText = (EditText)findViewById(R.id.userText);
        mUserText.setOnClickListener(this);
        mUserText.setOnKeyListener(this);
        mUserText.addTextChangedListener(mUserTextWatcher);
        
        // Set the message composer max length
		RcsSettings.createInstance(getApplicationContext());
		int maxLength = RcsSettings.getInstance().getMaxChatMessageLength();
		InputFilter[] filterArray = new InputFilter[1];
		filterArray[0]=new InputFilter.LengthFilter(maxLength);
		mUserText.setFilters(filterArray);
        
		// Set button listener
        Button btn = (Button)findViewById(R.id.send_button);
        btn.setOnClickListener(this);
        
        // Get chat session-ID
		sessionId = getIntent().getStringExtra("sessionId");
		// Get subject
		String subject = getIntent().getStringExtra("subject");
		if (subject!=null && subject.trim().length()>0){
			// Add subject to the message history as first message
			if (getIntent().getBooleanExtra("originating", false)){
				mAdapter.add("[Me] " + subject);
			}else{
				mAdapter.add("["+getIntent().getStringExtra("contact")+"] " + subject);
			}
		}
        
		// Instanciate messaging API
        messagingApi = new MessagingApi(getApplicationContext());
        messagingApi.addApiEventListener(this);
        messagingApi.addImsEventListener(this);
        messagingApi.connectApi();
        
        // Instanciate contacts API
        contactsApi = new ContactsApi(getApplicationContext());
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();

        // Remove session listener
        if (chatSession != null) {
        	try {
        		chatSession.removeSessionListener(chatSessionListener);
        	} catch(Exception e) {
        	}
        }

        // Disconnect messaging API
        messagingApi.removeApiEventListener(this);
        messagingApi.disconnectApi();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	isInBackground = true;
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	isInBackground = false;
    	
    	// Mark all messages that were received while we were in background as "displayed" 
    	for (int i=0;i<imReceivedInBackground.size();i++){
    		InstantMessage msg = imReceivedInBackground.get(i);
    		markMessageAsDisplayed(msg);
    	}
    	imReceivedInBackground.clear();
    }
    
    /**
     * Message composer listener
     * 
     * @param v View
     */
    public void onClick(View v) {
        sendText();
    }

    /**
     * Message composer listener
     * 
     * @param v View
     * @param keyCode Key code
     * @event Key event
     */
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    sendText();
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Send a text and display it
     */
    private void sendText() {
    	if (chatSession == null) {
    		return;
    	}
    	
        String text = mUserText.getText().toString();
        if ((text == null) || (text.length() == 0)) {
        	return;
        }
        
        // Warn the composing manager that the message was sent
		composingManager.messageWasSent();
		
        try {
        	// Send the text to remote
        	chatSession.sendMessage(text);
        	
        	// Add text to the message history
            mAdapter.add("[Me] " + text);
            mUserText.setText(null);
        } catch(Exception e) {
        	Utils.showMessage(ChatView.this, getString(R.string.label_send_im_failed));
        }
    }

    /**
     * Send a wizz and display it
     */
    private void sendWizz() {
    	if (chatSession == null) {
    		return;
    	}
    	
        // Warn the composing manager that the message was sent
		composingManager.messageWasSent();
		
        try {
        	// Send the text to remote
        	chatSession.sendMessage(WIZZ_MSG);
        	
        	// Add text to the message history
            mAdapter.add("[Me] " + getString(R.string.label_chat_wizz));
        } catch(Exception e) {
        	Utils.showMessage(ChatView.this, getString(R.string.label_send_im_failed));
        }
    }
    
    
    /**
     * Mark a message as "displayed"
     * 
     * @param msg
     */
    private void markMessageAsDisplayed(InstantMessage msg){
    	try {
    		chatSession.setMessageDeliveryStatus(msg.getMessageId(), msg.getRemote(), ImdnDocument.DELIVERY_STATUS_DISPLAYED);
    	}catch(RemoteException e){
    	}
    }

    /**
     * Receive a text and display it
     * 
     * @param msg Instant message
     */
    private void receiveText(InstantMessage msg) {
    	// Add text to the message history
		String number = PhoneUtils.extractNumberFromUri(msg.getRemote());
        mAdapter.add("[" + number + "] " + msg.getTextMessage());
    }
    
    /**
     * Receive a notification and display it
     * 
     * @param notif Text
     */
    private void receiveNotif(String notif) {
    	// Add text to the message history
        mAdapter.add("[CHAT] " + notif);
    }

    /**
     * Receive a Wizz and play it
     * 
     * @param msg Instant message
     */
    private void receiveWizz(InstantMessage msg) {
    	// Add Wizz to the message history
		String number = PhoneUtils.extractNumberFromUri(msg.getRemote());
        mAdapter.add("[" + number + "] " + getString(R.string.label_chat_wizz));
        
        // Vibrate
        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(600);
    }
    
    /**
     * API disabled
     */
    public void handleApiDisabled() {
		handler.post(new Runnable() { 
			public void run() {
				Utils.showMessageAndExit(ChatView.this, getString(R.string.label_api_disabled));
			}
		});
    }
    
    /**
     * API connected
     */
    public void handleApiConnected() {
		handler.post(new Runnable() { 
			public void run() {
	    		try {
	    			// Register to receive session events
					chatSession = messagingApi.getChatSession(sessionId);    			
					chatSession.addSessionListener(chatSessionListener);
					
					// Set UI title
					chatGroup = chatSession.isChatGroup();
					if (chatGroup) {
						setTitle(getString(R.string.title_chat_view_group));
					} else {
						setTitle(getString(R.string.title_chat_view_oneone) + " " +
								PhoneUtils.extractNumberFromUri(chatSession.getRemoteContact())); 
					}		
	    		} catch(Exception e) {
	    			Utils.showMessageAndExit(ChatView.this, getString(R.string.label_api_failed));
	    		}
			}
		});
    }

    /**
     * API disconnected
     */
    public void handleApiDisconnected() {
		handler.post(new Runnable(){
			public void run(){
				Utils.showMessageAndExit(ChatView.this, getString(R.string.label_api_disconnected));
			}
		});
    }  
    
    /**
     * IMS connected
     */
	public void handleImsConnected() {
	}

    /**
     * IMS disconnected
     */
	public void handleImsDisconnected() {
    	// IMS has been disconnected
		handler.post(new Runnable(){
			public void run(){
				Utils.showMessageAndExit(ChatView.this, getString(R.string.label_ims_disconnected));
			}
		});
	}
    
    /**
     * Chat session event listener
     */
    private IChatEventListener chatSessionListener = new IChatEventListener.Stub() {
		// Session is started
		public void handleSessionStarted() {
		}
	
		// Session has been aborted
		public void handleSessionAborted() {
			handler.post(new Runnable(){
				public void run(){
					// Session aborted
					Utils.showMessageAndExit(ChatView.this, getString(R.string.label_chat_aborted));
				}
			});
		}
	    
		// Session has been terminated by remote
		public void handleSessionTerminatedByRemote() {
			handler.post(new Runnable(){
				public void run(){
					Utils.showMessageAndExit(ChatView.this, getString(R.string.label_chat_terminated));
				}
			});
		}
		
		// New text message received
		public void handleReceiveMessage(final InstantMessage msg) {
			
			if (msg.isImdnDisplayedRequested()){
				if (!isInBackground){
					// We received the message, mark it as displayed if the view is not in background
					markMessageAsDisplayed(msg);
				}else{
					// We save this message and will mark it as displayed when the activity resumes
					imReceivedInBackground.add(msg);
				}
			}
			
			handler.post(new Runnable() { 
				public void run() {
					if (msg.getTextMessage().equals(WIZZ_MSG)) {
						receiveWizz(msg);
					} else {
						receiveText(msg);
					}
				}
			});
		}		
				
		// Chat error
		public void handleImError(int error) {
			handler.post(new Runnable() {
				public void run(){
					Utils.showMessageAndExit(ChatView.this, getString(R.string.label_chat_failed));
				}
			});
		}	
		
		// Is composing event
		public void handleIsComposingEvent(String contact, final boolean isComposing) {
			final String number = PhoneUtils.extractNumberFromUri(contact);
			handler.post(new Runnable() {
				public void run(){
					TextView view = (TextView)findViewById(R.id.isComposingText);
					if (isComposing) {
						view.setText(number	+ " " + getString(R.string.label_contact_is_composing));
						view.setVisibility(View.VISIBLE);
					} else {
						view.setVisibility(View.GONE);
					}
				}
			});
		}

		// Conference event
	    public void handleConferenceEvent(final String contact, final String contactDisplayname, final String state) {
			handler.post(new Runnable() {
				public void run(){
					String number = PhoneUtils.extractNumberFromUri(contact);
					receiveNotif(number + " is " + state);
				}
			});
		}
	    
		// Message delivery status
		public void handleMessageDeliveryStatus(final String msgId, final String contact, final String status) {
			handler.post(new Runnable(){
				public void run(){
					String number = PhoneUtils.extractNumberFromUri(contact);
					// for now we do not display the messages except if it is "displayed"
					if (status.equalsIgnoreCase(ImdnDocument.DELIVERY_STATUS_DISPLAYED)){
						receiveNotif(getString(R.string.label_receive_displayed_delivery_status, number));
					}
				}
			});
		}
		
		// Request to add participant is successful
		public void handleAddParticipantSuccessful() {
			handler.post(new Runnable() {
				public void run(){
					receiveNotif(getString(R.string.label_add_participant_success));
				}
			});
		}
	    
		// Request to add participant has failed
		public void handleAddParticipantFailed(String reason) {
			handler.post(new Runnable() {
				public void run(){
					receiveNotif(getString(R.string.label_add_participant_failed));
				}
			});
		}
    };
    
    /**
     * Format text message
     * 
	 * @param txt Text
	 * @return Formatted message
	 */
	private CharSequence formatMessage(String txt) {
		SpannableStringBuilder buf = new SpannableStringBuilder();
		if (!TextUtils.isEmpty(txt)) {
			SmileyParser smileyParser = new SmileyParser(txt, smileyResources);
			smileyParser.parse();
			buf.append(smileyParser.getSpannableString(this));
		}
		return buf;
	}
    
    /**
     * Quit the session
     */
    private void quitSession() {
		// Stop session
        Thread thread = new Thread() {
        	public void run() {
            	try {
                    if (chatSession != null) {
                    	try {
                    		chatSession.removeSessionListener(chatSessionListener);
                    		chatSession.cancelSession();
                    	} catch(Exception e) {
                    	}
                    	chatSession = null;
                    }
            	} catch(Exception e) {
            	}
        	}
        };
        thread.start();
        
        // Exit activity
		finish();        
    }    
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
    			if (chatSession != null) {
    				AlertDialog.Builder builder = new AlertDialog.Builder(this);
    				builder.setTitle(getString(R.string.title_chat_exit));
    				builder.setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
    					public void onClick(DialogInterface dialog, int which) {
    		            	// Quit the session
    		            	quitSession();
    					}
    				});
    				builder.setNegativeButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// Exit activity
    		                finish();
						}
					});
    				builder.setCancelable(true);
    				builder.show();
    			} else {
                	// Exit activity
    				finish();
    			}
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater=new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_chat_view, menu);
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_insert_smiley:
			Smileys.showSmileyDialog(
					this, 
					mUserText, 
					getResources(), 
					getString(R.string.menu_insert_smiley));
			break;
			
		case R.id.menu_wizz:
	        sendWizz();
			break;

		case R.id.menu_participants:
			try {
				List<String> list = chatSession.getParticipants();
				CharSequence[] participants = new CharSequence[list.size()];
				for(int i=0; i < list.size(); i++) {
					participants[i] = PhoneUtils.extractNumberFromUri(list.get(i));
				}
				Utils.showList(this, getString(R.string.menu_participants), participants);			
			} catch(Exception e) {
				Utils.showMessage(ChatView.this, getString(R.string.label_api_failed));
			}
			break;

		case R.id.menu_add_participant:
			if (chatGroup) {
				addParticipants();
			} else {
				Utils.showMessage(ChatView.this, getString(R.string.label_note_add_participant));
			}
			break;
			
		case R.id.menu_close_session:
			if (chatSession != null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(getString(R.string.title_chat_exit));
				builder.setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
		            	// Quit the session
		            	quitSession();
					}
				});
				builder.setNegativeButton(getString(R.string.label_cancel), null);
				builder.setCancelable(true);
				builder.show();
			} else {
            	// Exit activity
				finish();
			}
			break;
			
		case R.id.menu_quicktext:
			addQuickText();
			break;
		}
		return true;
	}
    
    /**
	 * Add participants to be invited in the session
	 */
    private void addParticipants() {
    	final List<String> participants = new ArrayList<String>(); 

    	// Get list of RCS contacts
		List<String> contacts = contactsApi.getImSessionCapableContacts();
		
		// Remove contacts already in the session
		try {
			List<String> currentContacts = chatSession.getParticipants();
			for(int i=0; i < currentContacts.size(); i++) {
				String c  = currentContacts.get(i);
				if (contacts.contains(c)) {
					contacts.remove(c);
				}
			}
		} catch(Exception e) {}
		
		// Display contacts
		final CharSequence[] items = new CharSequence[contacts.size()];
		for(int i=0; i < contacts.size(); i++) {
			items[i] = contacts.get(i);
		}
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.label_select_contacts);
    	builder.setCancelable(true);
        builder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            	String c = (String)items[which];
            	if (isChecked) {
            		participants.add(c);
            	} else {
            		participants.remove(c);
            	}
            }
        });    	
    	builder.setNegativeButton(getString(R.string.label_cancel), null);                        
    	builder.setPositiveButton(getString(R.string.label_ok), new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int position) {
        		// Add new participants in the session in background
                Thread thread = new Thread() {
            		private Dialog progressDialog = null;

            		public void run() {
                        // Display a progress dialog
    					handler.post(new Runnable(){
    						public void run(){
    							progressDialog = Utils.showProgressDialog(ChatView.this, getString(R.string.label_command_in_progress));            
    						}
    					});
    					
                        try {
                    		if (participants.size() > 0) {
        						if (participants.size() == 1) {
        							// Add one contact
        							chatSession.addParticipant(participants.get(0));
        						} else {
        							// Add several contacts
        							chatSession.addParticipants(participants);					
        						}
            				}
                    		
        					handler.post(new Runnable(){
        						public void run(){
        							progressDialog.dismiss();
        						}
        					});
                    	} catch(Exception e) {
        					handler.post(new Runnable(){
        						public void run(){
    								progressDialog.dismiss();
        							Utils.showMessage(ChatView.this, getString(R.string.label_add_participant_failed));
        						}
        					});
                    	}
                	}
                };
                thread.start();
		    }
		});
        AlertDialog alert = builder.create();
    	alert.show();
    }

    /**
	 * Add quick text
	 */
    private void addQuickText() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.label_select_quicktext);
    	builder.setCancelable(true);
        builder.setItems(R.array.select_quicktext, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String[] items = getResources().getStringArray(R.array.select_quicktext);
        		mUserText.append(items[which]);
            }
        });
        
        AlertDialog alert = builder.create();
    	alert.show();
    }

    /**********************************************************************
     ******************	Deals with isComposing feature ********************
     **********************************************************************/
    
    private final TextWatcher mUserTextWatcher = new TextWatcher(){
		@Override
		public void afterTextChanged(Editable s) {
			// Check if the text is not null.
			// we do not wish to consider putting the edit text back to null (like when sending message), is having activity 
			if (s.length()>0){
				// Warn the composing manager that we have some activity
				composingManager.hasActivity();
			}
		}
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}
    };
    
	public void setTypingStatus(boolean isTyping){
		if (chatSession!=null){
			try {
				chatSession.setIsComposingStatus(isTyping);
			} catch (RemoteException e) {
			}
		}
	}
	
    /**
	 * Utility class to handle is_typing timers (see RFC3994)  
	 */
	private class IsComposingManager{

		// Idle time out (in ms)
		final static int IDLE_TIME_OUT = 5*1000;

		// Active state refresh interval (in ms)
		final static int ACTIVE_STATE_REFRESH = 60*1000; 

		private ClockHandler handler = new ClockHandler();

		// Is composing state
		public boolean isComposing = false;

		private final static int IS_STARTING_COMPOSING = 1;
		private final static int IS_STILL_COMPOSING = 2;
		private final static int MESSAGE_WAS_SENT = 3;
		private final static int ACTIVE_MESSAGE_NEEDS_REFRESH = 4;
		private final static int IS_IDLE = 5;

		private class ClockHandler extends Handler{

			//TODO handle when several chat sessions are active
			public void handleMessage(Message msg){
				switch(msg.what){
					case IS_STARTING_COMPOSING :{
						// Send a typing status "active"
						ChatView.this.setTypingStatus(true);
	
						// In IDLE_TIME_OUT we will need to send a is-idle status message 
						handler.sendEmptyMessageDelayed(IS_IDLE, IDLE_TIME_OUT);
	
						// In ACTIVE_STATE_REFRESH we will need to send an active status message refresh
						handler.sendEmptyMessageDelayed(ACTIVE_MESSAGE_NEEDS_REFRESH, ACTIVE_STATE_REFRESH);
						break;
					}    			
					case IS_STILL_COMPOSING :{
						// Cancel the IS_IDLE messages in queue, if there was one
						handler.removeMessages(IS_IDLE);
	
						// In IDLE_TIME_OUT we will need to send a is-idle status message
						handler.sendEmptyMessageDelayed(IS_IDLE, IDLE_TIME_OUT);
						break;
					}
					case MESSAGE_WAS_SENT :{
						// We are now going to idle state
						composingManager.hasNoActivity();
	
						// Cancel the IS_IDLE messages in queue, if there was one
						handler.removeMessages(IS_IDLE);
	
						// Cancel the ACTIVE_MESSAGE_NEEDS_REFRESH messages in queue, if there was one
						handler.removeMessages(ACTIVE_MESSAGE_NEEDS_REFRESH);
						break;
					}	    			
					case ACTIVE_MESSAGE_NEEDS_REFRESH :{
						// We have to refresh the "active" state
						ChatView.this.setTypingStatus(true);
	
						// In ACTIVE_STATE_REFRESH we will need to send an active status message refresh
						handler.sendEmptyMessageDelayed(ACTIVE_MESSAGE_NEEDS_REFRESH, ACTIVE_STATE_REFRESH);
						break;
					}
					case IS_IDLE :{
						// End of typing
						composingManager.hasNoActivity();
	
						// Send a typing status "idle"
						ChatView.this.setTypingStatus(false);
	
						// Cancel the ACTIVE_MESSAGE_NEEDS_REFRESH messages in queue, if there was one
						handler.removeMessages(ACTIVE_MESSAGE_NEEDS_REFRESH);
						break;
					}
				}
			}
		}

		/**
		 * Edit text has activity
		 */
		public void hasActivity(){
			// We have activity on the edit text
			if (!isComposing){
				// If we were not already in isComposing state
				handler.sendEmptyMessage(IS_STARTING_COMPOSING);
				isComposing = true;
			}else{
				// We already were composing
				handler.sendEmptyMessage(IS_STILL_COMPOSING);
			}
		}

		/**
		 * Edit text has no activity anymore
		 */
		public void hasNoActivity(){
			isComposing = false;
		}

		/**
		 * The message was sent
		 */
		public void messageWasSent(){
			handler.sendEmptyMessage(MESSAGE_WAS_SENT);
		}
	}
}
