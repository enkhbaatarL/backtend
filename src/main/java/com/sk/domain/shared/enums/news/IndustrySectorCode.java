package com.sk.domain.shared.enums.news;

public enum IndustrySectorCode {
    FIN("Financials", "금융: 은행, 증권, 보험, 투자 서비스 등 금융과 관련된 모든 기업을 포함"),
    ENE("Energy", "에너지: 석유, 가스, 석탄과 같은 전통적인 에너지 자원뿐만 아니라 태양광, 풍력 등 신재생 에너지 기업도 포함"),
    TMT("Technology, Media, Telecommunications (TMT)",
        "기술, 미디어, 통신: 소프트웨어, IT 서비스, 반도체, 미디어 콘텐츠 제작, 방송, 유/무선 통신 등 기술 및 정보 전달과 관련된 산업"),
    IND("Industrials & Manufacturing (Mfg.)",
        "산업재 및 제조업: 항공우주, 건설, 기계, 전기장비, 방위산업 등 다양한 산업재를 생산하거나 제조업에 속한 기업들을 포함"),
    MAT("Materials & Mining", "소재 및 광업: 화학, 건축자재, 포장재, 철강, 금속, 광물 채굴 등 원자재 생산 및 가공과 관련된 산업"),
    AUT("Automotive & Mobility",
        "자동차 및 모빌리티: 자동차, 오토바이, 전기차, 자율주행 기술, 차량 공유 서비스 등 운송수단 및 이동성 관련 산업을 포함"),
    HEA("Healthcare", "헬스케어: 제약, 생명공학, 의료 장비, 병원 서비스 등 보건 및 의료와 관련된 모든 분야를 포함"),
    CON("Consumer & Retail",
        "소비재 및 소매업: 의류, 식품, 가전제품, 식료품점, 백화점 등 소비자가 직접 구매하는 상품 및 소매 서비스를 제공하는 기업들을 포함"),
    TRA("Transportation & Logistics",
        "운송 및 물류: 항공, 해운, 철도, 육상 운송 및 창고, 물류 관리 등 상품 및 사람의 이동과 관련된 산업"),
    REA("Real Estate", "부동산: 부동산 개발, 임대, 관리, 투자 등 부동산 자산과 관련된 모든 활동을 포함"),
    UTI("Utilities", "유틸리티: 전기, 가스, 수도 등 공공 서비스와 관련된 기업들을 포함");

    private final String name;
    private final String description;

    private IndustrySectorCode(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
