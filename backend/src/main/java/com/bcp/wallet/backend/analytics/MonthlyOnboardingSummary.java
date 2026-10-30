package com.bcp.wallet.backend.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record MonthlyOnboardingSummary(
        String month,
        long startedCount,
        long newActiveUsers,
        Long medianCompletionSeconds,
        List<ChannelFigures> channels,
        AcquisitionFigures totals) {

    private static final int MONEY_SCALE = 2;

    public record AcquisitionFigures(long newActiveUsers, BigDecimal acquisitionSpendPen, BigDecimal cacPen) {

        public static AcquisitionFigures of(long newActiveUsers, BigDecimal spend) {
            BigDecimal cac = newActiveUsers == 0 ? null
                    : spend.divide(BigDecimal.valueOf(newActiveUsers), MONEY_SCALE, RoundingMode.HALF_UP);
            return new AcquisitionFigures(newActiveUsers, spend.setScale(MONEY_SCALE, RoundingMode.HALF_UP), cac);
        }
    }

    public record ChannelFigures(String channel, long newActiveUsers, BigDecimal acquisitionSpendPen,
            BigDecimal cacPen) {

        public static ChannelFigures of(String channel, long newActiveUsers, BigDecimal spend) {
            AcquisitionFigures figures = AcquisitionFigures.of(newActiveUsers, spend);
            return new ChannelFigures(channel, newActiveUsers, figures.acquisitionSpendPen(), figures.cacPen());
        }
    }
}
