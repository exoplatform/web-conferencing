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