<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- @ taglib uri="http://java.sun.com/portlet.tld" prefix="portlet" --%>

<%@ page import="java.util.Map"%>
<%-- @ page import="javax.portlet.PortletURL"%>
<%@ page import="javax.portlet.PortletMode"%>
<%@ page import="javax.portlet.PortletPreferences"--%>
<%-- <portlet:defineObjects /> --%>

<%
  Map<String, String> messages = (Map<String, String>) request.getAttribute("messages");
%>

<div id="webconferencingAdmin" class="container-fluid">
	<div class="header">
		<h3 class="titleWithBorder">${messages["webconferencing.admin.title"]}</h3>
	</div>
	<div class="content">
		<p>${messages["webconferencing.admin.info"]}</p>
		<table class="uiGrid table table-hover table-striped">
			<thead>
				<tr>
					<th>${messages["webconferencing.admin.provider"]}</th>
					<th>${messages["webconferencing.admin.description"]}</th>
					<th class="center actionContainer">${messages["webconferencing.admin.active"]}</th>
					<th class="center actionContainer">${messages["webconferencing.admin.actions"]}</th>
				</tr>
			</thead>
			<tbody>
				<tr class="callProvider template" style="display: none;">
					<td class="title"></td>
					<td class="description"></td>
					<td class="center actionContainer active">
						<label class="switch"><input checked="checked" type="checkbox" /> <span class="slider round btn-primary">&nbsp;</span> </label>
					</td>
					<td class="center actionContainer actions">
						<a class="actionIcon settings" data-original-title="Settings" data-placement="bottom" data-toggle="modal" href="javascript: void(0);" rel="tooltip" style="display: none;">
							<i class="uiIconSetting uiIconBlue"><span>&nbsp;</span></i>
						</a>
						<a class="actionIcon permissions" data-original-title="Lock" data-placement="bottom" rel="tooltip" style="display: none;">
							<i class="uiIconLockMedium uiIconBlue"><span>&nbsp;</span></i>
						</a>
					</td>
				</tr>
			</tbody>
		</table>
	</div>
</div>