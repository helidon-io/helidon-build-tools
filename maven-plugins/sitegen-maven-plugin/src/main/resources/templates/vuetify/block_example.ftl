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
<#if title??>
<#assign _title = title>
<#elseif getString("caption") ??>
<#assign _title = getString("caption")>
</#if>
<#assign _content>
<v-card flat color="grey lighten-3" <#if id??>id="${id}"</#if> class="card__example">
<v-card-text>${content}</v-card-text>
</v-card>
</#assign>
<#if _title ??>
<v-card>
<v-toolbar :color="$store.state.currentColor" flat dense dark>
<v-btn dark icon to="#example">
<v-icon>link</v-icon>
</v-btn>
<span class="title white--text layout align-end">${_title}</span>
<#if attributes["github-url"]??>
<v-spacer/>
<v-tooltip bottom>
<v-btn icon dark flat tag="a" slot="activator"
       v-bind:href="'${attributes["github-url"]}'"
       target="_blank">
<v-icon>fa-github</v-icon>
</v-btn>
<span>View on Github</span>
</v-tooltip>
</#if>
</v-toolbar>
<v-card-text>
${_content}
</v-card-text>
</v-card>
<#else>
${_content}
</#if>
