/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mesclasses.objects.tasks;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mesclasses.handlers.DonneesHandler;
import mesclasses.handlers.ModelHandler;
import mesclasses.model.Classe;
import mesclasses.model.Cours;
import mesclasses.model.Eleve;
import mesclasses.model.EleveData;
import mesclasses.model.Journee;
import mesclasses.model.Seance;
import mesclasses.util.NodeUtil;
import mesclasses.util.validation.FError;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author rrrt3491
 */
public class ComputeTask extends AppTask<Object> {
    
    private static final Logger LOG = LogManager.getLogger(ComputeTask.class);
    
    private ModelHandler handler;
    
    private int nbJournees;
    private int nbCoursPonctuels;
    private int nbSeances;
    private int nbDonnees;
    private int nbPunitions;
    
    private int nbDonneesInitiales;
    private int nbPunitionsInitiales;
    
    public ComputeTask(){
        super();
        updateProgress(0, 5.0);
    }
    @Override
    public Object call() throws Exception {
        handler = ModelHandler.getInstance();
        process();
        return null;
    }

    private void process() throws Exception {
        
        try {
            updateDonnees();
            if(handler.getTrimestres().isEmpty()){
                return;
            }
            getInitialStats();
            LocalDate day = handler.getTrimestres().get(0).getStartAsDate();
            long nbDays = ChronoUnit.DAYS.between(day, LocalDate.now());
            while(day.isBefore(LocalDate.now().plusDays(1))){
                // pas de journée créée pour ce jour
                if(!handler.getJournees().containsKey(day)){
                    Journee journee = createJournee(day);
                    handler.declareJournee(journee);
                }
                nbJournees++;
                day = day.plusDays(1);
                updateProgress(nbJournees, nbDays);
            }
            updatePunitions();
        } catch(Exception e){
            LOG.error(e);
            setMsg("Impossible d'effectuer la conversion en séances : "+e.getMessage());
            throw e;
        }
        checkData();
        LOG.info("nbDonneesInitiales : "+nbDonneesInitiales+", nbPunitionsInitiales : "+nbPunitionsInitiales);
        LOG.info("journees : "+nbJournees+", seances "+nbSeances+", cours additionnels : "
                +nbCoursPonctuels+", donnees traitées : "+nbDonnees+", punitions traitées : "+nbPunitions);
    }
    
    public Journee createJournee(LocalDate date){
        if(date == null){
            return null;
        }
        Journee journee = new Journee();
        journee.setDate(date);
        handler.getClasses().forEach(c -> {
            buildSeancesForClasse(journee, c, date);
        });
        Collections.sort(journee.getCoursPonctuels());
        Collections.sort(journee.getSeances());
        return journee;
    }

    private void updatePunitions(){
        handler.getClasses().forEach(c -> {
            c.getEleves().forEach(e -> {
                e.getPunitions().forEach(p -> {
                    Journee journee = handler.getJournee(p.getDateAsDate());
                    if(journee == null){
                        LOG.error("erreur : punition "+p.getId()+" n'a pas de journée associée...");
                        return;
                    }
                    List<Seance> seances = journee.getSeances().filtered(s -> s.getClasse().getName().equals(p.getEleve().getClasse().getName()));
                    if(seances == null || seances.isEmpty()){
                        LOG.error("erreur : punition "+p.getId()+" n'a pas de séance associée...");
                        return;
                    }
                    p.setSeance(seances.get(0));
                    nbPunitions++;
                });
            });
        });
    }
    
    private void checkData() throws Exception {
        final List<FError> errList = handler.getData().validate();
        if(!errList.isEmpty()){
            LOG.error("\n[ "+StringUtils.join(errList, " ]\n[ ")+" ]");
        }
    }
    
    private void addDonneesToSeance(List<EleveData> donnees, Seance seance){
        donnees.forEach(d -> {
            LOG.info(d.getDisplayName()+" ajoutée à la séance "+seance.getDisplayName());
            d.getEleve().getData().remove(d);
            d.setSeance(seance);
            DonneesHandler.getInstance().persistEleveData(d);
        });
    }
    
    private void buildSeancesForClasse(Journee journee, Classe classe, LocalDate date) {
        try {
            // liste des cours pour la classe à cette date
            List<Cours> listeCours = handler.getCoursForDateAndClasse(date, classe);
            // map des données élèves, réparties par cours
            Map<Integer, List<EleveData>> donneesParCours = getDataParCoursForDate(classe, date);
            
            if(listeCours.size() < donneesParCours.size()){
                LOG.debug("Cours trouvés pour la "+classe+" le "+NodeUtil.getJour(date)+" :");
                LOG.debug("\n"+StringUtils.join(listeCours, "\n"));
                LOG.debug("Journée du "+journee.getDate()+" : "+listeCours.size()+" cours prévus, "+donneesParCours.size()+" cours trouvés");
                LOG.debug("Index des cours : "+StringUtils.join(donneesParCours.keySet(), ", "));
            }
            //cours normaux
            for(int index = 0; index < listeCours.size(); index++){
                Seance seance = handler.addSeanceWithCours(journee, listeCours.get(index));
                if(donneesParCours.containsKey(index+1)){
                    LOG.debug("Création de séance pour le cours "+(index+1));
                    addDonneesToSeance(donneesParCours.get(index+1), seance);
                    nbDonnees+=donneesParCours.get(index+1).size();
                }
            }
            //cours 0 ?
            if(donneesParCours.containsKey(0)){
                Seance seance = handler.addSeanceWithCoursPonctuel(journee, classe);
                addDonneesToSeance(donneesParCours.get(0), seance);
                    nbDonnees+=donneesParCours.get(0).size();
            }
            
            // cours ponctuels
            for(int index = listeCours.size(); index < donneesParCours.size(); index++){
                if(!donneesParCours.containsKey(index+1)){
                    break;
                }
                LOG.debug("Création de séance ponctuelle pour le cours "+(index+1));
                Seance seance = handler.addSeanceWithCoursPonctuel(journee, classe);
                addDonneesToSeance(donneesParCours.get(index+1), seance);
                nbDonnees+=donneesParCours.get(index+1).size();
            }
            nbSeances += journee.getSeances().size();
        } catch(Exception e){
            LOG.error(e);
            setMsg(e.getMessage());
        }
    }
    
    
    private Map<Integer, List<EleveData>> getDataParCoursForDate(Classe classe, LocalDate date){
        Map<Integer, List<EleveData>> res = new HashMap<>();
        if(classe == null || classe.getEleves() == null || classe.getEleves().isEmpty()){
            return null;
        }
        for(Eleve eleve : classe.getEleves()){
            for(EleveData eleveData : eleve.getData()){
                if(eleveData.getDateAsDate().isEqual(date)){
                    if(!res.containsKey(eleveData.getCours())){
                        res.put(eleveData.getCours(), new ArrayList<>());
                    }
                    res.get(eleveData.getCours()).add(eleveData);
                }
            }
        }
        return res;
    }
    @Override
    public String getName() {
        return "Construction des séances";
    }

    private void getInitialStats() {
        handler.getClasses().forEach(c -> {
            c.getEleves().forEach(e -> {
                nbDonneesInitiales+=e.getData().size();
                nbPunitionsInitiales+=e.getPunitions().size();
            });
        });
    }

    /**
     * inscrit l'élève associé dans chaque donnée
     */
    private void updateDonnees() {
        handler.getClasses().forEach(c -> {
            c.getEleves().forEach(e -> {
                e.getData().forEach(d -> {
                    d.setEleve(e);
                });
                e.getPunitions().forEach(d -> {
                    d.setEleve(e);
                });
            });
        });
    }
    
    
}

