import app.javo.security.UpdateSecurity;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public class UpdateSecurityTest {
    static int passed=0;
    static void ok(boolean value,String label){if(!value)throw new AssertionError(label);passed++;System.out.println("PASS "+label);}
    interface Action{void run()throws Exception;}
    static void rejects(Action action,String label)throws Exception{try{action.run();}catch(Exception e){passed++;System.out.println("PASS "+label);return;}throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        var generator=KeyPairGenerator.getInstance("EC");generator.initialize(new ECGenParameterSpec("secp256r1"));
        var pair=generator.generateKeyPair();var b64=Base64.getEncoder();
        var payload="authentic release metadata".getBytes(StandardCharsets.UTF_8);
        var signer=Signature.getInstance("SHA256withECDSA");signer.initSign(pair.getPrivate());signer.update(payload);
        var signature=b64.encodeToString(signer.sign());var key=b64.encodeToString(pair.getPublic().getEncoded());
        ok(java.util.Arrays.equals(payload,UpdateSecurity.verifyPayload(b64.encodeToString(payload),signature,key)),"valid ECDSA manifest");
        rejects(()->UpdateSecurity.verifyPayload(b64.encodeToString("tampered".getBytes()),signature,key),"tampered manifest");
        var other=generator.generateKeyPair();
        rejects(()->UpdateSecurity.verifyPayload(b64.encodeToString(payload),signature,b64.encodeToString(other.getPublic().getEncoded())),"wrong manifest key");
        rejects(()->UpdateSecurity.verifyPayload("a".repeat(262145),signature,key),"oversized manifest");
        rejects(()->UpdateSecurity.verifyPayload(b64.encodeToString(payload),signature,""),"unconfigured key");
        for(var url:new String[]{"http://example.org/a.apk","file:///a.apk","https://u:p@example.org/a.apk","https://example.org:8080/a.apk","https://example.org/a.apk#x"})rejects(()->UpdateSecurity.requireHttps(url),"unsafe URL "+url);
        UpdateSecurity.requireHttps("https://example.org/a.apk");passed++;System.out.println("PASS valid HTTPS URL");
        ok(UpdateSecurity.sha256("abc".getBytes()).equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),"SHA256 known vector");
        var file=Files.createTempFile("javo-security-test", ".bin");try{Files.write(file,payload);ok(UpdateSecurity.sha256(file.toFile()).equals(UpdateSecurity.sha256(payload)),"streaming file checksum");}finally{Files.delete(file);}
        System.out.println(passed+" Java security checks passed");
    }
}
