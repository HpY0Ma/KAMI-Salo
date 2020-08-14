package me.zeroeightsix.kami.module;

import com.google.common.base.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.zeroeightsix.kami.KamiMod;
import me.zeroeightsix.kami.event.events.RenderEvent;
import me.zeroeightsix.kami.gui.kami.component.SettingsPanel;
import me.zeroeightsix.kami.gui.rgui.util.ContainerHelper;
import me.zeroeightsix.kami.module.modules.client.CommandConfig;
import me.zeroeightsix.kami.setting.Setting;
import me.zeroeightsix.kami.setting.Settings;
import me.zeroeightsix.kami.setting.builder.SettingBuilder;
import me.zeroeightsix.kami.util.Bind;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static me.zeroeightsix.kami.KamiMod.MODULE_MANAGER;
import static me.zeroeightsix.kami.util.MessageSendHelper.sendChatMessage;

/**
 * Created by 086 on 23/08/2017.
 * Updated by dominikaaaa on 15/04/20
 */
public class Module {

    private final String originalName = getAnnotation().name();
    private final Category category = getAnnotation().category();
    private final String description = getAnnotation().description();
    private final Setting<String> name = register(Settings.s("Name", originalName));
    private Setting<Bind> bind = register(Settings.custom("Bind", Bind.none(), new BindConverter()).build());
    private Setting<Boolean> enabled = register(Settings.booleanBuilder("Enabled").withVisibility(aBoolean -> false).withValue(false).build());
    private Setting<ShowOnArray> showOnArray = register(Settings.e("Visible", getAnnotation().showOnArray()));

    public boolean alwaysListening;
    protected static final Minecraft mc = Minecraft.getMinecraft();

    public List<Setting> settingList = new ArrayList<>();

    public Module() {
        alwaysListening = getAnnotation().alwaysListening();
        registerAll(bind, enabled, showOnArray);
    }

    private Info getAnnotation() {
        if (getClass().isAnnotationPresent(Info.class)) {
            return getClass().getAnnotation(Info.class);
        }
        throw new IllegalStateException("No Annotation on class " + this.getClass().getCanonicalName() + "!");
    }

    public void onUpdate() {}

    public void onRender() {}

    public void onWorldRender(RenderEvent event) {}

    public Bind getBind() {
        return bind.getValue();
    }

    public enum ShowOnArray {
        ON, OFF
    }

    public ShowOnArray getShowOnArray() {
        return showOnArray.getValue();
    }

    public String getBindName() {
        return bind.getValue().toString();
    }

    public void setName(String name) {
        this.name.setValue(name);
    }

    public String getOriginalName() {
        return originalName;
    }

    /**
     * @see me.zeroeightsix.kami.command.commands.GenerateWebsiteCommand
     * @see me.zeroeightsix.kami.module.modules.client.ActiveModules
     */
    public enum Category {
        CHAT("Chat", false),
        COMBAT("Combat", false),
        EXPERIMENTAL("Experimental", false),
        CLIENT("Client", false),
        HIDDEN("Hidden", true),
        MISC("Misc", false),
        MOVEMENT("Movement", false),
        PLAYER("Player", false),
        RENDER("Render", false);

        boolean hidden;
        String name;

        Category(String name, boolean hidden) {
            this.name = name;
            this.hidden = hidden;
        }

        public boolean isHidden() {
            return hidden;
        }
        public String getName() {
            return name;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Info {
        String name();
        String description();
        Module.Category category();
        boolean alwaysListening() default false;
        ShowOnArray showOnArray() default ShowOnArray.ON;
    }

    public String getName() {
        return name.getValue();
    }

    public String getChatName() {
        return "[" + name.getValue() + "]";
    }

    public String getDescription() { return description; }

    public Category getCategory() { return category; }

    public boolean isEnabled() { return enabled.getValue(); }

    public boolean isOnArray() { return showOnArray.getValue().equals(ShowOnArray.ON); }

    public boolean isProduction() { return name.getValue().equals("clickGUI") || (!category.equals(Category.EXPERIMENTAL) && !category.equals(Category.HIDDEN)); }

    protected void onEnable() {}

    protected void onDisable() {}

    protected void onToggle() {
        if (!name.getValue().equals("clickGUI") && MODULE_MANAGER.getModuleT(CommandConfig.class).toggleMessages.getValue()) {
            sendChatMessage(name.getValue() + (enabled.getValue() ? " &aenabled" : " &cdisabled"));
        }
    }

    public void toggle() { setEnabled(!isEnabled()); }

    public void enable() {
        enabled.setValue(true);
        onEnable();
        onToggle();
        if (!alwaysListening)
            KamiMod.EVENT_BUS.subscribe(this);
    }

    public void disable() {
        enabled.setValue(false);
        onDisable();
        onToggle();
        if (!alwaysListening)
            KamiMod.EVENT_BUS.unsubscribe(this);
    }

    public boolean isDisabled() {
        return !isEnabled();
    }

    public void setEnabled(boolean enabled) {
        boolean prev = this.enabled.getValue();
        if (prev != enabled)
            if (enabled)
                enable();
            else
                disable();
    }

    public String getHudInfo() {
        return null;
    }

    protected final void setAlwaysListening(boolean alwaysListening) {
        this.alwaysListening = alwaysListening;
        if (alwaysListening) KamiMod.EVENT_BUS.subscribe(this);
        if (!alwaysListening && isDisabled()) KamiMod.EVENT_BUS.unsubscribe(this);
    }

    /**
     * Cleanup method in case this module wants to do something when the client closes down
     */
    public void destroy() {
    }

    protected void registerAll(Setting... settings) {
        for (Setting setting : settings) {
            register(setting);
        }
    }

    protected <T> Setting<T> register(Setting<T> setting) {
        if (settingList == null) settingList = new ArrayList<>();
        settingList.add(setting);
        return SettingBuilder.register(setting, "modules." + originalName);
    }

    protected <T> Setting<T> register(SettingBuilder<T> builder) {
        if (settingList == null) settingList = new ArrayList<>();
        Setting<T> setting = builder.buildAndRegister("modules." + name);
        settingList.add(setting);
        return setting;
    }


    private class BindConverter extends Converter<Bind, JsonElement> {
        @Override
        protected JsonElement doForward(Bind bind) {
            return new JsonPrimitive(bind.toString());
        }

        @Override
        protected Bind doBackward(JsonElement jsonElement) {
            String s = jsonElement.getAsString();
            if (s.equalsIgnoreCase("None")) return Bind.none();
            boolean ctrl = false, alt = false, shift = false;

            if (s.startsWith("Ctrl+")) {
                ctrl = true;
                s = s.substring(5);
            }
            if (s.startsWith("Alt+")) {
                alt = true;
                s = s.substring(4);
            }
            if (s.startsWith("Shift+")) {
                shift = true;
                s = s.substring(6);
            }

            int key = -1;
            try {
                key = Keyboard.getKeyIndex(s.toUpperCase());
            } catch (Exception ignored) {
            }

            if (key == 0) return Bind.none();
            return new Bind(ctrl, alt, shift, key);
        }
    }
}
