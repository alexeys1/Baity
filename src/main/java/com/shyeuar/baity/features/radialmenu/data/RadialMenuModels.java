package com.shyeuar.baity.features.radialmenu.data;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class RadialMenuModels {

    public static final int MAX_TOP_LEVEL_PRESETS = 5;
    public static final int MAX_CUSTOM_PRESETS = MAX_TOP_LEVEL_PRESETS - 1;
    public static final int MAX_LAYER_DEPTH = 4;
    public static final int MAX_SLOTS_PER_LAYER = 8;
    public static final int MAX_UNICODE_ICON_CODE_POINTS = 2;

    private RadialMenuModels() {
    }

    public static final class RadialSlot {
        public String id = UUID.randomUUID().toString();
        public int serial;
        public String command = "";
        public String displayName = "";
        public String icon = "";
        public String unicodeIcon = "";
        public String childLayerId;
    }

    public static final class RadialLayer {
        public String id = UUID.randomUUID().toString();
        public String parentSlotId;
        public List<RadialSlot> slots = new ArrayList<>();
        public int nextSerial = 1;
        public List<Integer> freeSerials = new ArrayList<>();
    }

    public static final class RadialPreset {
        public String id = UUID.randomUUID().toString();
        public String name;
        public boolean deletable = true;
        public String rootLayerId;
        public Map<String, RadialLayer> layers = new HashMap<>();
    }

    public static final class EditorState {
        public String activePresetId;
        public String selectedLayerId;
        public int selectedSlotIndex = -1;
        public Set<String> expandedNodes = new HashSet<>();
    }

    public static final class RadialPresetBundle {
        public List<RadialPreset> presets = new ArrayList<>();
        public EditorState editor = new EditorState();

        public RadialPreset findPreset(String presetId) {
            if (presetId == null) {
                return null;
            }
            for (RadialPreset preset : presets) {
                if (presetId.equals(preset.id)) {
                    return preset;
                }
            }
            return null;
        }

        public RadialPreset activePreset() {
            return findPreset(editor.activePresetId);
        }
    }
}
