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
import org.yaml.snakeyaml.error.MissingEnvironmentVariableException;
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
            // XMine: allow environment variable substitution in config files. Substitution
            // happens ONLY on scalars explicitly tagged !ENV, so untagged values keep their
            // literal text and every existing config keeps parsing exactly as before:
            //     password: !ENV ${MYSQL_PASSWORD}
            // See StrictEnvScalarConstructor below for the unset-variable policy.
            //
            // new Yaml(BaseConstructor) takes the LoaderOptions from the constructor, so the
            // code point limit above is kept. The dumper is irrelevant here: save() builds
            // its own Yaml instance.
            Yaml yaml = new Yaml(new StrictEnvScalarConstructor(loaderOptions));
            values = yaml.load(input);
            if (values == null) values = new LinkedHashMap<>();
            input.close();
        } catch (MissingEnvironmentVariableException e) {
            // Reported separately from the generic YAMLException below: the file is not
            // broken, the environment it was started in is.
            if (input != null) input.close();
            TAB tab = TAB.getInstance();
            tab.setBrokenFile(destination.getName());
            tab.getPlatform().logWarn(new TabTextComponent("File " + destination + " uses an environment variable that is not set.", TabTextColor.RED));
            tab.getPlatform().logInfo(new TabTextComponent(e.getMessage(), TabTextColor.GOLD));
            tab.getPlatform().logInfo(new TabTextComponent("Set the variable before starting the server, or write a default into the config as ${VARIABLE:-default}.", TabTextColor.GOLD));
            throw e;
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

    /**
     * XMine: {@link EnvScalarConstructor} with a fail-loud policy for unset variables.
     *
     * <p>Upstream resolves a bare {@code ${VAR}} of an unset or empty variable to an empty
     * string. For us that is a step backwards from the shell substitution it replaces: an
     * empty database password produces a connection failure far away from its cause, and
     * the config looks fine while the server is down. So a bare {@code ${VAR}} now aborts
     * loading with {@link MissingEnvironmentVariableException}, naming the variable.
     *
     * <p>All the forms that state an intent are left to upstream and keep working:
     * {@code ${VAR:-default}} and {@code ${VAR-default}} substitute the default,
     * {@code ${VAR:-}} deliberately yields an empty string, and {@code ${VAR:?}} /
     * {@code ${VAR?}} keep their own error messages.
     */
    private static class StrictEnvScalarConstructor extends EnvScalarConstructor {

        StrictEnvScalarConstructor(@NonNull LoaderOptions loaderOptions) {
            // Object.class as the root type keeps rootTag untouched, so this behaves exactly
            // like the default Constructor that new Yaml(loaderOptions) used to build. The
            // no-argument EnvScalarConstructor() could not be used: it creates its own
            // LoaderOptions and would drop the code point limit set above.
            super(new TypeDescription(Object.class), null, loaderOptions);
        }

        @Override
        public String apply(String name, String separator, String value, String environment) {
            // separator == null means the config wrote a bare ${VAR}: no default, no explicit
            // "?" error form. Anything else is an intent upstream already handles correctly.
            if (separator == null && (environment == null || environment.isEmpty())) {
                throw new MissingEnvironmentVariableException("Environment variable " + name +
                        " is not set (or is empty). Set it, or write ${" + name + ":-default} to allow a fallback.");
            }
            return super.apply(name, separator, value, environment);
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