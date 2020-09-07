<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import="java.util.Map"%>

<%
  Map<String, String> messages = (Map<String, String>) request.getAttribute("messages");
%>

<div id="webconferencingAdmin">
	<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light titleWithBorder">
	</div>
	<div class="v-skeleton-loader__bone blockProvidersInner">
	  <div class="providersItem providersItem--name">
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton tableHeader">
		</div>
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonText">
		</div>
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonText">
		</div>
	  </div>
	  <div class="providersItem providersItem--description">
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton tableHeader">
		</div>
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonText">
		</div>
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonText">
		</div>
	  </div>
	  <div class="providersItem providersItem--active">
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton tableHeader">
		</div>
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonSwitcher">
		</div>
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonSwitcher">
		</div>
	  </div>
	  <div class="providersItem providersItem--permissions">
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton tableHeader">
		</div>
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonSettings">
		</div>
		<div class="v-skeleton-loader  v-skeleton-loader--is-loading theme--light providersSkeleton skeletonSettings">
		</div>
	  </div>
	</div>
  </div>




<!-- <div id="webconferencingAdmin" class="container-fluid">
	<h3 class="titleWithBorder">${messages["webconferencing.admin.title"]}</h3>
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
						<label class="switch">
							<input type="checkbox" checked="checked" />
							<span class="slider"></span>
						</label>
					</td>
					<td class="center actionContainer actions">
						<a class="actionIcon settings" data-toggle="tooltip" data-original-title="Settings" data-placement="top" href="javascript: void(0);" rel="tooltip" style="display: none;">
							<i class="uiIconSetting"></i>
						</a>
						<a class="actionIcon permissions" data-toggle="tooltip" data-original-title="Lock" data-placement="top" href="javascript: void(0);" rel="tooltip" style="display: none;">
							<i class="uiIconLockMedium"></i>
						</a>
					</td>
				</tr>
			</tbody>
		</table>
	</div>
</div> -->