// holds our config
class AndroidConfig {
    def config = [:]
    
    def target(String gradleTarget) {
        config.targets = gradleTarget
    }
    
    def gradleOpts(String opts) {
        config.gradleOpts = opts
    }
    
    def credential(String id, String variable) {
        def credentials = config.credentials ?: []
        credentials.add([id, variable])
        config.credentials = credentials
    }
    
    def sdkPackage(String what) {
        //TODO sanitize input somewhat
        def sdkPackages = config.sdkPackages ?: []
        sdkPackages.add(what)
        config.sdkPackages = sdkPackages
    }
}

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = new AndroidConfig()
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def dockerFilePath
    
	// If the project doesn't contain a Dockerfile, copy the one in this library and setup a dockerignore file in a .build directory.
	// Otherwise, use the file within the project.
    if (!fileExists ('Dockerfile')) {
        println 'Using the default Android build Docker image.'
        def dockerFile = libraryResource 'android/Dockerfile'
        writeFile file: '.build/Dockerfile', text: dockerFile
        writeFile file: '.build/dockerignore', text: '*' 
        dockerFilePath = '.build/'
    } else {
        dockerFilePath = '.'
    }
    
    def targets = config.config.targets ?: "assemDebug"
	// For every credential setup a Gradle project property by setting the appropriate enviroment variable.
	// Currently this needs to be a single word, otherwise setting the env var will probably not work.
    def credentials = (config.config.credentials ?: []).collect { string(credentialsId: it[0], variable: "ORG_GRADLE_PROJECT_${it[1]}") }
    def sdkPackages = ""
    if (!(config.config.sdkPackages ?: []).empty) {
        sdkPackages = "--build-arg SDK_PACKAGES='${config.config.sdkPackages.join(' ')}'"
    }
    def image = docker.build("android", "$sdkPackages $dockerFilePath")
    image.inside {        
        if (credentials.empty) {
            runBuild(targets)
        } else {
			// if we have credentials, pass them all to the credentials plugin and run the build within that context
            withCredentials(credentials) {
                runBuild(targets)
            }
        }
    }
}

def runBuild(targets) {
    sh "./gradlew $targets"
    archiveArtifacts allowEmptyArchive: true, artifacts: '**/build/outputs/**/*.apk, **/build/outputs/**/*.aab, **/build/outputs/mapping/**/mapping.txt', onlyIfSuccessful: true
    junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
    androidLint()
}