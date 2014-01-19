# Simple WebRTC for android

webrct android client connect to [simple-webrtc-video-chat](https://github.com/simplehappy2600/simple-webrtc-video-chat)

1> if your network cannot reach google's stun server:

- run stun server, [download](http://www.stunprotocol.org)
- modify [simple-webrtc-video-chat](https://github.com/simplehappy2600/simple-webrtc-video-chat), 
  replace stun server address in app.js
- modify [simple-webrtc-android](https://github.com/simplehappy2600/simple-webrtc-android), replace stun server address in AppRTCDemoActivity.java

2> run [simple-webrtc-video-chat](https://github.com/simplehappy2600/simple-webrtc-video-chat), mvn jetty:run

3> modify [simple-webrtc-android](https://github.com/simplehappy2600/simple-webrtc-android)

- replace websocket address in AppRTCClient.java
- run it

4> open one chome window, `http://XXX/WebSocketServer/webrtc.jsp`, click "call"




