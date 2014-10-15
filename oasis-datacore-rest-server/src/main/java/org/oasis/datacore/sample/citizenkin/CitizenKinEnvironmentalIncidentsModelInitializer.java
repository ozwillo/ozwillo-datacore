package org.oasis.datacore.sample.citizenkin;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.sample.DatacoreSampleBase;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * User: schambon
 * Date: 10/15/14
 */
@Component
public class CitizenKinEnvironmentalIncidentsModelInitializer extends DatacoreSampleBase {

    public static final String CITIZENKIN_PROCEDURE_ENVIRONMENTAL_INCIDENTS_DECLARATION = "citizenkin.procedure.environmental-incidents";

    public static final String STRING_TYPE = DCFieldTypeEnum.STRING.getType();
    public static final String DATE_TYPE = DCFieldTypeEnum.DATE.getType();


    @Override
    protected void buildModels(List<DCModelBase> models) {
        DCModel model = new DCModel(CITIZENKIN_PROCEDURE_ENVIRONMENTAL_INCIDENTS_DECLARATION);
        model.setDocumentation(""); // TODO: provide documentation for that model
        model.addField(new DCField("nom", STRING_TYPE, true, 0));
        model.addField(new DCField("cognom", STRING_TYPE, true, 0));
        model.addField(new DCField("telefon", STRING_TYPE, false, 0));
        model.addField(new DCField("email", STRING_TYPE, false, 0));
        model.addField(new DCField("municipiusuari", STRING_TYPE, false, 0));
        model.addField(new DCField("tipususuari", STRING_TYPE, true, 0));
        model.addField(new DCField("data", DATE_TYPE, true, 0));
        model.addField(new DCField("municipiincidencia", STRING_TYPE, true, 0));
        model.addField(new DCField("tipusincidencia", STRING_TYPE, true, 0));
        model.addField(new DCField("textincidencia", STRING_TYPE, false, 0));
        model.addField(new DCField("adjuntarimatge", STRING_TYPE, false, 0));

        models.add(model);
    }

    @Override
    protected void fillData() {

    }
}
