/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mesclasses.view;

import com.google.common.eventbus.Subscribe;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import mesclasses.controller.PageController;
import mesclasses.handlers.EventBusHandler;
import mesclasses.handlers.ModelHandler;
import mesclasses.handlers.PropertiesCache;
import mesclasses.model.Constants;
import mesclasses.model.datamodel.ObservableData;
import mesclasses.model.datamodel.XMLData;
import mesclasses.objects.Screen;
import mesclasses.objects.events.ChangeEvent;
import mesclasses.objects.events.IsAliveEvent;
import mesclasses.objects.events.MessageEvent;
import mesclasses.objects.events.OpenMenuEvent;
import mesclasses.util.CssUtil;
import mesclasses.util.DataLoadUtil;
import mesclasses.util.EleveFileUtil;
import mesclasses.util.FileSaveUtil;
import mesclasses.util.ModalUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author rrrt3491
 */
public class RootLayoutController extends PageController implements Initializable {
    
    @FXML private BorderPane mainPane;
    
    @FXML private StackPane notificationPane;
    @FXML private Button deleteNotifBtn;
    
    @FXML private Label notificationTitleLabel;
    @FXML private Label notificationMessageLabel;
    
    private PageController currentController;
    
    private Map<String, Screen> screenMap;
    
    private boolean saveNeeded;
    
    @FXML
    public void openAccueil(ActionEvent event){
        loadView(Constants.ACCUEIL_VIEW, true);
    }
    
    @FXML
    public void openAdminTrimestre(ActionEvent event){
        loadView(Constants.ADMIN_TRIMESTRE_VIEW, true);
    }
    
    @FXML
    public void openAdminClasse(ActionEvent event){
        loadView(Constants.ADMIN_CLASSE_VIEW, true);
    }
    
    @FXML
    public void openAdminEleve(ActionEvent event){
        loadView(Constants.ADMIN_ELEVE_VIEW, true);
    }
    
    @FXML
    public void openTimetable(ActionEvent event){
        loadView(Constants.EMPLOI_DU_TEMPS_VIEW, true);
    }
    @FXML
    public void openClasses(ActionEvent event){
        loadView(Constants.CLASSE_CONTENT_TABS_VIEW, true);
    }
    
    @FXML
    public void openRapports(ActionEvent event){
        loadView(Constants.CLASSE_RAPPORT_TABS_VIEW, true);
    }
    
    @FXML
    public void openHistorique(ActionEvent event){
        loadView(Constants.HISTORIQUE_VIEW, true);
    }
    
    @Subscribe
    public void openMenu(OpenMenuEvent event){
        logEvent(event);
        if(loadView(event.getView(), event.isReload())){
            currentController.setPreviousPage(event.getFromView());
        }
    }
    
    @Subscribe
    public void detectChange(ChangeEvent event){
        logEvent(event);
        primaryStage.setTitle(Constants.APPLICATION_TITLE+" *");
        saveNeeded = true;
    }
    
    @Override 
    public void setPrimaryStage(Stage primaryStage){
        this.primaryStage = primaryStage;
        this.primaryStage.setOnCloseRequest(ev -> {
            ev.consume();
            handleExit();
        });
    }
    
    private boolean loadView(String view, boolean reload){
        try {
            if(currentController != null){
                if(!currentController.notifyExit()){
                    return false;
                }
            }
            log("Loading view "+view);
            Screen screen = getScreen(view);
            if(screen == null || screen.getCtrl() == null){

                log("screen is null");
                return false;
            }
            mainPane.setCenter(screen.getRoot());
            if(reload){
                screen.getCtrl().reload();
            }
            currentController = screen.getCtrl();
            return true;
        } catch(Exception e){
            notif(e);
            return false;
        }
    }
    
    private Screen getScreen(String view){
        Screen screen = screenMap.get(view);
        if(screen == null){
            screen = new Screen(view, primaryStage);
            screenMap.put(view, screen);
        }
        return screen;
    }
    
    /**
     * Saves the data
     */
    @FXML
    public void handleSave() {
        if(currentController != null && !currentController.notifySave()){
            return;
        }
        save();
        EventBusHandler.post(MessageEvent.success("Données sauvegardées"));
        modelHandler.getData().resetChange();
        primaryStage.setTitle(Constants.APPLICATION_TITLE);
        saveNeeded = false;
        if(currentController != null){
            currentController.reload();
        }
    }
    
    private void save(){
        XMLData xmlData = new XMLData();
        Collections.sort(trimestres);
        modelHandler.cleanupClasses();
        xmlData.getTrimestres().addAll(trimestres);
        xmlData.getClasses().addAll(classes);
        xmlData.getCours().addAll(cours);
        DataLoadUtil.writeData(xmlData, FileSaveUtil.getSaveFile());
    }
    
    @FXML
    public void handleLoad(){
        FileChooser chooser =new FileChooser();
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers de sauvegarde", "*.xml"));
        chooser.setTitle("Sélectionnez un fichier de sauvegarde");
        chooser.setInitialDirectory(new File(FileSaveUtil.BCK_DIR));
        File file = chooser.showOpenDialog(primaryStage);
        if(file == null){
            return;
        }
        if(DataLoadUtil.isfileEmpty(file)){
            ModalUtil.alert("Impossible de lire les données du fichier", "Ce fichier est vide");
            return;
        }
        ObservableData data;
        try {
            data = DataLoadUtil.initializeData(file);
        } catch(Exception e){
            ModalUtil.alert("Impossible de lire les données du fichier", 
                    "Ce fichier n'est pas un fichier de sauvegarde "+Constants.APPLICATION_TITLE+" valide");
            return;
        }
        if(ModalUtil.confirm("Charger le fichier "+file.getName(), "Les données actuelles seront perdues")){
            FileSaveUtil.restoreBackupFile(file);
            loadData(data);
        }
    }
    
    @Subscribe
    public void onMessage(MessageEvent event){
            displayNotification(event.getType(), event.getMessage());
    }
    
    private void displayNotification(int type, String texte){
        notificationMessageLabel.setText(texte);
        notificationPane.getStyleClass().clear();
        CssUtil.removeClass(deleteNotifBtn, "notif-warning");
        CssUtil.removeClass(deleteNotifBtn, "notif-error");
        Double timeDisplayed = 3.0D;
        switch(type){
            case MessageEvent.SUCCESS :
                CssUtil.addClass(notificationPane, "notif-success");
                deleteNotifBtn.setManaged(false);
                deleteNotifBtn.setVisible(false);
                notificationTitleLabel.setText("SUCCES");
                break;
            case MessageEvent.WARNING :
                CssUtil.addClass(notificationPane, "notif-warning");
                CssUtil.addClass(deleteNotifBtn, "notif-warning");
                notificationTitleLabel.setText("ATTENTION");
                break;
            case MessageEvent.ERROR :
                CssUtil.addClass(notificationPane, "notif-error");
                CssUtil.addClass(deleteNotifBtn, "notif-error");
                notificationTitleLabel.setText("ERREUR");
                timeDisplayed = 0.0;
                break;
        }
        notificationPane.setManaged(true);
        notificationPane.setVisible(true);
        
        if(timeDisplayed > 0.0){
            FadeTransition ft = new FadeTransition(Duration.millis(150), notificationPane);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.setCycleCount(1);
            ft.play();
            Timeline timeline = new Timeline();
            timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(timeDisplayed), (ActionEvent event) -> {
                FadeTransition ft2 = new FadeTransition(Duration.millis(150), notificationPane);
                ft2.setFromValue(1.0);
                ft2.setToValue(0.0);
                ft2.setCycleCount(1);
                ft2.play();
                ft2.setOnFinished((ActionEvent event1) -> {
                    notificationPane.setManaged(false);
                    notificationPane.setVisible(false);
                });

            }));
            timeline.play();
        }
    }
    
    @FXML public void closeNotif(){
        notificationPane.setManaged(false);
        notificationPane.setVisible(false);
    }
    
    @FXML
    private void handleExit() {
        if(canExit()){
            PropertiesCache.getInstance().save();
            System.exit(0);
        }
    }
    
    private boolean canExit(){
        if(saveNeeded){
            String res = ModalUtil.yesNoCancel("Des modifications n'ont pas été sauvegardées", "Voulez-vous sauvegarder avant de quitter ?");
            if("cancel".equals(res)){
                return false;
            }
            if("yes".equals(res)){
                try {
                    handleSave();
                } catch(Exception e){
                    notif(e);
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        name = "Root Layout Ctrl";
        
        notificationPane.setManaged(false);
        notificationPane.setVisible(false);
        notificationPane.setOnMouseClicked((MouseEvent mouseEvent) -> {
            if(mouseEvent.getButton().equals(MouseButton.PRIMARY)){
                closeNotif();
            }
        });
        FileSaveUtil.createBackupFile();
        screenMap = new HashMap<>();
        try {
            ObservableData data = DataLoadUtil.initializeData(FileSaveUtil.getSaveFile());
            log("Loading data with classes "+data.getClasses());
            loadData(data);
        } catch (Exception e) { // catches ANY exception
            Logger.getLogger(DataLoadUtil.class.getName()).log(Level.SEVERE, null, e);
            ModalUtil.alert("Impossible de charger les données", 
                    "Le fichier "+FileSaveUtil.getSaveFile().getPath()+" n'est pas lisible");
            return;
        }
        
        super.initialize(location, resources);
    }
    
    
    private void loadData(ObservableData data){
        ModelHandler.getInstance().injectData(data);
        data.startChangeDetection();
        screenMap.values().forEach(screen -> {
            screen.stop();
        });
        screenMap.clear();
        openAccueil(null);
    }
    
    @Override
    public boolean notifyExit() {
        return true;
    }
    
    @FXML public void onReset(){
        if(ModalUtil.confirm("Réinitialisation des données", "Etes-vous sûr(e) ?")){
            try {
                FileSaveUtil.archive();
                EleveFileUtil.purge();
            } catch(IOException e){
                notif(e);
                return;
            }
            loadData(new ObservableData());
            save();
        }
    }
    @FXML public void onExport(){
        File archive;
        try {
            archive = FileSaveUtil.archive();
        } catch(IOException e){
            notif(e);
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sauvegardez les données");
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Archive MesClasses", "*.zip"));
        log(archive.getPath());
        chooser.setInitialFileName(FilenameUtils.getName(archive.getPath()));
        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                FileUtils.moveFile(archive, file);
            } catch (IOException ex) {
                notif(ex);
            }
            displayNotification(MessageEvent.SUCCESS, "Données sauvegardées");
        }
    
    }
    @FXML public void onImport(){
        if(ModalUtil.confirm("Importer des données", "Cette opération écrasera les données existantes.\nEtes-vous sûr(e) ?")){
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Archive MesClasses", "*.zip"));
            chooser.setTitle("Sélectionnez une archive");
            chooser.setInitialDirectory(new File(FileSaveUtil.ARCHIVE_DIR));
            File file = chooser.showOpenDialog(primaryStage);
            if(file == null){
                return;
            }
            try {
                FileSaveUtil.restoreArchive(file);
            } catch(IOException e){
                notif(e);
                return;
            }
            try {
                ObservableData data = DataLoadUtil.initializeData(FileSaveUtil.getSaveFile());
                loadData(data);
            } catch(Exception e){
                notif(e);
            }
        }
    }
    
    @FXML public void isAlive(){
        EventBusHandler.post(new IsAliveEvent());
    }
}