<%@ page language="java"%>
<%--  contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" --%>
<%-- @ taglib uri="http://java.sun.com/portlet.tld" prefix="portlet" --%>

<%@ page import="java.util.ResourceBundle"%>
<%@ page import="javax.portlet.PortletURL"%>
<%@ page import="javax.portlet.PortletMode"%>
<%@ page import="javax.portlet.PortletPreferences"%>

<%-- <portlet:defineObjects /> --%>

<%
  // TODO It's sample properties...
  //String navigation_uri = request.getParameter("navigation_uri");
  //String page_name = request.getParameter("page_name");
  //String site_type = request.getParameter("site_type");
%>

<div id="webconferencingAdmin" class="container-fluid">
	<div class="header">
		<h3 class="titleWithBorder">Web Conferencing Administration</h3>
	</div>
	<div class="content">
		<p>Web Conferencing can be handled by different providers. You can enable the provider you need and configure settings.</p>
		<table class="uiGrid table table-hover table-striped">
			<thead>
				<tr>
					<th>Provider</th>
					<th>Description</th>
					<th class="center actionContainer">Active</th>
					<th class="center actionContainer">Actions</th>
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