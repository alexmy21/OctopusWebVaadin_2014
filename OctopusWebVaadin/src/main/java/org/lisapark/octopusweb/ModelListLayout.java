/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lisapark.octopusweb;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.lisapark.octopus.core.ModelBean;
import org.openide.util.Exceptions;

/**
 *
 * @author alexmy
 */
public class ModelListLayout extends VerticalLayout {
    
    private HorizontalLayout searchLayout;
    private VerticalLayout tableLayout;
    private Table modelList;
    
    TextField searchField  = new TextField();; 
    private Button syncButton;
    
    private IndexedContainer modelContainer;
    private static final String[] fieldNames = new String[]{MRUI.MODEL_NAME, MRUI.MODEL_JSON};
            
    ModelListLayout(){
        
        this.searchField.setSizeFull();        
        
        this.syncButton = new Button("Sync");
        
        syncButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {

                modelContainer.removeAllItems();
                modelContainer = getModelDatasource();
                modelList.setContainerDataSource(modelContainer);
                modelList.setVisibleColumns((Object[]) new String[]{MRUI.MODEL_NAME});

                modelContainer.removeAllContainerFilters();
               
                VaadinSession.getCurrent().setAttribute(MRUI.MODEL_JSON, null);
                
                ModelTreeLayout.initModelTreeLayout(null);
            }
        });

        this.searchLayout = new HorizontalLayout();
        this.searchLayout.addComponent(searchField);
        this.searchLayout.addComponent(syncButton);
        this.searchLayout.setMargin(true);
        
        this.addComponent(searchLayout);
        
        this.modelList = new Table();
        
        this.modelContainer = getModelDatasource();
        modelList.setContainerDataSource(modelContainer);
        modelList.setVisibleColumns((Object[])new String[]{MRUI.MODEL_NAME});
        modelList.setSelectable(true);
        modelList.setImmediate(true);
        modelList.setSizeFull();
        modelList.setWidth("100%");
        
        modelList.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
            
                Object modelId = modelList.getValue();

                VaadinSession.getCurrent().setAttribute(MRUI.TREE_VISIBLE, (modelId != null));
                
                if (modelId != null) {
                    VaadinSession.getCurrent().setAttribute(MRUI.MODEL_TREE_FIELDS, 
                            modelList.getItem(modelId));
                    String json = (String) modelList.getItem(modelId).getItemProperty(MRUI.MODEL_JSON).getValue();
                    
                    VaadinSession.getCurrent().setAttribute(MRUI.MODEL_JSON, json);
                   
                    System.out.println("Model JSON: " + json);

                    ModelTreeLayout.initModelTreeLayout(json);

                }                
            }
        });
        
        this.tableLayout = new VerticalLayout();
        this.tableLayout.setSizeFull();
        this.tableLayout.setHeight("100%");
        this.tableLayout.setMargin(true);
        
        this.tableLayout.addComponent(modelList);
        
        this.addComponent(tableLayout);
        
        initSearch(searchField);
        
        this.setExpandRatio(tableLayout, 1);        
        
    }
    
    void initSearch(TextField searchField) {

        searchField.setInputPrompt("Search Model");
        searchField.setTextChangeEventMode(AbstractTextField.TextChangeEventMode.LAZY);
        searchField.addTextChangeListener(new FieldEvents.TextChangeListener() {
            @Override
            public void textChange(final FieldEvents.TextChangeEvent event) {

                /* Reset the content and filter for the modelContainer. */
                modelContainer.removeAllContainerFilters();
                modelContainer.addContainerFilter(
                        (Container.Filter) new ModelFilter(event.getText()));
            }
        });
    }
    
    /*
     * A custom filter for searching names and models in the
     * modelContainer.
     */
    private class ModelFilter implements Container.Filter {

        private String needle;

        public ModelFilter(String needle) {
            this.needle = needle.toLowerCase();
        }

        @Override
        public boolean passesFilter(Object itemId, Item item) {
            String haystack = ("" + item.getItemProperty(MRUI.MODEL_NAME).getValue()).toLowerCase();
            return haystack.contains(needle);
        }

        @Override
        public boolean appliesToProperty(Object id) {
            return true;
        }
    }
    
    private IndexedContainer getModelDatasource() {

        IndexedContainer _modelContainer = new IndexedContainer();

        for (String p : fieldNames) {
            _modelContainer.addContainerProperty(p, String.class, "");
        }

        try {
            HttpClient client = new DefaultHttpClient();

            HttpGet request = new HttpGet(MRUI.JETTY_SEARCH_URL);
            HttpResponse response = client.execute(request);

            // Get the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder jsonResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonResponse.append(line);
            }

            List<String> _modelList = new Gson().fromJson(jsonResponse.toString(), List.class);

            List<ModelBean> beanList = Lists.newArrayList();

            for (String item : _modelList) {
                ModelBean bean = new Gson().fromJson(item, ModelBean.class);
                beanList.add(bean);

                Object id = _modelContainer.addItem();
                _modelContainer.getContainerProperty(id, MRUI.MODEL_NAME).setValue(bean.getModelName());
                _modelContainer.getContainerProperty(id, MRUI.MODEL_JSON).setValue(item);
            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return _modelContainer;
    }
}
