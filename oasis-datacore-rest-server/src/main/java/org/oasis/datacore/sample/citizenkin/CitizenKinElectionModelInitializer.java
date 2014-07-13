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

    public static final String CITIZENKIN_PROCEDURE_ELECTORAL_ROLL_REGISTRATION = "citizenkin.procedure.electoral_roll_registration";

    public static final String STRING_TYPE = DCFieldTypeEnum.STRING.getType();
    public static final String DATE_TYPE = DCFieldTypeEnum.DATE.getType();

    @Override
    public void buildModels(List<DCModelBase> modelsToCreate) {

        DCModel model = new DCModel(CITIZENKIN_PROCEDURE_ELECTORAL_ROLL_REGISTRATION);

        model.addField(new DCField("nom_de_famille", STRING_TYPE, true, 0));
        model.addField(new DCField("nom_d_usage", STRING_TYPE, false, 0));
        model.addField(new DCField("prenom", STRING_TYPE, true, 0));
        model.addField(new DCField("sexe", STRING_TYPE, true, 0));
        model.addField(new DCField("date_de_naissance", DATE_TYPE, true, 0));
        model.addField(new DCField("commune_de_naissance", STRING_TYPE, true, 0));
        model.addField(new DCField("code_departement_de_naissance", STRING_TYPE, true, 0));
        model.addField(new DCField("libelle_territoire_de_naissance_outremer", STRING_TYPE, false, 0));
        model.addField(new DCField("pays_de_naissance", STRING_TYPE, false, 0));

        /* One of:
        "premiere_demande"
        "demenagement_meme_commune"
        "changement_commune"
         */
        model.addField(new DCField("type_de_demande", STRING_TYPE, true, 0));

        model.addField(new DCField("commune_origine", STRING_TYPE, false, 0));
        model.addField(new DCField("code_departement_origine", STRING_TYPE, false, 0));
        model.addField(new DCField("libelle_territoire_outremer_origine", STRING_TYPE, false, 0));

        model.addField(new DCField("ambassade_poste_consulaire_origine", STRING_TYPE, false, 0));
        model.addField(new DCField("pays_origine", STRING_TYPE, false, 0));

        model.addField(new DCField("adresse1", STRING_TYPE, true, 0));
        model.addField(new DCField("adresse2", STRING_TYPE, false, 0));
        model.addField(new DCField("code_postal", STRING_TYPE, true, 0));
        model.addField(new DCField("commune", STRING_TYPE, true, 0));
        model.addField(new DCField("telephone", STRING_TYPE, false, 0));
        model.addField(new DCField("courriel", STRING_TYPE, false, 0));

        /*
         * in 2014: points to a CK-local URL
         */
        model.addField(new DCField("justificatif_identite", STRING_TYPE, true, 0));
        model.addField(new DCListField("justificatifs_domicile", new DCField("justificatif_domicile", STRING_TYPE, true, 0)));


        modelsToCreate.addAll(Arrays.asList(new DCModelBase[] { model }));
    }

    @Override
    public void fillData() {
    }
    
}
