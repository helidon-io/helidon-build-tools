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
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="description" content="${metadata.description!""}">
  <meta name="keywords" content="${metadata.keywords!""}">
  <title>${metadata.title}</title>
  <link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css" rel="stylesheet" type="text/css">
  <link href='https://fonts.googleapis.com/css?family=Roboto:300,400,500,700|Material+Icons' rel="stylesheet" type="text/css">
  <link href="https://unpkg.com/vuetify@0.17.7/dist/vuetify.min.css" rel="stylesheet" type="text/css">
  <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.12.0/styles/tomorrow.min.css">
  <link rel="stylesheet" href="/css/helidon-sitegen.css" type="text/css">
<#list header.stylesheets as stylesheet>
  <link rel="stylesheet" href="${stylesheet.location}" type="text/css">
</#list>
<#if header.favicon??>
  <link rel="icon" type="${header.favicon.type}" href="${header.favicon.location}">
</#if>
</head>
<body>

  <div id="app">
    <main-view></main-view>
  </div>

  <!--
    components
  -->
  <script>
      window.allComponents = {};
      window.allCustoms = {};
  </script>
  <script src="/components/mainView.js"></script>
  <script src="/components/docView.js"></script>
  <script src="/components/defaultView.js"></script>
  <script src="/components/docNav.js"></script>
  <script src="/components/docToolbar.js"></script>
  <script src="/components/docFooter.js"></script>
  <script src="/components/markup.js"></script>

  <!--
    main
  -->
  <script src="/libs/superagent.js"></script>
  <script src="/main/utils.js"></script>
  <script src="/main/config.js"></script>
  <script src="/main/app.js"></script>

  <!--
    libs
  -->
  <script src="https://unpkg.com/vuetify@0.17.7/dist/vuetify.js"></script>
  <script src="https://unpkg.com/vue@2.5.13/dist/vue.js"></script>
  <script src="https://unpkg.com/vue-router@3.0.1/dist/vue-router.js"></script>
  <script src="https://unpkg.com/vuex@3.0.1/dist/vuex.js"></script>
  <script src="https://unpkg.com/lunr@2.1.6/lunr.js"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.12.0/highlight.min.js"></script>
  <script src="/libs/vuex-router-sync.js"></script>

  <!--
    entrypoint
  -->
  <script>
    main();
  </script>
</body>
</html>
