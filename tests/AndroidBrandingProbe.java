import app.javo.branding.BrandingResources;
import java.nio.file.*;
/** Small diagnostic for the Android XML implementation, runnable with app_process. */
public final class AndroidBrandingProbe {
    public static void main(String[] args) throws Exception {
        Path root=Paths.get(args[0]);
        BrandingResources.apply(root,"com.google.android.youtube","Javo Video",Files.newInputStream(root.resolve("icon.xml")));
        String manifest=new String(Files.readAllBytes(root.resolve("AndroidManifest.xml")),java.nio.charset.StandardCharsets.UTF_8);
        if(!manifest.contains("Javo Video") || !Files.exists(root.resolve("res/drawable/javo_brand_icon.xml")))throw new AssertionError("Branding transformation incomplete");
        System.out.println("PASS Android runtime branding XML transformation");
        String hostile="<!DOCTYPE manifest [<!ENTITY x SYSTEM 'file:///data/local/tmp/private'>]><manifest/>";
        Files.write(root.resolve("AndroidManifest.xml"),hostile.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        boolean rejected=false;
        try { BrandingResources.apply(root,"com.google.android.youtube","Javo Video",Files.newInputStream(root.resolve("icon.xml"))); }
        catch(SecurityException expected) { rejected=true; }
        if(!rejected)throw new AssertionError("Android accepted external entity declaration");
        System.out.println("PASS Android runtime rejects external entity declaration");
    }
}
