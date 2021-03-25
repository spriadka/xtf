package cz.xtf.core.openshift;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import cz.xtf.core.config.OpenShiftConfig;
import cz.xtf.core.config.XTFConfig;
import cz.xtf.core.http.Https;

public class HelmClients {

    private static final String HELM_CLIENTS_URL = "https://get.helm.sh";
    private static final String HELM_FILE_NAME = "helm.tar.gz";

    private static HelmBinary adminHelmBinary;
    private static HelmBinary masterHelmBinary;

    public static HelmBinary admin() {
        if (adminHelmBinary == null) {
            adminHelmBinary = getBinary(OpenShiftConfig.adminToken(),
                    OpenShiftConfig.adminUsername(),
                    OpenShiftConfig.adminPassword(),
                    OpenShiftConfig.adminKubeconfig(),
                    OpenShiftConfig.namespace());
        }
        return adminHelmBinary;
    }

    public static HelmBinary master() {
        if (masterHelmBinary == null) {
            masterHelmBinary = getBinary(OpenShiftConfig.masterToken(),
                    OpenShiftConfig.masterUsername(),
                    OpenShiftConfig.masterPassword(),
                    OpenShiftConfig.masterKubeconfig(),
                    OpenShiftConfig.namespace());
        }
        return masterHelmBinary;
    }

    private static HelmBinary getBinary(String token, String username, String password, String kubeconfig,
            String namespace) {
        OpenShiftBinary openShiftBinary = OpenShifts.getBinary(token, username, password, kubeconfig, namespace);
        return new HelmBinary(downloadHelmBinary(), openShiftBinary.getOcConfigPath());

    }

    private static String downloadHelmBinary() {
        String systemType = "";
        String helmClientVersion = XTFConfig.get("helm.client.version", latestHelmVersion());
        if (SystemUtils.IS_OS_MAC) {
            systemType = "mac";
        } else {
            systemType = "linux";
        }
        //https://get.helm.sh/helm-v3.5.3-darwin-amd64.tar.gz
        String architecture = System.getProperty("os.arch");
        String helmClientUrl = String.format("%s/helm-%s-%s-%s.tar.gz", HELM_CLIENTS_URL, helmClientVersion, systemType,
                architecture);
        return downloadHelmBinaryInternal(helmClientVersion, helmClientUrl, true);
    }

    private static String latestHelmVersion() {
        String gitApiResponse = Https.getContent("https://api.github.com/repos/helm/helm/releases/latest");
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(gitApiResponse, JsonObject.class);
        return jsonObject.get("tag_name").getAsString();
    }

    private static String downloadHelmBinaryInternal(final String version, final String helmClientUrl, final boolean trustAll) {
        int code = Https.httpsGetCode(helmClientUrl);

        if (code != 200) {
            throw new IllegalStateException("Client binary for version " + version + " isn't available at " + helmClientUrl);
        }

        File workdir = helmBinaryFolder();

        // Download and extract client
        File helmTarFile = new File(workdir, HELM_FILE_NAME);
        File helmFile = new File(workdir, "helm");

        try {
            URL requestUrl = new URL(helmClientUrl);

            if (trustAll) {
                Https.copyHttpsURLToFile(requestUrl, helmTarFile, 20_000, 300_000);
            } else {
                FileUtils.copyURLToFile(requestUrl, helmTarFile, 20_000, 300_000);
            }

            CLIUtils.executeCommand("tar", "-xf", helmTarFile.getPath(), "-C", workdir.getPath(), "--strip", "1");
            FileUtils.deleteQuietly(helmTarFile);

            return helmFile.getAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to download and extract oc binary from " + helmClientUrl, e);
        }
    }

    private static File helmBinaryFolder() {
        File workdir = new File(Paths.get("tmp/helm").toAbsolutePath().toString());
        if (workdir.exists()) {
            return workdir;
        }
        if (!workdir.mkdirs()) {
            throw new IllegalStateException("Cannot mkdirs " + workdir);
        }
        return workdir;
    }
}
