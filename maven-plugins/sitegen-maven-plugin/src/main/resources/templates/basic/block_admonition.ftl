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
<div>
<span>${attributes["name"]}</span>
<#if attributes["caption"]??><span><i>${attributes["caption"]}</i></span></#if>
<#if title??><span><strong>${title}</strong></span></#if>
${content}
</div>
</#compress>