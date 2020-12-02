package be.taguan.kubetest.kubernetesapiconsumer;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class DemoRestController {

    private String configMapYaml;
    private String deploymentYaml;
    private String serviceYaml;

    @Value("${simulator.bundle.location}")
    private Path bundlePath;

    @PostConstruct
    public void init() throws IOException {
        this.configMapYaml = IOUtils.toString(new ClassPathResource("simulator-configmap.yaml").getInputStream(), StandardCharsets.UTF_8);
        this.deploymentYaml = IOUtils.toString(new ClassPathResource("simulator-depl.yaml").getInputStream(), StandardCharsets.UTF_8);
        this.serviceYaml = IOUtils.toString(new ClassPathResource("simulator-service.yaml").getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     * Stupid POST method to write the content of a request paramèter named bundleContext
     * in a file (bundleName) in configured location.
     *
     * In reality, this should properly take the content of a zip file in its body.
     * @param bundleName the name of the bundle
     * @param bundleContent the string content to write at the given location
     */
    @PostMapping("/bundle/{bundleName}")
    public void postFakeBundle(@PathVariable String bundleName, @RequestParam String bundleContent) throws IOException {
        Path bundleFilePath = bundlePath.resolve(bundleName);
        Files.deleteIfExists(bundleFilePath);
        Files.writeString(bundleFilePath, bundleContent);
    }

    @PutMapping("/bundle/{bundleName}")
    public void createBundle(@PathVariable String bundleName) throws IOException, ApiException, InterruptedException {
        Path bundleFilePath = bundlePath.resolve(bundleName);

        ApiClient apiClient = Config.defaultClient();
        Configuration.setDefaultApiClient(apiClient);

        CoreV1Api coreApi = new CoreV1Api();
        AppsV1Api appsApi = new AppsV1Api();

        V1ConfigMap configMap = Yaml.loadAs(this.configMapYaml.replaceAll("REPLACE-THIS", bundleName), V1ConfigMap.class);
        coreApi.createNamespacedConfigMap("default", configMap, null, null, null);

        V1Deployment deployment = Yaml.loadAs(this.deploymentYaml.replaceAll("REPLACE-THIS", bundleName), V1Deployment.class);
        appsApi.createNamespacedDeployment("default", deployment, null, null, null);

        V1Service service = Yaml.loadAs(this.serviceYaml.replaceAll("REPLACE-THIS", bundleName), V1Service.class);
        coreApi.createNamespacedService("default", service, null, null, null);

    }

    @DeleteMapping("/bundle/{bundleName}")
    public void deleteBundle(@PathVariable String bundleName) throws IOException, ApiException {
        ApiClient apiClient = Config.defaultClient();
        Configuration.setDefaultApiClient(apiClient);

        CoreV1Api coreApi = new CoreV1Api();
        AppsV1Api appsApi = new AppsV1Api();

        appsApi.deleteNamespacedDeployment(bundleName + "-depl", "default", null, null, null,null, null, null);
        coreApi.deleteNamespacedService(bundleName + "-svc", "default", null, null, null, null, null, null);
        coreApi.deleteNamespacedConfigMap(bundleName + "-cm", "default", null, null, null,null, null, null);

        Path bundleFilePath = bundlePath.resolve(bundleName);
        Files.deleteIfExists(bundleFilePath);
    }

}
