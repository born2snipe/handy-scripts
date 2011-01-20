if (args.length == 0) {
	println "usage: script [groupId:]artifactId"
	return
}

def groupId = "b2s"
def artifactId = args[0]

def parts = args[0].split(":")
if (parts.length == 2) {
	groupId = parts[0]
	artifactId = parts[1]
}

println "mvn -DgroupId=${groupId} -DartifactId=${artifactId} -DarchetypeVersion=1.7 -Darchetype.interactive=false -DarchetypeArtifactId=netbeans-platform-app-archetype -DarchetypeRepository=http://repo1.maven.org/maven2/ -Dversion=1.0-SNAPSHOT -DarchetypeGroupId=org.codehaus.mojo.archetypes -DnetbeansVersion=RELEASE70-BETA  --batch-mode archetype:generate".execute().text

