package me.vennlmao.ariscore.spawners.managers;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MobSpawnerDefinition {

    private final EntityType entityType;
    private final String spawnerName;
    private final String title;
    private final String material;
    private final String giveMaterial;
    private final int timeSeconds;
    private final String displayName;
    private final List<String> lore;
    private final long xpAmount;
    private final List<Material> itemLayoutOrder;
    private final Map<Material, Long> drops;

    public MobSpawnerDefinition(EntityType entityType, String spawnerName, String title, String material,
                                 String giveMaterial, int timeSeconds, String displayName, List<String> lore,
                                 long xpAmount, List<Material> itemLayoutOrder, Map<Material, Long> drops) {
        this.entityType = entityType;
        this.spawnerName = spawnerName;
        this.title = title;
        this.material = material;
        this.giveMaterial = giveMaterial;
        this.timeSeconds = timeSeconds;
        this.displayName = displayName;
        this.lore = lore;
        this.xpAmount = xpAmount;
        this.itemLayoutOrder = itemLayoutOrder;
        this.drops = new LinkedHashMap<>(drops);
    }

    public EntityType getEntityType() { return entityType; }
    public String getSpawnerName() { return spawnerName; }
    public String getTitle() { return title; }
    /** Icon dùng cho GUI (vd. slot "fullness"). Thường là TEXTURE:... (đầu skin). */
    public String getMaterial() { return material; }
    /** Icon dùng cho item khi /spawner give. Mặc định là "SPAWNER" nếu không cấu hình riêng, tách biệt khỏi icon GUI. */
    public String getGiveMaterial() { return giveMaterial; }
    public int getTimeSeconds() { return timeSeconds; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public long getXpAmount() { return xpAmount; }
    public List<Material> getItemLayoutOrder() { return itemLayoutOrder; }
    public Map<Material, Long> getDrops() { return drops; }
}
