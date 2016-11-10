/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mesclasses.view;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import mesclasses.controller.PageController;
import org.apache.commons.lang3.tuple.Pair;

/**
 * FXML Controller class
 *
 * @author rrrt3491
 */
public class ConfigurationController extends PageController implements Initializable {

    @FXML TabPane tabPane;
    
    @FXML Tab semainesTab;
    @FXML AnchorPane configSemaines;
    @FXML ConfigSemainesController configSemainesController;
    
    ChangeListener<Tab> selectionListener;
    PageController selectedController;
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        name = "Configuration ctrl";
        super.initialize(url, rb);
        tabPane.getSelectionModel().select(semainesTab);
        selectedController = configSemainesController;
        selectionListener = new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
                
            }
        };
        tabPane.getSelectionModel().selectedItemProperty().addListener(selectionListener);
    }

    private void initTab(Tab tab, Node root, PageController controller){
        tab.setUserData(Pair.of(root, controller));
    }    
    
    @Override
    public void reload(){
        super.reload();
    }
    
    @Override
    public boolean notifyExit(){
        
        return configSemainesController.notifyExit();
    }
}