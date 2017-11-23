<%@ page language="java" %> <%--  contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" --%>
<%-- @ taglib uri="http://java.sun.com/portlet.tld" prefix="portlet" --%>

<%@ page import="java.util.ResourceBundle"%>
<%@ page import="javax.portlet.PortletURL" %>
<%@ page import="javax.portlet.PortletMode" %>
<%@ page import="javax.portlet.PortletPreferences" %>

<%-- <portlet:defineObjects /> --%>

<%
	// TODO It's sample properties...
  //String navigation_uri = request.getParameter("navigation_uri");
  //String page_name = request.getParameter("page_name");
  //String site_type = request.getParameter("site_type");
%>

	<!-- Admin scripts -->
	<script type="text/javascript">
		// TODO add nasty stuff here, or use right place in webconferencing-admin-portlet.js
	</script>	

	<div id="webconferencing-admin-container">
		<div class="title"><h1></h1>
		</div>
		<div class="table">
		</div>
		<div class="controls">
			<button class="close">Close</button>			
		</div>
	</div>
