package me.vennlmao.ariscore.crates.managers;

import me.vennlmao.ariscore.crates.models.CrateModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CrateRegistry {

    private final Map<String, CrateModel> registry = new HashMap<>();

    public void cache(CrateModel crateModel) {
        registry.put(crateModel.getName().toLowerCase(), crateModel);
    }

    public CrateModel find(String name) {
        return name != null ? registry.get(name.toLowerCase()) : null;
    }

    public CrateModel remove(String name) {
        return name != null ? registry.remove(name.toLowerCase()) : null;
    }

    public Collection<CrateModel> values() {
        return registry.values();
    }

    public void clear() {
        registry.clear();
    }
}
 
