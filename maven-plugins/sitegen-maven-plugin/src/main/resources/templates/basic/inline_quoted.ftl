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
<#compress>
<#if id??>
<a id="${id}"></a>
</#if>
<#assign _text><@passthroughfix/></#assign>
<#switch type>
    <#case "emphasis">
        <em>${_text}</em>
        <#break>
    <#case "strong">
        <strong>${_text}</strong>
        <#break>
    <#case "monospaced">
        <code>${_text}</code>
        <#break>
    <#case "superscript">
        <sup>${_text}</sup>
        <#break>
    <#case "subscript">
        <sub>${_text}</sub>
        <#break>
    <#case "mark">
        <mark>${_text}</mark>
        <#break>
    <#case "double">
        &#8220;${_text}&#8221;
        <#break>
    <#case "single">
        &#8216;${_text}&#8217;
        <#break>
    <#default>
        ${_text}
</#switch>
</#compress>