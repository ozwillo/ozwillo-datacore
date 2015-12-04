package org.oasis.datacore.server.metrics.cxf;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.Bus;
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
 * Provides CXF codahale-based metrics, but with a CSV Reporter
 * with per minute rates to allow to compute requirements (D2.6 T5, see #48) :
 * "the maximum “number of requests per minute” per day",
 * which is then the daily max of the m1_rate (14th) column of the file
 * DatacoreApiImpl_Attribute=Totals.csv (or per operation).
 * 
 * Said one minute rate can be displayed in test by :
 * tail -f "target/metrics/DatacoreApiImpl_Attribute=Totals.csv" | awk ' { print $14 }' FS=","
 * =>
 * 0.095510
 * 0.087874
 * 2.959249
 * 3.682107
 * 3.387702
 * 
 * @author mdutoo
 *
 */
@Component("datacoreApiServer.metrics.provider")
public class DatacoreCodahaleMetricsProvider extends CodahaleMetricsProvider {
   
   /** metrics/ , below target/ for tests */
   //@Value("${datacoreApiServer.metrics.csvReportPath}")
   private String csvReportPath = "target/metrics/";
   /** 60 SECONDS (or finer ex. 5 for tests) */
   //@Value("${datacoreApiServer.metrics.csvReportPeriod}")
   private long csvReportPeriod = 5;

   /**
    * NB. can't inject props as fields
    * 
    * @param datacoreApiServer used to get the server-side CXF Bus, else direct injection
    * mistakes the client-side bus for it.
    * @param csvReportPath metrics/ , below target/ for tests
    * @param csvReportPeriod 60 SECONDS (or finer ex. 5 for tests)
    */
   @Autowired
   public DatacoreCodahaleMetricsProvider(@Qualifier("datacoreApiServer") final Object datacoreApiServer,
         @Value("${datacoreApiServer.metrics.csvReportPath}") final String csvReportPath,
         @Value("${datacoreApiServer.metrics.csvReportPeriod}") final long csvReportPeriod) {
      super(((JAXRSServerFactoryBean) datacoreApiServer).getBus()); // NB. by default InstrumentationManager.class extension is missing
      // so won't create any JMX reporter
      
      this.csvReportPath = csvReportPath;
      this.csvReportPeriod = csvReportPeriod;
      Bus b = ((JAXRSServerFactoryBean) datacoreApiServer).getBus();
      setupCSVReporter(b, registry);
   }

   private void setupCSVReporter(Bus b, MetricRegistry registry) {
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
