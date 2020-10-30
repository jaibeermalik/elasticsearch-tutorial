package org.jai.search.client.impl;

import static com.google.common.collect.Maps.newHashMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.jai.search.client.SearchClientService;
import org.jai.search.model.ElasticSearchReservedWords;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service(value="searchClientService")
public class SearchClientServiceMockImpl implements SearchClientService
{
    private Map<String, Node> nodes = newHashMap();

    private Map<String, Client> clients = newHashMap();

    private Settings defaultSettings = Settings.builder()
            
            .put(ElasticSearchReservedWords.CLUSTER_NAME.getText(), "test-cluster-localhost-" + UUID.randomUUID())
            //data dir, other node dir for lock etc will still be created
            .put("path.home", new File(System.getProperty("java.io.tmpdir") + "/esintegrationtest").getAbsolutePath())
            .put(ElasticSearchReservedWords.PATH_DATA.getText(), new File(System.getProperty("java.io.tmpdir") + "/esintegrationtest/data").getAbsolutePath())
//            .put(ElasticSearchReservedWords.PATH_WORK.getText(), new File(System.getProperty("java.io.tmpdir") + "/esintegrationtest/work").getAbsolutePath())
            .put(ElasticSearchReservedWords.PATH_LOG.getText(), new File(System.getProperty("java.io.tmpdir") + "/esintegrationtest/log").getAbsolutePath())
            .put(ElasticSearchReservedWords.PATH_CONF.getText(), new File("config").getAbsolutePath())
            
            //will not survive restart
            //TODO: memory store type cause out of memory in eclipse on low config machine
            // Check how to set memory setting and allocations in memory store type.
            .put("index.store.type", "mmapfs")
            .build();

    @PostConstruct
    public void createNodes() throws Exception {
        Settings settings = Settings.builder()
//        		.put(ElasticSearchReservedWords.NUMBER_OF_SHARDS.getText(), 3)
//                .put(ElasticSearchReservedWords.NUMBER_OF_REPLICAS.getText(), 1)
//                .put(ElasticSearchReservedWords.INDEX_MAPPER_DYNAMIC.getText(), false)
                .build();
        startNode("server1", settings);
//        startNode("server2", settings);
    }

//    @PreDestroy
    public void closeNodes() {
        getClient().close();
        try {
			closeAllNodes();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public Client getClient() {
        return client("server1");
    }
    
//    @Override
    public void addNewNode(String name)
    {
        try {
			buildNode(name);
			startNode(name);
		} catch (IOException | NodeValidationException e) {
			e.printStackTrace();
		}
        
    }
    
//    @Override
    public void removeNode(String nodeName)
    {
        try {
			closeNode(nodeName);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public void putDefaultSettings(Settings.Builder settings) {
        putDefaultSettings(settings.build());
    }

    public void putDefaultSettings(Settings settings) {
        defaultSettings = Settings.builder().put(defaultSettings).put(settings).build();
    }

    public Node startNode(String id) throws NodeValidationException, IOException {
        return buildNode(id).start();
    }

    public Node startNode(String id, Settings.Builder settings) throws NodeValidationException, IOException {
        return startNode(id, settings.build());
    }

    public Node startNode(String id, Settings settings) throws NodeValidationException, IOException {
        return buildNode(id, settings).start();
    }

    public Node buildNode(String id) throws IOException {
        return buildNode(id, Settings.EMPTY);
    }

    public Node buildNode(String id, Settings.Builder settings) throws IOException {
        return buildNode(id, settings.build());
    }

    public Node buildNode(String id, Settings settings) throws IOException {
        String settingsSource = getClass().getName().replace('.', '/') + ".yml";
        Settings finalSettings = Settings.builder()
//                .loadFromPath(Paths.get(new ClassPathResource(settingsSource).getPath()))
                .put(defaultSettings)
                .put(settings)
                .put("node.name", id)
                .put("transport.type", "local")
                .put("http.enabled", false) 
                .put("node.max_local_storage_nodes", 2) 
//                .put("script.engine.groovy.inline.update", true)
                .put("script.inline", true)
                .put("script.stored", true)
                .put("script.update", true)
//                .put("script.engine.painless.inline.update", true)
//                .put("script.engine.painless.inline", true)
//                .put("script.plugin", true)
                
                .build();

        if (finalSettings.get("gateway.type") == null) {
            // default to non gateway
//            finalSettings = Settings.builder().put(finalSettings).put("gateway.type", "local").build();
        }
        if (finalSettings.get("cluster.routing.schedule") != null) {
            // decrease the routing schedule so new nodes will be added quickly
            finalSettings = Settings.builder().put(finalSettings).put("cluster.routing.schedule", "50ms").build();
        }

        Node node = new Node(finalSettings);
        nodes.put(id, node);
        clients.put(id, node.client());
        return node;
    }

    public void closeNode(String id) throws IOException {
        Client client = clients.remove(id);
        if (client != null) {
            client.close();
        }
        Node node = nodes.remove(id);
        if (node != null) {
            node.close();
        }
    }

    public Node node(String id) {
        return nodes.get(id);
    }

    public Client client(String id) {
        return clients.get(id);
    }

    public void closeAllNodes() throws IOException {
        for (Client client : clients.values()) {
            client.close();
        }
        clients.clear();
        for (Node node : nodes.values()) {
            node.close();
        }
        nodes.clear();
    }

}
