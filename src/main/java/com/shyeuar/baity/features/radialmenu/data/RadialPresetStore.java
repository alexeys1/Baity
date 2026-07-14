package com.shyeuar.baity.features.radialmenu.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shyeuar.baity.config.BaityConfigDir;
import com.shyeuar.baity.config.ConfigManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.shyeuar.baity.features.radialmenu.data.RadialMenuModels.MAX_CUSTOM_PRESETS;
import static com.shyeuar.baity.features.radialmenu.data.RadialMenuModels.MAX_LAYER_DEPTH;
import static com.shyeuar.baity.features.radialmenu.data.RadialMenuModels.MAX_SLOTS_PER_LAYER;
import static com.shyeuar.baity.features.radialmenu.data.RadialMenuModels.MAX_UNICODE_ICON_CODE_POINTS;

@Environment(EnvType.CLIENT)
public final class RadialPresetStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/RadialPresets");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "radial-presets.json";
    private static final String DEFAULT_PRESET_ID = "default";
    private static final String DEFAULT_ROOT_LAYER_ID = "default_root";

    private static RadialMenuModels.RadialPresetBundle bundle;

    private RadialPresetStore() {
    }

    public static void init() {
        load();
    }

    public static RadialMenuModels.RadialPresetBundle getBundle() {
        if (bundle == null) {
            load();
        }
        return bundle;
    }

    public static RadialMenuModels.RadialPreset getActivePreset() {
        RadialMenuModels.RadialPreset preset = getBundle().activePreset();
        if (preset != null) {
            return preset;
        }
        if (!getBundle().presets.isEmpty()) {
            return getBundle().presets.getFirst();
        }
        return null;
    }

    public static void save() {
        if (bundle == null) {
            return;
        }
        Path file = BaityConfigDir.getBaityConfigDir().resolve(FILE_NAME);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(bundle, writer);
            }
            ConfigManager.radialMenuActivePresetId = bundle.editor.activePresetId;
        } catch (IOException e) {
            LOGGER.warn("Failed to save radial presets: {}", e.toString());
        }
    }

    public static void load() {
        Path file = BaityConfigDir.getBaityConfigDir().resolve(FILE_NAME);
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                bundle = GSON.fromJson(reader, RadialMenuModels.RadialPresetBundle.class);
            } catch (Exception e) {
                LOGGER.warn("Failed to load radial presets, using default: {}", e.toString());
                bundle = createDefaultBundle();
            }
        } else {
            bundle = createDefaultBundle();
        }
        if (bundle == null || bundle.presets == null || bundle.presets.isEmpty()) {
            bundle = createDefaultBundle();
        }
        if (bundle.editor == null) {
            bundle.editor = new RadialMenuModels.EditorState();
        }
        if (bundle.editor.expandedNodes == null) {
            bundle.editor.expandedNodes = new HashSet<>();
        }
        normalizeBundle(bundle);
        if (ConfigManager.radialMenuActivePresetId != null && !ConfigManager.radialMenuActivePresetId.isBlank()) {
            if (bundle.findPreset(ConfigManager.radialMenuActivePresetId) != null) {
                bundle.editor.activePresetId = ConfigManager.radialMenuActivePresetId;
            }
        }
        save();
    }

    public static void selectPreset(String presetId) {
        RadialMenuModels.RadialPreset preset = getBundle().findPreset(presetId);
        if (preset == null) {
            return;
        }
        getBundle().editor.activePresetId = presetId;
        getBundle().editor.selectedLayerId = preset.rootLayerId;
        getBundle().editor.selectedSlotIndex = -1;
        collapseUnrelatedEmptyExpands();
        save();
    }

    public static void selectLayer(String presetId, String layerId) {
        RadialMenuModels.RadialPreset preset = getBundle().findPreset(presetId);
        if (preset == null || !preset.layers.containsKey(layerId)) {
            return;
        }
        getBundle().editor.activePresetId = presetId;
        getBundle().editor.selectedLayerId = layerId;
        getBundle().editor.selectedSlotIndex = -1;
        save();
    }

    public static void selectSlot(int slotIndex) {
        getBundle().editor.selectedSlotIndex = slotIndex;
        collapseUnrelatedEmptyExpands();
        save();
    }

    public static void toggleExpanded(String nodeId) {
        setExpanded(nodeId, !isExpanded(nodeId));
    }

    public static void setExpanded(String nodeId, boolean expanded) {
        Set<String> nodes = getBundle().editor.expandedNodes;
        if (expanded) {
            nodes.add(nodeId);
        } else {
            nodes.remove(nodeId);
        }
        save();
    }

    public static boolean isExpanded(String nodeId) {
        return getBundle().editor.expandedNodes.contains(nodeId);
    }

    private static void collapseUnrelatedEmptyExpands() {
        RadialMenuModels.EditorState editor = getBundle().editor;
        RadialMenuModels.RadialPreset preset = getBundle().findPreset(editor.activePresetId);
        if (preset == null || editor.expandedNodes.isEmpty()) {
            return;
        }
        String prefix = preset.id + ":";
        Set<String> remove = new HashSet<>();
        for (String expandKey : editor.expandedNodes) {
            if (!expandKey.startsWith(prefix)) {
                continue;
            }
            String childLayerId = expandKey.substring(prefix.length());
            RadialMenuModels.RadialLayer child = preset.layers.get(childLayerId);
            if (child == null || !child.slots.isEmpty()) {
                continue;
            }
            String[] owner = findChildOwner(preset, childLayerId);
            if (owner == null
                    || !isSelectionRelatedToEmptyChild(
                    preset, owner[0], Integer.parseInt(owner[1]), childLayerId,
                    editor.selectedLayerId, editor.selectedSlotIndex)) {
                remove.add(expandKey);
            }
        }
        editor.expandedNodes.removeAll(remove);
    }

    private static String[] findChildOwner(RadialMenuModels.RadialPreset preset, String childLayerId) {
        for (RadialMenuModels.RadialLayer layer : preset.layers.values()) {
            for (int i = 0; i < layer.slots.size(); i++) {
                RadialMenuModels.RadialSlot slot = layer.slots.get(i);
                if (childLayerId.equals(slot.childLayerId)) {
                    return new String[]{layer.id, String.valueOf(i)};
                }
            }
        }
        return null;
    }

    private static boolean isSelectionRelatedToEmptyChild(
            RadialMenuModels.RadialPreset preset,
            String ownerLayerId,
            int ownerSlotIndex,
            String childLayerId,
            String selectedLayerId,
            int selectedSlotIndex
    ) {
        if (childLayerId.equals(selectedLayerId)) {
            return true;
        }
        if (ownerLayerId.equals(selectedLayerId) && selectedSlotIndex == ownerSlotIndex) {
            return true;
        }
        return isLayerUnder(preset, selectedLayerId, childLayerId);
    }

    private static boolean isLayerUnder(RadialMenuModels.RadialPreset preset, String layerId, String ancestorLayerId) {
        if (layerId == null || ancestorLayerId == null || layerId.equals(preset.rootLayerId)) {
            return false;
        }
        if (layerId.equals(ancestorLayerId)) {
            return true;
        }
        RadialMenuModels.RadialLayer layer = preset.layers.get(layerId);
        if (layer == null || layer.parentSlotId == null) {
            return false;
        }
        for (RadialMenuModels.RadialLayer candidate : preset.layers.values()) {
            for (RadialMenuModels.RadialSlot slot : candidate.slots) {
                if (layer.parentSlotId.equals(slot.id)) {
                    return isLayerUnder(preset, candidate.id, ancestorLayerId);
                }
            }
        }
        return false;
    }

    public static RadialMenuModels.RadialPreset addPreset() {
        if (countCustomPresets() >= MAX_CUSTOM_PRESETS) {
            return null;
        }
        RadialMenuModels.RadialPreset preset = new RadialMenuModels.RadialPreset();
        preset.name = nextPresetName();
        RadialMenuModels.RadialLayer root = new RadialMenuModels.RadialLayer();
        root.id = preset.id + "_root";
        preset.rootLayerId = root.id;
        preset.layers.put(root.id, root);
        getBundle().presets.add(preset);
        selectPreset(preset.id);
        save();
        return preset;
    }

    public static boolean deletePreset(String presetId) {
        RadialMenuModels.RadialPreset preset = getBundle().findPreset(presetId);
        if (preset == null || !preset.deletable) {
            return false;
        }
        int presetIndex = indexOfPreset(presetId);
        getBundle().presets.removeIf(p -> p.id.equals(presetId));
        if (presetId.equals(getBundle().editor.activePresetId) || getBundle().findPreset(getBundle().editor.activePresetId) == null) {
            int fallbackIndex = Math.max(0, presetIndex - 1);
            if (!getBundle().presets.isEmpty()) {
                fallbackIndex = Math.min(fallbackIndex, getBundle().presets.size() - 1);
                selectPreset(getBundle().presets.get(fallbackIndex).id);
            }
        }
        save();
        return true;
    }

    public static boolean deleteSelection(RadialMenuModels.RadialPreset preset) {
        if (preset == null) {
            return false;
        }
        RadialMenuModels.EditorState editor = getBundle().editor;
        if (editor.selectedSlotIndex >= 0) {
            RadialMenuModels.RadialLayer layer = preset.layers.get(editor.selectedLayerId);
            if (layer == null || editor.selectedSlotIndex >= layer.slots.size()) {
                return false;
            }
            int deletedIndex = editor.selectedSlotIndex;
            String layerId = editor.selectedLayerId;
            RadialMenuModels.RadialSlot slot = layer.slots.get(deletedIndex);
            if (slot.childLayerId != null && preset.layers.containsKey(slot.childLayerId)) {
                stripChildLayer(preset, slot);
                save();
                return true;
            }
            removeSlot(preset, layerId, deletedIndex);
            selectAfterSlotDeletion(preset, layerId, deletedIndex);
            save();
            return true;
        }
        if (!preset.deletable || !editor.selectedLayerId.equals(preset.rootLayerId)) {
            return false;
        }
        return deletePreset(preset.id);
    }

    private static void stripChildLayer(RadialMenuModels.RadialPreset preset, RadialMenuModels.RadialSlot slot) {
        if (slot.childLayerId == null) {
            return;
        }
        String childId = slot.childLayerId;
        deleteLayerRecursive(preset, childId);
        slot.childLayerId = null;
        getBundle().editor.expandedNodes.remove(treeNodeId(getBundle().editor.activePresetId, childId));
    }

    private static void selectAfterSlotDeletion(RadialMenuModels.RadialPreset preset, String layerId, int deletedIndex) {
        RadialMenuModels.EditorState editor = getBundle().editor;
        RadialMenuModels.RadialLayer layer = preset.layers.get(layerId);
        if (layer != null && !layer.slots.isEmpty()) {
            int newIndex = Math.min(deletedIndex - 1, layer.slots.size() - 1);
            editor.selectedLayerId = layerId;
            editor.selectedSlotIndex = Math.max(0, newIndex);
            return;
        }
        selectParentOfLayer(preset, layerId);
    }

    private static void selectParentOfLayer(RadialMenuModels.RadialPreset preset, String layerId) {
        RadialMenuModels.EditorState editor = getBundle().editor;
        if (layerId.equals(preset.rootLayerId)) {
            editor.selectedLayerId = preset.rootLayerId;
            editor.selectedSlotIndex = -1;
            return;
        }
        RadialMenuModels.RadialLayer layer = preset.layers.get(layerId);
        if (layer == null || layer.parentSlotId == null) {
            editor.selectedLayerId = preset.rootLayerId;
            editor.selectedSlotIndex = -1;
            return;
        }
        for (RadialMenuModels.RadialLayer candidate : preset.layers.values()) {
            for (int i = 0; i < candidate.slots.size(); i++) {
                RadialMenuModels.RadialSlot slot = candidate.slots.get(i);
                if (layer.parentSlotId.equals(slot.id)) {
                    editor.selectedLayerId = candidate.id;
                    editor.selectedSlotIndex = i;
                    return;
                }
            }
        }
        editor.selectedLayerId = preset.rootLayerId;
        editor.selectedSlotIndex = -1;
    }

    private static int indexOfPreset(String presetId) {
        List<RadialMenuModels.RadialPreset> presets = getBundle().presets;
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).id.equals(presetId)) {
                return i;
            }
        }
        return -1;
    }

    public static RadialMenuModels.RadialSlot addSlot(RadialMenuModels.RadialPreset preset, String layerId) {
        RadialMenuModels.RadialLayer layer = preset.layers.get(layerId);
        if (layer == null || layer.slots.size() >= MAX_SLOTS_PER_LAYER) {
            return null;
        }
        RadialMenuModels.RadialSlot slot = new RadialMenuModels.RadialSlot();
        int serial = allocateSerial(layer);
        String slotNumber = String.valueOf(serial);
        slot.serial = serial;
        slot.displayName = slotNumber;
        slot.unicodeIcon = slotNumber;
        layer.slots.add(slot);
        getBundle().editor.selectedSlotIndex = layer.slots.size() - 1;
        save();
        return slot;
    }

    public static RadialMenuModels.RadialLayer addChildLayer(RadialMenuModels.RadialPreset preset, String layerId, int slotIndex) {
        RadialMenuModels.RadialLayer parentLayer = preset.layers.get(layerId);
        if (parentLayer == null || slotIndex < 0 || slotIndex >= parentLayer.slots.size()) {
            return null;
        }
        if (getLayerDepth(preset, layerId) >= MAX_LAYER_DEPTH) {
            return null;
        }
        RadialMenuModels.RadialSlot slot = parentLayer.slots.get(slotIndex);
        if (slot.childLayerId != null && preset.layers.containsKey(slot.childLayerId)) {
            return preset.layers.get(slot.childLayerId);
        }
        RadialMenuModels.RadialLayer child = new RadialMenuModels.RadialLayer();
        child.parentSlotId = slot.id;
        slot.childLayerId = child.id;
        preset.layers.put(child.id, child);
        getBundle().editor.expandedNodes.add(treeNodeId(preset.id, child.id));
        selectLayer(preset.id, child.id);
        selectSlot(-1);
        save();
        return child;
    }

    public static void removeSlot(RadialMenuModels.RadialPreset preset, String layerId, int slotIndex) {
        RadialMenuModels.RadialLayer layer = preset.layers.get(layerId);
        if (layer == null || slotIndex < 0 || slotIndex >= layer.slots.size()) {
            return;
        }
        RadialMenuModels.RadialSlot slot = layer.slots.remove(slotIndex);
        recycleSerial(layer, slot.serial);
        if (slot.childLayerId != null) {
            deleteLayerRecursive(preset, slot.childLayerId);
        }
        if (getBundle().editor.selectedSlotIndex == slotIndex) {
            getBundle().editor.selectedSlotIndex = -1;
        } else if (getBundle().editor.selectedSlotIndex > slotIndex) {
            getBundle().editor.selectedSlotIndex--;
        }
        save();
    }

    public static void reorderSlot(RadialMenuModels.RadialLayer layer, int fromIndex, int toIndex) {
        if (layer == null || fromIndex == toIndex || fromIndex < 0 || toIndex < 0
                || fromIndex >= layer.slots.size() || toIndex >= layer.slots.size()) {
            return;
        }
        RadialMenuModels.RadialSlot moved = layer.slots.remove(fromIndex);
        layer.slots.add(toIndex, moved);
        if (getBundle().editor.selectedSlotIndex == fromIndex) {
            getBundle().editor.selectedSlotIndex = toIndex;
        }
        save();
    }

    public static void updateSelectedSlot(RadialMenuModels.RadialPreset preset, String layerId, int slotIndex,
                                          String command, String displayName, String icon, String unicodeIcon) {
        RadialMenuModels.RadialLayer layer = preset.layers.get(layerId);
        if (layer == null || slotIndex < 0 || slotIndex >= layer.slots.size()) {
            return;
        }
        RadialMenuModels.RadialSlot slot = layer.slots.get(slotIndex);
        slot.command = command == null ? "" : command.trim();
        slot.displayName = displayName == null ? "" : displayName;
        slot.icon = icon == null ? "" : icon.trim();
        slot.unicodeIcon = clampUnicode(unicodeIcon);
        save();
    }

    public static int getLayerDepth(RadialMenuModels.RadialPreset preset, String layerId) {
        if (layerId == null || layerId.equals(preset.rootLayerId)) {
            return 0;
        }
        RadialMenuModels.RadialLayer layer = preset.layers.get(layerId);
        if (layer == null || layer.parentSlotId == null) {
            return 0;
        }
        for (RadialMenuModels.RadialLayer candidate : preset.layers.values()) {
            for (RadialMenuModels.RadialSlot slot : candidate.slots) {
                if (layer.parentSlotId.equals(slot.id)) {
                    return getLayerDepth(preset, candidate.id) + 1;
                }
            }
        }
        return 0;
    }

    public static String treeNodeId(String presetId, String nodeKey) {
        return presetId + ":" + nodeKey;
    }

    public static String clampUnicode(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        if (raw.codePointCount(0, raw.length()) <= MAX_UNICODE_ICON_CODE_POINTS) {
            return raw;
        }
        return raw.substring(0, raw.offsetByCodePoints(0, MAX_UNICODE_ICON_CODE_POINTS));
    }

    private static void deleteLayerRecursive(RadialMenuModels.RadialPreset preset, String layerId) {
        RadialMenuModels.RadialLayer layer = preset.layers.remove(layerId);
        if (layer == null) {
            return;
        }
        for (RadialMenuModels.RadialSlot slot : layer.slots) {
            if (slot.childLayerId != null) {
                deleteLayerRecursive(preset, slot.childLayerId);
            }
        }
        if (layerId.equals(getBundle().editor.selectedLayerId)) {
            getBundle().editor.selectedLayerId = preset.rootLayerId;
            getBundle().editor.selectedSlotIndex = -1;
        }
    }

    private static int countCustomPresets() {
        int count = 0;
        for (RadialMenuModels.RadialPreset preset : getBundle().presets) {
            if (preset.deletable) {
                count++;
            }
        }
        return count;
    }

    private static String nextPresetName() {
        int index = 1;
        outer:
        while (true) {
            String candidate = "preset" + index;
            for (RadialMenuModels.RadialPreset preset : getBundle().presets) {
                if (candidate.equalsIgnoreCase(preset.name)) {
                    index++;
                    continue outer;
                }
            }
            return candidate;
        }
    }

    private static void normalizeBundle(RadialMenuModels.RadialPresetBundle bundle) {
        for (RadialMenuModels.RadialPreset preset : bundle.presets) {
            if (preset.layers == null) {
                preset.layers = new HashMap<>();
            }
            if (preset.rootLayerId == null || !preset.layers.containsKey(preset.rootLayerId)) {
                RadialMenuModels.RadialLayer root = new RadialMenuModels.RadialLayer();
                root.id = preset.id + "_root";
                preset.rootLayerId = root.id;
                preset.layers.put(root.id, root);
            }
            for (RadialMenuModels.RadialLayer layer : preset.layers.values()) {
                normalizeLayerSerials(layer);
            }
        }
        if (bundle.findPreset(bundle.editor.activePresetId) == null) {
            bundle.editor.activePresetId = DEFAULT_PRESET_ID;
        }
        RadialMenuModels.RadialPreset active = bundle.findPreset(bundle.editor.activePresetId);
        if (active != null && (bundle.editor.selectedLayerId == null || !active.layers.containsKey(bundle.editor.selectedLayerId))) {
            bundle.editor.selectedLayerId = active.rootLayerId;
            bundle.editor.selectedSlotIndex = -1;
        }
    }

    private static void normalizeLayerSerials(RadialMenuModels.RadialLayer layer) {
        if (layer.slots == null) {
            layer.slots = new ArrayList<>();
        }
        if (layer.freeSerials == null) {
            layer.freeSerials = new ArrayList<>();
        }
        if (layer.nextSerial < 1) {
            layer.nextSerial = 1;
        }
        Set<Integer> used = new HashSet<>();
        for (RadialMenuModels.RadialSlot slot : layer.slots) {
            if (slot.serial > 0) {
                used.add(slot.serial);
            }
        }
        for (RadialMenuModels.RadialSlot slot : layer.slots) {
            if (slot.serial > 0) {
                continue;
            }
            int serial = 1;
            while (used.contains(serial)) {
                serial++;
            }
            slot.serial = serial;
            used.add(serial);
        }
        int maxUsed = 0;
        for (int serial : used) {
            maxUsed = Math.max(maxUsed, serial);
        }
        layer.nextSerial = Math.max(layer.nextSerial, maxUsed + 1);
        layer.freeSerials.removeIf(serial -> serial == null || serial <= 0 || used.contains(serial));
    }

    private static int allocateSerial(RadialMenuModels.RadialLayer layer) {
        if (layer.freeSerials == null) {
            layer.freeSerials = new ArrayList<>();
        }
        if (layer.nextSerial < 1) {
            layer.nextSerial = 1;
        }
        while (!layer.freeSerials.isEmpty()) {
            Integer recycled = layer.freeSerials.remove(layer.freeSerials.size() - 1);
            if (recycled != null && recycled > 0 && !isSerialInUse(layer, recycled)) {
                return recycled;
            }
        }
        int serial = layer.nextSerial++;
        while (isSerialInUse(layer, serial)) {
            serial = layer.nextSerial++;
        }
        return serial;
    }

    private static void recycleSerial(RadialMenuModels.RadialLayer layer, int serial) {
        if (serial <= 0) {
            return;
        }
        if (layer.freeSerials == null) {
            layer.freeSerials = new ArrayList<>();
        }
        if (!layer.freeSerials.contains(serial) && !isSerialInUse(layer, serial)) {
            layer.freeSerials.add(serial);
        }
    }

    private static boolean isSerialInUse(RadialMenuModels.RadialLayer layer, int serial) {
        for (RadialMenuModels.RadialSlot slot : layer.slots) {
            if (slot.serial == serial) {
                return true;
            }
        }
        return false;
    }

    private static RadialMenuModels.RadialPresetBundle createDefaultBundle() {
        RadialMenuModels.RadialPresetBundle result = new RadialMenuModels.RadialPresetBundle();
        RadialMenuModels.RadialPreset preset = buildDefaultPreset();
        result.presets.add(preset);
        result.editor.activePresetId = preset.id;
        result.editor.selectedLayerId = preset.rootLayerId;
        return result;
    }

    private static RadialMenuModels.RadialPreset buildDefaultPreset() {
        RadialMenuModels.RadialPreset preset = new RadialMenuModels.RadialPreset();
        preset.id = DEFAULT_PRESET_ID;
        preset.name = "default";
        preset.deletable = false;
        preset.rootLayerId = DEFAULT_ROOT_LAYER_ID;

        RadialMenuModels.RadialLayer root = new RadialMenuModels.RadialLayer();
        root.id = DEFAULT_ROOT_LAYER_ID;

        RadialMenuModels.RadialSlot warpmenu = slot("warpmenu_slot", "", "WarpMenu", "", "\u2690");
        RadialMenuModels.RadialSlot ah = slot("ah_slot", "/ah", "Auction", "", "\u2692");
        RadialMenuModels.RadialSlot bz = slot("bz_slot", "/bz", "Bazaar", "", "\u2696");
        root.slots.add(warpmenu);
        root.slots.add(ah);
        root.slots.add(bz);
        preset.layers.put(root.id, root);

        RadialMenuModels.RadialLayer warpRoot = new RadialMenuModels.RadialLayer();
        warpRoot.id = "default_warp_root";
        warpRoot.parentSlotId = warpmenu.id;
        warpmenu.childLayerId = warpRoot.id;

        addCategoryLayer(preset, warpRoot, slot("basic_slot", "", "Basic", "", "\u2B50"), basicDestinations());
        addCategoryLayer(preset, warpRoot, slot("park_slot", "", "Park & Barn", "", "\u2618"), parkDestinations());
        addCategoryLayer(preset, warpRoot, slot("mining_slot", "", "Mining", "", "\u26CF"), miningDestinations());
        addCategoryLayer(preset, warpRoot, slot("others_slot", "", "Others", "", "\uD83C\uDFB2"), othersDestinations());
        addCategoryLayer(preset, warpRoot, slot("crimson_slot", "", "Crimson Isle", "", "\u2620"), crimsonDestinations());
        addCategoryLayer(preset, warpRoot, slot("combat_slot", "", "Combat", "", "\u2694"), combatDestinations());

        preset.layers.put(warpRoot.id, warpRoot);
        return preset;
    }

    private static void addCategoryLayer(RadialMenuModels.RadialPreset preset, RadialMenuModels.RadialLayer parent,
                                         RadialMenuModels.RadialSlot categorySlot, List<DestinationSeed> destinations) {
        parent.slots.add(categorySlot);
        RadialMenuModels.RadialLayer layer = new RadialMenuModels.RadialLayer();
        layer.id = "default_" + categorySlot.id;
        layer.parentSlotId = categorySlot.id;
        categorySlot.childLayerId = layer.id;
        for (DestinationSeed destination : destinations) {
            layer.slots.add(slot(
                    destination.id,
                    destination.command,
                    destination.displayName,
                    destination.icon,
                    ""
            ));
        }
        preset.layers.put(layer.id, layer);
    }

    private static RadialMenuModels.RadialSlot slot(String id, String command, String displayName, String icon, String unicodeIcon) {
        RadialMenuModels.RadialSlot slot = new RadialMenuModels.RadialSlot();
        slot.id = id;
        slot.command = command;
        slot.displayName = displayName;
        slot.icon = icon;
        slot.unicodeIcon = unicodeIcon;
        return slot;
    }

    private record DestinationSeed(String id, String displayName, String command, String icon) {
    }

    private static List<DestinationSeed> basicDestinations() {
        return List.of(
                dest("hub", "Hub", "hub", "hub"),
                dest("community_center", "Community Center", "warp elizabeth", "community_center"),
                dest("museum", "Museum", "warp museum", "museum"),
                dest("wizard_tower", "Wizard Tower", "warp tower", "wizard_tower"),
                dest("private_island", "Private Island", "is", "private_island"),
                dest("castle", "Castle", "warp castle", "castle"),
                dest("sirius_shack", "Sirius' Shack", "warp da", "sirius_shack"),
                dest("crypts", "Crypts", "warp crypt", "crypts")
        );
    }

    private static List<DestinationSeed> parkDestinations() {
        return List.of(
                dest("the_park", "The Park", "warp park", "the_park"),
                dest("jungle", "Jungle", "warp jungle", "jungle"),
                dest("howling_cave", "Howling Cave", "warp howl", "howling_cave"),
                dest("murkwater_loch", "Murkwater Loch", "warp murkwater", "murkwater_loch"),
                dest("galatea", "Galatea", "warp galatea", "galatea"),
                dest("the_barn", "The Barn", "warp barn", "the_barn"),
                dest("mushroom_desert", "Mushroom Desert", "warp desert", "mushroom_desert"),
                dest("trappers_den", "Trapper's Den", "warp trap", "trappers_den")
        );
    }

    private static List<DestinationSeed> miningDestinations() {
        return List.of(
                dest("gold_mine", "Gold Mine", "warp gold", "gold_mine"),
                dest("dwarven_mines", "Dwarven Mines", "warp mines", "dwarven_mines"),
                dest("the_forge", "The Forge", "warp forge", "the_forge"),
                dest("dwarven_base_camp", "Dwarven Base Camp", "warp gt", "dwarven_base_camp"),
                dest("crystal_hollows", "Crystal Hollows", "warp ch", "crystal_hollows"),
                dest("crystal_nucleus", "Crystal Nucleus", "warp cn", "crystal_nucleus"),
                dest("deep_caverns", "Deep Caverns", "warp deep", "deep_caverns")
        );
    }

    private static List<DestinationSeed> combatDestinations() {
        return List.of(
                dest("spiders_den", "Spider's Den", "warp spider", "spiders_den"),
                dest("spider_mound", "Spider Mound", "warp top", "spider_mound"),
                dest("arachnes_sanctuary", "Arachne's Sanctuary", "warp arachne", "arachnes_sanctuary"),
                dest("the_end", "The End", "warp end", "the_end"),
                dest("dragons_nest", "Dragon's Nest", "warp drag", "dragons_nest"),
                dest("void_sepulture", "Void Sepulture", "warp void", "void_sepulture")
        );
    }

    private static List<DestinationSeed> crimsonDestinations() {
        return List.of(
                dest("crimson_isle", "Crimson Isle", "warp isle", "crimson_isle"),
                dest("forgotten_skull", "Forgotten Skull", "warp kuudra", "forgotten_skull"),
                dest("the_wasteland", "The Wasteland", "warp wasteland", "the_wasteland"),
                dest("dragontail", "Dragontail", "warp dragontail", "dragontail"),
                dest("scarleton", "Scarleton", "warp scarleton", "scarleton"),
                dest("smoldering_tomb", "Smoldering Tomb", "warp smold", "smoldering_tomb")
        );
    }

    private static List<DestinationSeed> othersDestinations() {
        return List.of(
                dest("dungeon_hub", "Dungeon Hub", "warp dh", "dungeon_hub"),
                dest("wizard_tower_rift", "Wizard Tower (Rift)", "warp rift", "wizard_tower_rift"),
                dest("jerrys_workshop", "Jerry's Workshop", "warp jerry", "jerrys_workshop"),
                dest("lotus_atoll", "Lotus Atoll", "warp lotus", "lotus_atoll"),
                dest("backwater_bayou", "Backwater Bayou", "warp bayou", "backwater_bayou"),
                dest("garden", "Garden", "warp garden", "garden")
        );
    }

    private static DestinationSeed dest(String id, String displayName, String command, String icon) {
        return new DestinationSeed(id, displayName, asCommand(command), icon);
    }

    private static String asCommand(String command) {
        if (command == null || command.isBlank() || command.startsWith("/")) {
            return command;
        }
        return "/" + command;
    }
}
