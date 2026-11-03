// Javo additions 2026-11-03. SPDX-License-Identifier: GPL-3.0-only
package app.javo.branding;
import java.io.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Changes only the label and launcher icon of an already-decoded compatible APK. */
public final class BrandingResources {
    private static final String ANDROID="http://schemas.android.com/apk/res/android";
    private BrandingResources() {}
    public static void apply(Path decoded, String packageId, String displayName, InputStream icon) throws Exception {
        if (!displayName.matches("Javo (Video|Music)")) throw new IllegalArgumentException("Unknown Javo product");
        var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        Path manifest=decoded.resolve("AndroidManifest.xml");
        // Android's XML factory does not implement the desktop Xerces security flags.
        // Parse exactly the UTF-8 text inspected here, so encoding declarations cannot
        // hide a DTD from this check. Decoded Apktool manifests are UTF-8.
        if(Files.size(manifest)>16*1024*1024)throw new IOException("Manifest exceeds size limit");
        byte[] bytes=Files.readAllBytes(manifest);
        if(bytes.length>16*1024*1024)throw new IOException("Manifest exceeds size limit");
        String xml=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        String markup=xml.toUpperCase(Locale.ROOT);
        if(markup.contains("<!DOCTYPE") || markup.contains("<!ENTITY") || xml.indexOf('\0')>=0)
            throw new SecurityException("Document types and entity declarations are forbidden");
        var builder=factory.newDocumentBuilder();
        builder.setEntityResolver((publicId,systemId)->{throw new SAXException("External XML entities are forbidden");});
        var doc=builder.parse(new InputSource(new StringReader(xml)));
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
        var transformerFactory=TransformerFactory.newInstance();
        // Identity transformation of the guarded DOM; no stylesheet or URL is loaded.
        var temp=Files.createTempFile(decoded,"javo-manifest-", ".xml");
        try{transformerFactory.newTransformer().transform(new DOMSource(doc),new StreamResult(temp.toFile()));Files.move(temp,manifest,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}
    }
}
