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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.I420Frame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Main Activity of the AppRTCDemo Android app demonstrating interoperability
 * between the Android/Java implementation of PeerConnection and the WooGeen
 * demo webapp.
 */
public class AppRTCDemoActivity extends Activity {
	private static final String TAG = "AppRTCDemoActivity";
	private PeerConnection pc;
	private final PCObserver pcObserver = new PCObserver();
	private final SDPObserver sdpObserver = new SDPObserver();
	private AppRTCClient appRtcClient = new AppRTCClient(this);
	private VideoStreamsView vsv;
	private Toast logToast;
	private LinkedList<IceCandidate> queuedRemoteCandidates = new LinkedList<IceCandidate>();
	private final Boolean[] quit = new Boolean[] { false };
	private MediaConstraints sdpMediaConstraints;

	private Context mContext;
	private String mIP;
	private int mPort;
	private String mName;
	private String mPwd;
	public String mDesName;
	private boolean mbInited;
	VideoCapturer capturer;

	public static final int MSG_LOGIN = 100;
	Handler myHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_LOGIN:
				if (connectToServer(mIP, mPort, mName, mPwd)) {
					///if (mDesName != "") {
					///	pc.createOffer(sdpObserver, sdpMediaConstraints);
					///}
				}
				break;
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectNetwork().penaltyLog().build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().penaltyLog()
				.penaltyDeath().build());

		// Since the error-handling of this demo consists of throwing
		// RuntimeExceptions and we assume that'll terminate the app, we install
		// this default handler so it's applied to background threads as well.
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				Log.e(TAG, "", e);
				///e.printStackTrace();
				System.exit(-1);
			}
		});
		mContext = this;
		mbInited = false;
		mDesName = "";
		Point displaySize = new Point();
		getWindowManager().getDefaultDisplay().getSize(displaySize);
		vsv = new VideoStreamsView(this, displaySize);
		setContentView(vsv);

		abortUnless(PeerConnectionFactory.initializeAndroidGlobals(this),
				"Failed to initializeAndroidGlobals");

		((AudioManager) getSystemService(AUDIO_SERVICE))
				.setMode(AudioManager.MODE_IN_COMMUNICATION);

		sdpMediaConstraints = new MediaConstraints();
		sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));		
		sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

		// showGetPeerUI();
		Initialize();

		Message mesg = new Message();
		mesg.what = MSG_LOGIN;
		myHandler.sendMessage(mesg);
	}

	private void showGetPeerUI() {
		View convertView;
		LayoutInflater inflater = (LayoutInflater) this.mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		convertView = inflater.inflate(R.layout.login, null);
		final EditText EditTextIP = (EditText) convertView
				.findViewById(R.id.ipaddr);
		final EditText EditTextPort = (EditText) convertView
				.findViewById(R.id.port);
		final EditText EditTextName = (EditText) convertView
				.findViewById(R.id.name);
		final EditText EditTextPwd = (EditText) convertView
				.findViewById(R.id.password);
		final EditText EditTextDesName = (EditText) convertView
				.findViewById(R.id.desname);

		EditTextIP.setText("61.129.90.140"); // WooGeen XMPP server IP
		EditTextPort.setText("5222"); // WooGeen XMPP port
		EditTextName.setText("jule"); // WooGeen XMPP demo user: jule / tom
		EditTextPwd.setText("jule"); // WooGeen XMPP demo password: jule / tom

		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mIP = EditTextIP.getText().toString();
				String tmp = EditTextPort.getText().toString();
				mPort = Integer.parseInt(tmp);
				mName = EditTextName.getText().toString();
				mPwd = EditTextPwd.getText().toString();
				tmp = EditTextDesName.getText().toString();
				if (tmp.length() != 0)
					mDesName = tmp + "@woogeen";
				dialog.dismiss();
				Message mesg = new Message();
				mesg.what = MSG_LOGIN;
				myHandler.sendMessage(mesg);
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(convertView).setPositiveButton("Go!", listener).show();
	}

	private boolean connectToServer(String ip, int port, String name, String pwd) {
		logAndToast("Connecting to room...");
		boolean result = appRtcClient.connectToServer(ip, port);
		if (!result) {
			Toast.makeText(mContext, "Can't connect server.", Toast.LENGTH_SHORT).show();
			return false;
		}
		result = appRtcClient.Login(name, pwd);
		if (!result) {
			Toast.makeText(mContext, "Login failed.", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}

	@Override
	public void onPause() {
		super.onPause();
		vsv.onPause();
		disconnectAndExit();
	}

	@Override
	public void onResume() {
		super.onResume();
		vsv.onResume();
	}

	/**
	 * var configuration = { "iceServers": [
                        { "url": "stun:stunserver.org:3478" },
                        { "url": "turn:eye@218.206.129.158:5000?transport=udp", credential:"wanlian"}
                     ] };
	 */
	public void Initialize() {
		PeerConnectionFactory factory = new PeerConnectionFactory();
		List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
		// PeerConnection.IceServer iceserver = new PeerConnection.IceServer("stun:61.129.90.140");
		//PeerConnection.IceServer turnserver = new PeerConnection.IceServer("turn:woogeen@61.129.90.140:4478", "", "master");
		
		//PeerConnection.IceServer iceserver = new PeerConnection.IceServer("stun:stun.l.google.com:19302");
		PeerConnection.IceServer iceserver = new PeerConnection.IceServer("stun:172.16.42.39:3478");
		//PeerConnection.IceServer iceserver = new PeerConnection.IceServer("stun:stunserver.org:3478");
		//PeerConnection.IceServer turnserver = new PeerConnection.IceServer("turn:eye@218.206.129.158:5000?transport=udp", "wanlian", "");
		
		//PeerConnection.IceServer iceserver = new PeerConnection.IceServer("stun:192.168.100.108:3478");		
		iceServers.add(iceserver);
		//iceServers.add(turnserver);

		pc = factory.createPeerConnection(iceServers, new MediaConstraints(), pcObserver);

		{
			final PeerConnection finalPC = pc;
			final Runnable repeatedStatsLogger = new Runnable() {
				public void run() {
					synchronized (quit[0]) {
						if (quit[0]) {
							return;
						}
						final Runnable runnableThis = this;
						boolean success = finalPC.getStats(new StatsObserver() {
							public void onComplete(StatsReport[] reports) {
								for (StatsReport report : reports) {
									//Log.d(TAG, "Stats: " + report.toString());
									Log.d("Stats", "Stats: " + report.toString());									
								}
								vsv.postDelayed(runnableThis, 10000);
							}
						}, null);
						if (!success) {
							throw new RuntimeException("getStats() return false!");
						}
					}
				}
			};
			vsv.postDelayed(repeatedStatsLogger, 10000);
		}

		{
			logAndToast("Creating local video source...");
			capturer = getVideoCapturer();
			VideoSource videoSource = factory.createVideoSource(capturer, new MediaConstraints());
			//local media stream
			MediaStream lMS = factory.createLocalMediaStream("ARDAMS");
			VideoTrack videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
			videoTrack.addRenderer(new VideoRenderer(new VideoCallbacks(vsv, VideoStreamsView.Endpoint.LOCAL)));
			lMS.addTrack(videoTrack);
			///lMS.addTrack(factory.createAudioTrack("ARDAMSa0"));
			pc.addStream(lMS, new MediaConstraints());
		}
		logAndToast("Waiting for ICE candidates...");
	}

	// Cycle through likely device names for the camera and return the first
	// capturer that works, or crash if none do.
	private VideoCapturer getVideoCapturer() {
		String[] cameraFacing = { "front", "back" };
		int[] cameraIndex = { 0, 1 };
		int[] cameraOrientation = { 0, 90, 180, 270 };
		for (String facing : cameraFacing) {
			for (int index : cameraIndex) {
				for (int orientation : cameraOrientation) {
					String name = "Camera " + index + ", Facing " + facing
							+ ", Orientation " + orientation;
					VideoCapturer capturer = VideoCapturer.create(name);
					if (capturer != null) {
						logAndToast("Using camera: " + name);
						return capturer;
					}
				}
			}
		}
		throw new RuntimeException("Failed to open capturer");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	// Poor-man's assert(): die with |msg| unless |condition| is true.
	private static void abortUnless(boolean condition, String msg) {		
		if (!condition) {
			Log.e(TAG, msg);
			throw new RuntimeException(msg);
		}
	}

	// Log |msg| and Toast about it.
	private void logAndToast(String msg) {
		Log.d(TAG, msg);
		if (logToast != null) {
			logToast.cancel();
		}
		logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		logToast.show();
	}

	// Send |json| to the underlying AppEngine Channel.
	private void sendMessage(JSONObject json) {
		// Log.d(TAG, json.toString());
		appRtcClient.sendMessage(mDesName, json.toString());
	}

	// Put a |key|->|value| mapping in |json|.
	private static void jsonPut(JSONObject json, String key, Object value) {
		try {
			json.put(key, value);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	// Implementation detail: observe ICE & stream changes and react
	// accordingly.
	private class PCObserver implements PeerConnection.Observer {

		/**
		 * Triggered when a new ICE candidate has been found.
		 */
		@Override
		public void onIceCandidate(final IceCandidate candidate) {
			Log.d(TAG, "onIceCandidate");			
			
			runOnUiThread(new Runnable() {
				public void run() {
					
					Log.d(TAG, "send Candidate to peer");

					JSONObject data = new JSONObject();
					jsonPut(data, "label", candidate.sdpMLineIndex);
					jsonPut(data, "id", candidate.sdpMid);
					jsonPut(data, "candidate", candidate.sdp);

					JSONObject json = new JSONObject();					
					jsonPut(json, "type", "received_candidate");					
					jsonPut(json, "data", data);

					sendMessage(json);
				}
			});
		}

		@Override
		public void onError() {
			Log.e(TAG, "onError");
			runOnUiThread(new Runnable() {
				public void run() {
					throw new RuntimeException("PeerConnection error!");
				}
			});
		}

		@Override
		public void onSignalingChange(PeerConnection.SignalingState newState) {
		}

		@Override
		public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
		}

		@Override
		public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
		}

	    /** 
	     * Triggered when media is received on a new stream from remote peer. 
	     */
		@Override
		public void onAddStream(final MediaStream stream) {
			Log.d(TAG, "onAddStream");
			runOnUiThread(new Runnable() {
				public void run() {
					Log.d(TAG, "videoTracks: " + stream.videoTracks.size());
					Log.d(TAG, "audioTracks: " + stream.audioTracks.size());
					
					abortUnless(stream.audioTracks.size() == 1
							&& stream.videoTracks.size() == 1,
							"Weird-looking stream: " + stream);
					
					stream.videoTracks.get(0).addRenderer(new VideoRenderer(new VideoCallbacks(vsv, VideoStreamsView.Endpoint.REMOTE)));
				}
			});
		}
	    
		/** 
		 * Triggered when a remote peer close a stream. 
		 */
		@Override
		public void onRemoveStream(final MediaStream stream) {
			Log.d(TAG, "onRemoveStream");
			runOnUiThread(new Runnable() {
				public void run() {
					stream.videoTracks.get(0).dispose();
				}
			});
		}
	}

	// Implementation detail: handle offer creation/signaling and answer
	// setting,
	// as well as adding remote ICE candidates once the answer SDP is set.
	private class SDPObserver implements SdpObserver {

		/**
		 * Called on success of Create{Offer,Answer}().
		 */
		@Override
		public void onCreateSuccess(final SessionDescription sdp) {			
			Log.d(TAG, "onCreateSuccess: " + sdp.type);
			runOnUiThread(new Runnable() {
				public void run() {
					logAndToast("Sending SessionDescription: " + sdp.type);
					
					pc.setLocalDescription(sdpObserver, sdp);

					JSONObject data = new JSONObject();
					jsonPut(data, "type", sdp.type.canonicalForm());
					jsonPut(data, "sdp", sdp.description);
					
					JSONObject json = new JSONObject();
					jsonPut(json, "type", "received_answer");
					jsonPut(json, "data", data);					

					sendMessage(json);

					
				}
			});
		}

		/**
		 * Called on success of Set{Local,Remote}Description().
		 */
		@Override
		public void onSetSuccess() {
			Log.d(TAG, "onSetSuccess_1");
			runOnUiThread(new Runnable() {
				public void run() {
					Log.d(TAG, "onSetSuccess_2");
					if (mbInited) {
						if (pc.getRemoteDescription() != null) {
							// We've set our local offer and received & set the
							// remote
							// answer, so drain candidates.
							drainRemoteCandidates();
						}
					} else {
						if (pc.getLocalDescription() == null) {
							// We just set the remote offer, time to create our
							// answer.
							logAndToast("Creating answer");
							pc.createAnswer(SDPObserver.this, sdpMediaConstraints);
						} else {
							// Sent our answer and set it as local description;
							// drain
							// candidates.
							drainRemoteCandidates();
						}
					}
				}
			});
		}

		/**
		 * Called on error of Create{Offer,Answer}().
		 */
		@Override
		public void onCreateFailure(final String error) {
			Log.e(TAG, "onCreateFailure: " + error);
			runOnUiThread(new Runnable() {
				public void run() {					
					throw new RuntimeException("createSDP error: " + error);
				}
			});
		}

		/**
		 * Called on error of Set{Local,Remote}Description().
		 */
		@Override
		public void onSetFailure(final String error) {
			Log.e(TAG, "onSetFailure: " + error);
			runOnUiThread(new Runnable() {
				public void run() {
					throw new RuntimeException("setSDP error: " + error);
				}
			});
		}

		private void drainRemoteCandidates() {
			if (queuedRemoteCandidates == null
					|| queuedRemoteCandidates.size() == 0)
				return;
			for (IceCandidate candidate : queuedRemoteCandidates) {
				pc.addIceCandidate(candidate);
			}
			queuedRemoteCandidates = null;
		}
	}

	public void onMessage(String data) {

		try {
			JSONObject jsonObj = new JSONObject(data);
			String type = jsonObj.getString("type");

			if ("assigned_id".endsWith(type)) {
				Log.d(TAG, "assigned_id");
				// socket.id = msg.id;
			} else if ("received_offer".endsWith(type)) {
				Log.d(TAG, "received offer");
				String description = jsonObj.getJSONObject("data").getString("sdp");

				Log.d(TAG, "pc.setRemoteDescription");
				pc.setRemoteDescription(sdpObserver, new SessionDescription(
						SessionDescription.Type.OFFER, description));
				
				// pc.createAnswer(function(description) {
				// //console.log("sending answer");
				// pc.setLocalDescription(description);
				//
				//
				// socket.send(JSON.stringify({type: "received_answer", data:
				// description }));
				// }, null, mediaConstraints);
			} else if ("received_answer".endsWith(type)) {
				Log.d(TAG, "received answer");
				String description = jsonObj.getJSONObject("data").getString("sdp");
				
				Log.d(TAG, "pc.setRemoteDescription");
				pc.setRemoteDescription(sdpObserver, new SessionDescription(
						SessionDescription.Type.ANSWER, description));

				// if(!connected) {
				// pc.setRemoteDescription(new RTCSessionDescription(msg.data));
				// connected = true;
				// }

			} else if ("received_candidate".endsWith(type)) {
				Log.d(TAG, "received candidate");
				JSONObject msg = jsonObj.getJSONObject("data");
				IceCandidate candidate = new IceCandidate(
						msg.getString("id"),
						msg.getInt("label"), 
						msg.getString("candidate")
				);
				if (queuedRemoteCandidates != null) {
					queuedRemoteCandidates.add(candidate);
				} else {
					pc.addIceCandidate(candidate);
				}
			}

		} catch (Exception e) {
			Log.e(TAG, "", e);
		}

	}

	// public void onMessage(String data) {
	// try {
	// JSONObject json = new JSONObject(data);
	// String type = (String) json.get("type");
	// if (type.equals("candidate")) {
	// IceCandidate candidate = new IceCandidate(
	// (String) json.get("id"),
	// json.getInt("label"),
	// (String) json.get("candidate"));
	// if (queuedRemoteCandidates != null) {
	// queuedRemoteCandidates.add(candidate);
	// } else {
	// pc.addIceCandidate(candidate);
	// }
	// } else if (type.equals("answer") || type.equals("offer")) {
	// SessionDescription sdp = new SessionDescription(
	// SessionDescription.Type.fromCanonicalForm(type),
	// (String) json.get("sdp"));
	// pc.setRemoteDescription(sdpObserver, sdp);
	// } else if (type.equals("bye")) {
	// logAndToast("Remote end hung up; dropping PeerConnection");
	// disconnectAndExit();
	// } else {
	// throw new RuntimeException("Unexpected message: " + data);
	// }
	// } catch (JSONException e) {
	// throw new RuntimeException(e);
	// }
	// }

	// Disconnect from remote resources, dispose of local resources, and exit.
	private void disconnectAndExit() {
		synchronized (quit[0]) {
			if (quit[0]) {
				return;
			}
			quit[0] = true;
			if (pc != null) {
				pc.dispose();
				pc = null;
			}
			if (appRtcClient != null) {
				// appRtcClient.sendMessage(mDesName,"{\"type\": \"bye\"}");
				appRtcClient.disconnect();
				appRtcClient = null;
			}
			if (capturer != null)
				capturer.dispose();
			finish();
		}
	}

	// Implementation detail: bridge the VideoRenderer.Callbacks interface to
	// the
	// VideoStreamsView implementation.
	private class VideoCallbacks implements VideoRenderer.Callbacks {
		private final VideoStreamsView view;
		private final VideoStreamsView.Endpoint stream;

		public VideoCallbacks(VideoStreamsView view,
				VideoStreamsView.Endpoint stream) {
			this.view = view;
			this.stream = stream;
		}

		@Override
		public void setSize(final int width, final int height) {
			view.queueEvent(new Runnable() {
				public void run() {
					view.setSize(stream, width, height);
				}
			});
		}

		@Override
		public void renderFrame(I420Frame frame) {
			view.queueFrame(stream, frame);
		}
	}
}
