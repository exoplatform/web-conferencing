<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.Map"%>

<%
  Map<String, String> messages = (Map<String, String>) request.getAttribute("messages");
	messages.toString();
%>

<div class="NormalStyle popupContainer uiPopup settingsForm">
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

<%-- Confirmation popup --%>
<div class="UIPopupWindow uiPopup UIDragObject NormalStyle serverRemovalDialog"
	style="width: 560px; position: relative; top: auto; left: auto; margin: 0 auto 20px; z-index: 1; max-width: 100%; display: none;">
	<div class="popupHeader ClearFix">
		<a class="uiIconClose pull-right" aria-hidden="true"></a> <span class="PopupTitle popupTitle">${messages["webrtc.admin.confirmServerRemoval"]}</span>
	</div>
	<div class="PopupContent popupContent">
		<div class="form-horizontal resizable">
			<div class="popupContent">
				<span class="help-block">${messages["webrtc.admin.serverRemoveText"]}</span>
			</div>
		</div>
		<div class="uiAction uiActionBorder">
			<button class="btn removeButton" type="button">${messages["webrtc.admin.remove"]}</button>
			<button class="btn cancelButton" type="button">${messages["webrtc.admin.cancel"]}</button>
		</div>
	</div>
	<span class="uiIconResize pull-right uiIconLightGray"></span>
</div>
