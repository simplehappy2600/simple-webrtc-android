# Simple WebRTC for android

webrct android client connect to [simple-webrtc-video-chat](https://github.com/simplehappy2600/simple-webrtc-video-chat)

1> run stun server, [download](http://www.stunprotocol.org)

2> modify simple-webrtc-video-chat

- replace stun server with yours, `var configuration = {"iceServers": [{"url": "stun:stun.l.google.com:19302"}]};`
- run it

3> modify simple-webrtc-android

- replace websocket address in AppRTCClient.java
- replace stun server address in AppRTCDemoActivity.java
- run it

4> open one chome window, `http://XXX/WebSocketServer/webrtc.jsp`, click "call"




