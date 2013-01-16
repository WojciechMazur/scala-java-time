/*
 * Copyright (c) 2007-2013, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.threeten.bp;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.threeten.bp.temporal.ChronoUnit.NANOS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.threeten.bp.format.DateTimeParseException;
import org.threeten.bp.temporal.TemporalQuery;

/**
 * Test Instant.
 */
@Test
public class TestInstant extends AbstractTest {

    //-----------------------------------------------------------------------
    @Test(groups={"implementation"})
    public void test_interfaces() {
        assertTrue(Serializable.class.isAssignableFrom(Duration.class));
        assertTrue(Comparable.class.isAssignableFrom(Duration.class));
    }

    @Test(groups={"tck"})
    public void test_zero() {
        assertEquals(Instant.EPOCH.getEpochSecond(), 0L);
        assertEquals(Instant.EPOCH.getNano(), 0);
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void now() {
        Instant expected = Instant.now(Clock.systemUTC());
        Instant test = Instant.now();
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = Instant.now(Clock.systemUTC());
            test = Instant.now();
        }
        assertEquals(test, expected);
    }

    //-----------------------------------------------------------------------
    // now(Clock)
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class, groups={"tck"})
    public void now_Clock_nullClock() {
        Instant.now(null);
    }

    @Test(groups={"tck"})
    public void now_Clock_allSecsInDay_utc() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant expected = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(expected, ZoneOffset.UTC);
            Instant test = Instant.now(clock);
            assertEquals(test, expected);
        }
    }

    @Test(groups={"tck"})
    public void now_Clock_allSecsInDay_beforeEpoch() {
        for (int i =-1; i >= -(24 * 60 * 60); i--) {
            Instant expected = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(expected, ZoneOffset.UTC);
            Instant test = Instant.now(clock);
            assertEquals(test, expected);
        }
    }

    //-----------------------------------------------------------------------
    // ofEpochSecond(long)
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_seconds_long() {
        for (long i = -2; i <= 2; i++) {
            Instant t = Instant.ofEpochSecond(i);
            assertEquals(t.getEpochSecond(), i);
            assertEquals(t.getNano(), 0);
        }
    }

    //-----------------------------------------------------------------------
    // ofEpochSecond(long,long)
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void factory_seconds_long_long() {
        for (long i = -2; i <= 2; i++) {
            for (int j = 0; j < 10; j++) {
                Instant t = Instant.ofEpochSecond(i, j);
                assertEquals(t.getEpochSecond(), i);
                assertEquals(t.getNano(), j);
            }
            for (int j = -10; j < 0; j++) {
                Instant t = Instant.ofEpochSecond(i, j);
                assertEquals(t.getEpochSecond(), i - 1);
                assertEquals(t.getNano(), j + 1000000000);
            }
            for (int j = 999999990; j < 1000000000; j++) {
                Instant t = Instant.ofEpochSecond(i, j);
                assertEquals(t.getEpochSecond(), i);
                assertEquals(t.getNano(), j);
            }
        }
    }

    @Test(groups={"tck"})
    public void factory_seconds_long_long_nanosNegativeAdjusted() {
        Instant test = Instant.ofEpochSecond(2L, -1);
        assertEquals(test.getEpochSecond(), 1);
        assertEquals(test.getNano(), 999999999);
    }

    @Test(expectedExceptions=ArithmeticException.class, groups={"tck"})
    public void factory_seconds_long_long_tooBig() {
        Instant.ofEpochSecond(Long.MAX_VALUE, 1000000000);
    }

    //-----------------------------------------------------------------------
    // ofEpochMilli(long)
    //-----------------------------------------------------------------------
    @DataProvider(name="MillisInstantNoNanos")
    Object[][] provider_factory_millis_long() {
        return new Object[][] {
            {0, 0, 0},
            {1, 0, 1000000},
            {2, 0, 2000000},
            {999, 0, 999000000},
            {1000, 1, 0},
            {1001, 1, 1000000},
            {-1, -1, 999000000},
            {-2, -1, 998000000},
            {-999, -1, 1000000},
            {-1000, -1, 0},
            {-1001, -2, 999000000},
        };
    }

    @Test(dataProvider="MillisInstantNoNanos", groups={"tck"})
    public void factory_millis_long(long millis, long expectedSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochMilli(millis);
        assertEquals(t.getEpochSecond(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    //-----------------------------------------------------------------------
    // parse(String)
    //-----------------------------------------------------------------------
    @DataProvider(name="Parse")
    Object[][] provider_factory_parse() {
        return new Object[][] {
            {"1970-01-01T00:00:00Z", 0, 0},
            {"1970-01-01t00:00:00Z", 0, 0},
            {"1970-01-01T00:00:00z", 0, 0},
            {"1970-01-01T00:00:00.0Z", 0, 0},
            {"1970-01-01T00:00:00.000000000Z", 0, 0},

            {"1970-01-01T00:00:00.000000001Z", 0, 1},
            {"1970-01-01T00:00:00.100000000Z", 0, 100000000},
            {"1970-01-01T00:00:01Z", 1, 0},
            {"1970-01-01T00:01:00Z", 60, 0},
            {"1970-01-01T00:01:01Z", 61, 0},
            {"1970-01-01T00:01:01.000000001Z", 61, 1},
            {"1970-01-01T01:00:00.000000000Z", 3600, 0},
            {"1970-01-01T01:01:01.000000001Z", 3661, 1},
            {"1970-01-02T01:01:01.100000000Z", 90061, 100000000},
        };
    }

    @Test(dataProvider="Parse")
    public void factory_parse(String text, long expectedEpochSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.parse(text);
        assertEquals(t.getEpochSecond(), expectedEpochSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(dataProvider="Parse")
    public void factory_parseLowercase(String text, long expectedEpochSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.parse(text.toLowerCase(Locale.ENGLISH));
        assertEquals(t.getEpochSecond(), expectedEpochSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

// TODO: should comma be accepted?
//    @Test(dataProvider="Parse")
//    public void factory_parse_comma(String text, long expectedEpochSeconds, int expectedNanoOfSecond) {
//        text = text.replace('.', ',');
//        Instant t = Instant.parse(text);
//        assertEquals(t.getEpochSecond(), expectedEpochSeconds);
//        assertEquals(t.getNano(), expectedNanoOfSecond);
//    }

    @DataProvider(name="ParseFailures")
    Object[][] provider_factory_parseFailures() {
        return new Object[][] {
            {""},
            {"Z"},
            {"1970-01-01T00:00:00"},
            {"1970-01-01T00:00:0Z"},
            {"1970-01-01T00:00:00.0000000000Z"},
        };
    }

    @Test(dataProvider="ParseFailures", expectedExceptions=DateTimeParseException.class)
    public void factory_parseFailures(String text) {
        Instant.parse(text);
    }

    @Test(dataProvider="ParseFailures", expectedExceptions=DateTimeParseException.class)
    public void factory_parseFailures_comma(String text) {
        text = text.replace('.', ',');
        Instant.parse(text);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_nullText() {
        Instant.parse(null);
    }

    //-----------------------------------------------------------------------
    // serialization
    //-----------------------------------------------------------------------
    @Test( groups={"implementation"})
    public void test_deserializationSingleton() throws Exception {
        Instant orginal = Instant.EPOCH;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(orginal);
        out.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bais);
        Instant ser = (Instant) in.readObject();
        assertSame(ser, Instant.EPOCH);
    }

    @Test( groups={"tck"})
    public void test_deserialization() throws Exception {
        Instant orginal = Instant.ofEpochSecond(2);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(orginal);
        out.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream in = new ObjectInputStream(bais);
        Instant ser = (Instant) in.readObject();
        assertEquals(Instant.ofEpochSecond(2), ser);
    }

    //-----------------------------------------------------------------------
    // query(Query)
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void test_query_chrono() {
        assertEquals(Instant.EPOCH.query(TemporalQuery.CHRONO), null);
    }

    @Test(groups={"tck"})
    public void test_query_zone() {
        assertEquals(Instant.EPOCH.query(TemporalQuery.ZONE_ID), null);
    }

    @Test(groups={"tck"})
    public void test_query_timePrecision() {
        assertEquals(Instant.EPOCH.query(TemporalQuery.TIME_PRECISION), NANOS);
    }

    @Test(expectedExceptions=NullPointerException.class, groups={"tck"})
    public void test_query_null() {
        Instant.EPOCH.query(null);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="Plus")
    Object[][] provider_plus() {
        return new Object[][] {
            {Long.MIN_VALUE, 0, Long.MAX_VALUE, 0, -1, 0},

            {Long.MIN_VALUE, 0, 1, 0, Long.MIN_VALUE + 1, 0},
            {Long.MIN_VALUE, 0, 0, 500, Long.MIN_VALUE, 500},
            {Long.MIN_VALUE, 0, 0, 1000000000, Long.MIN_VALUE + 1, 0},

            {Long.MIN_VALUE + 1, 0, -1, 0, Long.MIN_VALUE, 0},
            {Long.MIN_VALUE + 1, 0, 0, -500, Long.MIN_VALUE, 999999500},
            {Long.MIN_VALUE + 1, 0, 0, -1000000000, Long.MIN_VALUE, 0},

            {-4, 666666667, -4, 666666667, -7, 333333334},
            {-4, 666666667, -3,         0, -7, 666666667},
            {-4, 666666667, -2,         0, -6, 666666667},
            {-4, 666666667, -1,         0, -5, 666666667},
            {-4, 666666667, -1, 333333334, -4,         1},
            {-4, 666666667, -1, 666666667, -4, 333333334},
            {-4, 666666667, -1, 999999999, -4, 666666666},
            {-4, 666666667,  0,         0, -4, 666666667},
            {-4, 666666667,  0,         1, -4, 666666668},
            {-4, 666666667,  0, 333333333, -3,         0},
            {-4, 666666667,  0, 666666666, -3, 333333333},
            {-4, 666666667,  1,         0, -3, 666666667},
            {-4, 666666667,  2,         0, -2, 666666667},
            {-4, 666666667,  3,         0, -1, 666666667},
            {-4, 666666667,  3, 333333333,  0,         0},

            {-3, 0, -4, 666666667, -7, 666666667},
            {-3, 0, -3,         0, -6,         0},
            {-3, 0, -2,         0, -5,         0},
            {-3, 0, -1,         0, -4,         0},
            {-3, 0, -1, 333333334, -4, 333333334},
            {-3, 0, -1, 666666667, -4, 666666667},
            {-3, 0, -1, 999999999, -4, 999999999},
            {-3, 0,  0,         0, -3,         0},
            {-3, 0,  0,         1, -3,         1},
            {-3, 0,  0, 333333333, -3, 333333333},
            {-3, 0,  0, 666666666, -3, 666666666},
            {-3, 0,  1,         0, -2,         0},
            {-3, 0,  2,         0, -1,         0},
            {-3, 0,  3,         0,  0,         0},
            {-3, 0,  3, 333333333,  0, 333333333},

            {-2, 0, -4, 666666667, -6, 666666667},
            {-2, 0, -3,         0, -5,         0},
            {-2, 0, -2,         0, -4,         0},
            {-2, 0, -1,         0, -3,         0},
            {-2, 0, -1, 333333334, -3, 333333334},
            {-2, 0, -1, 666666667, -3, 666666667},
            {-2, 0, -1, 999999999, -3, 999999999},
            {-2, 0,  0,         0, -2,         0},
            {-2, 0,  0,         1, -2,         1},
            {-2, 0,  0, 333333333, -2, 333333333},
            {-2, 0,  0, 666666666, -2, 666666666},
            {-2, 0,  1,         0, -1,         0},
            {-2, 0,  2,         0,  0,         0},
            {-2, 0,  3,         0,  1,         0},
            {-2, 0,  3, 333333333,  1, 333333333},

            {-1, 0, -4, 666666667, -5, 666666667},
            {-1, 0, -3,         0, -4,         0},
            {-1, 0, -2,         0, -3,         0},
            {-1, 0, -1,         0, -2,         0},
            {-1, 0, -1, 333333334, -2, 333333334},
            {-1, 0, -1, 666666667, -2, 666666667},
            {-1, 0, -1, 999999999, -2, 999999999},
            {-1, 0,  0,         0, -1,         0},
            {-1, 0,  0,         1, -1,         1},
            {-1, 0,  0, 333333333, -1, 333333333},
            {-1, 0,  0, 666666666, -1, 666666666},
            {-1, 0,  1,         0,  0,         0},
            {-1, 0,  2,         0,  1,         0},
            {-1, 0,  3,         0,  2,         0},
            {-1, 0,  3, 333333333,  2, 333333333},

            {-1, 666666667, -4, 666666667, -4, 333333334},
            {-1, 666666667, -3,         0, -4, 666666667},
            {-1, 666666667, -2,         0, -3, 666666667},
            {-1, 666666667, -1,         0, -2, 666666667},
            {-1, 666666667, -1, 333333334, -1,         1},
            {-1, 666666667, -1, 666666667, -1, 333333334},
            {-1, 666666667, -1, 999999999, -1, 666666666},
            {-1, 666666667,  0,         0, -1, 666666667},
            {-1, 666666667,  0,         1, -1, 666666668},
            {-1, 666666667,  0, 333333333,  0,         0},
            {-1, 666666667,  0, 666666666,  0, 333333333},
            {-1, 666666667,  1,         0,  0, 666666667},
            {-1, 666666667,  2,         0,  1, 666666667},
            {-1, 666666667,  3,         0,  2, 666666667},
            {-1, 666666667,  3, 333333333,  3,         0},

            {0, 0, -4, 666666667, -4, 666666667},
            {0, 0, -3,         0, -3,         0},
            {0, 0, -2,         0, -2,         0},
            {0, 0, -1,         0, -1,         0},
            {0, 0, -1, 333333334, -1, 333333334},
            {0, 0, -1, 666666667, -1, 666666667},
            {0, 0, -1, 999999999, -1, 999999999},
            {0, 0,  0,         0,  0,         0},
            {0, 0,  0,         1,  0,         1},
            {0, 0,  0, 333333333,  0, 333333333},
            {0, 0,  0, 666666666,  0, 666666666},
            {0, 0,  1,         0,  1,         0},
            {0, 0,  2,         0,  2,         0},
            {0, 0,  3,         0,  3,         0},
            {0, 0,  3, 333333333,  3, 333333333},

            {0, 333333333, -4, 666666667, -3,         0},
            {0, 333333333, -3,         0, -3, 333333333},
            {0, 333333333, -2,         0, -2, 333333333},
            {0, 333333333, -1,         0, -1, 333333333},
            {0, 333333333, -1, 333333334, -1, 666666667},
            {0, 333333333, -1, 666666667,  0,         0},
            {0, 333333333, -1, 999999999,  0, 333333332},
            {0, 333333333,  0,         0,  0, 333333333},
            {0, 333333333,  0,         1,  0, 333333334},
            {0, 333333333,  0, 333333333,  0, 666666666},
            {0, 333333333,  0, 666666666,  0, 999999999},
            {0, 333333333,  1,         0,  1, 333333333},
            {0, 333333333,  2,         0,  2, 333333333},
            {0, 333333333,  3,         0,  3, 333333333},
            {0, 333333333,  3, 333333333,  3, 666666666},

            {1, 0, -4, 666666667, -3, 666666667},
            {1, 0, -3,         0, -2,         0},
            {1, 0, -2,         0, -1,         0},
            {1, 0, -1,         0,  0,         0},
            {1, 0, -1, 333333334,  0, 333333334},
            {1, 0, -1, 666666667,  0, 666666667},
            {1, 0, -1, 999999999,  0, 999999999},
            {1, 0,  0,         0,  1,         0},
            {1, 0,  0,         1,  1,         1},
            {1, 0,  0, 333333333,  1, 333333333},
            {1, 0,  0, 666666666,  1, 666666666},
            {1, 0,  1,         0,  2,         0},
            {1, 0,  2,         0,  3,         0},
            {1, 0,  3,         0,  4,         0},
            {1, 0,  3, 333333333,  4, 333333333},

            {2, 0, -4, 666666667, -2, 666666667},
            {2, 0, -3,         0, -1,         0},
            {2, 0, -2,         0,  0,         0},
            {2, 0, -1,         0,  1,         0},
            {2, 0, -1, 333333334,  1, 333333334},
            {2, 0, -1, 666666667,  1, 666666667},
            {2, 0, -1, 999999999,  1, 999999999},
            {2, 0,  0,         0,  2,         0},
            {2, 0,  0,         1,  2,         1},
            {2, 0,  0, 333333333,  2, 333333333},
            {2, 0,  0, 666666666,  2, 666666666},
            {2, 0,  1,         0,  3,         0},
            {2, 0,  2,         0,  4,         0},
            {2, 0,  3,         0,  5,         0},
            {2, 0,  3, 333333333,  5, 333333333},

            {3, 0, -4, 666666667, -1, 666666667},
            {3, 0, -3,         0,  0,         0},
            {3, 0, -2,         0,  1,         0},
            {3, 0, -1,         0,  2,         0},
            {3, 0, -1, 333333334,  2, 333333334},
            {3, 0, -1, 666666667,  2, 666666667},
            {3, 0, -1, 999999999,  2, 999999999},
            {3, 0,  0,         0,  3,         0},
            {3, 0,  0,         1,  3,         1},
            {3, 0,  0, 333333333,  3, 333333333},
            {3, 0,  0, 666666666,  3, 666666666},
            {3, 0,  1,         0,  4,         0},
            {3, 0,  2,         0,  5,         0},
            {3, 0,  3,         0,  6,         0},
            {3, 0,  3, 333333333,  6, 333333333},

            {3, 333333333, -4, 666666667,  0,         0},
            {3, 333333333, -3,         0,  0, 333333333},
            {3, 333333333, -2,         0,  1, 333333333},
            {3, 333333333, -1,         0,  2, 333333333},
            {3, 333333333, -1, 333333334,  2, 666666667},
            {3, 333333333, -1, 666666667,  3,         0},
            {3, 333333333, -1, 999999999,  3, 333333332},
            {3, 333333333,  0,         0,  3, 333333333},
            {3, 333333333,  0,         1,  3, 333333334},
            {3, 333333333,  0, 333333333,  3, 666666666},
            {3, 333333333,  0, 666666666,  3, 999999999},
            {3, 333333333,  1,         0,  4, 333333333},
            {3, 333333333,  2,         0,  5, 333333333},
            {3, 333333333,  3,         0,  6, 333333333},
            {3, 333333333,  3, 333333333,  6, 666666666},

            {Long.MAX_VALUE - 1, 0, 1, 0, Long.MAX_VALUE, 0},
            {Long.MAX_VALUE - 1, 0, 0, 500, Long.MAX_VALUE - 1, 500},
            {Long.MAX_VALUE - 1, 0, 0, 1000000000, Long.MAX_VALUE, 0},

            {Long.MAX_VALUE, 0, -1, 0, Long.MAX_VALUE - 1, 0},
            {Long.MAX_VALUE, 0, 0, -500, Long.MAX_VALUE - 1, 999999500},
            {Long.MAX_VALUE, 0, 0, -1000000000, Long.MAX_VALUE - 1, 0},

            {Long.MAX_VALUE, 0, Long.MIN_VALUE, 0, -1, 0},
       };
    }

    @Test(dataProvider="Plus", groups={"tck"})
    public void plus(long seconds, int nanos, long otherSeconds, int otherNanos, long expectedSeconds, int expectedNanoOfSecond) {
       Instant i = Instant.ofEpochSecond(seconds, nanos).plus(Duration.ofSeconds(otherSeconds, otherNanos));
       assertEquals(i.getEpochSecond(), expectedSeconds);
       assertEquals(i.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions=ArithmeticException.class, groups={"tck"})
    public void plusOverflowTooBig() {
       Instant i = Instant.ofEpochSecond(Long.MAX_VALUE, 999999999);
       i.plus(Duration.ofSeconds(0, 1));
    }

    @Test(expectedExceptions=ArithmeticException.class, groups={"tck"})
    public void plusOverflowTooSmall() {
       Instant i = Instant.ofEpochSecond(Long.MIN_VALUE);
       i.plus(Duration.ofSeconds(-1, 999999999));
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="PlusSeconds")
    Object[][] provider_plusSeconds_long() {
        return new Object[][] {
            {0, 0, 0, 0, 0},
            {0, 0, 1, 1, 0},
            {0, 0, -1, -1, 0},
            {0, 0, Long.MAX_VALUE, Long.MAX_VALUE, 0},
            {0, 0, Long.MIN_VALUE, Long.MIN_VALUE, 0},
            {1, 0, 0, 1, 0},
            {1, 0, 1, 2, 0},
            {1, 0, -1, 0, 0},
            {1, 0, Long.MAX_VALUE - 1, Long.MAX_VALUE, 0},
            {1, 0, Long.MIN_VALUE, Long.MIN_VALUE + 1, 0},
            {1, 1, 0, 1, 1},
            {1, 1, 1, 2, 1},
            {1, 1, -1, 0, 1},
            {1, 1, Long.MAX_VALUE - 1, Long.MAX_VALUE, 1},
            {1, 1, Long.MIN_VALUE, Long.MIN_VALUE + 1, 1},
            {-1, 1, 0, -1, 1},
            {-1, 1, 1, 0, 1},
            {-1, 1, -1, -2, 1},
            {-1, 1, Long.MAX_VALUE, Long.MAX_VALUE - 1, 1},
            {-1, 1, Long.MIN_VALUE + 1, Long.MIN_VALUE, 1},

            {Long.MAX_VALUE, 2, Long.MIN_VALUE, -1, 2},
            {Long.MIN_VALUE, 2, Long.MAX_VALUE, -1, 2},
        };
    }

    @Test(dataProvider="PlusSeconds", groups={"tck"})
    public void plusSeconds_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochSecond(seconds, nanos);
        t = t.plusSeconds(amount);
        assertEquals(t.getEpochSecond(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void plusSeconds_long_overflowTooBig() {
        Instant t = Instant.ofEpochSecond(1, 0);
        t.plusSeconds(Long.MAX_VALUE);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void plusSeconds_long_overflowTooSmall() {
        Instant t = Instant.ofEpochSecond(-1, 0);
        t.plusSeconds(Long.MIN_VALUE);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="PlusMillis")
    Object[][] provider_plusMillis_long() {
        return new Object[][] {
            {0, 0, 0,       0, 0},
            {0, 0, 1,       0, 1000000},
            {0, 0, 999,     0, 999000000},
            {0, 0, 1000,    1, 0},
            {0, 0, 1001,    1, 1000000},
            {0, 0, 1999,    1, 999000000},
            {0, 0, 2000,    2, 0},
            {0, 0, -1,      -1, 999000000},
            {0, 0, -999,    -1, 1000000},
            {0, 0, -1000,   -1, 0},
            {0, 0, -1001,   -2, 999000000},
            {0, 0, -1999,   -2, 1000000},

            {0, 1, 0,       0, 1},
            {0, 1, 1,       0, 1000001},
            {0, 1, 998,     0, 998000001},
            {0, 1, 999,     0, 999000001},
            {0, 1, 1000,    1, 1},
            {0, 1, 1998,    1, 998000001},
            {0, 1, 1999,    1, 999000001},
            {0, 1, 2000,    2, 1},
            {0, 1, -1,      -1, 999000001},
            {0, 1, -2,      -1, 998000001},
            {0, 1, -1000,   -1, 1},
            {0, 1, -1001,   -2, 999000001},

            {0, 1000000, 0,       0, 1000000},
            {0, 1000000, 1,       0, 2000000},
            {0, 1000000, 998,     0, 999000000},
            {0, 1000000, 999,     1, 0},
            {0, 1000000, 1000,    1, 1000000},
            {0, 1000000, 1998,    1, 999000000},
            {0, 1000000, 1999,    2, 0},
            {0, 1000000, 2000,    2, 1000000},
            {0, 1000000, -1,      0, 0},
            {0, 1000000, -2,      -1, 999000000},
            {0, 1000000, -999,    -1, 2000000},
            {0, 1000000, -1000,   -1, 1000000},
            {0, 1000000, -1001,   -1, 0},
            {0, 1000000, -1002,   -2, 999000000},

            {0, 999999999, 0,     0, 999999999},
            {0, 999999999, 1,     1, 999999},
            {0, 999999999, 999,   1, 998999999},
            {0, 999999999, 1000,  1, 999999999},
            {0, 999999999, 1001,  2, 999999},
            {0, 999999999, -1,    0, 998999999},
            {0, 999999999, -1000, -1, 999999999},
            {0, 999999999, -1001, -1, 998999999},

            {0, 0, Long.MAX_VALUE, Long.MAX_VALUE / 1000, (int) (Long.MAX_VALUE % 1000) * 1000000},
            {0, 0, Long.MIN_VALUE, Long.MIN_VALUE / 1000 - 1, (int) (Long.MIN_VALUE % 1000) * 1000000 + 1000000000},
        };
    }

    @Test(dataProvider="PlusMillis", groups={"tck"})
    public void plusMillis_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochSecond(seconds, nanos);
        t = t.plusMillis(amount);
        assertEquals(t.getEpochSecond(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }
    @Test(dataProvider="PlusMillis", groups={"tck"})
    public void plusMillis_long_oneMore(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochSecond(seconds + 1, nanos);
        t = t.plusMillis(amount);
        assertEquals(t.getEpochSecond(), expectedSeconds + 1);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }
    @Test(dataProvider="PlusMillis", groups={"tck"})
    public void plusMillis_long_minusOneLess(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochSecond(seconds - 1, nanos);
        t = t.plusMillis(amount);
        assertEquals(t.getEpochSecond(), expectedSeconds - 1);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(groups={"tck"})
    public void plusMillis_long_max() {
        Instant t = Instant.ofEpochSecond(Long.MAX_VALUE, 998999999);
        t = t.plusMillis(1);
        assertEquals(t.getEpochSecond(), Long.MAX_VALUE);
        assertEquals(t.getNano(), 999999999);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void plusMillis_long_overflowTooBig() {
        Instant t = Instant.ofEpochSecond(Long.MAX_VALUE, 999000000);
        t.plusMillis(1);
    }

    @Test(groups={"tck"})
    public void plusMillis_long_min() {
        Instant t = Instant.ofEpochSecond(Long.MIN_VALUE, 1000000);
        t = t.plusMillis(-1);
        assertEquals(t.getEpochSecond(), Long.MIN_VALUE);
        assertEquals(t.getNano(), 0);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void plusMillis_long_overflowTooSmall() {
        Instant t = Instant.ofEpochSecond(Long.MIN_VALUE, 0);
        t.plusMillis(-1);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="PlusNanos")
    Object[][] provider_plusNanos_long() {
        return new Object[][] {
            {0, 0, 0,           0, 0},
            {0, 0, 1,           0, 1},
            {0, 0, 999999999,   0, 999999999},
            {0, 0, 1000000000,  1, 0},
            {0, 0, 1000000001,  1, 1},
            {0, 0, 1999999999,  1, 999999999},
            {0, 0, 2000000000,  2, 0},
            {0, 0, -1,          -1, 999999999},
            {0, 0, -999999999,  -1, 1},
            {0, 0, -1000000000, -1, 0},
            {0, 0, -1000000001, -2, 999999999},
            {0, 0, -1999999999, -2, 1},

            {1, 0, 0,           1, 0},
            {1, 0, 1,           1, 1},
            {1, 0, 999999999,   1, 999999999},
            {1, 0, 1000000000,  2, 0},
            {1, 0, 1000000001,  2, 1},
            {1, 0, 1999999999,  2, 999999999},
            {1, 0, 2000000000,  3, 0},
            {1, 0, -1,          0, 999999999},
            {1, 0, -999999999,  0, 1},
            {1, 0, -1000000000, 0, 0},
            {1, 0, -1000000001, -1, 999999999},
            {1, 0, -1999999999, -1, 1},

            {-1, 0, 0,           -1, 0},
            {-1, 0, 1,           -1, 1},
            {-1, 0, 999999999,   -1, 999999999},
            {-1, 0, 1000000000,  0, 0},
            {-1, 0, 1000000001,  0, 1},
            {-1, 0, 1999999999,  0, 999999999},
            {-1, 0, 2000000000,  1, 0},
            {-1, 0, -1,          -2, 999999999},
            {-1, 0, -999999999,  -2, 1},
            {-1, 0, -1000000000, -2, 0},
            {-1, 0, -1000000001, -3, 999999999},
            {-1, 0, -1999999999, -3, 1},

            {1, 1, 0,           1, 1},
            {1, 1, 1,           1, 2},
            {1, 1, 999999998,   1, 999999999},
            {1, 1, 999999999,   2, 0},
            {1, 1, 1000000000,  2, 1},
            {1, 1, 1999999998,  2, 999999999},
            {1, 1, 1999999999,  3, 0},
            {1, 1, 2000000000,  3, 1},
            {1, 1, -1,          1, 0},
            {1, 1, -2,          0, 999999999},
            {1, 1, -1000000000, 0, 1},
            {1, 1, -1000000001, 0, 0},
            {1, 1, -1000000002, -1, 999999999},
            {1, 1, -2000000000, -1, 1},

            {1, 999999999, 0,           1, 999999999},
            {1, 999999999, 1,           2, 0},
            {1, 999999999, 999999999,   2, 999999998},
            {1, 999999999, 1000000000,  2, 999999999},
            {1, 999999999, 1000000001,  3, 0},
            {1, 999999999, -1,          1, 999999998},
            {1, 999999999, -1000000000, 0, 999999999},
            {1, 999999999, -1000000001, 0, 999999998},
            {1, 999999999, -1999999999, 0, 0},
            {1, 999999999, -2000000000, -1, 999999999},

            {Long.MAX_VALUE, 0, 999999999, Long.MAX_VALUE, 999999999},
            {Long.MAX_VALUE - 1, 0, 1999999999, Long.MAX_VALUE, 999999999},
            {Long.MIN_VALUE, 1, -1, Long.MIN_VALUE, 0},
            {Long.MIN_VALUE + 1, 1, -1000000001, Long.MIN_VALUE, 0},

            {0, 0, Long.MAX_VALUE, Long.MAX_VALUE / 1000000000, (int) (Long.MAX_VALUE % 1000000000)},
            {0, 0, Long.MIN_VALUE, Long.MIN_VALUE / 1000000000 - 1, (int) (Long.MIN_VALUE % 1000000000) + 1000000000},
        };
    }

    @Test(dataProvider="PlusNanos", groups={"tck"})
    public void plusNanos_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant t = Instant.ofEpochSecond(seconds, nanos);
        t = t.plusNanos(amount);
        assertEquals(t.getEpochSecond(), expectedSeconds);
        assertEquals(t.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void plusNanos_long_overflowTooBig() {
        Instant t = Instant.ofEpochSecond(Long.MAX_VALUE, 999999999);
        t.plusNanos(1);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void plusNanos_long_overflowTooSmall() {
        Instant t = Instant.ofEpochSecond(Long.MIN_VALUE, 0);
        t.plusNanos(-1);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="Minus")
    Object[][] provider_minus() {
        return new Object[][] {
            {Long.MIN_VALUE, 0, Long.MIN_VALUE + 1, 0, -1, 0},

            {Long.MIN_VALUE, 0, -1, 0, Long.MIN_VALUE + 1, 0},
            {Long.MIN_VALUE, 0, 0, -500, Long.MIN_VALUE, 500},
            {Long.MIN_VALUE, 0, 0, -1000000000, Long.MIN_VALUE + 1, 0},

            {Long.MIN_VALUE + 1, 0, 1, 0, Long.MIN_VALUE, 0},
            {Long.MIN_VALUE + 1, 0, 0, 500, Long.MIN_VALUE, 999999500},
            {Long.MIN_VALUE + 1, 0, 0, 1000000000, Long.MIN_VALUE, 0},

            {-4, 666666667, -4, 666666667,  0,         0},
            {-4, 666666667, -3,         0, -1, 666666667},
            {-4, 666666667, -2,         0, -2, 666666667},
            {-4, 666666667, -1,         0, -3, 666666667},
            {-4, 666666667, -1, 333333334, -3, 333333333},
            {-4, 666666667, -1, 666666667, -3,         0},
            {-4, 666666667, -1, 999999999, -4, 666666668},
            {-4, 666666667,  0,         0, -4, 666666667},
            {-4, 666666667,  0,         1, -4, 666666666},
            {-4, 666666667,  0, 333333333, -4, 333333334},
            {-4, 666666667,  0, 666666666, -4,         1},
            {-4, 666666667,  1,         0, -5, 666666667},
            {-4, 666666667,  2,         0, -6, 666666667},
            {-4, 666666667,  3,         0, -7, 666666667},
            {-4, 666666667,  3, 333333333, -7, 333333334},

            {-3, 0, -4, 666666667,  0, 333333333},
            {-3, 0, -3,         0,  0,         0},
            {-3, 0, -2,         0, -1,         0},
            {-3, 0, -1,         0, -2,         0},
            {-3, 0, -1, 333333334, -3, 666666666},
            {-3, 0, -1, 666666667, -3, 333333333},
            {-3, 0, -1, 999999999, -3,         1},
            {-3, 0,  0,         0, -3,         0},
            {-3, 0,  0,         1, -4, 999999999},
            {-3, 0,  0, 333333333, -4, 666666667},
            {-3, 0,  0, 666666666, -4, 333333334},
            {-3, 0,  1,         0, -4,         0},
            {-3, 0,  2,         0, -5,         0},
            {-3, 0,  3,         0, -6,         0},
            {-3, 0,  3, 333333333, -7, 666666667},

            {-2, 0, -4, 666666667,  1, 333333333},
            {-2, 0, -3,         0,  1,         0},
            {-2, 0, -2,         0,  0,         0},
            {-2, 0, -1,         0, -1,         0},
            {-2, 0, -1, 333333334, -2, 666666666},
            {-2, 0, -1, 666666667, -2, 333333333},
            {-2, 0, -1, 999999999, -2,         1},
            {-2, 0,  0,         0, -2,         0},
            {-2, 0,  0,         1, -3, 999999999},
            {-2, 0,  0, 333333333, -3, 666666667},
            {-2, 0,  0, 666666666, -3, 333333334},
            {-2, 0,  1,         0, -3,         0},
            {-2, 0,  2,         0, -4,         0},
            {-2, 0,  3,         0, -5,         0},
            {-2, 0,  3, 333333333, -6, 666666667},

            {-1, 0, -4, 666666667,  2, 333333333},
            {-1, 0, -3,         0,  2,         0},
            {-1, 0, -2,         0,  1,         0},
            {-1, 0, -1,         0,  0,         0},
            {-1, 0, -1, 333333334, -1, 666666666},
            {-1, 0, -1, 666666667, -1, 333333333},
            {-1, 0, -1, 999999999, -1,         1},
            {-1, 0,  0,         0, -1,         0},
            {-1, 0,  0,         1, -2, 999999999},
            {-1, 0,  0, 333333333, -2, 666666667},
            {-1, 0,  0, 666666666, -2, 333333334},
            {-1, 0,  1,         0, -2,         0},
            {-1, 0,  2,         0, -3,         0},
            {-1, 0,  3,         0, -4,         0},
            {-1, 0,  3, 333333333, -5, 666666667},

            {-1, 666666667, -4, 666666667,  3,         0},
            {-1, 666666667, -3,         0,  2, 666666667},
            {-1, 666666667, -2,         0,  1, 666666667},
            {-1, 666666667, -1,         0,  0, 666666667},
            {-1, 666666667, -1, 333333334,  0, 333333333},
            {-1, 666666667, -1, 666666667,  0,         0},
            {-1, 666666667, -1, 999999999, -1, 666666668},
            {-1, 666666667,  0,         0, -1, 666666667},
            {-1, 666666667,  0,         1, -1, 666666666},
            {-1, 666666667,  0, 333333333, -1, 333333334},
            {-1, 666666667,  0, 666666666, -1,         1},
            {-1, 666666667,  1,         0, -2, 666666667},
            {-1, 666666667,  2,         0, -3, 666666667},
            {-1, 666666667,  3,         0, -4, 666666667},
            {-1, 666666667,  3, 333333333, -4, 333333334},

            {0, 0, -4, 666666667,  3, 333333333},
            {0, 0, -3,         0,  3,         0},
            {0, 0, -2,         0,  2,         0},
            {0, 0, -1,         0,  1,         0},
            {0, 0, -1, 333333334,  0, 666666666},
            {0, 0, -1, 666666667,  0, 333333333},
            {0, 0, -1, 999999999,  0,         1},
            {0, 0,  0,         0,  0,         0},
            {0, 0,  0,         1, -1, 999999999},
            {0, 0,  0, 333333333, -1, 666666667},
            {0, 0,  0, 666666666, -1, 333333334},
            {0, 0,  1,         0, -1,         0},
            {0, 0,  2,         0, -2,         0},
            {0, 0,  3,         0, -3,         0},
            {0, 0,  3, 333333333, -4, 666666667},

            {0, 333333333, -4, 666666667,  3, 666666666},
            {0, 333333333, -3,         0,  3, 333333333},
            {0, 333333333, -2,         0,  2, 333333333},
            {0, 333333333, -1,         0,  1, 333333333},
            {0, 333333333, -1, 333333334,  0, 999999999},
            {0, 333333333, -1, 666666667,  0, 666666666},
            {0, 333333333, -1, 999999999,  0, 333333334},
            {0, 333333333,  0,         0,  0, 333333333},
            {0, 333333333,  0,         1,  0, 333333332},
            {0, 333333333,  0, 333333333,  0,         0},
            {0, 333333333,  0, 666666666, -1, 666666667},
            {0, 333333333,  1,         0, -1, 333333333},
            {0, 333333333,  2,         0, -2, 333333333},
            {0, 333333333,  3,         0, -3, 333333333},
            {0, 333333333,  3, 333333333, -3,         0},

            {1, 0, -4, 666666667,  4, 333333333},
            {1, 0, -3,         0,  4,         0},
            {1, 0, -2,         0,  3,         0},
            {1, 0, -1,         0,  2,         0},
            {1, 0, -1, 333333334,  1, 666666666},
            {1, 0, -1, 666666667,  1, 333333333},
            {1, 0, -1, 999999999,  1,         1},
            {1, 0,  0,         0,  1,         0},
            {1, 0,  0,         1,  0, 999999999},
            {1, 0,  0, 333333333,  0, 666666667},
            {1, 0,  0, 666666666,  0, 333333334},
            {1, 0,  1,         0,  0,         0},
            {1, 0,  2,         0, -1,         0},
            {1, 0,  3,         0, -2,         0},
            {1, 0,  3, 333333333, -3, 666666667},

            {2, 0, -4, 666666667,  5, 333333333},
            {2, 0, -3,         0,  5,         0},
            {2, 0, -2,         0,  4,         0},
            {2, 0, -1,         0,  3,         0},
            {2, 0, -1, 333333334,  2, 666666666},
            {2, 0, -1, 666666667,  2, 333333333},
            {2, 0, -1, 999999999,  2,         1},
            {2, 0,  0,         0,  2,         0},
            {2, 0,  0,         1,  1, 999999999},
            {2, 0,  0, 333333333,  1, 666666667},
            {2, 0,  0, 666666666,  1, 333333334},
            {2, 0,  1,         0,  1,         0},
            {2, 0,  2,         0,  0,         0},
            {2, 0,  3,         0, -1,         0},
            {2, 0,  3, 333333333, -2, 666666667},

            {3, 0, -4, 666666667,  6, 333333333},
            {3, 0, -3,         0,  6,         0},
            {3, 0, -2,         0,  5,         0},
            {3, 0, -1,         0,  4,         0},
            {3, 0, -1, 333333334,  3, 666666666},
            {3, 0, -1, 666666667,  3, 333333333},
            {3, 0, -1, 999999999,  3,         1},
            {3, 0,  0,         0,  3,         0},
            {3, 0,  0,         1,  2, 999999999},
            {3, 0,  0, 333333333,  2, 666666667},
            {3, 0,  0, 666666666,  2, 333333334},
            {3, 0,  1,         0,  2,         0},
            {3, 0,  2,         0,  1,         0},
            {3, 0,  3,         0,  0,         0},
            {3, 0,  3, 333333333, -1, 666666667},

            {3, 333333333, -4, 666666667,  6, 666666666},
            {3, 333333333, -3,         0,  6, 333333333},
            {3, 333333333, -2,         0,  5, 333333333},
            {3, 333333333, -1,         0,  4, 333333333},
            {3, 333333333, -1, 333333334,  3, 999999999},
            {3, 333333333, -1, 666666667,  3, 666666666},
            {3, 333333333, -1, 999999999,  3, 333333334},
            {3, 333333333,  0,         0,  3, 333333333},
            {3, 333333333,  0,         1,  3, 333333332},
            {3, 333333333,  0, 333333333,  3,         0},
            {3, 333333333,  0, 666666666,  2, 666666667},
            {3, 333333333,  1,         0,  2, 333333333},
            {3, 333333333,  2,         0,  1, 333333333},
            {3, 333333333,  3,         0,  0, 333333333},
            {3, 333333333,  3, 333333333,  0,         0},

            {Long.MAX_VALUE - 1, 0, -1, 0, Long.MAX_VALUE, 0},
            {Long.MAX_VALUE - 1, 0, 0, -500, Long.MAX_VALUE - 1, 500},
            {Long.MAX_VALUE - 1, 0, 0, -1000000000, Long.MAX_VALUE, 0},

            {Long.MAX_VALUE, 0, 1, 0, Long.MAX_VALUE - 1, 0},
            {Long.MAX_VALUE, 0, 0, 500, Long.MAX_VALUE - 1, 999999500},
            {Long.MAX_VALUE, 0, 0, 1000000000, Long.MAX_VALUE - 1, 0},

            {Long.MAX_VALUE, 0, Long.MAX_VALUE, 0, 0, 0},
       };
    }

    @Test(dataProvider="Minus", groups={"tck"})
    public void minus_Duration(long seconds, int nanos, long otherSeconds, int otherNanos, long expectedSeconds, int expectedNanoOfSecond) {
       Instant i = Instant.ofEpochSecond(seconds, nanos).minus(Duration.ofSeconds(otherSeconds, otherNanos));
       assertEquals(i.getEpochSecond(), expectedSeconds);
       assertEquals(i.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions=ArithmeticException.class, groups={"tck"})
    public void minus_Duration_overflowTooSmall() {
       Instant i = Instant.ofEpochSecond(Long.MIN_VALUE);
       i.minus(Duration.ofSeconds(0, 1));
    }

    @Test(expectedExceptions=ArithmeticException.class, groups={"tck"})
    public void minus_Duration_overflowTooBig() {
       Instant i = Instant.ofEpochSecond(Long.MAX_VALUE, 999999999);
       i.minus(Duration.ofSeconds(-1, 999999999));
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="MinusSeconds")
    Object[][] provider_minusSeconds_long() {
        return new Object[][] {
            {0, 0, 0, 0, 0},
            {0, 0, 1, -1, 0},
            {0, 0, -1, 1, 0},
            {0, 0, Long.MAX_VALUE, -Long.MAX_VALUE, 0},
            {0, 0, Long.MIN_VALUE + 1, Long.MAX_VALUE, 0},
            {1, 0, 0, 1, 0},
            {1, 0, 1, 0, 0},
            {1, 0, -1, 2, 0},
            {1, 0, Long.MAX_VALUE - 1, -Long.MAX_VALUE + 2, 0},
            {1, 0, Long.MIN_VALUE + 2, Long.MAX_VALUE, 0},
            {1, 1, 0, 1, 1},
            {1, 1, 1, 0, 1},
            {1, 1, -1, 2, 1},
            {1, 1, Long.MAX_VALUE, -Long.MAX_VALUE + 1, 1},
            {1, 1, Long.MIN_VALUE + 2, Long.MAX_VALUE, 1},
            {-1, 1, 0, -1, 1},
            {-1, 1, 1, -2, 1},
            {-1, 1, -1, 0, 1},
            {-1, 1, Long.MAX_VALUE, Long.MIN_VALUE, 1},
            {-1, 1, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1, 1},
            {-1, 1, Long.MIN_VALUE, Long.MAX_VALUE, 1},

            {Long.MAX_VALUE, 2, Long.MAX_VALUE, 0, 2},
            {Long.MAX_VALUE - 1, 2, Long.MAX_VALUE, -1, 2},
            {Long.MIN_VALUE, 2, Long.MIN_VALUE, 0, 2},
            {Long.MIN_VALUE + 1, 2, Long.MIN_VALUE, 1, 2},
        };
    }

    @Test(dataProvider="MinusSeconds", groups={"tck"})
    public void minusSeconds_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds, nanos);
        i = i.minusSeconds(amount);
        assertEquals(i.getEpochSecond(), expectedSeconds);
        assertEquals(i.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void minusSeconds_long_overflowTooBig() {
        Instant i = Instant.ofEpochSecond(1, 0);
        i.minusSeconds(Long.MIN_VALUE + 1);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void minusSeconds_long_overflowTooSmall() {
        Instant i = Instant.ofEpochSecond(-2, 0);
        i.minusSeconds(Long.MAX_VALUE);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="MinusMillis")
    Object[][] provider_minusMillis_long() {
        return new Object[][] {
            {0, 0, 0,       0, 0},
            {0, 0, 1,      -1, 999000000},
            {0, 0, 999,    -1, 1000000},
            {0, 0, 1000,   -1, 0},
            {0, 0, 1001,   -2, 999000000},
            {0, 0, 1999,   -2, 1000000},
            {0, 0, 2000,   -2, 0},
            {0, 0, -1,      0, 1000000},
            {0, 0, -999,    0, 999000000},
            {0, 0, -1000,   1, 0},
            {0, 0, -1001,   1, 1000000},
            {0, 0, -1999,   1, 999000000},

            {0, 1, 0,       0, 1},
            {0, 1, 1,      -1, 999000001},
            {0, 1, 998,    -1, 2000001},
            {0, 1, 999,    -1, 1000001},
            {0, 1, 1000,   -1, 1},
            {0, 1, 1998,   -2, 2000001},
            {0, 1, 1999,   -2, 1000001},
            {0, 1, 2000,   -2, 1},
            {0, 1, -1,      0, 1000001},
            {0, 1, -2,      0, 2000001},
            {0, 1, -1000,   1, 1},
            {0, 1, -1001,   1, 1000001},

            {0, 1000000, 0,       0, 1000000},
            {0, 1000000, 1,       0, 0},
            {0, 1000000, 998,    -1, 3000000},
            {0, 1000000, 999,    -1, 2000000},
            {0, 1000000, 1000,   -1, 1000000},
            {0, 1000000, 1998,   -2, 3000000},
            {0, 1000000, 1999,   -2, 2000000},
            {0, 1000000, 2000,   -2, 1000000},
            {0, 1000000, -1,      0, 2000000},
            {0, 1000000, -2,      0, 3000000},
            {0, 1000000, -999,    1, 0},
            {0, 1000000, -1000,   1, 1000000},
            {0, 1000000, -1001,   1, 2000000},
            {0, 1000000, -1002,   1, 3000000},

            {0, 999999999, 0,     0, 999999999},
            {0, 999999999, 1,     0, 998999999},
            {0, 999999999, 999,   0, 999999},
            {0, 999999999, 1000, -1, 999999999},
            {0, 999999999, 1001, -1, 998999999},
            {0, 999999999, -1,    1, 999999},
            {0, 999999999, -1000, 1, 999999999},
            {0, 999999999, -1001, 2, 999999},

            {0, 0, Long.MAX_VALUE, -(Long.MAX_VALUE / 1000) - 1, (int) -(Long.MAX_VALUE % 1000) * 1000000 + 1000000000},
            {0, 0, Long.MIN_VALUE, -(Long.MIN_VALUE / 1000), (int) -(Long.MIN_VALUE % 1000) * 1000000},
        };
    }

    @Test(dataProvider="MinusMillis", groups={"tck"})
    public void minusMillis_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds, nanos);
        i = i.minusMillis(amount);
        assertEquals(i.getEpochSecond(), expectedSeconds);
        assertEquals(i.getNano(), expectedNanoOfSecond);
    }

    @Test(dataProvider="MinusMillis", groups={"tck"})
    public void minusMillis_long_oneMore(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds + 1, nanos);
        i = i.minusMillis(amount);
        assertEquals(i.getEpochSecond(), expectedSeconds + 1);
        assertEquals(i.getNano(), expectedNanoOfSecond);
    }

    @Test(dataProvider="MinusMillis", groups={"tck"})
    public void minusMillis_long_minusOneLess(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds - 1, nanos);
        i = i.minusMillis(amount);
        assertEquals(i.getEpochSecond(), expectedSeconds - 1);
        assertEquals(i.getNano(), expectedNanoOfSecond);
    }

    @Test(groups={"tck"})
    public void minusMillis_long_max() {
        Instant i = Instant.ofEpochSecond(Long.MAX_VALUE, 998999999);
        i = i.minusMillis(-1);
        assertEquals(i.getEpochSecond(), Long.MAX_VALUE);
        assertEquals(i.getNano(), 999999999);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void minusMillis_long_overflowTooBig() {
        Instant i = Instant.ofEpochSecond(Long.MAX_VALUE, 999000000);
        i.minusMillis(-1);
    }

    @Test(groups={"tck"})
    public void minusMillis_long_min() {
        Instant i = Instant.ofEpochSecond(Long.MIN_VALUE, 1000000);
        i = i.minusMillis(1);
        assertEquals(i.getEpochSecond(), Long.MIN_VALUE);
        assertEquals(i.getNano(), 0);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void minusMillis_long_overflowTooSmall() {
        Instant i = Instant.ofEpochSecond(Long.MIN_VALUE, 0);
        i.minusMillis(1);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="MinusNanos")
    Object[][] provider_minusNanos_long() {
        return new Object[][] {
            {0, 0, 0,           0, 0},
            {0, 0, 1,          -1, 999999999},
            {0, 0, 999999999,  -1, 1},
            {0, 0, 1000000000, -1, 0},
            {0, 0, 1000000001, -2, 999999999},
            {0, 0, 1999999999, -2, 1},
            {0, 0, 2000000000, -2, 0},
            {0, 0, -1,          0, 1},
            {0, 0, -999999999,  0, 999999999},
            {0, 0, -1000000000, 1, 0},
            {0, 0, -1000000001, 1, 1},
            {0, 0, -1999999999, 1, 999999999},

            {1, 0, 0,            1, 0},
            {1, 0, 1,            0, 999999999},
            {1, 0, 999999999,    0, 1},
            {1, 0, 1000000000,   0, 0},
            {1, 0, 1000000001,  -1, 999999999},
            {1, 0, 1999999999,  -1, 1},
            {1, 0, 2000000000,  -1, 0},
            {1, 0, -1,           1, 1},
            {1, 0, -999999999,   1, 999999999},
            {1, 0, -1000000000,  2, 0},
            {1, 0, -1000000001,  2, 1},
            {1, 0, -1999999999,  2, 999999999},

            {-1, 0, 0,           -1, 0},
            {-1, 0, 1,           -2, 999999999},
            {-1, 0, 999999999,   -2, 1},
            {-1, 0, 1000000000,  -2, 0},
            {-1, 0, 1000000001,  -3, 999999999},
            {-1, 0, 1999999999,  -3, 1},
            {-1, 0, 2000000000,  -3, 0},
            {-1, 0, -1,          -1, 1},
            {-1, 0, -999999999,  -1, 999999999},
            {-1, 0, -1000000000,  0, 0},
            {-1, 0, -1000000001,  0, 1},
            {-1, 0, -1999999999,  0, 999999999},

            {1, 1, 0,           1, 1},
            {1, 1, 1,           1, 0},
            {1, 1, 999999998,   0, 3},
            {1, 1, 999999999,   0, 2},
            {1, 1, 1000000000,  0, 1},
            {1, 1, 1999999998, -1, 3},
            {1, 1, 1999999999, -1, 2},
            {1, 1, 2000000000, -1, 1},
            {1, 1, -1,          1, 2},
            {1, 1, -2,          1, 3},
            {1, 1, -1000000000, 2, 1},
            {1, 1, -1000000001, 2, 2},
            {1, 1, -1000000002, 2, 3},
            {1, 1, -2000000000, 3, 1},

            {1, 999999999, 0,           1, 999999999},
            {1, 999999999, 1,           1, 999999998},
            {1, 999999999, 999999999,   1, 0},
            {1, 999999999, 1000000000,  0, 999999999},
            {1, 999999999, 1000000001,  0, 999999998},
            {1, 999999999, -1,          2, 0},
            {1, 999999999, -1000000000, 2, 999999999},
            {1, 999999999, -1000000001, 3, 0},
            {1, 999999999, -1999999999, 3, 999999998},
            {1, 999999999, -2000000000, 3, 999999999},

            {Long.MAX_VALUE, 0, -999999999, Long.MAX_VALUE, 999999999},
            {Long.MAX_VALUE - 1, 0, -1999999999, Long.MAX_VALUE, 999999999},
            {Long.MIN_VALUE, 1, 1, Long.MIN_VALUE, 0},
            {Long.MIN_VALUE + 1, 1, 1000000001, Long.MIN_VALUE, 0},

            {0, 0, Long.MAX_VALUE, -(Long.MAX_VALUE / 1000000000) - 1, (int) -(Long.MAX_VALUE % 1000000000) + 1000000000},
            {0, 0, Long.MIN_VALUE, -(Long.MIN_VALUE / 1000000000), (int) -(Long.MIN_VALUE % 1000000000)},
        };
    }

    @Test(dataProvider="MinusNanos", groups={"tck"})
    public void minusNanos_long(long seconds, int nanos, long amount, long expectedSeconds, int expectedNanoOfSecond) {
        Instant i = Instant.ofEpochSecond(seconds, nanos);
        i = i.minusNanos(amount);
        assertEquals(i.getEpochSecond(), expectedSeconds);
        assertEquals(i.getNano(), expectedNanoOfSecond);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void minusNanos_long_overflowTooBig() {
        Instant i = Instant.ofEpochSecond(Long.MAX_VALUE, 999999999);
        i.minusNanos(-1);
    }

    @Test(expectedExceptions = {ArithmeticException.class}, groups={"tck"})
    public void minusNanos_long_overflowTooSmall() {
        Instant i = Instant.ofEpochSecond(Long.MIN_VALUE, 0);
        i.minusNanos(1);
    }

    //-----------------------------------------------------------------------
    // toEpochMilli()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void test_toEpochMilli() {
        assertEquals(Instant.ofEpochSecond(1L, 1000000).toEpochMilli(), 1001L);
        assertEquals(Instant.ofEpochSecond(1L, 2000000).toEpochMilli(), 1002L);
        assertEquals(Instant.ofEpochSecond(1L, 567).toEpochMilli(), 1000L);
        assertEquals(Instant.ofEpochSecond(Long.MAX_VALUE / 1000).toEpochMilli(), (Long.MAX_VALUE / 1000) * 1000);
        assertEquals(Instant.ofEpochSecond(Long.MIN_VALUE / 1000).toEpochMilli(), (Long.MIN_VALUE / 1000) * 1000);
        assertEquals(Instant.ofEpochSecond(0L, -1000000).toEpochMilli(), -1L);
        assertEquals(Instant.ofEpochSecond(0L, 1000000).toEpochMilli(), 1);
        assertEquals(Instant.ofEpochSecond(0L, 999999).toEpochMilli(), 0);
        assertEquals(Instant.ofEpochSecond(0L, 1).toEpochMilli(), 0);
        assertEquals(Instant.ofEpochSecond(0L, 0).toEpochMilli(), 0);
        assertEquals(Instant.ofEpochSecond(0L, -1).toEpochMilli(), -1L);
        assertEquals(Instant.ofEpochSecond(0L, -999999).toEpochMilli(), -1L);
        assertEquals(Instant.ofEpochSecond(0L, -1000000).toEpochMilli(), -1L);
        assertEquals(Instant.ofEpochSecond(0L, -1000001).toEpochMilli(), -2L);
    }

    @Test(expectedExceptions=ArithmeticException.class, groups={"tck"})
    public void test_toEpochMilli_tooBig() {
        Instant.ofEpochSecond(Long.MAX_VALUE / 1000 + 1).toEpochMilli();
    }

    @Test(expectedExceptions=ArithmeticException.class, groups={"tck"})
    public void test_toEpochMilli_tooSmall() {
        Instant.ofEpochSecond(Long.MIN_VALUE / 1000 - 1).toEpochMilli();
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void test_comparisons() {
        doTest_comparisons_Instant(
            Instant.ofEpochSecond(-2L, 0),
            Instant.ofEpochSecond(-2L, 999999998),
            Instant.ofEpochSecond(-2L, 999999999),
            Instant.ofEpochSecond(-1L, 0),
            Instant.ofEpochSecond(-1L, 1),
            Instant.ofEpochSecond(-1L, 999999998),
            Instant.ofEpochSecond(-1L, 999999999),
            Instant.ofEpochSecond(0L, 0),
            Instant.ofEpochSecond(0L, 1),
            Instant.ofEpochSecond(0L, 2),
            Instant.ofEpochSecond(0L, 999999999),
            Instant.ofEpochSecond(1L, 0),
            Instant.ofEpochSecond(2L, 0)
        );
    }

    void doTest_comparisons_Instant(Instant... instants) {
        for (int i = 0; i < instants.length; i++) {
            Instant a = instants[i];
            for (int j = 0; j < instants.length; j++) {
                Instant b = instants[j];
                if (i < j) {
                    assertEquals(a.compareTo(b) < 0, true, a + " <=> " + b);
                    assertEquals(a.isBefore(b), true, a + " <=> " + b);
                    assertEquals(a.isAfter(b), false, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else if (i > j) {
                    assertEquals(a.compareTo(b) > 0, true, a + " <=> " + b);
                    assertEquals(a.isBefore(b), false, a + " <=> " + b);
                    assertEquals(a.isAfter(b), true, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else {
                    assertEquals(a.compareTo(b), 0, a + " <=> " + b);
                    assertEquals(a.isBefore(b), false, a + " <=> " + b);
                    assertEquals(a.isAfter(b), false, a + " <=> " + b);
                    assertEquals(a.equals(b), true, a + " <=> " + b);
                }
            }
        }
    }

    @Test(expectedExceptions=NullPointerException.class, groups={"tck"})
    public void test_compareTo_ObjectNull() {
        Instant a = Instant.ofEpochSecond(0L, 0);
        a.compareTo(null);
    }

    @Test(expectedExceptions=NullPointerException.class, groups={"tck"})
    public void test_isBefore_ObjectNull() {
        Instant a = Instant.ofEpochSecond(0L, 0);
        a.isBefore(null);
    }

    @Test(expectedExceptions=NullPointerException.class, groups={"tck"})
    public void test_isAfter_ObjectNull() {
        Instant a = Instant.ofEpochSecond(0L, 0);
        a.isAfter(null);
    }

    @Test(expectedExceptions=ClassCastException.class, groups={"tck"})
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void compareToNonInstant() {
       Comparable c = Instant.ofEpochSecond(0L);
       c.compareTo(new Object());
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void test_equals() {
        Instant test5a = Instant.ofEpochSecond(5L, 20);
        Instant test5b = Instant.ofEpochSecond(5L, 20);
        Instant test5n = Instant.ofEpochSecond(5L, 30);
        Instant test6 = Instant.ofEpochSecond(6L, 20);

        assertEquals(test5a.equals(test5a), true);
        assertEquals(test5a.equals(test5b), true);
        assertEquals(test5a.equals(test5n), false);
        assertEquals(test5a.equals(test6), false);

        assertEquals(test5b.equals(test5a), true);
        assertEquals(test5b.equals(test5b), true);
        assertEquals(test5b.equals(test5n), false);
        assertEquals(test5b.equals(test6), false);

        assertEquals(test5n.equals(test5a), false);
        assertEquals(test5n.equals(test5b), false);
        assertEquals(test5n.equals(test5n), true);
        assertEquals(test5n.equals(test6), false);

        assertEquals(test6.equals(test5a), false);
        assertEquals(test6.equals(test5b), false);
        assertEquals(test6.equals(test5n), false);
        assertEquals(test6.equals(test6), true);
    }

    @Test(groups={"tck"})
    public void test_equals_null() {
        Instant test5 = Instant.ofEpochSecond(5L, 20);
        assertEquals(test5.equals(null), false);
    }

    @Test(groups={"tck"})
    public void test_equals_otherClass() {
        Instant test5 = Instant.ofEpochSecond(5L, 20);
        assertEquals(test5.equals(""), false);
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void test_hashCode() {
        Instant test5a = Instant.ofEpochSecond(5L, 20);
        Instant test5b = Instant.ofEpochSecond(5L, 20);
        Instant test5n = Instant.ofEpochSecond(5L, 30);
        Instant test6 = Instant.ofEpochSecond(6L, 20);

        assertEquals(test5a.hashCode() == test5a.hashCode(), true);
        assertEquals(test5a.hashCode() == test5b.hashCode(), true);
        assertEquals(test5b.hashCode() == test5b.hashCode(), true);

        assertEquals(test5a.hashCode() == test5n.hashCode(), false);
        assertEquals(test5a.hashCode() == test6.hashCode(), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void test_toString() {
        Instant t = Instant.ofEpochSecond(0L, 567);
        assertEquals(t.toString(), "1970-01-01T00:00:00.000000567Z");
    }

    //-----------------------------------------------------------------------
    @Test(groups={"tck"})
    public void test_serialization_format() throws ClassNotFoundException, IOException {
        assertEqualsSerialisedForm(Instant.ofEpochMilli(1347830279338l));
    }

    @Test(groups={"tck"})
    public void test_serialization() throws ClassNotFoundException, IOException {
        assertSerializable(Instant.ofEpochMilli(134l));
    }

}
