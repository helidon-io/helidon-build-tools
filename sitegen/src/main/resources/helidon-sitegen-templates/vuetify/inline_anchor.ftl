<#--
  Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
 
      http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 -->
<#-- TODO use a helper method to do the logic below -->
<#assign page = document.attributes["page"]/>
<#assign pages = document.attributes["pages"]/>
<#if text?has_content>
<#assign _text = text />
<#else>
<#assign _text = "" />
</#if>
<#compress>
<#switch type>
<#case "xref">
<#if attributes["path"]??>
<#assign source = attributes["path"]?replace(".html",".adoc")>
<#else>
<#assign source = attributes["refid"]>
</#if>
<#if pages["/" + source]??>
<#assign target = pages["/" + source].target>
<#else>
<#assign target = "">
</#if>
<#assign hash = attributes["fragment"]>
<#if (hash?? && !(target?has_content)) || (page.target == target)>
<#-- link to an anchor on the same page -->
<router-link to="#${hash}" @click.native="this.scrollFix('#${hash}')">${_text}</router-link>
<#elseif hash?? && target?? && hash != source>
<#-- link to an anchor on a different page -->
<router-link :to="{path: '${target}', hash: '#${hash}'}">${_text}</router-link>
<#else>
<#-- link to a page -->
<router-link to="${target}">${_text}</router-link>
</#if>
<#break>
<#case "ref">
<a href="${source}">${_text}</a>
<#break>
<#case "bibref">
<a id="${source}">${_text}</a>
<#break>
<#default>
<a id="${id???then(id,"")}"
   title="${attributes["title"]???then(attributes["title"], "")}"
   target="${attributes["window"]???then( attributes["window"],"_blank")}"
   href="${target}">${_text}</a>
</#switch>
</#compress>