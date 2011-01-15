import java.util.zip.*
import java.io.*

if (args.length < 2) {
	println "script {binary-dir} {sourceDir}"
}

def startTime = System.currentTimeMillis()

def lwjglDir = args[0]
def version = lwjglDir.substring(lwjglDir.indexOf('-')+1)

println "\n*** deploy jars ***"
def jarDir = new File(lwjglDir, "jar")

mvnDeploy("lwjgl-jinput", version, new File(jarDir, "jinput.jar"))
mvnDeploy("lwjgl", version, new File(jarDir, "lwjgl.jar"))
mvnDeploy("lwjgl-util", version, new File(jarDir, "lwjgl_util.jar"))

println "\n*** deploy native libraries ***"

// LINUX
mvnDeploy("lwjgl-native", "linux64", version, nativeJar(lwjglDir, "linux", /.+64.+/, "linux64.jar"))
mvnDeploy("lwjgl-native", "linux32", version, nativeJar(lwjglDir, "linux", /.+l\.so|.+x\.so/, "linux32.jar"))

// WINDOWS
mvnDeploy("lwjgl-native", "win64",  version, nativeJar(lwjglDir, "windows", /.+64.+/, "win64.jar"))
mvnDeploy("lwjgl-native", "win32",  version, nativeJar(lwjglDir, "windows", /.+?[^64]\.dll/, "win32.jar"))

// MAC
mvnDeploy("lwjgl-native", "mac", version, nativeJar(lwjglDir, "macosx", /.+lib/, "mac.jar"))


def lwjglSourceDir = new File(args[1])

if (!lwjglSourceDir.exists()) {
	throw new IllegalArgumentException("Can not locate the given directory: ${args[1]}")
}

println "\n*** deploy source files ***"
def sourceZipFile = zipSource(lwjglSourceDir)

mvnDeploy("lwjgl", "sources", version, sourceZipFile)

println "\nElapsed time: ${(System.currentTimeMillis()-startTime)/1000} seconds\n"

def zipSource(sourceDir) {
	def buf = new byte[1024]
	def zipFile = new File(sourceDir, "src.zip")
	def zip = new ZipOutputStream(new FileOutputStream(zipFile))
	def javaSourceDir = new File(new File(sourceDir, "src"), "java")
	javaSourceDir.eachFileRecurse {
		if (!it.isDirectory()) {		
			def input = new FileInputStream(it)
			zip.putNextEntry(new ZipEntry(it.absolutePath.replace(javaSourceDir.absolutePath, "")))
			def len;
			while ((len = input.read(buf)) > 0) {
	            zip.write(buf, 0, len)
	        }
			zip.closeEntry()
	        input.close()
		}
	}
	zip.close()
	zipFile
}

def nativeJar(lwjglDir, platform, includeRegex, jarFilename) {
	def platformDir = new File(new File(lwjglDir, "native"), platform)
	def nativeLibrariesToJar = []
	platformDir.eachFile {
		if (it.name.matches(includeRegex)) {
			nativeLibrariesToJar << it
		}
	}
	println "found native libraries for (${platform}) to JAR -- ${nativeLibrariesToJar.size()} libraries found"
	def jarFile = new File(platformDir, jarFilename)
	if (!jarFile.exists())  {
		putFileInToZipFile(jarFile, nativeLibrariesToJar)
	} else {
		println "jar of natives already exists"
	}
	jarFile
}

def putFileInToZipFile(zipFile, filesToZip) {
	def buf = new byte[1024]
	def zip = new ZipOutputStream(new FileOutputStream(zipFile))
	filesToZip.each {
		def input = new FileInputStream(it)
		zip.putNextEntry(new ZipEntry(it.name))
		def len;
		while ((len = input.read(buf)) > 0) {
            zip.write(buf, 0, len)
        }
		zip.closeEntry()
        input.close()
	}
	zip.close()
}

def mvnDeploy(artifactId, version, file) {
	mvnDeploy(artifactId, null, version, file)
}

def mvnDeploy(artifactId, classifier, version, file) {
	println "deploying: ${artifactId} v.${version}"
	
	def url="dav:https://b2s-repo.googlecode.com/svn/trunk/mvn-repo"
	def cmd="mvn deploy:deploy-file -DgroupId=org.lwjgl -DartifactId=${artifactId} -Dversion=${version} -Dfile=${file.absolutePath} -DrepositoryId=b2s-repo -Durl=${url} -Dpackaging=jar"
	if (classifier) {
		cmd += " -Dclassifier=${classifier}"
	}
	def output = cmd.execute().text
}