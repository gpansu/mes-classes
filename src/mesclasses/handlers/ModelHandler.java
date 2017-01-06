/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mesclasses.handlers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import mesclasses.model.Classe;
import mesclasses.model.Constants;
import mesclasses.model.Cours;
import mesclasses.model.Eleve;
import mesclasses.model.EleveData;
import mesclasses.model.Journee;
import mesclasses.model.Punition;
import mesclasses.model.Seance;
import mesclasses.model.Trimestre;
import mesclasses.model.datamodel.ObservableData;
import mesclasses.util.EleveFileUtil;
import mesclasses.util.NodeUtil;
import mesclasses.util.validation.FError;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author rrrt3491
 */
public class ModelHandler {
    
    private ObservableData data;
    private static ModelHandler handler;
    
    public static ModelHandler getInstance(){
        if(handler == null){
            handler = new ModelHandler();
        }
        return handler;
    }
    
    public void injectData(ObservableData data){
        this.data = data;
    }

    public ObservableData getData() {
        return data;
    }
    
    public ObservableList<Trimestre> getTrimestres(){
        return data.getTrimestres();
    }
    
    public ObservableList<Classe> getClasses(){
        return data.getClasses();
    }
    
    public ObservableList<Cours> getCours(){
        return data.getCours();
    }
    
    public ObservableMap<LocalDate, Journee> getJournees(){
        return data.getJournees();
    }
    
    public List<FError> validate(){
        return data.validate();
    }
    /* TRIMESTRES */
    
    public Trimestre createTrimestre(){
        Trimestre trimestre = new Trimestre();
        trimestre.setName("Nom trimestre");
        if(!data.getTrimestres().isEmpty()){
            Trimestre last = data.getTrimestres().get(data.getTrimestres().size()-1);
            trimestre.setStart(last.getEndAsDate().plusDays(1));
            trimestre.setEnd(trimestre.getStartAsDate().plusMonths(3));
        }
        data.getTrimestres().add(trimestre);
        trimestre.startChangeDetection();
        Collections.sort(data.getTrimestres());
        return trimestre;
    }
    
    public void cleanupTrimestres(){
        data.getTrimestres().removeIf(t -> StringUtils.isEmpty(t.getName()));
    }
    
    public void delete(Trimestre trimestre){
        data.getTrimestres().remove(trimestre);
    }
    
    public Trimestre getForDate(LocalDate date){
        for(Trimestre trimestre : data.getTrimestres()){
            if(NodeUtil.isBetween(date, trimestre.getStartAsDate(), trimestre.getEndAsDate())){
             return trimestre;
            }
        }
        return null;
    }
    
    /* CLASSES */
    
    public Classe createClasse(Classe classe){
        data.getClasses().add(classe);
        classe.startChangeDetection();
        return classe;
    }
    
    public void cleanupClasses(){
        data.getClasses().removeIf(t -> StringUtils.isEmpty(t.getName()));
        data.getClasses().forEach(classe -> {
            cleanupElevesForClasse(classe);
        });
    }
    
    public void delete(Classe classe){
        // eleves
        classe.getEleves().forEach(e -> delete(e));
        classe.getEleves().clear();
        
        //cours
        data.getCours().forEach(c -> {
            if(c.getClasse().getName().equals(classe.getName())){
                delete(c);
            }
        });
        data.getCours().removeIf((c) -> c.getClasse().getName().equals(classe.getName()));
        
        data.getClasses().remove(classe);
    }
    
    
    /* ELEVES */
    
    public Eleve createEleve(Eleve eleve){
        eleve.getClasse().getEleves().add(eleve);
        Collections.sort(eleve.getClasse().getEleves());
        eleve.startChangeDetection();
        return eleve;
    }
    
    public void cleanupElevesForClasse(Classe classe){
        if(classe == null){
            return;
        }
        classe.getEleves().removeIf(t -> StringUtils.isEmpty(t.getFirstName()) || StringUtils.isEmpty(t.getLastName()));
        Collections.sort(classe.getEleves());
         classe.getEleves().stream().forEach(eleve -> {
            cleanupDataForEleve(eleve);
         });
    }
    
    public void delete(Eleve eleve){
        eleve.getClasse().getEleves().remove(eleve);
        EleveFileUtil.deleteFilesForEleve(eleve);
    }
    
    public List<Eleve> getOnlyActive(List<Eleve> list){
        return list.stream().filter(e -> e.isActif()).collect(Collectors.toList());
    }
    
    public Eleve moveEleveToClasse(Eleve source, Classe classe){
        Eleve clone = new Eleve();
        clone.setActif(true);
        clone.setClasse(classe);
        clone.setId(source.getId());
        clone.setFirstName(source.getFirstName());
        clone.setLastName(source.getLastName());
        return createEleve(clone);
    }
    
    /* COURS */
    
    /**
     * utilisé par l'emploi du temps
     * @param day
     * @return 
     */
    public ObservableList<Cours> getCoursForDay(String day){
        return data.getCours().stream().filter(c -> c.getDay().equalsIgnoreCase(day)).collect(
                Collectors.toCollection(FXCollections::observableArrayList));
    }
    
    public ObservableList<Cours> getCoursForClasse(Classe classe){
        return data.getCours().stream().filter(c -> 
                c.getClasse().getName().equals(classe.getName())
        ).collect(Collectors.toCollection(FXCollections::observableArrayList));
    }
    
    public ObservableList<Cours> getCoursForDate(LocalDate date){
        
        String day = NodeUtil.getJour(date);
        return data.getCours().stream().filter(c -> 
                c.getDay().equals(day) && NodeUtil.coursHappensThisDay(c, date)
        ).collect(Collectors.toCollection(FXCollections::observableArrayList));
    }
    
    public ObservableList<Cours> getCoursForDateAndClasse(LocalDate date, Classe classe){
        
        String day = NodeUtil.getJour(date);
        return data.getCours().filtered(c -> 
                c.getDay().equals(day) 
                && c.getClasse().getName().equals(classe.getName())
                && NodeUtil.coursHappensThisDay(c, date)
        );
    }
    
    public ObservableList<Cours> getCoursForDayAndClasse(String day, Classe classe){
        return data.getCours().stream().filter(c -> 
                c.getDay().equals(day) && c.getClasse().getName().equals(classe.getName())
        ).collect(Collectors.toCollection(FXCollections::observableArrayList));
    }
    
    /**
     * Utilisé par la page content
     * @param date
     * @param classe
     * @return 
     */
    public int getNbCoursForDay(LocalDate date, Classe classe){
        final String formattedDay = Constants.DAYMAP.get(date.getDayOfWeek());
        int nbCoursPrevus = getCoursForDayAndClasse(formattedDay, classe).size();
        for(Eleve eleve : classe.getEleves()){
            for(EleveData eleveData : eleve.getData()){
                if(eleveData.getDateAsDate().isEqual(date) ){
                    nbCoursPrevus = Math.max(nbCoursPrevus, eleveData.getCours());
                }
            }
        }
        return nbCoursPrevus;
    }
    
    
    public Cours createCours(Cours newCours){
        data.getCours().add(newCours);
        getJourneesForDay(newCours.getDay()).forEach(j -> {
            addSeanceWithCours(j, newCours);
        });
        newCours.startChangeDetection();
        return newCours;
    }
    
    public void cleanupCours(){
        data.getCours().removeIf(t -> t.getClasse() == null);
    }
    
    /**
     * supprime un cours et crée des cours ponctuels pour les séances existantes
     * renvoie la liste des séances impactées
     * @param cours
     * @return 
     */
    public List<Seance> delete(Cours cours){
        data.getCours().remove(cours);
        return handleSeanceWithDeletedCours(cours);
    }
    
    public Cours cloneCours(Cours sourceCours){
        Cours clone = new Cours();
        clone.setClasse(sourceCours.getClasse());
        clone.setDay(sourceCours.getDay());
        clone.setEndHour(sourceCours.getEndHour());
        clone.setEndMin(sourceCours.getEndMin());
        clone.setStartHour(sourceCours.getStartHour());
        clone.setStartMin(sourceCours.getStartMin());
        clone.setWeek(sourceCours.getWeek());
        clone.setRoom(sourceCours.getRoom());
        clone.setEvent(sourceCours.getEvent());
        return clone;
    }
    
    public void updateCours(Cours dest, Cours source){
        int index = getCours().indexOf(dest);
        getCours().set(index, source);
        getCours().get(index).setChanged(true);
    }
    
    //DATA
    public EleveData createEleveData(Eleve eleve, int cours, LocalDate date){
        EleveData newData = new EleveData();
        newData.setEleve(eleve);
        newData.setCours(cours);
        newData.setDate(date);
        eleve.getData().add(newData);
        newData.startChangeDetection();
        return newData;
    }
    
    public EleveData createEleveData(Eleve eleve, Seance seance){
        EleveData newData = new EleveData();
        newData.setEleve(eleve);
        newData.setSeance(seance);
        seance.getDonneesAsMap().put(eleve, newData);
        newData.startChangeDetection();
        return newData;
    }
    
    
    public EleveData getDataForCoursAndDate(Eleve eleve, int cours, LocalDate date){
        if(eleve == null || eleve.getData() == null){
            return null;
        }
        for(EleveData eleveData : eleve.getData()){
            if(eleveData.getDateAsDate().isEqual(date) && eleveData.getCours() == cours){
                return eleveData;
            }
        }
        return createEleveData(eleve, cours, date);
    }
    
    public List<EleveData> filterDataByTrimestre(List<EleveData> liste, Trimestre trimestre, LocalDate optionalEnDate){
        LocalDate tmp = trimestre.getEndAsDate();
        if(optionalEnDate != null){
            tmp = optionalEnDate;
        }
        final LocalDate endDate = tmp;
        return liste.stream().filter(d -> NodeUtil.isBetween(d.getDateAsDate(), trimestre.getStartAsDate(), endDate))
                .collect(Collectors.toList());
    }
    
    public List<Punition> filterPunitionsByTrimestre(List<Punition> liste, Trimestre trimestre, LocalDate optionalEnDate){
        LocalDate tmp = trimestre.getEndAsDate();
        if(optionalEnDate != null){
            tmp = optionalEnDate;
        }
        final LocalDate endDate = tmp;
        return liste.stream().filter(d -> NodeUtil.isBetween(d.getSeance().getDateAsDate(), trimestre.getStartAsDate(), endDate))
                .collect(Collectors.toList());
    }

    public void cleanupDataForEleve(Eleve eleve){
        eleve.getData().removeIf(eleveData -> eleveData.isEmpty());
    }
    // PUNITIONS
    
    public Punition createPunition(Eleve eleve, Seance seance, String texte) {
        //TODO
        Punition punition = new Punition(eleve, seance, texte);
        eleve.getPunitions().add(punition);
        punition.startChangeDetection();
        return punition;
    }
    
    public Punition createPunition(Eleve eleve, LocalDate date, int cours, String texte) {
        //TODO
        Punition punition = new Punition(eleve, date, cours, texte);
        eleve.getPunitions().add(punition);
        punition.startChangeDetection();
        return punition;
    }
    
    public void deletePunition(Punition punition){
        punition.getEleve().getPunitions().remove(punition);
    }
    
    // JOURNEES
    public List<Journee> getJourneesForDay(String day){
        return data.getJournees().values()
                .stream()
                .filter(j -> day.equals(NodeUtil.getJour(j.getDateAsDate()))).collect(Collectors.toList());
    }
    
    public Journee getJournee(LocalDate date){
        if(getJournees().containsKey(date)){
            return getJournees().get(date);
        }
        return buildJournee(date);
    }
    
    public Journee buildJournee(LocalDate date){
        Journee journee = new Journee();
        List<Cours> cours = getCoursForDate(date);
        cours.forEach(c -> {
            journee.getSeances().add(createSeance(journee, c));
        });
        journee.setDate(date);
        Collections.sort(journee.getSeances());
        declareJournee(journee);
        return journee;
    } 
    
    public void declareJournee(Journee journee){
        data.getJournees().put(journee.getDateAsDate(), journee);
    }
    
    public void cleanUpJournees(){
        data.getJournees().entrySet().forEach(e -> {
            cleanUpJournee(e.getValue());
        });
    }
    public void cleanUpJournee(Journee journee){
        journee.getSeances().forEach(s -> {
            cleanupSeance(s);
        });
    }
    
    // SEANCES
    
    
    public Seance addSeanceWithCoursPonctuel(Journee journee, Classe classe){
        Cours coursPonctuel = new Cours();
        coursPonctuel.setPonctuel(true);
        coursPonctuel.setClasse(classe);
        coursPonctuel.setDay(NodeUtil.getJour(journee.getDateAsDate()));
        coursPonctuel.setWeek("ponctuel");
        coursPonctuel.setStartHour(16);
        coursPonctuel.setStartMin(00);
        coursPonctuel.setEndHour(17);
        coursPonctuel.setEndMin(00);
        return addSeanceWithCoursPonctuel(journee, coursPonctuel);
    }
    
    
    public Seance addSeanceWithCoursPonctuel(Journee journee, Cours cours){
        journee.getCoursPonctuels().add(cours);
        return addSeanceWithCours(journee, cours);
    }
    
    public Seance addSeanceWithCours(Journee journee, Cours cours){
        Seance seance = createSeance(journee, cours);
        journee.getSeances().add(seance);
        Collections.sort(journee.getSeances());
        return seance;
    }
    
    public Seance createSeance(Journee journee, Cours cours){
        Seance seance = new Seance();
        seance.setCours(cours);
        seance.setJournee(journee);
        seance.setClasse(cours.getClasse());
        return seance;
    }
    
    public void cleanupSeance(Seance seance){
        seance.getDonneesAsMap().entrySet().forEach(e -> {
            if(e.getValue().getOubliMateriel() != null){
                e.getValue().setOubliMateriel(e.getValue().getOubliMateriel().trim());
            }
            if(e.getValue().getMotif() != null){
                e.getValue().setMotif(e.getValue().getMotif().trim());
            }
        });
        seance.getDonneesAsMap().entrySet().removeIf(e -> 
                e == null || e.getKey() == null ||
                e.getValue() == null || e.getValue().isEmpty());
    }
    
    public List<Seance> handleSeanceWithDeletedCours(Cours cours){
        Stream<Seance> seances = data.getJournees().values().stream().flatMap(j -> j.getSeances().stream());
        List<Seance> seancesModifiees = new ArrayList<>();
        seances.forEach(s -> {
            if(s.getCours().getId().equals(cours.getId())){
                Cours coursPonctuel = cloneCours(cours);
                coursPonctuel.setPonctuel(true);
                s.setCours(coursPonctuel);
                s.getJournee().getCoursPonctuels().add(coursPonctuel);
                seancesModifiees.add(s);
            }
        });
        return seancesModifiees;
    }
}
