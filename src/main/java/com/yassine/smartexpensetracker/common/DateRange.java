package com.yassine.smartexpensetracker.common;

import java.time.LocalDate;

public record DateRange(LocalDate from, LocalDate to) {

    public static DateRange defaultLast12Months(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = (to != null) ? to : LocalDate.now();
        LocalDate effectiveFrom = (from != null)
                ? from
                : effectiveTo.withDayOfMonth(1).minusMonths(11);

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("'from' must be <= 'to'");
        }
        return new DateRange(effectiveFrom, effectiveTo);
    }
}
