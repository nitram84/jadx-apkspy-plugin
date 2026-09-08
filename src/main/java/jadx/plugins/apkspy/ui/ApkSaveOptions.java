package jadx.plugins.apkspy.ui;

public class ApkSaveOptions {
	private boolean keepOnErrors = true;
	private boolean cleanOnSuccess = true;
	private boolean createDebugableApk;
	private boolean addNetworkSecurityConfiguration;

	public boolean isKeepOnErrors() {
		return keepOnErrors;
	}

	public void setKeepOnErrors(boolean keepOnErrors) {
		this.keepOnErrors = keepOnErrors;
	}

	public boolean isCleanOnSuccess() {
		return cleanOnSuccess;
	}

	public void setCleanOnSuccess(boolean cleanOnSuccess) {
		this.cleanOnSuccess = cleanOnSuccess;
	}

	public boolean isCreateDebugableApk() {
		return createDebugableApk;
	}

	public void setCreateDebugableApk(boolean createDebugableApk) {
		this.createDebugableApk = createDebugableApk;
	}

	public boolean isAddNetworkSecurityConfiguration() {
		return addNetworkSecurityConfiguration;
	}

	public void setAddNetworkSecurityConfiguration(boolean addNetworkSecurityConfiguration) {
		this.addNetworkSecurityConfiguration = addNetworkSecurityConfiguration;
	}
}
