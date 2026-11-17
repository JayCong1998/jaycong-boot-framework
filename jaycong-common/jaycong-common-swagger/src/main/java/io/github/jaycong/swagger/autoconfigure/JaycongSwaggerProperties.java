package io.github.jaycong.swagger.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 文档元信息；扫描范围、开关、分组等继续使用原生 springdoc 配置。 */
@ConfigurationProperties("jaycong.swagger")
public class JaycongSwaggerProperties {
    private String title = "API 文档";
    private String description = "";
    private String version = "v1";
    private String serverUrl = "/";

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
}
