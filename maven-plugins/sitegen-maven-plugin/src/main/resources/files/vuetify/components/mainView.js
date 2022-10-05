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

/* global config, Vue */

window.allComponents["mainView"] = {
    init: function(){
        Vue.component('mainView', {
            template: `
                <v-fade-transition mode="out-in">
                    <component :is="component">
                    <slot></slot>
                    </component>
                </v-fade-transition>
            `,
            created() {
                // noinspection DuplicatedCode
                const metaData = this.$route.meta || {};
                const section = this.$route.path.split('/');
                let h1 = metaData.h1;
                let h1Prefix = null;

                if (section.length > 2) {
                    h1Prefix = capitalize(section[1]);
                }

                // noinspection JSUnresolvedVariable
                if (metaData.h1Prefix) {
                    h1Prefix = metaData.h1Prefix;
                }

                if (h1Prefix) {
                    h1 = `<div class="hidden-sm-and-down">${h1Prefix} &nbsp;&mdash;&nbsp;&nbsp;</div><div>${h1}</div>`;
                }
                document.title = `${metaData.title}`;
                // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                this.$store.commit('vuetify/H1', h1);
            },
            computed: {
                component() {
                    // noinspection JSUnresolvedVariable
                    if (this.$route.meta.customLayout !== null)
                        { // noinspection JSUnresolvedVariable
                            return this.$route.meta.customLayout;
                        }
                    return 'defaultView';
                }
            },
            watch: {
                '$route'() {
                    this.setMeta();
                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('vuetify/COLOR', this.getColor(this.$route.path));
                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('vuetify/NAVGROUP', this.getNavGroup(this.$route.path));
                    this.getPrevNext();
                }
            },
            mounted() {
                // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                this.$store.commit('vuetify/COLOR', this.getColor(this.$route.path));
                this.getPrevNext();
            },
            methods: {
                setMeta() {
                    if (typeof document === 'undefined') return;
                    // noinspection DuplicatedCode
                    const metaData = this.$route.meta || {};
                    const section = this.$route.path.split('/');
                    let h1 = metaData.h1;
                    let h1Prefix = null;

                    if (section.length > 2) {
                        h1Prefix = capitalize(section[1]);
                    }

                    // noinspection JSUnresolvedVariable
                    if (metaData.h1Prefix) {
                        h1Prefix = metaData.h1Prefix;
                    }

                    if (h1Prefix) {
                        h1 = `${h1Prefix} &nbsp;&mdash;&nbsp; ${h1}`;
                    }

                    document.title = `${metaData.title}`;
                    document.querySelector('meta[name="description"]').setAttribute('content', metaData.description);
                    // noinspection JSUnresolvedVariable
                    document.querySelector('meta[name="keywords"]').setAttribute('content', metaData.keywords);

                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('vuetify/H1', h1);
                },
                getNavGroup(path){
                    return path.split('/')[1];
                },
                getColor(path) {
                    const section = path.split('/');
                    let color
                    if (section !== 'undefined' && section.length > 1) {
                        // noinspection JSUnresolvedVariable
                        color = config.pathColors["/" + section[1]];
                    }
                    if(color === undefined){
                        // noinspection JSUnresolvedVariable
                        color = config.pathColors['*'];
                    }
                    if (color === undefined) {
                        color = 'primary';
                    }
                    return color;
                },
                getNext(currentIndex) {
                    // noinspection JSUnresolvedVariable
                    for(let i=currentIndex ; i < this.$router.options.routes.length - 1 ; i++){
                        // noinspection JSUnresolvedVariable
                        let _next = this.$router.options.routes[i + 1];
                        // noinspection JSUnresolvedVariable
                        if(_next.meta && _next.meta.hasNav === true){
                            return _next;
                        }
                    }
                    return null;
                },
                getPrevNext() {
                    // noinspection JSUnresolvedVariable
                    const currentIndex = this.$router.options.routes.findIndex(r => r.path === this.$route.path);
                    // noinspection JSUnresolvedVariable
                    const previous = currentIndex > 0 ? this.$router.options.routes[currentIndex - 1] : null;
                    const next = this.getNext(currentIndex);

                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('vuetify/NEXT', {
                        name: next ? next.meta && next.meta.h1 : null,
                        color: next ? this.getColor(next.path) : null,
                        route: next ? next.path : null
                    });

                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('vuetify/PREVIOUS', {
                        name: previous ? previous.meta && previous.meta.h1 : null,
                        color: previous ? this.getColor(previous.path) : null,
                        route: previous && previous.meta.customLayout === null ? previous.path : null
                    });
                },
                meta(obj) {
                    this.title = obj.h1;
                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('vuetify/TITLE', obj.title);
                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('vuetify/DESCRIPTION', obj.description);
                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('vuetify/KEYWORDS', obj.keywords);
                }
            }
        });
    }
};
