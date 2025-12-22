package com.sk.domain.shared.enums.news;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

public enum NewsSupplier {

    ACN("ACN", "Japan Corporate News Network K.K.(JCN)"),
    ACT("ACT", "Actus Finance & Communication"),
    ADX("ADX", "Abu Dhabi Securities Exchange (ADX)"),
    BEQ("BEQ", "BeQuoted"),
    BER("BER", "Pertubuhan Berita Nasional Malaysia"),
    BMV("BMV", "Bolsa Mexicana de Valores S.A. de C.V."),
    BMYS("BMYS", "Bursa Malaysia Information Sdn Bhd - 102193"),
    BOJP("BOJP", "Bank of Japan Announcements"),
    BSW("BSW", "Business Wire"),
    CAIXIN("CAIXIN", "Caixin Global"),
    CIS("CIS", "Waymaker AB"),
    CNMV("CNMV", "Bolsas y Mercados Espanoles Market Data S.A.U."),
    CNW("CNW", "PR Newswire"),
    COIN("COIN", "Coindesk"),
    CQ("CQ", "CQ Roll Call Group (Congressional Quarterly Inc)"),
    CSE("CSE", "NASDAQ Corporate Solutions"),
    DGP("DGP", "EQS Group AG (DGAP)"),
    DIR("DIR", "Infront Direkt"),
    EANS("EANS", "news aktuell GmbH and APA OTS Originaltext-Service GmbH"),
    EDLY("EDLY", "Edaily Co Ltd"),
    FSC("FSC", "Filing Services Canada"),
    GNW("GNW", "West publishing (prev NASDAQ; formerly Hugin)"),
    GNWN("GNWN", "West Publiishing (previously NASDAQ Corporate Solutions)"),
    GPWNC("GPWNC", "Warsaw Stock Exchange"),
    GTSM("GTSM", "Taipei Exchange"),
    HBIN("HBIN", "HBI International"),
    HSE("HSE", "NASDAQ Corporate Solutions"),
    HSE_BSW_GNW("HSE BSW GNW", "Business Wire (BSW) and NASDAQ Corporate Solutions (HSE), GlobeNewswire (ONE)"),
    JCN("JCN", "Japan Corporate News Network K.K.(JCN)"),
    JSE("JSE", "Johannesburg Stock Exchange"),
    LNGBN("LNGBN", "Longbo Information Technology"),
    MKN("MKN", "MAEKYUNG.COM.Inc.,"),
    MKW("MKW", "Marketwire - now owned by West Publishing"),
    MOPS("MOPS", "Taiwan Stock Exchange"),
    MSM("MSM", "Muscat Securities Market"),
    MTD("MTD", "Money Today Ltd"),
    NBSX("NBSX", "Namibia Stock Exchange"),
    NFC("NFC", "Newsfile"),
    NFRI("NFRI", "NewsRise"),
    OBI_OSL("OBI, OSL", "Oslo Bors ASA"),
    PLB("PLB", "Springer Fachmedien Wiesbaden GmbH"),
    PRN("PRN", "PR Newswire"),
    PTX("PTX", "Pressetext Nachrichtenagentur GmbH"),
    PUDT("PUDT", "Polymerupdate"),
    RSE("RSE", "NASDAQ Corporate Solutions"),
    RTRS("RTRS", "Tankan News"),
    SSN("SSN", "NASDAQ Corporate Solutions"),
    TEN("TEN", "Tensid AG Communication Services"),
    TEN_GNW("TEN, GNW", "GlobeNewswire (ONE)"),
    TLSE("TLSE", "NASDAQ Corporate Solutions"),
    TNW("TNW", "The Newswire CA"),
    TSE("TSE", "Tokyo Stock Exchange"),
    VSE("VSE", "Vilnius Stock Exchange"),
    ZTCJ("ZTCJ", "Zhitong Caijing");

    // 성능 최적화를 위한 code -> enum 매핑 캐시
    private static final Map<String, NewsSupplier> CODE_MAP =
        Arrays.stream(values())
            .collect(Collectors.toMap(NewsSupplier::getCode, Function.identity()));
    @Getter
    private final String code;
    @Getter
    private final String supplierName;

    NewsSupplier(String code, String name) {
        this.code = code;
        this.supplierName = name;
    }

    /**
     * Source code로 NewsSupplier를 조회합니다.
     *
     * @param code source code (예: "RTRS")
     * @return NewsSupplier enum, 없으면 null
     */
    public static NewsSupplier fromCode(String code) {
        return CODE_MAP.get(code);
    }

    /**
     * Source code로 supplier 이름을 조회합니다.
     *
     * @param code source code (예: "RTRS")
     * @return supplier 이름 (예: "Tankan News"), 없으면 null
     */
    public static String getSupplierNameByCode(String code) {
        NewsSupplier supplier = fromCode(code);
        return supplier != null ? supplier.getSupplierName() : null;
    }
}
