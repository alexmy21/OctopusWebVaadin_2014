/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lisapark.octopusweb;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vaadin.data.Container;
import com.vaadin.data.util.ContainerHierarchicalWrapper;
import com.vaadin.data.util.HierarchicalContainerOrderedWrapper;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.lisapark.octopus.core.ModelBean;
import org.lisapark.octopus.core.ProcessorBean;
import org.openide.util.Exceptions;

/**
 *
 * @author alexmy
 */
public class ModelTreeLayout extends VerticalLayout {

    private static String PROCESSOR_NAME = "Tree Item";
    private static String PARAM_VALUE = "Param Value";
    private static String UPDATE_BUTTON = "update";
    private Button runButton;
    private TreeTable treeTable;
    private VerticalLayout treeLayout;
    private HorizontalLayout buttonLayout;

    // Non public constructor
    //==========================================================================
    ModelTreeLayout() {
        
        super();

        runButton = new Button("run selected model");
        
        runButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                
                String json = VaadinSession.getCurrent().getAttribute(MRUI.MODEL_JSON) == null
                        ? null : (String) VaadinSession.getCurrent().getAttribute(MRUI.MODEL_JSON);

                if (json == null) {
                    Notification.show("Model JSON cannot be NULL.");
                    return;
                }

                ModelBean modelBean = new Gson().fromJson(json, ModelBean.class);
                try {
                    String modelname = (String) VaadinSession.getCurrent().getAttribute(MRUI.MODEL_NAME_PARAM);
                    String modeljson = (String) VaadinSession.getCurrent().getAttribute(MRUI.JSON_NAME_PARAM);

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(modelname, modelBean.getModelName());
                    jsonObject.put(modeljson, json);

                    HttpClient client = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(MRUI.JETTY_RUN_URL);

                    httpPost.setHeader("id", modelBean.getModelName());
                    httpPost.setHeader("name", modelBean.getModelName());

                    httpPost.setHeader("Content-Type", "application/json");
                    StringEntity entity = new StringEntity(jsonObject.toString(), HTTP.UTF_8);
                    httpPost.setEntity(entity);

                    HttpResponse httpResponse = client.execute(httpPost);

                    System.out.println("HTTP response: " + httpResponse);

                    Notification.show("Model: " + modelBean.getModelName() + " - finished it's Run successfuly.");

                } catch (UnsupportedEncodingException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (JSONException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });

        treeTable = new TreeTable();

        treeTable.setSelectable(false);
        treeTable.setEditable(true);
        treeTable.setImmediate(true);
        treeTable.setSizeFull();
        treeTable.setWidth("100%");

        buttonLayout = new HorizontalLayout();
        buttonLayout.addComponent(runButton);

        treeLayout = new VerticalLayout();
        treeLayout.addComponent(treeTable);

        this.addComponent(buttonLayout);
        this.addComponent(treeLayout);

    }

    static VerticalLayout initModelTreeLayout(String json) {

        System.out.println("initModelTreeLayout");
        System.out.println(json);

        ModelTreeLayout modelTreeLayout;

        if (VaadinSession.getCurrent().getAttribute(MRUI.MODEL_TREE_LAYOUT) == null) {
            modelTreeLayout = new ModelTreeLayout();
            VaadinSession.getCurrent().setAttribute(MRUI.MODEL_TREE_LAYOUT, modelTreeLayout);
        } else {
            modelTreeLayout = (ModelTreeLayout) VaadinSession.getCurrent().getAttribute(MRUI.MODEL_TREE_LAYOUT);
        }

        //modelTreeLayout.getRunButton()
        modelTreeLayout.getRunButton().setImmediate(true);

        modelTreeLayout.getButtonLayout().addComponent(modelTreeLayout.getRunButton());
//        modelTreeLayout.getTreeLayout().addComponent(modelTreeLayout.getButtonLayout()); 

        ContainerHierarchicalWrapper containerHierarchicalWrapper = new ContainerHierarchicalWrapper(
                new IndexedContainer());
        HierarchicalContainerOrderedWrapper hc = new HierarchicalContainerOrderedWrapper(
                containerHierarchicalWrapper);

        // First remove all components
        modelTreeLayout.getTreeLayout().removeAllComponents();
        modelTreeLayout.getTreeTable().setContainerDataSource(hc);

        modelTreeLayout.addModelData(hc, json);


        modelTreeLayout.getTreeTable().setVisibleColumns((Object[]) new String[]{PROCESSOR_NAME, PARAM_VALUE, UPDATE_BUTTON});
        modelTreeLayout.getTreeTable().setSizeFull();
        modelTreeLayout.getTreeTable().setWidth("100%");

        // And then add new treeTable
        modelTreeLayout.getTreeLayout().setSizeFull();
        modelTreeLayout.getTreeLayout().setHeight("100%");
        modelTreeLayout.getTreeLayout().setMargin(true);

        modelTreeLayout.getTreeLayout().addComponent(modelTreeLayout.getTreeTable());

        modelTreeLayout.setExpandRatio(modelTreeLayout.getTreeLayout(), 1);

        return modelTreeLayout;
    }

    private void addModelData(HierarchicalContainerOrderedWrapper hc, String json) {

        System.out.println("addModelData");
        System.out.println(json);

        hc.addContainerProperty(PROCESSOR_NAME, String.class, "");
        hc.addContainerProperty(PARAM_VALUE, String.class, "");
        hc.addContainerProperty(UPDATE_BUTTON, Button.class, "");
        hc.addContainerProperty(MRUI.PROC_TYPE, String.class, "");
        hc.addContainerProperty(MRUI.PROC_NAME_VALUE, String.class, "");

        if (json == null || json.isEmpty()) {
            return;
        }

        getTreeTable().setTableFieldFactory(new TableFieldFactory() {
            @Override
            public Field createField(Container container, Object itemId,
                    Object propertyId, Component uiContext) {

                Container.Hierarchical hc = getTreeTable().getContainerDataSource();

                TextField field = new TextField((String) propertyId);
                field.setSizeFull();
                field.setWidth("100%");

                // If you want to disable edition on a column, use ReadOnly
                if (PARAM_VALUE.equals(propertyId) && !hc.hasChildren(itemId)) {
                    field.setData(itemId);                    
                } else {
                    field.setReadOnly(true);
                }

                field.setImmediate(true);

                return field;
            }
        });

        createModelTree(hc, json);
    }

    /**
     *
     * @param hc
     * @param json
     */
    private void createModelTree(Container.Hierarchical hc, String json) {

        ModelBean modelBean = new Gson().fromJson(json, ModelBean.class);

        // Extract all sinks
        Set<String> sources = modelBean.getSources();

        // Populate Source Group
        Object id;
        Object sourceGroupId = hc.addItem();
        for (String source : sources) {

            ProcessorBean procBean = new Gson().fromJson(source, ProcessorBean.class);
            id = hc.addItem();

            String procName = procBean.getName();
            hc.getItem(id).getItemProperty(PROCESSOR_NAME).setValue(procBean.getName());

            // Get all source's params
            Map<String, Object> params = procBean.getParams();            
            
            if (params.isEmpty()) {
                hc.getItem(id).getItemProperty(PARAM_VALUE).setReadOnly(true);
                hc.setChildrenAllowed(id, false);
            } else {

                for (Map.Entry<String, Object> param : params.entrySet()) {
                    Object paramId = hc.addItem();
                    hc.getItem(paramId).getItemProperty(PROCESSOR_NAME).setValue(param.getKey());
                    hc.getItem(paramId).getItemProperty(PARAM_VALUE).setValue(convert2string(param.getValue()));

                    hc.getItem(paramId).getItemProperty(UPDATE_BUTTON)
                            .setValue(updateButton(MRUI.SOURCE, paramId, procName, param.getKey()));

                    hc.getItem(paramId).getItemProperty(MRUI.PROC_TYPE).setValue(MRUI.SOURCE);
                    hc.getItem(paramId).getItemProperty(MRUI.PROC_NAME_VALUE).setValue(procName);
                    hc.setParent(paramId, id);
                    hc.setChildrenAllowed(paramId, false);
                }
            }
            
            hc.setParent(id, sourceGroupId);
        }
        
        hc.getItem(sourceGroupId).getItemProperty(PROCESSOR_NAME).setValue(MRUI.SOURCE_GROUP_NAME);

        // Populate Processor's Group
        // Extract all Procs
        Set<String> procs = modelBean.getProcessors();

        Object procGroupId = hc.addItem();
        
        for (String proc : procs) {

            ProcessorBean procBean = new Gson().fromJson(proc, ProcessorBean.class);
            id = hc.addItem();

            String procName = procBean.getName();
            hc.getItem(id).getItemProperty(PROCESSOR_NAME).setValue(procName);

            // Get all proc's params
            Map<String, Object> params = procBean.getParams();            
            
            if (params.isEmpty()) {
                hc.getItem(id).getItemProperty(PARAM_VALUE).setReadOnly(true);
                hc.setChildrenAllowed(id, false);

            } else {

                for (Map.Entry<String, Object> param : params.entrySet()) {
                    Object paramId = hc.addItem();
                    hc.getItem(paramId).getItemProperty(PROCESSOR_NAME)
                            .setValue(param.getKey());
                    hc.getItem(paramId).getItemProperty(PARAM_VALUE)
                            .setValue(convert2string(param.getValue()));

                    hc.getItem(paramId).getItemProperty(UPDATE_BUTTON)
                            .setValue(updateButton(MRUI.PROCESSOR, paramId, procName, param.getKey()));

                    hc.getItem(paramId).getItemProperty(MRUI.PROC_TYPE).setValue(MRUI.PROCESSOR);
                    hc.getItem(paramId).getItemProperty(MRUI.PROC_NAME_VALUE).setValue(procName);
                    hc.setParent(paramId, id);
                    hc.setChildrenAllowed(paramId, false);
                }
            }
            
            hc.setParent(id, procGroupId);
        }
        
        if(procs.isEmpty()){
            hc.getItem(procGroupId).getItemProperty(PARAM_VALUE).setReadOnly(true);
            hc.setChildrenAllowed(procGroupId, false);
        } 
            
        hc.getItem(procGroupId).getItemProperty(PROCESSOR_NAME).setValue(MRUI.PROC_GROUP_NAME);        

        // Populate Sink's Group
        // Extract all Sinks
        Set<String> sinks = modelBean.getSinks();

        Object sinkGroupId = hc.addItem();
        for (String sink : sinks) {

            ProcessorBean procBean = new Gson().fromJson(sink, ProcessorBean.class);
            id = hc.addItem();

            String procName = procBean.getName();
            hc.getItem(id).getItemProperty(PROCESSOR_NAME).setValue(procBean.getName());

            // Get all sink's params
            Map<String, Object> params = procBean.getParams();
            
            if (params.isEmpty()) {
                hc.getItem(id).getItemProperty(PARAM_VALUE).setReadOnly(true);
                hc.setChildrenAllowed(id, false);                
            } else {
                
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    Object paramId = hc.addItem();
                    hc.getItem(paramId).getItemProperty(PROCESSOR_NAME).setValue(param.getKey());
                    hc.getItem(paramId).getItemProperty(PARAM_VALUE).setValue(convert2string(param.getValue()));

                    hc.getItem(paramId).getItemProperty(UPDATE_BUTTON)
                            .setValue(updateButton(MRUI.SINK, paramId, procName, param.getKey()));

                    hc.getItem(paramId).getItemProperty(MRUI.PROC_TYPE).setValue(MRUI.SINK);
                    hc.getItem(paramId).getItemProperty(MRUI.PROC_NAME_VALUE).setValue(procName);
                    hc.setParent(paramId, id);
                    hc.setChildrenAllowed(paramId, false);
                }
            }
            
            hc.setParent(id, sinkGroupId);
        }
        
        hc.getItem(sinkGroupId).getItemProperty(PROCESSOR_NAME).setValue(MRUI.SINK_GROUP_NAME);
        
    }    

    private String convert2string(Object value) {
        String retString;

        if (value != null && !(value instanceof String)) {
            retString = value.toString();
        } else {
            retString = (String) value;
        }

        return retString;
    }

    private Button updateButton(final String procType, final Object procId, final String procName,
            final String paramKey) {

        Button button = new NativeButton();

        button.setCaption("update");
        button.setImmediate(true);

        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {

                String paramValue = (String) getTreeTable().getItem(procId)
                        .getItemProperty(PARAM_VALUE).getValue();

                System.out.println("Button clicked: " + procType
                        + "; procId: " + procId
                        + "; procName: " + procName
                        + "; paramKey: " + paramKey
                        + "; paramValue: " + paramValue);

                VaadinSession.getCurrent().setAttribute(MRUI.TREE_VISIBLE, Boolean.TRUE);

                String json = (String) VaadinSession.getCurrent().getAttribute(MRUI.MODEL_JSON);

                ModelBean modelBean = new Gson().fromJson(json, ModelBean.class);

                if (MRUI.SOURCE.equalsIgnoreCase(procType)) {
                    Set<String> sources = modelBean.getSources();
                    modelBean.setSources(updateParams(sources, procName, paramValue));
                } else if (MRUI.SINK.equalsIgnoreCase(procType)) {
                    Set<String> sinks = modelBean.getSinks();
                    modelBean.setSinks(updateParams(sinks, procName, paramValue));
                } else {
                    Set<String> procs = modelBean.getProcessors();
                    modelBean.setProcessors(updateParams(procs, procName, paramValue));
                }

                VaadinSession.getCurrent().setAttribute(MRUI.MODEL_JSON,
                        new Gson().toJson(modelBean, ModelBean.class));

                System.out.println("Updated modelJson: " + VaadinSession.getCurrent().getAttribute(MRUI.MODEL_JSON));
            }

            public Set<String> updateParams(Set<String> procs, String procName, String paramValue) throws JsonSyntaxException {
                Set<String> newProcs = Sets.newHashSet();
                for (String proc : procs) {
                    ProcessorBean procBean = new Gson().fromJson(proc, ProcessorBean.class);
                    if (procBean.getName().equalsIgnoreCase(procName)) {
                        procBean.getParams().put(paramKey, paramValue);
                        String procJson = new Gson().toJson(procBean, ProcessorBean.class);
                        newProcs.add(procJson);
                    } else {
                        newProcs.add(proc);
                    }
                }

                return newProcs;
            }
        });

        return button;
    }

    /**
     * @return the treeTable
     */
    public TreeTable getTreeTable() {
        return treeTable;
    }

    /**
     * @param treeTable the treeTable to set
     */
    public void setTreeTable(TreeTable treeTable) {
        this.treeTable = treeTable;
    }

    /**
     * @return the treeLayout
     */
    public VerticalLayout getTreeLayout() {
        return treeLayout;
    }

    /**
     * @param treeLayout the treeLayout to set
     */
    public void setTreeLayout(VerticalLayout treeLayout) {
        this.treeLayout = treeLayout;
    }

    /**
     * @return the runButton
     */
    public Button getRunButton() {
        return runButton;
    }

    /**
     * @param runButton the runButton to set
     */
    public void setRunButton(Button runButton) {
        this.runButton = runButton;
    }

    /**
     * @return the buttonLayout
     */
    public HorizontalLayout getButtonLayout() {
        return buttonLayout;
    }

    /**
     * @param buttonLayout the buttonLayout to set
     */
    public void setButtonLayout(HorizontalLayout buttonLayout) {
        this.buttonLayout = buttonLayout;
    }
}
