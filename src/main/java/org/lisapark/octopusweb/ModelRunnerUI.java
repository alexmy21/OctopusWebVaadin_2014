package org.lisapark.octopusweb;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import com.vaadin.annotations.Theme;
import com.vaadin.data.Container;
import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.util.ContainerHierarchicalWrapper;
import com.vaadin.data.util.HierarchicalContainerOrderedWrapper;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.lisapark.octopus.core.ModelBean;
import org.lisapark.octopus.core.ProcessorBean;
import org.openide.util.Exceptions;

@Theme("mytheme")
@SuppressWarnings("serial")
public class ModelRunnerUI extends UI {

    /* User interface components are stored in session. */
    private Table modelList = new Table();
    private TreeTable treeTable = new TreeTable();
    private TextField searchField = new TextField();
    private Button syncModelListButton = new Button("Sync");
    private Button runSelectedModelButton = new Button("Run Selected Model");
    private FormLayout modelTreeLayout = new FormLayout();
    private VerticalLayout treeLayout = new VerticalLayout();
    private FieldGroup modelTreeFields = new FieldGroup();
    private static final String MODEL_NAME = "modelName";
    private static final String MODEL_JSON = "modelJson";
    private static String JETTY_URL = "http://10.1.10.11:8084/search/search";
    private static final String[] fieldNames = new String[]{MODEL_NAME, MODEL_JSON};
    /*
     * Any component can be bound to an external data sink. This example uses
     * just a dummy in-memory list, but there are many more practical
     * implementations.
     */
    IndexedContainer modelContainer = getModelDatasource();
    String modelJson = "";
    private static final String PROCESSOR_NAME = "Group/Processor Name";
    private static final String PARAM_VALUE = "Param Value";
    private static final String[] treeFieldNames = new String[]{PROCESSOR_NAME, PARAM_VALUE};
    private static final String SOURCE_GROUP_NAME = "SOURCES: ";
    private static final String PROC_GROUP_NAME = "PROCESSORS: ";
    private static final String SINK_GROUP_NAME = "SINKS: ";
    private static final String PROC_TYPE = "proctype";
    private static final String PROC_NAME_VALUE = "procnamevalue";
    private static final String SOURCE = "SOURCE";
    private static final String PROCESSOR = "PROCESSOR";
    private static final String SINK = "SINK";
    private static final String UPDATE_BUTTON = "UPDATE";

    /*
     * After UI class is created, init() is executed. You should build and wire
     * up your user interface here.
     */
    @Override
    protected void init(VaadinRequest request) {
        initLayout();
        initModelList();
        initTreeGrid();
        initSearch();
        initSyncRunButtons();
    }

    /*
     * In this example layouts are programmed in Java. You may choose use a
     * visual editor, CSS or HTML templates for layout instead.
     */
    private void initLayout() {

        /* Root of the user interface component tree is set */
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        setContent(splitPanel);

        /* Build the component tree */
        VerticalLayout leftLayout = new VerticalLayout();
        splitPanel.addComponent(leftLayout);
        splitPanel.addComponent(modelTreeLayout);

        HorizontalLayout topLeftLayout = new HorizontalLayout();
        leftLayout.addComponent(topLeftLayout);
        topLeftLayout.addComponent(searchField);
        topLeftLayout.addComponent(syncModelListButton);

        leftLayout.addComponent(modelList);

        /* Set the contents in the left of the split panel to use all the space */
        leftLayout.setSizeFull();

        /*
         * On the left side, expand the size of the contactList so that it uses
         * all the space left after from topLeftLayout
         */
        leftLayout.setExpandRatio(modelList, 1);
        modelList.setSizeFull();

        /*
         * In the topLeftLayout, searchField takes all the width there is
         * after adding addNewContactButton. The height of the layout is defined
         * by the tallest component.
         */
        topLeftLayout.setWidth("100%");
        searchField.setWidth("100%");
        topLeftLayout.setExpandRatio(searchField, 1);

        /* Put a little margin around the fields in the right side editor */
        modelTreeLayout.setMargin(true);
        modelTreeLayout.setSizeFull();
        modelTreeLayout.setVisible(false);
    }

    private void initTreeGrid() {

        modelTreeLayout.addComponent(runSelectedModelButton);

        ContainerHierarchicalWrapper containerHierarchicalWrapper = new ContainerHierarchicalWrapper(
                new IndexedContainer());
        HierarchicalContainerOrderedWrapper hc = new HierarchicalContainerOrderedWrapper(
                containerHierarchicalWrapper);

        /* User interface can be created dynamically to reflect underlying data. */
        for (String fieldName : fieldNames) {
            TextField field = new TextField(fieldName);
            modelTreeLayout.addComponent(field);
            field.setWidth("100%");

            modelTreeFields.bind(field, fieldName);
        }

        /*
         * Data can be buffered in the user interface. When doing so, commit()
         * writes the changes to the data sink. Here we choose to write the
         * changes automatically without calling commit().
         */
        modelTreeFields.setBuffered(false);

        treeLayout.setSizeFull();

        treeTable.setContainerDataSource(hc);

        treeTable.setTableFieldFactory(new TableFieldFactory() {
            @Override
            public Field createField(Container container, Object itemId,
                    Object propertyId, Component uiContext) {

                Container.Hierarchical hc = treeTable.getContainerDataSource();

                TextField field = new TextField((String) propertyId);

                // If you want to disable edition on a column, use ReadOnly
                if (PARAM_VALUE.equals(propertyId) && !hc.hasChildren(itemId)) {
                    field.setData(itemId);
                } else {
                    field.setReadOnly(true);
                }

                return field;
            }
        });

        treeTable.setWidth("500px");
        treeTable.setCaption("Model Parameter List");

        treeTable.setImmediate(true);
        treeTable.setSizeFull();

        generateModelTree(null);

        modelTreeLayout.addComponent(treeLayout);

    }

    private void generateModelTree(String json) {

        System.out.println("Model JSON from generate tree: " + modelJson);

        Container.Hierarchical hc = treeTable.getContainerDataSource();

        addModelData(hc, json);

//        treeTable.addGeneratedColumn(UPDATE_BUTTON, new ColumnGenerator() {
//            @Override
//            public Object generateCell(final Table source, final Object itemId, Object columnId) {
//
//                Button button = new Button("Delete");
//
//                button.addClickListener(new ClickListener() {
//                    @Override
//                    public void buttonClick(ClickEvent event) {
//
//                        source.getContainerDataSource().removeItem(itemId);
//                    }
//                });
//
//                return button;
//            }
//        });

        treeTable.setVisibleColumns((Object[]) new String[]{PROCESSOR_NAME, PARAM_VALUE, UPDATE_BUTTON});

        // First remove all components
        treeLayout.removeAllComponents();
        // And then add new treeTable
        treeLayout.addComponent(treeTable);

        treeTable.setSelectable(false);
        treeTable.setEditable(true);


//        treeTable.addValueChangeListener(new Property.ValueChangeListener() {
//            @Override
//            public void valueChange(Property.ValueChangeEvent event) {
//
//                Object procId = treeTable.getValue();
//
//                if (procId == null) {
//                    return;
//                }
//
//                if (modelList.isSelected(procId)
//                        && !treeTable.hasChildren(procId)) {
//
//                    String procType = (String) treeTable.getItem(procId)
//                            .getItemProperty(PROC_TYPE).getValue();
//                    String procName = (String) treeTable.getItem(procId)
//                            .getItemProperty(PROC_NAME_VALUE).getValue();
//                    String paramName = (String) treeTable.getItem(procId)
//                            .getItemProperty(PROCESSOR_NAME).getValue();
//                    String paramValue = (String) treeTable.getItem(procId)
//                            .getItemProperty(PARAM_VALUE).getValue();
//
//                    modelJson = updateJson(procType, procName, paramName, paramValue);
//
//                    System.out.println("Updated Model JSON: " + procId + "; " + modelJson);
//
////                    generateModelTree(modelJson);
//                } else {
//                }
//            }
//
//            private String updateJson(String procType, String procName, String paramName, String paramValue) {
//                return modelJson;
//            }
//        });

        treeLayout.setExpandRatio(treeTable, 1);

    }

    private synchronized void addModelData(Container.Hierarchical hc, String json) {

        hc.addContainerProperty(PROCESSOR_NAME, String.class, "");
        hc.addContainerProperty(PARAM_VALUE, String.class, "");
        hc.addContainerProperty(UPDATE_BUTTON, Button.class, "");
        hc.addContainerProperty(PROC_TYPE, String.class, "");
        hc.addContainerProperty(PROC_NAME_VALUE, String.class, "");

        if (json == null || json.isEmpty()) {
            return;
        }

        modelJsonData(hc, json);

    }

    private void initSearch() {

        /*
         * We want to show a subtle prompt in the search field. We could also
         * set a caption that would be shown above the field or description to
         * be shown in a tooltip.
         */
        searchField.setInputPrompt("Search Model");

        /*
         * Granularity for sending events over the wire can be controlled. By
         * default simple changes like writing a text in TextField are sent to
         * server with the next Ajax call. You can set your component to be
         * immediate to send the changes to server immediately after focus
         * leaves the field. Here we choose to send the text over the wire as
         * soon as user stops writing for a moment.
         */
        searchField.setTextChangeEventMode(TextChangeEventMode.LAZY);

        /*
         * When the event happens, we handle it in the anonymous inner class.
         * You may choose to use separate controllers (in MVC) or presenters (in
         * MVP) instead. In the end, the preferred application architecture is
         * up to you.
         */
        searchField.addTextChangeListener(new TextChangeListener() {
            @Override
            public void textChange(final TextChangeEvent event) {

                /* Reset the content and filter for the modelContainer. */
                modelContainer.removeAllContainerFilters();
                modelContainer.addContainerFilter(
                        (Container.Filter) new ModelFilter(event.getText()));
            }
        });
    }

    /**
     *
     * @param hc
     * @param json
     */
    private void modelJsonData(Container.Hierarchical hc, String json) {

        ModelBean modelBean = new Gson().fromJson(json, ModelBean.class);

        // Extract all sources
        Set<String> sources = modelBean.getSources();

        // Populate Source Group
        Object id;
        for (String source : sources) {
            Object sourceGroupId = hc.addItem();

            ProcessorBean procBean = new Gson().fromJson(source, ProcessorBean.class);
            id = hc.addItem();

            String procName = procBean.getName();
            hc.getItem(id).getItemProperty(PROCESSOR_NAME).setValue(procBean.getName());

            // Get all source's params
            Map<String, Object> params = procBean.getParams();
            for (Entry<String, Object> param : params.entrySet()) {
                Object paramId = hc.addItem();
                hc.getItem(paramId).getItemProperty(PROCESSOR_NAME).setValue(param.getKey());
                hc.getItem(paramId).getItemProperty(PARAM_VALUE).setValue(convert2string(param.getValue()));
                
                hc.getItem(paramId).getItemProperty(UPDATE_BUTTON)
                        .setValue(newButton(SOURCE, paramId, procName, param.getKey(), param.getValue()));
                hc.getItem(paramId).getItemProperty(PROC_TYPE).setValue(SOURCE);
                hc.getItem(paramId).getItemProperty(PROC_NAME_VALUE).setValue(procName);
                hc.setParent(paramId, id);
                hc.setChildrenAllowed(paramId, false);
            }
            hc.setParent(id, sourceGroupId);
            hc.getItem(sourceGroupId).getItemProperty(PROCESSOR_NAME).setValue(SOURCE_GROUP_NAME);
        }

        // Populate Processor's Group

        // Extract all Procs
        Set<String> procs = modelBean.getProcessors();

        for (String proc : procs) {
            Object procGroupId = hc.addItem();

            ProcessorBean procBean = new Gson().fromJson(proc, ProcessorBean.class);
            id = hc.addItem();

            String procName = procBean.getName();
            hc.getItem(id).getItemProperty(PROCESSOR_NAME).setValue(procName);

            // Get all proc's params
            Map<String, Object> params = procBean.getParams();
            for (Entry<String, Object> param : params.entrySet()) {
                Object paramId = hc.addItem();
                hc.getItem(paramId).getItemProperty(PROCESSOR_NAME)
                        .setValue(param.getKey());
                hc.getItem(paramId).getItemProperty(PARAM_VALUE)
                        .setValue(convert2string(param.getValue()));
                hc.getItem(paramId).getItemProperty(PROC_TYPE).setValue(PROCESSOR);
                hc.getItem(paramId).getItemProperty(PROC_NAME_VALUE).setValue(procName);
                hc.setParent(paramId, id);
                hc.setChildrenAllowed(paramId, false);
            }
            hc.setParent(id, procGroupId);
            hc.getItem(procGroupId).getItemProperty(PROCESSOR_NAME).setValue(PROC_GROUP_NAME);
        }

        // Populate Sink's Group

        // Extract all Sinks
        Set<String> sinks = modelBean.getSinks();

        for (String sink : sinks) {
            Object sinkGroupId = hc.addItem();

            ProcessorBean procBean = new Gson().fromJson(sink, ProcessorBean.class);
            id = hc.addItem();

            String procName = procBean.getName();
            hc.getItem(id).getItemProperty(PROCESSOR_NAME).setValue(procBean.getName());

            // Get all sink's params
            Map<String, Object> params = procBean.getParams();
            for (Entry<String, Object> param : params.entrySet()) {
                Object paramId = hc.addItem();
                hc.getItem(paramId).getItemProperty(PROCESSOR_NAME).setValue(param.getKey());
                hc.getItem(paramId).getItemProperty(PARAM_VALUE).setValue(convert2string(param.getValue()));
                hc.getItem(paramId).getItemProperty(PROC_TYPE).setValue(SINK);
                hc.getItem(paramId).getItemProperty(PROC_NAME_VALUE).setValue(procName);
                hc.setParent(paramId, id);
                hc.setChildrenAllowed(paramId, false);
            }
            hc.setParent(id, sinkGroupId);
            hc.getItem(sinkGroupId).getItemProperty(PROCESSOR_NAME).setValue(SINK_GROUP_NAME);
        }
    }

    private String convert2string(Object value) {
        String retString = "";

        if (value != null && value instanceof String) {
            retString = value.toString();
        }

        return retString;
    }

    private Button newButton(final String procType, final Object procId, final String procName, 
            final String paramKey, final Object paramValue) {
        Button button = new Button();
        
        button.setCaption("update");
        button.setImmediate(true);
        button.setStyleName("reindeer");
        
        button.addClickListener(new ClickListener(){
            
            @Override
            public void buttonClick(ClickEvent event) {
                String paramValue = (String) treeTable.getItem(procId)
                            .getItemProperty(PARAM_VALUE).getValue();
                System.out.println("Button clicked: " + procType 
                        + "; procId: " + procId 
                        + "; procName: " + procName 
                        + "; paramKey: " + paramKey 
                        + "; paramValue: " + paramValue);
            }
            
        });
        
        return button;
    }

    /*
     * A custom filter for searching names and models in the
     * modelContainer.
     */
    private class ModelFilter implements Filter {

        private String needle;

        public ModelFilter(String needle) {
            this.needle = needle.toLowerCase();
        }

        @Override
        public boolean passesFilter(Object itemId, Item item) {
            String haystack = ("" + item.getItemProperty(MODEL_NAME).getValue()).toLowerCase();
            return haystack.contains(needle);
        }

        @Override
        public boolean appliesToProperty(Object id) {
            return true;
        }
    }

    private void initSyncRunButtons() {
        syncModelListButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {

                modelContainer.removeAllItems();
                modelContainer = getModelDatasource();
                modelList.setContainerDataSource(modelContainer);

                modelContainer.removeAllContainerFilters();
            }
        });

        runSelectedModelButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                Object contactId = modelList.getValue();
                modelList.removeItem(contactId);
            }
        });
    }

    private void initModelList() {
        modelList.setContainerDataSource(modelContainer);
        modelList.setVisibleColumns((Object[]) new String[]{MODEL_NAME, MODEL_JSON});
        modelList.setSelectable(true);
        modelList.setImmediate(true);

        modelList.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                Object modelId = modelList.getValue();

                /*
                 * When a model is selected from the list, we want to show
                 * that in our editor on the right. This is nicely done by the
                 * FieldGroup that binds all the fields to the corresponding
                 * Properties in our contact at once.
                 */
                if (modelId != null) {
                    modelTreeFields.setItemDataSource(modelList
                            .getItem(modelId));
                    modelJson = (String) modelList.getItem(modelId)
                            .getItemProperty(MODEL_JSON).getValue();

                    System.out.println("Model JSON: " + modelJson);

                    generateModelTree(modelJson);
                }

                modelTreeLayout.setVisible(modelId != null);
            }
        });
    }

    /*
     * Generate some in-memory example data to play with. In a real application
     * we could be using SQLContainer, JPAContainer or some other to persist the
     * data.
     */
    private static IndexedContainer getModelDatasource() {

        IndexedContainer modelContainer = new IndexedContainer();

        for (String p : fieldNames) {
            modelContainer.addContainerProperty(p, String.class, "");
        }

        try {
            HttpClient client = new DefaultHttpClient();

            HttpGet request = new HttpGet(JETTY_URL);
            HttpResponse response = client.execute(request);

            // Get the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder jsonResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonResponse.append(line);
            }

            List<String> modelList = new Gson().fromJson(jsonResponse.toString(), List.class);

            List<ModelBean> beanList = Lists.newArrayList();

            for (String item : modelList) {
                ModelBean bean = new Gson().fromJson(item, ModelBean.class);
                beanList.add(bean);

                Object id = modelContainer.addItem();
                modelContainer.getContainerProperty(id, MODEL_NAME).setValue(bean.getModelName());
                modelContainer.getContainerProperty(id, MODEL_JSON).setValue(item);
            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return modelContainer;
    }
}
