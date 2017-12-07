<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map"%>
<%
  Map<String, String> messages = (Map<String, String>) request.getAttribute("messages");
%>
<div class="incomingCall" title="${messages[" webrtc.incomingCall"]}" style="display: none;">
	<a class="uiIconClose" aria-hidden="true">&nbsp;</a>
	<div class="callMessage">
		<div class="avatar">
			<a target="_blank" href="" class="avatarMedium"> <img src="/eXoSkin/skin/images/system/UserAvtDefault.png" /></a>
		</div>
		<p>
		<div class="messageText"></div>
		</p>
	</div>
	<div class="callActions">
		<button class="btn btn-primary answerButton" type="button">
			<i class="uiIconVideo">&nbsp;</i>${messages["webrtc.answer"]}
		</button>
		<button class="btn declineButton" type="button">
			<i class="icon-phone-hang-uphamburger">&nbsp;</i>${messages["webrtc.decline"]}
		</button>
	</div>
</div>