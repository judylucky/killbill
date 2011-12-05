/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.util.clock;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.ning.billing.catalog.api.Duration;

// STEPH should really be in tests but not accessible from other sub modules
public class ClockMock extends DefaultClock {

    private enum DeltaType {
        DELTA_NONE,
        DELTA_DURATION,
        DELTA_ABS
    }

    private long deltaFromRealityMs;
    private List<Duration> deltaFromRealityDuration;
    private long deltaFromRealitDurationEpsilon;
    private DeltaType deltaType;

    public ClockMock() {
        deltaType = DeltaType.DELTA_NONE;
        deltaFromRealityMs = 0;
        deltaFromRealitDurationEpsilon = 0;
        deltaFromRealityDuration = null;
    }

    @Override
    public synchronized DateTime getNow(DateTimeZone tz) {
        return adjust(super.getNow(tz));
    }

    @Override
    public synchronized DateTime getUTCNow() {
        return getNow(DateTimeZone.UTC);
    }

    public synchronized void setDeltaFromReality(Duration delta, long epsilon) {
        deltaType = DeltaType.DELTA_DURATION;
        deltaFromRealityDuration = new ArrayList<Duration>();
        deltaFromRealityDuration.add(delta);
        deltaFromRealitDurationEpsilon = epsilon;
        deltaFromRealityMs = 0;
    }

    public synchronized void addDeltaFromReality(Duration delta) {
        if (deltaType != DeltaType.DELTA_DURATION) {
            throw new RuntimeException("ClockMock should be set with type DELTA_DURATION");
        }
        deltaFromRealityDuration.add(delta);
    }

    public synchronized void setDeltaFromReality(long delta) {
        deltaType = DeltaType.DELTA_ABS;
        deltaFromRealityDuration = null;
        deltaFromRealitDurationEpsilon = 0;
        deltaFromRealityMs = delta;
    }

    public synchronized void resetDeltaFromReality() {
        deltaType = DeltaType.DELTA_NONE;
        deltaFromRealityDuration = null;
        deltaFromRealitDurationEpsilon = 0;
        deltaFromRealityMs = 0;
    }

    private DateTime adjust(DateTime realNow) {
        switch(deltaType) {
            case DELTA_NONE:
                return realNow;
            case DELTA_ABS:
                return adjustFromAbsolute(realNow);
            case DELTA_DURATION:
                return adjustFromDuration(realNow);
            default:
                return null;
        }
    }

    private DateTime adjustFromDuration(DateTime input) {

        DateTime result = input;
        for (Duration cur : deltaFromRealityDuration) {

            int length = cur.getNumber();
            switch (cur.getUnit()) {
            case DAYS:
                result = result.plusDays(cur.getNumber());
                break;

            case MONTHS:
                result = result.plusMonths(cur.getNumber());
                break;

            case YEARS:
                result = result.plusYears(cur.getNumber());
                break;

            case UNLIMITED:
            default:
                throw new RuntimeException("ClockMock is adjusting an unlimtited time period");
            }
        }
        if (deltaFromRealitDurationEpsilon != 0) {
            result = result.plus(deltaFromRealitDurationEpsilon);
        }
        return result;
    }

    private DateTime adjustFromAbsolute(DateTime input) {
        return input.plus(deltaFromRealityMs);
    }

}
