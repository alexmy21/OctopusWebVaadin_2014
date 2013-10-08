/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lisapark.octopusweb;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.openide.util.Exceptions;

/**
 *
 * @author alexmy
 */
public class MRUI extends UI {
    
    static String TREE_VISIBLE      = "TREE_VISIBLE";
    static String MODEL_TREE_FIELDS = "MODEL_TREE_FIELDS";
    static String MODEL_JSON        = "MODEL_JSON";
    static final String MODEL_NAME  = "MODEL_NAME";
    
    static String JETTY_SEARCH_URL  = "http://10.1.10.10:8084/search/search";
    static String JETTY_RUN_URL     = "http://10.1.10.10:8084/run/run";
    
    static String MODEL_TREE_LAYOUT = "MODEL_TREE_LAYOUT";
    
    static String SOURCE            = "SOURCE";
    static String SINK              = "SINK";
    static String PROCESSOR         = "PROCESSOR";
    static String PROC_TYPE         = "PROC_TYPE";
    static String PROC_NAME_VALUE   = "PROC_NAME_VALUE";
    static String PROC_GROUP_NAME   = "PROCESSORS";
    static String SINK_GROUP_NAME   = "SINKS";
    static String SOURCE_GROUP_NAME = "SOURCES";
    static String OCTOPUS_PROPERTIS = "octopus.properties";

    static String MODEL_NAME_PARAM  = "MODEL_NAME_PARAM";
    static String JSON_NAME_PARAM   = "JSON_NAME_PARAMs";
    
    @Override
    protected void init(VaadinRequest request) {
        try {
            
            Properties properties = parseProperties(OCTOPUS_PROPERTIS);
            
            VaadinSession.getCurrent().setAttribute(MODEL_NAME_PARAM, properties.getProperty("model.name.param"));
            VaadinSession.getCurrent().setAttribute(JSON_NAME_PARAM, properties.getProperty("model.json.param"));
            
            initLayout();
            
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void initLayout() {

        /* Root of the user interface component tree is set */
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setSplitPosition(30, Unit.PERCENTAGE);

        setContent(splitPanel);

        VerticalLayout leftLayout = new ModelListLayout();
        
        VaadinSession.getCurrent().setAttribute(MRUI.TREE_VISIBLE, Boolean.TRUE);
        
        if(VaadinSession.getCurrent().getAttribute(MRUI.MODEL_TREE_LAYOUT) == null){
            VaadinSession.getCurrent().setAttribute(MRUI.MODEL_TREE_LAYOUT, 
                    ModelTreeLayout.initModelTreeLayout(null));
        }
        
        VerticalLayout rightLayout = (VerticalLayout) VaadinSession.getCurrent().getAttribute(MRUI.MODEL_TREE_LAYOUT);

        splitPanel.addComponent(leftLayout);
        splitPanel.addComponent(rightLayout);

        leftLayout.setSizeFull();

        /* Put a little margin around the fields in the right side editor */
        rightLayout.setMargin(true);
        rightLayout.setSizeFull();
        
//        rightLayout.setVisible(false);
    }
    
    private static Properties parseProperties(String propertyFileName) throws IOException {
        InputStream fin = null;
        Properties properties = null;
        try {
            fin = MRUI.class.getResourceAsStream("/" + propertyFileName);

            properties = new Properties();
            properties.load(fin);

        } finally {
            IOUtils.closeQuietly(fin);
        }

        return properties;
    }
}
