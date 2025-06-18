package com.sk.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AESUtilTarget {

    @Test
    void test1() {
        String decrypt = AESUtil.decrypt("V9BqqhHvz+6NTgQhyRMPKWTyzb5Xt1/UGo13YxF5DcS8jmsVsA8KfNNVuupzmFKDnF4=", "mysecretmysecret");
        System.out.println(decrypt);
    }

}
