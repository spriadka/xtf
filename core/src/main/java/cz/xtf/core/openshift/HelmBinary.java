package cz.xtf.core.openshift;

import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelmBinary {

    private final String path;

    @Getter
    private String ocConfigPath;

    public HelmBinary(String path) {
        this.path = path;
    }

    public HelmBinary(String path, String ocConfigPath) {
        this.path = path;
        this.ocConfigPath = ocConfigPath;
    }

    public String execute(String... args) {
        String[] prefix = Optional.ofNullable(ocConfigPath).map(ocConfig -> new String[] { path, "--kubeconfig", ocConfig })
                .orElseGet(() -> new String[] { path });
        return CLIUtils.executeCommand(ArrayUtils
                .addAll(prefix, args));
    }

}
