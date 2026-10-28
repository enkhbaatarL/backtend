package com.sk.domain.mi.service.fundamentalmodel.input;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** 커서는 권한을 부여하지 않는다. 검증된 현재 소유/표/revision 범위에서 마지막 항목만 식별한다. */
final class InputDraftChangeCursor {
    private final byte[] secret;
    InputDraftChangeCursor(String secret) {
        if(secret==null||secret.getBytes(StandardCharsets.UTF_8).length<32)throw InputDraftException.conflict("POLICY_UNSUPPORTED",null);
        this.secret=secret.getBytes(StandardCharsets.UTF_8);
    }
    String encode(String scope,int offset) {
        String payload=Base64.getUrlEncoder().withoutPadding().encodeToString(("1\n"+scope+"\n"+offset).getBytes(StandardCharsets.UTF_8));
        return payload+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(sign(payload));
    }
    int decode(String token,String scope) {
        try {
            if(token==null)return 0;if(token.isEmpty()||token.length()>4096||!token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"))throw InputDraftException.invalid();
            String[] parts=token.split("\\.");if(!MessageDigest.isEqual(sign(parts[0]),Base64.getUrlDecoder().decode(parts[1])))throw InputDraftException.invalid();
            String payload=new String(Base64.getUrlDecoder().decode(parts[0]),StandardCharsets.UTF_8),prefix="1\n"+scope+"\n";
            if(!payload.startsWith(prefix))throw InputDraftException.invalid();int offset=Integer.parseInt(payload.substring(prefix.length()));if(offset<0)throw InputDraftException.invalid();return offset;
        }catch(IllegalArgumentException ex){throw InputDraftException.invalid();}
    }
    private byte[] sign(String payload) {
        try {Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));}
        catch(java.security.GeneralSecurityException ex){throw InputDraftException.internal();}
    }
}
