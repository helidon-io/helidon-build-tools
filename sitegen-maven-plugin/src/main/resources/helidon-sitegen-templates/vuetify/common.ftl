<#--
  Copyright (c) 2019, 2023 Oracle and/or its affiliates.
 
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
<#macro renderLink link>
<#-- macro arguments cannot be null, anything else than hash is treated as null -->
<#if link?is_hash>
<#switch link.type>
<#case "xref_anchor_self">
<#-- link to an anchor on the same page -->
<router-link to="#${link.hash}" @click.native="this.scrollFix('#${link.hash}')"><#nested></router-link>
<#break>
<#case "xref_anchor">
<#-- link to an anchor on a different page -->
<router-link :to="{path: '${link.target}', hash: '#${link.hash}'}"><#nested></router-link>
<#break>
<#case "xref">
<#-- link to a page -->
<router-link to="${link.target}"><#nested></router-link>
<#break>
<#case "ref">
<#-- link to an external page -->
<a href="${source}"><#nested></a>
<#break>
<#case "bibref">
<#-- anchor -->
<a id="${source}"><#nested></a>
<#break>
<#default>
<a id="${link.id}" title="${link.title}" target="${link.window}" href="${link.target}"><#nested></a>
</#switch>
</#if>
</#macro>