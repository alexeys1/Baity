package com.shyeuar.baity.utils;

import com.shyeuar.baity.gui.module.Module;
import com.shyeuar.baity.gui.value.GroupValue;
import com.shyeuar.baity.gui.value.Option;
import com.shyeuar.baity.gui.value.Value;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModuleUtils {
    
    public static boolean getOptionBoolean(Module module, String name, boolean def) {
        if (module == null) {
            return def;
        }
        
        Value v = findValueByName(module.getValues(), name);
        if (!(v instanceof Option)) {
            return def;
        }
        if (!shouldExecuteSubModule(module, v)) {
            if (module.isEnabled()) {
                return false;
            }
            return def;
        }
        Object val = v.getValue();
        return val instanceof Boolean ? (Boolean) val : def;
    }
    
    public static boolean getOptionBooleanRaw(Module module, String name, boolean def) {
        if (module == null) {
            return def;
        }
        
        Value v = findValueByName(module.getValues(), name);
        if (!(v instanceof Option)) {
            return def;
        }
        Object val = v.getValue();
        return val instanceof Boolean ? (Boolean) val : def;
    }
    
    public static String getOptionString(Module module, String name, String def) {
        if (module == null) {
            return def;
        }
        
        Value v = findValueByName(module.getValues(), name);
        if (v == null) {
            return def;
        }
        if (!shouldExecuteSubModule(module, v)) {
            return def;
        }
        Object val = v.getValue();
        return val != null ? val.toString() : def;
    }
   
    public static String getOptionStringRaw(Module module, String name, String def) {
        if (module == null) {
            return def;
        }
        
        Value v = findValueByName(module.getValues(), name);
        if (v == null) {
            return def;
        }
        Object val = v.getValue();
        return val != null ? val.toString() : def;
    }
    
    public static Module getEnabledModule(String moduleName) {
        Module module = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName(moduleName);
        if (module == null || !module.isEnabled()) {
            return null;
        }
        return module;
    }
   
    public static boolean shouldExecuteSubModule(Module module, Value value) {
        if (value == null) {
            return false;
        }
        
        if (value.isIndependentOfParentModule()) {
            return true;
        }
        
        if (module == null || !module.isEnabled()) {
            return false;
        }
        return passesAllAncestorGroupSwitches(module, value);
    }
    
    public static boolean shouldExecuteSubModule(String moduleName, String valueName) {
        Module module = com.shyeuar.baity.gui.module.ModuleManager.getModuleByName(moduleName);
        if (module == null) {
            return false;
        }
        
        Value value = findValueByName(module.getValues(), valueName);
        return value != null && shouldExecuteSubModule(module, value);
    }

    private static Value findValueByName(Iterable<Value> values, String name) {
        if (values == null || name == null) {
            return null;
        }
        for (Value v : values) {
            if (v == null) continue;
            if (v.getName() != null && v.getName().equalsIgnoreCase(name)) {
                return v;
            }
            if (v instanceof GroupValue group) {
                Value nested = findValueByName(group.getChildren(), name);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static boolean passesAllAncestorGroupSwitches(Module module, Value target) {
        Value cursor = target;
        while (cursor != null) {
            GroupValue parent = findImmediateParentGroup(module.getValues(), cursor);
            if (parent == null) {
                break;
            }
            String switchName = parent.getSubModuleSwitchChildName();
            if (switchName != null && !switchName.isEmpty()) {
                Value switchChild = findNamedDirectChild(parent, switchName);
                if (switchChild instanceof Option opt) {
                    Object sv = opt.getValue();
                    boolean switchOn = sv instanceof Boolean && (Boolean) sv;
                    boolean isThisGroupsSwitchOption = false;
                    if (cursor instanceof Option && switchName.equals(cursor.getName())) {
                        for (Value c : parent.getChildren()) {
                            if (c == cursor) {
                                isThisGroupsSwitchOption = true;
                                break;
                            }
                        }
                    }
                    if (!switchOn && !isThisGroupsSwitchOption) {
                        return false;
                    }
                }
            }
            cursor = parent;
        }
        return true;
    }

    private static GroupValue findImmediateParentGroup(Iterable<Value> roots, Value target) {
        if (roots == null || target == null) {
            return null;
        }
        for (Value root : roots) {
            GroupValue p = findImmediateParentUnderNode(root, target);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    private static GroupValue findImmediateParentUnderNode(Value node, Value target) {
        if (node instanceof GroupValue g) {
            for (Value c : g.getChildren()) {
                if (c == target) {
                    return g;
                }
                GroupValue deeper = findImmediateParentUnderNode(c, target);
                if (deeper != null) {
                    return deeper;
                }
            }
        }
        return null;
    }

    private static Value findNamedDirectChild(GroupValue group, String childName) {
        if (group == null || childName == null) {
            return null;
        }
        for (Value c : group.getChildren()) {
            if (c != null && childName.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }
}
