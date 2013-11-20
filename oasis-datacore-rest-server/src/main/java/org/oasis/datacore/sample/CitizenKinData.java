package org.oasis.datacore.sample;

import java.util.AbstractMap;
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
 *
 * @author jguillemotte
 */
@Component
@DependsOn("CitizenkinModel")
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

		listUser = new ArrayList<DCResource>();
		listProcedure = new ArrayList<DCResource>();

		mapData = new HashMap<String, List<DCResource>>();

		createDataSample();

		if(enableCitizenKinSampleDataInsertionAtStartup) {
			insertData();
		}
	}

    public void createDataSample() {

		DCResource user1 = buildResource(CitizenKinModel.USER_MODEL_NAME,
			new AbstractMap.SimpleEntry<>("id", 1),
            new AbstractMap.SimpleEntry<>("firstName", "Eric"),
			new AbstractMap.SimpleEntry<>("lastName", "Arbos"));

		DCResource user2 = buildResource(CitizenKinModel.USER_MODEL_NAME,
			new AbstractMap.SimpleEntry<>("id", 2),
            new AbstractMap.SimpleEntry<>("firstName", "Marie"),
			new AbstractMap.SimpleEntry<>("lastName", "Dumont"));

		DCResource user3 = buildResource(CitizenKinModel.USER_MODEL_NAME,
			new AbstractMap.SimpleEntry<>("id", 1),
            new AbstractMap.SimpleEntry<>("firstName", "Jim"),
			new AbstractMap.SimpleEntry<>("lastName", "Van den boss"));

		DCResource user4 = buildResource(CitizenKinModel.USER_MODEL_NAME,
			new AbstractMap.SimpleEntry<>("id", 1),
            new AbstractMap.SimpleEntry<>("firstName", "Robert"),
			new AbstractMap.SimpleEntry<>("lastName", "Dillingen"));

		DCResource user5 = buildResource(CitizenKinModel.USER_MODEL_NAME,
			new AbstractMap.SimpleEntry<>("id", 1),
            new AbstractMap.SimpleEntry<>("firstName", "Céline"),
			new AbstractMap.SimpleEntry<>("lastName", "Arboldi"));

		listUser.add(user1);
        listUser.add(user2);
        listUser.add(user3);
        listUser.add(user4);
        listUser.add(user5);

		DCResource procedure1 = buildResource(CitizenKinModel.PROCEDURE_MODEL_NAME,
            new AbstractMap.SimpleEntry<>("name", "election"));

		DCResource procedure2 = buildResource(CitizenKinModel.PROCEDURE_MODEL_NAME,
			new AbstractMap.SimpleEntry<>("name", "simpleForm"));

        listProcedure.add(procedure1);
        listProcedure.add(procedure2);

        mapData.put(CitizenKinModel.USER_MODEL_NAME, listUser);
        mapData.put(CitizenKinModel.PROCEDURE_MODEL_NAME, listProcedure);

    }

	public void insertData() {

		try {
			api.postAllDataInType(listUser, CitizenKinModel.USER_MODEL_NAME);
		} catch (WebApplicationException e) {}
		try {
			api.postAllDataInType(listProcedure, CitizenKinModel.PROCEDURE_MODEL_NAME);
		} catch (WebApplicationException e) {}
	}

	public HashMap<String, List<DCResource>> getData() {
		return mapData;
	}

	@SafeVarargs
	private final DCResource buildResource(String modelType, AbstractMap.SimpleEntry<String,?>... entries) {
		DCResource resource = new DCResource();
		List<String> types = new ArrayList<>();
		types.add(modelType);
		resource.setTypes(types);
		for(AbstractMap.SimpleEntry<String, ?> entry : entries) {
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
