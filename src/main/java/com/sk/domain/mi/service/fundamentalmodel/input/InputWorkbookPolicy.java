package com.sk.domain.mi.service.fundamentalmodel.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sk.domain.shared.repository.master.tmm.ModelInputDraftRepository.TableRef;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 발행 정책과 실제 구현한 표별 처리의 교집합만 허용한다. 색/빈 셀로 권한을 추정하지 않는다. */
final class InputWorkbookPolicy {
    enum Kind { VALUES, ROWS, COST, UPSTREAM, ALLOCATION, SHIPPING_COST, SHIPPING_DISRUPTION, SHIPPING_TECHNICAL, SHIPPING_ROUTE, MACRO, HYDROGEN, WEATHER_LOGIC, BCINPUT, BCPRODUCTION, PRODUCTION, HURRICANE, HURRICANE_ADJUSTMENT, ANNUAL, LNG, SECTOR, MONTHLY, LIQPROD, LIQUTIL, REGAS, REGAS_MONTHLY, UNSUPPORTED }

    static Kind kind(TableRef table) {
        if (table.role().equals("DEMAND") && table.sheet().equals("PMILogicInput") && Set.of(2, 7).contains(table.startColumn())) return Kind.VALUES;
        if (table.role().equals("DEMAND") && table.sheet().equals("MacroParametersLogicInput") && table.startColumn() == 9) return Kind.MACRO;
        if (table.role().equals("TRADE") && Set.of("LNG_TradeRestrictionInput", "PNG_TradeRestrictionInput", "LNGExportRestictionInput").contains(table.sheet())) return Kind.ROWS;
        if (midstreamContracts(table)) return Kind.VALUES;
        if (table.role().equals("TRADE") && table.sheet().equals("MidstreamCapacityInput")) {
            if (table.startColumn() == 128) return Kind.ALLOCATION;
            if (table.startColumn() == 158) return Kind.ROWS;
        }
        if (table.role().equals("TRADE") && table.sheet().equals("ShippingCostInput")) {
            if (table.startColumn() == 2 && Set.of(2, 15, 26, 37, 48).contains(table.startRow())) return Kind.SHIPPING_COST;
            if (table.startColumn() == 2 && Set.of(59, 70).contains(table.startRow())) return Kind.SHIPPING_DISRUPTION;
            if (table.startColumn() == 29) return Kind.SHIPPING_ROUTE;
            if (Set.of(42, 46).contains(table.startColumn())) return Kind.ROWS;
            if (table.startColumn() == 2 && table.startRow() == 81) return Kind.SHIPPING_TECHNICAL;
            if (table.startColumn() == 2 && table.startRow() == 86) return Kind.VALUES;
        }
        if (table.role().equals("TRADE") && table.sheet().equals("UpstreamAdjLogicInput") && Set.of(9, 20).contains(table.startColumn())) return Kind.UPSTREAM;
        if (table.role().equals("TRADE") && table.sheet().equals("DemandAdjLogicInput")) {
            if (table.startColumn() == 1) return Kind.ROWS;
            if (Set.of(3, 8).contains(table.startColumn())) return Kind.VALUES;
        }
        if (table.role().equals("TRADE") && table.sheet().equals("CostInput") && Set.of(1, 8, 15, 22, 29, 37).contains(table.startColumn())) return Kind.COST;
        if (table.role().equals("SUPPLY") && table.sheet().equals("BCHurricanes")) {
            if (table.startColumn() == 2) return Kind.HURRICANE;
            if (table.startColumn() == 9) return table.dataStart() == 16 ? Kind.ROWS : Kind.VALUES;
        }
        if (table.role().equals("SUPPLY") && table.sheet().equals("ProductionAdjustment") && table.startColumn() == 8) return Kind.PRODUCTION;
        if (table.role().equals("SUPPLY") && table.sheet().equals("BCProduction") && table.startColumn() == 1) return Kind.BCPRODUCTION;
        if (table.role().equals("SUPPLY") && table.sheet().equals("{USA}Hurricanes") && table.startColumn() == 2) return Kind.HURRICANE_ADJUSTMENT;
        if (table.role().equals("DEMAND") && table.sheet().equals("WeatherLogicInput") && Set.of(2, 7, 12, 17).contains(table.startColumn())) return Kind.WEATHER_LOGIC;
        if (table.role().equals("DEMAND") && table.sheet().equals("HydrogenInput") && table.startColumn() == 6) return Kind.HYDROGEN;
        if (table.role().equals("DEMAND") && table.sheet().equals("HydrogenInput") && table.startColumn() == 17) return Kind.VALUES;
        if (table.role().equals("DEMAND") && table.sheet().equals("LNGBunkeringInput") && table.startColumn() == 8) return Kind.ROWS;
        if (table.role().equals("DEMAND") && table.sheet().equals("BCInput") && Set.of(3, 17, 26, 37, 43, 49, 54, 59).contains(table.startColumn())) return Kind.BCINPUT;
        if (table.role().equals("SUPPLY") && table.sheet().equals("RegasAdjustment") && table.startColumn() == 2) return Kind.REGAS_MONTHLY;
        if (table.role().equals("SUPPLY") && table.sheet().equals("BCRegasification") && Set.of(1, 2).contains(table.startColumn())) return Kind.REGAS;
        if (table.role().equals("SUPPLY") && table.sheet().equals("LiqProdAdjustment") && table.startColumn() == 2) return Kind.LIQPROD;
        if (table.role().equals("SUPPLY") && table.sheet().equals("LiqUtilAdjustment") && table.startColumn() == 2) return Kind.LIQUTIL;
        if (table.role().equals("DEMAND") && table.sheet().equals("LNG Demand") && table.startColumn() == 2) return Kind.LNG;
        if (table.role().equals("DEMAND") && table.sheet().equals("DemandOutputOverwrite")) {
            if (table.startColumn() == 4 && table.endColumn() == 11) return Kind.SECTOR;
            if (table.startColumn() == 13 && table.endColumn() == 19) return Kind.MONTHLY;
        }
        if (table.role().equals("OUTPUT") && table.sheet().equals("USCostofSupply")) return Kind.VALUES;
        if (table.role().equals("OUTPUT") && Set.of("PremiumCalc", "PriceSeasonality").contains(table.sheet())) return Kind.VALUES;
        if (table.role().equals("OUTPUT") && table.sheet().equals("BCPrices")) return Kind.ROWS;
        if (table.role().equals("SUPPLY") && table.sheet().equals("BCLiquefaction") && table.startColumn() == 2) return Kind.ROWS;
        if (table.role().equals("TRADE") && table.sheet().equals("4.5 PriceModelInput") && Set.of(35, 39).contains(table.startColumn())) return Kind.ROWS;
        if (table.role().equals("SUPPLY") && table.sheet().equals("BCPipeline")) return Kind.ROWS;
        if (table.role().equals("DEMAND") && Set.of("GDPPopulationAdjustment", "ElectrificationRateAdjustment",
                "PowerMixAdjustment", "CoalGasAdjustment", "WeatherAdjustment", "PMIAdjustment",
                "HydrogenAdjustment", "LNGBunkeringAdjustment").contains(table.sheet())) return Kind.ROWS;
        if (annualHeader(table) != null) return Kind.ANNUAL;
        return Kind.UNSUPPORTED;
    }

    static Integer annualHeader(TableRef table) {
        if (table.role().equals("SUPPLY") && table.sheet().equals("BCRegasification") && Set.of(1, 2).contains(table.startColumn())) return 7;
        if (table.role().equals("DEMAND")) {
            if (table.sheet().equals("LNG Demand") && table.startColumn() == 2) return 3;
            if (table.sheet().equals("PMIInput") && table.startColumn() == 22) return 8;
            if (table.sheet().equals("WeatherInput") && Set.of(74, 92, 139).contains(table.startColumn())) return 8;
            if (table.sheet().equals("LNGBunkeringLogicInput") && table.startColumn() == 2) return 7;
        }
        if (table.role().equals("TRADE") && table.sheet().equals("MidstreamCapacityInput") && table.startColumn() == 2) return 3;
        return null;
    }

    static boolean supports(TableRef table, InputReadDefinition definition, String action) {
        if (definition == null || definition.bodyRanges().isEmpty()
                || table.scopes().isEmpty() && definition.columnCreation() == null) return false;
        Kind kind = kind(table);
        if (kind == Kind.UNSUPPORTED) return false;
        if (midstreamContracts(table)) {
            int first = table.startColumn(), last = first == 1 ? 4 : 21, end = first == 1 ? 1104 : 2918;
            if (table.startRow() != 2 || table.endRow() != end || table.endColumn() != last || table.dataStart() != 5
                    || !definition.headerRows().equals(List.of(2, 3, 4))
                    || !definition.bodyRanges().equals(List.of(new InputReadDefinition.Range(5, end)))
                    || !definition.columns().equals(java.util.stream.IntStream.rangeClosed(first, last).boxed().toList())
                    || table.scopes().stream().anyMatch(s -> s.startRow() < 5 || s.endRow() > end
                        || s.startColumn() < first || s.endColumn() > last)) return false;
        }
        if (table.role().equals("DEMAND") && table.sheet().equals("PMILogicInput")
                && (table.startRow() != 7 || table.endRow() < 92 || table.endColumn() != table.startColumn() + 3
                    || !definition.headerRows().equals(List.of(7, 8))
                    || !definition.bodyRanges().equals(List.of(new InputReadDefinition.Range(9, table.endRow())))
                    || table.scopes().stream().anyMatch(s -> s.startRow() < 9 || s.endRow() > table.endRow()
                        || s.startColumn() < table.endColumn() - 1 || s.endColumn() > table.endColumn()))) return false;
        if (infrastructure(table) && (table.startRow() != 1 || !definition.headerRows().equals(List.of(1, 2, 3))
                || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 11)
                || definition.columns().stream().anyMatch(c -> !infrastructureInput(c))
                || table.scopes().stream().anyMatch(s -> s.startRow() < 11 || java.util.stream.IntStream.rangeClosed(s.startColumn(), s.endColumn()).anyMatch(c -> !infrastructureInput(c))))) return false;
        if (table.role().equals("TRADE") && table.sheet().equals("MidstreamCapacityInput") && Set.of(128, 158).contains(table.startColumn())) {
            int inputLast = table.startColumn() == 128 ? 132 : 160, last = table.startColumn() == 128 ? 133 : 160;
            if (table.startRow() != 1 || table.endColumn() != last || !definition.headerRows().equals(List.of(1, 2, 3))
                    || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 4)
                    || table.scopes().stream().anyMatch(s -> s.startRow() < 4 || s.startColumn() < table.startColumn() || s.endColumn() > inputLast)) return false;
        }
        if (table.role().equals("TRADE") && Set.of("LNG_TradeRestrictionInput", "PNG_TradeRestrictionInput", "LNGExportRestictionInput").contains(table.sheet())) {
            boolean export = table.sheet().equals("LNGExportRestictionInput"); int first = export ? 4 : 6, last = export ? 5 : table.sheet().startsWith("LNG_") ? 11 : 10;
            if (table.startRow() != (export ? 2 : 5) || table.startColumn() != 2 || table.endColumn() != last
                    || !definition.headerRows().equals(export ? List.of(2, 3) : List.of(5))
                    || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < first)
                    || table.scopes().stream().anyMatch(s -> s.startRow() < first || s.startColumn() < 2 || s.endColumn() > last)) return false;
        }
        if (kind == Kind.MACRO && (table.startRow() != 7 || table.endColumn() != 17 || !definition.headerRows().equals(List.of(7, 8))
                || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 9)
                || table.scopes().stream().anyMatch(s -> s.startRow() < 9 || s.startColumn() < 9 || s.endColumn() > 16))) return false;
        if (kind == Kind.WEATHER_LOGIC && (table.startRow() != 7 || table.endColumn() != table.startColumn() + 3 || !definition.headerRows().equals(List.of(7, 8))
                || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 9)
                || table.scopes().stream().anyMatch(s -> s.startRow() < 9 || s.startColumn() < table.startColumn() + 2 || s.endColumn() > table.endColumn()))) return false;
        if (table.role().equals("DEMAND") && table.sheet().equals("HydrogenInput") && table.startColumn() == 17
                && (table.startRow() != 7 || table.endRow() != 8 || table.endColumn() != 19 || !definition.headerRows().equals(List.of(7))
                    || !definition.bodyRanges().equals(List.of(new InputReadDefinition.Range(8, 8)))
                    || table.scopes().stream().anyMatch(s -> s.startRow() != 8 || s.endRow() != 8 || s.startColumn() != 18 || s.endColumn() != 18))) return false;
        if (kind == Kind.HYDROGEN && (table.startRow() != 7 || table.endColumn() != 8 || !definition.headerRows().equals(List.of(7, 8))
                || !definition.bodyRanges().get(0).equals(new InputReadDefinition.Range(9, 736))
                || table.scopes().stream().anyMatch(s -> s.startColumn() < 6 || s.endColumn() > 8
                    || definition.bodyRanges().stream().noneMatch(r -> s.startRow() >= r.startRow() && s.endRow() <= r.endRow())))) return false;
        if (table.role().equals("DEMAND") && table.sheet().equals("LNGBunkeringInput")
                && (table.startRow() != 7 || table.endColumn() != 11 || !definition.headerRows().equals(List.of(7, 8))
                    || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 9)
                    || table.scopes().stream().anyMatch(s -> s.startRow() < 9 || s.startColumn() < 8 || s.endColumn() > 11))) return false;
        if (table.role().equals("TRADE") && table.sheet().equals("ShippingCostInput")) {
            int first = table.startColumn();
            if (first == 42 || first == 46) {
                int last = first == 42 ? 43 : 46;
                if (table.startRow() != 2 || table.endColumn() != last || !definition.headerRows().equals(List.of(2, 3))
                        || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 4)
                        || table.scopes().stream().anyMatch(s -> s.startRow() < 4 || s.startColumn() < first || s.endColumn() > last)) return false;
            } else if (kind == Kind.SHIPPING_ROUTE) {
                if (table.startRow() != 2 || table.endColumn() != 39 || !definition.headerRows().equals(List.of(2, 3))
                        || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 4)
                        || table.scopes().stream().anyMatch(s -> s.startRow() < 4 || !(s.startColumn() >= 29 && s.endColumn() <= 31
                            || s.startColumn() >= 34 && s.endColumn() <= 35 || s.startColumn() >= 38 && s.endColumn() <= 39))) return false;
            } else if (shippingStacked(table)) {
                boolean disruption = kind == Kind.SHIPPING_DISRUPTION, charter = table.startRow() <= 15;
                int begin = table.startRow() == 2 ? 6 : table.startRow() + 2;
                List<Integer> headers = table.startRow() == 2 ? List.of(3, 4, 5) : List.of(table.startRow(), table.startRow() + 1);
                if (table.endColumn() != (disruption ? 4 : 6) || !definition.headerRows().equals(headers)
                        || !definition.bodyRanges().get(0).equals(new InputReadDefinition.Range(begin, begin + 7))
                        || charter && !definition.columns().equals(List.of(2, 4, 6))
                        || table.scopes().stream().anyMatch(s -> definition.bodyRanges().stream().noneMatch(r -> s.startRow() >= r.startRow() && s.endRow() <= r.endRow())
                            || java.util.stream.IntStream.rangeClosed(s.startColumn(), s.endColumn()).anyMatch(c -> disruption ? c < 2 || c > 3 : c != 2 && c != 4 && c != 6 && (charter || c != 3)))) return false;
            } else {
                boolean technical = kind == Kind.SHIPPING_TECHNICAL; int header = technical ? 81 : 86, begin = technical ? 83 : 88, end = technical ? 83 : 90;
                if (table.endRow() != end || table.endColumn() != 5 || !definition.headerRows().equals(List.of(header, header + 1))
                        || !definition.bodyRanges().equals(List.of(new InputReadDefinition.Range(begin, end)))
                        || table.scopes().stream().anyMatch(s -> s.startRow() < begin || s.endRow() > end || (technical
                            ? s.startColumn() != s.endColumn() || !Set.of(2, 5).contains(s.startColumn()) : s.startColumn() < 2 || s.endColumn() > 5))) return false;
            }
        }
        if (kind == Kind.UPSTREAM) {
            boolean supply = table.startColumn() == 9;
            if (table.startRow() != 1 || table.endColumn() != (supply ? 18 : 21)
                    || !definition.headerRows().equals(supply ? List.of(1, 2) : List.of(1, 2, 3))
                    || supply && !definition.bodyRanges().equals(List.of(new InputReadDefinition.Range(3, 674)))
                    || !supply && definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 4)
                    || table.scopes().stream().anyMatch(s -> supply
                        ? s.startRow() < 3 || s.endRow() > 674 || !(s.startColumn() >= 9 && s.endColumn() <= 10 || s.startColumn() == 16 && s.endColumn() == 16)
                        : s.startRow() < 4 || s.startColumn() != 20 || s.endColumn() != 20)) return false;
        }
        if (table.role().equals("TRADE") && table.sheet().equals("DemandAdjLogicInput") && table.startColumn() == 1
                && (table.startRow() != 1 || table.endColumn() != 1 || !definition.headerRows().equals(List.of(1, 2))
                    || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 3)
                    || table.scopes().stream().anyMatch(s -> s.startRow() < 3 || s.startColumn() != 1 || s.endColumn() != 1))) return false;
        if (table.role().equals("TRADE") && table.sheet().equals("DemandAdjLogicInput") && table.startColumn() == 3
                && (table.startRow() != 1 || table.endRow() != 3 || table.endColumn() != 6 || !definition.headerRows().equals(List.of(1))
                    || !definition.bodyRanges().equals(List.of(new InputReadDefinition.Range(2, 3)))
                    || table.scopes().stream().anyMatch(s -> s.startRow() < 2 || s.endRow() > 3 || s.startColumn() != s.endColumn() || !Set.of(4, 6).contains(s.startColumn())))) return false;
        if (table.role().equals("TRADE") && table.sheet().equals("DemandAdjLogicInput") && table.startColumn() == 8
                && (table.startRow() != 1 || table.endRow() != 7 || table.endColumn() != 10 || !definition.headerRows().equals(List.of(1, 2))
                    || !definition.bodyRanges().equals(List.of(new InputReadDefinition.Range(3, 7)))
                    || table.scopes().stream().anyMatch(s -> s.startRow() < 3 || s.endRow() > 7 || s.startColumn() < 8 || s.endColumn() > 10))) return false;
        if (kind == Kind.COST) {
            int inputLast = table.startColumn() + (table.startColumn() == 29 ? 4 : 3);
            if (table.startRow() != 1 || table.endColumn() != inputLast + (table.startColumn() == 37 ? 4 : 2) || !definition.headerRows().equals(List.of(1, 2))
                    || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 3)
                    || table.scopes().stream().anyMatch(s -> s.startRow() < 3 || s.startColumn() < table.startColumn() || s.endColumn() > inputLast)) return false;
        }
        if (kind == Kind.HURRICANE_ADJUSTMENT && (table.startRow() != 3 || table.endColumn() != 9 || !definition.headerRows().equals(List.of(7))
                || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 13)
                || table.scopes().stream().anyMatch(s -> s.startRow() < 13 || s.startColumn() < 2 || s.endColumn() > 4))) return false;
        if (kind == Kind.BCPRODUCTION && (table.startRow() != 7 || table.endColumn() != 11 || !definition.headerRows().equals(List.of(7))
                || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 8)
                || table.scopes().stream().anyMatch(s -> s.startRow() < 8 || !(s.startColumn() >= 3 && s.endColumn() <= 6 || s.startColumn() >= 8 && s.endColumn() <= 11)))) return false;
        if (table.role().equals("SUPPLY") && table.sheet().equals("BCLiquefaction")
                && (table.startRow() != 7 || table.endColumn() != 14 || !definition.headerRows().equals(List.of(7))
                    || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 8)
                    || table.scopes().stream().anyMatch(s -> s.startRow() < 8 || !(s.startColumn() >= 2 && s.endColumn() <= 3
                        || s.startColumn() >= 5 && s.endColumn() <= 12 || s.startColumn() == 14 && s.endColumn() == 14)))) return false;
        if (table.role().equals("TRADE") && table.sheet().equals("4.5 PriceModelInput")
                && (table.startRow() != 2 || table.endColumn() != table.startColumn() + 2 || !definition.headerRows().equals(List.of(2, 3))
                    || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 4)
                    || table.scopes().stream().anyMatch(s -> s.startRow() < 4 || s.startColumn() < table.startColumn() || s.endColumn() > table.endColumn()))) return false;
        if (kind == Kind.HURRICANE && (table.endColumn() != 7 || !(table.dataStart() == 8 && table.endRow() == 11 || table.dataStart() == 16 && table.endRow() == 19)
                || !definition.bodyRanges().equals(List.of(new InputReadDefinition.Range(table.dataStart(), table.endRow())))
                || table.scopes().stream().anyMatch(s -> s.startRow() < table.dataStart() || s.endRow() > table.endRow() || s.startColumn() < 2 || s.endColumn() > 6))) return false;
        if (table.role().equals("SUPPLY") && table.sheet().equals("BCHurricanes") && table.startColumn() == 9
                && (table.endColumn() != 11 || !(table.dataStart() == 8 && table.endRow() == 13 || table.dataStart() == 16 && table.endRow() >= 41)
                    || table.scopes().stream().anyMatch(s -> s.startRow() < table.dataStart() || s.endRow() > table.endRow()
                        || s.startColumn() != s.endColumn() || !Set.of(9, 11).contains(s.startColumn())))) return false;
        if (kind == Kind.PRODUCTION && (table.startRow() != 3 || table.endColumn() != 15 || !definition.headerRows().equals(List.of(7))
                || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 8)
                || table.scopes().stream().anyMatch(s -> s.startRow() < 8 || !(s.startColumn() >= 9 && s.endColumn() <= 11 || s.startColumn() == 13 && s.endColumn() == 13)))) return false;
        if (kind == Kind.BCINPUT) {
            int last = Map.of(3, 14, 17, 24, 26, 35, 37, 41, 43, 47, 49, 51, 54, 56, 59, 63).get(table.startColumn());
            int inputLast = table.startColumn() == 17 ? 22 : table.startColumn() == 37 ? 40 : last;
            if (table.startRow() != 2 || table.endColumn() != last || !definition.headerRows().equals(List.of(2, 3))
                    || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 4)
                    || table.scopes().stream().anyMatch(s -> s.startRow() < 4 || s.startColumn() < table.startColumn() || s.endColumn() > inputLast
                        || table.startColumn() == 26 && s.startColumn() <= 33 && s.endColumn() >= 31)) return false;
        }
        if (kind == Kind.REGAS && (table.startRow() != 7 || table.endColumn() < 18 || !definition.headerRows().equals(List.of(7))
                || definition.columns().contains(table.endColumn()) || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 8))) return false;
        // 기존 용량 READY는 A Helper를 포함하는 연결 계산 경계가 있어야 조회에도 지원으로 표시한다.
        if (kind == Kind.REGAS && table.startColumn() != 1 && Set.of("SET", "CLEAR").contains(action))
            for (JsonNode field : definition.fields()) if (field.path("columnNumber").asInt() < 18
                    && field.path("policyStatus").path(action).asText().equals("READY")) return false;
        if (kind == Kind.REGAS_MONTHLY && (table.startRow() != 8 || table.endColumn() != 126 || !definition.headerRows().equals(List.of(8, 9, 10))
                || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 11)
                || table.scopes().stream().anyMatch(s -> s.startRow() < 11 || s.startColumn() < 8 || s.endColumn() > 31))) return false;
        if (months(table) && (table.startRow() != 7 || !definition.headerRows().equals(List.of(7, 8, 9, 10))
                || definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 11)
                || table.scopes().stream().anyMatch(s -> s.startRow() < 11 || s.startColumn() < 2))) return false;
        if (table.sheet().equals("USCostofSupply") && (definition.bodyRanges().stream().anyMatch(r -> r.startRow() < 8 || r.endRow() > 816)
                || table.scopes().stream().anyMatch(s -> s.startRow() < 8 || s.endRow() > 816 || s.startColumn() < 2 || s.endColumn() > 12))) return false;
        if (table.role().equals("OUTPUT") && table.sheet().equals("PremiumCalc") && table.scopes().stream().anyMatch(s -> !(
                s.startColumn() == 3 && s.endColumn() == 3 && (s.startRow() >= 39 && s.endRow() <= 47 || s.startRow() >= 57 && s.endRow() <= 61)
                || s.startRow() == 49 && s.endRow() == 49 && s.startColumn() >= 6 && s.endColumn() <= 7))) return false;
        if (table.role().equals("OUTPUT") && table.sheet().equals("PriceSeasonality") && table.scopes().stream().anyMatch(s -> s.startRow() < 2 || s.endRow() > 4 || s.startColumn() < 3 || s.endColumn() > 15)) return false;
        return switch (action) {
            case "SET", "CLEAR" -> true;
            case "ADD_ROW" -> (Set.of(Kind.ROWS, Kind.COST, Kind.MACRO, Kind.SHIPPING_COST, Kind.SHIPPING_DISRUPTION, Kind.SHIPPING_ROUTE, Kind.ALLOCATION, Kind.HYDROGEN, Kind.BCINPUT, Kind.BCPRODUCTION, Kind.PRODUCTION, Kind.HURRICANE_ADJUSTMENT, Kind.ANNUAL, Kind.SECTOR, Kind.MONTHLY, Kind.LIQPROD, Kind.LIQUTIL).contains(kind)
                || InputDraftPmiRows.user(table) || kind == Kind.UPSTREAM && table.startColumn() == 20 || kind == Kind.WEATHER_LOGIC && table.startColumn() >= 12 || kind == Kind.REGAS && table.startColumn() == 1) && table.rowAdd();
            case "ADD_COLUMN" -> (kind == Kind.ANNUAL || kind == Kind.LNG || kind == Kind.REGAS || months(table)) && table.columnAdd();
            case "DELETE_ROW" -> infrastructure(table) || kind == Kind.SECTOR || kind == Kind.MONTHLY || kind == Kind.LIQUTIL || kind == Kind.REGAS && table.startColumn() == 1;
            case "DELETE_COLUMN" -> infrastructure(table) || months(table);
            default -> false;
        };
    }

    static void validate(ModelInputDraftService.Source source, Map<Long, Map<Integer, String>> bindings,
            Set<String> addedRowIds, Set<String> addedFieldKeys,
            InputDraftJson.ChangeRequest request, long revision) {
        Map<String, String> addedRows = new HashMap<>();
        Map<String, JsonNode> addedColumns = new HashMap<>();
        Map<String, Map<String, JsonNode>> newValues = new LinkedHashMap<>();
        Map<String, JsonNode> rowCommands = new HashMap<>();
        Set<String> deletedRows = new HashSet<>();
        Set<String> deletedColumns = new HashSet<>();
        for (JsonNode command : request.operations()) {
            TableRef table = source.catalog().tables().stream().filter(t -> t.key().equals(command.path("tableId").asText())).findFirst().orElseThrow(InputDraftException::missing);
            InputReadDefinition definition = source.definitions().get(table.id());
            String action = command.path("type").asText();
            if (!supports(table, definition, action)) throw InputDraftException.conflict("POLICY_UNSUPPORTED", revision);
            policy(definition.capabilities().path(action).asText(), revision, command, request, "ACTION_NOT_ALLOWED");
            if (command.has("controlId")) {
                validateControl(table, definition, command, request, revision);
                continue;
            }
            if (action.equals("ADD_ROW")) {
                String client = command.path("clientRowKey").asText();
                if (addedRows.putIfAbsent(client, table.key()) != null) throw InputDraftException.invalid();
                Map<String, JsonNode> values = new LinkedHashMap<>();
                for (JsonNode field : definition.fields()) if (field.path("policyStatus").path("CREATE").asText().equals("READY")) {
                    String key = field.path("fieldKey").asText();
                    JsonNode value = command.path("values").has(key) ? command.path("values").get(key) : initial(field.path("create"));
                    if (value != null) value(field, value, field.path("create").path("constraints"), command, request, revision);
                    values.put(key, value);
                }
                command.path("values").fieldNames().forEachRemaining(key -> {
                    JsonNode field = field(definition, key);
                    policy(field.path("policyStatus").path("CREATE").asText(), revision, command, request, "ACTION_NOT_ALLOWED");
                });
                newValues.put(client, values); rowCommands.put(client, command);
                continue;
            }
            if (action.equals("ADD_COLUMN")) {
                JsonNode creation = definition.columnCreation();
                if (creation == null) throw InputDraftException.conflict("POLICY_INCOMPLETE", revision);
                if (kind(table) == Kind.REGAS && (!creation.path("body").path("dataType").asText().equals("DECIMAL")
                        || !creation.path("body").path("create").path("required").asBoolean()
                        || initial(creation.path("body").path("create")) == null
                        || !initial(creation.path("body").path("create")).isNumber()
                        || initial(creation.path("body").path("create")).decimalValue().signum() != 0))
                    throw InputDraftException.conflict("POLICY_INCOMPLETE", revision);
                Set<String> allowed = new HashSet<>(); creation.path("headerValues").fieldNames().forEachRemaining(allowed::add);
                command.path("headerValues").fieldNames().forEachRemaining(key -> { if (!allowed.contains(key)) throw InputDraftException.invalid(); });
                for (String key : allowed) {
                    JsonNode rule = creation.path("headerValues").path(key), submitted = command.path("headerValues").get(key);
                    if (submitted == null) submitted = initial(rule.path("create"));
                    if (submitted == null && rule.path("create").path("required").asBoolean()) throw invalid(command, request, revision, "REQUIRED");
                    if (submitted != null) value(rule, submitted, rule.path("create").path("constraints"), command, request, revision);
                }
                Set<String> headers = kind(table) == Kind.LIQUTIL ? Set.of() : kind(table) == Kind.LIQPROD ? Set.of("year", "month") : Set.of("year");
                if (!allowed.equals(headers) || headers.stream().anyMatch(key -> !command.path("headerValues").has(key))) throw InputDraftException.conflict("POLICY_INCOMPLETE", revision);
                ObjectNode created = createdField(creation, deletableAddedColumn(table, definition));
                ((ObjectNode) created.path("policyStatus")).put("SET_HEADER", newHeaderPolicy(table, definition));
                addedColumns.put(command.path("clientColumnKey").asText(), created);
                continue;
            }
            String columnIdentity = table.key() + "/" + (command.has("fieldKey") ? command.path("fieldKey").asText() : command.path("clientColumnKey").asText());
            if (deletedColumns.contains(columnIdentity)) throw InputDraftException.invalid();
            if (action.equals("DELETE_COLUMN")) {
                JsonNode field = command.has("fieldKey") ? field(definition, command.path("fieldKey").asText()) : addedColumns.get(command.path("clientColumnKey").asText());
                if (field == null) throw InputDraftException.invalid();
                if (command.has("fieldKey") && field.path("columnNumber").asInt() < 7) throw invalid(command, request, revision, "ACTION_NOT_ALLOWED");
                if (infrastructure(table) && command.has("fieldKey") && !addedFieldKeys.contains(table.key() + "/" + command.path("fieldKey").asText()))
                    throw invalid(command, request, revision, "ACTION_NOT_ALLOWED");
                policy(field.path("policyStatus").path(action).asText(), revision, command, request, "ACTION_NOT_ALLOWED");
                deletedColumns.add(columnIdentity); continue;
            }
            if (command.has("headerKey")) {
                JsonNode field = command.has("fieldKey") ? field(definition, command.path("fieldKey").asText()) : addedColumns.get(command.path("clientColumnKey").asText());
                if (field == null) throw InputDraftException.invalid();
                policy(field.path("policyStatus").path("SET_HEADER").asText(), revision, command, request, "ACTION_NOT_ALLOWED");
                if (!supportsHeader(table, command.has("fieldKey") ? field.path("columnNumber").asInt() : table.endColumn() + 1)) throw InputDraftException.conflict("POLICY_UNSUPPORTED", revision);
                if (!command.path("headerKey").asText().equals("year") || !command.path("value").isIntegralNumber()) throw InputDraftException.invalid();
                continue;
            }
            JsonNode ref = command.path("rowRef");
            String client = ref.path("clientRowKey").asText(); Integer row = null;
            if (ref.has("rowId")) row = bindings.getOrDefault(table.id(), Map.of()).entrySet().stream()
                .filter(e -> e.getValue().equals(ref.path("rowId").asText())).map(Map.Entry::getKey).findFirst().orElseThrow(InputDraftException::missing);
            else if (!table.key().equals(addedRows.get(client))) throw InputDraftException.invalid();
            String identity = ref.has("rowId") ? ref.path("rowId").asText() : client;
            if (deletedRows.contains(identity)) throw InputDraftException.invalid();
            if (action.equals("DELETE_ROW")) {
                if (infrastructure(table) && ref.has("rowId") && !addedRowIds.contains(ref.path("rowId").asText()))
                    throw invalid(command, request, revision, "ACTION_NOT_ALLOWED");
                deletedRows.add(identity); continue;
            }
            JsonNode field = command.has("fieldKey") ? field(definition, command.path("fieldKey").asText()) : addedColumns.get(command.path("clientColumnKey").asText());
            if (field == null) throw InputDraftException.invalid();
            // 기존 master 입력은 A Helper를 포함한 연결 정의에서만 처리한다.
            if (kind(table) == Kind.REGAS && table.startColumn() != 1 && row != null && command.has("fieldKey") && field.path("columnNumber").asInt() < 18)
                throw InputDraftException.conflict("POLICY_UNSUPPORTED", revision);
            if (kind(table) == Kind.LNG && row != null && lngProtected(row, field.path("columnNumber").asInt())) throw invalid(command, request, revision, "ACTION_NOT_ALLOWED");
            policy(field.path("policyStatus").path(action).asText(), revision, command, request, action.equals("CLEAR") ? "CLEAR_NOT_ALLOWED" : "ACTION_NOT_ALLOWED");
            if (command.has("fieldKey")) {
                int col = field.path("columnNumber").asInt(); final Integer existing = row;
                if (kind(table) == Kind.LNG && action.equals("CLEAR") && Integer.valueOf(6).equals(row) && col == 5) throw invalid(command, request, revision, "CLEAR_NOT_ALLOWED");
                if (table.role().equals("OUTPUT") && table.sheet().equals("PriceSeasonality") && action.equals("CLEAR") && Integer.valueOf(2).equals(row) && col == 3) throw invalid(command, request, revision, "CLEAR_NOT_ALLOWED");
                if (table.scopes().stream().noneMatch(s -> col >= s.startColumn() && col <= s.endColumn()
                        && (existing == null || existing >= s.startRow() && existing <= s.endRow()))) throw invalid(command, request, revision, "ACTION_NOT_ALLOWED");
            }
            JsonNode submitted = action.equals("CLEAR") ? null : command.get("value");
            // 빈 값 정책은 미정이다. 기존행의 비어 있지 않은 입력만 열고 필수값 정책을 추론하지 않는다.
            if (midstreamContracts(table) && submitted != null && submitted.isTextual() && submitted.asText().isBlank())
                throw InputDraftException.conflict("POLICY_INCOMPLETE", revision);
            if (submitted != null) value(field, submitted, field.path("setConstraints"), command, request, revision);
            if (!client.isEmpty() && command.has("fieldKey")) newValues.get(client).put(command.path("fieldKey").asText(), submitted);
        }
        for (var addition : newValues.entrySet()) {
            if (deletedRows.contains(addition.getKey())) continue;
            JsonNode command = rowCommands.get(addition.getKey());
            TableRef table = source.catalog().tables().stream().filter(t -> t.key().equals(command.path("tableId").asText())).findFirst().orElseThrow();
            for (JsonNode field : source.definitions().get(table.id()).fields()) if (field.path("create").path("required").asBoolean()) {
                JsonNode value = addition.getValue().get(field.path("fieldKey").asText());
                if (value == null || value.isNull() || value.isTextual() && value.asText().isBlank()) throw invalidField(field, command, request, revision, "REQUIRED");
            }
        }
    }

    static ObjectNode createdField(JsonNode creation) {
        return createdField(creation, false);
    }
    static boolean months(TableRef table) { return kind(table) == Kind.LIQPROD || kind(table) == Kind.LIQUTIL; }
    static boolean deletableAddedColumn(TableRef table, InputReadDefinition definition) {
        return months(table) || infrastructure(table) && definition.capabilities().path("DELETE_COLUMN").asText().equals("READY");
    }
    static ObjectNode createdField(JsonNode creation, boolean delete) {
        ObjectNode field = creation.path("body").deepCopy();
        boolean required = field.path("create").path("required").asBoolean();
        field.putObject("policyStatus").put("CREATE", "READY").put("SET", "READY").put("CLEAR", required ? "NOT_ALLOWED" : "READY").put("DELETE_COLUMN", delete ? "READY" : "NOT_ALLOWED");
        field.set("setConstraints", field.path("create").path("constraints").deepCopy());
        return field;
    }

    private static boolean midstreamContracts(TableRef table) {
        return table.role().equals("TRADE") && table.sheet().equals("MidstreamContractInput") && Set.of(1, 17).contains(table.startColumn());
    }
    static boolean infrastructure(TableRef table) { return table.role().equals("TRADE") && table.sheet().equals("MidstreamCapacityInput") && table.startColumn() == 2; }
    private static boolean infrastructureInput(int column) { return Set.of(2, 4, 7).contains(column) || column >= 10 && column <= 17 || column >= 170; }
    static boolean supportsHeader(TableRef table, int column) {
        return kind(table) == Kind.LNG && table.startRow() == 3 && column >= 5;
    }
    static String newHeaderPolicy(TableRef table, InputReadDefinition definition) {
        if (kind(table) != Kind.LNG) return "NOT_ALLOWED";
        for (JsonNode field : definition.fields()) if (field.path("policyStatus").path("SET_HEADER").asText().equals("READY")) return "READY";
        return "INCOMPLETE";
    }
    static boolean lngProtected(int row, int column) {
        return column >= 5 && InputWorkbookPeriods.LNG_TOTALS.contains(row)
            || column == 2 && Set.of(4, 109).contains(row)
            || column == 3 && Set.of(5, 10, 15, 25, 27, 35, 43).contains(row);
    }
    static JsonNode control(InputReadDefinition definition, String id) {
        for (JsonNode control : definition.controls()) if (control.path("id").asText().equals(id)) return control;
        throw InputDraftException.invalid();
    }
    static boolean supportsControl(TableRef table, JsonNode control) {
        return control.path("binding").path("columnIndex").asInt() == 2
            && (kind(table) == Kind.LNG && table.startRow() == 3 && table.startColumn() == 2 && control.path("binding").path("rowNumber").asInt() == 1
                || kind(table) == Kind.SHIPPING_COST && control.path("binding").path("rowNumber").asInt() == 3);
    }
    static boolean shippingStacked(TableRef table) { return Set.of(Kind.SHIPPING_COST, Kind.SHIPPING_DISRUPTION).contains(kind(table)); }
    private static void validateControl(TableRef table, InputReadDefinition definition, JsonNode command,
            InputDraftJson.ChangeRequest request, long revision) {
        JsonNode control = control(definition, command.path("controlId").asText());
        if (!control.path("editable").asBoolean()) throw invalid(command, request, revision, "ACTION_NOT_ALLOWED");
        if (!supportsControl(table, control)) throw InputDraftException.conflict("POLICY_UNSUPPORTED", revision);
        JsonNode value = command.path("value");
        for (JsonNode option : control.path("options")) {
            JsonNode allowed = option.path("value");
            if (value.isNumber() && allowed.isNumber() ? value.decimalValue().compareTo(allowed.decimalValue()) == 0 : value.equals(allowed)) return;
        }
        throw invalid(command, request, revision, "OPTION_NOT_ALLOWED", null, "/value");
    }
    static JsonNode initial(JsonNode create) { return create.path("initial").path("kind").asText().equals("VALUE") ? create.path("initial").get("value") : null; }
    static JsonNode field(InputReadDefinition definition, String key) {
        for (JsonNode field : definition.fields()) if (field.path("fieldKey").asText().equals(key)) return field;
        throw InputDraftException.invalid();
    }
    private static void value(JsonNode field, JsonNode value, JsonNode constraints, JsonNode command, InputDraftJson.ChangeRequest request, long revision) {
        String type = field.path("dataType").asText();
        if (type.equals("DECIMAL_OR_TEXT") ? !value.isTextual() && !value.isNumber()
                : type.equals("TEXT") ? !value.isTextual() : !value.isNumber()) throw InputDraftException.invalid();
        if (constraints.has("required") && !constraints.path("required").isBoolean()) throw InputDraftException.conflict("POLICY_INCOMPLETE", revision);
        if (value.isTextual() && value.asText().isBlank()
                && (field.path("create").path("required").asBoolean() || constraints.path("required").asBoolean()))
            throw invalidField(field, command, request, revision, "REQUIRED");
        if (type.equals("INTEGER")) try { value.decimalValue().toBigIntegerExact(); } catch (ArithmeticException ex) { throw InputDraftException.invalid(); }
        if (!constraints.isObject()) throw InputDraftException.conflict("POLICY_INCOMPLETE", revision);
        var keys = constraints.fieldNames(); while (keys.hasNext()) if (!Set.of("minimum", "maximum", "options", "required").contains(keys.next())) throw InputDraftException.conflict("POLICY_UNSUPPORTED", revision);
        boolean valid = !constraints.has("minimum") || value.isNumber() && value.decimalValue().compareTo(constraints.path("minimum").decimalValue()) >= 0;
        valid &= !constraints.has("maximum") || value.isNumber() && value.decimalValue().compareTo(constraints.path("maximum").decimalValue()) <= 0;
        if (!valid) throw invalidField(field, command, request, revision, "OUT_OF_RANGE");
        if (constraints.has("options")) {
            boolean found = false; for (JsonNode option : constraints.path("options")) if (option.isNumber() && value.isNumber() ? option.decimalValue().compareTo(value.decimalValue()) == 0 : option.equals(value)) found = true;
            if (!found) throw invalidField(field, command, request, revision, "OPTION_NOT_ALLOWED");
        }
    }
    private static void policy(String status, long revision, JsonNode command, InputDraftJson.ChangeRequest request, String reason) {
        if (status.equals("NOT_ALLOWED")) throw invalid(command, request, revision, reason);
        if (!status.equals("READY")) throw InputDraftException.conflict(status.equals("INCOMPLETE") ? "POLICY_INCOMPLETE" : "POLICY_UNSUPPORTED", revision);
    }
    static InputDraftException invalid(JsonNode command, InputDraftJson.ChangeRequest request, long revision, String reason) {
        String suffix = reason.startsWith("DUPLICATE_") ? command.path("type").asText().equals("ADD_ROW") ? "/values" : "/rowRef" : "";
        return invalid(command, request, revision, reason, command.has("fieldKey") && !reason.startsWith("DUPLICATE_") ? command.path("fieldKey").asText() : null, suffix);
    }
    private static InputDraftException invalidField(JsonNode field, JsonNode command, InputDraftJson.ChangeRequest request, long revision, String reason) {
        String key = field.has("fieldKey") ? field.path("fieldKey").asText() : null;
        String suffix = command.path("type").asText().equals("ADD_ROW") && key != null ? "/values/" + key : command.path("type").asText().equals("SET") ? "/value" : "/headerValues";
        return invalid(command, request, revision, reason, key, suffix);
    }
    private static InputDraftException invalid(JsonNode command, InputDraftJson.ChangeRequest request, long revision, String reason, String field, String suffix) {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("commandId", command.path("commandId").asText()); issue.put("tableId", command.path("tableId").asText());
        issue.put("rowRef", command.has("rowRef") ? command.path("rowRef") : command.has("clientRowKey") ? Map.of("clientRowKey", command.path("clientRowKey").asText()) : null);
        issue.put("fieldKey", field);
        issue.put("path", "/operations/" + request.operations().indexOf(command) + suffix); issue.put("reason", reason);
        return InputDraftException.validation(revision, List.of(issue));
    }
}
