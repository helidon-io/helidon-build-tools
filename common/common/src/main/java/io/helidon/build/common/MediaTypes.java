/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
package io.helidon.build.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mapping of file extension to mime-type.
 */
public final class MediaTypes {

    private static final Map<String, String> MEDIA_TYPES = new HashMap<>();

    static {
        MEDIA_TYPES.put("3gp", "video/3gpp");
        MEDIA_TYPES.put("3g2", "video/3gpp2");
        MEDIA_TYPES.put("7z", "application/x-7z-compressed");
        MEDIA_TYPES.put("aac", "audio/aac");
        MEDIA_TYPES.put("abs", "audio/x-mpeg");
        MEDIA_TYPES.put("abw", "application/x-abiword");
        MEDIA_TYPES.put("ai", "application/postscript");
        MEDIA_TYPES.put("aif", "audio/x-aiff");
        MEDIA_TYPES.put("aifc", "audio/x-aiff");
        MEDIA_TYPES.put("aiff", "audio/x-aiff");
        MEDIA_TYPES.put("aim", "application/x-aim");
        MEDIA_TYPES.put("arc", "application/x-freearc");
        MEDIA_TYPES.put("art", "image/x-jg");
        MEDIA_TYPES.put("asf", "video/x-ms-asf");
        MEDIA_TYPES.put("asx", "video/x-ms-asf");
        MEDIA_TYPES.put("au", "audio/basic");
        MEDIA_TYPES.put("avi", "video/x-msvideo");
        MEDIA_TYPES.put("avx", "video/x-rad-screenplay");
        MEDIA_TYPES.put("azw", "application/vnd.amazon.ebook");
        MEDIA_TYPES.put("bcpio", "application/x-bcpio");
        MEDIA_TYPES.put("bin", "application/octet-stream");
        MEDIA_TYPES.put("bmp", "image/bmp");
        MEDIA_TYPES.put("body", "text/html");
        MEDIA_TYPES.put("bz", "application/x-bzip");
        MEDIA_TYPES.put("bz2", "application/x-bzip2");
        MEDIA_TYPES.put("cdf", "application/x-cdf");
        MEDIA_TYPES.put("cer", "application/x-x509-ca-cert");
        MEDIA_TYPES.put("class", "application/java");
        MEDIA_TYPES.put("conf", "application/hocon");
        MEDIA_TYPES.put("cpio", "application/x-cpio");
        MEDIA_TYPES.put("csh", "application/x-csh");
        MEDIA_TYPES.put("css", "text/css");
        MEDIA_TYPES.put("csv", "text/csv");
        MEDIA_TYPES.put("dib", "image/bmp");
        MEDIA_TYPES.put("doc", "application/msword");
        MEDIA_TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MEDIA_TYPES.put("dtd", "application/xml-dtd");
        MEDIA_TYPES.put("dv", "video/x-dv");
        MEDIA_TYPES.put("dvi", "application/x-dvi");
        MEDIA_TYPES.put("eot", "application/vnd.ms-fontobject");
        MEDIA_TYPES.put("epub", "application/epub+zip");
        MEDIA_TYPES.put("eps", "application/postscript");
        MEDIA_TYPES.put("etx", "text/x-setext");
        MEDIA_TYPES.put("exe", "application/octet-stream");
        MEDIA_TYPES.put("gif", "image/gif");
        MEDIA_TYPES.put("gk", "application/octet-stream");
        MEDIA_TYPES.put("gtar", "application/x-gtar");
        MEDIA_TYPES.put("gz", "application/gzip");
        MEDIA_TYPES.put("hdf", "application/x-hdf");
        MEDIA_TYPES.put("hqx", "application/mac-binhex40");
        MEDIA_TYPES.put("htc", "text/x-component");
        MEDIA_TYPES.put("htm", "text/html");
        MEDIA_TYPES.put("html", "text/html");
        MEDIA_TYPES.put("ief", "image/ief");
        MEDIA_TYPES.put("ico", "image/x-icon");
        MEDIA_TYPES.put("ics", "text/calendar");
        MEDIA_TYPES.put("jad", "text/vnd.sun.j2me.app-descriptor");
        MEDIA_TYPES.put("jar", "application/java-archive");
        MEDIA_TYPES.put("java", "text/plain");
        MEDIA_TYPES.put("jnlp", "application/x-java-jnlp-file");
        MEDIA_TYPES.put("jpe", "image/jpeg");
        MEDIA_TYPES.put("jpeg", "image/jpeg");
        MEDIA_TYPES.put("jpg", "image/jpeg");
        MEDIA_TYPES.put("js", "text/javascript");
        MEDIA_TYPES.put("json", "application/json");
        MEDIA_TYPES.put("jsonld", "application/ld+json");
        MEDIA_TYPES.put("kar", "audio/x-midi");
        MEDIA_TYPES.put("latex", "application/x-latex");
        MEDIA_TYPES.put("m3u", "audio/x-mpegurl");
        MEDIA_TYPES.put("mac", "image/x-macpaint");
        MEDIA_TYPES.put("man", "application/x-troff-man");
        MEDIA_TYPES.put("mathml", "application/mathml+xml");
        MEDIA_TYPES.put("me", "application/x-troff-me");
        MEDIA_TYPES.put("mid", "audio/midi");
        MEDIA_TYPES.put("midi", "audio/midi");
        MEDIA_TYPES.put("mif", "application/x-mif");
        MEDIA_TYPES.put("mjs", "text/javascript");
        MEDIA_TYPES.put("mov", "video/quicktime");
        MEDIA_TYPES.put("movie", "video/x-sgi-movie");
        MEDIA_TYPES.put("mp1", "audio/mpeg");
        MEDIA_TYPES.put("mp2", "audio/mpeg");
        MEDIA_TYPES.put("mp3", "audio/mpeg");
        MEDIA_TYPES.put("mpa", "audio/mpeg");
        MEDIA_TYPES.put("mpe", "video/mpeg");
        MEDIA_TYPES.put("mpeg", "video/mpeg");
        MEDIA_TYPES.put("mpega", "audio/mpeg");
        MEDIA_TYPES.put("mpkg", "application/vnd.apple.installer+xml");
        MEDIA_TYPES.put("mpg", "video/mpeg");
        MEDIA_TYPES.put("mpv2", "video/mpeg2");
        MEDIA_TYPES.put("ms", "application/x-wais-source");
        MEDIA_TYPES.put("nc", "application/x-netcdf");
        MEDIA_TYPES.put("oda", "application/oda");
        MEDIA_TYPES.put("odp", "application/vnd.oasis.opendocument.presentation");
        MEDIA_TYPES.put("ods", "application/vnd.oasis.opendocument.spreadsheet");
        MEDIA_TYPES.put("odt", "application/vnd.oasis.opendocument.text");
        MEDIA_TYPES.put("oga", "audio/ogg");
        MEDIA_TYPES.put("ogg", "application/ogg");
        MEDIA_TYPES.put("ogv", "video/ogg");
        MEDIA_TYPES.put("ogx", "application/ogg");
        MEDIA_TYPES.put("opus", "audio/opus");
        MEDIA_TYPES.put("otf", "font/otf");
        MEDIA_TYPES.put("pbm", "image/x-portable-bitmap");
        MEDIA_TYPES.put("pct", "image/pict");
        MEDIA_TYPES.put("pdf", "application/pdf");
        MEDIA_TYPES.put("pgm", "image/x-portable-graymap");
        MEDIA_TYPES.put("php", "appliction/php");
        MEDIA_TYPES.put("pic", "image/pict");
        MEDIA_TYPES.put("pict", "image/pict");
        MEDIA_TYPES.put("pls", "audio/x-scpls");
        MEDIA_TYPES.put("png", "image/png");
        MEDIA_TYPES.put("pnm", "image/x-portable-anymap");
        MEDIA_TYPES.put("pnt", "image/x-macpaint");
        MEDIA_TYPES.put("ppm", "image/x-portable-pixmap");
        MEDIA_TYPES.put("ppt", "application/vnd.ms-powerpoint");
        MEDIA_TYPES.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        MEDIA_TYPES.put("properties", "text/x-java-properties");
        MEDIA_TYPES.put("ps", "application/postscript");
        MEDIA_TYPES.put("psd", "image/x-photoshop");
        MEDIA_TYPES.put("qt", "video/quicktime");
        MEDIA_TYPES.put("qti", "image/x-quicktime");
        MEDIA_TYPES.put("qtif", "image/x-quicktime");
        MEDIA_TYPES.put("rar", "application/x-rar-compressed");
        MEDIA_TYPES.put("ras", "image/x-cmu-raster");
        MEDIA_TYPES.put("rdf", "application/rdf+xml");
        MEDIA_TYPES.put("rgb", "image/x-rgb");
        MEDIA_TYPES.put("rm", "application/vnd.rn-realmedia");
        MEDIA_TYPES.put("roff", "application/x-troff");
        MEDIA_TYPES.put("rtf", "application/rtf");
        MEDIA_TYPES.put("rtx", "text/richtext");
        MEDIA_TYPES.put("sh", "application/x-sh");
        MEDIA_TYPES.put("shar", "application/x-shar");
        MEDIA_TYPES.put("shtml", "text/x-server-parsed-html");
        MEDIA_TYPES.put("sit", "application/x-stuffit");
        MEDIA_TYPES.put("smf", "audio/x-midi");
        MEDIA_TYPES.put("snd", "audio/basic");
        MEDIA_TYPES.put("src", "application/x-wais-source");
        MEDIA_TYPES.put("sv4cpio", "application/x-sv4cpio");
        MEDIA_TYPES.put("sv4crc", "application/x-sv4crc");
        MEDIA_TYPES.put("svg", "image/svg+xml");
        MEDIA_TYPES.put("svgz", "image/svg+xml");
        MEDIA_TYPES.put("swf", "application/x-shockwave-flash");
        MEDIA_TYPES.put("t", "application/x-troff");
        MEDIA_TYPES.put("tar", "application/x-tar");
        MEDIA_TYPES.put("tcl", "application/x-tcl");
        MEDIA_TYPES.put("tex", "application/x-tex");
        MEDIA_TYPES.put("texi", "application/x-texinfo");
        MEDIA_TYPES.put("texinfo", "application/x-texinfo");
        MEDIA_TYPES.put("tif", "image/tiff");
        MEDIA_TYPES.put("tiff", "image/tiff");
        MEDIA_TYPES.put("tr", "application/x-troff");
        MEDIA_TYPES.put("ts", "video/mp2t");
        MEDIA_TYPES.put("tsv", "text/tab-separated-values");
        MEDIA_TYPES.put("ttf", "font/ttf");
        MEDIA_TYPES.put("txt", "text/plain");
        MEDIA_TYPES.put("ulw", "audio/basic");
        MEDIA_TYPES.put("ustar", "application/x-ustar");
        MEDIA_TYPES.put("vsd", "application/vnd.visio");
        MEDIA_TYPES.put("vxml", "application/voicexml+xml");
        MEDIA_TYPES.put("wav", "audio/wav");
        MEDIA_TYPES.put("wbmp", "image/vnd.wap.wbmp");
        MEDIA_TYPES.put("weba", "audio/webm");
        MEDIA_TYPES.put("webm", "video/webm");
        MEDIA_TYPES.put("webp", "image/webp");
        MEDIA_TYPES.put("wml", "text/vnd.wap.wml");
        MEDIA_TYPES.put("wmlc", "application/vnd.wap.wmlc");
        MEDIA_TYPES.put("wmls", "text/vnd.wap.wmls");
        MEDIA_TYPES.put("wmlscriptc", "application/vnd.wap.wmlscriptc");
        MEDIA_TYPES.put("woff", "font/woff");
        MEDIA_TYPES.put("woff2", "font/woff2");
        MEDIA_TYPES.put("wrl", "x-world/x-vrml");
        MEDIA_TYPES.put("xbm", "image/x-xbitmap");
        MEDIA_TYPES.put("xht", "application/xhtml+xml");
        MEDIA_TYPES.put("xhtml", "application/xhtml+xml");
        MEDIA_TYPES.put("xls", "application/vnd.ms-excel");
        MEDIA_TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        MEDIA_TYPES.put("xml", "application/xml");
        MEDIA_TYPES.put("xpm", "image/x-xpixmap");
        MEDIA_TYPES.put("xsl", "application/xml");
        MEDIA_TYPES.put("xslt", "application/xslt+xml");
        MEDIA_TYPES.put("xul", "application/vnd.mozilla.xul+xml");
        MEDIA_TYPES.put("xwd", "image/x-xwindowdump");
        MEDIA_TYPES.put("yaml", "application/x-yaml");
        MEDIA_TYPES.put("yml", "application/x-yaml");
        MEDIA_TYPES.put("Z", "application/x-compress");
        MEDIA_TYPES.put("z", "application/x-compress");
        MEDIA_TYPES.put("zip", "application/zip");
    }

    private MediaTypes() {
        // cannot be instanciated
    }

    /**
     * Get the media type for the given file extension.
     *
     * @param ext file extension E.g. {@code "png"}
     * @return optional of media type
     */
    public static Optional<String> of(String ext) {
        return Optional.ofNullable(MEDIA_TYPES.get(ext));
    }
}
