package org.oasis.datacore.sample;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ws.rs.WebApplicationException;
import org.oasis.datacore.rest.api.DCResource;
import org.oasis.datacore.rest.api.util.UriHelper;
import org.oasis.datacore.rest.server.DatacoreApiImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Citizen Kin data
 * @author jguillemotte
 */
@Component
@DependsOn("citizenKinModel")
public class CitizenKinData {

	private List<DCResource> listUser;
	private List<DCResource> listProcedure;

    private HashMap<String, List<DCResource>> mapData;

	@Value("${datacoreApiServer.containerUrl}")
	private String containerUrl;

	@Value("#{new Boolean('${datacoreApiServer.enableCitizenKinSampleDataInsertionAtStartup}')}")
	private Boolean enableCitizenKinSampleDataInsertionAtStartup;

	@Autowired
	protected DatacoreApiImpl api;

	@PostConstruct
	public void init() {

		listUser = new ArrayList<>();
		listProcedure = new ArrayList<>();
		mapData = new HashMap<>();

		createDataSample();

		if(enableCitizenKinSampleDataInsertionAtStartup) {
			insertData();
		}
	}

    public void createDataSample() {

		DCResource user1 = buildResource(CitizenKinModel.USER_MODEL_NAME,
			new SimpleEntry<>("id", 1),
            new SimpleEntry<>("firstName", "Eric"),
			new SimpleEntry<>("lastName", "Arbos"));

		DCResource user2 = buildResource(CitizenKinModel.USER_MODEL_NAME,
			new SimpleEntry<>("id", 2),
            new SimpleEntry<>("firstName", "Marie"),
			new SimpleEntry<>("lastName", "Dumont"));

		DCResource user3 = buildResource(CitizenKinModel.USER_MODEL_NAME,
			new SimpleEntry<>("id", 3),
            new SimpleEntry<>("firstName", "Jim"),
			new SimpleEntry<>("lastName", "Van den boss"));

		DCResource user4 = buildResource(CitizenKinModel.USER_MODEL_NAME,
			new SimpleEntry<>("id", 4),
            new SimpleEntry<>("firstName", "Robert"),
			new SimpleEntry<>("lastName", "Dillingen"));

		DCResource user5 = buildResource(CitizenKinModel.USER_MODEL_NAME,
			new SimpleEntry<>("id", 5),
            new SimpleEntry<>("firstName", "Céline"),
			new SimpleEntry<>("lastName", "Arboldi"));

		listUser.add(user1);
        listUser.add(user2);
        listUser.add(user3);
        listUser.add(user4);
        listUser.add(user5);

        List<String> agentsList1 = new ArrayList<>();
        agentsList1.add(user2.getUri());
        agentsList1.add(user5.getUri());
		DCResource procedure1 = buildResource(CitizenKinModel.PROCEDURE_MODEL_NAME,
            //new SimpleEntry<>("id", 1),
            new SimpleEntry<>("id", "election"),
            new SimpleEntry<>("agents", agentsList1));

        List<String> agentsList2 = new ArrayList<>();
        agentsList2.add(user1.getUri());
		DCResource procedure2 = buildResource(CitizenKinModel.PROCEDURE_MODEL_NAME,
			//new SimpleEntry<>("id", 2),
            new SimpleEntry<>("id", "simpleform"),
            new SimpleEntry<>("agents", agentsList2));

        listProcedure.add(procedure1);
        listProcedure.add(procedure2);

        mapData.put(CitizenKinModel.USER_MODEL_NAME, listUser);
        mapData.put(CitizenKinModel.PROCEDURE_MODEL_NAME, listProcedure);
    }

	public void insertData() {
		try {
			api.postAllDataInType(listUser, CitizenKinModel.USER_MODEL_NAME);
		} catch (WebApplicationException ex) {}
		try {
			api.postAllDataInType(listProcedure, CitizenKinModel.PROCEDURE_MODEL_NAME);
		} catch (WebApplicationException ex) {}
	}

	public HashMap<String, List<DCResource>> getData() {
		return mapData;
	}

	@SafeVarargs
	private final DCResource buildResource(String modelType, SimpleEntry<String,?>... entries) {
		DCResource resource = new DCResource();
		List<String> types = new ArrayList<>();
		types.add(modelType);
		resource.setTypes(types);
		for(SimpleEntry<String, ?> entry : entries) {
			if(entry != null) {
				if("id".equals(entry.getKey())) {
					resource.setUri(UriHelper.buildUri(containerUrl, modelType, String.valueOf(entry.getValue())));
				}
				resource.setProperty(entry.getKey(), entry.getValue());
			}
		}
		return resource;
	}

}
