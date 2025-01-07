<#--
  Copyright (c) 2018, 2025 Oracle and/or its affiliates.

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
<#--
TODO:
 - helper for css class
 - support halign valign
 - support width attribute
 - color the header with some grey (see mkdocs-material)
-->
<#assign css_classes>
<#if attributes["role"]??>
<#list attributes["role"]?replace(","," ")?replace("  ", " ")?split(" ") as class>${class}<#sep> </#sep></#list>
</#if>
</#assign>
<#assign nowrap = attributes["nowrap"]?? && attributes["nowrap"]?boolean>
<#assign cell_classes = nowrap?then('no_wrap_cell', '')>
<#assign table_classes = nowrap?then('no_wrap_table', '') + ' ' + css_classes>
<#if title??><div class="block-title"><span>${title}</span></div></#if>
<div class="table__overflow elevation-1 ${table_classes}">
<table class="datatable table">
<colgroup>
<#list columns as column>
<col style="width: ${column.attributes["colpcwidth"]}%;">
</#list>
</colgroup>
<thead>
<#if header?? && (header?size > 0) >
<tr>
<#list header[0].cells as header>
<th>${header.text}</th>
</#list>
</tr>
</#if>
</thead>
<tbody>
<#list body as row>
<tr>
<#list row.cells as cell>
<#if cell.content?is_enumerable>
<#list cell.content as cellcontent><td class="${cell_classes}"
<#if cell.rowspan??> rowspan="${cell.rowspan}"</#if>
<#if cell.colspan??> colspan="${cell.colspan}"</#if>
>${cellcontent}</td></#list>
<#else><td class="${cell_classes}"
<#if cell.rowspan??> rowspan="${cell.rowspan}"</#if>
<#if cell.colspan??> colspan="${cell.colspan}"</#if>
>${cell.content}</td>
</#if>
</#list>
</tr>
</#list>
</tbody>
</table>
</div>
