
<div class="VuetifyApp">
  <div id="webconferencingAdmin">
    <script>
      		require(['SHARED/webConferencingPortlet','PORTLET/webconferencing/webConferencingAdminPortlet'], function(webConferencingPortlet,webConferencingAdminPortlet) {
      			webConferencingPortlet.start('<%=request.getAttribute("user")%>','<%=request.getAttribute("context")%>');
      			webConferencingAdminPortlet.init();
    	    });
      	</script>
  </div>
</div>
