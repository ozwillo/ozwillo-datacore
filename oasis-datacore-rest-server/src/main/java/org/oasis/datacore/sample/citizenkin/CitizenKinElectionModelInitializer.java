package org.oasis.datacore.sample.citizenkin;

import java.util.Arrays;
import java.util.List;

import org.oasis.datacore.core.meta.model.*;
import org.oasis.datacore.sample.DatacoreSampleBase;
import org.springframework.stereotype.Component;

/**
 * Electoral roll registration request. To be valid, the resource MUST have BOTH an envelope and a procedure
 *
 * NOTE: this models the specifically French procedure. This has very
 * little applicability to other countries; therefore for clarity the
 * field names are in the original French.
 *
 * User: schambon
 * Date: 12/24/13
 */
@Component
public class CitizenKinElectionModelInitializer extends DatacoreSampleBase {

    public static final String CITIZENKIN_PROCEDURE_ELECTORAL_ROLL_REGISTRATION = "citizenkin:electoral_roll_registration_0";

    public static final String STRING_TYPE = DCFieldTypeEnum.STRING.getType();
    public static final String DATE_TYPE = DCFieldTypeEnum.DATE.getType();
    public static final String PREFIX = "ck_electoral_roll_registration:";

    @Override
    public void buildModels(List<DCModelBase> modelsToCreate) {

        DCModel model = new DCModel(CITIZENKIN_PROCEDURE_ELECTORAL_ROLL_REGISTRATION);
        model.setDocumentation(""); // TODO

        model.addField(new DCField(PREFIX + "nom_de_famille", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "nom_d_usage", STRING_TYPE, false, 0));
        model.addField(new DCField(PREFIX + "prenom", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "sexe", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "date_de_naissance", DATE_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "commune_de_naissance", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "code_departement_de_naissance", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "libelle_territoire_de_naissance_outremer", STRING_TYPE, false, 0));
        model.addField(new DCField(PREFIX + "pays_de_naissance", STRING_TYPE, false, 0));

        /* One of:
        "premiere_demande"
        "demenagement_meme_commune"
        "changement_commune"
         */
        model.addField(new DCField(PREFIX + "type_de_demande", STRING_TYPE, true, 0));

        model.addField(new DCField(PREFIX + "commune_origine", STRING_TYPE, false, 0));
        model.addField(new DCField(PREFIX + "code_departement_origine", STRING_TYPE, false, 0));
        model.addField(new DCField(PREFIX + "libelle_territoire_outremer_origine", STRING_TYPE, false, 0));

        model.addField(new DCField(PREFIX + "ambassade_poste_consulaire_origine", STRING_TYPE, false, 0));
        model.addField(new DCField(PREFIX + "pays_origine", STRING_TYPE, false, 0));

        model.addField(new DCField(PREFIX + "adresse1", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "adresse2", STRING_TYPE, false, 0));
        model.addField(new DCField(PREFIX + "code_postal", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "commune", STRING_TYPE, true, 0));
        model.addField(new DCField(PREFIX + "telephone", STRING_TYPE, false, 0));
        model.addField(new DCField(PREFIX + "courriel", STRING_TYPE, false, 0));

        /*
         * in 2014: points to a CK-local URL
         */
        model.addField(new DCField(PREFIX + "justificatif_identite", STRING_TYPE, true, 0));
        model.addField(new DCListField(PREFIX + "justificatifs_domicile", new DCField("justificatif_domicile", STRING_TYPE, true, 0)));


        modelsToCreate.addAll(Arrays.asList(new DCModelBase[] { model }));
    }

    @Override
    public void fillData() {
    }
    
}
