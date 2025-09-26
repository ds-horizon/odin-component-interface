package com.dream11.spec

import com.dream11.ExecutionContext
import com.dream11.OdinUtil
import com.dream11.exec.CommandResponse
import com.dream11.exec.FileCommandExecutor
import com.dream11.storage.StorageConfig
import com.dream11.storage.StorageConfigParser
import com.dream11.validation.BeanValidator
import groovy.util.logging.Slf4j

import static com.dream11.OdinUtil.mustExistProperty

@Slf4j
class FileDownloadSpec extends BaseCommand {

    private String provider
    private String uri
    private String relativeDestination
    private CredentialsSpec credentials
    private StorageConfig storageConfig

    FileDownloadSpec(FlavourSpec flavour) {
        super(flavour)
    }

    void attributes(Map<String, Object> attrs) {
        this.storageConfig = StorageConfigParser.parse(provider, OdinUtil.getObjectMapper().valueToTree(attrs))
    }

    void provider(String provider) {
        this.provider = provider
    }

    void uri(String uri) {
        this.uri = uri
    }

    void relativeDestination(String relativeDestination) {
        this.relativeDestination = relativeDestination
    }

    void credentials(@DelegatesTo(CredentialsSpec) Closure cl) {
        this.credentials = new CredentialsSpec(getFlavour())
        def code = cl.rehydrate(credentials, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
    }

    @Override
    protected CommandResponse executeCommand(ExecutionContext context) {
        return FileCommandExecutor.download(this, context.getWorkingDir())
    }

    @Override
    void validate(ExecutionContext context) {
        mustExistProperty(() -> provider == null || provider.isEmpty(), "download block in ${getFlavour().getFlavour()} flavour", "provider")
        mustExistProperty(() -> uri == null || uri.isEmpty(), "download block in ${getFlavour().getFlavour()} flavour", "uri")

        if (this.storageConfig != null) {
            BeanValidator.validate(this.storageConfig, "${provider}StorageConfig")
        }

        if (this.credentials != null) {
            this.credentials.validate(context)
        }
    }
}
