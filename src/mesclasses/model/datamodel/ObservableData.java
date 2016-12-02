/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mesclasses.model.datamodel;

import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import mesclasses.model.Classe;
import mesclasses.model.Cours;
import mesclasses.model.Journee;
import mesclasses.model.MonitoredObject;
import mesclasses.model.Trimestre;

/**
 *
 * @author rrrt3491
 */
public class ObservableData extends MonitoredObject {
    
    private ObservableList<Trimestre> trimestres = FXCollections.observableArrayList();
    
    private ObservableList<Classe> classes = FXCollections.observableArrayList();

    private ObservableList<Cours> cours = FXCollections.observableArrayList();
    
    private ObservableMap<LocalDate, Journee> journees = FXCollections.observableHashMap();
    
    @Override
    public void startChangeDetection() {
        classes.addListener(listAddRemoveSortListener);
        trimestres.addListener(listAddRemoveListener);
        cours.addListener(listAddRemoveListener);
        classes.forEach(c -> c.startChangeDetection());
        trimestres.forEach(c -> c.startChangeDetection());
        cours.forEach(c -> c.startChangeDetection());
        journees.values().forEach(j -> j.startChangeDetection());
    }
    
    @Override
    public void resetChange(){
        super.resetChange();
        classes.forEach(c -> c.resetChange());
        trimestres.forEach(c -> c.resetChange());
        cours.forEach(c -> c.resetChange());
        journees.values().forEach(j -> j.resetChange());
        
    }
    
    public ObservableList<Trimestre> getTrimestres() {
        return trimestres;
    }

    public void setTrimestres(ObservableList<Trimestre> trimestres) {
        this.trimestres = trimestres;
    }

    public ObservableList<Classe> getClasses() {
        return classes;
    }

    public void setClasses(ObservableList<Classe> classes) {
        this.classes = classes;
    }

    public ObservableList<Cours> getCours() {
        return cours;
    }

    public void setCours(ObservableList<Cours> cours) {
        this.cours = cours;
    }

    public ObservableMap<LocalDate, Journee> getJournees() {
        return journees;
    }

    public void setJournees(ObservableMap<LocalDate, Journee> journees) {
        this.journees = journees;
    }
    
}
