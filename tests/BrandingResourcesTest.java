import app.javo.branding.BrandingResources;
import java.nio.file.*;
import java.io.*;
import javax.xml.parsers.DocumentBuilderFactory;

public class BrandingResourcesTest {
    public static void main(String[] args)throws Exception{
        var folder=Files.createTempDirectory("javo-branding-test");
        var file=folder.resolve("AndroidManifest.xml");
        String original="<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"com.google.android.youtube\"><uses-permission android:name=\"android.permission.INTERNET\"/><application android:label=\"Old\"><service android:name=\".KeepService\"/><activity android:name=\".Launcher\" android:label=\"Old activity\"><intent-filter><action android:name=\"android.intent.action.MAIN\"/><category android:name=\"android.intent.category.LAUNCHER\"/></intent-filter></activity></application></manifest>";
        try{
            Files.writeString(file,original);
            BrandingResources.apply(folder,"com.google.android.youtube","Javo Video",new ByteArrayInputStream("<vector/>".getBytes()));
            var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
            var doc=factory.newDocumentBuilder().parse(file.toFile());var ns="http://schemas.android.com/apk/res/android";
            var app=(org.w3c.dom.Element)doc.getElementsByTagName("application").item(0);
            if(!app.getAttributeNS(ns,"label").equals("Javo Video"))throw new AssertionError("label");
            if(!((org.w3c.dom.Element)doc.getElementsByTagName("activity").item(0)).getAttributeNS(ns,"label").equals("Javo Video"))throw new AssertionError("launcher alias");
            if(!doc.getDocumentElement().getAttribute("package").equals("com.google.android.youtube")||doc.getElementsByTagName("service").getLength()!=1||doc.getElementsByTagName("uses-permission").getLength()!=1)throw new AssertionError("compatibility contracts changed");
            System.out.println("PASS labels/icons changed while preserving package, permission and service");
            Files.writeString(file,original);boolean failed=false;try{BrandingResources.apply(folder,"com.wrong.package","Javo Video",new ByteArrayInputStream(new byte[0]));}catch(SecurityException e){failed=true;}if(!failed||!Files.readString(file).equals(original))throw new AssertionError("wrong package not rejected atomically");
            System.out.println("PASS wrong package rejected without modifying manifest");
            Files.delete(folder.resolve("res/drawable/javo_brand_icon.xml"));
            Files.writeString(file,original.replace("com.google.android.youtube","app.revanced.android.youtube"));
            BrandingResources.apply(folder,"com.google.android.youtube","Javo Video",new ByteArrayInputStream("<vector/>".getBytes()));
            if(!Files.readString(file).contains("app.revanced.android.youtube"))throw new AssertionError("GmsCore package was overwritten");
            System.out.println("PASS prior GmsCore package rename remains compatible and preserved");
            Files.writeString(file,"<!DOCTYPE manifest [<!ENTITY injected SYSTEM 'file:///nonexistent'>]><manifest package=\"com.google.android.youtube\"><application/></manifest>");
            failed=false;try{BrandingResources.apply(folder,"com.google.android.youtube","Javo Video",new ByteArrayInputStream(new byte[0]));}catch(org.xml.sax.SAXParseException e){failed=true;}if(!failed)throw new AssertionError("DTD accepted");
            System.out.println("PASS XML external entities rejected");
        }finally{try(var paths=Files.walk(folder)){for(var p:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
        System.out.println("4 branding resource checks passed; APK patch execution not covered");
    }
}
