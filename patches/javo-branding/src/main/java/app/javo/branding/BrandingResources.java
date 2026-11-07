// Javo additions 2026-11-03. SPDX-License-Identifier: GPL-3.0-only
package app.javo.branding;
import java.io.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.util.Locale;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Applies Javo branding to an already decoded compatible APK. */
public final class BrandingResources {
    private static final String ANDROID="http://schemas.android.com/apk/res/android";
    private BrandingResources() {}
    public static void apply(Path decoded, String packageId, String displayName, InputStream icon) throws Exception {
        if (!displayName.matches("Javo (Video|Music)")) throw new IllegalArgumentException("Unknown Javo product");
        Path manifest=decoded.resolve("AndroidManifest.xml");
        var doc = readXml(manifest);
        String current=doc.getDocumentElement().getAttribute("package");
        // The trusted GmsCore-support patch can already have renamed the manifest.
        String renamed=switch(packageId){
            case "com.google.android.youtube" -> "app.revanced.android.youtube";
            case "com.google.android.apps.youtube.music" -> "app.revanced.android.apps.youtube.music";
            default -> packageId;
        };
        if(!current.equals(packageId) && !current.equals(renamed)) throw new SecurityException("Unexpected source package");
        var app=(Element)doc.getElementsByTagName("application").item(0);
        if(app==null || icon==null)throw new IOException("Missing application or branding icon");
        app.setAttributeNS(ANDROID,"android:label",displayName);
        app.setAttributeNS(ANDROID,"android:icon","@drawable/javo_brand_icon");
        app.setAttributeNS(ANDROID,"android:roundIcon","@drawable/javo_brand_icon");
        for(var tag:new String[]{"activity","activity-alias"}){
            var components=app.getElementsByTagName(tag);
            for(int i=0;i<components.getLength();i++){
                var component=(Element)components.item(i);var categories=component.getElementsByTagName("category");
                for(int j=0;j<categories.getLength();j++)if(((Element)categories.item(j)).getAttributeNS(ANDROID,"name").equals("android.intent.category.LAUNCHER")){
                    component.setAttributeNS(ANDROID,"android:label",displayName);
                    component.setAttributeNS(ANDROID,"android:icon","@drawable/javo_brand_icon");
                }
            }
        }
        var drawable=decoded.resolve("res/drawable/javo_brand_icon.xml");Files.createDirectories(drawable.getParent());
        // Refuse to overwrite an existing resource of unknown ownership.
        if(Files.exists(drawable))throw new IOException("javo_brand_icon already exists");
        try(var in=icon){Files.copy(in,drawable);}
        writeXml(doc, manifest);
    }

    public static void applyInterface(Path resources, String packageId, String label) throws Exception {
        var headerNames = Arrays.asList(
            "yt_ringo2_wordmark_header_dark", "yt_ringo2_wordmark_header_light",
            "yt_ringo2_premium_wordmark_header_dark", "yt_ringo2_premium_wordmark_header_light",
            "yt_wordmark_header_dark", "yt_wordmark_header_light",
            "yt_premium_wordmark_header_dark", "yt_premium_wordmark_header_light");
        var wordmarkText = Arrays.asList(
            "yt_logo_text_dark", "yt_logo_text_light", "yt_premium_logo_text_dark", "yt_premium_logo_text_light");
        var videoIcons = Arrays.asList("yt_icon_header", "ringo2_action_bar_logo_release",
            "ic_youtube_logo", "yt_color_youtube_logo_icon_24dp");
        boolean video = packageId.equals("com.google.android.youtube");
        var settingsIcons = Arrays.asList("revanced_settings_icon", "revanced_settings_icon_bold");
        var productLabel = video ? "application_name" : "app_launcher_name";
        var launcher = resources.resolve("drawable/javo_brand_icon.xml");
        try (var directories = Files.newDirectoryStream(resources)) {
            for (var directory : directories) {
                if (!Files.isDirectory(directory)) continue;
                var qualifier = directory.getFileName().toString();
                if (qualifier.equals("values") || qualifier.startsWith("values-")) {
                    try (var files = Files.newDirectoryStream(directory, "{strings,revanced_strings}.xml")) {
                        for (var file : files) updateLabels(file, label, productLabel);
                    }
                } else if (qualifier.equals("drawable") || qualifier.startsWith("drawable-")) {
                    try (var files = Files.newDirectoryStream(directory)) {
                        for (var file : files) {
                            var filename = file.getFileName().toString();
                            int dot = filename.indexOf('.');
                            if (dot == -1) continue;
                            var resourceName = filename.substring(0, dot);
                            var replacement = directory.resolve(resourceName + ".xml");
                            if (settingsIcons.contains(resourceName)) {
                                Files.copy(launcher, replacement, StandardCopyOption.REPLACE_EXISTING);
                            } else if (video && (headerNames.contains(resourceName) || wordmarkText.contains(resourceName))) {
                                Files.write(replacement, header(resourceName.endsWith("_dark"),
                                    headerNames.contains(resourceName)).getBytes(StandardCharsets.UTF_8));
                            } else if (video && videoIcons.contains(resourceName)) {
                                Files.write(replacement, videoIcon().getBytes(StandardCharsets.UTF_8));
                            } else continue;
                            if (!file.equals(replacement)) Files.delete(file);
                        }
                    }
                }
            }
        }
    }

    private static void updateLabels(Path file, String label, String productLabel) throws Exception {
        var document = readXml(file);
        var strings = document.getElementsByTagName("string");
        boolean changed = false;
        for (int i = 0; i < strings.getLength(); i++) {
            var entry = (Element) strings.item(i);
            switch (entry.getAttribute("name")) {
                case "revanced_settings_title" -> entry.setTextContent(label + " settings");
                default -> {
                    if (!entry.getAttribute("name").equals(productLabel)) continue;
                    entry.setTextContent(label);
                }
            }
            changed = true;
        }
        if (changed) writeXml(document, file);
    }

    private static final String VIDEO_MARK = """
        <path android:fillColor="#FF475F" android:fillType="evenOdd"
            android:pathData="M5,5 Q5,3 7,4 L25,14 Q29,16 25,18 L7,28 Q5,29 5,26 Z M10,11 L10,21 L19,16 Z"/>
        <path android:strokeColor="#FF8A38" android:strokeWidth="2.3" android:strokeLineCap="round"
            android:pathData="M7,5 L24,15"/>
        """;

    private static String videoIcon() {
        return """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp" android:height="24dp" android:viewportWidth="32" android:viewportHeight="32">
                %s
            </vector>
            """.replace("%s", VIDEO_MARK);
    }

    private static String header(boolean dark, boolean includeIcon) {
        return """
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="WIDTHdp" android:height="HEIGHTdp" android:viewportWidth="WIDTH" android:viewportHeight="HEIGHT">
                <group android:translateX="LEFT" android:translateY="TOP">
                MARK
                <path android:strokeColor="INK" android:strokeWidth="2.6" android:strokeLineCap="round"
                    android:strokeLineJoin="round"
                    android:pathData="M42,9 L42,19 Q42,24 38,24 Q35,24 34,21 M46,24 L51,9 L56,24 M48,19 L54,19 M59,9 L64,24 L69,9 M75,10 C82,5 85,12 84,17 C83,25 77,26 74,21 C71,17 72,12 75,10"/>
                </group>
            </vector>
            """.replace("WIDTH", includeIcon ? "110" : "54")
            .replace("HEIGHT", includeIcon ? "48" : "32")
            .replace("LEFT", includeIcon ? "11" : "-32")
            .replace("TOP", includeIcon ? "8" : "0")
            .replace("MARK", includeIcon ? VIDEO_MARK : "")
            .replace("INK", dark ? "#FFF7FA" : "#22202A");
    }

    private static Document readXml(Path file) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        // Android's XML factory does not implement the desktop Xerces security flags.
        // Parse exactly the UTF-8 text inspected here, so encoding declarations cannot
        // hide a DTD from this check. Decoded Apktool manifests are UTF-8.
        if(Files.size(file)>16*1024*1024)throw new IOException("Decoded XML exceeds size limit");
        byte[] bytes=Files.readAllBytes(file);
        if(bytes.length>16*1024*1024)throw new IOException("Decoded XML exceeds size limit");
        String xml=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        String markup=xml.toUpperCase(Locale.ROOT);
        if(markup.contains("<!DOCTYPE") || markup.contains("<!ENTITY") || xml.indexOf('\0')>=0)
            throw new SecurityException("Document types and entity declarations are forbidden");
        var builder=factory.newDocumentBuilder();
        builder.setEntityResolver((publicId,systemId)->{throw new SAXException("External XML entities are forbidden");});
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private static void writeXml(Document doc, Path file) throws Exception {
        var transformerFactory=TransformerFactory.newInstance();
        // Identity transformation of the guarded DOM; no stylesheet or URL is loaded.
        var temp=Files.createTempFile(file.getParent(),"javo-resource-", ".xml");
        try{transformerFactory.newTransformer().transform(new DOMSource(doc),new StreamResult(temp.toFile()));Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}
    }
}
