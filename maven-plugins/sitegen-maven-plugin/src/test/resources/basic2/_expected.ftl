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
<#-- @ftlvariable name="basedir" type="java.lang.String" -->
<#--noinspection HtmlRequiredLangAttribute-->
<html>
<head>
<title>Example Manual</title>
</head>
<body>
<h1>Example Manual</h1>
<p>This is a user manual for an example project.</p>
<h2>Introduction</h2>
<p>This project does something.
We just haven&#8217;t decided what that is yet.</p>
<h2>Source Code</h2>
<pre>
<code>public boolean contains(String haystack, String needle) {
    return haystack.contains(needle);
}</code>
</pre>
<p>This page was built by the following command:</p>
<pre>
$ mvn
</pre>
<h2>Images</h2>
<img src="./images/sunset.jpg" alt="sunset" />
<h2>Attributes</h2>
<dl>
<dt>asciidoctor-version</dt>
<dd>
2.0.20
</dd>
<dt>safe-mode-name</dt>
<dd>
unsafe
</dd>
<dt>docdir</dt>
<dd>
${basedir}/target/test-classes/basic2
</dd>
<dt>docfile</dt>
<dd>
${basedir}/target/test-classes/basic2/example-manual.adoc
</dd>
<dt>imagesdir</dt>
<dd>
./images
</dd>
</dl>
<h2>Includes</h2>
<div>
<span>example:</span>
<span>include::sub-dir/_b.adoc[]</span>
<p>content from <em>src/docs/asciidoc/sub-dir/_b.adoc</em>.</p>
<div>
<span>example:</span>
<span>include::_c.adoc[]</span>
<p>content from <em>src/docs/asciidoc/sub-dir/c.adoc</em>.</p>
</div>
</div>
<div>
<span>warning</span>
Includes can be tricky!
</div>
</body>
</html>
