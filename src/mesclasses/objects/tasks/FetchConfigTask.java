/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mesclasses.objects.tasks;

import java.util.logging.Level;
import java.util.logging.Logger;
import mesclasses.handlers.PropertiesCache;
import mesclasses.util.DataLoadUtil;
import mesclasses.util.ModalUtil;

/**
 *
 * @author rrrt3491
 */
public class FetchConfigTask extends AppTask<Void> {
    
    public FetchConfigTask(){
        super();
        updateProgress(0, 100.0);
    }
    
    @Override 
    public String getName(){
        return "Configuration";
    }
    
    @Override
    protected Void call() throws Exception {
        try {
            PropertiesCache.getInstance().load();
        } catch (Exception e) { // catches ANY exception
            Logger.getLogger(DataLoadUtil.class.getName()).log(Level.SEVERE, null, e);
            ModalUtil.alert("Impossible de charger la configuration", 
                    "Le fichier de properties n'est pas lisible");
        }
        return null;
    }
}