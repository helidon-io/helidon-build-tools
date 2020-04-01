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
function createConfig() {
    return {
        home: "${home.target?remove_beginning("/")}",
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
        navTitle: <#if navigation??>'${navigation.title?js_string}'<#else>null</#if>,
        navIcon: <#if navigation?? && navigation.glyph?? && navigation.glyph.type == "icon">'${navigation.glyph.value}'<#else>null</#if>,
        navLogo: <#if navigation?? && navigation.glyph?? && navigation.glyph.type == "image">'${navigation.glyph.value}'<#else>null</#if>
    };
}

function createRoutes(){
    return [
<#list routeEntries as source>
<#assign page = pages[source] />
<#assign pageId = page.target?remove_beginning("/")?replace("/","-") />
<#assign pageBindings = bindings[source]?has_content />
        {
            path: '${page.target}',
            meta: {
                h1: '${page.metadata.h1?js_string}',
                title: '${page.metadata.title?js_string}',
                parentTitle: <#if page.metadata.parentTitle??>'${page.metadata.parentTitle?js_string}'<#else>null</#if>,
                description: <#if page.metadata.description??>'${page.metadata.description?js_string}'<#else>null</#if>,
                keywords: <#if page.metadata.keywords??>'${page.metadata.keywords?js_string}'<#else>null</#if>,
                customLayout: <#if customLayoutEntries[source]??>'${customLayoutEntries[source]}'<#else>null</#if>,
                hasNav: <#if navRouteEntries?seq_contains(source)>true<#else>false</#if>
            },
            component: loadPage('${pageId}', '${page.target}', {}<#if pageBindings>, '${page.target?remove_beginning("/")}_custom.js'</#if>)
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

function createNav(){
    return [
<#if navigation??>
<#list navigation.items as navitem>
<#if navitem.isgroup>
        { header: '${navitem.title?js_string}' },
<#list navitem.items as groupitem>
        {
            title: '${groupitem.title?js_string}',
            action: <#if groupitem.glyph??>'${groupitem.glyph.value}'<#else>null</#if>,
<#if groupitem.islink>
            href: '${groupitem.href}',
            target: '_blank'
<#elseif groupitem.isgroup>
            group: <#if groupitem.pathprefix??>'${groupitem.pathprefix}'<#else>null</#if>,
            items: [
<#list groupitem.items as subgroupitem>
<#if subgroupitem.islink>
                { href: '${subgroupitem.href}', title: '${subgroupitem.title?js_string}' }<#sep>,</#sep>
</#if>
</#list>
            ]
</#if>
        }<#if navitem?has_next || groupitem?has_next>,</#if>
</#list>
<#elseif navitem.islink>
        {
            title: '${navitem.title?js_string}',
            action: <#if navitem.glyph??>'${navitem.glyph}'<#else>null</#if>,
            href: '${navitem.href}',
            target: '_blank'
        }<#if navitem?has_next>,</#if>
</#if>
<#sep>
        { divider: true },
</#sep>
</#list>
</#if>
    ];
}