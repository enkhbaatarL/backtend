package com.sk.domain.mi.dto;

import com.sk.domain.shared.enums.TimeUnit;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ChartRes(

    @Schema(description = "시가", example = "[1000, 1002]")
    Double[] open,

    @Schema(description = "고가", example = "[1010, 1005]")
    Double[] high,

    @Schema(description = "저가", example = "[995, 998]")
    Double[] low,

    @Schema(description = "종가", example = "[1005, 1000]")
    Double[] close,

    @Schema(description = "시간 단위 (MINUTE, HOUR, DAY)", example = "[1005, 1000]")
    TimeUnit timeUnit,

    @Schema(description = "분, 시간, 일자", example = "[\"20250301\", \"20250302\"]")
    String[] interval,

    @Schema(description = "이동평균선 + 볼린저밴드 묶음 리스트")
    List<MovingAverageBundle> movingAverageBundle

) {

    public record MovingAverageBundle(

        @Schema(description = "이동평균선의 윈도우 사이즈 (ex: 5일, 20일 등)", example = "5")
        int windowSize,

        @Schema(description = "이동평균선", example = "[1003, 1004]")
        Double[] ma,

        @Schema(description = "볼린저 밴드 묶음 (이동평균선 기준)")
        BollingerBandsBundle bollingerBandsBundle

    ) {

    }

    public record BollingerBandsBundle(

        @Schema(description = "볼린저 밴드의 윈도우 사이즈", example = "5")
        int windowSize,

        @Schema(description = "볼린저 밴드 상단", example = "[1012, 1013]")
        Double[] high,

        @Schema(description = "볼린저 밴드 중단 (이동평균선)", example = "[1003, 1004]")
        Double[] middle,

        @Schema(description = "볼린저 밴드 하단", example = "[994, 995]")
        Double[] low

    ) {

    }

}

