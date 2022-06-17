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
<#assign image_attr = "">
<#if attributes.alt??><#assign image_attr = image_attr + "alt=\"" + attributes.alt + "\""></#if>
<#if attributes.width??><#assign image_attr = image_attr + "width=\"" + attributes.width + "\""></#if>
<#if attributes.heigh??><#assign image_attr = image_attr + "heigh=\"" + attributes.height + "\""></#if>
<#if attributes.role??><div class="${attributes.role}"><div></#if>
<#if title??><div class="block-title"><span>${title}</span></div></#if>
<v-card>
<v-card-text class="overflow-y-hidden" <#if attributes.align??>style="text-align:${attributes.align}"</#if>>
<img src="${helper.imageUri(this, attributes.target)}" ${image_attr} />
</v-card-text>
</v-card>
<#if attributes.role??></div></div></#if>
