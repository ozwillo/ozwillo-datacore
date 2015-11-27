package org.oasis.datacore.sample.citizenkin;

import org.oasis.datacore.core.meta.model.DCField;
import org.oasis.datacore.core.meta.model.DCFieldTypeEnum;
import org.oasis.datacore.core.meta.model.DCModel;
import org.oasis.datacore.core.meta.model.DCModelBase;
import org.oasis.datacore.core.meta.pov.DCProject;
import org.oasis.datacore.sample.DatacoreSampleBase;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * User: schambon
 * Date: 10/15/14
 */
@Component
public class CitizenKinEnvironmentalIncidentsModelInitializer extends DatacoreSampleBase {

    public static final String CITIZENKIN_PROCEDURE_ENVIRONMENTAL_INCIDENTS_DECLARATION = "citizenkin:environmental-incidents_0";

    public static final String STRING_TYPE = DCFieldTypeEnum.STRING.getType();
    public static final String DATE_TYPE = DCFieldTypeEnum.DATE.getType();
    public static final String PREFIX = "ck_environmental-incidents:";

    /** main model !! */
    @Override
    protected boolean neverCleanData() {
       return true;
    }
    
    /** overriden to stay as before in oasis.main */
    @Override
    protected DCProject getProject() {
       return projectInitService.getCitizenKinProject();
    }


    @Override
    protected void buildModels(List<DCModelBase> models) {
        DCModel model = new DCModel(CITIZENKIN_PROCEDURE_ENVIRONMENTAL_INCIDENTS_DECLARATION);
        model.setDocumentation(""); // TODO: provide documentation for that model
        model.addField(new DCField(PREFIX + "nom", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "cognom", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "telefon", STRING_TYPE, false, 0));
        model.addField(new DCField(PREFIX + "email", STRING_TYPE, false, 0));
        model.addField(new DCField(PREFIX + "municipiusuari", STRING_TYPE, false, 0));
        model.addField(new DCField(PREFIX + "tipususuari", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "data", DATE_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "municipiincidencia", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "tipusincidencia", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "textincidencia", STRING_TYPE, false, 0));
        model.addField(new DCField(PREFIX + "adjuntarimatge", STRING_TYPE, false, 0));

        models.add(model);
    }

    @Override
    protected void fillData() {

    }
}
