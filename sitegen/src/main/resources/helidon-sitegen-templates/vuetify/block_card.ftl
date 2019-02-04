<#--
  Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
 
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
<#compress>
<v-flex xs12 sm4 lg3>
<v-card>
<#if attributes["icon"]?? || attributes["image"]??>
<v-layout align-center justify-center class="pa-5">
<#if attributes["icon"]??>
<v-avatar size="150px">
<v-icon class="xxx-large">${attributes["icon"]}</v-icon>
</v-avatar>
<#elseif attributes["image"]??>
<img width="150" height="150" src="${imageUri(attributes["image"])}"/>
</#if>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
</#if>
<#if attributes["title"]??>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">${attributes["title"]}</span>
</v-card-title>
</#if>
<v-card-text class="caption">
${content}
</v-card-text>
</v-card>
</v-flex>
</#compress>