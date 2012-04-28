
if (args.length == 0) {
  println "Please provide the path to the zip containing all the LibGDX info"
  return
}

def file = new File(args[0])
def configFile = new File(args[1])

if (!file.exists()) {
  println "Could not locate zip file: ${zipFile}"
  return
} else if (!configFile.exists()) {
  println "Could not locate config file: ${configFile}"
  return
}

def config = new Properties()
config.load(new FileInputStream(configFile))

def groupId = config.get("groupId")
def version = config.get("version")

def zipFile = new java.util.zip.ZipFile(file)
zipFile.entries().findAll { !it.directory }.each { entry ->
   if (config.containsKey(entry.getName())) {
     deployArtifact(zipFile, entry, groupId, version, config.get(entry.getName()))
   }
}

def deployArtifact(zipFile, entry, groupId, version, config) {
  def parts = config.split(",")  
  def artifactId = parts[0]
  def packaging = parts[1]
  def classifier = parts[2]
  def file = extractToFile(zipFile, entry)

  def url="https://b2s-repo.googlecode.com/svn/trunk/mvn-repo"
  def cmd="mvn install:install-file -e -DgroupId=${groupId} -DartifactId=${artifactId} -Dversion=${version} -Dfile=${file.absolutePath} -Dpackaging=${packaging}"
  if (classifier) {
    cmd += " -Dclassifier=${classifier}"
  }
  def output = cmd.execute().text
  if (output.contains("BUILD FAILURE") || output.contains("BUILD ERROR")) {
    throw new IllegalStateException("Failed deploying artifact\n\n${output}")
  }
  println "Installed: ${groupId}:${artifactId}:${version}:${packaging}:${classifier}"
}

def extractToFile(zipFile, entry) {
  int indexOfSlash = entry.name.lastIndexOf('/')
  def filename = entry.name
  if (indexOfSlash != -1) {
    filename = filename.substring(indexOfSlash+1)
  }

  def subDir = new File(System.getProperty("user.home"))
  subDir.mkdirs()  

  def file = new File(subDir, filename)
  file.deleteOnExit()
  file <<  zipFile.getInputStream(entry).bytes
  
  file
}
