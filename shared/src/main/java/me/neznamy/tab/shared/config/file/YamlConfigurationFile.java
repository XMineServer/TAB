package me.neznamy.tab.shared.config.file;

import lombok.NonNull;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.chat.TabTextColor;
import me.neznamy.tab.shared.chat.component.TabTextComponent;
import me.neznamy.yamlassist.YamlAssist;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.env.EnvScalarConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * YAML implementation of ConfigurationFile
 */
public class YamlConfigurationFile extends ConfigurationFile {

    /**
     * Constructs new instance and attempts to load specified configuration file.
     * If file does not exist, default file is copied from {@code source}.
     *
     * @param   source
     *          Source to copy file from if it does not exist
     * @param   destination
     *          File destination to use
     * @throws  IllegalArgumentException
     *          if {@code destination} is null
     * @throws  IllegalStateException
     *          if file does not exist and source is null
     * @throws  YAMLException
     *          if file has invalid YAML syntax
     * @throws  IOException
     *          if I/O operation with the file unexpectedly fails
     */
    public YamlConfigurationFile(@Nullable InputStream source, @NonNull File destination) throws IOException {
        super(source, destination);
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            LoaderOptions loaderOptions = new LoaderOptions();
            loaderOptions.setCodePointLimit(Integer.MAX_VALUE);
            // XMine: allow environment variable substitution in config files.
            //
            // EnvScalarConstructor resolves ${VAR} and ${VAR:-default} but ONLY on scalars
            // explicitly tagged !ENV, so untagged values keep their literal text and every
            // existing config keeps parsing exactly as before. Example:
            //     password: !ENV ${MYSQL_PASSWORD}
            //
            // The 3-argument constructor is used instead of the no-arg one so that our
            // loaderOptions (code point limit) is kept: the no-arg one builds its own
            // LoaderOptions internally. Passing Object.class as the root type makes it
            // behave exactly like the default `new Constructor(loaderOptions)` that
            // `new Yaml(loaderOptions)` used to create - Constructor leaves rootTag alone
            // for Object.class - with the !ENV tag added on top.
            //
            // new Yaml(BaseConstructor) picks the LoaderOptions up from the constructor,
            // and the dumper is irrelevant here: save() builds its own Yaml instance.
            Yaml yaml = new Yaml(new EnvScalarConstructor(new TypeDescription(Object.class), null, loaderOptions));
            values = yaml.load(input);
            if (values == null) values = new LinkedHashMap<>();
            input.close();
        } catch (YAMLException e) {
            if (input != null) input.close();
            TAB tab = TAB.getInstance();
            tab.setBrokenFile(destination.getName());
            tab.getPlatform().logWarn(new TabTextComponent("File " + destination + " has broken syntax.", TabTextColor.RED));
            tab.getPlatform().logInfo(new TabTextComponent("Error message from yaml parser: " + e.getMessage(), TabTextColor.GOLD));
            List<String> suggestions = YamlAssist.getSuggestions(file);
            if (!suggestions.isEmpty()) {
                tab.getPlatform().logInfo(new TabTextComponent("Suggestions to fix yaml syntax:", TabTextColor.LIGHT_PURPLE));
                for (String suggestion : suggestions) {
                    tab.getPlatform().logInfo(new TabTextComponent("- " + suggestion, TabTextColor.LIGHT_PURPLE));
                }
            }
            throw e;
        }
    }

    @Override
    public synchronized void save() {
        try {
            Writer writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            new Yaml(options).dump(values, writer);
            writer.close();
        } catch (IOException e) {
            TAB.getInstance().getPlatform().logWarn(new TabTextComponent(String.format(
                    "Failed to save yaml file %s: %s: %s",
                    file.getPath(), e.getClass().getName(), e.getMessage()
            ), TabTextColor.RED));
        }
    }
}