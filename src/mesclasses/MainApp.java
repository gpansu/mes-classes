/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mesclasses;

import java.io.IOException;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import mesclasses.model.Constants;
import mesclasses.objects.LoadWindow;
import mesclasses.objects.tasks.ComputeTask;
import mesclasses.objects.tasks.FetchConfigTask;
import mesclasses.objects.tasks.FetchDataTask;
import mesclasses.util.AppLogger;
import mesclasses.util.FileSaveUtil;
import mesclasses.view.RootLayoutController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author rrrt3491
 */
public class MainApp extends Application {
    
    private static final Logger LOG = LogManager.getLogger(MainApp.class);
    
    private Stage primaryStage;
    private AnchorPane rootLayout;
     
    @Override
    public void start(Stage primaryStage) throws InterruptedException {
        AppLogger.logStart(MainApp.class);
        handleParams();
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(Constants.APPLICATION_TITLE);
        this.primaryStage.getIcons().add(new Image(
            MainApp.class.getResourceAsStream( "/resources/package/windows/MesClasses.png" ))); 
        MainApp.class.getResourceAsStream("/resources/fonts/fontawesome-webfont.ttf");
        
        Platform.setImplicitExit(false);
        LoadWindow loading = new LoadWindow(this.primaryStage, new FetchDataTask(), new FetchConfigTask(), new ComputeTask());
        loading.startAndWait();
        if(loading.isSuccessful()){
            initRootLayout();
        } else {
            AppLogger.logExit(MainApp.class);
            System.exit(0);
        }
    }
    
    /**
     * initialise le root layout, qui contient uniquement la barre de menus
     */
    
    public void initRootLayout() {
        try {
            // Load root layout from fxml file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
            rootLayout = loader.load();
            RootLayoutController rootController = loader.getController();
            rootController.setPrimaryStage(primaryStage);
            // Show the scene containing the root layout.
            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            scene.getStylesheets().add(MainApp.class.getResource(Constants.DEFAULT_EVENT_CSS).toExternalForm());
            primaryStage.show();
        } catch (IOException e) {
            LOG.error(e);
        }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    private void handleParams() {
        if(getParameters() == null || getParameters().getRaw().isEmpty()){
            LOG.info("no additional paramaters sent with application startup");
        } else {
            List<String> params = getParameters().getRaw();
            if(params.size() != 2){
                LOG.error(STYLESHEET_CASPIAN);
                LOG.error(params.size()+" paramaters sent with application startup, which is wrong. Should be 2. Default conf used");
            }
            String dataDir = params.get(0);
            String dataFile = params.get(1);
            FileSaveUtil.set(dataDir, dataFile);
        }
        LOG.info("**** CONFIGURATION ****");
        LOG.info(FileSaveUtil.displayConf());
        LOG.info("***********************");
    }
    
}
