/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mesclasses.model;

import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;

/**
 *
 * @author rrrt3491
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Journee extends MonitoredObject implements Comparable<Journee> {
    
    private LocalDate date;
    private ObservableList<Cours> coursPonctuels = FXCollections.observableArrayList();
    private ObservableList<Seance> seances = FXCollections.observableArrayList();

    @Override
    public void startChangeDetection() {
        coursPonctuels.addListener(listAddRemoveListener);
        coursPonctuels.forEach(c -> c.startChangeDetection());
        seances.addListener(listAddRemoveListener);
        coursPonctuels.forEach(c -> c.startChangeDetection());
    }

    @XmlID
    @XmlAttribute
    public String getDate() {
        return date.format(Constants.DATE_FORMATTER);
    }
    
    public LocalDate getDateAsDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @XmlElement(name="coursPonctuels")
    @XmlElementWrapper(name="coursPonctuel")
    public ObservableList<Cours> getCoursPonctuels() {
        return coursPonctuels;
    }

    public void setCoursPonctuels(ObservableList<Cours> coursPonctuels) {
        this.coursPonctuels = coursPonctuels;
    }

    @XmlElement(name="seances")
    @XmlElementWrapper(name="seance")
    public ObservableList<Seance> getSeances() {
        return seances;
    }

    public void setSeances(ObservableList<Seance> seances) {
        this.seances = seances;
    }
    
    @Override
    public int compareTo(Journee t) {
        return date.compareTo(t.getDateAsDate());
    }
    
    
}
