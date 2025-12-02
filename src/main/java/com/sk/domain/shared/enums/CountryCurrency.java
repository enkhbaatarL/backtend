package com.sk.domain.shared.enums;

public enum CountryCurrency {
    // ENUM_NAME("Country Name", "Alpha-2 Code", "Numeric Code", "Currency Code", "Currency Symbol")

    // --- 비-국가 항목 ---
    EURO_ZONE("Euro Zone", "EZ", null, "EUR", "€"),

    // --- 아시아/오세아니아 (Asia/Oceania) ---
    AUSTRALIA("Australia", "AU", "036", "AUD", "$"),
    AZERBAIJAN("Azerbaijan", "AZ", "031", "AZN", "₼"),
    BAHRAIN("Bahrain", "BH", "048", "BHD", "BD"),
    BANGLADESH("Bangladesh", "BD", "050", "BDT", "৳"),
    CHINA("China (Mainland)", "CN", "156", "CNY", "¥"),
    HONG_KONG("Hong Kong", "HK", "344", "HKD", "$"),
    INDIA("India", "IN", "356", "INR", "₹"),
    INDONESIA("Indonesia", "ID", "360", "IDR", "Rp"),
    IRAQ("Iraq", "IQ", "368", "IQD", "IQD"),
    ISRAEL("Israel", "IL", "376", "ILS", "₪"),
    JAPAN("Japan", "JP", "392", "JPY", "¥"),
    JORDAN("Jordan", "JO", "400", "JOD", "JOD"),
    KAZAKHSTAN("Kazakhstan", "KZ", "398", "KZT", "₸"),
    KUWAIT("Kuwait", "KW", "414", "KWD", "KD"),
    KYRGYZSTAN("Kyrgyzstan", "KG", "417", "KGS", "сом"),
    LEBANON("Lebanon", "LB", "422", "LBP", "L.L."),
    MALAYSIA("Malaysia", "MY", "458", "MYR", "RM"),
    MONGOLIA("Mongolia", "MN", "496", "MNT", "₮"),
    OMAN("Oman", "OM", "512", "OMR", "OMR"),
    PAKISTAN("Pakistan", "PK", "586", "PKR", "₨"),
    PALESTINIAN_TERRITORY("Palestinian Territory", "PS", "275", "ILS", "₪"),
    PHILIPPINES("Philippines", "PH", "608", "PHP", "₱"),
    QATAR("Qatar", "QA", "634", "QAR", "QR"),
    SAUDI_ARABIA("Saudi Arabia", "SA", "682", "SAR", "SR"),
    SINGAPORE("Singapore", "SG", "702", "SGD", "$"),
    SOUTH_KOREA("South Korea", "KR", "410", "KRW", "₩"),
    SRI_LANKA("Sri Lanka", "LK", "144", "LKR", "Rs"),
    TAIWAN("Taiwan", "TW", "158", "TWD", "NT$"),
    THAILAND("Thailand", "TH", "764", "THB", "฿"),
    TURKEY("Türkiye", "TR", "792", "TRY", "₺"),
    UNITED_ARAB_EMIRATES("United Arab Emirates", "AE", "784", "AED", "Dhs"),
    UZBEKISTAN("Uzbekistan", "UZ", "860", "UZS", "сум"),
    VIETNAM("Vietnam", "VN", "704", "VND", "₫"),
    NEW_ZEALAND("New Zealand", "NZ", "554", "NZD", "$"),

    // --- 유럽 (Europe) ---
    ALBANIA("Albania", "AL", "008", "ALL", "Lek"),
    AUSTRIA("Austria", "AT", "040", "EUR", "€"),
    BELGIUM("Belgium", "BE", "056", "EUR", "€"),
    BOSNIA_HERZEGOVINA("Bosnia-Herzegovina", "BA", "070", "BAM", "KM"),
    BULGARIA("Bulgaria", "BG", "100", "BGN", "лв"),
    CROATIA("Croatia", "HR", "191", "EUR", "€"),
    CYPRUS("Cyprus", "CY", "196", "EUR", "€"),
    CZECH_REPUBLIC("Czech Republic", "CZ", "203", "CZK", "Kč"),
    DENMARK("Denmark", "DK", "208", "DKK", "kr"),
    ESTONIA("Estonia", "EE", "233", "EUR", "€"),
    FINLAND("Finland", "FI", "246", "EUR", "€"),
    FRANCE("France", "FR", "250", "EUR", "€"),
    GERMANY("Germany", "DE", "276", "EUR", "€"),
    GREECE("Greece", "GR", "300", "EUR", "€"),
    HUNGARY("Hungary", "HU", "348", "HUF", "Ft"),
    ICELAND("Iceland", "IS", "352", "ISK", "kr"),
    IRELAND("Ireland", "IE", "372", "EUR", "€"),
    ITALY("Italy", "IT", "380", "EUR", "€"),
    LATVIA("Latvia", "LV", "428", "EUR", "€"),
    LITHUANIA("Lithuania", "LT", "440", "EUR", "€"),
    LUXEMBOURG("Luxembourg", "LU", "442", "EUR", "€"),
    MALTA("Malta", "MT", "470", "EUR", "€"),
    MONTENEGRO("Montenegro", "ME", "499", "EUR", "€"),
    NETHERLANDS("Netherlands", "NL", "528", "EUR", "€"),
    NORWAY("Norway", "NO", "578", "NOK", "kr"),
    POLAND("Poland", "PL", "616", "PLN", "zł"),
    PORTUGAL("Portugal", "PT", "620", "EUR", "€"),
    ROMANIA("Romania", "RO", "642", "RON", "lei"),
    RUSSIA("Russia", "RU", "643", "RUB", "₽"),
    SERBIA("Serbia", "RS", "688", "RSD", "дин"),
    SLOVAKIA("Slovakia", "SK", "703", "EUR", "€"),
    SLOVENIA("Slovenia", "SI", "705", "EUR", "€"),
    SPAIN("Spain", "ES", "724", "EUR", "€"),
    SWEDEN("Sweden", "SE", "752", "SEK", "kr"),
    SWITZERLAND("Switzerland", "CH", "756", "CHF", "CHF"),
    TUNISIA("Tunisia", "TN", "788", "TND", "DT"),
    UKRAINE("Ukraine", "UA", "804", "UAH", "₴"),
    UNITED_KINGDOM("United Kingdom", "GB", "826", "GBP", "£"),

    ARGENTINA("Argentina", "AR", "032", "ARS", "$"),
    BERMUDA("Bermuda", "BM", "060", "BMD", "$"),
    BRAZIL("Brazil", "BR", "076", "BRL", "R$"),
    CANADA("Canada", "CA", "124", "CAD", "$"),
    CAYMAN_ISLANDS("Cayman Islands", "KY", "136", "KYD", "$"),
    CHILE("Chile", "CL", "152", "CLP", "$"),
    COLOMBIA("Colombia", "CO", "170", "COP", "$"),
    COSTA_RICA("Costa Rica", "CR", "188", "CRC", "₡"),
    ECUADOR("Ecuador", "EC", "218", "USD", "$"),
    JAMAICA("Jamaica", "JM", "388", "JMD", "J$"),
    MEXICO("Mexico", "MX", "484", "MXN", "$"),
    PARAGUAY("Paraguay", "PY", "600", "PYG", "₲"),
    PERU("Peru", "PE", "604", "PEN", "S/"),
    UNITED_STATES("United States", "US", "840", "USD", "$"),
    URUGUAY("Uruguay", "UY", "858", "UYU", "$U"),
    VENEZUELA("Venezuela", "VE", "862", "VES", "Bs.S"),

    // --- 아프리카 (Africa) ---
    ANGOLA("Angola", "AO", "024", "AOA", "Kz"),
    BOTSWANA("Botswana", "BW", "072", "BWP", "P"),
    COTE_DIVOIRE("Cote D'Ivoire", "CI", "384", "XOF", "CFA"),
    EGYPT("Egypt", "EG", "818", "EGP", "E£"),
    GHANA("Ghana", "GH", "288", "GHS", "GH₵"),
    KENYA("Kenya", "KE", "404", "KES", "KSh"),
    MALAWI("Malawi", "MW", "454", "MWK", "MK"),
    MOROCCO("Morocco", "MA", "504", "MAD", "DH"),
    MOZAMBIQUE("Mozambique", "MZ", "508", "MZN", "MT"),
    NAMIBIA("Namibia", "NA", "516", "NAD", "$"),
    NIGERIA("Nigeria", "NG", "566", "NGN", "₦"),
    RWANDA("Rwanda", "RW", "646", "RWF", "RF"),
    SOUTH_AFRICA("South Africa", "ZA", "710", "ZAR", "R"),
    TANZANIA("Tanzania", "TZ", "834", "TZS", "TSh"),
    UGANDA("Uganda", "UG", "800", "UGX", "USh"),
    ZAMBIA("Zambia", "ZM", "894", "ZMW", "ZK"),
    ZIMBABWE("Zimbabwe", "ZW", "716", "ZWL", "Z$"),
    MAURITIUS("Mauritius", "MU", "480", "MUR", "Rs"),
    ;

    private final String countryName;
    private final String countryCode;
    private final String numericCode;
    private final String currencyCode;
    private final String currencySymbol;

    CountryCurrency(String countryName, String countryCode, String numericCode, String currencyCode, String currencySymbol) {
        this.countryName = countryName;
        this.countryCode = countryCode;
        this.numericCode = numericCode;
        this.currencyCode = currencyCode;
        this.currencySymbol = currencySymbol;
    }

    // --- 유틸리티 메서드: Country Name으로 조회 ---
    public static CountryCurrency getByCountryName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (CountryCurrency cc : values()) {
            if (cc.countryName != null && cc.countryName.equalsIgnoreCase(name)) {
                return cc;
            }
        }
        return null;
    }

    // --- 유틸리티 메서드: Country Code (Alpha-2)로 조회 ---
    public static CountryCurrency getByCountryCode(String code) {
        if (code == null || code.isEmpty()) { return null; }
        for (CountryCurrency cc : values()) {
            if (cc.countryCode != null && cc.countryCode.equalsIgnoreCase(code)) {
                return cc;
            }
        }
        return null;
    }

    // --- 유틸리티 메서드: Numeric Code로 조회 ---
    public static CountryCurrency getByNumericCode(String code) {
        if (code == null || code.isEmpty()) { return null; }
        for (CountryCurrency cc : values()) {
            if (cc.numericCode != null && cc.numericCode.equals(code)) {
                return cc;
            }
        }
        return null;
    }

    // --- Getter 메서드 ---
    public String getCountryName() { return countryName; }

    public String getCountryCode() { return countryCode; }

    public String getNumericCode() { return numericCode; }

    public String getCurrencyCode() { return currencyCode; }

    public String getCurrencySymbol() { return currencySymbol; }
}
