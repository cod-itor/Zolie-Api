package com.example.zolieapi.common;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Ages {

    public static final int MINIMUM_AGE = 13;
    public static final int ADULT_AGE = 18;

    public int of(LocalDate dateOfBirth) {
        return Period.between(dateOfBirth, LocalDate.now(ZoneOffset.UTC)).getYears();
    }

    public boolean isAdult(LocalDate dateOfBirth) {
        return of(dateOfBirth) >= ADULT_AGE;
    }

    public String group(LocalDate dateOfBirth) {
        return isAdult(dateOfBirth) ? "adult" : "teen";
    }
}
