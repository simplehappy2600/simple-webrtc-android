/*
 * libjingle
 * Copyright 2013, Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.appspot.apprtc;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.message.BasicNameValuePair;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.json.JSONObject;

import android.util.Log;
import android.widget.Toast;

import com.codebutler.android_websockets.WebSocketClient;

class UserInfo {
	String username;
	String userstate;
};

/**
 * Negotiates signaling for chatting over WooGeen XMPP service.
 * 
 */
public class AppRTCClient {
	private static final String TAG = "AppRTCClient";
	private final AppRTCDemoActivity activity;

	//XMPPConnection mConnection;
	ChatManager mChatManager;
	ChatManagerListener mChatListener;
	MessageListener mMessageListener;
	Chat mChat;
	List<UserInfo> mUserList;
	boolean mIsConnected;
	String mDesName;

	//List<BasicNameValuePair> extraHeaders = Arrays.asList(new BasicNameValuePair("Cookie", "session=abcd"));
	List<BasicNameValuePair> extraHeaders = new ArrayList<BasicNameValuePair>();

	URI uri = URI.create("ws://172.16.41.183:8080/WebSocketServer/SampleServlet");
	//URI uri = URI.create("ws://192.168.1.106:8080/WebSocketServer/SampleServlet");	

	WebSocketClient.Listener webSocketClientListener = new WebSocketClient.Listener() {

		@Override
		public void onMessage(byte[] data) {
		}

		@Override
		public void onMessage(String message) {
			Log.i(TAG, "onMessage: " + message);
			activity.onMessage(message);		
		}

		@Override
		public void onError(Exception error) {
			Log.e(TAG, "websocket异常", error);
		}

		@Override
		public void onDisconnect(int code, String reason) {
			Log.i(TAG, "disconnect server.");
			mIsConnected = false;
		}

		@Override
		public void onConnect() {
			Log.i(TAG, "connect server.");
			mIsConnected = true;
		}
	};

	WebSocketClient webSocketClient = new WebSocketClient(uri,
			webSocketClientListener, extraHeaders);

	public AppRTCClient(AppRTCDemoActivity activity) {
		this.activity = activity;
		//mConnection = null;
		mChatManager = null;
		mChatListener = null;
		mMessageListener = null;
		mChat = null;
		mUserList = new ArrayList<UserInfo>();
		mIsConnected = false;
		mDesName = "";

	}

	/**
	 * Asynchronously connect to WooGeen XMPP server and listen on XMPP messages
	 */
	public boolean connectToServer(String ip, int port) {
		webSocketClient.connect();

		return mIsConnected;
	}

	public boolean Login(String name, String pwd) {

		return true;
	}

	/**
	 * Disconnect from XMPP server.
	 */
	public void disconnect() {
//		if (mConnection != null && mIsConnected) {
//			mConnection.disconnect();
//			mConnection = null;
//		}
		this.webSocketClient.disconnect();
		
		mChat = null;
		mChatManager = null;		
	}

	/**
	 * Send message to peer over WooGeen XMPP service.
	 */
	public void sendMessage(String name, String msg) {
		this.webSocketClient.send(msg);
		
//		if (mChat == null) {
//			if (mChatManager == null)
//				return;
//			mChat = mChatManager.createChat(name, null);
//		}
//		try {
//			mChat.sendMessage(msg);
//		} catch (XMPPException e) {
//			e.printStackTrace();
//		}
	}
}
