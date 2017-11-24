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

		    /*
             * OPEN HIDE POPUPs
             * String id; contain the Id of element to duplicate
             * INT inputID; Contain the number of elements
             */

            function addInput(id, inputID) {

                var domToAdd = jQuery("#" + id + " > .group-container" + inputID).html();
                newId = inputID + 1;
                console.log(newId);
                jQuery("#" + id).append("<div class='group-container" + newId + "'>" + domToAdd + "</div>");
                jQuery("#" + id + " > .group-container" + newId + " > input").attr("id", "url" + newId);
                jQuery("#" + id + " > .group-container" + newId + " > i.uiIconPlus ").attr("onclick", "addInput('urlGroup', " + newId + ")");

                if (jQuery("#" + id + " > .group-container" + newId + " > i.uiIconTrash").length > 0) {
                    jQuery("#" + id + " > .group-container" + newId + " > i.uiIconTrash").attr("onclick", "removeInput('" + id.toString() + "', " + newId + ")");
                } else {
                    jQuery("<i class='uiIconTrash uiIconLightGray' onclick='removeInput('" + id.toString() + "', " + newId + ")'>&nbsp;</i>").insertBefore("#" + id + " > .group-container" + newId + " > i.uiIconPlus ");
                }
            }

            function removeInput(id, inputID) {
                jQuery("#" + id + " .group-container" + inputID).remove();
            }



            require(['SHARED/jquery'], function (jQuery) {
                jQuery(document).ready(function () {
                    /* OPEN HIDE POPUPs*/
                    jQuery("#webConfPopupClique").click(function () {
                        jQuery(".maskPopup").removeClass("hide");
                    });

                    jQuery("#webConfPopup .uiIconClose.pull-right").click(function () {
                        jQuery(".maskPopup").addClass("hide");
                    });

                    /* TOOLTIP */
                    jQuery('[data-toggle="tooltip"]').tooltip();

                    jQuery('#check').click(function(){
                        if (jQuery('#check').is(":checked")){
                            jQuery(".toggleInputs").removeClass("hideInputs");
                        }else{
                            jQuery(".toggleInputs").addClass("hideInputs");
                        }
                    });

                });

            });

	</script>

<div class="container-fluid webconferencing">
    <div class="header">
        <h3 class="titleWithBorder">Web Conferencing Administration</h3>
    </div>

    <div class="content">
        <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur</p>

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
                <tr>
                    <td>WebRTC</td>
                    <td>Lorem ipsum dolor sit amet, vel perpetua adolescens no. Hinc facilis incorrupte mea</td>
                    <td class="center actionContainer"><label class="switch"><input checked="checked" type="checkbox" /> <span class="slider round btn-primary">&nbsp;</span> </label></td>
                    <td class="center actionContainer"><a class="actionIcon" data-original-title="Setting" data-placement="bottom" data-toggle="modal" href="#webConfPopup" id="webConfPopupClique" rel="tooltip"><i class="uiIconSetting uiIconBlue"><span>&nbsp;</span></i> </a> <a class="actionIcon" data-original-title="Lock" data-placement="bottom" rel="tooltip"> <i class="uiIconLockMedium uiIconBlue"><span>&nbsp;</span></i> </a></td>
                </tr>
                <tr>
                    <td>Skype</td>
                    <td>Adhuc mundi te sed, in porro reprehendunt vel. Cum augue persius vituperata ad,</td>
                    <td class="center actionContainer"><label class="switch"><input type="checkbox" /> <span class="slider round btn-primary">&nbsp;</span> </label></td>
                    <td class="center actionContainer"><a class="actionIcon" data-original-title="Setting" data-placement="bottom" data-toggle="modal" href="#webConfPopup" rel="tooltip"><i class="uiIconSetting uiIconBlue"><span>&nbsp;</span></i> </a> <a class="actionIcon" data-original-title="Lock" data-placement="bottom" rel="tooltip"> <i class="uiIconLockMedium uiIconBlue"><span>&nbsp;</span></i> </a></td>
                </tr>
                <tr>
                    <td>Skype for business</td>
                    <td>Eu propriae corrumpit his, an meis democritum nam</td>
                    <td class="center actionContainer"><label class="switch"><input checked="checked" type="checkbox" /> <span class="slider round btn-primary">&nbsp;</span> </label></td>
                    <td class="center actionContainer"><a class="actionIcon" data-original-title="Setting" data-placement="bottom" data-toggle="modal" href="#webConfPopup" rel="tooltip"><i class="uiIconSetting uiIconBlue"><span>&nbsp;</span></i> </a> <a class="actionIcon" data-original-title="Lock" data-placement="bottom" rel="tooltip"> <i class="uiIconLockMedium uiIconBlue"><span>&nbsp;</span></i> </a></td>
                </tr>
            </tbody>
        </table>
    </div>

    <div class="maskPopup hide" id="webConfPopup">
        <div class="NormalStyle popupContainer uiPopup">
            <div class="popupHeader ClearFix"><a aria-hidden="true" class="uiIconClose pull-right">&nbsp;</a> <span class="PopupTitle popupTitle">Popup header</span></div>

            <div class="PopupContent popupContent">
                <p class="title">STUN / TURN servers&nbsp;&nbsp;<i class="uiIconInformation uiIconBlue" data-placement="bottom" data-toggle="tooltip" title="Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.">&nbsp;</i></p>

                <div class="control-group">
                    <label class="control-label" for="url">URL :</label>
                    <div id="urlGroup">
                        <div class="group-container0"><input id="url" name="label" placeholder="input text..." type="text" />&nbsp;&nbsp; <i class="uiIconPlus uiIconLightGray" onclick="addInput('urlGroup', 0)">&nbsp;</i></div>
                    </div>
                </div>
                <div class="control-group">
                    <label class="control-label" for="url">URL :</label>
                    <div id="urlGroup2">
                        <div class="group-container0">
                            <input id="url" name="label" placeholder="input text..." type="text" />&nbsp;&nbsp; <i class="uiIconPlus uiIconLightGray" onclick="addInput('urlGroup2', 0)">&nbsp;</i>
                        </div>
                    </div>
                    <div id="ContainerUrlGroup">
                        <label class="control-label" for="url">&nbsp;</label>
                        <div id="urlGroup">
                            <div class="control-group">
                                <span class="uiCheckbox">
                                    <input type="checkbox" id="check" class="checkbox">
                                    <span>Credentials</span>
                                </span>
                            </div>
                        </div>
                        <div class="toggleInputs hideInputs">
                            <div class="control-group">
                                <label class="control-label" for="userName">UserName</label>
                                <div class="controls"><input name="userName" type="text" id="userName" value=""></div>
                            </div>
                            <div class="control-group">
                                <label class="control-label" for="credentials">Credentials</label>
                                <div class="controls"><input name="credentials" type="text" id="credentials" value=""></div>
                            </div>
                        </div>
                    </div>
                </div>

                <!--
                <div>
                    <span class="uiCheckbox">
                        <input type="checkbox" class="checkbox">
                        <span>Credentials</span>
                    </span>
                </div>
                <div class="control-group">
                    <label class="control-label" for="userName">UserName</label>
                    <div class="controls"><input name="userName" type="text" id="userName" value=""></div>
                </div>
                <div class="control-group">
                    <label class="control-label" for="credentials">Credentials</label>
                    <div class="controls"><input name="credentials" type="text" id="credentials" value=""></div>
                </div>
                <!-- -->






            </div>
        </div>

    </div>
</div>
