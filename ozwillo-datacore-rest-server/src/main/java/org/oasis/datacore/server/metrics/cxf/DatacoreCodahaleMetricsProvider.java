package org.oasis.datacore.server.metrics.cxf;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.metrics.MetricsContext;
import org.apache.cxf.metrics.codahale.CodahaleMetricsContext;
import org.apache.cxf.metrics.codahale.CodahaleMetricsProvider;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;


/**
 * Setup of CXF codahale-based metrics, but with
 * - a CSV reporter outputting "the maximum “number of requests per minute” per day" as required in specs
 * - a custom reporter using the same metric to allow for horizontal scalability.
 * 
 * The CSV Reporter is conf'd with per minute rates to allow to compute requirements (D2.6 T5, see #48) :
 * "the maximum “number of requests per minute” per day",
 * which is then the daily max of the m1_rate (14th) column of the file
 * DatacoreApiImpl_Attribute=Totals.csv (or per operation).
 * 
 * Said one minute rate can be displayed in test by :
 * tail -f "target/metrics/DatacoreApiImpl_Attribute=Totals.csv" | awk ' { print $14 }' FS=","
 * 
 * =>
 * 0.095510
 * 0.087874
 * 2.959249
 * 3.682107
 * 3.387702
 * 
 * The custom scalability reporter uses CXF codahale-based metrics to provide horizontal scalability,
 * by scheduling a reporter which triggers an alert if the currently
 * measured number of requests per minute is outside defined min & max range.
 * This min & max range is dynamically retrieved the configured OCCI server's configurations
 * if any, or else from configured properties. Default is 0, which disables it.
 * Said alert consists in creating a file on the file system, which can be detected by
 * deployment & monitoring solutions such as Roboconf.
 * 
 * NB. both reporters can't be setup outside the single class that contains
 * the MetricsRegistry (else registries beyond the first one contain no
 * metrics, or accessing it from outside fails to resolve in Spring injection).
 * 
 * NB. CXF's Dropwizard metrics are only registered when the first request
 * (to an endpoint...) is intercepted. Init only registers interceptors.
 * 
 * @author mdutoo
 *
 */
@Component("datacoreApiServer.metrics.provider")
public class DatacoreCodahaleMetricsProvider extends CodahaleMetricsProvider {
   
   ////////////////////
   // CSV
   
   /** metrics/ , below target/ for tests */
   //@Value("${datacoreApiServer.metrics.csvReportPath}")
   private String csvReportPath = "target/metrics/";
   /** 60 SECONDS (or finer ex. 5 for tests) */
   //@Value("${datacoreApiServer.metrics.csvReportPeriod}")
   private long csvReportPeriod = 5;

   ////////////////////
   // SCALABILITY

   /** 60 SECONDS (or finer ex. 5 for tests) */
   //@Value("${datacoreApiServer.metrics.meanRequestThreshold.period}")
   private long meanRequestThresholdPeriod = 5;
   /** OCCI conf in (MART) server where the threshold is retrieved from, ex.
    * http://localhost:8080/LDServicePerformanceSLOMixin/ or to test it .../compute/
    * (if empty, explicit max/min props are used) */
   //@Value("${datacoreApiServer.metrics.meanRequestThreshold.occiUrl}")
   private String meanRequestThresholdOcciUrl = "";
   /** ex. "/0/attributes/requestPerMinuteMax" or to test it "/0/attributes/occi.compute.cores" */
   //@Value("${datacoreApiServer.metrics.meanRequestThreshold.maxOcciJsonPointer}")
   private String meanRequestThresholdMaxOcciJsonPointer = "";
   /** max threshold (only if no occiMartServerUrl, disabled if 0) */
   //@Value("${datacoreApiServer.metrics.meanRequestThreshold.defaultMax}")
   private float meanRequestThresholdMax = 0;
   /** this file will be created by BrokenThresholdReporter if there is a max threshold and it is broken */
   //@Value("${datacoreApiServer.metrics.meanRequestThreshold.maxAlertFilePath}")
   private String meanRequestThresholdMaxAlertFilePath = "/tmp/vmfile";
   /** ex. "/0/attributes/requestPerMinuteMin" */
   //@Value("${datacoreApiServer.metrics.meanRequestThreshold.minOcciJsonPointer}")
   private String meanRequestThresholdMinOcciJsonPointer = "";
   /** min threshold (only if no occiMartServerUrl, disabled if 0) */
   //@Value("${datacoreApiServer.metrics.meanRequestThreshold.defaultMin}")
   private float meanRequestThresholdMin = 0;
   /** this file will be created by BrokenThresholdReporter if there is a min threshold and it is broken */
   //@Value("${datacoreApiServer.metrics.meanRequestThreshold.minAlertFilePath}")
   private String meanRequestThresholdMinAlertFilePath = "/tmp/vmfilemin";
   
   /**
    * NB. can't inject props as fields
    * 
    * @param datacoreApiServer used to get the server-side CXF Bus, else direct injection
    * mistakes the client-side bus for it.
    * @param csvReportPath metrics/ , below target/ for tests
    * @param csvReportPeriod 60 SECONDS (or finer ex. 5 for tests)
    */
   @Autowired
   public DatacoreCodahaleMetricsProvider(
         @Qualifier("datacoreApiServer") final Object datacoreApiServer,
         @Value("${datacoreApiServer.metrics.csvReportPath}") final String csvReportPath,
         @Value("${datacoreApiServer.metrics.csvReportPeriod}") final long csvReportPeriod,
         @Value("${datacoreApiServer.metrics.meanRequestThreshold.period}") final long meanRequestThresholdPeriod,
         @Value("${datacoreApiServer.metrics.meanRequestThreshold.occiUrl}") final String meanRequestThresholdOcciUrl,
         @Value("${datacoreApiServer.metrics.meanRequestThreshold.maxOcciJsonPointer}") final String meanRequestThresholdMaxOcciJsonPointer,
         @Value("${datacoreApiServer.metrics.meanRequestThreshold.defaultMax}") final float meanRequestThresholdMax,
         @Value("${datacoreApiServer.metrics.meanRequestThreshold.maxAlertFilePath}") final String meanRequestThresholdMaxAlertFilePath,
         @Value("${datacoreApiServer.metrics.meanRequestThreshold.minOcciJsonPointer}") final String meanRequestThresholdMinOcciJsonPointer,
         @Value("${datacoreApiServer.metrics.meanRequestThreshold.defaultMin}") final float meanRequestThresholdMin,
         @Value("${datacoreApiServer.metrics.meanRequestThreshold.minAlertFilePath}") final String meanRequestThresholdMinAlertFilePath) {
      super(((JAXRSServerFactoryBean) datacoreApiServer).getBus()); // NB. by default InstrumentationManager.class extension is missing
      // so won't create any JMX reporter
      
      // csv
      this.csvReportPath = csvReportPath;
      this.csvReportPeriod = csvReportPeriod;
      setupCSVReporter(this.registry);
      
      // scalability
      this.meanRequestThresholdPeriod = meanRequestThresholdPeriod;
      this.meanRequestThresholdOcciUrl = meanRequestThresholdOcciUrl;
      this.meanRequestThresholdMaxOcciJsonPointer = meanRequestThresholdMaxOcciJsonPointer;
      this.meanRequestThresholdMax = meanRequestThresholdMax;
      this.meanRequestThresholdMaxAlertFilePath = meanRequestThresholdMaxAlertFilePath;
      this.meanRequestThresholdMinOcciJsonPointer = meanRequestThresholdMinOcciJsonPointer;
      this.meanRequestThresholdMin = meanRequestThresholdMin;
      this.meanRequestThresholdMinAlertFilePath = meanRequestThresholdMinAlertFilePath;
      setupMeanRequestThresholdAlertReporter(this.registry);
   }

   private void setupCSVReporter(MetricRegistry registry) {
      if (csvReportPeriod <= 0) {
         return; // disabled
      }
      
      File csvReportDir = new File(csvReportPath);
      if (!csvReportDir.exists()) {
         csvReportDir.mkdirs(); // else IOException in CsvReporter
      }
      
      // (inspired by CXF')
      final CsvReporter reporter = CsvReporter.forRegistry(registry) // SanitizedCsvReporter
            .formatFor(Locale.US)
            .convertRatesTo(TimeUnit.MINUTES) // MINUTES as per requirements
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build(csvReportDir);
      reporter.start(csvReportPeriod, TimeUnit.SECONDS); // 60 SECONDS (or finer ex. for tests)
   }

   private void setupMeanRequestThresholdAlertReporter(MetricRegistry registry) {
      if (meanRequestThresholdPeriod <= 0) {
         return; // disabled
      }
      
      // (inspired by CXF')
      if (meanRequestThresholdMax > 0 || meanRequestThresholdMin > 0) {
         final MeanRequestThresholdAlertFileReporter meanThresholdBrokenFileReporter = MeanRequestThresholdAlertFileReporter.forRegistry(registry)
               .convertRatesTo(TimeUnit.MINUTES) // MINUTES as per requirements
               .convertDurationsTo(TimeUnit.MILLISECONDS)
               .occiUrl(meanRequestThresholdOcciUrl)
               .maxOcciJsonPointer(meanRequestThresholdMaxOcciJsonPointer)
               .max(meanRequestThresholdMax)
               .maxAlertFilePath(meanRequestThresholdMaxAlertFilePath)
               .minOcciJsonPointer(meanRequestThresholdMinOcciJsonPointer)
               .min(meanRequestThresholdMin)
               .minAlertFilePath(meanRequestThresholdMinAlertFilePath)
               .build();
         meanThresholdBrokenFileReporter.start(meanRequestThresholdPeriod, TimeUnit.SECONDS); // 60 SECONDS (or finer ex. for tests)
         // TODO LATER get csvReportPeriod from OCCI server
      } // else disabled
   }
   

   // NOT override
   StringBuilder getBaseServiceName(Endpoint endpoint, boolean isClient, String clientId) {
       /*StringBuilder buffer = new StringBuilder();
       if (endpoint.get("org.apache.cxf.management.service.counter.name") != null) {
           buffer.append((String)endpoint.get("org.apache.cxf.management.service.counter.name"));
       } else {
           Service service = endpoint.getService();

           String serviceName = "\"" + escapePatternChars(service.getName().toString()) + "\"";*/
           String portName = "\"" + endpoint.getEndpointInfo().getName().getLocalPart() + "\"";

           /*buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME + ":");
           buffer.append(ManagementConstants.BUS_ID_PROP + "=" + bus.getId() + ",");
           buffer.append(ManagementConstants.TYPE_PROP).append("=Metrics");
           if (isClient) {
               buffer.append(".Client,");
           } else {
               buffer.append(".Server,");
           }
           buffer.append(ManagementConstants.SERVICE_NAME_PROP + "=" + serviceName + ",");
           buffer.append(ManagementConstants.PORT_NAME_PROP + "=" + portName + ",");
           if (clientId != null) {
               buffer.append("Client=" + clientId + ",");
           }
           */
           
           // [OZWILLO] to get a (sanitized) and simpler CSV file name, otherwise
           // org.apache.cxf:bus.id=cxf1091151531,type=Metrics.Server,service="{http://server.rest.datacore.oasis.org/}DatacoreApiImpl",port="DatacoreApiImpl",Attribute=In Flight
           // => File IOException in CsvReporter (but breaks JMX export)
           // TODO LATER use metrics v>v3.1.2 when there is one (not yet on 20151204)
           // to be able to write a custom CsvFileProvider for CsvReporter
           // (for now CsvReporter.sanitize() is protected but unoverridable)
           return new StringBuilder(/*bus.getId() + "_" + */portName.replaceAll("\\W+", "") + "_"/* + "_"*/);
       /*}
       return buffer;*/
   }
   
   
   /** {@inheritDoc}*/
   @Override
   public MetricsContext createEndpointContext(final Endpoint endpoint, boolean isClient, String clientId) {
       if (isClient) {
          return null; // [OZWILLO] disable for client, else twice as many files
          // ("DatacoreClientJson/RdfApi....csv" for all metrics)
       }
       StringBuilder buffer = getBaseServiceName(endpoint, isClient, clientId); // [OZWILLO] override name
       final String baseName = buffer.toString();
       return new CodahaleMetricsContext(baseName, registry);
   }

   /** {@inheritDoc}*/
   @Override
   public MetricsContext createOperationContext(Endpoint endpoint, BindingOperationInfo boi,
                                                boolean asClient, String clientId) {
       if (asClient) {
          return null; // [OZWILLO] disable for client, else twice as many files
          // ("DatacoreClientJson/RdfApi....csv" for all metrics)
       }
       StringBuilder buffer = getBaseServiceName(endpoint, asClient, clientId); // [OZWILLO] override name
       buffer.append("Operation=").append(boi.getName().getLocalPart()).append(',');
       return new CodahaleMetricsContext(buffer.toString(), registry); // null; // disable
   }

   /** {@inheritDoc}*/
   @Override
   public MetricsContext createResourceContext(Endpoint endpoint, String resourceName, 
                                               boolean asClient, String clientId) {
       if (asClient) {
          return null; // [OZWILLO] disable for client, else twice as many files
          // ("DatacoreClientJson/RdfApi....csv" for all metrics)
       }
       StringBuilder buffer = getBaseServiceName(endpoint, asClient, clientId); // [OZWILLO] override name
       buffer.append("Operation=").append(resourceName).append(',');
       return new CodahaleMetricsContext(buffer.toString(), registry); // null; // disable
   }
   
}
