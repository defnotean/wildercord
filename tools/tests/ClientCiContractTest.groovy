// Executes the very same class loaded by build.gradle; no Loom/native client.
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def contract = new GroovyClassLoader(getClass().classLoader).parseClass(new File(args[0]))
def cases = new JsonSlurper().parse(new File(args[1]))
def results = cases.collect { fixture ->
    File processed = null
    try {
        if (fixture.containsKey('processed')) {
            processed = File.createTempFile('client-descriptor-', '.json')
            processed.setText(JsonOutput.toJson([entrypoints: ['fabric-client-gametest': fixture.processed],
                custom: ['wildercord:clientShardPlan': fixture.processedPlan]]), 'UTF-8')
        }
        def selection = contract.selection(fixture.entries, fixture.properties,
            new File(args[2]), new File(fixture.get('planPath', args[3])),
            new File(fixture.get('sourcePath', args[4])),
            fixture.containsKey('processedPath') ? new File(fixture.processedPath) : processed)
        def profile = contract.profile(fixture.environment, fixture.shaders)
        return [selection: selection, profile: profile]
    } catch (IllegalArgumentException exc) {
        return [error: true]
    } finally {
        if (processed != null) processed.delete()
    }
}
println(JsonOutput.toJson(results))
