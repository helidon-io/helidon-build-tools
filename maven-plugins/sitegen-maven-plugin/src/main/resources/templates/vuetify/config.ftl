<#--
  Copyright (c) 2018, 2022 Oracle and/or its affiliates.

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
function createConfig() {
    return {
        home: "${home.target}",
<#if releases?? && (releases?size > 0)>
        release: "${releases[0]?js_string}",
        releases: [
<#list releases as release>
            "${release?js_string}"<#sep>,</#sep>
</#list>
        ],
<#else>
        release: null,
        releases: [],
</#if>
        pathColors: {
            "*": "blue-grey"
        },
        theme: {
            primary: '${theme.primary!"#1976D2"}',
            secondary: '${theme.secondary!"#424242"}',
            accent: '${theme.accent!"#82B1FF"}',
            error: '${theme.error!"#FF5252"}',
            info: '${theme.info!"#2196F3"}',
            success: '${theme.success!"#4CAF50"}',
            warning: '${theme.warning!"#FFC107"}'
        },
        navTitle: <#if nav??>'${nav.title?js_string}'<#else>null</#if>,
        navIcon: <#if nav?? && nav.glyph?? && nav.glyph.type == "icon">'${nav.glyph.value}'<#else>null</#if>,
        navLogo: <#if nav?? && nav.glyph?? && nav.glyph.type == "image">'${nav.glyph.value}'<#else>null</#if>
    };
}

function createRoutes(){
    return [
<#list allRoutes as source>
<#assign page = pages[source] />
<#assign pageId = page.target?replace("/","-") />
<#assign pageBindings = bindings[source]?has_content />
        {
            path: '/${page.target}',
            meta: {
                h1: '${page.metadata.h1?js_string}',
                title: '${page.metadata.title?js_string}',
                h1Prefix: <#if page.metadata.h1Prefix??>'${page.metadata.h1Prefix?js_string}'<#else>null</#if>,
                description: <#if page.metadata.description??>'${page.metadata.description?js_string}'<#else>null</#if>,
                keywords: <#if page.metadata.keywords??>'${page.metadata.keywords?js_string}'<#else>null</#if>,
                customLayout: <#if customLayoutEntries[source]??>'${customLayoutEntries[source]}'<#else>null</#if>,
                hasNav: <#if navRoutes?seq_contains(source)>true<#else>false</#if>
            },
            component: loadPage('${pageId}', '${page.target}', {}<#if pageBindings>, '${page.target}_custom.js'</#if>)
        },
</#list>
        {
            path: '/', redirect: '${home.target}'
        },
        {
            path: '*', redirect: '/'
        }
    ];
}
<#macro idt i>${i}<#nested></#macro>
<#macro render_nav elt, i, has_next>
    <@idt i=i>{</@idt><#lt>
    <@idt i=i + "    ">type: '${elt.type}',</@idt><#lt>
    <#switch elt.type>
        <#case "page">
            <@idt i=i + "    ">title: '${elt.title?js_string}',</@idt><#lt>
            <@idt i=i + "    ">to: '/${elt.to}',</@idt><#lt>
            <#break>
        <#case "link">
            <@idt i=i + "    ">title: '${elt.title?js_string}',</@idt><#lt>
            <@idt i=i + "    ">href: '${elt.href}',</@idt><#lt>
            <@idt i=i + "    ">target: '${elt.target}',</@idt><#lt>
            <#break>
        <#case "header">
            <@idt i=i + "    ">title: '${elt.title?js_string}',</@idt><#lt>
            <#break>
        <#case "group">
        <#case "menu">
            <@idt i=i + "    ">title: '${elt.title?js_string}',</@idt><#lt>
            <@idt i=i + "    ">group: <#if elt.pathprefix??>'${elt.pathprefix}'<#else>null</#if>,</@idt><#lt>
        <#case "groups">
            <@idt i=i + "    ">items: [</@idt><#lt>
            <#list elt.items as it>
                <@render_nav elt=it has_next=it?has_next i=i + "        "/><#lt>
            </#list>
            <@idt i=i + "    ">],</@idt><#lt>
            <#break>
    </#switch>
    <@idt i=i + "    ">action: <#if elt.glyph??>'${elt.glyph.value}'<#else>null</#if></@idt><#lt>
    <@idt i=i>}<#if has_next>,</#if></@idt><#lt>
</#macro>
function createNav(){
    return [
  <#if nav??>
      <#list nav.items as it>
          <@render_nav elt=it has_next=it?has_next i="        "/><#lt>
      </#list>
  </#if>
    ];
}
