/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mesclasses.objects.tasks;

import java.util.logging.Level;
import java.util.logging.Logger;
import mesclasses.handlers.ModelHandler;
import mesclasses.model.datamodel.ObservableData;
import mesclasses.util.AppLogger;
import mesclasses.util.DataLoadUtil;
import mesclasses.util.FileSaveUtil;
import mesclasses.util.ModalUtil;

/**
 *
 * @author rrrt3491
 */
public class FetchDataTask extends AppTask<ObservableData> {
    
    public FetchDataTask(){
        super();
        updateProgress(0, 100.0);
    }
    
    @Override 
    public String getName(){
        return "Chargement des données";
    }
    
    @Override
    protected ObservableData call() throws Exception {
        try {
            long start = System.currentTimeMillis();
            ObservableData data = DataLoadUtil.initializeData(FileSaveUtil.getSaveFile());
            long end = System.currentTimeMillis();
            if(end-start < 300){
                Thread.sleep(300-end+start);
            }
            updateProgress(33.0, 100.0);
            loadData(data);
            AppLogger.log("data fetch done");
            return data;
        } catch (Exception e) { // catches ANY exception
            Logger.getLogger(DataLoadUtil.class.getName()).log(Level.SEVERE, null, e);
            ModalUtil.alert("Impossible de charger les données", 
                    "Le fichier "+FileSaveUtil.getSaveFile().getPath()+" n'est pas lisible");
            return null;
        }
    }
    
    private void loadData(ObservableData data) throws InterruptedException{
        long start = System.currentTimeMillis();
        ModelHandler.getInstance().injectData(data);
        long end1 = System.currentTimeMillis();
        if(end1-start < 300){
            Thread.sleep(300-end1+start);
        }
        updateProgress(66.0, 100.0);
        data.startChangeDetection();
        long end2 = System.currentTimeMillis();
        if(end2-end1 < 300){
            Thread.sleep(300-end2+end1);
        }
        updateProgress(100.0, 100.0);
    }
}