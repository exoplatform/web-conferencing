<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map"%>

<%
  Map<String, String> messages = (Map<String, String>) request.getAttribute("messages");
	messages.toString();
%>

<div class="NormalStyle popupContainer uiPopup">
	<div class="popupHeader ClearFix">
		<a aria-hidden="true" class="uiIconClose pull-right">&nbsp;</a> <span class="PopupTitle popupTitle">${messages["webrtc.admin.title"]}</span>
	</div>

	<div class="PopupContent popupContent">
		<p class="title">
			${messages["webrtc.admin.servers"]}&nbsp;&nbsp;<i class="uiIconInformation uiIconBlue" data-placement="bottom" data-toggle="tooltip"
				title="${messages["webrtc.admin.serversTip"]}">&nbsp;</i>
		</p>

		<div class="iceServers">
			<div class="control-group iceServer" style="display: none;">
				<div class="control-group urlsGroup">
					<label class="control-label" for="url">${messages["webrtc.admin.url"]} :</label>
					<div class="urlGroup">
						<input name="url" placeholder="${messages["webrtc.admin.serverUrl"]}" type="text" />&nbsp;&nbsp;<i class="uiIconTrash uiIconLightGray">&nbsp;</i><i class="uiIconPlus uiIconLightGray">&nbsp;</i>
					</div>
				</div>
				<div class="credentialsGroup">
					<div class="enabler">
						<div class="control-group">
							<span class="uiCheckbox"> <input type="checkbox" class="checkbox"><span>${messages["webrtc.admin.credential"]}</span></span>
						</div>
					</div>
					<div class="credentials" style="display: none;">
						<div class="control-group">
							<label class="control-label" for="username">${messages["webrtc.admin.username"]}</label>
							<div class="controls">
								<input name="username" type="text" placeholder="${messages["webrtc.admin.username"]}...">
							</div>
						</div>
						<div class="control-group">
							<label class="control-label" for="credential">${messages["webrtc.admin.credential"]}</label>
							<div class="controls">
								<input name="credential" type="text" placeholder="${messages["webrtc.admin.credential"]}...">
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
		
		<div class="control-group buttonsGroup">
			<div class="uiAction">
				<button class="btn saveButton" type="button">${messages["webrtc.admin.save"]}</button>
				<button class="btn cancelButton" type="button">${messages["webrtc.admin.cancel"]}</button>
			</div>
		</div>

	</div>
</div>
