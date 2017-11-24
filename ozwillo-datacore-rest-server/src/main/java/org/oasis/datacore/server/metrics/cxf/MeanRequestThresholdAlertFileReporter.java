package org.oasis.datacore.server.metrics.cxf;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A reporter which allows for horizontal scalability, by triggering an alert if the currently
 * measured number of requests per minute is outside defined min & max range.
 * This min & max range is dynamically retrieved the configured OCCI server's configurations
 * if any, or else from configured properties. Default is 0, which disables it.
 * Said alert consists in creating a file on the file system, which can be detected by
 * deployment & monitoring solutions such as Roboconf.
 * 
 * @author mdutoo
 */
public class MeanRequestThresholdAlertFileReporter extends ScheduledReporter {

    private static final Logger logger = LoggerFactory.getLogger(MeanRequestThresholdAlertFileReporter.class);
   
    /**
     * Returns a new {@link Builder} for {@link MeanRequestThresholdAlertFileReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link MeanRequestThresholdAlertFileReporter}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link MeanRequestThresholdAlertFileReporter} instances.
     */
    public static class Builder {
        private final MetricRegistry registry;
        private Locale locale;
        private Clock clock;
        private TimeZone timeZone;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;

        /** OCCI conf in (MART) server where the threshold is retrieved from, ex.
         * http://localhost:8080/LDServicePerformanceSLOMixin/ or to test it .../compute/
         * (if empty, explicit max/min props are used) */
        private String occiUrl = null;
        /** ex. "/0/attributes/requestPerMinuteMax" or to test it "/0/attributes/occi.compute.cores" */
        private String maxOcciJsonPointer = "";
        /** max threshold (only if no occiMartServerUrl, disabled if 0) */
        private float max = 0;
        /** this file will be created by BrokenThresholdReporter if there is a max threshold and it is broken */
        private String maxAlertFilePath = "/tmp/vmfile";
        /** ex. "/0/attributes/requestPerMinuteMax" */
        private String minOcciJsonPointer = "";
        /** min threshold (only if no occiMartServerUrl, disabled if 0) */
        private float min = 0;
        /** this file will be created by BrokenThresholdReporter if there is a min threshold and it is broken */
        private String minAlertFilePath = "/tmp/vmfilemin";

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.locale = Locale.getDefault();
            this.clock = Clock.defaultClock();
            this.timeZone = TimeZone.getDefault();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        /**
         * Format numbers for the given {@link Locale}.
         *
         * @param locale a {@link Locale}
         * @return {@code this}
         */
        public Builder formattedFor(Locale locale) {
            this.locale = locale;
            return this;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Use the given {@link TimeZone} for the time.
         *
         * @param timeZone a {@link TimeZone}
         * @return {@code this}
         */
        public Builder formattedFor(TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder occiUrl(String occiUrl) {
           this.occiUrl = occiUrl;
           return this;
        }
        public Builder maxOcciJsonPointer(String maxOcciJsonPointer) {
           this.maxOcciJsonPointer = maxOcciJsonPointer;
           return this;
        }
        public Builder max(float max) {
           this.max = max;
           return this;
        }
        public Builder maxAlertFilePath(String maxAlertFilePath) {
           this.maxAlertFilePath = maxAlertFilePath;
           return this;
        }
        public Builder minOcciJsonPointer(String minOcciJsonPointer) {
           this.minOcciJsonPointer = minOcciJsonPointer;
           return this;
        }
        public Builder min(float min) {
           this.min = min;
           return this;
        }
        public Builder minAlertFilePath(String minAlertFilePath) {
           this.minAlertFilePath = minAlertFilePath;
           return this;
        }

        /**
         * Builds a {@link MeanRequestThresholdAlertFileReporter} with the given properties.
         *
         * @return a {@link MeanRequestThresholdAlertFileReporter}
         */
        public MeanRequestThresholdAlertFileReporter build() {
            return new MeanRequestThresholdAlertFileReporter(registry,
                                       locale,
                                       clock,
                                       timeZone,
                                       rateUnit,
                                       durationUnit,
                                       filter,
                                       occiUrl,
                                       maxOcciJsonPointer,
                                       max,
                                       maxAlertFilePath,
                                       minOcciJsonPointer,
                                       min,
                                       minAlertFilePath);
        }
    }

    private final DateFormat dateFormat;

    /** OCCI conf in (MART) server where the threshold is retrieved from, ex.
     * http://localhost:8080/LDServicePerformanceSLOMixin/ or to test it .../compute/
     * (if empty, explicit max/min props are used) */
    private String occiUrl = null;
    /** ex. "/0/attributes/requestPerMinuteMax" or to test it "/0/attributes/occi.compute.cores" */
    private String maxOcciJsonPointer = "";
    /** max threshold (disabled if 0) */
    private float max = 0;
    /** this file will be created by BrokenThresholdReporter if there is a max threshold and it is broken */
    private String maxAlertFilePath = "/tmp/vmfile";
    /** ex. "/0/attributes/requestPerMinuteMin" */
    private String minOcciJsonPointer = "";
    /** min threshold (disabled if 0) */
    private float min = 0;
    /** this file will be created by BrokenThresholdReporter if there is a min threshold and it is broken */
    private String minAlertFilePath = "/tmp/vmfilemin";
    private String timerName = "DatacoreApiImpl_Attribute=Totals";

    private MeanRequestThresholdAlertFileReporter(MetricRegistry registry,
                            Locale locale,
                            Clock clock,
                            TimeZone timeZone,
                            TimeUnit rateUnit,
                            TimeUnit durationUnit,
                            MetricFilter filter,
                            String occiUrl,
                            String maxOcciJsonPointer,
                            float max,
                            String maxAlertFilePath,
                            String minOcciJsonPointer,
                            float min,
                            String minAlertFilePath) {
        super(registry, "MeanRequestThresholdAlertFile-reporter", filter, rateUnit, durationUnit);
        this.dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                         DateFormat.MEDIUM,
                                                         locale);
        dateFormat.setTimeZone(timeZone);

        this.occiUrl = occiUrl;
        this.maxOcciJsonPointer = maxOcciJsonPointer;
        this.max = max;
        this.maxAlertFilePath = maxAlertFilePath;
        this.minOcciJsonPointer = minOcciJsonPointer;
        this.min = min;
        this.minAlertFilePath = minAlertFilePath;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
       // get value :
       Timer datacoreApiTimer = timers.get(timerName);
       if (datacoreApiTimer == null) {
          return; // no request yet, not not metrics have been initialized
       }
       double oneMinuteRate = convertRate(datacoreApiTimer.getOneMinuteRate());
       
       // get thresholds :
       float max = this.max;
       float min = this.min;
       try {
          // get SLO conf from OCCI server :
          JsonNode occiJsonNode = OcciUtils.getOcciJsonNode(this.occiUrl);
          if (occiJsonNode != null) {
             max = (float) occiJsonNode.at(this.maxOcciJsonPointer).asDouble(0);
             min = (float) occiJsonNode.at(this.minOcciJsonPointer).asDouble(0);
          } // else disabled
       } catch (Exception e) {
          logger.error("Error retrieving min/max thresholds from OCCI MARTServer "
                + this.occiUrl + " using JSON pointer expressions "
                + this.minOcciJsonPointer + " "
                + this.maxOcciJsonPointer);
       }
       
       // compare :
       File alertFile = null;
       if (oneMinuteRate > max) {
          alertFile = new File(this.maxAlertFilePath);
       } else if (oneMinuteRate < min) {
          alertFile = new File(this.minAlertFilePath);
       }

       // act :
       if (alertFile != null) {
          String msg = String.format("[min=%f, max=%f, current=%f]: ",
                min, max, oneMinuteRate);
          if (alertFile.exists()) {
             logger.error(msg + "alert file already exists, deleting " + alertFile.getAbsolutePath());
             alertFile.delete();
          } else {
             logger.warn(msg + "creating max alert file " + alertFile.getAbsolutePath());
             try {
                alertFile.createNewFile();
             } catch (IOException e) {
                logger.error("Error creating alert file " + alertFile.getAbsolutePath(), e);
             }
          }
       }
    }
    
}
