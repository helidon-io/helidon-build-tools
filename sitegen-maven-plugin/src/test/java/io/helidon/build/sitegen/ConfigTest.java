/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.build.sitegen;

import java.util.List;
import java.util.Map;

import io.helidon.build.sitegen.WebResource.Location;

import org.junit.jupiter.api.Test;

import static io.helidon.build.sitegen.TestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author rgrecour
 */
public class ConfigTest {

    private static void assertWebResource(WebResource actual,
                                          String expectedPath,
                                          String expectedHref,
                                          String name) {
        assertNotNull(actual, name);
        assertEquals(Location.from(expectedPath, expectedHref), actual.getLocation(), name);
    }

    @Test
    public void testLoadConfig() {

        // TODO add assertions for navigation: glyph, pathprefix
        Site site = Site.builder()
                .config(getFile(SOURCE_DIR_PREFIX + "config/basic.yaml"))
                .build();
        assertNotNull(site, "site");

        // backend
        Backend backend = site.getBackend();
        assertNotNull(backend, "backend");
        assertString("basic", backend.getName(), "backend");

        SiteEngine engine = site.getEngine();
        assertNotNull(engine);
        assertNotNull(engine.asciidoc(), "engine.asciidoctor");
        assertNotNull(engine.freemarker(), "engine.freemarker");

        // asciidoctor imagesDir
        assertString("./images", engine.asciidoc().getImagesdir(), "engine.asciidoctor.imagesdir");

        // asciidoctor libraries
        List<String> asciidoctorLibs = engine.asciidoc().getLibraries();
        assertList(1, asciidoctorLibs, "engine.asciidoctor.libraries");
        assertString("testlib", asciidoctorLibs.get(0), "engine.asciidoctor.libraries[0]");

        // asciidoctor attributes
        Map<String, Object> asciidoctorAttrs = engine.asciidoc().getAttributes();
        assertEquals(1, asciidoctorAttrs.size(), "engine.asciidoctor.attributes");
        assertEquals("alice", asciidoctorAttrs.get("bob"), "engine.asciidoctor.attributes[bob]");

        // freemarker directives
        Map<String, String> freemarkerDirectives = engine.freemarker().getDirectives();
        assertEquals(1, freemarkerDirectives.size(), "engine.freemarker.directives");
        assertEquals("com.acme.foo.FooDirective", freemarkerDirectives.get("foo"), "engine.freemarker.directives[foo]");

        // freemarker model
        Map<String, String> freemarkerModel = engine.freemarker().getModel();
        assertEquals(1, freemarkerModel.size(), "engine.freemarker.model");
        assertEquals("value", freemarkerModel.get("key"), "engine.freemarker.model[foo]");

        // header
        Header header = site.getHeader();
        assertNotNull(header, "header");

        // favicon
        assertWebResource(header.getFavicon(), "assets/images/favicon.ico", null, "header.favicon");

        // stylesheets
        List<WebResource> stylesheets = header.getStylesheets();
        assertList(2, stylesheets, "header.stylesheets");
        assertWebResource(stylesheets.get(0), "assets/css/style.css", null, "header.stylesheets[0]");
        assertWebResource(stylesheets.get(1), null, "https://css.com/style.css", "header.stylesheets[1]");

        // scripts
        List<WebResource> scripts = header.getScripts();
        assertList(2, scripts, "header.scripts");
        assertWebResource(scripts.get(0), "assets/js/script.js", null, "header.scripts[0]");
        assertWebResource(scripts.get(1), null, "https://js.com/script.js", "header.scripts[1]");

        // meta
        Map<String, String> meta = header.getMeta();
        assertNotNull(meta, "header.meta");
        assertEquals("a global description", meta.get("description"), "header.meta[description]");

        // static assets
        List<StaticAsset> assets = site.getAssets();
        assertList(1, assets, "assets");

        StaticAsset firstAsset = assets.get(0);
        assertEquals("/assets", firstAsset.getTarget(), "assets[0].target");
        assertList(1, firstAsset.getIncludes(), "assets[0].includes");
        assertEquals(System.getProperty("basedir", "") + "/assets/**", firstAsset.getIncludes().get(0), "assets[0].includes[0]");
        assertList(1, firstAsset.getExcludes(), "assets[0].excludes");
        assertEquals("**/_*", firstAsset.getExcludes().get(0), "assets[0].excludes[0]");

        // pages
        List<SourcePathFilter> pages = site.getPages();
        assertList(1, pages, "pages");

        SourcePathFilter firstPageDef = pages.get(0);
        assertList(1, firstPageDef.getIncludes(), "pages[0].includes");
        assertEquals("docs/**/*.adoc", firstPageDef.getIncludes().get(0), "pages[0].includes[0]");
        assertList(1, firstPageDef.getExcludes(), "pages[0].excludes");
        assertEquals("**/_*", firstPageDef.getExcludes().get(0), "pages[0].excludes[0]");
    }

    @Test
    public void testLoadVuetifyConfig() {

        Site site = Site.builder()
                .config(getFile(SOURCE_DIR_PREFIX + "config/vuetify.yaml"))
                .build();
        assertNotNull(site, "site");

        // backend
        Backend backend = site.getBackend();
        assertNotNull(backend, "backend");
        assertString("vuetify", backend.getName(), "backend.name");

        assertTrue(backend instanceof VuetifyBackend, "vuetify backend class");
        VuetifyBackend vbackend = (VuetifyBackend) backend;

        // homePage
        assertString("home.adoc", vbackend.getHomePage(), "homePage");

        // releases
        assertList(1, vbackend.getReleases(), "releases");
        assertString("1.0", vbackend.getReleases().get(0), "releases[0]");

        // navigation
        VuetifyNavigation navigation = vbackend.getNavigation();
        assertNotNull(navigation, "navigation");
        assertString("Pet Project Documentation", navigation.getTitle(), "nav.title");

        List<VuetifyNavigation.Item> topNavItems = navigation.getItems();
        assertList(3, topNavItems, "nav.items");

        VuetifyNavigation.Group mainDocNavGroup = assertType(topNavItems.get(0), VuetifyNavigation.Group.class, "nav.items[0]");
        assertEquals("Main Documentation", mainDocNavGroup.getTitle(), "nav.items[0].title");

        List<VuetifyNavigation.Item> mainDocNavItems = mainDocNavGroup.getItems();
        assertList(3, mainDocNavItems, "nav.items[0].items");

        VuetifyNavigation.SubGroup mainDoc1stItem = assertType(mainDocNavItems.get(0), VuetifyNavigation.SubGroup.class, "nav.items[0].items[0]");
        assertEquals("About", mainDoc1stItem.getTitle(), "nav.items[0].items[0].title");
        assertEquals("/about", mainDoc1stItem.getPathprefix(), "nav.items[0].items[0].pathprefix");

        VuetifyNavigation.SubGroup mainDoc2ndItem = assertType(mainDocNavItems.get(1), VuetifyNavigation.SubGroup.class, "nav.items[0].items[1]");
        assertEquals("Getting Started", mainDoc2ndItem.getTitle(), "nav.items[0].items[1].title");
        assertEquals("/getting-started", mainDoc2ndItem.getPathprefix(), "nav.items[0].items[1].pathprefix");

        VuetifyNavigation.SubGroup mainDoc3rdItem = assertType(mainDocNavItems.get(2), VuetifyNavigation.SubGroup.class, "nav.items[0].items[2]");
        assertEquals("Let's code", mainDoc3rdItem.getTitle(), "nav.items[0].items[2].title");
        assertEquals("/lets-code", mainDoc3rdItem.getPathprefix(), "nav.items[0].items[2].pathprefix");

        VuetifyNavigation.Group extraResourcesNavGroup = assertType(topNavItems.get(1), VuetifyNavigation.Group.class, "nav.items[1]");
        assertEquals("Extra Resources", extraResourcesNavGroup.getTitle(), "nav.items[1].title");

        List<VuetifyNavigation.Item> extraResourcesItems = extraResourcesNavGroup.getItems();
        assertList(2, extraResourcesItems, "nav.items[1].items");

        VuetifyNavigation.Link google = assertType(extraResourcesItems.get(0), VuetifyNavigation.Link.class, "nav.items[1].items[0]");
        assertEquals("Google", google.getTitle(), "nav.items[1].items[0].title");
        assertEquals("https://google.com", google.getHref(), "nav.items[1].items[0].href");

        VuetifyNavigation.Link amazon = assertType(extraResourcesItems.get(1), VuetifyNavigation.Link.class, "nav.items[1].items[1]");
        assertEquals("Amazon", amazon.getTitle(), "nav.items[1].items[1].title");
        assertEquals("https://amazon.com", amazon.getHref(), "nav.items[1].items[1].href");

        VuetifyNavigation.Link githubNavLink = assertType(topNavItems.get(2), VuetifyNavigation.Link.class, "nav.items[2]");
        assertEquals("Github", githubNavLink.getTitle(), "nav.items[2].title");
        assertEquals("https://github.com", githubNavLink.getHref(), "nav.items[2].href");

        Map<String, String> options = vbackend.getTheme();
        assertNotNull(options, "backend.theme");

        assertEquals("#1976D2", options.get("primary"), "backend.theme.primary");
        assertEquals("#424242", options.get("secondary"), "backend.theme.secondary");
        assertEquals("#82B1FF", options.get("accent"), "backend.theme.accent");
        assertEquals("#FF5252", options.get("error"), "backend.theme.error");
        assertEquals("#2196F3", options.get("info"), "backend.theme.info");
        assertEquals("#4CAF50", options.get("success"), "backend.theme.success");
        assertEquals("#FFC107", options.get("warning"), "backend.theme.warning");
        assertEquals("true", options.get("toolbar.enabled"), "backend.theme.toolbar.enabled");
        assertEquals("true", options.get("navmenu.enabled"), "backend.theme.navmenu.enabled");
        assertEquals("true", options.get("navfooter.enabled"), "backend.theme.navfooter.enabled");
    }
}
