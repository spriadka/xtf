package cz.xtf.core.openshift;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelmBinary {

    private final String path;
    private final String helmConfigPath;

    @Getter
    private String ocConfigPath;

    public HelmBinary(String path) {
        this(path, null, null);
    }

    public HelmBinary(String path, String ocConfigPath) {
        this.path = path;
        this.ocConfigPath = ocConfigPath;
        Path helmConfigFile = Paths.get(path).getParent().resolve(".config");
        try {
            helmConfigFile = Files.createDirectories(helmConfigFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.helmConfigPath = helmConfigFile.toAbsolutePath().toString();
    }

    public HelmBinary(String path, String helmConfigPath, String ocConfigPath) {
        this.path = path;
        this.helmConfigPath = helmConfigPath;
        this.ocConfigPath = ocConfigPath;
    }

    public String execute(String... args) {
        String[] prefix = Optional.ofNullable(ocConfigPath)
                .map(ocConfig -> new String[] { path, "--kubeconfig", ocConfig })
                .orElseGet(() -> new String[] { path });
        return CLIUtils.executeCommand(Collections.singletonMap("HELM_CONFIG_HOME", helmConfigPath), ArrayUtils
                .addAll(prefix, args));
    }

}
