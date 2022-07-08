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

/* global Vue, navItems, config */

window.allComponents['docNav'] = {
    init: function () {
        const navItem = Vue.component('navItem', {
            template: `
                <v-list-tile
                    ripple
                    rel="noopener"
                    v-bind="{ to: item.to, href: item.href }"
                    @click.native="onFocus"
                    v-bind:disabled="item.disabled"
                    v-bind:target="item.target"
                >
                    <v-list-tile-action
                        v-if="item.action"
                    >
                        <v-icon dark>{{ item.action }}</v-icon>
                    </v-list-tile-action>
                    <v-list-tile-content>
                        <v-list-tile-title>{{ item.title }}</v-list-tile-title>
                    </v-list-tile-content>
                </v-list-tile>`,
            props: ['item'],
            methods: {
                onFocus: function() {
                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('sitegen/ISSEARCHING', false);
                }
            }
        })

        const navMenu = Vue.component('navMenu', {
            template: `
                <div>
                    <v-list-group
                        v-if="item.type === 'menu'"
                        v-model="isActive"
                        v-bind:group="item.group"
                    >
                        <v-list-tile
                            ripple
                            slot="item"
                            @click.native="toggle"
                        >
                            <v-list-tile-action v-if="item.action">
                                <v-icon dark>{{ item.action }}</v-icon>
                            </v-list-tile-action>
                            <v-list-tile-content> 
                                <v-list-tile-title>{{ item.title }}</v-list-tile-title>
                            </v-list-tile-content>
                            <v-list-tile-action>
                                <v-icon dark>keyboard_arrow_down</v-icon>
                            </v-list-tile-action>
                        </v-list-tile>
                        <nav-item
                            v-for="(sub_item, i) in item.items"
                            v-bind:item="sub_item"
                            v-bind:key="i"
                        />
                    </v-list-group>
                    <v-list-tile 
                        v-else-if="item.type === 'header'"
                        disabled
                    >
                        <v-list-tile-content>
                            <v-list-tile-title>{{ item.title }}</v-list-tile-title>
                        </v-list-tile-content>
                    </v-list-tile>
                    <nav-item
                        v-else
                        v-bind:item="item"
                    />
                </div>`,
            components: {
                'navItem': navItem
            },
            props: ['item'],
            data: function () {
                return {
                    isActive: false
                }
            },
            created: function () {
                // noinspection JSUnresolvedVariable,JSUnresolvedFunction
                this.$router.afterEach(this.toggleRoute)
            },
            mounted: function () {
                this.toggleRoute(this.$route)
            },
            methods: {
                toggle: function () {
                    this.isActive = !this.isActive
                    this.onFocus()
                },
                toggleRoute: function (to) {
                    // noinspection JSUnresolvedVariable
                    const path = this.item.group || this.item.to
                    if (to.path.startsWith(path)) {
                        this.isActive = true
                        this.onFocus()
                        // noinspection JSUnresolvedFunction
                        this.$emit('onActive')
                    } else {
                        this.isActive = false
                    }
                },
                onFocus: function() {
                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('sitegen/ISSEARCHING', false);
                }
            }
        })

        // noinspection HtmlUnknownAttribute
        const navGroup = Vue.component('navGroup', {
            template: `
                <v-card 
                    @click.stop="onFocus"
                    class="navGroupItem"
                >
                    <nav-menu 
                        v-for="(item,i) in items"
                        v-bind:ref="'menu-'+i"
                        v-bind:key="i"
                        v-bind:item="item"
                        @onActive="$emit('onActive')"
                    />
                </v-card>`,
            components: {
                'navMenu': navMenu
            },
            props: ['items']
        })

        // noinspection HtmlUnknownAttribute
        const navGroups = Vue.component('navGroups', {
            template: `
                <v-expansion-panel class="navGroups">
                    <v-expansion-panel-content
                        hide-actions
                        v-for="(group,i) in item.items"
                        v-bind:key="i"
                        v-bind:value="groups[i].isActive"
                    >
                        <ul slot="header"
                            class="list--group__header"
                            v-bind:class="{ 'list--group__header--active': groups[i] === true}"
                            @click.stop="toggle(i)">
                            <li>
                                <a 
                                    class="list__tile list__tile--link" 
                                    data-ripple="true"
                                    style="position: relative;"
                                 >
                                    <v-list-tile-action v-if="group.action">
                                        <v-icon dark>{{ group.action }}</v-icon>
                                    </v-list-tile-action>
                                    <div class="list__tile__content">
                                        <div class="list__tile__title">{{ group.title }}</div>
                                    </div>
                                </a>
                            </li>
                        </ul>
                        <nav-group
                            v-bind:ref="'group-' + i"
                            v-bind:items="group.items"
                            @onActive="toggle(i)"
                        />
                    </v-expansion-panel-content>
                </v-expansion-panel>`,
            components: {
                'navGroup': navGroup
            },
            props: ['item'],
            data: function () {
                return {
                    groups: []
                }
            },
            created: function() {
                for (let i=0; i < this.item.items.length; i++) {
                    this.groups.push({isActive: i === 0})
                }
                // noinspection JSUnresolvedVariable,JSUnresolvedFunction
                this.$router.afterEach(this.afterEachRoute)
            },
            mounted: function () {
                this.afterEachRoute(this.$route)
            },
            methods: {
                afterEachRoute: function(to) {
                    for (let i=0; i < this.item.items.length; i++) {
                        const path = this.item.items[i].group
                        if (!to.path.startsWith(path)) {
                            this.groups[i].isActive = false
                        }
                    }
                },
                toggle: function(index) {
                    if (this.item.items) {
                        for (let i=0; i < this.item.items.length; i++) {
                            this.groups[i].isActive = i === index
                        }
                    }
                    this.onFocus()
                },
                onFocus: function() {
                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('sitegen/ISSEARCHING', false);
                }
            }
        })

        // noinspection HtmlUnknownAttribute,RequiredAttributes
        Vue.component('docNav', {
            template: `
                <v-navigation-drawer
                    v-model="isActive"
                    fixed
                    dark app 
                    class="docNav"
                >
                    <v-toolbar
                        flat
                        dark
                        class="transparent"
                    >
                        <v-list class="pa-0 vuetify">
                            <v-list-tile tag="div">
                                <v-list-tile-avatar>
                                    <router-link to="/">
                                        <div 
                                            v-if="navLogo"
                                            class="navLogo"
                                         >
                                            <img :src="navLogo" alt="Navigation"/>
                                        </div>
                                        <v-icon v-else-if="navIcon">{{ navIcon }}</v-icon>
                                    </router-link>
                                </v-list-tile-avatar>
                                <v-list-tile-content>
                                    <v-list-tile-title>{{ navTitle }}</v-list-tile-title>
                                    <v-list-tile-sub-title>
                                    <v-menu v-if="releases.length > 1">
                                        <span flat slot="activator">
                                        Version: {{ release === releases[0] ? \`(\${release})\` : release }}
                                        <v-icon dark>arrow_drop_down</v-icon>
                                        </span>
                                        <v-list>
                                        <v-list-tile 
                                            v-for="(release, i) in releases" 
                                            v-if="i === 0" 
                                            to="/" 
                                            v-bind:key="release"
                                        >
                                            <v-list-tile-title>{{ release }}</v-list-tile-title>
                                        </v-list-tile>
                                        <v-list-tile tag="a" v-else :href="\`/releases/\${release}\`">
                                            <v-list-tile-title>{{ release }}</v-list-tile-title>
                                        </v-list-tile>
                                        </v-list>
                                    </v-menu>
                                    <span v-else>
                                        Version: {{ release === releases[0] ? \`(\${release})\` : release }}
                                    </span>
                                    </v-list-tile-sub-title>
                                </v-list-tile-content>
                            </v-list-tile>
                        </v-list>
                    </v-toolbar>
                    <v-divider></v-divider>
                    <v-list dense>
                        <template v-for="(item,i) in items">
                            <nav-groups
                                v-if="item.type === 'groups'"
                                v-bind:item="item"
                            />
                            <nav-menu
                                v-else-if="item.type === 'menu'"
                                v-bind:item="item"
                            />
                            <v-list-tile 
                                v-else-if="item.type === 'header'"
                                disabled
                            >
                                <v-list-tile-content>
                                    <v-list-tile-title>{{ item.title }}</v-list-tile-title>
                                </v-list-tile-content>
                            </v-list-tile>
                            <nav-item
                                v-else
                                v-bind:item="item"
                            />
                        </template>
                    </v-list>
                </v-navigation-drawer>`,
            components: {
                'navMenu': navMenu,
                'navGroups': navGroups
            },
            data: function () {
                return {
                    navIcon: config.navIcon,
                    navLogo: config.navLogo,
                    navTitle: config.navTitle,
                    release: config.release,
                    releases: config.releases,
                    items: navItems,
                    groups: [],
                    activeIndex: 0,
                    activeGroupIndex: 0
                };
            },
            computed: {
                filter: function () {
                    // noinspection JSUnresolvedVariable
                    return {
                        filter: `hue-rotate(${hue(this.$store.state.currentColor)}deg)`
                    };
                },
                isActive: {
                    get: function () {
                        // noinspection JSUnresolvedVariable
                        return this.$store.state.sidebar;
                    },
                    set: function (val) {
                        // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                        this.$store.commit('vuetify/SIDEBAR', val);
                    }
                }
            },
            methods: {
                setIsSearching: function (val) {
                    // noinspection JSUnresolvedVariable,JSCheckFunctionSignatures
                    this.$store.commit('sitegen/ISSEARCHING', val);
                }
            }
        });
    }
};
