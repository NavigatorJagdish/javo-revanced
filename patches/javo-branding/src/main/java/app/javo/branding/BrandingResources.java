// Javo additions 2026-11-03. SPDX-License-Identifier: GPL-3.0-only
package app.javo.branding;
import java.io.*;
import java.nio.file.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/** Changes only the label and launcher icon of an already-decoded compatible APK. */
public final class BrandingResources {
    private static final String ANDROID="http://schemas.android.com/apk/res/android";
    private BrandingResources() {}
    public static void apply(Path decoded, String packageId, String displayName, InputStream icon) throws Exception {
        if (!displayName.matches("Javo (Video|Music)")) throw new IllegalArgumentException("Unknown Javo product");
        var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);
        Path manifest=decoded.resolve("AndroidManifest.xml");
        var doc=factory.newDocumentBuilder().parse(manifest.toFile());
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
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        var temp=Files.createTempFile(decoded,"javo-manifest-", ".xml");
        try{transformerFactory.newTransformer().transform(new DOMSource(doc),new StreamResult(temp.toFile()));Files.move(temp,manifest,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}
    }
}
