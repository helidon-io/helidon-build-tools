/*
 * Copyright (c) 2018, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* global Vue */

window.allComponents["defaultView"] = {
    init: function(){
        window.scrollFix = function (hashtag) {
            let element = document.querySelector(hashtag);
            if (element) {
                // noinspection JSDeprecatedSymbols
                let initialY = window.pageYOffset;
                let elementY = initialY + element.getBoundingClientRect().top;
                let targetY = document.body.scrollHeight - elementY < window.innerHeight
                    ? document.body.scrollHeight - window.innerHeight
                    : elementY;

                // offset
                targetY -= 75;
                window.scrollTo(0, targetY);
            }
        };

        Vue.component('defaultView', {
            template: `
                <v-app light>
                    <doc-nav></doc-nav>
                    <doc-toolbar></doc-toolbar>
                    <v-content>
                    <v-container fluid>
                        <v-slide-x-reverse-transition
                            mode="out-in" 
                            @after-leave="afterLeave"
                        >
                            <router-view></router-view>
                        </v-slide-x-reverse-transition>
                    </v-container>
                    </v-content>
                    <docFooter></docFooter>
                </v-app>
            `,
            mounted () {
              if (this.$route.hash) {
                setTimeout(() => scrollFix(this.$route.hash), 2);
              }
            },
            methods: {
                afterLeave () {
                    if (this.$route.hash) {
                      setTimeout(() => scrollFix(this.$route.hash), 2);
                    } else {
                        window.scrollTo(0, 0);
                    }
                }
            }
        });
    }
};
