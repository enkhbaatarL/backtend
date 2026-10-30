package com.sk.domain.mi.service.fundamentalmodel.input;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** 커서는 권한을 부여하지 않는다. 검증된 현재 소유/표/revision 범위에서 마지막 항목만 식별한다. */
final class InputDraftChangeCursor {
    String encode(String scope,int offset) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(("2\n"+scope+"\n"+offset).getBytes(StandardCharsets.UTF_8));
    }
    int decode(String token,String scope) {
        try {
            if(token==null)return 0;if(token.isEmpty()||token.length()>4096||!token.matches("[A-Za-z0-9_-]+"))throw InputDraftException.invalid();
            String payload=new String(Base64.getUrlDecoder().decode(token),StandardCharsets.UTF_8),prefix="2\n"+scope+"\n";
            if(!payload.startsWith(prefix))throw InputDraftException.invalid();String position=payload.substring(prefix.length());
            if(!position.matches("[0-9]+"))throw InputDraftException.invalid();return Integer.parseInt(position);
        }catch(IllegalArgumentException ex){throw InputDraftException.invalid();}
    }
}
